package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.taucoin.account.AccountManager;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.DataType;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipItem;
import io.taucoin.types.GossipList;
import io.taucoin.types.GossipType;
import io.taucoin.types.Message;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

public class Communication implements DHT.GetDHTItemCallback {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 对UI使用的, Queue capability.
    public static final int QueueCapability = 1000;

    // 判断中间层队列的门槛值，中间层队列使用率超过0.8，则增加主循环时间间隔
    private final double THRESHOLD = 0.8;

    // 主循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 主循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // UI相关请求存放的queue，统一收发所有的请求，包括UI以及内部算法产生的请求， ConcurrentLinkedQueue是一个支持并发操作的队列
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();

    // 发现的gossip item集合，synchronizedSet是支持并发操作的集合
    private final Set<GossipItem> gossipItems = Collections.synchronizedSet(new HashSet<>());

    // 得到的消息集合（hash <--> Message），synchronizedMap是支持并发操作的集合
    private final Map<ByteArrayWrapper, Message> messageMap = Collections.synchronizedMap(new HashMap<>());

    // 得到的消息与发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = Collections.synchronizedMap(new HashMap<>());

    // message content set
    private final Set<ByteArrayWrapper> messageContentSet = Collections.synchronizedSet(new HashSet<>());

    // 请求到的demand item
    private final Set<ByteArrayWrapper> demandItem = Collections.synchronizedSet(new HashSet<>());

    // 我的朋友集合
    private final Set<ByteArrayWrapper> friends = Collections.synchronizedSet(new HashSet<>());

    // 我的朋友的最新消息的时间戳 <friend, timestamp>
    private final Map<ByteArrayWrapper, Long> friendTimeStamp = Collections.synchronizedMap(new HashMap<>());

    // 我的朋友的最新消息的root <friend, root>
    private final Map<ByteArrayWrapper, byte[]> friendRoot = Collections.synchronizedMap(new HashMap<>());

    // 新发现的，等待通知UI的，我的朋友的最新确认的root <friend, root>
    private final Map<ByteArrayWrapper, byte[]> friendConfirmationRoot = Collections.synchronizedMap(new HashMap<>());

    // 给我的朋友的最新消息的时间戳 <friend, timestamp>
    private final Map<ByteArrayWrapper, Long> timeStampToFriend = Collections.synchronizedMap(new HashMap<>());

    // 给我的朋友的最新消息的root <friend, root>
    private final Map<ByteArrayWrapper, byte[]> rootToFriend = Collections.synchronizedMap(new HashMap<>());

    // 发给朋友的confirmation root hash <<sender, receiver>, root>
    private final Map<ByteArrayWrapper, byte[]> confirmationRootToFriend = Collections.synchronizedMap(new HashMap<>());

    // 我的需求 <friend, demand hash>，用于通知我的朋友我的需求
    private final Map<ByteArrayWrapper, byte[]> demandHash = Collections.synchronizedMap(new HashMap<>());

    // 我收到的需求hash
    private final Set<ByteArrayWrapper> friendDemandHash = Collections.synchronizedSet(new HashSet<>());

    // active friend set
    private final Set<ByteArrayWrapper> activeFriends = Collections.synchronizedSet(new HashSet<>());

    // 最新时间 <<sender, receiver>, timestamp>
    private final Map<Pair<byte[], byte[]>, Long> timeStamp = Collections.synchronizedMap(new HashMap<>());

    // message root hash <<sender, receiver>, root>
    private final Map<Pair<byte[], byte[]>, byte[]> root = Collections.synchronizedMap(new HashMap<>());

