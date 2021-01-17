package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.taucoin.account.AccountManager;
import io.taucoin.account.KeyChangedListener;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.DataType;
import io.taucoin.core.FriendPair;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.listener.MsgStatus;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipItem;
import io.taucoin.types.Gossip;
import io.taucoin.types.GossipStatus;
import io.taucoin.types.Message;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

/**
 * 主要功能：
 * 1. gossip机制的实现:通过gossip来打听哪个节点有新消息，从而用作下次访问的依据;
 * 2. app端与中间层数据的交互协调：app端通过communication模块来间接向中间层请求数据，中间层的数据通过communication
 * 模块间接调度送达app端；
 *
 * 设计思想：限于APP端不允许与中间层直接交互，中间层数据回调不允许处理繁重的任务，也就是app端与中间层没办法直接交互，
 * 因此，Communication模块必然需要设计大量缓存来缓存通报app端与中间层的交互数据；
 *
 * 目前，所有的数据，包括app端的需求数据以及中间层送来的数据，都会在主循环里面统一处理调度，主循化的主要任务包括：
 * 1. 通知UI发现的在线朋友
 * 2. 通知UI已读root
 * 3. 访问正在写状态的朋友
 * 4. 处理获得的消息
 * 5. 处理获得的gossip
 * 6. 处理获得的demand数据
 * 7. 相应远端的需求
 * 8. 尝试对gossip数据进行瘦身
 * 9. 尝试发布gossip数据
 * 10. 访问在线peer
 * 11. 访问通过gossip机制发现的活跃peer
 * 12. 请求demand数据
 * 13. 尝试向中间层发送所有的请求
 * 14. 尝试调整间隔时间
 */
