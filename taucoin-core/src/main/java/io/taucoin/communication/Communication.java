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
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.listener.MsgStatus;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Gossip;
import io.taucoin.types.GossipItem;
import io.taucoin.types.GossipMutableData;
import io.taucoin.types.GossipStatus;
import io.taucoin.types.HashList;
import io.taucoin.types.IndexMutableData;
import io.taucoin.types.Message;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

import static io.taucoin.param.ChainParam.SHORT_ADDRESS_LENGTH;

public class Communication implements DHT.GetDHTItemCallback, DHT.PutDHTItemCallback, KeyChangedListener {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 对UI使用的, Queue capability.
    public static final int QueueCapability = 1000;

    // 判断中间层队列的门槛值，中间层队列使用率超过0.8，则增加主循环时间间隔
    private final double THRESHOLD = 0.8;

    // 2 min
    private static final int TWO_MINUTE = 120; // 120s

    // 主循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

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

    private byte[] deviceID = new byte[0];

    // UI相关请求存放的queue，统一收发所有的请求，包括UI以及内部算法产生的请求，LinkedHashSet确保队列的顺序性与唯一性
    private final Set<Object> queue = Collections.synchronizedSet(new LinkedHashSet<>());

    // 当前我加的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> friends = new CopyOnWriteArraySet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> friendLastSeen = new ConcurrentHashMap<>();

    // 当前发现的等待通知的在线的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> onlineFriendsToNotify = new CopyOnWriteArraySet<>();

    // 发现的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    private final Set<GossipItem> gossipItems = new CopyOnWriteArraySet<>();

    // 等待发送出去的gossip item集合，CopyOnWriteArraySet是支持并发操作的集合
    private final Set<GossipItem> gossipItemsToPut = new LinkedHashSet<>();

    // 得到的消息集合（hash <--> Message），ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, Message> messageMap = new ConcurrentHashMap<>();

    // 得到的消息与发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = new ConcurrentHashMap<>();

    // 得到的消息集合（hash <--> Latest Message List），最新的消息放在最后，ConcurrentHashMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, LinkedList<Message>> messageListMap = new ConcurrentHashMap<>();

    // 我的朋友的最新消息的哈希集合 <friend, IndexMutableData>（完整公钥）
    private final Map<ByteArrayWrapper, IndexMutableData> friendIndexData = new ConcurrentHashMap<>();

    // 新发现的，等待通知UI的，我的朋友的IndexMutableData <friend, IndexMutableData>（完整公钥）
    private final Map<ByteArrayWrapper, IndexMutableData> friendIndexDataToNotify = new ConcurrentHashMap<>();

    // 等待通知的消息状态
    private final Map<ByteArrayWrapper, MsgStatus> msgStatus = new ConcurrentHashMap<>();