    // confirmation root hash <<sender, receiver>, root>
    private final Map<Pair<byte[], byte[]>, byte[]> confirmationRoot = Collections.synchronizedMap(new HashMap<>());

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
     * 获取gossip list
     * @return gossip list
     */
    private List<GossipItem> getGossipList() {
        List<GossipItem> list = new ArrayList<>();

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        // 统计我发给我朋友的消息
        for (ByteArrayWrapper friend: this.friends) {
            Long timeStamp = this.timeStampToFriend.get(friend);
            byte[] root = this.rootToFriend.get(friend);
            byte[] confirmationRoot = this.confirmationRootToFriend.get(friend);

            GossipItem gossipItem = new GossipItem(pubKey, friend.getData(), ByteUtil.longToBytes(timeStamp), GossipType.MSG, root, confirmationRoot);
            list.add(gossipItem);
        }

        // 统计我对朋友的需求
        long currentTime = System.currentTimeMillis() / 1000;
        byte[] timeBytes = ByteUtil.longToBytes(currentTime);
        for (Map.Entry<ByteArrayWrapper, byte[]> entry : this.demandHash.entrySet()) {
            byte[] confirmationRoot = this.confirmationRootToFriend.get(entry.getKey());
            list.add(new GossipItem(pubKey, entry.getKey().getData(), timeBytes, GossipType.DEMAND, entry.getValue(), confirmationRoot));
        }

        // 统计其他人发给我朋友的消息
        for (Map.Entry<Pair<byte[], byte[]>, Long> entry : this.timeStamp.entrySet()) {
            if (this.friends.contains(new ByteArrayWrapper(entry.getKey().second))) {
                // 只添加一天以内有新消息的
                if (currentTime - entry.getValue() < ChainParam.ONE_DAY) {
                    byte[] root = this.root.get(entry.getKey());
                    byte[] confirmationRoot = this.confirmationRoot.get(entry.getKey());
                    if (null != root) {
                        list.add(new GossipItem(entry.getKey().first, entry.getKey().second,
                                ByteUtil.longToBytes(entry.getValue()), GossipType.MSG, root, confirmationRoot));
                    }
                }
            }
        }


        return list;
    }

