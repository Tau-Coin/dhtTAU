package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.taucoin.account.AccountManager;
import io.taucoin.account.KeyChangedListener;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.FriendInfo;
import io.taucoin.core.FriendPair;
import io.taucoin.core.HashPrefixArrayInfo;
import io.taucoin.core.MessageList;
import io.taucoin.core.MutableDataWrapper;
import io.taucoin.core.NewMsgSignal;
import io.taucoin.core.OnlineSignal;
import io.taucoin.db.DBException;
import io.taucoin.dht2.DHT;
import io.taucoin.dht2.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.repository.AppRepository;
import io.taucoin.types.GossipItem;
import io.taucoin.types.Message;
import io.taucoin.types.MutableDataType;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

import static io.taucoin.param.ChainParam.SHORT_ADDRESS_LENGTH;

public class Communication implements DHT.GetMutableItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 朋友延迟访问时间，根据dht short time设定
    private final int DELAY_TIME = 1000; // 1000 ms

    // 主循环间隔最小时间
    private final int DEFAULT_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 数据允许接受的时间： 以当前时间6h之前为界
    private final int ACCEPT_DATA_TIME = 6 * 60 * 60; // 6 h

    // 最大可容纳的多设备数量
    private final int MAX_DEVICE_NUMBER = 32;

    // 主循环间隔时间
    private int loopIntervalTime = DEFAULT_LOOP_INTERVAL_TIME;

    // 设备ID
    private final byte[] deviceID;

    private final MsgListener msgListener;

    private final AppRepository repository;

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 朋友被延迟访问的时间
//    private final Map<ByteArrayWrapper, BigInteger> friendDelayTime = new ConcurrentHashMap<>();

    // TODO:: 1. 对方上次给我发信息的时间； 2. 对方在新时间
    // TODO:: 对方在线可能是个隐私问题，需要从YY中获得
    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> lastSeen = new ConcurrentHashMap<>();

    // 发现的最新信号时间(区分多设备)<friend, <device id, time> >
    private final Map<ByteArrayWrapper, HashMap<ByteArrayWrapper, BigInteger>> latestSignalTime = new ConcurrentHashMap<>();

    // 待处理的消息哈希前缀数组集合
    private final Map<ByteArrayWrapper, LinkedHashSet<HashPrefixArrayInfo>> hashPrefixArrayCache = new ConcurrentHashMap<>();

    // 最新的新消息信号集合