public class Communication implements DHT.GetDHTItemCallback, DHT.PutDHTItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 对UI使用的, Queue capability.
    public static final int QueueCapability = 1000;

    // 判断中间层队列的门槛值，中间层队列使用率超过0.8，则增加主循环时间间隔
    private final double THRESHOLD = 0.8;

    // gossip item中public key保留的长度
    private static final int SHORT_ADDRESS_LENGTH = 4;

    // 2 min
    private static final int TWO_MINUTE = 120; // 120s

    // 主循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    // 发布gossip信息的时间间隔，默认10s
    private long gossipPublishIntervalTime = 10; // 10 s

    // 记录上一次发布gossip的时间
    private long lastGossipPublishTime = 0;

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // UI相关请求存放的queue，统一收发所有的请求，包括UI以及内部算法产生的请求，LinkedHashSet确保队列的顺序性与唯一性
    private final Set<Object> queue = Collections.synchronizedSet(new LinkedHashSet<>());

    // 发现的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    private final Set<GossipItem> gossipItems = new CopyOnWriteArraySet<>();

    // 等待发送出去的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    private final Set<GossipItem> gossipItemsToPut = new LinkedHashSet<>();

    // 得到的消息集合（hash <--> Message），ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, Message> messageMap = new ConcurrentHashMap<>();

    // 得到的消息与发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = new ConcurrentHashMap<>();

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendLastSeen = new ConcurrentHashMap<>();

    // 当前发现的等待通知的在线的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> onlineFriendsToNotify = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendTimeStamp = new ConcurrentHashMap<>();

    // 我的朋友的最新消息的root <friend, root>（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> friendRoot = new ConcurrentHashMap<>();

    // 我的朋友的最新确认的root <friend, root>（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> friendConfirmationRoot = new ConcurrentHashMap<>();

    // 新发现的，等待通知UI的，我的朋友的最新确认的root <friend, root>（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> friendConfirmationRootToNotify = new ConcurrentHashMap<>();

    // 等待通知的消息状态
    private final Map<ByteArrayWrapper, MsgStatus> msgStatus = new ConcurrentHashMap<>();

    // 给我的朋友的最新消息的时间戳 <friend, timestamp>，发现对方新的确认信息root也会更新该时间（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> timeStampToFriend = new ConcurrentHashMap<>();

    // 给我的朋友的最新消息的root <friend, root>（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> rootToFriend = new ConcurrentHashMap<>();

    // 当前我正在聊天的朋友集合，即用即弃（完整公钥）
    private final Set<ByteArrayWrapper> writingFriends = new CopyOnWriteArraySet<>();

    // 当前发现的处于写状态的朋友集合，即用即弃，<friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, Long> writingFriendsToVisit = new ConcurrentHashMap<>();

    // 我的需求 <friend, demand hash>，用于通知我的朋友我的需求（完整公钥）
    private final Map<ByteArrayWrapper, byte[]> demandImmutableDataHash = new ConcurrentHashMap<>();

    // 我收到的需求hash
    private final Set<ByteArrayWrapper> friendDemandHash = new CopyOnWriteArraySet<>();

    // 通过gossip推荐机制发现的有新消息的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> referredFriends = new CopyOnWriteArraySet<>();

    // 通过gossip机制发现的给朋友的gossip item <FriendPair, Timestamp>（非完整公钥, FriendPair均为短地址）
    private final Map<FriendPair, BigInteger> friendGossipItem = Collections.synchronizedMap(new HashMap<>());

    // Communication thread.
    private Thread communicationThread;

    public Communication(MessageDB messageDB, MsgListener msgListener) {
        this.messageDB = messageDB;
        this.msgListener = msgListener;
    }

    /**
     * 初始化，获取我以及朋友的最新root
     * @return true if success, false otherwise
     */
    private boolean init() {
        try {
            // get friend latest msg root
            Set<byte[]> friends = this.messageDB.getFriends();

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    logger.debug("My friend:{}", key.toString());

                    // 获取朋友给我消息的最新root
                    byte[] root = this.messageDB.getFriendMessageRoot(friend);
                    if (null != root) {
                        this.friendRoot.put(key, root);
                    }

                    // 获取我发给朋友消息的最新root
                    root = this.messageDB.getMessageToFriendRoot(friend);
                    if (null != root) {
                        this.rootToFriend.put(key, root);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 尝试发布gossip信息，看看时间间隔是否已到，每10s一次
     */
    private void tryToPublishGossipInfo() {
        long currentTime = System.currentTimeMillis() / 1000;

        if (currentTime - lastGossipPublishTime > gossipPublishIntervalTime) {
            publishGossipInfo();
        }
    }

    /**
     * 构建短地址
     * @param pubKey public key
     * @return short address
     */
    private byte[] makeShortAddress(byte[] pubKey) {
        if (pubKey.length > SHORT_ADDRESS_LENGTH) {
            byte[] shortAddress = new byte[SHORT_ADDRESS_LENGTH];
            System.arraycopy(pubKey, 0, shortAddress, 0, SHORT_ADDRESS_LENGTH);
            return shortAddress;
        }

        return pubKey;
    }

    /**
     * 使用短地址来构建gossip item
     * @param friendPair 短地址friend pair
     * @param timestamp gossip时间戳
     * @return gossip item
     */
    private GossipItem makeGossipItemWithShortAddress(FriendPair friendPair, BigInteger timestamp) {
        return new GossipItem(friendPair.getSender(), friendPair.getReceiver(), timestamp);
    }

    /**
     * 使用短地址来构建gossip item
     * @param sender sender public key
     * @param receiver receiver public key
     * @param timestamp timestamp
     * @return gossip item
     */
    private GossipItem makeGossipItemWithShortAddress(byte[] sender, byte[] receiver, BigInteger timestamp) {
        byte[] shortSender = new byte[SHORT_ADDRESS_LENGTH];
        System.arraycopy(sender, 0, shortSender, 0, SHORT_ADDRESS_LENGTH);

        byte[] shortReceiver = new byte[SHORT_ADDRESS_LENGTH];
        System.arraycopy(receiver, 0, shortReceiver, 0, SHORT_ADDRESS_LENGTH);

        return new GossipItem(shortSender, shortReceiver, timestamp);
    }

    /**
     * 使用短地址来构建gossip item
     * @param sender sender public key
     * @param receiver receiver public key
     * @param timestamp timestamp
     * @param root msg root
     * @param confirmationRoot confirmation root
     * @param demandImmutableDataHash demand hash
     * @param gossipStatus gossip status
     * @return gossip item
     */
    private GossipItem makeGossipItemWithShortAddress(byte[] sender, byte[] receiver,
                                                      BigInteger timestamp, byte[] root,
                                                      byte[] confirmationRoot, byte[] demandImmutableDataHash,
                                                      GossipStatus gossipStatus) {
        byte[] shortSender = new byte[SHORT_ADDRESS_LENGTH];
        System.arraycopy(sender, 0, shortSender, 0, SHORT_ADDRESS_LENGTH);

        byte[] shortReceiver = new byte[SHORT_ADDRESS_LENGTH];
        System.arraycopy(receiver, 0, shortReceiver, 0, SHORT_ADDRESS_LENGTH);

        return new GossipItem(shortSender, shortReceiver, timestamp, root, confirmationRoot, demandImmutableDataHash, gossipStatus);
    }

    /**
     * 获取下一个gossip临时集合，从缓存（各个消息渠道的队列）获取gossip所需的各项数据
     * @return gossip set
     */
    private LinkedHashSet<GossipItem> getGossipList() {
        LinkedHashSet<GossipItem> gossipList = new LinkedHashSet<>();

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        // 1.统计我发给我朋友的gossip
        long currentTime = System.currentTimeMillis() / 1000;
        for (ByteArrayWrapper friend: this.friends) {
            BigInteger timeStamp = this.timeStampToFriend.get(friend);
            byte[] root = this.rootToFriend.get(friend);
//            byte[] confirmationRoot = this.confirmationRootToFriend.get(friend);
            byte[] confirmationRoot = this.friendRoot.get(friend);
            byte[] demandImmutableDataHash = this.demandImmutableDataHash.get(friend);

            // 如果有demand需求，时间戳用最新时间，以便被捕获
            if (null != demandImmutableDataHash || null == timeStamp) {
                timeStamp = BigInteger.valueOf(currentTime);
            }

            if (this.writingFriends.contains(friend)) {
                GossipItem gossipItem = makeGossipItemWithShortAddress(pubKey, friend.getData(),
                        timeStamp, root, confirmationRoot, demandImmutableDataHash, GossipStatus.ON_WRITING);
                gossipList.add(gossipItem);

                this.writingFriends.remove(friend);
            } else {
                GossipItem gossipItem = makeGossipItemWithShortAddress(pubKey, friend.getData(),
                        timeStamp, root, confirmationRoot, demandImmutableDataHash, GossipStatus.UNKNOWN);
                gossipList.add(gossipItem);
            }
        }

        // 2.统计其他人发给我朋友的消息
        for (Map.Entry<FriendPair, BigInteger> entry : this.friendGossipItem.entrySet()) {
            BigInteger timestamp = entry.getValue();
            // 只添加一天以内有新消息的
            if (currentTime - timestamp.longValue() < ChainParam.ONE_DAY) {
                gossipList.add(makeGossipItemWithShortAddress(entry.getKey(), timestamp));
            }
        }

        return gossipList;
    }

    /**
     * 尝试对gossip信息瘦身，通过移除一天前的旧数据的方法
     */
    private void tryToSlimDownGossip() {
        long currentTime = System.currentTimeMillis() / 1000;
        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipItem.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            if (currentTime - entry.getValue().longValue() > ChainParam.ONE_DAY) {
                it.remove();
            }
        }
    }

    /**
     * 从朋友列表获取完整公钥
     * @param pubKey public key
     * @return 完整的公钥
     */
    private ByteArrayWrapper getCompletePubKeyFromFriend(byte[] pubKey) {
        for (ByteArrayWrapper key: this.friends) {
            if (ByteUtil.startsWith(key.getData(), pubKey)) {
                return key;
            }
        }
        
        return null;
    }

    /**
     * 处理朋友相关的gossip item
     */
    private void dealWithGossipItem() {
        Iterator<GossipItem> it = this.gossipItems.iterator();

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        // 顶层gossip是按照朋友关系进行平等遍历，未来可能根据频率心跳信号的存在来调整
        while (it.hasNext()) {
            GossipItem gossipItem = it.next();
            byte[] sender = gossipItem.getSender();
            byte[] receiver = gossipItem.getReceiver();

            // 发送者是我自己的gossip信息直接忽略，因为我自己的信息不需要依赖gossip
            if (!ByteUtil.startsWith(pubKey, sender)) {
                // 首先，判断一下是否有涉及自己的gossip，有的话则加入待访问的活跃朋友集合
                if (ByteUtil.startsWith(pubKey, receiver)) { // 接收者是我
                    // 寻找发送者完整公钥
                    ByteArrayWrapper peer = getCompletePubKeyFromFriend(sender);
                    if (null != peer) {
                        this.referredFriends.add(peer);
                    }
                } else {
                    // 寻找跟我朋友相关的gossip
                    for (ByteArrayWrapper friend : this.friends) {
                        if (ByteUtil.startsWith(friend.getData(), receiver)) { // 找到
                            FriendPair pair = FriendPair.create(makeShortAddress(sender), makeShortAddress(receiver));

                            BigInteger oldTimestamp = this.friendGossipItem.get(pair);

                            BigInteger timeStamp = gossipItem.getTimestamp();

                            // 判断是否是新数据，若是，则记录下来以待发布
                            if (null == oldTimestamp || oldTimestamp.compareTo(timeStamp) < 0) {
                                this.friendGossipItem.put(pair, timeStamp);
                            }

                            break;
                        }
                    }
                }
            }

            this.gossipItems.remove(gossipItem);
//            it.remove();
        }
    }

    /**
     * 挑选一个推荐的朋友访问，没有则随机挑一个访问
     */
    private void visitReferredFriends() {
        Iterator<ByteArrayWrapper> it = this.referredFriends.iterator();
        // 如果有现成的peer，则挑选一个peer访问
        if (it.hasNext()) {
            ByteArrayWrapper peer = it.next();
            requestGossipInfoFromPeer(peer);

            this.referredFriends.remove(peer);
//            it.remove();
        } else {
            // 没有找到推荐的活跃的peer，则自己随机访问一个自己的朋友
            Iterator<ByteArrayWrapper> iterator = this.friends.iterator();
            if (iterator.hasNext()) {

                Random random = new Random(System.currentTimeMillis());
                int index = random.nextInt(this.friends.size());

                ByteArrayWrapper peer = null;
                int i = 0;
                while (iterator.hasNext()) {
                    peer = iterator.next();
                    if (i == index) {
                        break;
                    }

                    i++;
                }

                if (null != peer) {
                    requestGossipInfoFromPeer(peer);
                }
            }
        }
    }

    /**
     * 通知UI消息状态
     */
    private void notifyUIMessageStatus() {
        Iterator<Map.Entry<ByteArrayWrapper, MsgStatus>> iterator = this.msgStatus.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, MsgStatus> entry = iterator.next();

            logger.debug("Notify UI msg status:{}, {}", entry.getKey().toString(), entry.getValue());
            this.msgListener.onMessageStatus(entry.getKey().getData(), entry.getValue());

            this.msgStatus.remove(entry.getKey());
        }
    }

    /**
     * 通知UI发现的还在线的朋友
     */
    private void notifyUIOnlineFriend() {
        for (ByteArrayWrapper friend: this.onlineFriendsToNotify) {
            logger.trace("Notify UI online friend:{}", friend.toString());
            this.msgListener.onDiscoveryFriend(friend.getData());

            this.onlineFriendsToNotify.remove(friend);
        }
    }

    /**
     * 通知UI新发现的朋友的已读消息的root
     */
    private void notifyUIReadMessageRoot() {
        Iterator<Map.Entry<ByteArrayWrapper, byte[]>> iterator = this.friendConfirmationRootToNotify.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, byte[]> entry = iterator.next();

            if (null != entry.getValue()) {
                logger.debug("Notify UI read message from friend:{}, root:{}",
                        entry.getKey().toString(), Hex.toHexString(entry.getValue()));

                this.msgListener.onReadMessageRoot(entry.getKey().getData(), entry.getValue());
            }

            this.friendConfirmationRootToNotify.remove(entry.getKey());
        }
    }

    /**
     * 访问当前正在写状态的朋友节点，发现写状态在120s以内的继续访问，超过120s的从访问清单删除
     */
    private void retrieveChattingFriendMsg() {
        long currentTime = System.currentTimeMillis() / 1000;
        for (Map.Entry<ByteArrayWrapper, Long> entry: this.writingFriendsToVisit.entrySet()) {
            if (currentTime - entry.getValue() > TWO_MINUTE) {
                this.writingFriendsToVisit.remove(entry.getKey());
            } else {
                requestLatestMessageFromPeer(entry.getKey());
            }
        }
    }

    /**
     * 处理收到的消息
     * @throws DBException database exception
     */
    private void dealWithMessage() throws DBException {

        // 将消息存入数据库并通知UI，尝试获取下一条消息
        Iterator<Map.Entry<ByteArrayWrapper, Message>> iterator = this.messageMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, Message> entry = iterator.next();
            Message message = entry.getValue();
            ByteArrayWrapper msgHash = entry.getKey();

            ByteArrayWrapper sender = this.messageSenderMap.get(msgHash);

            // save to db
            this.messageDB.putMessage(message.getHash(), message.getEncoded());

            logger.debug("Notify UI new message from friend:{}, hash:{}",
                    sender.toString(), Hex.toHexString(message.getHash()));

            // 通知UI新消息
            this.msgListener.onNewMessage(sender.getData(), message);

            // 从demand集合移除
            Iterator<Map.Entry<ByteArrayWrapper, byte[]>> iterator1 = this.demandImmutableDataHash.entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry<ByteArrayWrapper, byte[]> entry1 = iterator1.next();
                byte[] value = entry1.getValue();
                if (Arrays.equals(msgHash.getData(), value)) {
                    logger.debug("Remove hash[{}] from demand hash set", msgHash.toString());
                    this.demandImmutableDataHash.remove(entry1.getKey());
                }
            }

            // try to find next one
            byte[] hash = message.getPreviousMsgDAGRoot();
            if (null != hash) {
                // 先判断一下本地是否有，有的话则终止，没有则继续同步DAG，直至到连接到之前的数据
                if (null == this.messageDB.getMessageByHash(hash)) {
                    Message previousMsg = this.messageMap.get(new ByteArrayWrapper(hash));
                    if (null == previousMsg) {
                        // request next one
                        requestMessage(hash, sender);
                    }
                }
            }

            this.messageSenderMap.remove(msgHash);
            this.messageMap.remove(msgHash);