    /**
     * 尝试对gossip信息瘦身，通过移除一天前的旧数据的方法
     */
    private void tryToSlimDownGossip() {
        Long currentTime = System.currentTimeMillis() / 1000;
        Iterator<Map.Entry<Pair<byte[], byte[]>, Long>> it = this.timeStamp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, Long> entry = it.next();
            if (entry.getValue() - currentTime > ChainParam.ONE_DAY) {
                this.root.remove(entry.getKey());
                this.confirmationRoot.remove(entry.getKey());

                it.remove();
            }
        }
    }

    /**
     * 处理朋友相关的gossip item
     */
    private void dealWithGossipItem() {
        Iterator<GossipItem> it = this.gossipItems.iterator();

        // 遍历
        while (it.hasNext()) {
            GossipItem gossipItem = it.next();

            // 寻找跟我朋友相关的gossip
            for(ByteArrayWrapper friend: this.friends) {
                if (Arrays.equals(friend.getData(), gossipItem.getReceiver())) { // 找到
                    Pair<byte[], byte[]> pair = new Pair<>(gossipItem.getSender(), gossipItem.getReceiver());

                    Long oldTimeStamp = this.timeStamp.get(pair);
                    long timeStamp = ByteUtil.byteArrayToLong(gossipItem.getTimestamp());

                    // 判断是否是新数据，若是，则记录下来以待发布
                    if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                        // 朋友的root帮助记录，由其自己去验证
                        this.root.put(pair, gossipItem.getMessageRoot());
                        this.confirmationRoot.put(pair, gossipItem.getConfirmationRoot());
                        // 更新时间戳
                        this.timeStamp.put(pair, timeStamp);
                    }

                    break;
                }
            }

            it.remove();
        }
    }

    /**
     * 挑选一个活跃的peer访问
     */
    private void visitActivePeer() {
        Iterator<ByteArrayWrapper> it = this.activeFriends.iterator();
        // 如果有现成的peer，则挑选一个peer访问
        if (it.hasNext()) {
            requestGossipInfoFromPeer(it.next());

            it.remove();
        } else {
            // 没有找到活跃的peer，则自己随机访问一个自己的朋友
            Iterator<ByteArrayWrapper> iterator = this.friends.iterator();
            if (iterator.hasNext()) {

                Random random = new Random(System.currentTimeMillis());
                int index = random.nextInt(this.friends.size());

                ByteArrayWrapper peer = null;
                int i = 0;
                while (iterator.hasNext()) {
                    peer = it.next();
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
     * 通知UI新发现的朋友的已读消息的root
     */
    private void notifyUIReadMessageRoot() {
        Iterator<Map.Entry<ByteArrayWrapper, byte[]>> iterator = this.friendConfirmationRoot.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ByteArrayWrapper, byte[]> entry = iterator.next();

            logger.debug("Notify UI read message from friend:{}, root:{}",
                    entry.getKey().toString(), Hex.toHexString(entry.getValue()));

            this.msgListener.onReadMessageRoot(entry.getKey().getData(), entry.getValue());

            iterator.remove();
        }
    }

    /**
     * 处理收到的消息
     * @throws DBException database exception
     */
    private void dealWithMessage() throws DBException {

        // 将消息的内容直接存入数据库
        Iterator<ByteArrayWrapper> it = this.messageContentSet.iterator();
        while (it.hasNext()) {
            byte[] messageContent = it.next().getData();
            this.messageDB.putMessage(HashUtil.bencodeHash(messageContent), messageContent);
            it.remove();
        }

        // 将消息存入数据库并通知UI，尝试获取下一条消息
        Iterator<Map.Entry<ByteArrayWrapper, Message>> iterator = this.messageMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Message message = iterator.next().getValue();
            ByteArrayWrapper msgHash = new ByteArrayWrapper(message.getHash());
            ByteArrayWrapper sender = this.messageSenderMap.get(msgHash);

            // save to db
            this.messageDB.putMessage(message.getHash(), message.getEncoded());

            logger.debug("Notify UI new message from friend:{}, hash:{}",
                    sender.toString(), Hex.toHexString(message.getHash()));

            // 通知UI新消息
            this.msgListener.onNewMessage(sender.getData(), message);

            // try to find next one
            byte[] hash = message.getPreviousMsgDAGRoot();
            if (null == this.messageDB.getMessageByHash(hash)) {
                Message previousMsg = this.messageMap.get(new ByteArrayWrapper(hash));
                if (null == previousMsg) {
                    // request next one
                    requestMessage(hash, sender);
                }
            }

            this.messageSenderMap.remove(msgHash);
            iterator.remove();
        }
    }

    /**
     * 回应远端的需求
     * @throws DBException database exception
     */
    private void responseRemoteDemand() throws DBException {
        Iterator<ByteArrayWrapper> it = this.friendDemandHash.iterator();
        while (it.hasNext()) {
            byte[] hash = it.next().getData();
            logger.info("Response demand:{}", Hex.toHexString(hash));

            byte[] data = this.messageDB.getMessageByHash(hash);
            publishImmutableData(data);

            it.remove();
        }
    }

    /**
     * 处理请求回来的数据，将其存储到数据库，并且demand hash集合中清除掉
     * @throws DBException database exception
     */
    private void dealWithDemandItem() throws DBException {
        Iterator<ByteArrayWrapper> it = this.demandItem.iterator();
        while (it.hasNext()) {
            ByteArrayWrapper demandItem = it.next();
            byte[] hash = HashUtil.bencodeHash(demandItem.getData());
            // 数据存入数据库
            this.messageDB.putMessage(hash, demandItem.getData());

            // 从demand集合移除
            Iterator<Map.Entry<ByteArrayWrapper, byte[]>> iterator = this.demandHash.entrySet().iterator();
            while (iterator.hasNext()) {
                byte[] value = iterator.next().getValue();
                if (Arrays.equals(hash, value)) {
                    logger.debug("Remove hash[{}] from demand hash set", Hex.toHexString(hash));
                    iterator.remove();
                }
            }

            it.remove();
        }
    }

    /**
     * 请求immutable data
     */
    private void requestDemandHash() {
        for (Map.Entry<ByteArrayWrapper, byte[]> entry: this.demandHash.entrySet()) {
            logger.debug("Request demand hash:{}", Hex.toHexString(entry.getValue()));
            requestImmutableData(entry.getValue());
        }
    }

    /**
     * 死循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                notifyUIReadMessageRoot();

                dealWithMessage();

                dealWithGossipItem();

                dealWithDemandItem();

                responseRemoteDemand();

                tryToSlimDownGossip();

                visitActivePeer();

                requestDemandHash();

                tryToSendAllRequest();

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
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), ChainParam.GOSSIP_CHANNEL);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER, pubKey);

        DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(mutableItemRequest);
    }

    /**
     * 请求gossip list, pubKey用于辅助识别是否我的朋友
     * @param hash gossip list hash
     * @param pubKey public key
     */
    private void requestGossipList(byte[] hash, ByteArrayWrapper pubKey) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_LIST, pubKey);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    /**
     * 请求对应哈希值的消息
     * @param hash msg hash
     * @param pubKey public key
     */
    private void requestMessage(byte[] hash, ByteArrayWrapper pubKey) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.MESSAGE, pubKey);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    /**
     * 请求消息携带的内容
     * @param hash 内容哈希
     */
    private void requestMessageContent(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.MESSAGE_CONTENT);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    /**
     * 请求immutable数据
     * @param hash 数据哈希
     */
    public void requestImmutableData(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.IMMUTABLE_DATA);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    /**
     * 发布消息
     * @param message msg to publish
     */
    private void publishMessage(Message message) {
        if (null != message) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(message.getEncoded());

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
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
    private void publishImmutableData(byte[] data) {
        if (null != data) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(data);

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
        }
    }

    /**
     * 发布gossip数据
     * @param gossipList gossip list
     */
    private void publishGossipList(GossipList gossipList) {
        if (null != gossipList) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(gossipList.getEncoded());

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
        }
    }

    /**
     * 在gossip频道发布gossip信息
     * @param gossipList last gossip list
     */
    private void publishMutableGossipList(GossipList gossipList) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = ChainParam.GOSSIP_CHANNEL;
        byte[] encode = gossipList.getEncoded();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHT.MutableItemDistribution mutableItemDistribution = new DHT.MutableItemDistribution(mutableItem, null, null);

            this.queue.offer(mutableItemDistribution);
        }
    }

    /**
     * 发布所有gossip信息
     */
    private void publishGossipInfo() {
        // put mutable item
        List<GossipItem> gossipItemList = getGossipList();

        // 切分gossip列表，前面的列表以immutable形式发布
        byte[] previousGossipListHash = null;
        while (gossipItemList.size() > ChainParam.GOSSIP_SIZE) {
            List<GossipItem> list = new ArrayList<>();
            Iterator<GossipItem> it = gossipItemList.iterator();

            int i = 0;
            while (i <= ChainParam.GOSSIP_SIZE) {
                list.add(it.next());
                it.remove();
                i ++;
            }

            GossipList gossipList = new GossipList(previousGossipListHash, list);
            logger.debug(gossipList.toString());

            publishGossipList(gossipList);

            previousGossipListHash = gossipList.getHash();
        }

        // 最后一个gossip list以mutable形式发布
        GossipList gossipList = new GossipList(previousGossipListHash, gossipItemList);
        logger.debug(gossipList.toString());
        publishMutableGossipList(gossipList);
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
        DHTEngine.getInstance().request(req.getSpec(), req.getCallback(), req.getCallbackData());
    }

    private void requestMutableItem(DHT.MutableItemRequest req) {
        DHTEngine.getInstance().request(req.getSpec(), req.getCallback(), req.getCallbackData());
    }

    private void putImmutableItem(DHT.ImmutableItemDistribution d) {
        DHTEngine.getInstance().distribute(d.getItem(), d.getCallback(), d.getCallbackData());
    }

    private void putMutableItem(DHT.MutableItemDistribution d) {
        DHTEngine.getInstance().distribute(d.getItem(), d.getCallback(), d.getCallbackData());
    }

    /**
     * 将所有的请求一次发给中间层
     */
    private void tryToSendAllRequest() {
        int size = DHTEngine.getInstance().queueOccupation();
        if ((double)size / DHTEngine.DHTQueueCapability < THRESHOLD) {
            while (true) {
                Object request = this.queue.poll();
                if (null != request) {
                    process(request);
                } else {
                    break;
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
        this.timeStampToFriend.put(key, ByteUtil.byteArrayToLong(message.getTimestamp()));
        this.rootToFriend.put(key, message.getHash());
        this.confirmationRootToFriend.put(key, message.getFriendLatestMessageRoot());

        this.messageDB.setMessageToFriendRoot(friend, message.getHash());
    }

    /**
     * 更新我的最新消息的root
     * @param friend msg receiver
     * @param hash msg hash
     * @throws DBException database exception
     */
    private void updateLatestRootToFriend(byte[] friend, byte[] hash) throws DBException {
        this.rootToFriend.put(new ByteArrayWrapper(friend), hash);
        this.messageDB.setMessageToFriendRoot(friend, hash);
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
                saveMessageDataInDB(message, data);
                updateMessageInfoToFriend(friend, message);
                publishMessage(message);
                publishImmutableData(data);
                // 目前仅仅在发布新消息时才发布gossip，留待讨论
                publishGossipInfo();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 调整间隔时间
     */
    private void adjustIntervalTime() {
        int size = DHTEngine.getInstance().queueOccupation();
        if ((double)size / DHTEngine.DHTQueueCapability > THRESHOLD) {
            increaseIntervalTime();
        } else {
            decreaseIntervalTime();
        }
    }

    /**
     * 增加间隔时间
     */
    private void increaseIntervalTime() {
        this.loopIntervalTime = this.loopIntervalTime * 2;
    }

    /**
     * 减少间隔时间
     */
    private void decreaseIntervalTime() {
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
        this.friends.add(new ByteArrayWrapper(pubKey));

        this.messageDB.addFriend(pubKey);
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
        this.timeStampToFriend.remove(key);
        this.rootToFriend.remove(key);
        this.confirmationRootToFriend.remove(key);
        this.demandHash.remove(key);

        Iterator<Map.Entry<Pair<byte[], byte[]>, Long>> iterator = this.timeStamp.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, Long> entry = iterator.next();
            if (Arrays.equals(pubKey, entry.getKey().second)) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<Pair<byte[], byte[]>, byte[]>> it = this.root.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, byte[]> entry = it.next();
            if (Arrays.equals(pubKey, entry.getKey().second)) {
                it.remove();
            }
        }

        it = this.confirmationRoot.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, byte[]> entry = it.next();
            if (Arrays.equals(pubKey, entry.getKey().second)) {
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
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

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
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case IMMUTABLE_DATA: {
                if (null == item) {
                    logger.debug("IMMUTABLE_DATA is empty");
                    return;
                }

                this.demandItem.add(new ByteArrayWrapper(item));

                break;
            }
            case MESSAGE: {
                if (null == item) {
                    logger.debug("MESSAGE is empty");
                    return;
                }

                Message message = new Message(item);
                requestMessageContent(message.getPreviousMsgDAGRoot());
                this.messageMap.put(new ByteArrayWrapper(message.getHash()), message);
                this.messageSenderMap.put(new ByteArrayWrapper(message.getHash()), dataIdentifier.getKey());

                break;
            }
            case MESSAGE_CONTENT: {
                if (null == item) {
                    logger.debug("MESSAGE_CONTENT is empty");
                    return;
                }

                this.messageContentSet.add(new ByteArrayWrapper(item));

                break;
            }
            case GOSSIP_FROM_PEER:
            case GOSSIP_LIST: {
                if (null == item) {
                    logger.debug("GOSSIP_LIST is empty");
                    return;
                }

                GossipList gossipList = new GossipList(item);
                if (null != gossipList.getPreviousGossipListHash()) {
                    requestGossipList(gossipList.getPreviousGossipListHash(), dataIdentifier.getKey());
                }


                if (null != gossipList.getGossipList()) {
                    this.gossipItems.addAll(gossipList.getGossipList());

                    // 信任发送方自己给的gossip信息
                    byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

                    for (GossipItem gossipItem : gossipList.getGossipList()) {
                        ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                        // 如果是对方直接给我的gossip信息，也即gossip item的sender与请求gossip channel的peer一样，
                        // 并且gossip item的receiver是我，那么会直接信任该gossip消息
                        if (Arrays.equals(dataIdentifier.getKey().getData(), gossipItem.getSender())
                                && Arrays.equals(pubKey, gossipItem.getReceiver())) {
                            if (gossipItem.getGossipType() == GossipType.DEMAND) {
                                this.friendDemandHash.add(new ByteArrayWrapper(gossipItem.getMessageRoot()));
                            } else if (gossipItem.getGossipType() == GossipType.MSG) {
                                Long oldTimeStamp = this.friendTimeStamp.get(sender);
                                long timeStamp = ByteUtil.byteArrayToLong(gossipItem.getTimestamp());

                                if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                                    // 加入活跃朋友集合
                                    activeFriends.add(sender);

                                    // 请求该root
                                    requestMessage(gossipItem.getMessageRoot(), dataIdentifier.getKey());
                                    this.friendRoot.put(sender, gossipItem.getMessageRoot());
                                    this.friendConfirmationRoot.put(sender, gossipItem.getConfirmationRoot());
                                    // 更新时间戳，不更新root，因为gossip不可靠，等亲自访问到节点自己给出的信息再更新
                                    this.friendTimeStamp.put(sender, timeStamp);
                                }
                            }
                        }
                    }

                }

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }

}