    // 给我的朋友的最新消息的时间戳 <friend, timestamp>（完整公钥）
    private final Map<ByteArrayWrapper, BigInteger> timeStampToFriend = new ConcurrentHashMap<>();

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
     * 初始化，获取朋友列表以及最新消息
     * @return true if success, false otherwise
     */
    private boolean init() {
        try {
            // get friends
            Set<byte[]> friends = this.messageDB.getFriends();

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    logger.debug("My friend:{}", key.toString());

                    LinkedList<Message> linkedList = new LinkedList<>();
                    // 获取最新消息的编码
                    byte[] encode = this.messageDB.getLatestMessageHashListEncode(friend);
                    HashList hashList = new HashList(encode);
                    for (byte[] hash: hashList.getHashList()) {
                        logger.debug("Hash:{}", Hex.toHexString(hash));
                        byte[] msgEncode = this.messageDB.getMessageByHash(hash);
                        if (null != msgEncode) {
                            linkedList.add(new Message(msgEncode));
                        }
                    }
                    this.messageListMap.put(key, linkedList);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 保存与朋友的最近的聊天信息的哈希集合
     * @throws DBException database exception
     */
    private void saveMessageHashList() throws DBException {
        for (Map.Entry<ByteArrayWrapper, LinkedList<Message>> entry: this.messageListMap.entrySet()) {
            List<byte[]> list = new ArrayList<>();
            for (Message message: entry.getValue()) {
                list.add(message.getHash());
            }

            HashList hashList = new HashList(list);
            this.messageDB.saveLatestMessageHashListEncode(entry.getKey().getData(), hashList.getEncoded());
        }
    }

    /**
     * 尝试往聊天消息集合里面插入新消息
     * @param pubKey 聊天的peer
     * @param message 新消息
     */
    private void tryToUpdateLatestMessageList(ByteArrayWrapper pubKey, Message message) {
        LinkedList<Message> linkedList = this.messageListMap.get(pubKey);

        if (null != linkedList) {
            int size = linkedList.size();
            if (size > 0) {
                for (int i = size - 1; i > 0; i--) {
                    // 寻找第一个时间戳不大于当前消息的消息，将当前消息加到后面即可
                    if (message.getTimestamp().compareTo(linkedList.get(i).getTimestamp()) <= 0) {
                        linkedList.add(i + 1, message);

                        if (size >= ChainParam.MAX_HASH_NUMBER) {
                            linkedList.remove(0);
                        }

                        break;
                    }
                }
            } else {
                linkedList.add(message);
            }
        } else {
            linkedList = new LinkedList<>();
            linkedList.add(message);
        }

        this.messageListMap.put(pubKey, linkedList);
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
        Iterator<Map.Entry<ByteArrayWrapper, IndexMutableData>> iterator = this.friendIndexDataToNotify.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, IndexMutableData> entry = iterator.next();

            if (null != entry.getValue()) {
                for (byte[] hash: entry.getValue().getHashList()) {
                    logger.debug("Notify UI read message from friend:{}, root:{}",
                            entry.getKey().toString(), Hex.toHexString(hash));

                    this.msgListener.onReadMessageRoot(entry.getKey().getData(), hash);
                }
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

            // 更新最新消息列表
            tryToUpdateLatestMessageList(sender, message);

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
     * 回应远端的需求
     * @throws DBException database exception
     */
    private void responseRemoteDemand() throws DBException {
        Iterator<ByteArrayWrapper> it = this.friendDemandHash.iterator();
        while (it.hasNext()) {
            ByteArrayWrapper hash = it.next();
            logger.info("Response demand:{}", hash.toString());

            byte[] data = this.messageDB.getMessageByHash(hash.getData());
            if (null != data) {
                publishImmutableData(data);
            }

            this.friendDemandHash.remove(hash);
//            it.remove();
        }
    }

    /**
     * 尝试发布gossip信息，看看时间间隔是否已到，每10s一次
     */
    private void tryToPublishGossipInfo() {
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
            requestGossipMutableDataFromPeer(peer);

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
                    requestGossipMutableDataFromPeer(peer);
                }
            }
        }
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
     * 向某个peer请求gossip数据
     * @param pubKey public key
     */
    private void requestGossipMutableDataFromPeer(ByteArrayWrapper pubKey) {
        if (null != pubKey) {
            logger.trace("Request gossip mutable data from peer:{}", pubKey.toString());

            byte[] salt = ChainParam.GOSSIP_CHANNEL;
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), salt);
            DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER, pubKey);
            DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);

            this.queue.add(mutableItemRequest);
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
            DHT.MutableItemDistribution mutableItemDistribution = new DHT.MutableItemDistribution(mutableItem, null, null);

            this.queue.add(mutableItemDistribution);
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
            DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);

            this.queue.add(mutableItemRequest);
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

        byte[] encode = getLatestMessageHashListEncode(friend);

        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHT.MutableItemDistribution mutableItemDistribution = new DHT.MutableItemDistribution(mutableItem, null, null);
            this.queue.add(mutableItemDistribution);
        }
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
     * 主循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 通知UI消息状态
                notifyUIMessageStatus();

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

                // 尝试对gossip数据进行瘦身