//            iterator.remove();
        }
    }

    /**
     * 回应远端的需求，一次回应连续10s的消息
     * @throws DBException database exception
     */
    private void responseRemoteDemand() throws DBException {
        Iterator<ByteArrayWrapper> it = this.friendDemandHash.iterator();
        while (it.hasNext()) {
            ByteArrayWrapper hash = it.next();
            logger.info("Response demand:{}", hash.toString());

            byte[] data = this.messageDB.getMessageByHash(hash.getData());
            if (null != data) {
                Message msg = new Message(data);
                BigInteger timestamp = msg.getTimestamp();
                publishImmutableData(data);

                while (null != msg.getPreviousMsgDAGRoot()) {
                    data = this.messageDB.getMessageByHash(msg.getPreviousMsgDAGRoot());
                    if (null != data) {
                        msg = new Message(data);
                        publishImmutableData(data);
                        if (timestamp.longValue() - msg.getTimestamp().longValue() > 10) {
                            break;
                        }
                    }
                }
            }

            this.friendDemandHash.remove(hash);
//            it.remove();
        }
    }

    /**
     * 请求immutable data
     */
    private void requestDemandHash() throws DBException {
        for (Map.Entry<ByteArrayWrapper, byte[]> entry: this.demandImmutableDataHash.entrySet()) {
            logger.trace("Request demand message hash:{}", Hex.toHexString(entry.getValue()));
            requestMessage(entry.getValue(), entry.getKey());
        }
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                // 1. 通知UI消息状态
                notifyUIMessageStatus();

                // 2. 通知UI发现的在线朋友
                notifyUIOnlineFriend();

                // 3. 通知UI已读root
                notifyUIReadMessageRoot();

                // 4. 访问正在写状态的朋友
                retrieveChattingFriendMsg();

                // TODO: 并行中继节点搜索，先进行搜索，将put与get分开，get的时候获取中继节点，put时候直接put

                // 5. 处理获得的消息
                dealWithMessage();

                // 6. 处理获得的gossip
                dealWithGossipItem();

                // 7. 相应远端的需求
                responseRemoteDemand();

                // 8. 尝试对gossip数据进行瘦身
                tryToSlimDownGossip();

                // 9. 尝试发布gossip数据
                tryToPublishGossipInfo();

                // 10. 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                // 11. 请求demand数据
                requestDemandHash();

                // 12. 尝试向中间层发送所有的请求
                tryToSendAllRequest();

                // 13. 尝试调整间隔时间
                adjustIntervalTime();

                try {
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            } catch (DBException e) {
                this.msgListener.onMsgError("Data Base Exception!");
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 向某个peer请求gossip数据
     * @param pubKey public key
     */
    private void requestGossipInfoFromPeer(ByteArrayWrapper pubKey) {
        if (null != pubKey) {
            logger.trace("Request gossip info from peer:{}", pubKey.toString());

            byte[] salt = ChainParam.GOSSIP_CHANNEL;
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER, pubKey);
            DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);
            this.queue.add(mutableItemRequest);
        }
    }

    /**
     * 向某个peer直接请求最新的message数据
     * @param pubKey public key
     */
    private void requestLatestMessageFromPeer(ByteArrayWrapper pubKey) {
        if (null != pubKey) {
            logger.debug("Request latest message from peer:{}", pubKey.toString());

            // 在当前时间频道请求
            byte[] salt = getReceivingSalt(pubKey.getData());
            logger.debug("Salt:{}", Hex.toHexString(salt));
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.LATEST_MESSAGE, pubKey);
            DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);
            this.queue.add(mutableItemRequest);
        }
    }

    /**
     * 请求对应哈希值的消息
     * @param hash msg hash
     * @param pubKey public key
     */
    public void requestMessage(byte[] hash, ByteArrayWrapper pubKey) {
        if (null != hash) {
            logger.debug("Message root:{}, public key:{}", Hex.toHexString(hash), pubKey.toString());
            DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.MESSAGE, pubKey, new ByteArrayWrapper(hash));

            DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
            this.queue.add(immutableItemRequest);
        }
    }

    /**
     * 发布消息
     * @param message msg to publish
     */
    private void publishMessage(Message message) {
        if (null != message) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(message.getEncoded());
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.PUT_IMMUTABLE_DATA,
                    new ByteArrayWrapper(message.getHash()));

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, this, dataIdentifier);
            this.queue.add(immutableItemDistribution);

            logger.debug("Msg status:{}, {}", Hex.toHexString(message.getHash()), MsgStatus.TO_COMMUNICATION_QUEUE);
            this.msgListener.onMessageStatus(message.getHash(), MsgStatus.TO_COMMUNICATION_QUEUE);
        }
    }

    /**
     * 发布immutable data
     * @param list immutable data list
     */
    public void publishImmutableData(List<byte[]> list) {
        if (null != list) {
            for (byte[] data: list) {
                publishImmutableData(data);
            }
        }
    }

    /**
     * 发布immutable data
     * @param data immutable data
     */
    public void publishImmutableData(byte[] data) {
        if (null != data) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(data);

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.add(immutableItemDistribution);
        }
    }

    /**
     * 在gossip频道发布gossip信息
     * @param gossip last gossip list
     */
    private void publishMutableGossip(Gossip gossip) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = ChainParam.GOSSIP_CHANNEL;
        byte[] encode = gossip.getEncoded();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHT.MutableItemDistribution mutableItemDistribution = new DHT.MutableItemDistribution(mutableItem, null, null);

            this.queue.add(mutableItemDistribution);
        }
    }

    /**
     * 发布所有gossip信息
     */
    private void publishGossipInfo() {
        // put mutable item
        // TODO:以后可能根据前台状态调整
        if (this.gossipItemsToPut.isEmpty()) {
            LinkedHashSet<GossipItem> gossipItemSet = getGossipList();
            for(GossipItem gossipItem: gossipItemSet) {
                logger.trace("Gossip set:{}", gossipItem.toString());
            }

            this.gossipItemsToPut.addAll(gossipItemSet);
        }

        List<GossipItem> list = new ArrayList<>();

        Iterator<GossipItem> iterator = this.gossipItemsToPut.iterator();
        int i = 0;
        // TODO:: 等待测试新的GOSSIP_LIMIT_SIZE
        while (iterator.hasNext() && i <= ChainParam.GOSSIP_LIMIT_SIZE) {
            GossipItem gossipItem = iterator.next();
            list.add(gossipItem);

            iterator.remove();
            i++;
        }

        if (!list.isEmpty()) {
            Gossip gossip = new Gossip(list);
            while (gossip.getEncoded().length >= ChainParam.DHT_ITEM_LIMIT_SIZE) {
                list.remove(list.size() - 1);
                gossip = new Gossip(list);
            }

            logger.debug(gossip.toString());
            publishMutableGossip(gossip);
        }

        // 记录发布时间
        this.lastGossipPublishTime = System.currentTimeMillis() / 1000;
    }

    /**
     * 处理分发各种请求
     * @param req request
     */
    private void process(Object req) {

        if (req == null) {
            return;
        }

        // dispatch dht request
        if (req instanceof DHT.ImmutableItemRequest) {
            requestImmutableItem((DHT.ImmutableItemRequest)req);
        } else if (req instanceof DHT.MutableItemRequest) {
            requestMutableItem((DHT.MutableItemRequest)req);
        } else if (req instanceof DHT.ImmutableItemDistribution) {
            putImmutableItem((DHT.ImmutableItemDistribution)req);
        } else if (req instanceof DHT.MutableItemDistribution) {
            putMutableItem((DHT.MutableItemDistribution)req);
        }
    }

    private void requestImmutableItem(DHT.ImmutableItemRequest req) {
        logger.info("requestImmutableItem:{}", req.toString());
        DHTEngine.getInstance().request(req.getSpec(), req.getCallback(), req.getCallbackData());
    }

    private void requestMutableItem(DHT.MutableItemRequest req) {
        logger.info("requestMutableItem:{}", req.toString());
        DHTEngine.getInstance().request(req.getSpec(), req.getCallback(), req.getCallbackData());
    }

    private void putImmutableItem(DHT.ImmutableItemDistribution d) {
        logger.info("putImmutableItem:{}", d.toString());
        DHTEngine.getInstance().distribute(d.getItem(), d.getCallback(), d.getCallbackData());

        logger.debug("Msg status:{}, {}", d.toString(), MsgStatus.TO_DHT_QUEUE);
        this.msgListener.onMessageStatus(Hex.decode(d.getItem().hash().toHex()), MsgStatus.TO_DHT_QUEUE);
    }

    private void putMutableItem(DHT.MutableItemDistribution d) {
        logger.info("putMutableItem:{}", d.toString());
        DHTEngine.getInstance().distribute(d.getItem(), d.getCallback(), d.getCallbackData());
    }

    /**
     * 将所有的请求一次发给中间层
     */
    private void tryToSendAllRequest() {
        int size = DHTEngine.getInstance().queueOccupation();

        // 0.2 * 10000是中间层剩余空间，大于本地队列最大长度1000，目前肯定能放下
        if ((double)size / DHTEngine.DHTQueueCapability < THRESHOLD) {
            Set<Object> all = new LinkedHashSet<>(this.queue);
            this.queue.removeAll(all);
            for (Object request: all) {
                if (null != request) {
                    process(request);
                }
            }
        }
    }

    /**
     * 更新发给朋友的消息的时间和root等信息
     * @param friend friend to send msg
     * @param message msg
     */
    private void updateMessageInfoToFriend(byte[] friend, Message message) throws DBException {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);
        logger.info("Update friend [{}] info.", key.toString());
        this.timeStampToFriend.put(key, message.getTimestamp());
        this.rootToFriend.put(key, message.getHash());
