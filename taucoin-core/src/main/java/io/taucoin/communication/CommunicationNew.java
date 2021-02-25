package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
import io.taucoin.core.DataType;
import io.taucoin.core.FriendPair;
import io.taucoin.core.MutableDataWrapper;
import io.taucoin.core.OnlineSignal;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht2.DHT;
import io.taucoin.dht2.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipItem;
import io.taucoin.types.GossipMutableData;
import io.taucoin.types.HashList;
import io.taucoin.types.IndexMutableData;
import io.taucoin.types.Message;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

import static io.taucoin.param.ChainParam.SHORT_ADDRESS_LENGTH;

public class CommunicationNew implements DHT.GetMutableItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 主循环间隔最小时间
    private int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔最大时间
    private final int MAX_LOOP_INTERVAL_TIME = 1000; // 1000 ms

    // 主循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    // 发布gossip信息的时间间隔，默认10s
    private long gossipPublishIntervalTime = 10; // 10 s

    // 记录上一次发布gossip的时间
    private long lastGossipPublishTime = 0;

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    private final byte[] deviceID;

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 多设备发现的朋友
    private final Set<ByteArrayWrapper> friendsFromRemote = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendLastSeen = new ConcurrentHashMap<>();

    // 当前发现的等待通知的在线的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> onlineFriendsToNotify = new CopyOnWriteArraySet<>();

    // 当前发现的等待通知的device id集合（完整公钥）
    private final Set<ByteArrayWrapper> deviceIDToNotify = new CopyOnWriteArraySet<>();

    // 发现的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    private final Set<GossipItem> gossipItems = new CopyOnWriteArraySet<>();

    // 等待发送出去的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    // TODO:: 细化
    private final Set<GossipItem> gossipItemsToPut = new LinkedHashSet<>();

    // 得到的消息集合（hash <--> Message），ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, Message> messageMap = new ConcurrentHashMap<>();

    // 得到的消息与发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = new ConcurrentHashMap<>();

    // 得到的消息集合（friend pair（完整公钥） <--> Latest Message List），最新的消息放在最后，ConcurrentHashMap是支持并发操作的集合
    private final Map<FriendPair, LinkedList<Message>> messageListMap = new ConcurrentHashMap<>();

    // index mutable data更新了，需要重新发布数据的friend集合（完整公钥）
    private final Set<ByteArrayWrapper> freshFriends = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的哈希集合 <friend（包含多设备上的我）, IndexMutableData>（完整公钥）
    private final Map<ByteArrayWrapper, IndexMutableData> friendIndexData = new ConcurrentHashMap<>();

    // 新发现的，等待通知UI的，我的朋友的IndexMutableData <friend, IndexMutableData>（完整公钥）
    private final Map<ByteArrayWrapper, IndexMutableData> friendIndexDataToNotify = new ConcurrentHashMap<>();

    // 给我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    // TODO:: remove
    private final Map<ByteArrayWrapper, BigInteger> timeStampToFriend = new ConcurrentHashMap<>();

    // 我从朋友收到的需求hash，<hash, friend>（完整公钥）
    // TODO:: remove
    private final Map<ByteArrayWrapper, ByteArrayWrapper> friendDemandHash = new ConcurrentHashMap<>();

    // 发现的我的朋友跟我聊天的最新时间 <friend, time>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendChattingTime = new ConcurrentHashMap<>();

    // 通过gossip推荐机制发现的有新消息的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> referredFriends = new CopyOnWriteArraySet<>();

    // 通过gossip机制发现的给朋友的gossip time <FriendPair, Timestamp>（完整公钥）
    private final Map<FriendPair, BigInteger> friendGossipTime = new ConcurrentHashMap<>();

    // Communication thread.
    private Thread communicationThread;

    public CommunicationNew(byte[] deviceID, MessageDB messageDB, MsgListener msgListener) {
        this.deviceID = adjustDeviceID(deviceID);
        this.messageDB = messageDB;
        this.msgListener = msgListener;
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
     * 初始化，获取朋友列表以及最新消息
     * @return true if success, false otherwise
     */
    private boolean init() {
        try {
            // get friends
            Set<byte[]> friends = this.messageDB.getFriends();
            // 我的公钥
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    logger.debug("My friend:{}", key.toString());

                    // 获取我发给朋友的最近的消息
                    FriendPair friendPair1 = new FriendPair(pubKey, friend);
                    LinkedList<Message> linkedList1 = new LinkedList<>();
                    // 获取最新消息的编码
                    byte[] encode1 = this.messageDB.getLatestMessageHashListEncode(friendPair1);
                    if (null != encode1) {
                        HashList hashList = new HashList(encode1);
                        for (byte[] hash : hashList.getHashList()) {
                            logger.debug("Hash:{}", Hex.toHexString(hash));
                            byte[] msgEncode = this.messageDB.getMessageByHash(hash);
                            if (null != msgEncode) {
                                linkedList1.add(new Message(msgEncode));
                            }
                        }
                    }

                    this.messageListMap.put(friendPair1, linkedList1);

                    // 获取朋友发给我的最近的消息
                    FriendPair friendPair2 = new FriendPair(pubKey, friend);
                    LinkedList<Message> linkedList2 = new LinkedList<>();
                    // 获取最新消息的编码
                    byte[] encode2 = this.messageDB.getLatestMessageHashListEncode(friendPair2);
                    if (null != encode2) {
                        HashList hashList = new HashList(encode2);
                        for (byte[] hash : hashList.getHashList()) {
                            logger.debug("Hash:{}", Hex.toHexString(hash));
                            byte[] msgEncode = this.messageDB.getMessageByHash(hash);
                            if (null != msgEncode) {
                                linkedList2.add(new Message(msgEncode));
                            }
                        }
                    }

                    this.messageListMap.put(friendPair2, linkedList2);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 将Gossip Item对应的时间戳更新到最新，发出请求访问我信号
     * @param friend 我的朋友
     */
    private void touchGossipItem(ByteArrayWrapper friend) {
        this.timeStampToFriend.put(friend, BigInteger.valueOf(System.currentTimeMillis() / 1000));
    }

    /**
     * 保存朋友的最新消息哈希列表
     * @param friendPair friend pair
     * @throws DBException database exception
     */
    private void saveFriendLatestMessageHashList(FriendPair friendPair) throws DBException {
        LinkedList<Message> linkedList = this.messageListMap.get(friendPair);

        if (null != linkedList && !linkedList.isEmpty()) {
            List<byte[]> list = new ArrayList<>();
            for (Message message : linkedList) {
                try {
                    logger.debug("message hash:{}", Hex.toHexString(message.getHash()));
                    list.add(message.getHash());
                } catch (RuntimeException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            HashList hashList = new HashList(list);
            this.messageDB.saveLatestMessageHashListEncode(friendPair, hashList.getEncoded());
        }
    }

    /**
     * 尝试往聊天消息集合里面插入新消息
     * @param friendPair friend pair
     * @param message 新消息
     */
    private void tryToUpdateLatestMessageList(FriendPair friendPair, Message message) throws DBException {
        LinkedList<Message> linkedList = this.messageListMap.get(friendPair);

        // 更新成功标志
        boolean updated = false;

        if (null != linkedList) {
            int size = linkedList.size();
            if (size > 0) {
                try {
                    // 先判断一下是否比最后一个消息时间戳大，如果是，则直接插入末尾
                    if (message.getTimestamp().compareTo(linkedList.get(size - 1).getTimestamp()) > 0) {
                        linkedList.add(message);
                        updated = true;
                    } else {
                        // 寻找从后往前寻找第一个时间小于当前消息时间的消息，将当前消息插入到到该消息后面
                        for (int i = size - 1; i > 0; i--) {
                            // 比较当前位置消息与新消息的时间戳差值
                            int m = linkedList.get(i).getTimestamp().compareTo(message.getTimestamp());

                            // 如果差值小于零，说明找到了比当前消息时间戳小的消息位置，将消息插入到目标位置后面一位
                            if (m < 0) {
                                linkedList.add(i + 1, message);
                                updated = true;
                            } else if (0 == m && !Arrays.equals(linkedList.get(i).getHash(), message.getHash())) {
                                // 如果时间戳一样，并且是不同的消息，则认为后来的消息时间戳更大
                                linkedList.add(i + 1, message);
                                updated = true;
                            }

                            // 如果更新了消息列表，则判断是否列表长度过长，过长则删掉旧数据，然后停止循环
                            if (updated) {
                                if (size >= ChainParam.INDEX_HASH_LIMIT_SIZE) {
                                    linkedList.remove(0);
                                }

                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                linkedList.add(message);
                updated = true;
            }
        } else {
            linkedList = new LinkedList<>();
            linkedList.add(message);
            updated = true;
        }

        // 更新成功
        if (updated) {
            this.messageListMap.put(friendPair, linkedList);

            saveFriendLatestMessageHashList(friendPair);

            // 更新成功则记为fresh节点
            this.freshFriends.add(new ByteArrayWrapper(friendPair.getSender()));
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
     * 通知UI发现的新设备
     */
    private void notifyUINewDevice() {
        for (ByteArrayWrapper deviceID: this.deviceIDToNotify) {
            this.msgListener.onNewDeviceID(deviceID.getData());

            this.deviceIDToNotify.remove(deviceID);
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
        Iterator<Map.Entry<ByteArrayWrapper, IndexMutableData>> iterator = this.friendIndexDataToNotify.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, IndexMutableData> entry = iterator.next();

            try {
                if (null != entry.getValue()) {
                    for (byte[] hash : entry.getValue().getHashList()) {
                        logger.debug("Notify UI read message from friend:{}, root:{}",
                                entry.getKey().toString(), Hex.toHexString(hash));

                        this.msgListener.onReadMessageRoot(entry.getKey().getData(), hash);
                    }
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

            this.friendIndexDataToNotify.remove(entry.getKey());
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
     * 合并来自多设备的朋友列表
     * @throws DBException database exception
     */
    private void tryToMergeFriends() throws DBException {
        for (ByteArrayWrapper friend: this.friendsFromRemote) {
            addNewFriend(friend.getData());

            this.msgListener.onNewFriend(friend.getData());

            this.friendsFromRemote.remove(friend);
        }
    }

    /**
     * 处理收到的消息
     * @throws DBException database exception
     */
    private void dealWithMessage() throws DBException {

        // 将消息存入数据库并通知UI，尝试获取下一条消息
        Iterator<Map.Entry<ByteArrayWrapper, Message>> iterator = this.messageMap.entrySet().iterator();

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, Message> entry = iterator.next();
            Message message = entry.getValue();
            ByteArrayWrapper msgHash = entry.getKey();

            ByteArrayWrapper sender = this.messageSenderMap.get(msgHash);

            try {
                // save to db
                this.messageDB.putMessage(message.getHash(), message.getEncoded());

                logger.debug("Notify UI new message from friend:{}, hash:{}",
                        sender.toString(), Hex.toHexString(message.getHash()));

                // 通知UI新消息
                this.msgListener.onNewMessage(sender.getData(), message);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

            // 更新最新消息列表
            tryToUpdateLatestMessageList(new FriendPair(sender.getData(), pubKey), message);

            this.messageSenderMap.remove(msgHash);
            this.messageMap.remove(msgHash);
//            iterator.remove();
        }
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
            try {
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

                                BigInteger oldTimestamp = this.friendGossipTime.get(pair);

                                BigInteger timeStamp = gossipItem.getTimestamp();

                                // 判断是否是新数据，若是，则记录下来以待发布
                                if (null == oldTimestamp || oldTimestamp.compareTo(timeStamp) < 0) {
                                    this.friendGossipTime.put(pair, timeStamp);
                                }

                                break;
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }

            this.gossipItems.remove(gossipItem);
//            it.remove();
        }
    }

    /**
     * 回应远端的需求
     * @throws DBException database exception
     */
    private void responseRemoteDemand() throws DBException {
        for (Map.Entry<ByteArrayWrapper, ByteArrayWrapper> entry: this.friendDemandHash.entrySet()) {
            logger.info("Response demand:{}", entry.getKey().toString());

            byte[] data = this.messageDB.getMessageByHash(entry.getKey().getData());
            if (null != data) {
                publishImmutableData(data);

                touchGossipItem(entry.getValue());
            }

            this.friendDemandHash.remove(entry.getKey());
        }
    }

    /**
     * 看看是否有新信息需要向朋友发布，有则发布新的index数据
     */
    private void tryToPublishIndexMutableData() {
        // 如果有同步请求，则发布自己的index数据以便对方比对
        for (ByteArrayWrapper friend: this.freshFriends) {
            publishIndexMutableData(friend.getData());
            this.freshFriends.remove(friend);
        }
    }

    /**
     * 尝试发布gossip信息，看看时间间隔是否已到
     */
    private void tryToPublishGossipMutableData() {
        long currentTime = System.currentTimeMillis() / 1000;

        if (currentTime - lastGossipPublishTime > gossipPublishIntervalTime) {
            publishGossip();
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
            requestIndexMutableDataFromPeer(peer);
            requestGossipMutableDataFromPeer(peer);

            this.referredFriends.remove(peer);
//            it.remove();
        } else {
            // 没有找到推荐的活跃的peer，则自己随机访问一个自己的朋友或者自己
            Iterator<ByteArrayWrapper> iterator = this.friends.iterator();
            // TODO:: 同步自己消息的频率问题
            if (iterator.hasNext()) {

                Random random = new Random(System.currentTimeMillis());
                // 取值范围0 ~ size，当index取size时选自己
                int index = random.nextInt(this.friends.size() + 1);

                ByteArrayWrapper peer = null;
                if (index != this.friends.size()) {
                    // (0 ~ size)
                    int i = 0;
                    while (iterator.hasNext()) {
                        peer = iterator.next();
                        if (i == index) {
                            break;
                        }

                        i++;
                    }
                } else {
                    peer = new ByteArrayWrapper(AccountManager.getInstance().getKeyPair().first);
                }

                if (null != peer) {
                    requestIndexMutableDataFromPeer(peer);
                    requestGossipMutableDataFromPeer(peer);
                }
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

            DHTEngine.getInstance().distribute(immutableItem, null, null);
        }
    }

    /**
     * 发布消息
     * @param message msg to publish
     */
    private void publishMessage(byte[] friend, Message message) {
        if (null != message) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(message.getEncoded());

            DHTEngine.getInstance().distribute(immutableItem, null, null);
        }
    }

    /**
     * 向某个peer请求gossip数据
     * @param pubKey public key
     */
    private void requestGossipMutableDataFromPeer(ByteArrayWrapper pubKey) {
        if (null != pubKey) {
            logger.trace("Request gossip mutable data from peer:{}", pubKey.toString());

            byte[] salt = ChainParam.GOSSIP_CHANNEL;
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER, pubKey);

            DHTEngine.getInstance().request(spec, this, dataIdentifier);
        }
    }

    /**
     * 在gossip频道发布gossip信息
     * @param gossipMutableData gossip mutable data
     */
    private void publishGossipMutableData(GossipMutableData gossipMutableData) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = ChainParam.GOSSIP_CHANNEL;
        byte[] encode = gossipMutableData.getEncoded();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHTEngine.getInstance().distribute(mutableItem, null, null);
        }
    }

    /**
     * 向某个peer请求index数据
     * @param pubKey public key
     */
    private void requestIndexMutableDataFromPeer(ByteArrayWrapper pubKey) {
        if (null != pubKey) {
            logger.trace("Request index mutable data from peer:{}", pubKey.toString());

            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            byte[] salt = makeIndexChannelReceivingSalt(keyPair.first, pubKey.getData());
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.INDEX_FROM_PEER, pubKey);
            DHTEngine.getInstance().request(spec, this, dataIdentifier);
        }
    }

    /**
     * 发布index频道的数据
     * @param friend 当前聊天的朋友
     */
    private void publishIndexMutableData(byte[] friend) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
        byte[] salt = makeIndexChannelSendingSalt(keyPair.first, friend);

        List<byte[]> list = getLatestMessageHashList(friend);
        IndexMutableData indexMutableData = new IndexMutableData(this.deviceID, list);
        logger.debug("publishIndexMutableData:{}", indexMutableData.toString());

        byte[] encode = indexMutableData.getEncoded();

        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHTEngine.getInstance().distribute(mutableItem, null, null);
        }
    }

    /**
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 合并来自多设备的朋友
                tryToMergeFriends();

                // 通知UI新设备
                notifyUINewDevice();

                // 通知UI发现的在线朋友
                notifyUIOnlineFriend();

                // 通知UI已读root
                notifyUIReadMessageRoot();

                // 处理获得的消息
                dealWithMessage();

                // 处理获得的gossip
                dealWithGossipItem();

                // 相应远端的需求
                responseRemoteDemand();

                // 尝试发布gossip数据
                tryToPublishGossipMutableData();

                // 尝试发布index数据
                tryToPublishIndexMutableData();

                // 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                // 尝试调整间隔时间
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
     * 调整间隔时间
     */
    private void adjustIntervalTime() {
        int size = this.friends.size();
        if (size > 0) {
            // 主循环频率：1s / 在线朋友数；最小值50ms; 最大值 1s
            // 流量控制是控制主循环频率最小值
            this.loopIntervalTime = MAX_LOOP_INTERVAL_TIME / size;

            if (this.loopIntervalTime < MIN_LOOP_INTERVAL_TIME) {
                this.loopIntervalTime = MIN_LOOP_INTERVAL_TIME;
            }
        } else {
            this.loopIntervalTime = MAX_LOOP_INTERVAL_TIME;
        }
//        int size = DHTEngine.getInstance().queueOccupation();
//        // 理想状态是直接问中间层是否资源紧张，根据资源紧张度来调整访问频率，资源紧张则降低访问频率
//        if ((double)size / DHTEngine.DHTQueueCapability > THRESHOLD) {
//            increaseIntervalTime();
//        }
    }

    /**
     * 增加间隔时间
     */
    public void increaseIntervalTime() {
        this.loopIntervalTime = this.loopIntervalTime * 2;

        if (this.loopIntervalTime > this.MAX_LOOP_INTERVAL_TIME) {
            this.loopIntervalTime = MAX_LOOP_INTERVAL_TIME;
        }
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
     * 获取间隔时间
     * @return 间隔时间
     */
    public int getIntervalTime() {
        return this.loopIntervalTime;
    }

    /**
     * 设置间隔时间
     */
    public void setIntervalTime(int intervalTime) {
        if (intervalTime < MIN_LOOP_INTERVAL_TIME) {
            this.loopIntervalTime = MIN_LOOP_INTERVAL_TIME;
        } else if (intervalTime > MAX_LOOP_INTERVAL_TIME) {
            this.loopIntervalTime = MAX_LOOP_INTERVAL_TIME;
        } else {
            this.loopIntervalTime = intervalTime;
        }
    }

    /**
     * 设置最小间隔时间
     */
    public void setMinIntervalTime(int minIntervalTime) {
        this.MIN_LOOP_INTERVAL_TIME = Math.min(minIntervalTime, MAX_LOOP_INTERVAL_TIME);
    }

    /**
     * save message data in database
     * @param message msg
     * @throws DBException database exception
     */
    private void saveMessageDataInDB(Message message) throws DBException {
        if (null != message) {
            this.messageDB.putMessage(message.getHash(), message.getEncoded());
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
        touchGossipItem(key);

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        tryToUpdateLatestMessageList(new FriendPair(pubKey, friend), message);
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message) {
//        if (this.queue.size() <= QueueCapability) {
            try {
                if (null != message) {
                    logger.debug("Publish message:{}", message.toString());

                    saveMessageDataInDB(message);
                    updateMessageInfoToFriend(friend, message);
                    publishGossip();
                    publishIndexMutableData(friend);
                    publishMessage(friend, message);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            return true;
//        } else {
//            return false;
//        }
    }

    /**
     * 构造index数据频道发送侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeIndexChannelSendingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(pubKey, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(friend, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 构造index数据频道接收侧salt
     * @param pubKey my public key
     * @param friend friend public key
     * @return salt
     */
    private byte[] makeIndexChannelReceivingSalt(byte[] pubKey, byte[] friend) {
        byte[] salt = new byte[SHORT_ADDRESS_LENGTH * 2];
        System.arraycopy(friend, 0, salt, 0, SHORT_ADDRESS_LENGTH);
        System.arraycopy(pubKey, 0, salt, SHORT_ADDRESS_LENGTH, SHORT_ADDRESS_LENGTH);

        return salt;
    }

    /**
     * 获取最新聊天消息的哈希集合
     * @param friend friend
     * @return hash list
     */
    private ArrayList<byte[]> getLatestMessageHashList(byte[] friend) {
        ArrayList<byte[]> list = new ArrayList<>();

        LinkedList<Message> linkedList = this.messageListMap.get(new ByteArrayWrapper(friend));
        if (null != linkedList && !linkedList.isEmpty()) {

            for (Message message: linkedList) {
                list.add(message.getHash());
            }
        }

        return list;
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

            if (null == timeStamp) {
//                timeStamp = BigInteger.valueOf(currentTime);
                continue;
            }

            GossipItem gossipItem = makeGossipItemWithShortAddress(pubKey, friend.getData(), timeStamp);
            gossipList.add(gossipItem);
        }

        // 2.统计其他人发给我朋友的消息
        for (Map.Entry<FriendPair, BigInteger> entry : this.friendGossipTime.entrySet()) {
            BigInteger timestamp = entry.getValue();
            // 只添加一天以内有新消息的
            if (currentTime - timestamp.longValue() < ChainParam.ONE_DAY) {
                gossipList.add(makeGossipItemWithShortAddress(entry.getKey(), timestamp));
            }
        }

        return gossipList;
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
     * 发布gossip，采用轮转发送策略，以便让各个friend相关的gossip都有机会得到发送
     */
    private void publishGossip() {
        if (this.gossipItemsToPut.isEmpty()) {
            LinkedHashSet<GossipItem> gossipItemSet = getGossipList();
            for(GossipItem gossipItem: gossipItemSet) {
                logger.trace("Gossip set:{}", gossipItem.toString());
            }

            this.gossipItemsToPut.addAll(gossipItemSet);
        }

        List<GossipItem> gossipItemList = new ArrayList<>();

        Iterator<GossipItem> iterator = this.gossipItemsToPut.iterator();
        int i = 0;
        while (iterator.hasNext() && i <= ChainParam.GOSSIP_ITEM_LIMIT_SIZE) {
            GossipItem gossipItem = iterator.next();
            gossipItemList.add(gossipItem);

            i++;
        }

        List<byte[]> friendList = new ArrayList<>();
        byte[] friend = getFriendRandomly();
        if (null != friend) {
            friendList.add(friend);
        }

        GossipMutableData gossipMutableData = new GossipMutableData(this.deviceID, friendList, gossipItemList);
        while (gossipMutableData.getEncoded().length >= ChainParam.DHT_ITEM_LIMIT_SIZE) {
            gossipItemList.remove(gossipItemList.size() - 1);
            gossipMutableData = new GossipMutableData(this.deviceID, friendList, gossipItemList);
        }

        for (GossipItem gossipItem: gossipItemList) {
            this.gossipItemsToPut.remove(gossipItem);
        }

        logger.debug(gossipMutableData.toString());
        publishGossipMutableData(gossipMutableData);

        // 记录发布时间
        this.lastGossipPublishTime = System.currentTimeMillis() / 1000;
    }

    /**
     * 添加新朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void addNewFriend(byte[] pubKey) throws DBException {
        ByteArrayWrapper peer = new ByteArrayWrapper(pubKey);
        // 没有才添加
        if (!this.friends.contains(peer)) {
            byte[] myPubKey = AccountManager.getInstance().getKeyPair().first;

            // 朋友列表排除自己
            if (!Arrays.equals(myPubKey, pubKey)) {
                this.friends.add(peer);
                this.messageListMap.put(new FriendPair(myPubKey, pubKey), new LinkedList<>());
                this.messageListMap.put(new FriendPair(pubKey, myPubKey), new LinkedList<>());

                this.messageDB.addFriend(pubKey);
            }
        }
    }

    /**
     * 删除朋友
     * @param pubKey public key
     * @throws DBException database exception
     */
    public void delFriend(byte[] pubKey) throws DBException {
        this.messageDB.delFriend(pubKey);

        ByteArrayWrapper key = new ByteArrayWrapper(pubKey);
        this.friends.remove(key);
        this.friendIndexData.remove(key);
        this.timeStampToFriend.remove(key);

        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FriendPair, BigInteger> entry = it.next();
            if (ByteUtil.startsWith(pubKey, entry.getKey().getReceiver())) {
                it.remove();
            }
        }
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
        init();
        this.friendLastSeen.clear();
        this.timeStampToFriend.clear();
        this.friendIndexData.clear();
    }

    /**
     * 预处理受到的gossip
     * @param gossipMutableData 收到的gossip mutable data
     * @param peer gossip发出的peer
     */
    private void preprocessGossipFromNet(GossipMutableData gossipMutableData, ByteArrayWrapper peer) {

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        // 如果是我自己频道发出的，判断是否属于另一个设备
        if (Arrays.equals(pubKey, peer.getData())) {
            if (!Arrays.equals(this.deviceID, gossipMutableData.getDeviceID())) { // 来自多设备
                this.deviceIDToNotify.add(new ByteArrayWrapper(gossipMutableData.getDeviceID()));
                List<byte[]> friendList = gossipMutableData.getFriendList();
                if (null != friendList) {
                    for (byte[] friend: friendList) {
                        this.friendsFromRemote.add(new ByteArrayWrapper(friend));
                    }
                }
            }
            // 本设备发的数据，只同步朋友列表即可
            return;
        }

        // 是否更新好友的在线时间（完整公钥）
        BigInteger gossipTime = gossipMutableData.getTimestamp();
        BigInteger lastSeen = this.friendLastSeen.get(peer);

        if (null != lastSeen && lastSeen.compareTo(gossipTime) > 0) {
            logger.debug("-----old gossip mutable data from peer:{}", peer.toString());
        }

        // 判断时间戳，以避免处理历史数据
        if (null == lastSeen || lastSeen.compareTo(gossipTime) < 0) { // 判断是否是更新的gossip
            logger.debug("See peer:{} again", peer.toString());
            this.friendLastSeen.put(peer, gossipTime);
            this.onlineFriendsToNotify.add(peer);

            if (null != gossipMutableData.getGossipItemList()) {

                // 信任发送方自己给的gossip信息
                for (GossipItem gossipItem : gossipMutableData.getGossipItemList()) {
                    logger.trace("Got gossip: {} from peer[{}]", gossipItem.toString(), peer.toString());

                    ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                    // 发送者是我自己的gossip信息直接忽略，因为我自己的信息不需要依赖gossip
                    // XX类别是自己
                    if (ByteUtil.startsWith(pubKey, gossipItem.getSender())) {
                        logger.trace("Sender[{}] is me.", sender.toString());
                        continue;
                    }

                    // 如果是对方直接给我的gossip信息，也即gossip item的sender与请求gossip channel的peer一样，
                    // 并且gossip item的receiver是我，那么会直接信任该gossip消息
                    // YX类别是给我的
                    if (ByteUtil.startsWith(peer.getData(), gossipItem.getSender())
                            && ByteUtil.startsWith(pubKey, gossipItem.getReceiver())) {
                        logger.trace("Got trusted gossip:{} from online friend[{}]",
                                gossipItem.toString(), peer.toString());
                        // 有值得信赖的gossip推荐我访问他，则将其加入推荐列表
                        this.referredFriends.add(peer);

                        continue;
                    }

                    // 剩余的留给主循环处理
                    // 剩下的YZ类别是别人给我朋友的
                    this.gossipItems.add(gossipItem);
                }
            }
        }
    }

    /**
     * 处理收到的mutable data
     * @param mutableDataWrapper 收到的mutable data
     * @param peer gossip发出的peer
     */
    private void processMutableData(MutableDataWrapper mutableDataWrapper, ByteArrayWrapper peer) {

        // 是否更新好友的在线时间（完整公钥）
        BigInteger timestamp = mutableDataWrapper.getTimestamp();
        BigInteger lastSeen = this.friendLastSeen.get(peer);

        if (null != lastSeen && lastSeen.compareTo(timestamp) > 0) {
            logger.debug("-----old online signal from peer:{}", peer.toString());
        }

        // 判断时间戳，以避免处理历史数据
        if (null == lastSeen || lastSeen.compareTo(timestamp) < 0) { // 判断是否是更新的online signal
            logger.debug("See peer:{} again", peer.toString());
            this.friendLastSeen.put(peer, timestamp);
            this.onlineFriendsToNotify.add(peer);

            switch (mutableDataWrapper.getMutableDataType()) {
                case MESSAGE: {
                    Message message = new Message(mutableDataWrapper.getData());
                    logger.debug("MESSAGE: Got message :{}", message.toString());
                    this.messageMap.put(new ByteArrayWrapper(message.getHash()), message);
                    this.messageSenderMap.put(new ByteArrayWrapper(message.getHash()), peer);

                    break;
                }
                case ONLINE_SIGNAL: {
                    OnlineSignal onlineSignal = new OnlineSignal(mutableDataWrapper.getData());

                    byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                    byte[] chattingFriend = onlineSignal.getChattingFriend();
                    if (Arrays.equals(pubKey, chattingFriend)) {
                        // 如果是正在跟我聊天，判断一下上次标记聊天时间戳是否最新
                        ByteArrayWrapper sender = new ByteArrayWrapper(chattingFriend);
                        BigInteger latestTimestamp = this.friendChattingTime.get(sender);
                        // 如果发现更新的推荐，则加入推荐列表
                        if (null == latestTimestamp || latestTimestamp.compareTo(onlineSignal.getChattingTime()) < 0) {
                            // 记录最新的聊天时间
                            this.friendChattingTime.put(sender, onlineSignal.getChattingTime());
                            this.referredFriends.add(sender);
                        }
                    } else {
                        // 记录下推荐给别人的聊天时间
                        FriendPair friendPair = new FriendPair(peer.getData(), chattingFriend);
                        BigInteger latestTimestamp = this.friendGossipTime.get(friendPair);
                        if (null == latestTimestamp || latestTimestamp.compareTo(onlineSignal.getChattingTime()) < 0) {
                            this.friendGossipTime.put(friendPair, onlineSignal.getChattingTime());
                        }
                    }


                    if (null != onlineSignal.getGossipItemList()) {

                        // 信任发送方自己给的gossip信息
                        for (GossipItem gossipItem : onlineSignal.getGossipItemList()) {
                            logger.trace("Got gossip: {} from peer[{}]", gossipItem.toString(), peer.toString());

                            ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                            // 发送者是我自己的gossip信息直接忽略，因为我自己的信息不需要依赖gossip
                            if (ByteUtil.startsWith(pubKey, gossipItem.getSender())) {
                                logger.trace("Sender[{}] is me.", sender.toString());
                                continue;
                            }

                            BigInteger latestTimestamp = this.friendChattingTime.get(sender);
                            // 如果发现更新的推荐，则加入推荐列表
                            if (null == latestTimestamp || latestTimestamp.compareTo(gossipItem.getTimestamp()) < 0) {
                                // 记录最新的聊天时间
                                this.friendChattingTime.put(sender, gossipItem.getTimestamp());
                                this.referredFriends.add(sender);
                            }
                        }
                    }

                    break;
                }
                case UNKNOWN: {
                    logger.error("Unknown type.");
                }
            }
        }
    }

    /**
     * 比较对方集合数据，从中寻找新消息或者对方缺少的数据
     * @param peer 集合数据所属的peer
     * @param indexMutableData 集合数据
     */
    private void compareIndexDataSet(ByteArrayWrapper peer, IndexMutableData indexMutableData) {
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        // 排除自己的数据
        if (Arrays.equals(pubKey, peer.getData()) &&
                Arrays.equals(this.deviceID, indexMutableData.getDeviceID())) {
            return;
        }

        IndexMutableData currentIndexData = this.friendIndexData.get(peer);
        // 判断时间戳，以避免处理历史数据，只处理保存和通知更新的数据
        if (null == currentIndexData ||
                currentIndexData.getTimestamp().compareTo(indexMutableData.getTimestamp()) < 0) {
            this.friendIndexData.put(peer, indexMutableData);
            this.friendIndexDataToNotify.put(peer, indexMutableData);
        }

        // 判断时间戳，以避免处理历史数据，可以多次最新的数据，以避免上次dht get失败的情况
        if (null == currentIndexData ||
                currentIndexData.getTimestamp().compareTo(indexMutableData.getTimestamp()) <= 0) {

            // 尝试发现对方新消息
            for (byte[] hash: indexMutableData.getHashList()) {
                boolean found = false;
                LinkedList<Message> messageLinkedList = this.messageListMap.get(peer);
                if (null != messageLinkedList) {
                    for (Message message : messageLinkedList) {
                        if (Arrays.equals(hash, message.getHash())) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) { // 找到新消息的哈希，则请求该消息
                    // 发现新的同步需求，更新时间戳，发出信号
                    logger.debug("Found new hash:{} from peer:{}", Hex.toHexString(hash), peer.toString());
                    touchGossipItem(peer);
//                    requestMessage(hash, peer);
                }
            }

            // 尝试满足对方需求，挑选一个满足即可
            LinkedList<Message> messageLinkedList = this.messageListMap.get(peer);
            if (null != messageLinkedList) {
                for (Message message : messageLinkedList) {
                    boolean found = false;
                    for (byte[] hash: indexMutableData.getHashList()) {
                        if (Arrays.equals(hash, message.getHash())) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {// 找到对方缺少的消息的哈希，则加入需求集合
                        ByteArrayWrapper hash = new ByteArrayWrapper(message.getHash());
                        logger.debug("Found demand hash:{} from peer:{}", hash.toString(), peer.toString());
                        this.friendDemandHash.put(hash, peer);
                        // 满足一个需求即可
                        break;
                    }
                }
            }

        } else {
            logger.debug("-----old index mutable data from peer:{}", peer.toString());
        }
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData, boolean auth) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case MULTIPLEX_DATA: {
                if (null == item) {
                    logger.debug("MULTIPLEX_DATA from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                MutableDataWrapper mutableDataWrapper = new MutableDataWrapper(item);
                processMutableData(mutableDataWrapper, dataIdentifier.getExtraInfo1());

                break;
            }
            case GOSSIP_FROM_PEER: {
                if (null == item) {
                    logger.debug("GOSSIP_FROM_PEER from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                GossipMutableData gossipMutableData = new GossipMutableData(item);
                preprocessGossipFromNet(gossipMutableData, dataIdentifier.getExtraInfo1());

                break;
            }
            case INDEX_FROM_PEER: {
                if (null == item) {
                    logger.debug("INDEX_FROM_PEER from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                ByteArrayWrapper peer = dataIdentifier.getExtraInfo1();
                IndexMutableData indexMutableData = new IndexMutableData(item);

                compareIndexDataSet(peer, indexMutableData);

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }

}
