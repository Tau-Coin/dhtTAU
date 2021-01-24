package io.taucoin.communication;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.taucoin.account.AccountManager;
import io.taucoin.account.KeyChangedListener;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.FriendPair;
import io.taucoin.db.DBException;
import io.taucoin.db.MessageDB;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.listener.MsgListener;
import io.taucoin.listener.MsgStatus;
import io.taucoin.types.Gossip;
import io.taucoin.types.GossipItem;
import io.taucoin.types.GossipStatus;
import io.taucoin.types.Message;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

public class Communication1 implements DHT.GetDHTItemCallback, DHT.PutDHTItemCallback, KeyChangedListener {
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

    private byte[] deviceID;

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

    // 通过gossip推荐机制发现的有新消息的朋友集合（完整公钥）
    private final Set<ByteArrayWrapper> referredFriends = new CopyOnWriteArraySet<>();

    // 通过gossip机制发现的给朋友的gossip item <FriendPair, Timestamp>（非完整公钥, FriendPair均为短地址）
    private final Map<FriendPair, BigInteger> friendGossipItem = Collections.synchronizedMap(new HashMap<>());

    // Communication thread.
    private Thread communicationThread;

    public Communication1(MessageDB messageDB, MsgListener msgListener) {
        this.messageDB = messageDB;
        this.msgListener = msgListener;
    }

    private boolean init() {
        return true;
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
            } /*catch (DBException e) {
                this.msgListener.onMsgError("Data Base Exception!");
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }*/ catch (Exception e) {
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

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {
        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case GOSSIP_FROM_PEER: {
                if (null == item) {
                    logger.debug("GOSSIP_FROM_PEER from peer[{}] is empty", dataIdentifier.getExtraInfo1().toString());
                    return;
                }

                Gossip gossip = new Gossip(item);
                preprocessGossipFromNet(gossip, dataIdentifier.getExtraInfo1());

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }

    @Override
    public void onDHTItemPut(int success, Object cbData) {

    }
}
