package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.taucoin.account.AccountManager;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.DataType;
import io.taucoin.db.BlockStore;
import io.taucoin.db.StateDB;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.types.GossipItem;
import io.taucoin.types.GossipList;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

public class Communication implements DHT.GetDHTItemCallback {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // Queue capability.
    public static final int QueueCapability = 100;

    private final double THRESHOLD = 0.8;

    // 循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    private final TauListener tauListener;

    // block db
    private final BlockStore blockStore;

    // state db
    private final StateDB stateDB;

    // queue
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();

    // gossip item set
    private final Set<GossipItem> gossipItems = new HashSet<>();

    // 我的朋友集合
    private final Set<ByteArrayWrapper> friends = new HashSet<>();

    // 我的朋友的最新消息的时间戳
    private final Map<ByteArrayWrapper, Long> friendTimeStamp = Collections.synchronizedMap(new HashMap<>());

    // 我的朋友的最新消息的root
    private final Map<ByteArrayWrapper, byte[]> friendRoot = Collections.synchronizedMap(new HashMap<>());

    // active friend set
    private final Set<ByteArrayWrapper> activeFriends = new HashSet<>();

    // 最新时间
    private final Map<Pair<byte[], byte[]>, Long> timeStamp = Collections.synchronizedMap(new HashMap<>());

    // message root hash
    private final Map<Pair<byte[], byte[]>, byte[]> rootHash = Collections.synchronizedMap(new HashMap<>());

    // Communication thread.
    private Thread communicationThread;

    public Communication(BlockStore blockStore, StateDB stateDB, TauListener tauListener) {
        this.blockStore = blockStore;
        this.stateDB = stateDB;
        this.tauListener = tauListener;
    }

    private boolean init() {
        try {
            Set<byte[]> friends = this.stateDB.getFriends();

            if (null != friends) {
                for (byte[] friend: friends) {
                    ByteArrayWrapper key = new ByteArrayWrapper(friend);
                    this.friends.add(key);

                    byte[] root = this.stateDB.getFriendMessageRoot(friend);
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

    private void updateGossipInfo() {
        Iterator<GossipItem> it = this.gossipItems.iterator();
        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;
        while (it.hasNext()) {
            GossipItem gossipItem = it.next();
            ByteArrayWrapper sender = new ByteArrayWrapper(gossipItem.getSender());
            ByteArrayWrapper receiver = new ByteArrayWrapper(gossipItem.getReceiver());

            if (Arrays.equals(pubKey, gossipItem.getReceiver())) {
                Long oldTimeStamp = this.friendTimeStamp.get(sender);
                long timeStamp = ByteUtil.byteArrayToLong(gossipItem.getTimestamp());

                if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                    // 加入活跃朋友集合
                    activeFriends.add(sender);

                    this.friendRoot.put(sender, gossipItem.getMessageRoot());
                    // 更新时间戳
                    this.friendTimeStamp.put(sender, timeStamp);
                }
            } else {
                for(ByteArrayWrapper friend: this.friends) {
                    if (Arrays.equals(friend.getData(), gossipItem.getReceiver())) {
                        Pair<byte[], byte[]> pair = new Pair<>(gossipItem.getSender(), gossipItem.getReceiver());

                        Long oldTimeStamp = this.timeStamp.get(pair);
                        long timeStamp = ByteUtil.byteArrayToLong(gossipItem.getTimestamp());

                        if (null == oldTimeStamp || oldTimeStamp.compareTo(timeStamp) < 0) {
                            // 加入活跃朋友集合
                            activeFriends.add(sender);

                            this.rootHash.put(pair, gossipItem.getMessageRoot());
                            // 更新时间戳
                            this.timeStamp.put(pair, timeStamp);
                        }

                        break;
                    }
                }
            }

            it.remove();
        }
    }

    /**
     * 死循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                updateGossipInfo();

                tryToSendAllRequest();

                adjustIntervalTime();

                try {
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }/* catch (DBException e) {
                this.tauListener.onTauError("Data Base Exception!");
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

    private void requestGossipInfoFromPeer(byte[] pubKey) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey, ChainParam.GOSSIP_CHANNEL);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_FROM_PEER);
        // TODO:: put into queue
        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestGossipList(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(DataType.GOSSIP_LIST);
        // TODO:: put into queue
        DHTEngine.getInstance().request(spec, this, dataIdentifier);
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
    }

    private void requestMutableItem(DHT.MutableItemRequest req) {

    }

    private void putImmutableItem(DHT.ImmutableItemDistribution d) {
    }

    private void putMutableItem(DHT.MutableItemDistribution d) {
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
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

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
            case GOSSIP_FROM_PEER:
            case GOSSIP_LIST: {
                if (null == item) {
                    logger.debug("GOSSIP_FROM_PEER is empty");
                    return;
                }

                GossipList gossipList = new GossipList(item);
                if (null != gossipList.getPreviousGossipListHash()) {
                    requestGossipList(gossipList.getPreviousGossipListHash());
                }

                if (null != gossipList.getGossipList()) {
                    this.gossipItems.addAll(gossipList.getGossipList());
                }

                break;
            }
            default: {
                logger.info("Type mismatch.");
            }
        }
    }

}