//    private final Map<ByteArrayWrapper, NewMsgSignal> latestNewMsgSignal = new ConcurrentHashMap<>();

    // 等待发布数据的peer
    private final Set<ByteArrayWrapper> publishFriends = new CopyOnWriteArraySet<>();

    // Communication thread.
    private Thread communicationThread;

    public Communication(byte[] deviceID, MsgListener msgListener, AppRepository repository) {
        this.deviceID = adjustDeviceID(deviceID);
        this.msgListener = msgListener;
        this.repository = repository;
    }

    /**
     * 调整device id长度，使之不超过长度限制
     * @param deviceID device id
     * @return adjusted device id
     */
    private byte[] adjustDeviceID(byte[] deviceID) {
        if (null == deviceID) {
            return new byte[1];
        }

        if (deviceID.length <= ChainParam.DEVICE_ID_LIMIT_SIZE) {
            return deviceID;
        }

        byte[] shortDeviceID = new byte[ChainParam.DEVICE_ID_LIMIT_SIZE];
        System.arraycopy(deviceID, 0, shortDeviceID, 0, ChainParam.DEVICE_ID_LIMIT_SIZE);
        return shortDeviceID;
    }

    /**
     * 检查朋友列表是否变动，变动则进行增删调整，避免内存泄露
     */
    private void checkFriends() {
        Set<byte[]> friends = this.repository.getAllFriends();

        if (null != friends) {
            // 移除已经删除的朋友
            for (ByteArrayWrapper localFriend: this.friends) {
                boolean found = false;
                for (byte[] friend: friends) {
                    if (Arrays.equals(localFriend.getData(), friend)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    removeFriend(localFriend.getData());
                }
            }

            // 添加新朋友
            for (byte[] friend: friends) {
                addNewFriend(friend);
            }
        }
    }

    /**
     * 发布需要发布数据的peer的数据
     */
    private void publishFriendMutableData() {
        for (ByteArrayWrapper peer: this.publishFriends) {
            try {
                publishFriendMutableData(peer);
            } catch (RuntimeException e) {
                logger.error(peer.toString() + ":" + e.getMessage(), e);
            }

            this.publishFriends.remove(peer);
        }
    }

    /**
     * 向朋友发布相关mutable数据
     * @param peer 发布数据的对象
     */
    private void publishFriendMutableData(ByteArrayWrapper peer) {
        List<ByteArrayWrapper> dataSet = new ArrayList<>();
        // 关于此处的missing message集合，使用linked hash set，而不是set或者list的原因:
        // 1. 不使用list的原因，是因为每个消息是前后两个相结合生成的bloom filter，因此，每个消息会被使用两次，
        // 因而也就有可能被添加进集合两次，list不具有去重作用
        // 2. 不使用set的原因，是识别对方缺少消息机制存在假阴性问题（也就是不缺该消息，却会被判定为缺少该消息），
        // 而set不像list那样是有序的，如果这样把set集合里面的消息put给对方，对方可能会拿到一些互相不相邻的消息，
        // 而这些不相邻的消息，由于假阴性问题的存在，仍然会重新会被判定为不存在而重新put，
        // 这样就会导致一直put这多个对方已有的数据，从而陷入死局
        // 3. 使用linked hash set这种既有顺序，又有唯一性保证的集合，能实现按顺序put，只要按顺序put，
        // 消息之间是相邻的，那么最多只有右边界的消息是假阴性的，这样dht put的8个一个item中最多只有一个是假阴性的，
        // 其它7个（若有7个）肯定是对方缺少的消息，这样不会陷入死局，对方会逐渐拿到缺少的数据

        Set<Message> missingMessageSet = new HashSet<>();

        BigInteger currentTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        // 新消息信号必发送
        // 判断是XY频道还是XX频道
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        if (Arrays.equals(pubKey, peer.getData())) {
            OnlineSignal onlineSignal = makeOnlineSignal();
            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.ONLINE_SIGNAL,
                    onlineSignal.getEncoded());
            dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));
        } else {
            NewMsgSignal newMsgSignal = makeNewMsgSignal(peer);
            MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.NEW_MSG_SIGNAL,
                    newMsgSignal.getEncoded());
            dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));
        }

        // 取出待处理的新消息信号，用来构建本次put的消息集合
        LinkedHashSet<HashPrefixArrayInfo> hashPrefixArrayInfoList = this.hashPrefixArrayCache.get(peer);

        if (null != hashPrefixArrayInfoList) {
            for (HashPrefixArrayInfo hashPrefixArrayInfo : hashPrefixArrayInfoList) {
                try {
                    byte[] hashPrefixArray = hashPrefixArrayInfo.getHashPrefixArray();
                    BigInteger timestamp = hashPrefixArrayInfo.getTimestamp();

                    // 寻找对方缺失的消息和确认收到的消息
                    List<Message> messageList = this.repository.getLatestMessageList(peer.getData(), ChainParam.MAX_MESSAGE_LIST_SIZE);

                    SolutionInfo solutionInfo = findBestSolution(messageList, hashPrefixArray);

                    // 将发现的确认消息通知UI
                    if (!solutionInfo.confirmationRootList.isEmpty()) {
                        this.msgListener.onReadMessageRoot(peer.getData(), solutionInfo.confirmationRootList, timestamp);
                    }

                    // 将发现的缺失消息加入集合，等待组合put出去
                    missingMessageSet.addAll(solutionInfo.missingMessageList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                hashPrefixArrayInfoList.remove(hashPrefixArrayInfo);
            }
        }

        // 随机排列missing messages，实现随机发送消息的效果
        List<Message> missingMessageList = new ArrayList<>();
        int index = 0;
        while (!missingMessageSet.isEmpty()) {
            // 随机数种子采用时间+last index方式，避免产生一样的随机数
            Random random = new Random(System.currentTimeMillis() + index);
            index = random.nextInt(missingMessageSet.size());
            Iterator<Message> iterator = missingMessageSet.iterator();

            int i = 0;
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (i == index) {
                    missingMessageList.add(message);
                    iterator.remove();
                    break;
                }
                i++;
            }
        }

        // 构建put数据集合
        while (dataSet.size() < ChainParam.MAX_DHT_PUT_ITEM_SIZE && !missingMessageList.isEmpty()) {
            List<Message> messages = new ArrayList<>();
            MessageList messageList = new MessageList(messages);

            Iterator<Message> iterator = missingMessageList.iterator();
            // 构造一个尺寸安全的消息列表
            while (iterator.hasNext()) {
                Message message= iterator.next();

                // 合法性判断
                if (validateMessage(message)) {
                    if (messageList.getEncoded().length + message.getEncoded().length <= ChainParam.MESSAGE_LIST_SAFE_SIZE) {
                        // 如果还能装载，继续填装
                        logger.info("Put message:{}", message.toString());
                        this.msgListener.onSyncMessage(message, currentTime);
                        messages.add(message);
                        messageList = new MessageList(messages);

                        // 用过的消息删掉
                        iterator.remove();
                    } else {
                        break;
                    }
                } else {
                    iterator.remove();
                }
            }

            // 如果消息列表里面有消息
            if (!messages.isEmpty()) {
                MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(MutableDataType.MESSAGE_LIST,
                        messageList.getEncoded());
                dataSet.add(new ByteArrayWrapper(mutableDataWrapper.getEncoded()));
            }
        }

        publishMutableData(peer.getData(), dataSet);
    }

    /**
     * 挑选一个推荐的朋友访问，没有则随机挑一个访问
     */
    private void visitReferredFriends() {
        ByteArrayWrapper peer = null;

        // 首先看是否有正在聊天的朋友
        byte[] chattingFriend = this.repository.getChattingFriend();

        Random random = new Random(System.currentTimeMillis());
        int index = random.nextInt(10);

        // 80%的概率选中正在聊天的朋友
        if (null != chattingFriend && index < 8) {
            peer = new ByteArrayWrapper(chattingFriend);
        } else {
            // 其次，从近期有聊天的朋友里挑选
            List<byte[]> activeFriends = this.repository.getActiveFriends();

            random = new Random(System.currentTimeMillis() + index);
            index = random.nextInt(10);

            // 70%的概率选中LAST COMM 在一周内 && Last seen 在10 minutes的朋友
            if (null != activeFriends) {
                if (!activeFriends.isEmpty() && index < 7) {
                    Iterator<byte[]> iterator = activeFriends.iterator();

                    random = new Random(System.currentTimeMillis() + index);
                    index = random.nextInt(activeFriends.size());

                    int i = 0;
                    while (iterator.hasNext()) {
                        byte[] pubKey = iterator.next();
                        if (i == index) {
                            peer = new ByteArrayWrapper(pubKey);
                            break;
                        }

                        i++;
                    }
                } else {
                    // 最后，在剩下的其它朋友里面挑选
                    List<byte[]> otherFriends = new ArrayList<>();

                    for (ByteArrayWrapper friend: this.friends) {
                        boolean found = false;
                        for (byte[] activeFriend: activeFriends) {
                            if (Arrays.equals(friend.getData(), activeFriend)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            otherFriends.add(friend.getData());
                        }
                    }

                    if (!otherFriends.isEmpty()) {
                        Iterator<byte[]> iterator = otherFriends.iterator();

                        random = new Random(System.currentTimeMillis() + index);
                        index = random.nextInt(otherFriends.size());

                        int i = 0;
                        while (iterator.hasNext()) {
                            byte[] pubKey = iterator.next();
                            if (i == index) {
                                peer = new ByteArrayWrapper(pubKey);
                                break;
                            }

                            i++;
                        }
                    }
                }
            }
        }

        if (null != peer) {
//            updateCounter(peer);

            // 如果选中的朋友没在延迟列表的延迟期(1600 ms)，则访问它
//            long currentTime = System.currentTimeMillis();
//            BigInteger timestamp = this.friendDelayTime.get(peer);
//            if (null == timestamp || currentTime >= timestamp.longValue() ) {
                // 没在延迟列表
                requestMutableDataFromPeer(peer);
                // 加入put列表
                this.publishFriends.add(peer);
                // 更新延迟时间
//                this.friendDelayTime.put(peer, BigInteger.valueOf(currentTime + this.DELAY_TIME));
//            }
        }
    }

    /**
     * 构造某个朋友的新消息信号
     * @param peer 构造新消息信号的朋友
     * @return 在线信号
     */
    private NewMsgSignal makeNewMsgSignal(ByteArrayWrapper peer) {
        byte[] hashPrefixArray = null;
        BigInteger chattingTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        List<Message> messageList = this.repository.getLatestMessageList(peer.getData(), ChainParam.MAX_MESSAGE_LIST_SIZE);
        if (null != messageList && !messageList.isEmpty()) {
            int size = messageList.size();
            hashPrefixArray = new byte[size];
            for (int i = 0; i < size; i++) {
                byte[] hash = messageList.get(i).getSha1Hash();
                hashPrefixArray[i] = hash[0];
            }
        }

        return new NewMsgSignal(this.deviceID, hashPrefixArray, chattingTime);
    }

    /**
     * 构造在线信号
     * @return 在线信号
     */
    private OnlineSignal makeOnlineSignal() {
        byte[] peer = AccountManager.getInstance().getKeyPair().first;

        byte[] hashPrefixArray = null;
        BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        List<Message> messageList = this.repository.getLatestMessageList(peer, ChainParam.MAX_MESSAGE_LIST_SIZE);
        if (null != messageList && !messageList.isEmpty()) {
            int size = messageList.size();
            hashPrefixArray = new byte[size];
            for (int i = 0; i < size; i++) {
                byte[] hash = messageList.get(i).getSha1Hash();
                hashPrefixArray[i] = hash[0];
            }
        }

        byte[] friend = getFriendRandomly();
        FriendInfo friendInfo = this.repository.getFriendInfo(friend);

        return new OnlineSignal(this.deviceID, hashPrefixArray, friendInfo, timestamp);
    }

    /**
     *发布mutable数据列表
     * @param peer 发布数据的对象
     * @param list 发布的数据列表
     */
    private void publishMutableData(byte[] peer, List<ByteArrayWrapper> list) {
        logger.debug("Put mutable data to peer:{}", Hex.toHexString(peer));
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        if (null != list && !list.isEmpty()) {
            byte[] salt = makeSendingSalt(keyPair.first, peer);
            DHT.MutableItemBatch mutableItemBatch = new DHT.MutableItemBatch(keyPair.first,
                    keyPair.second, list, salt);
            DHTEngine.getInstance().distribute(mutableItemBatch, null, null);
        }
    }

    /**
     * 向某个peer请求mutable数据
     * @param peer public key
     */
    private void requestMutableDataFromPeer(ByteArrayWrapper peer) {
        if (null != peer) {
            logger.trace("Request mutable data from peer:{}", peer.toString());

            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            byte[] salt = makeReceivingSalt(pubKey, peer.getData());
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(peer);

            DHTEngine.getInstance().request(spec, this, dataIdentifier);
        }
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long startTime = System.currentTimeMillis();

                checkFriends();

                // 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                // 发布需要发布数据的peer的数据
                publishFriendMutableData();

                try {
                    // 获取间隔时间
                    this.loopIntervalTime = this.repository.getMainLoopInterval();
                    if (this.loopIntervalTime < this.DEFAULT_LOOP_INTERVAL_TIME) {
                        this.loopIntervalTime = this.DEFAULT_LOOP_INTERVAL_TIME;
                    }

                    // 计算已经花费的时间，算出实际应该sleep的时间
                    long costTime = System.currentTimeMillis() - startTime;

                    if (this.loopIntervalTime > costTime) {
                        this.loopIntervalTime = this.loopIntervalTime - (int)costTime;
                    } else {
                        this.loopIntervalTime = 0;
                    }

                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.DEFAULT_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 验证消息，目前只验证编码长度
     * @param message 消息
     * @return true/false
     */
    private boolean validateMessage(Message message) {
        if (null != message) {
            if (message.getEncoded().length > ChainParam.MESSAGE_LIST_SAFE_SIZE) {
                logger.error("Oversize message:{}", Hex.toHexString(message.getHash()));
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * 构造mutable数据频道发送侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeSendingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(pubKey, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(friend, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 构造mutable数据频道接收侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeReceivingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 随机获取一个朋友
     * @return friend
     */
    private byte[] getFriendRandomly() {
        byte[] friend = null;
        int size = this.friends.size();
        if (size > 0) {
            Iterator<ByteArrayWrapper> it = this.friends.iterator();
            Random random = new Random(System.currentTimeMillis());
            int index = random.nextInt(size) + 1;

            int i = 0;
            while (it.hasNext() && i < index) {
                friend = it.next().getData();
                i++;
            }
        }

        return friend;
    }

    /**
     * 添加新朋友
     * @param friend public key
     */
    public void addNewFriend(byte[] friend) {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);
        this.friends.add(key);
    }

    /**
     * 删除朋友
     * @param friend public key
     */
    public void removeFriend(byte[] friend) {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);

        clearPeerCache(key);
    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {
        AccountManager.getInstance().addListener(this);

        communicationThread = new Thread(this::mainLoop);
        communicationThread.start();

        return true;
    }

    /**
     * Stop thread
     */
    public void stop() {
        if (null != communicationThread) {
            communicationThread.interrupt();
        }

        AccountManager.getInstance().removeListener(this);
    }

    /**
     * 删掉该朋友相关的缓存
     * @param peer 要删掉的朋友
     */
    private void clearPeerCache(ByteArrayWrapper peer) {
        this.friends.remove(peer);
//        this.friendDelayTime.remove(peer);
        this.lastSeen.remove(peer);
        this.publishFriends.remove(peer);
    }

    /**
     * 清空所有缓存数据
     */
    private void clearAllCache() {
        this.friends.clear();
//        this.friendDelayTime.clear();
        this.lastSeen.clear();
        this.publishFriends.clear();
    }

    @Override
    public void onKeyChanged(Pair<byte[], byte[]> newKey) {
        clearAllCache();
    }

    /**
     * 选用编辑代价最小的，并返回该操作代表的操作数
     * @param swap 替换的代价
     * @param insert 插入的代价
     * @param delete 删除的代价
     * @return 0:替换，1：插入，2：删除
     */
    private int optCode(int swap, int insert, int delete) {
        // 如果替换编辑距离最少，则返回0标识，
        // 即使三种操作距离一样，优先选择替换操作
        if (swap <= insert && swap <= delete) {
            return 0;
        }

        // 如果插入操作编辑最少，返回1标识，如果插入和删除距离一样，优先选择插入
        if (insert < swap && insert <= delete) {
            return 1;
        }

        // 如果删除操作编辑最少，返回2标识
        return 2;
    }

    /**
     * 求取LevenshteinDistance的解，得到的信息
     */
    private static class SolutionInfo {
        List<Message> missingMessageList = new ArrayList<>();
        List<byte[]> confirmationRootList = new ArrayList<>();
    }

    /**
     * 使用LevenshteinDistance算法寻找最佳匹配，并提取相应解需要的中间信息，
     * 作为missing message和confirmation root信息来源
     * @param messageList 本地消息列表
     * @param hashPrefixArray 远端哈希前缀列表
     * @return 获取的中间解信息
     */
    private SolutionInfo findBestSolution(List<Message> messageList, byte[] hashPrefixArray) {
        long startTime = System.currentTimeMillis();
        SolutionInfo solutionInfo = new SolutionInfo();

        // 如果对方没有信息，则本地消息全为缺失消息
        if (null == hashPrefixArray) {
            solutionInfo.missingMessageList.addAll(messageList);
            return solutionInfo;
        }

        if (null != messageList && !messageList.isEmpty()) {
            int size = messageList.size();

            // 对方数组为source
            byte[] source = hashPrefixArray;
            // 本地消息数组为target
            byte[] target = new byte[size];
            for (int i = 0; i < size; i++) {
                byte[] hash = messageList.get(i).getSha1Hash();
                target[i] = hash[0];
            }

            int sourceLength = source.length;
            int targetLength = target.length;

            // 如果源长度为零，则全插入
            if (sourceLength == 0) {
                solutionInfo.missingMessageList.addAll(messageList);
                return solutionInfo;
            }

            // 如果source和target一样，则直接跳过Levenshtein数组匹配计算
            if (Arrays.equals(source, target)) {
                for (Message message: messageList) {
                    solutionInfo.confirmationRootList.add(message.getHash());
                }
                return solutionInfo;
            }

            // 状态转移矩阵
            int[][] dist = new int[sourceLength + 1][targetLength + 1];
            // 操作矩阵
            int[][] operations = new int[sourceLength + 1][targetLength + 1];

            // 初始化，[i, 0]转换到空，需要编辑的距离，也即删除的数量
            for (int i = 0; i < sourceLength + 1; i++) {
                dist[i][0] = i;
                if (i > 0) {
                    operations[i][0] = 2;
                }
            }

            // 初始化，空转换到[0, j]，需要编辑的距离，也即增加的数量
            for (int j = 0; j < targetLength + 1; j++) {
                dist[0][j] = j;
                if (j > 0) {
                    operations[0][j] = 1;
                }
            }

            // 开始填充状态转移矩阵，第0位为空，所以从1开始有数据，[i, j]为当前子串最小编辑操作
            for (int i = 1; i < sourceLength + 1; i++) {
                for (int j = 1; j < targetLength + 1; j++) {
                    // 第i个数据，实际的index需要i-1，替换的代价，相同无需替换，代价为0，不同代价为1
                    int cost = source[i - 1] == target[j - 1] ? 0 : 1;
                    // [i, j]在[i, j-1]的基础上，最小的编辑操作为增加1
                    int insert = dist[i][j - 1] + 1;
                    // [i, j]在[i-1, j]的基础上，最小的编辑操作为删除1
                    int delete = dist[i - 1][j] + 1;
                    // [i, j]在[i-1, j-1]的基础上，最大的编辑操作为1次替换
                    int swap = dist[i - 1][j - 1] + cost;

                    // 在[i-1, j]， [i, j-1]， [i-1, j-1]三种转换到[i, j]的最小操作中，取最小值
                    dist[i][j] = Math.min(Math.min(insert, delete), swap);

                    // 选择一种最少编辑的操作
                    operations[i][j] = optCode(swap, insert, delete);
                }
            }

            // 回溯编辑路径，统计中间信息
            int i = sourceLength;
            int j = targetLength;
            while (0 != dist[i][j]) {
                if (0 == operations[i][j]) {
                    // 如果是替换操作，则将target对应的替换消息加入列表
                    if (source[i-1] != target[j-1]) {
                        solutionInfo.missingMessageList.add(messageList.get(j - 1));
                    } else {
                        solutionInfo.confirmationRootList.add(messageList.get(j - 1).getHash());
                    }
                    i--;
                    j--;
                } else if (1 == operations[i][j]) {
                    // 如果是插入操作，则将target对应的插入消息加入列表
                    // 如果缺最后一个，并且此时双方满载，则判定为被挤出去的
                    if (targetLength != j || targetLength != ChainParam.MAX_MESSAGE_LIST_SIZE ||
                            sourceLength != ChainParam.MAX_MESSAGE_LIST_SIZE) {
                        solutionInfo.missingMessageList.add(messageList.get(j-1));

                        // 如果是插入操作，则将邻近哈希前缀一样的消息也当作缺失的消息
                        int k = j - 1;
                        while (k + 1 < targetLength && target[k] == target[k + 1]) {
                            solutionInfo.missingMessageList.add(messageList.get(k + 1));
                            k++;
                        }
                    }

                    j--;
                } else if (2 == operations[i][j]) {
                    // 如果是删除操作，可能是对方新消息，忽略
                    i--;
                }
            }

            // 找到距离为0可能仍然不够，可能有前缀相同的情况，这时dist[i][j]很多为0的情况，
            // 因此，需要把剩余的加入confirmation root集合即可
            for(; j > 0; j--) {
                solutionInfo.confirmationRootList.add(messageList.get(j - 1).getHash());
            }
        }

//        Collections.reverse(solutionInfo.missingMessageList);
        long endTime = System.currentTimeMillis();
        logger.info("Cost time:{}", endTime - startTime);

        return solutionInfo;
    }

    /**
     * 处理收到的mutable data
     * @param mutableDataWrapper 收到的mutable data
     * @param peer gossip发出的peer
     */
    private void processMutableData(MutableDataWrapper mutableDataWrapper, ByteArrayWrapper peer) {

        // 是否更新好友的在线时间（完整公钥）
        BigInteger timestamp = mutableDataWrapper.getTimestamp();
        BigInteger lastSeen = this.lastSeen.get(peer);
        long currentTime = System.currentTimeMillis() / 1000;

        if (null != lastSeen && lastSeen.compareTo(timestamp) > 0) {
            logger.debug("-----old mutable data from peer:{}", peer.toString());
        }

        // 判断时间戳，以避免处理历史数据
        if (null == lastSeen || lastSeen.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
            logger.debug("Newer data from peer:{}", peer.toString());
            this.lastSeen.put(peer, timestamp);
            this.msgListener.onDiscoveryFriend(peer.getData(), timestamp);
//            this.onlineFriendsToNotify.put(peer, timestamp);
        }
        switch (mutableDataWrapper.getMutableDataType()) {
            case MESSAGE_LIST: {
                MessageList messageList = new MessageList(mutableDataWrapper.getData());
                List<Message> messages = messageList.getMessageList();
                if (null != messages) {
                    logger.debug("Got message list, size:{}", messages.size());
                    this.msgListener.onNewMessage(peer.getData(), messages);
                }

                break;
            }
            case NEW_MSG_SIGNAL: {
                byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
                if (Arrays.equals(peer.getData(), pubKey)) {
                    logger.warn("Error data.");
                    return;
                }

                NewMsgSignal newMsgSignal = new NewMsgSignal(mutableDataWrapper.getData());

                logger.info("------------Signal Time diff:{}", currentTime - timestamp.longValue());

                // 判断时间戳，以避免处理历史数据
                if (timestamp.longValue() > currentTime - this.ACCEPT_DATA_TIME && timestamp.longValue() < currentTime + this.ACCEPT_DATA_TIME) {
                    logger.info("Accepted new message signal:{} from peer:{}", newMsgSignal.toString(), peer.toString());

                    HashMap<ByteArrayWrapper, BigInteger> hashMap = this.latestSignalTime.get(peer);
                    // 如果map为空，填充新对象
                    if (null == hashMap) {
                        hashMap = new HashMap<>();
                        this.latestSignalTime.put(peer, hashMap);
                    }

                    // 查找对应peer对应设备的最新信号时间
                    byte[] deviceID = newMsgSignal.getDeviceID();
                    ByteArrayWrapper deviceIDKey = new ByteArrayWrapper(deviceID);
                    BigInteger latestSignalTime = hashMap.get(deviceIDKey);

                    // 只处理最新时间戳的信号
                    if (null == latestSignalTime || latestSignalTime.compareTo(newMsgSignal.getTimestamp()) < 0) {
                        // 记录改设备最新信号时间
                        hashMap.put(deviceIDKey, newMsgSignal.getTimestamp());

                        if (null != newMsgSignal.getHashPrefixArray()) {
                            // 添加到缓存，等待处理
                            LinkedHashSet<HashPrefixArrayInfo> linkedList = this.hashPrefixArrayCache.get(peer);
                            if (null == linkedList) {
                                linkedList = new LinkedHashSet<>();
                                this.hashPrefixArrayCache.put(peer, linkedList);
                            }
                            linkedList.add(new HashPrefixArrayInfo(newMsgSignal.getHashPrefixArray(), newMsgSignal.getTimestamp()));
                        }
                    }

                    // 如果设备太多，则删除一个时间最老的
                    if (hashMap.size() > MAX_DEVICE_NUMBER) {
                        ByteArrayWrapper oldestTimeKey = null;
                        BigInteger oldestTime = BigInteger.ZERO;
                        for (Map.Entry<ByteArrayWrapper, BigInteger> entry: hashMap.entrySet()) {
                            if (entry.getValue().compareTo(oldestTime) < 0) {
                                oldestTimeKey = entry.getKey();
                            }
                        }
                        hashMap.remove(oldestTimeKey);
                    }
                } else {
                    logger.warn("The timestamp from NewMsgSignal is too old. Current time:{}, timestamp:{}, diff:{}", currentTime, timestamp, currentTime - timestamp.longValue());
                }

                break;
            }
            case ONLINE_SIGNAL: {
                byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
                if (!Arrays.equals(peer.getData(), pubKey)) {
                    logger.warn("Error data..");
                    return;
                }

                OnlineSignal onlineSignal = new OnlineSignal(mutableDataWrapper.getData());

                byte[] deviceID = onlineSignal.getDeviceID();
                // 忽略同一台设备的信号
                if (Arrays.equals(this.deviceID, deviceID)) {
                    return;
                }

                // 判断时间戳，以避免处理历史数据
                if (timestamp.longValue() > currentTime - this.ACCEPT_DATA_TIME && timestamp.longValue() < currentTime + this.ACCEPT_DATA_TIME) {
                    logger.info("Accepted online signal:{} from peer:{}", onlineSignal.toString(), peer.toString());

                    // 先判断一下本地是否有该朋友
                    FriendInfo friendInfo = onlineSignal.getFriendInfo();
                    if (null != friendInfo) {
                        if (!this.friends.contains(new ByteArrayWrapper(friendInfo.getPubKey()))) {
                            this.msgListener.onNewFriendFromMultiDevice(friendInfo.getPubKey(),
                                    friendInfo.getNickname(), friendInfo.getTimestamp());
                        }
                    }

                    HashMap<ByteArrayWrapper, BigInteger> hashMap = this.latestSignalTime.get(peer);
                    if (null == hashMap) {
                        hashMap = new HashMap<>();
                        this.latestSignalTime.put(peer, hashMap);
                    }

                    ByteArrayWrapper deviceIDKey = new ByteArrayWrapper(deviceID);
                    BigInteger latestSignalTime = hashMap.get(deviceIDKey);

                    // 第一次发现，则通知新设备id
                    if (null == latestSignalTime) {
                        this.msgListener.onNewDeviceID(deviceID);
                    }

                    // 只处理最新时间戳的信号
                    if (null == latestSignalTime || latestSignalTime.compareTo(onlineSignal.getTimestamp()) < 0) {
                        // 记录改设备最新信号时间
                        hashMap.put(deviceIDKey, onlineSignal.getTimestamp());

                        if (null != onlineSignal.getHashPrefixArray()) {
                            // 添加到缓存，等待处理
                            LinkedHashSet<HashPrefixArrayInfo> linkedList = this.hashPrefixArrayCache.get(peer);
                            if (null == linkedList) {
                                linkedList = new LinkedHashSet<>();
                                this.hashPrefixArrayCache.put(peer, linkedList);
                            }
                            linkedList.add(new HashPrefixArrayInfo(onlineSignal.getHashPrefixArray(), onlineSignal.getTimestamp()));
                        }
                    }

                    // 如果设备太多，则删除一个时间最老的
                    if (hashMap.size() > MAX_DEVICE_NUMBER) {
                        ByteArrayWrapper oldestTimeKey = null;
                        BigInteger oldestTime = BigInteger.ZERO;
                        for (Map.Entry<ByteArrayWrapper, BigInteger> entry: hashMap.entrySet()) {
                            if (entry.getValue().compareTo(oldestTime) < 0) {
                                oldestTimeKey = entry.getKey();
                            }
                        }
                        hashMap.remove(oldestTimeKey);
                    }
                } else {
                    logger.warn("The timestamp from NewMsgSignal is too old. Current time:{}, timestamp:{}, diff:{}", currentTime, timestamp, currentTime - timestamp.longValue());
                }

                break;
            }
            case UNKNOWN: {
                logger.error("Unknown type.");
            }
        }
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData, boolean auth) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        if (null == item) {
            logger.debug("MULTIPLEX DATA from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
            return;
        }

        MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(item);
        processMutableData(mutableDataWrapper, dataIdentifier.getExtraInfo1());
    }

}
