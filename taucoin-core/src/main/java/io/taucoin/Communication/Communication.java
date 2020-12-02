package io.taucoin.Communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Queue capability.
    public static final int QueueCapability = 100;

    private final double THRESHOLD = 0.8;

    // 循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    private final MsgListener msgListener;

    // message db
    private final MessageDB messageDB;

    // queue
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();

    private byte[] myLatestMsgRoot = null;

    // gossip item set
    private final Set<GossipItem> gossipItems = new HashSet<>();

    // 消息集合（hash <--> Message）
    private final Map<ByteArrayWrapper, Message> messageMap = Collections.synchronizedMap(new HashMap<>());

    // 消息发送者对照表（Message hash <--> Sender）
    private final Map<ByteArrayWrapper, ByteArrayWrapper> messageSenderMap = Collections.synchronizedMap(new HashMap<>());

    // message content set
    private final Set<ByteArrayWrapper> messageContentSet = new HashSet<>();

    // 我的朋友集合
    private final Set<ByteArrayWrapper> friends = new HashSet<>();

    // 我的朋友的最新消息的时间戳 <friend, timestamp>
    private final Map<ByteArrayWrapper, Long> friendTimeStamp = Collections.synchronizedMap(new HashMap<>());

    // 我的朋友的最新消息的root <friend, root>
    private final Map<ByteArrayWrapper, byte[]> friendRoot = Collections.synchronizedMap(new HashMap<>());

    // 给我的朋友的最新消息的时间戳 <friend, timestamp>
    private final Map<ByteArrayWrapper, Long> toFriendTimeStamp = Collections.synchronizedMap(new HashMap<>());

    // 给我的朋友的最新消息的root <friend, root>
    private final Map<ByteArrayWrapper, byte[]> toFriendRoot = Collections.synchronizedMap(new HashMap<>());

    // 我的需求 <friend, demand hash>，用于通知我的朋友我的需求
    private final Map<ByteArrayWrapper, byte[]> demandHash = Collections.synchronizedMap(new HashMap<>());

    // 我收到的需求hash
    private final Set<ByteArrayWrapper> friendDemandHash = new HashSet<>();

    // active friend set
    private final Set<ByteArrayWrapper> activeFriends = new HashSet<>();

    // 最新时间 <<sender, receiver>, timestamp>
    private final Map<Pair<byte[], byte[]>, Long> timeStamp = Collections.synchronizedMap(new HashMap<>());

    // message root hash <<sender, receiver>, root>
    private final Map<Pair<byte[], byte[]>, byte[]> root = Collections.synchronizedMap(new HashMap<>());

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
            // get my latest msg root
            this.myLatestMsgRoot = this.messageDB.getFriendMessageRoot(AccountManager.getInstance().getKeyPair().first);

            // get friend latest msg root
            Set<byte[]> friends = this.messageDB.getFriends();

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    byte[] root = this.messageDB.getFriendMessageRoot(friend);
                    if (null != root) {
                        this.friendRoot.put(key, root);
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
     * @return
     */
    private List<GossipItem> getGossipList() {
        List<GossipItem> list = new ArrayList<>();

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        // 统计我发给我朋友的消息
        for (ByteArrayWrapper friend: this.friends) {
            Long timeStamp = this.toFriendTimeStamp.get(friend);
            byte[] root = this.toFriendRoot.get(friend);

            list.add(new GossipItem(pubKey, friend.getData(), ByteUtil.longToBytes(timeStamp), GossipType.MSG, root));
        }

        // 统计我对朋友的需求
        long currentTime = System.currentTimeMillis() / 1000;
        byte[] timeBytes = ByteUtil.longToBytes(currentTime);
        for (Map.Entry<ByteArrayWrapper, byte[]> entry : this.demandHash.entrySet()) {
            list.add(new GossipItem(pubKey, entry.getKey().getData(), timeBytes, GossipType.DEMAND, entry.getValue()));
        }

        // 统计其他人发给我朋友的消息
        for (Map.Entry<Pair<byte[], byte[]>, Long> entry : this.timeStamp.entrySet()) {
            if (this.friends.contains(new ByteArrayWrapper(entry.getKey().second))) {
                // 只添加一天以内有新消息的
                if (currentTime - entry.getValue() < ChainParam.ONE_DAY) {
                    byte[] root = this.root.get(entry.getKey());
                    if (null != root) {
                        list.add(new GossipItem(entry.getKey().first, entry.getKey().second,
                                ByteUtil.longToBytes(entry.getValue()), GossipType.MSG, root));
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
                it.remove();

                this.root.remove(entry.getKey());
            }
        }
    }

    private void dealWithGossipItem() {
        Iterator<GossipItem> it = this.gossipItems.iterator();

        while (it.hasNext()) {
            GossipItem gossipItem = it.next();

            for(ByteArrayWrapper friend: this.friends) {
                if (Arrays.equals(friend.getData(), gossipItem.getReceiver())) {
                    Pair<byte[], byte[]> pair = new Pair<>(gossipItem.getSender(), gossipItem.getReceiver());

                    Long oldTimeStamp = this.timeStamp.get(pair);
                    long timeStamp = ByteUtil.byteArrayToLong(gossipItem.getTimestamp());

                    if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                        // 朋友的root帮助记录，由其自己去验证
                        this.root.put(pair, gossipItem.getMessageRoot());
                        // 更新时间戳
                        this.timeStamp.put(pair, timeStamp);
                    }

                    break;
                }
            }

            it.remove();
        }
    }

    private void visitActivePeer() {
        Iterator<ByteArrayWrapper> it = this.activeFriends.iterator();
        if (it.hasNext()) {
            requestGossipInfoFromPeer(it.next());

            it.remove();
        } else {
            // 没有找到活跃的peer，则自己随机访问自己的朋友
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

    private void dealWithMessage() throws DBException {

        Iterator<ByteArrayWrapper> it = this.messageContentSet.iterator();
        while (it.hasNext()) {
            byte[] messageContent = it.next().getData();
            this.messageDB.putMessage(HashUtil.bencodeHash(messageContent), messageContent);
            it.remove();
        }

        Iterator<Map.Entry<ByteArrayWrapper, Message>> iterator = this.messageMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Message message = iterator.next().getValue();
            ByteArrayWrapper msgHash = new ByteArrayWrapper(message.getHash());
            ByteArrayWrapper sender = this.messageSenderMap.get(msgHash);

            // save
            this.messageDB.putMessage(message.getHash(), message.getEncoded());

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
     * 死循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                dealWithMessage();

                dealWithGossipItem();

                tryToSlimDownGossip();

                visitActivePeer();

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

    private void requestGossipInfoFromPeer(ByteArrayWrapper pubKey) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey.getData(), ChainParam.GOSSIP_CHANNEL);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER, pubKey);

        DHT.MutableItemRequest mutableItemRequest = new DHT.MutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(mutableItemRequest);
    }

    private void requestGossipList(byte[] hash, ByteArrayWrapper pubKey) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_LIST, pubKey);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    private void requestMessage(byte[] hash, ByteArrayWrapper pubKey) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.MESSAGE, pubKey);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    private void requestMessageContent(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.MESSAGE_CONTENT);

        DHT.ImmutableItemRequest immutableItemRequest = new DHT.ImmutableItemRequest(spec, this, dataIdentifier);
        this.queue.offer(immutableItemRequest);
    }

    private void publishMessage(Message message) {
        if (null != message) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(message.getEncoded());

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
        }
    }

    public void publishData(List<byte[]> list) {
        if (null != list) {
            for (byte[] data: list) {
                publishData(data);
            }
        }
    }

    private void publishData(byte[] data) {
        if (null != data) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(data);

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
        }
    }

    private void publishGossipList(GossipList gossipList) {
        if (null != gossipList) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(gossipList.getEncoded());

            DHT.ImmutableItemDistribution immutableItemDistribution = new DHT.ImmutableItemDistribution(immutableItem, null, null);
            this.queue.offer(immutableItemDistribution);
        }
    }

    private void publishGossipInfo() {
        // put mutable item
        List<GossipItem> gossipItemList = getGossipList();
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

            publishGossipList(gossipList);

            previousGossipListHash = gossipList.getHash();
        }

        GossipList gossipList = new GossipList(previousGossipListHash, gossipItemList);
        publishGossipList(gossipList);
    }

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
     * 更新发给朋友的消息的时间和root
     * @param friend friend to send msg
     * @param message msg
     */
    private void updateToFriendMessageInfo(byte[] friend, Message message) {
        ByteArrayWrapper key = new ByteArrayWrapper(friend);
        this.toFriendTimeStamp.put(key, ByteUtil.byteArrayToLong(message.getTimestamp()));
        this.toFriendRoot.put(key, message.getHash());
    }

    private void updateMyLatestRoot(byte[] hash) throws DBException {
        this.myLatestMsgRoot = hash;
        this.messageDB.setFriendMessageRoot(AccountManager.getInstance().getKeyPair().first, hash);
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
                updateMyLatestRoot(message.getHash());
                updateToFriendMessageInfo(friend, message);
                publishMessage(message);
                publishData(data);
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


    public void addNewFriend(byte[] pubKey) throws DBException {
        this.friends.add(new ByteArrayWrapper(pubKey));

        this.messageDB.addFriend(pubKey);
    }

    public void delFriend(byte[] pubKey) throws DBException {
        ByteArrayWrapper key = new ByteArrayWrapper(pubKey);
        this.friends.remove(key);
        this.friendTimeStamp.remove(key);
        this.friendRoot.remove(key);
        this.toFriendTimeStamp.remove(key);
        this.toFriendRoot.remove(key);

        Iterator<Map.Entry<Pair<byte[], byte[]>, Long>> iterator = this.timeStamp.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, Long> entry = iterator.next();
            if (Arrays.equals(pubKey, entry.getKey().second)) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<Pair<byte[], byte[]>, byte[]>> it = this.root.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<byte[], byte[]>, byte[]> entry = it.next();
            if (Arrays.equals(pubKey, entry.getKey().second)) {
                it.remove();
            }
        }


        this.messageDB.delFriend(pubKey);
    }

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
     * @return root
     */
    public byte[] getMyLatestMsgRoot() {
        return this.myLatestMsgRoot;
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
                    if (Arrays.equals(pubKey, dataIdentifier.getKey().getData())) {
                        for (GossipItem gossipItem : gossipList.getGossipList()) {
                            ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());

                            if (Arrays.equals(pubKey, gossipItem.getReceiver())) {
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
                                        // 更新时间戳，不更新root，因为gossip不可靠，等亲自访问到节点自己给出的信息再更新
                                        this.friendTimeStamp.put(sender, timeStamp);
                                    }
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