//        this.confirmationRootToFriend.put(key, message.getFriendLatestMessageRoot());

        this.messageDB.setMessageToFriendRoot(friend, message.getHash());
    }

    /**
     * save message data in database
     * @param message msg
     * @param list msg data
     * @throws DBException database exception
     */
    private void saveMessageDataInDB(Message message, List<byte[]> list) throws DBException {
        if (null != message) {
            this.messageDB.putMessage(message.getHash(), message.getEncoded());
        }

        if (null != list) {
            for (byte[] date: list) {
                this.messageDB.putMessage(HashUtil.bencodeHash(date), date);
            }
        }
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @param data 新消息的其它相关数据，比如可能有多级文字或者图片结构，这些数据会一起发布到dht
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message, List<byte[]> data) {
        if (this.queue.size() <= QueueCapability) {
            try {
                if (null != message) {
                    logger.debug("Publish message:{}", message.toString());

                    saveMessageDataInDB(message, data);
                    updateMessageInfoToFriend(friend, message);
                    publishMessage(message);
                    publishImmutableData(data);
                    publishLastMessage(friend);
                    publishGossipInfo();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取聊天发送频道salt
     * @param friend 对方public key
     * @return salt
     */
    public byte[] getSendingSalt(byte[] friend) {
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(pubKey, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(friend, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 获取聊天接收频道salt
     * @param friend 对方public key
     * @return salt
     */
    public byte[] getReceivingSalt(byte[] friend) {
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 发布上次发送的消息到专用聊天频道
     * @param friend 当前聊天的朋友
     */
    public void publishLastMessage(byte[] friend) throws DBException {
        logger.debug("TAU messaging publish last message, friend:{}", Hex.toHexString(friend));
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
        byte[] root = this.rootToFriend.get(new ByteArrayWrapper(friend));

        if (null != root) {
            byte[] encode = this.messageDB.getMessageByHash(root);
            if (null != encode) {
                String hash = Hex.toHexString(HashUtil.bencodeHash(encode));

                byte[] salt = getSendingSalt(friend);
                logger.info("TAU messaging, message hash:{}, sending salt:{}", hash, Hex.toHexString(salt));
                DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                        keyPair.second, encode, salt);
                DHT.MutableItemDistribution mutableItemDistribution = new DHT.MutableItemDistribution(mutableItem, null, null);
                this.queue.add(mutableItemDistribution);
            }

            this.writingFriends.add(new ByteArrayWrapper(friend));
        }
    }

    /**
     * 正在写给某个朋友
     * @param friend 正在写给的朋友
     */
    public void writingToFriend(byte[] friend) {
        // 有信息要发，更新时间戳
        ByteArrayWrapper peer = new ByteArrayWrapper(friend);
        long currentTime = System.currentTimeMillis() / 1000;
        this.timeStampToFriend.put(peer, BigInteger.valueOf(currentTime));
        this.writingFriends.add(peer);

        publishGossipInfo();
    }

    /**
     * 调整间隔时间
     */
    private void adjustIntervalTime() {
        int size = DHTEngine.getInstance().queueOccupation();
        // 理想状态是直接问中间层是否资源紧张，根据资源紧张度来调整访问频率，资源紧张则降低访问频率
        if ((double)size / DHTEngine.DHTQueueCapability > THRESHOLD) {
            increaseIntervalTime();
        }
    }

    /**
     * 增加间隔时间
     */
    public void increaseIntervalTime() {
        this.loopIntervalTime = this.loopIntervalTime * 2;
    }

    /**
     * 减少间隔时间
     */
    public void decreaseIntervalTime() {
        if (this.loopIntervalTime > this.MIN_LOOP_INTERVAL_TIME) {
            this.loopIntervalTime = this.loopIntervalTime / 2;
        }
    }


    /**
     * 添加新朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void addNewFriend(byte[] pubKey) throws DBException {
        byte[] myPubKey = AccountManager.getInstance().getKeyPair().first;

        // 朋友列表排除自己
        if (!Arrays.equals(myPubKey, pubKey)) {
            this.friends.add(new ByteArrayWrapper(pubKey));

            this.messageDB.addFriend(pubKey);
        }
    }

    /**
     * 删除朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void delFriend(byte[] pubKey) throws DBException {
        ByteArrayWrapper key = new ByteArrayWrapper(pubKey);
        this.friends.remove(key);
        this.friendTimeStamp.remove(key);
        this.friendRoot.remove(key);
        this.friendConfirmationRoot.remove(key);
        this.friendConfirmationRootToNotify.remove(key);
        this.timeStampToFriend.remove(key);
        this.rootToFriend.remove(key);
//        this.confirmationRootToFriend.remove(key);
        this.demandImmutableDataHash.remove(key);

        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipItem.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            if (ByteUtil.startsWith(pubKey, entry.getKey().getReceiver())) {
                it.remove();
            }
        }

        this.messageDB.delFriend(pubKey);
    }

    /**
     * 获取所有的朋友
     * @return 所有朋友的公钥
     */
    public List<byte[]> getAllFriends() {
        List<byte[]> list = new ArrayList<>();
        for (ByteArrayWrapper friend: this.friends) {
            list.add(friend.getData());
        }

        return list;
    }

    /**
     * 获取队列容量
     * @return 容量
     */
    public int getQueueCapability() {
        return QueueCapability;
    }

    /**
     * 获取队列当前大小
     * @return 队列大小
     */
    public int getQueueSize() {
        return this.queue.size();
    }

    /**
     * get my latest msg root
     * @param pubKey public key
     * @return root
     */
    public byte[] getMyLatestMsgRoot(byte[] pubKey) {
        return this.rootToFriend.get(new ByteArrayWrapper(pubKey));
    }

    /**
     * 获取朋友最新的root
     * @param pubKey public key
     * @return root
     */
    public byte[] getFriendLatestRoot(byte[] pubKey) {
        return this.friendRoot.get(new ByteArrayWrapper(pubKey));
    }

    /**
     * 获取朋友最新的confirmation root
     * @param pubKey public key
     * @return confirmation root
     */
    public byte[] getFriendConfirmationRoot(byte[] pubKey) {
        return this.friendConfirmationRoot.get(new ByteArrayWrapper(pubKey));
    }

    /**
     * 设置gossip时间片
     * @param timeInterval 时间间隔，单位:s
     */
    public void setGossipTimeInterval(long timeInterval) {
        this.gossipPublishIntervalTime = timeInterval;
    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

        AccountManager.getInstance().addListener(this);

        if (!init()) {
            return false;
        }

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

    @Override
    public void onKeyChanged(Pair<byte[], byte[]> newKey) {
        this.friendLastSeen.clear();
        this.friendTimeStamp.clear();
        this.friendRoot.clear();
        this.friendConfirmationRoot.clear();
        this.timeStampToFriend.clear();
        this.rootToFriend.clear();
    }

    /**
     * 预处理受到的gossip
     * @param gossip 收到的gossip
     * @param peer gossip发出的peer
     */
    private void preprocessGossipFromNet(Gossip gossip, ByteArrayWrapper peer) {

        // 是否更新好友的在线时间（完整公钥）
        BigInteger gossipTime = gossip.getTimestamp();
        BigInteger lastSeen = this.friendLastSeen.get(peer);
        if (null == lastSeen || lastSeen.compareTo(gossipTime) < 0) { // 判断是否是更新的gossip
            this.friendLastSeen.put(peer, gossipTime);
            this.onlineFriendsToNotify.add(peer);

            if (null != gossip.getGossipList()) {

                // 信任发送方自己给的gossip信息
                byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                for (GossipItem gossipItem : gossip.getGossipList()) {
                    logger.trace("Got gossip: {} from peer[{}]", gossipItem.toString(), peer.toString());

                    ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                    // 发送者是我自己的gossip信息直接忽略，因为我自己的信息不需要依赖gossip
                    if (ByteUtil.startsWith(pubKey, gossipItem.getSender())) {
                        logger.trace("Sender[{}] is me.", sender.toString());
                        continue;
                    }

                    // 如果是对方直接给我的gossip信息，也即gossip item的sender与请求gossip channel的peer一样，
                    // 并且gossip item的receiver是我，那么会直接信任该gossip消息
                    if (ByteUtil.startsWith(peer.getData(), gossipItem.getSender())
                            && ByteUtil.startsWith(pubKey, gossipItem.getReceiver())) {
                        logger.trace("Got trusted gossip:{} from online friend[{}]",
                                gossipItem.toString(), peer.toString());

                        BigInteger currentTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
                        BigInteger timeStamp = gossipItem.getTimestamp();
                        BigInteger oldTimeStamp = this.friendTimeStamp.get(peer);

                        // 如果是对方直接给我发的gossip消息，并且时间戳更新一些
                        if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                            logger.debug("Got a new gossip item:{}", gossipItem.toString());

                            if (null != gossipItem.getDemandImmutableDataHash()) {
                                this.friendDemandHash.add(new ByteArrayWrapper(gossipItem.getDemandImmutableDataHash()));
                            }

                            if (GossipStatus.ON_WRITING == gossipItem.getGossipStatus()) {
                                logger.debug("Got a writing peer:{}", peer.toString());
                                this.writingFriendsToVisit.put(peer, System.currentTimeMillis() / 1000);
                            }

                            // 更新时间戳
                            this.friendTimeStamp.put(peer, timeStamp);

                            // 只要发现更新的gossip信息，则使用其confirmation root
                            if (null != gossipItem.getConfirmationRoot()) {
                                logger.debug("Got a friend[{}] confirmation root[{}]",
                                        peer.toString(), Hex.toHexString(gossipItem.getConfirmationRoot()));

                                byte[] confirmationRoot = this.friendConfirmationRoot.get(peer);
                                if (!Arrays.equals(confirmationRoot, gossipItem.getConfirmationRoot())) {
                                    // 有信息要发，即确认信息要发，更新时间戳，确认收到该root

                                    this.timeStampToFriend.put(peer, currentTime);
                                    // 更新confirmation root
                                    this.friendConfirmationRoot.put(peer, gossipItem.getConfirmationRoot());
                                    // 加入待通知列表
                                    this.friendConfirmationRootToNotify.put(peer, gossipItem.getConfirmationRoot());
                                }
                            }

                            // 发现更新时间戳的gossip消息，并且root与之前的不同，才更新并请求新数据
                            byte[] currentRoot = this.friendRoot.get(peer);
                            if (!Arrays.equals(currentRoot, gossipItem.getMessageRoot()) && null != gossipItem.getMessageRoot()) {
                                logger.debug("Got a new message root:{}", Hex.toHexString(gossipItem.getMessageRoot()));
                                // 如果该消息的root之前没拿到过，说明是新消息过来
                                // 更新朋友root信息
                                this.friendRoot.put(peer, gossipItem.getMessageRoot());
                                // 请求该root
                                requestMessage(gossipItem.getMessageRoot(), peer);
                            }
                        }

                        continue;
                    }

                    // 剩余的留给主循环处理
                    this.gossipItems.add(gossipItem);
                }
            }
        }
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case MESSAGE: {
                if (null == item) {
                    logger.debug("MESSAGE[{}] from peer[{}] is empty",
                            dataIdentifier.getExtraInfo2().toString(), dataIdentifier.getExtraInfo1().toString());
                    this.demandImmutableDataHash.put(dataIdentifier.getExtraInfo1(), dataIdentifier.getExtraInfo2().getData());
                    return;
                }

                Message message = new Message(item);
                logger.debug("MESSAGE: Got message :{}", message.toString());
                this.messageMap.put(new ByteArrayWrapper(message.getHash()), message);
                this.messageSenderMap.put(new ByteArrayWrapper(message.getHash()), dataIdentifier.getExtraInfo1());

                break;
            }
            case GOSSIP_FROM_PEER: {
                if (null == item) {
                    logger.debug("GOSSIP_FROM_PEER from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                Gossip gossip = new Gossip(item);
                preprocessGossipFromNet(gossip, dataIdentifier.getExtraInfo1());

                break;
            }
            case LATEST_MESSAGE: {
                if (null == item) {
                    logger.debug("LATEST MESSAGE : from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                Message message = new Message(item);
                logger.debug("LATEST MESSAGE : Got message:{}", message.toString());
                this.messageMap.put(new ByteArrayWrapper(message.getHash()), message);
                this.messageSenderMap.put(new ByteArrayWrapper(message.getHash()), dataIdentifier.getExtraInfo1());

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }

    @Override
    public void onDHTItemPut(int success, Object cbData) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case PUT_IMMUTABLE_DATA: {
                if (success > 0) {
                    logger.debug("Msg status:{}, {}", dataIdentifier.getExtraInfo1().toString(), MsgStatus.PUT_SUCCESS);
                    this.msgStatus.put(dataIdentifier.getExtraInfo1(), MsgStatus.PUT_SUCCESS);
                } else {
                    logger.debug("Msg status:{}, {}", dataIdentifier.getExtraInfo1().toString(), MsgStatus.PUT_FAIL);
                    this.msgStatus.put(dataIdentifier.getExtraInfo1(), MsgStatus.PUT_FAIL);
                }

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }
}