//                tryToSlimDownGossip();

                // 尝试发布gossip数据
                tryToPublishGossipInfo();

                // 访问通过gossip机制推荐的活跃peer
                visitReferredFriends();

                // 尝试向中间层发送所有的请求
                tryToSendAllRequest();

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
        this.timeStampToFriend.put(key, message.getTimestamp());

        tryToUpdateLatestMessageList(key, message);
    }

    /**
     * 向朋友发布新消息
     * @param friend 朋友公钥
     * @param message 新消息
     * @return true:接受该消息， false:拒绝该消息
     */
    public boolean publishNewMessage(byte[] friend, Message message) {
        if (this.queue.size() <= QueueCapability) {
            try {
                if (null != message) {
                    logger.debug("Publish message:{}", message.toString());

                    saveMessageDataInDB(message);
                    updateMessageInfoToFriend(friend, message);
                    publishMessage(message);
                    publishIndexMutableData(friend);
                    publishGossip();
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
     * @return encode
     */
    private byte[] getLatestMessageHashListEncode(byte[] friend) {
        LinkedList<Message> linkedList = this.messageListMap.get(new ByteArrayWrapper(friend));
        if (null != linkedList && !linkedList.isEmpty()) {
            ArrayList<byte[]> list = new ArrayList<>();
            for (Message message: linkedList) {
                list.add(message.getHash());
            }

            HashList hashList = new HashList(list);

            return hashList.getEncoded();
        }

        return null;
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
                timeStamp = BigInteger.valueOf(currentTime);
            }

            GossipItem gossipItem = makeGossipItemWithShortAddress(pubKey, friend.getData(), timeStamp);
            gossipList.add(gossipItem);
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
     * 随机获取一个朋友
     * @return friend
     */
    private byte[] getFriendRandomly() {
        byte[] friend = null;
        Iterator<ByteArrayWrapper> it = this.friends.iterator();
        Random random = new Random(System.currentTimeMillis());
        int index = random.nextInt(this.friends.size()) + 1;

        int i = 0;
        while (it.hasNext() && i < index) {
            friend = it.next().getData();
            i++;
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
        while (iterator.hasNext() && i <= ChainParam.GOSSIP_LIMIT_SIZE) {
            GossipItem gossipItem = iterator.next();
            gossipItemList.add(gossipItem);

            i++;
        }

        if (!gossipItemList.isEmpty()) {

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
        }

        // 记录发布时间
        this.lastGossipPublishTime = System.currentTimeMillis() / 1000;
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
            ByteArrayWrapper peer = new ByteArrayWrapper(pubKey);
            this.friends.add(peer);

            this.messageDB.addFriend(pubKey);

            // TODO
            this.messageListMap.put(peer, new LinkedList<>());
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
        this.timeStampToFriend.remove(key);
        // TODO

        Iterator<Map.Entry<FriendPair, BigInteger>> it = this.friendGossipItem.entrySet().iterator();
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
        // TODO
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

                        continue;
                    }

                    // 剩余的留给主循环处理
                    this.gossipItems.add(gossipItem);
                }
            }
        }
    }

    /**
     * 比较集合数据，从中寻找新消息或者对方缺少的数据
     * @param peer 集合数据所属的peer
     * @param indexMutableData 集合数据
     */
    private void compareIndexDataSet(ByteArrayWrapper peer, IndexMutableData indexMutableData) {
        IndexMutableData currentIndexData = this.friendIndexData.get(peer);
        // 只处理发现的更新的数据
        if (null == currentIndexData ||
                currentIndexData.getTimestamp().compareTo(indexMutableData.getTimestamp()) < 0) {
            this.friendIndexData.put(peer, indexMutableData);
            this.friendIndexDataToNotify.put(peer, indexMutableData);

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
                    requestMessage(hash, peer);
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
                        this.friendDemandHash.add(new ByteArrayWrapper(message.getHash()));
                        // 满足一个需求即可
                        break;
                    }
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
