package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.taucoin.account.AccountManager;
import io.taucoin.core.AccountState;
import io.taucoin.core.DataIdentifier;
import io.taucoin.core.DataType;
import io.taucoin.core.ImportResult;
import io.taucoin.core.PeerManager;
import io.taucoin.core.ProofOfTransaction;
import io.taucoin.core.TransactionPool;
import io.taucoin.core.TransactionPoolImpl;
import io.taucoin.core.Vote;
import io.taucoin.core.VotingPool;
import io.taucoin.db.BlockInfo;
import io.taucoin.db.BlockStore;
import io.taucoin.db.DBException;
import io.taucoin.db.StateDB;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.processor.StateProcessor;
import io.taucoin.processor.StateProcessorImpl;
import io.taucoin.dht.DHT;
import io.taucoin.dht.DHTEngine;
import io.taucoin.types.Block;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.DemandItem;
import io.taucoin.types.HashList;
import io.taucoin.types.HorizontalItem;
import io.taucoin.types.LocalDemand;
import io.taucoin.types.TipItem;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
import io.taucoin.types.TypesConfig;
import io.taucoin.types.VerticalItem;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;

public class Chains implements DHT.GetDHTItemCallback{
    private static final Logger logger = LoggerFactory.getLogger("Chains");

    // 当前follow的chain ID集合
    private final Set<ByteArrayWrapper> chainIDs = Collections.synchronizedSet(new HashSet<>());

    // 记录等待停止follow的chain ID集合
    private final Set<ByteArrayWrapper> unFollowChainIDs = Collections.synchronizedSet(new HashSet<>());

    private final double THRESHOLD = 0.8;

    // 循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    // 循环间隔时间
    private int loopIntervalTime = MIN_LOOP_INTERVAL_TIME;

    // mutable item salt: tip channel
    private final Map<ByteArrayWrapper, byte[]> tipSalts = Collections.synchronizedMap(new HashMap<>());

    // multi-chain thread.
    private Thread multiChainThread;

    private final TauListener tauListener;

    // consensus: pot
    private final Map<ByteArrayWrapper, ProofOfTransaction> pots = Collections.synchronizedMap(new HashMap<>());

    // tx pool
    private final Map<ByteArrayWrapper, TransactionPool> txPools = Collections.synchronizedMap(new HashMap<>());

    // peer manager
    private final Map<ByteArrayWrapper, PeerManager> peerManagers = Collections.synchronizedMap(new HashMap<>());

    // block db
    private final BlockStore blockStore;

    // state db
    private final StateDB stateDB;

    // state processor: process and roll back block
    private final Map<ByteArrayWrapper, StateProcessor> stateProcessors = Collections.synchronizedMap(new HashMap<>());

    // the best block container of current chain
    private final Map<ByteArrayWrapper, BlockContainer> bestBlockContainers = Collections.synchronizedMap(new HashMap<>());

    // the synced block container of current chain
    private final Map<ByteArrayWrapper, BlockContainer> syncBlockContainers = Collections.synchronizedMap(new HashMap<>());

    // 时间记录器，用于处理定时事件
    private final Map<ByteArrayWrapper, Long> timeRecorders = Collections.synchronizedMap(new HashMap<>());

    // voting pool
    private final Map<ByteArrayWrapper, VotingPool> votingPools = Collections.synchronizedMap(new HashMap<>());

    // 记录寻找peer开始时间: {key: chain ID, value: starting time}
    private final Map<ByteArrayWrapper, Long> findingTime = Collections.synchronizedMap(new HashMap<>());

    // 是否进入挖矿标志: {key: chain ID, value: 是否进入挖矿标志}
    private final Map<ByteArrayWrapper, Boolean> miningFlag = Collections.synchronizedMap(new HashMap<>());

    // 记录投票开始时间: {key: chain ID, value: starting time of voting}
    private final Map<ByteArrayWrapper, Long> votingTime = Collections.synchronizedMap(new HashMap<>());

    // 是否在投票状态标志: {key: chain ID, value: 是否进行投票标志}
    private final Map<ByteArrayWrapper, Boolean> votingFlag = Collections.synchronizedMap(new HashMap<>());

    // 积累的投票所需区块: {key: chain ID, value: block list for voting}
    private final Map<ByteArrayWrapper, List<Block>> votingBlocks = Collections.synchronizedMap(new HashMap<>());

    // 交易池请求的交易: {key: chain ID, value: tx set for tx pool}
    private final Map<ByteArrayWrapper, Set<Transaction>> txMapForPool = Collections.synchronizedMap(new HashMap<>());

    // 区块容器数据集合: {key: chain ID, value: {key: block hash, value: block container} }，用于结果查询
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, BlockContainer>> blockContainerMap = Collections.synchronizedMap(new HashMap<>());

    // 区块数据集合: {key: chain ID, value: {key: block hash, value: block} }，用于缓存block，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Block>> blockMap = Collections.synchronizedMap(new HashMap<>());

    // 交易数据集合: {key: chain ID, value: {key: tx hash, value: Transaction} }， 用于缓存tx，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Transaction>> txMap = Collections.synchronizedMap(new HashMap<>());

    // horizontal item数据集合: {key: chain ID, value: {key: hash, value: horizontal item} }，用于缓存horizontal item，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, HorizontalItem>> horizontalItemMap = Collections.synchronizedMap(new HashMap<>());

    // vertical item数据集合: {key: chain ID, value: {key: hash, value: vertical item} }，用于缓存vertical item，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, VerticalItem>> verticalItemMap = Collections.synchronizedMap(new HashMap<>());

    // 同步计数器，控制一次同步数量
    private final Map<ByteArrayWrapper, Integer> syncCounter = Collections.synchronizedMap(new HashMap<>());

    // 同步所用区块容器数据集合: {key: chain ID, value: {key: block hash, value: block container} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, BlockContainer>> blockContainerMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步所用区块数据集合: {key: chain ID, value: {key: block hash, value: block} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Block>> blockMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步所用交易数据集合: {key: chain ID, value: {key: tx hash, value: Transaction} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Transaction>> txMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步horizontal item数据集合: {key: chain ID, value: {key: hash, value: horizontal item} }，用于缓存horizontal item，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, HorizontalItem>> horizontalItemMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步vertical item数据集合: {key: chain ID, value: {key: hash, value: vertical item} }，用于缓存vertical item，满载后清理
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, VerticalItem>> verticalItemMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 远端请求区块哈希数据集合: {key: chain ID, value: block hash set}
    private final Map<ByteArrayWrapper, LocalDemand> localDemandMap = Collections.synchronizedMap(new HashMap<>());

    // 远端请求区块哈希数据集合: {key: chain ID, value: block hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> blockHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

    // 远端请求交易哈希数据集合: {key: chain ID, value: tx hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> txHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

    // 远端请求horizontal hash数据集合: {key: chain ID, value: horizontal hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> horizontalHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

    // 远端请求vertical hash数据集合: {key: chain ID, value: vertical hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> verticalHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

    // 控制是否自己挖矿还是只同步
    private final Map<ByteArrayWrapper, Boolean> enableMineForTest = Collections.synchronizedMap(new HashMap<>());

    // for test
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Long>> tipSuccess = Collections.synchronizedMap(new HashMap<>());

    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Long>> tipFailure = Collections.synchronizedMap(new HashMap<>());

    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Long>> demandSuccess = Collections.synchronizedMap(new HashMap<>());

    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Long>> demandFailure = Collections.synchronizedMap(new HashMap<>());

    /**
     * Chain constructor.
     *
     * @param blockStore block store
     * @param stateDB state db
     */
    public Chains(BlockStore blockStore, StateDB stateDB, TauListener tauListener) {
        this.blockStore = blockStore;
        this.stateDB = stateDB;
        this.tauListener = tauListener;
    }

    /**
     * Start activities of this chain, mainly including votint and mining.
     *
     * @return boolean successful or not.
     */
    public boolean start() {

        multiChainThread = new Thread(this::blockChainProcess);
        multiChainThread.start();

        return true;
    }

    /**
     * Stop all activities of this chain.
     */
    public void stop() {
        if (null != multiChainThread) {
            multiChainThread.interrupt();
        }
    }

    /**
     * 死循环，遍历所有的链
     */
    private void blockChainProcess() {
        Set<ByteArrayWrapper> chainIDs = new HashSet<>();

        while (!Thread.currentThread().isInterrupted()) {
            try {

                for (ByteArrayWrapper chainID : this.unFollowChainIDs) {
                    this.chainIDs.remove(chainID);
                    removeChainComponent(chainID);
                    removeAllChainInfoInDB(chainID);
                }

                chainIDs.addAll(this.chainIDs);

                traverseMultiChain(chainIDs);

                chainIDs.clear();

                adjustIntervalTime();

                try {
                    Thread.sleep(this.loopIntervalTime);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
            catch (DBException e) {
                this.tauListener.onTauError("Data Base Exception!");
                logger.error(e.getMessage(), e);
            }
            catch (Exception e) {
                logger.error(e.getMessage(), e);
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
     * follow a new chain
     * @param chainID chain ID
     * @return true if succeed, false otherwise
     */
    public boolean followChain(byte[] chainID, List<byte[]> peerList) throws DBException {

        ByteArrayWrapper wChainID = new ByteArrayWrapper(chainID);

        if (null == peerList || peerList.isEmpty()) {
            logger.info("Chain:{} no peers.", wChainID.toString());
            return false;
        }

        for (byte[] peer : peerList) {
            this.stateDB.addPeer(chainID, peer);
        }

        startChain(chainID);

        return true;
    }

    /**
     * follow a new chain
     * @param chainID chain ID
     * @return true if succeed, false otherwise
     */
    public boolean startChain(byte[] chainID) throws DBException {
        ByteArrayWrapper wChainID = new ByteArrayWrapper(chainID);

        if (this.chainIDs.contains(wChainID)) {
            logger.info("Chain:{} is followed.", wChainID.toString());
            return true;
        }

        this.tipSalts.put(wChainID, Salt.makeTipSalt(chainID));

        this.timeRecorders.put(wChainID, 0L);

        // init voting pool
        this.votingPools.put(wChainID, new VotingPool(chainID));

        // init pot consensus
        this.pots.put(wChainID, new ProofOfTransaction(chainID));

        // init state processor
        this.stateProcessors.put(wChainID, new StateProcessorImpl(chainID));

        byte[] bestBlockHash = this.stateDB.getBestBlockHash(chainID);
        if (null != bestBlockHash) {
            logger.info("Chain ID[{}]: Best block hash[{}]",
                    new String(chainID), Hex.toHexString(bestBlockHash));
            this.bestBlockContainers.put(wChainID,
                    this.blockStore.getBlockContainerByHash(chainID, bestBlockHash));
        }

        byte[] syncBlockHash = this.stateDB.getSyncBlockHash(chainID);
        if (null != syncBlockHash) {
            logger.info("Chain ID[{}]: Sync block hash[{}]",
                    new String(chainID), Hex.toHexString(syncBlockHash));
            this.syncBlockContainers.put(wChainID, this.blockStore.getBlockContainerByHash(chainID, syncBlockHash));
        }

        // init peer manager
        PeerManager peerManager = new PeerManager(chainID);
        // get peers form db
        Set<byte[]> peers = this.stateDB.getPeers(chainID);
        Set<ByteArrayWrapper> allPeers = new HashSet<>(1);
        List<ByteArrayWrapper> priorityPeers = new ArrayList<>();
        if (null != peers && !peers.isEmpty()) {
            for (byte[] peer: peers) {
                allPeers.add(new ByteArrayWrapper(peer));
            }
        } else {
            // if there is no peers, add yourself
            allPeers.add(new ByteArrayWrapper(AccountManager.getInstance().getKeyPair().first));
        }

        // get from mutable block
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(wChainID);
        if (null != bestBlockContainer && bestBlockContainer.getBlock().getBlockNum() > 0) {
            // get priority peers in mutable range
            priorityPeers.add(new ByteArrayWrapper(bestBlockContainer.getBlock().getMinerPubkey()));
            byte[] previousHash = bestBlockContainer.getVerticalItem().getPreviousHash();
            for (int i = 0; i < ChainParam.MUTABLE_RANGE; i++) {
                BlockContainer blockContainer = this.blockStore.getBlockContainerByHash(chainID, previousHash);
                if (null != blockContainer) {
                    if (blockContainer.getBlock().getBlockNum() <= 0) {
                        break;
                    }
                    priorityPeers.add(new ByteArrayWrapper(blockContainer.getBlock().getMinerPubkey()));
                    previousHash = blockContainer.getVerticalItem().getPreviousHash();
                } else {
                    break;
                }
            }
        }

        // if no enough peers, get from all peers
        Iterator<ByteArrayWrapper> iterator = allPeers.iterator();
        for (int i = priorityPeers.size(); i < ChainParam.MUTABLE_RANGE; i++) {
            if (iterator.hasNext()) {
                priorityPeers.add(new ByteArrayWrapper(iterator.next().getData()));
            } else {
                break;
            }
        }

        peerManager.init(allPeers, priorityPeers);

        this.peerManagers.put(wChainID, peerManager);

        // init tx pool
        TransactionPool txPool = new TransactionPoolImpl(chainID,
                AccountManager.getInstance().getKeyPair().first, this.stateDB);
        txPool.init();

        this.txPools.put(wChainID, txPool);

        this.findingTime.put(wChainID, 0L);

        this.miningFlag.put(wChainID, true);

        this.votingTime.put(wChainID, 0L);

        this.votingFlag.put(wChainID, false);

        this.votingBlocks.put(wChainID, new ArrayList<>());

        this.txMapForPool.put(wChainID, new HashSet<>());

        this.blockContainerMap.put(wChainID, new HashMap<>());

        this.blockMap.put(wChainID, new HashMap<>());

        this.txMap.put(wChainID, new HashMap<>());

        this.horizontalItemMap.put(wChainID, new HashMap<>());

        this.verticalItemMap.put(wChainID, new HashMap<>());

        this.blockContainerMapForSync.put(wChainID, new HashMap<>());

        this.syncCounter.put(wChainID, 0);

        this.blockMapForSync.put(wChainID, new HashMap<>());

        this.txMapForSync.put(wChainID, new HashMap<>());

        this.horizontalItemMapForSync.put(wChainID, new HashMap<>());

        this.verticalItemMapForSync.put(wChainID, new HashMap<>());

        this.localDemandMap.put(wChainID, new LocalDemand());

        this.blockHashMapFromDemand.put(wChainID, new HashSet<>());

        this.txHashMapFromDemand.put(wChainID, new HashSet<>());

        this.horizontalHashMapFromDemand.put(wChainID, new HashSet<>());

        this.verticalHashMapFromDemand.put(wChainID, new HashSet<>());

        this.enableMineForTest.put(wChainID, true);

        // for test
        this.tipSuccess.put(wChainID, new HashMap<>());

        this.tipFailure.put(wChainID, new HashMap<>());

        this.demandSuccess.put(wChainID, new HashMap<>());

        this.demandFailure.put(wChainID, new HashMap<>());

        // 把新链放入数据库
        this.stateDB.followChain(chainID);

        // 最后增加添加标记
        this.chainIDs.add(wChainID);

        return true;
    }

    /**
     * remove all chain info in database
     * @param chainID chain ID
     */
    private void removeAllChainInfoInDB(ByteArrayWrapper chainID) throws DBException {
        this.blockStore.removeChainInfo(chainID.getData());
        this.stateDB.clearAllState(chainID.getData());
        this.tauListener.onClearChainAllState(chainID.getData());
    }

    /**
     * 移除链相关的各个组件
     * @param chainID chain ID
     */
    private void removeChainComponent(ByteArrayWrapper chainID) {

        this.tipSalts.remove(chainID);

        this.timeRecorders.remove(chainID);

        // init voting pool
        this.votingPools.remove(chainID);

        // init pot consensus
        this.pots.remove(chainID);

        // init state processor
        this.stateProcessors.remove(chainID);

        // init best block and sync block
        this.bestBlockContainers.remove(chainID);

        this.syncBlockContainers.remove(chainID);

        this.peerManagers.remove(chainID);

        this.txPools.remove(chainID);

        this.findingTime.remove(chainID);

        this.miningFlag.remove(chainID);

        this.votingTime.remove(chainID);

        this.votingFlag.remove(chainID);

        this.votingBlocks.remove(chainID);

        this.txMapForPool.remove(chainID);

        this.blockContainerMap.remove(chainID);

        this.blockMap.remove(chainID);

        this.txMap.remove(chainID);

        this.horizontalItemMap.remove(chainID);

        this.verticalItemMap.remove(chainID);

        this.blockContainerMapForSync.remove(chainID);

        this.syncCounter.remove(chainID);

        this.blockMapForSync.remove(chainID);

        this.txMapForSync.remove(chainID);

        this.horizontalItemMapForSync.remove(chainID);

        this.verticalItemMapForSync.remove(chainID);

        this.localDemandMap.remove(chainID);

        this.blockHashMapFromDemand.remove(chainID);

        this.txHashMapFromDemand.remove(chainID);

        this.horizontalHashMapFromDemand.remove(chainID);

        this.verticalHashMapFromDemand.remove(chainID);

        this.enableMineForTest.remove(chainID);

        // for test
        this.tipSuccess.remove(chainID);

        this.tipFailure.remove(chainID);

        this.demandSuccess.remove(chainID);

        this.demandFailure.remove(chainID);
    }

    /**
     * 停止follow一条链
     * @param chainID chain ID
     * @return true if success, false otherwise
     */
    public boolean unFollowChain(byte[] chainID) throws DBException {

        // 先把停止信息写入数据库
        this.stateDB.unfollowChain(chainID);

        // 再加入待处理集合
        this.unFollowChainIDs.add(new ByteArrayWrapper(chainID));

        return true;
    }

    /**
     * 是否空链
     * @param chainID chain ID
     * @return true if empty chain, false otherwise
     */
    private boolean isEmptyChain(ByteArrayWrapper chainID) {
        if (null == this.bestBlockContainers.get(chainID)) {
            logger.debug("Chain ID:{} has no best block container.", new String(chainID.getData()));
            return true;
        }

        return false;
    }

    /**
     * 是否离线时间过长
     * @param chainID chain ID
     * @return true if offline too long, false otherwise
     */
    private boolean isOfflineTooLong(ByteArrayWrapper chainID) {
        return (System.currentTimeMillis() / 1000 - this.bestBlockContainers.get(chainID).getBlock().
                getTimeStamp()) > ChainParam.WARNING_RANGE * ChainParam.DEFAULT_BLOCK_TIME;
    }

    /**
     * 对多条链进行一次遍历
     */
    private void traverseMultiChain(Set<ByteArrayWrapper> chainIDs) throws DBException {
        for (ByteArrayWrapper chainID : chainIDs) {

            // 1. 判断是否空链，非空链忽略这一步
            if (isEmptyChain(chainID)) {
                // 1.1 如果是空链，先查看是否有之前轮次请求回来的数据，有数据则进行链的初始化，没有数据则请求数据
                Iterator<Map.Entry<ByteArrayWrapper, BlockContainer>> iterator =
                        this.blockContainerMap.get(chainID).entrySet().iterator();
                if (iterator.hasNext()) {
                    // 有完整数据回来，则用数据进行初始化链
                    BlockContainer blockContainer = iterator.next().getValue();
                    if (null != blockContainer) {
                        initChain(chainID, blockContainer);
                    }

                    iterator.remove();
                } else {
                    // 没有数据则请求数据
                    byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
                    requestTipItemFromPeer(chainID, peer);
                }
            }

            if (!isEmptyChain(chainID)) {
                // 如果离线时间过长，先进行一个区块时间的数据查找，以决定是否在旧数据基础上挖矿
                if (isOfflineTooLong(chainID)) {
                    if (0 == this.votingTime.get(chainID)) {
                        this.votingTime.put(chainID, System.currentTimeMillis() / 1000);
                        this.miningFlag.put(chainID, false);
                    } else if (System.currentTimeMillis() / 1000 - this.votingTime.get(chainID) > ChainParam.DEFAULT_BLOCK_TIME) {
                        this.miningFlag.put(chainID, true);
                    } else {
                        // 先查看是否有之前轮次请求回来的数据，有数据则放弃之前的数据，用获得的数据进行链的初始化，没有数据则请求数据
                        Iterator<Map.Entry<ByteArrayWrapper, BlockContainer>> iterator =
                                this.blockContainerMap.get(chainID).entrySet().iterator();
                        if (iterator.hasNext()) {
                            // 有完整数据回来，则用数据进行初始化链
                            BlockContainer blockContainer = iterator.next().getValue();
                            if (null != blockContainer) {
                                initChain(chainID, blockContainer);

                                this.miningFlag.put(chainID, true);
                            }

                            iterator.remove();
                        } else {
                            // 没有数据则请求logN的数据
                            int counter = this.peerManagers.get(chainID).getPeerNumber();
                            counter = (int) Math.log(counter);
                            if (counter < 1) {
                                counter = 1;
                            }

                            for (int i = 0; i < counter; i++) {

                                byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
                                logger.debug("Chain ID:{} get a peer:{}",
                                        new String(chainID.getData()), Hex.toHexString(peer));
                                requestTipItemFromPeer(chainID, peer);
                            }
                        }
                    }
                }

                // 2. 如果是非空链，并且允许挖矿，进行挖矿等一系列操作
                if (this.miningFlag.get(chainID)) {

                    // 2.1 首先尝试进行一次状态同步，查看是否有之前轮次请求回来的需要同步的数据，有数据则进行链的同步
                    tryToSync(chainID);

                    // 2.2 判断当前是否处在投票阶段
                    if (!this.votingFlag.get(chainID)) {
                        // 2.2.1 如果不在投票阶段，则尝试用之前请求回来数据进行切换最难链的操作，
                        // 或者没有之前的数据的情况下，开始请求查找最难链
                        tryToReBranchOrRequest(chainID);
                    }

                    // 2.3 如果处于投票阶段, 可能是接着之前的轮次投票，或者在查找最难链的过程第一次触发投票，则尝试投票并切换
                    if (this.votingFlag.get(chainID)) {
                        Vote bestVote = tryToVote(chainID);
                        if (null != bestVote) {
                            tryToChangeToBestVoteOrRequest(chainID, bestVote);
                        }
                    }

                    // 2.4 查看是否有请求的交易回到交易池队列，有则把交易放入交易池入池
                    TransactionPool txPool = this.txPools.get(chainID);
                    for (Transaction tx : this.txMapForPool.get(chainID)) {
                        txPool.addTx(tx);
                    }
                    this.txMapForPool.get(chainID).clear();

                    // 2.5 尝试挖矿
                    tryToMine(chainID);

                    // 定时操作，检查是否到时间
                    if (System.currentTimeMillis() / 1000 - this.timeRecorders.get(chainID) >= ChainParam.DEFAULT_BLOCK_TIME) {
                        // 2.6 传播最佳区块或交易
                        publishTipItem(chainID);

                        //设定新时间起点
                        this.timeRecorders.put(chainID, System.currentTimeMillis() / 1000);
                    }

                    byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
                    // 2.8.1 请求最佳交易
//                    requestTipTxForMining(chainID, peer);

                    // 2.8.2 请求远端需求
                    requestDemandFromPeer(chainID, peer);

                    // 2.9 回应远端需求
                    responseDemand(chainID);

                    // 2.10 尝试缓存瘦身
                    tryToSlimDownCache(chainID);
                }
            }

            try {
                Thread.sleep(this.loopIntervalTime);
            } catch (InterruptedException e) {
                logger.info(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

        }
    }

    /**
     * 尝试使用已有的数据切换链，没有数据则请求数据
     * @param chainID chain ID
     */
    private void tryToReBranchOrRequest(ByteArrayWrapper chainID) throws DBException {
        if (this.blockContainerMap.get(chainID).isEmpty()) {
            // 随机挑选一个peer请求最难链
            byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
            requestTipItemFromPeer(chainID, peer);
        } else {

            boolean clearContainer = true;
            for (Map.Entry<ByteArrayWrapper, BlockContainer> entry : this.blockContainerMap.get(chainID).entrySet()) {
                BlockContainer blockContainer = entry.getValue();

                if (null != blockContainer && blockContainer.getBlock().getCumulativeDifficulty().
                        compareTo(this.bestBlockContainers.get(chainID).getBlock().
                                getCumulativeDifficulty()) > 0) {
                    logger.debug("Block[{}] is greater than best block.",
                            Hex.toHexString(blockContainer.getBlock().getBlockHash()));
                    // 是否需要清除数据，以备下一轮（处理完成或者处理出错，都将清理数据）
                    if (TryResult.REQUEST == tryToReBranch(chainID, blockContainer)) {
                        clearContainer = false;
                    }
                    break;
                }
            }

            if (clearContainer) {
                logger.debug("Clear block container map.");
                this.blockContainerMap.get(chainID).clear();
            }
        }
    }

    /**
     * 尝试切换分支
     * @param chainID chain ID
     * @param blockContainer block container
     * @return try result
     */
    private TryResult tryToReBranch(ByteArrayWrapper chainID, BlockContainer blockContainer) throws DBException {

        if (blockContainer.getBlock().getBlockNum() <
                this.bestBlockContainers.get(chainID).getBlock().getBlockNum()) {
            logger.error("Chain ID:{}, best block number is bigger than given block", new String(chainID.getData()));
            return TryResult.ERROR;
        }

        // 对齐区块号
        BlockContainer referenceBlockContainer = blockContainer;

        // 1. 高度差先缩小到mutable range之内
        BlockContainerResult blockContainerResult1 = tryToGetBlockContainerWithinMutableRange(chainID,
                referenceBlockContainer, this.bestBlockContainers.get(chainID).getBlock().getBlockNum());
        if (TryResult.SUCCESS == blockContainerResult1.tryResult) {
            referenceBlockContainer = blockContainerResult1.blockContainer;
        } else {
            return blockContainerResult1.tryResult;
        }

        // 2. 在mutable range范围内高度对齐
        BlockContainerResult blockContainerResult2 = tryToGetBlockContainerOfGivenNumber(chainID,
                referenceBlockContainer, this.bestBlockContainers.get(chainID).getBlock().getBlockNum());
        if (TryResult.SUCCESS == blockContainerResult2.tryResult) {
            referenceBlockContainer = blockContainerResult2.blockContainer;
        } else {
            return blockContainerResult2.tryResult;
        }

        // 3. 判断对齐之后的区块是否在主链上
        BlockInfo blockInfo = this.blockStore.getBlockInfoByHash(chainID.getData(),
                referenceBlockContainer.getBlock().getBlockHash());

        if (null == blockInfo || !blockInfo.isMainChain()) {
            // 该区块本身不在主链上
            // 3.1 看上一个immutable point block是否在主链上
            IfOnMainChainResult ifOnMainChainResult = checkIfImmutableBlockOnMainChain(chainID, referenceBlockContainer);
            if (TryResult.SUCCESS == ifOnMainChainResult.tryResult) {
                if (ifOnMainChainResult.isOnMainChain) {
                    return reBranch(chainID, blockContainer);
                } else {
                    // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                    // 获取参考点前面第1个immutable block container

                    BlockContainerResult result1 = tryToGetBlockContainerFromCache(chainID,
                            referenceBlockContainer.getBlock().getImmutableBlockHash());

                    if (TryResult.SUCCESS == result1.tryResult) {
                        BlockContainer blockContainer1 = result1.blockContainer;

                        IfOnMainChainResult ifOnMainChainResult1 = checkIfImmutableBlockOnMainChain(chainID, result1.blockContainer);
                        if (TryResult.SUCCESS == ifOnMainChainResult1.tryResult) {
                            if (ifOnMainChainResult1.isOnMainChain) {
                                this.votingFlag.put(chainID, true);
                            } else {
                                // 不在主链上，继续查看第3个immutable block hash
                                // 先获取前面第2个mutable point block container

                                BlockContainerResult result2 = tryToGetBlockContainerFromCache(chainID,
                                        blockContainer1.getBlock().getImmutableBlockHash());

                                if (TryResult.SUCCESS == result2.tryResult) {
                                    BlockContainer blockContainer2 = result2.blockContainer;

                                    IfOnMainChainResult ifOnMainChainResult2 = checkIfImmutableBlockOnMainChain(chainID, blockContainer2);
                                    if (TryResult.SUCCESS == ifOnMainChainResult2.tryResult) {
                                        if (ifOnMainChainResult2.isOnMainChain) {
                                            this.votingFlag.put(chainID, true);
                                        } else {
                                            // fork point out of warning range, maybe it's an attack chain
                                            logger.debug("++ctx-----------------------an attack chain.....");
                                        }
                                    } else {
                                        return ifOnMainChainResult2.tryResult;
                                    }
                                } else {
                                    return result2.tryResult;
                                }
                            }
                        } else {
                            return ifOnMainChainResult1.tryResult;
                        }
                    } else {
                        return result1.tryResult;
                    }
                }
            } else {
                return ifOnMainChainResult.tryResult;
            }
        } else {
            // 该区块在主链上
            logger.debug("Chain ID[{}] Block [{}] is on main chain, re-branch.",
                    new String(chainID.getData()),
                    Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()));
            return reBranch(chainID, blockContainer);
        }

        return TryResult.SUCCESS;
    }

    /**
     * 尝试对缓存数据集合(block & tx)进行瘦身
     * 瘦身策略：数量超过WARNING RANGE，则删除保留MUTABLE RANGE数量的数据
     * @param chainID chain ID
     */
    private void tryToSlimDownCache(ByteArrayWrapper chainID) {

        // block
        if (this.blockMap.get(chainID).size() > 2 * ChainParam.WARNING_RANGE) {
            logger.info("Chain ID:{}: Remove block cache.", new String(chainID.getData()));
            Map<ByteArrayWrapper, Block> oldBlockMap = this.blockMap.get(chainID);
            Map<ByteArrayWrapper, Block> newBlockMap = new HashMap<>(ChainParam.WARNING_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, Block> entry: oldBlockMap.entrySet()) {
                newBlockMap.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.WARNING_RANGE) {
                    break;
                }

                i++;
            }

            this.blockMap.put(chainID, newBlockMap);
            oldBlockMap.clear();
        }

        // tx
        if (this.txMap.get(chainID).size() > 2 * ChainParam.WARNING_RANGE) {
            logger.info("Chain ID:{}: Remove tx cache.", new String(chainID.getData()));
            Map<ByteArrayWrapper, Transaction> oldTxs = this.txMap.get(chainID);
            Map<ByteArrayWrapper, Transaction> newTxs = new HashMap<>(ChainParam.WARNING_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, Transaction> entry: oldTxs.entrySet()) {
                newTxs.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.WARNING_RANGE) {
                    break;
                }

                i++;
            }

            this.txMap.put(chainID, newTxs);
            oldTxs.clear();
        }

        // vertical item
        if (this.verticalItemMap.get(chainID).size() > 2 * ChainParam.WARNING_RANGE) {
            logger.info("Chain ID:{}: Remove vertical item cache.", new String(chainID.getData()));
            Map<ByteArrayWrapper, VerticalItem> oldItems = this.verticalItemMap.get(chainID);
            Map<ByteArrayWrapper, VerticalItem> newItems = new HashMap<>(ChainParam.WARNING_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, VerticalItem> entry: oldItems.entrySet()) {
                newItems.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.WARNING_RANGE) {
                    break;
                }

                i++;
            }

            this.verticalItemMap.put(chainID, newItems);
            oldItems.clear();
        }

        // horizontal item
        if (this.horizontalItemMap.get(chainID).size() > 2 * ChainParam.WARNING_RANGE) {
            logger.info("Chain ID:{}: Remove horizontal item cache.", new String(chainID.getData()));
            Map<ByteArrayWrapper, HorizontalItem> oldItems = this.horizontalItemMap.get(chainID);
            Map<ByteArrayWrapper, HorizontalItem> newItems = new HashMap<>(ChainParam.WARNING_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, HorizontalItem> entry: oldItems.entrySet()) {
                newItems.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.WARNING_RANGE) {
                    break;
                }

                i++;
            }

            this.horizontalItemMap.put(chainID, newItems);
            oldItems.clear();
        }

        if (this.blockContainerMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.blockContainerMapForSync.get(chainID).clear();
        }

        if (this.blockMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.blockMapForSync.get(chainID).clear();
        }

        if (this.txMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.txMapForSync.get(chainID).clear();
        }

        if (this.verticalItemMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.verticalItemMapForSync.get(chainID).clear();
        }

        if (this.horizontalItemMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.horizontalItemMapForSync.get(chainID).clear();
        }
    }

    /**
     * 回应发现的需求
     * @param chainID chain ID
     */
    private void responseDemand(ByteArrayWrapper chainID) throws DBException {
        // block
        for (ByteArrayWrapper blockHash : this.blockHashMapFromDemand.get(chainID)) {

            byte[] previousHash = blockHash.getData();
            logger.debug("Chain ID:{} Response from block hash:{}",
                    new String(chainID.getData()), Hex.toHexString(previousHash));

            for (int i = 0; i < ChainParam.MUTABLE_RANGE; i++) {
                BlockContainer blockContainer = this.blockStore.
                        getBlockContainerByHash(chainID.getData(), previousHash);

                if (null != blockContainer) {
                    publishBlockContainer(blockContainer);
                    if (blockContainer.getBlock().getBlockNum() == 0) {
                        break;
                    }
                    previousHash = blockContainer.getVerticalItem().getPreviousHash();
                } else {
                    logger.debug("Chain ID:{} Cannot find block hash in local:{}",
                            new String(chainID.getData()), Hex.toHexString(previousHash));
                    break;
                }
            }
        }

        // tx
        for (ByteArrayWrapper txid : this.txHashMapFromDemand.get(chainID)) {
            logger.debug("Chain ID:{} Response tx hash:{}",
                    new String(chainID.getData()), txid.toString());
            Transaction tx = this.blockStore.getTransactionByHash(chainID.getData(), txid.getData());
            publishTransaction(tx);
        }

        // horizontal item
        for (ByteArrayWrapper horizontalHash : this.horizontalHashMapFromDemand.get(chainID)) {
            logger.debug("Chain ID:{} Response horizontal hash:{}",
                    new String(chainID.getData()), horizontalHash.toString());
            HorizontalItem horizontalItem = this.blockStore.getHorizontalItemByHash(chainID.getData(),
                    horizontalHash.getData());
            publishHashList(horizontalItem);
        }

        // vertical item
        for (ByteArrayWrapper verticalHash : this.verticalHashMapFromDemand.get(chainID)) {
            logger.debug("Chain ID:{} Response vertical hash:{}",
                    new String(chainID.getData()), verticalHash.toString());
            VerticalItem verticalItem = this.blockStore.getVerticalItemByHash(chainID.getData(),
                    verticalHash.getData());
            publishHashList(verticalItem);
        }

        this.blockHashMapFromDemand.get(chainID).clear();
        this.txHashMapFromDemand.get(chainID).clear();
        this.horizontalHashMapFromDemand.get(chainID).clear();
        this.verticalHashMapFromDemand.get(chainID).clear();
    }

    /**
     * Is block synchronization uncompleted
     * @param chainID chain ID
     * @return true if uncompleted, false otherwise
     */
    private boolean isSyncUncompleted(ByteArrayWrapper chainID) {
        return null != this.syncBlockContainers.get(chainID) &&
                this.syncBlockContainers.get(chainID).getBlock().getBlockNum() > 0;
    }

    /**
     * 请求同步区块
     * @param chainID chain ID
     */
    private void requestSyncBlock(ByteArrayWrapper chainID) {
        logger.debug("Request sync block hash:{}, current block number:{}",
                Hex.toHexString(this.syncBlockContainers.get(chainID).getVerticalItem().getPreviousHash()),
                this.syncBlockContainers.get(chainID).getBlock().getBlockNum());
        this.syncCounter.put(chainID, 0);
        requestBlockForSync(chainID, this.syncBlockContainers.get(chainID).getVerticalItem().getPreviousHash());
    }

    /**
     * 尝试同步，有同步数据返回则同步，否则，不同步
     * @param chainID chain ID
     */
    private void tryToSync(ByteArrayWrapper chainID) throws DBException {
        // 合法性判断
        while (isSyncUncompleted(chainID)) {
            ByteArrayWrapper key = new ByteArrayWrapper(this.syncBlockContainers.get(chainID).getVerticalItem().getPreviousHash());
            BlockContainer blockContainer = this.blockContainerMapForSync.get(chainID).get(key);

            if (null != blockContainer) {
                // 如果同步遇到非法区块，放弃这条链
                if (ImportResult.INVALID_BLOCK == syncBlock(chainID, blockContainer)) {
                    logger.error("Chain ID:{}, Throw this chain away, invalid block:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(blockContainer.getBlock().getBlockHash()));
                    this.blockContainerMapForSync.get(chainID).clear();
                    resetChain(chainID);
                    break;
                }
            } else {
                this.blockContainerMapForSync.get(chainID).remove(key);
                break;
            }
        }
    }

    /**
     * 同步区块
     * @param chainID chain ID
     * @param blockContainer block container
     * @return import result
     */
    private ImportResult syncBlock(ByteArrayWrapper chainID, BlockContainer blockContainer) throws DBException {

        StateDB track = this.stateDB.startTracking(chainID.getData());

        ImportResult result = this.stateProcessors.get(chainID).backwardProcess(blockContainer, track);

        if (ImportResult.IMPORTED_BEST == result) {
            // after sync
            // 1. save block
            // 2. save sync block hash
            // 3. commit new state
            // 4. set sync block
            // 5. add old block peer to peer pool

            this.blockStore.saveBlockContainer(chainID.getData(), blockContainer, true);

            track.setSyncBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());
            track.commit();

            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);

            for (ByteArrayWrapper account: accounts) {
                this.stateDB.addPeer(chainID.getData(), account.getData());
            }

            this.tauListener.onSyncBlock(chainID.getData(), blockContainer);

            this.syncBlockContainers.put(chainID, blockContainer);

            this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
        }

        return result;
    }

    /**
     * 尝试进行一次挖矿
     * @param chainID chain ID
     */
    private void tryToMine(ByteArrayWrapper chainID) throws DBException {
        if (TryResult.SUCCESS == minable(chainID)) {
            BlockContainer blockContainer = mineBlock(chainID);

            if (this.enableMineForTest.get(chainID) && null != blockContainer) {
                StateDB track = this.stateDB.startTracking(chainID.getData());

                if (tryToConnect(chainID, blockContainer, track)) {
                    // after chain change
                    // 1. save block
                    // 2. save best block hash
                    // 3. commit new state
                    // 4. set best block
                    // 5. add new block peer to peer pool
                    // 6. update tx pool
                    // 7. publish new block
                    this.blockStore.saveBlockContainer(chainID.getData(),
                            blockContainer, true);

                    track.setBestBlockHash(chainID.getData(),
                            blockContainer.getBlock().getBlockHash());
                    track.commit();

                    setBestBlockContainer(chainID, blockContainer);

                    this.peerManagers.get(chainID).addNewBlockPeer(blockContainer.getBlock().getMinerPubkey());

                    Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);

                    txPools.get(chainID).recheckAccoutTx(accounts);

                    for (ByteArrayWrapper account: accounts) {
                        this.stateDB.addPeer(chainID.getData(), account.getData());
                    }

                    this.tauListener.onNewBlock(chainID.getData(), blockContainer);

                    publishTipItem(chainID);
                }
            }
        }
    }

    /**
     * re-branch chain
     * @param targetBlockContainer block that chain will change to
     */
    private TryResult reBranch(ByteArrayWrapper chainID, BlockContainer targetBlockContainer) throws DBException {

        byte[] previousHash = targetBlockContainer.getVerticalItem().getPreviousHash();
        List<BlockContainer> containerList = new ArrayList<>();

        containerList.add(targetBlockContainer);

        while (!Thread.interrupted()) {

            // 先查看数据库是否存在
            if (this.blockStore.isBlockOnChain(chainID.getData(), previousHash)) {
                // found in local
                logger.debug("+ctx--------found in local, hash:{}",
                        Hex.toHexString(previousHash));
                break;
            }

            ByteArrayWrapper key = new ByteArrayWrapper(previousHash);

            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID, previousHash);

            if (TryResult.SUCCESS == result.tryResult) {
                logger.debug("ChainID:{}, Find block:{} in cache.", new String(chainID.getData()),
                        Hex.toHexString(key.getData()));
                BlockContainer previousBlockContainer = result.blockContainer;
                // 如果有返回，但是数据不为空
                containerList.add(previousBlockContainer);
                if (previousBlockContainer.getBlock().getBlockNum() <= 0) {
                    break;
                }
                previousHash = previousBlockContainer.getVerticalItem().getPreviousHash();
            } else if (TryResult.ERROR == result.tryResult) {
                // 如果有返回数据，但是数据为空
                return TryResult.ERROR;
            } else if (TryResult.REQUEST == result.tryResult) {
                logger.debug("ChainID:{}, Try to find block from dht:{}", new String(chainID.getData()),
                        Hex.toHexString(key.getData()));
                return TryResult.REQUEST;
            }
        }

        // 如果拿到完整的链，先保存
        for (BlockContainer container : containerList) {
            this.blockStore.saveBlockContainer(chainID.getData(),
                    container, false);
        }

        logger.debug("++ctx-----------------------re-branch.....");

        //try to roll back and reconnect
        StateDB track = stateDB.startTracking(chainID.getData());

        List<BlockContainer> undoBlockContainers = new ArrayList<>();
        List<BlockContainer> newBlockContainers = new ArrayList<>();
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);
        if (!this.blockStore.getForkBlockContainersInfo(chainID.getData(), targetBlockContainer,
                bestBlockContainer, undoBlockContainers, newBlockContainers)) {
            logger.error("Chain ID[{}]: Cannot get fork block, best block[{}], target block[{}]",
                    new String(chainID.getData()),
                    Hex.toHexString(bestBlockContainer.getBlock().getBlockHash()),
                    Hex.toHexString(targetBlockContainer.getBlock().getBlockHash()));

            return TryResult.ERROR;
        }

        StateProcessor stateProcessor = this.stateProcessors.get(chainID);

        for (BlockContainer undoBlockContainer : undoBlockContainers) {
            if (!stateProcessor.rollback(undoBlockContainer, track)) {
                logger.error("Chain ID[{}]: Roll back fail, block hash:{}",
                        new String(chainID.getData()),
                        Hex.toHexString(undoBlockContainer.getBlock().getBlockHash()));
                return TryResult.ERROR;
            }
        }

        int size = newBlockContainers.size();
        for (int i = size - 1; i >= 0; i--) {

            TryResult validResult = isValidBlockContainer(chainID, newBlockContainers.get(i), track);
            if (TryResult.SUCCESS != validResult) {
                logger.error("Chain ID[{}]: Validation is not pass, block hash:{}",
                        new String(chainID.getData()),
                        Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                return validResult;
            }

            ImportResult result = stateProcessor.forwardProcess(newBlockContainers.get(i), track);
            // if need sync more block
            if (result == ImportResult.NO_ACCOUNT_INFO && isSyncUncompleted(chainID)) {
                requestSyncBlock(chainID);
                return TryResult.REQUEST;
            }

            if (result != ImportResult.IMPORTED_BEST) {
                logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                        new String(chainID.getData()),
                        Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                return TryResult.ERROR;
            }

            this.peerManagers.get(chainID).addNewBlockPeer(newBlockContainers.get(i).getBlock().getMinerPubkey());
        }

        // after chain change
        // 1. save block
        // 2. save best block hash
        // 3. commit new state
        // 4. set best block
        // 5. save and add new block peer to peer pool
        // 6. update tx pool
        // 7. publish new block

        this.blockStore.reBranchBlocksWithContainers(chainID.getData(), undoBlockContainers, newBlockContainers);

        track.setBestBlockHash(chainID.getData(), targetBlockContainer.getBlock().getBlockHash());

        track.commit();

        setBestBlockContainer(chainID, targetBlockContainer);

        publishTipItem(chainID);

        // 提取相关peer
        Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(undoBlockContainers);
        accounts.addAll(extractAccountFromBlockContainer(newBlockContainers));

        // 更新交易池
        this.txPools.get(chainID).recheckAccoutTx(accounts);
        // 添加发现的peer
        for (ByteArrayWrapper account: accounts) {
            this.stateDB.addPeer(chainID.getData(), account.getData());
        }

        // 回滚的区块，交易放回交易池
        for (BlockContainer undoBlockContainer : undoBlockContainers) {
            if (null != undoBlockContainer.getTx()) {
                this.txPools.get(chainID).addTx(undoBlockContainer.getTx());
            }

            // 通知UI区块回滚
            this.tauListener.onRollBack(chainID.getData(), undoBlockContainer);
        }

        size = newBlockContainers.size();
        for (int i = size - 1; i >= 0; i--) {
            this.tauListener.onNewBlock(chainID.getData(), newBlockContainers.get(i));
        }

        return TryResult.SUCCESS;
    }

    /**
     * 在投票结果出来，成功切到投票结果位置之后，从投票的block中选出最难的作为新tip
     * @param chainID chain ID
     */
    private void chooseBestBlockAsTipAfterVoting(ByteArrayWrapper chainID) {
        Block bestTipBlock = null;
        for (Block block: this.votingBlocks.get(chainID)) {
            if (null == bestTipBlock || block.getCumulativeDifficulty().
                    compareTo(bestTipBlock.getCumulativeDifficulty()) > 0) {
                bestTipBlock = block;
            }
        }

        if (null == bestTipBlock) {
            return;
        }

        BlockContainer blockContainer = new BlockContainer(bestTipBlock);

        ByteArrayWrapper blockKey = new ByteArrayWrapper(bestTipBlock.getBlockHash());
        this.blockMap.get(chainID).put(blockKey, bestTipBlock);

        boolean success = true;
        if (null != bestTipBlock.getVerticalHash()) {
            VerticalItem verticalItem = this.verticalItemMap.get(chainID).
                    get(new ByteArrayWrapper(bestTipBlock.getVerticalHash()));
            if (null != verticalItem) {
                blockContainer.setVerticalItem(verticalItem);
            } else {
                requestVerticalItemForMining(chainID, bestTipBlock.getVerticalHash(), blockKey);
                success = false;
            }
        }

        if (null != bestTipBlock.getHorizontalHash()) {
            HorizontalItem horizontalItem = this.horizontalItemMap.get(chainID).
                    get(new ByteArrayWrapper(bestTipBlock.getHorizontalHash()));
            if (null != horizontalItem) {
                if (null != horizontalItem.getTxHash()) {
                    ByteArrayWrapper key = new ByteArrayWrapper(horizontalItem.getTxHash());
                    Transaction tx = this.txMap.get(chainID).get(key);
                    if (null != tx) {
                        blockContainer.setHorizontalItem(horizontalItem);
                        blockContainer.setTx(tx);
                    } else {
                        requestTxForMining(chainID, horizontalItem.getTxHash(), blockKey);
                        success = false;
                    }
                }
            } else {
                requestHorizontalItemForMining(chainID, bestTipBlock.getHorizontalHash(), blockKey);
                success = false;
            }
        }

        if (success) {
            this.blockContainerMap.get(chainID).
                    put(new ByteArrayWrapper(bestTipBlock.getBlockHash()), blockContainer);
        }
    }

    /**
     * 重置数据状态集合等信息
     * @param chainID chain ID
     */
    private void resetAfterVoting(ByteArrayWrapper chainID) {
        this.votingPools.get(chainID).clearVotingPool();
        this.votingBlocks.get(chainID).clear();
        this.votingTime.put(chainID, 0L);
        this.votingFlag.put(chainID, false);
    }

    /**
     * 尝试进行投票及投票结束收集最佳选票工作
     * @param chainID chain ID
     * @return best vote or null
     */
    private Vote tryToVote(ByteArrayWrapper chainID) {
        // try to use all peers to vote
        PeerManager peerManager = this.peerManagers.get(chainID);

        int counter = peerManager.getPeerNumber();
        counter = (int)Math.log(counter);
        if (counter < 1) {
            counter = 1;
        }

        int size = this.votingBlocks.get(chainID).size();

        if (counter > size) {
            counter -= size;

            if (0 == this.votingTime.get(chainID)) {
                this.votingTime.put(chainID, System.currentTimeMillis() / 1000);
            } else if (System.currentTimeMillis() / 1000 - this.votingTime.get(chainID) > ChainParam.DEFAULT_MAX_BLOCK_TIME) {
                return getBestVote(chainID);
            }

            while (!Thread.interrupted() && counter > 0) {
                byte[] peer = peerManager.getBlockPeerRandomly();

                if (null != peer) {
                    requestTipBlockForVotingFromPeer(chainID, peer);
                    counter--;

//                    try {
//                        Thread.sleep(LOOP_INTERVAL_TIME);
//                    } catch (InterruptedException e) {
//                        logger.info(new String(chainID.getData()) + ":" + e.getMessage(), e);
//                        Thread.currentThread().interrupt();
//                    }
                }
            }

            return null;
        } else {
            return getBestVote(chainID);
        }
    }

    /**
     * 汇总选票，并挑出最佳选票
     * @param chainID chain ID
     * @return best vest or null
     */
    private Vote getBestVote(ByteArrayWrapper chainID) {
        VotingPool votingPool = this.votingPools.get(chainID);
        List<Block> blocks = this.votingBlocks.get(chainID);

        for (Block block : blocks) {
            if (null != block) {
                // vote on immutable point
                if (block.getBlockNum() > ChainParam.MUTABLE_RANGE) {
                    votingPool.putIntoVotingPool(block.getImmutableBlockHash(),
                            (int) block.getBlockNum() - ChainParam.MUTABLE_RANGE);
                } else {
                    votingPool.putIntoVotingPool(block.getImmutableBlockHash(), 0);
                }
            }
        }

        Vote bestVote = votingPool.getBestVote();

        if (null != bestVote) {
            logger.debug("Chain ID[{}]: Best vote:{}", new String(chainID.getData()), bestVote.toString());
        }

        // clear voting pool for next time when voting end
        this.votingTime.put(chainID, 0L);

        votingPool.clearVotingPool();

        blocks.clear();

        return bestVote;
    }

    /**
     * 用block container初始化链的状态
     * @param chainID chain ID
     * @param blockContainer block container
     * @return true if success, false otherwise
     */
    private boolean initChain(ByteArrayWrapper chainID, BlockContainer blockContainer) throws DBException {
        if (null == blockContainer) {
            logger.error("Chain ID[{}]: Block container is null.", new String(chainID.getData()));
            return false;
        }

        logger.debug("Chain ID[{}]: Init Chain.", new String(chainID.getData()));

        resetChain(chainID);

        // after sync
        // 1. save block
        // 2. save best and sync block hash
        // 3. commit new state
        // 4. set best and sync block
        // 5. add old block peer to peer pool

        // initial sync from best vote
        StateDB track = this.stateDB.startTracking(chainID.getData());
        StateProcessor stateProcessor = this.stateProcessors.get(chainID);
        if (ImportResult.IMPORTED_BEST != stateProcessor.backwardProcess(blockContainer, track)) {
            logger.error("Chain ID[{}]: Process block[{}] fail!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return false;
        }

        this.blockStore.saveBlockContainer(chainID.getData(), blockContainer, true);

        track.setBestBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());
        track.setSyncBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());
        track.commit();

        this.bestBlockContainers.put(chainID, blockContainer);
        this.syncBlockContainers.put(chainID, blockContainer);

        publishTipItem(chainID);

        Set<ByteArrayWrapper> accounts= extractAccountFromBlockContainer(blockContainer);
        for (ByteArrayWrapper account: accounts) {
            this.stateDB.addPeer(chainID.getData(), account.getData());
        }

        this.tauListener.onSyncBlock(chainID.getData(), blockContainer);

        this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());

        return true;
    }

    /**
     * 重置链，将状态归零，需要清楚的组件相应重置
     * @param chainID chain ID
     */
    private void resetChain(ByteArrayWrapper chainID) throws DBException {
        this.blockStore.removeChainBlockInfo(chainID.getData());
        this.stateDB.clearAllState(chainID.getData());

        this.tauListener.onClearChainAllState(chainID.getData());

        this.txPools.get(chainID).reinit();
    }

    /**
     * 试图切换到最佳投票链，没有数据则请求数据
     * @param chainID chain ID
     * @param bestVote best vote
     * @throws DBException data base exception
     */
    private void tryToChangeToBestVoteOrRequest(ByteArrayWrapper chainID, Vote bestVote) throws DBException {
        if (TryResult.REQUEST != tryToChangeToBestVote(chainID, bestVote)) {
            // 重置投票状态
            resetAfterVoting(chainID);
        }
    }

    /**
     * try to change chain tip to best vote block
     * 如果投票出的新ImmutablePointBlock，root在同一链上mutable range内，说明是链的正常发展;
     * 如果投出来的新ImmutablePointBlock, 这个block is within 1x - 3x out of mutable range，
     * 把自己当作新节点处理。检查下自己的历史交易是否在新链上，不在新链上的放回交易池。
     * 如果分叉点在3x mutable range之外，Alert the user of a potential attack。
     * @param bestVote best vote
     * @return true/false
     */
    private TryResult tryToChangeToBestVote(ByteArrayWrapper chainID, Vote bestVote) throws DBException {
        BlockContainer blockContainer = this.blockStore.
                getBlockContainerByHash(chainID.getData(), bestVote.getBlockHash());

        if (null == blockContainer) {

            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID, bestVote.getBlockHash());

            if (TryResult.SUCCESS == result.tryResult) {
                blockContainer = result.blockContainer;
                logger.debug("Chain ID[{}] Got in cache block hash[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(bestVote.getBlockHash()));
            } else {
                logger.debug("Chain ID[{}] Got failed in cache block hash[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(bestVote.getBlockHash()));

                return result.tryResult;
            }
        }

        // 此时投票区块高度即可能大于，也可能小于或者等于本地tip block高度

        BlockContainer referenceBlockContainer = blockContainer;
        // 若大于，先对齐区块号
        // 1. 高度差先缩小到mutable range之内
        while (referenceBlockContainer.getBlock().getBlockNum() - this.bestBlockContainers.get(chainID).
                getBlock().getBlockNum() >= ChainParam.MUTABLE_RANGE) {
            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                    referenceBlockContainer.getBlock().getImmutableBlockHash());

            if (TryResult.SUCCESS == result.tryResult) {
                logger.debug("Chain ID[{}] Got in cache block hash[{}] immutable block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getImmutableBlockHash()));

                referenceBlockContainer = result.blockContainer;
            } else {
                logger.debug("Chain ID[{}] Got failed in cache block hash[{}] immutable block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getImmutableBlockHash()));

                return result.tryResult;
            }
        }

        // 2. 在mutable range范围内高度对齐
        while (referenceBlockContainer.getBlock().getBlockNum() >
                this.bestBlockContainers.get(chainID).getBlock().getBlockNum()) {
            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                    referenceBlockContainer.getVerticalItem().getPreviousHash());

            if (TryResult.SUCCESS == result.tryResult) {
                logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                referenceBlockContainer = result.blockContainer;
            } else {
                logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                return result.tryResult;
            }
        }

        // 此时区块高度等于或者小于本地tip高度
        // 先看小于的情况
        if (this.bestBlockContainers.get(chainID).getBlock().getBlockNum()  > referenceBlockContainer.getBlock().getBlockNum()) {
            long num = this.bestBlockContainers.get(chainID).getBlock().getBlockNum() - referenceBlockContainer.getBlock().getBlockNum();

            if (num > ChainParam.WARNING_RANGE) {
                // fork point out of warning range, maybe it's an attack chain
                logger.debug("++ctx-----------------------an attack chain.....");
            } else if (num > 2 * ChainParam.MUTABLE_RANGE) {
                // 2 * mutable range ~ 3 * mutable range
                // 先与tip - 3 * mutable range高度对齐

                long immutableBlockNumber3 = 0;
                if (this.bestBlockContainers.get(chainID).getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                    immutableBlockNumber3 = this.bestBlockContainers.get(chainID).getBlock().getBlockNum() - 3 * ChainParam.MUTABLE_RANGE;
                }

                while (referenceBlockContainer.getBlock().getBlockNum() > immutableBlockNumber3) {
                    BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                            referenceBlockContainer.getVerticalItem().getPreviousHash());

                    if (TryResult.SUCCESS == result.tryResult) {
                        logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        referenceBlockContainer = result.blockContainer;
                    } else {
                        logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        return result.tryResult;
                    }
                }

                byte[] hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber3);
                byte[] immutableBlockHash3 = referenceBlockContainer.getBlock().getBlockHash();

                if (null == hash) {
                    // 在此高度没有主链信息
                    if (isSyncUncompleted(chainID)) {
                        logger.debug("Chain ID[{}] Need to sync in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber3);
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    } else {
                        logger.debug("Chain ID[{}] Cannot find main chain info in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber3);
                        return TryResult.ERROR;
                    }
                } else {
                    if (Arrays.equals(hash, immutableBlockHash3)) {
                        // 在immutable point2哈希一致，说明分叉点在1-3*mutable range之内
                        logger.debug("Chain ID[{}] Block hash3[{}] fork in mutable range",
                                new String(chainID.getData()), Hex.toHexString(immutableBlockHash3));
                        // be as a new chain when fork point between mutable range and warning range
                        resetChain(chainID);
                    } else {
                        // fork point out of warning range, maybe it's an attack chain
                        logger.debug("++ctx-----------------------an attack chain.....");
                    }
                }
            } else if (num > ChainParam.MUTABLE_RANGE) {
                // mutable range ~ 2 * mutable range
                // 先与tip - 2 * mutable range高度对齐

                long immutableBlockNumber2 = 0;
                if (this.bestBlockContainers.get(chainID).getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                    immutableBlockNumber2 = this.bestBlockContainers.get(chainID).getBlock().getBlockNum() - 2 * ChainParam.MUTABLE_RANGE;
                }

                while (referenceBlockContainer.getBlock().getBlockNum() > immutableBlockNumber2) {
                    BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                            referenceBlockContainer.getVerticalItem().getPreviousHash());

                    if (TryResult.SUCCESS == result.tryResult) {
                        logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        referenceBlockContainer = result.blockContainer;
                    } else {
                        logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        return result.tryResult;
                    }
                }

                byte[] hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber2);
                byte[] immutableBlockHash2 = referenceBlockContainer.getBlock().getBlockHash();

                if (null == hash) {
                    // 在此高度没有主链信息
                    if (isSyncUncompleted(chainID)) {
                        logger.debug("Chain ID[{}] Need to sync in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber2);
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    } else {
                        logger.debug("Chain ID[{}] Cannot find main chain info in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber2);
                        return TryResult.ERROR;
                    }
                } else {
                    if (Arrays.equals(hash, immutableBlockHash2)) {
                        // 在immutable point2哈希一致，说明分叉点在1-3*mutable range之内
                        logger.debug("Chain ID[{}] Block hash2[{}] fork in mutable range",
                                new String(chainID.getData()), Hex.toHexString(immutableBlockHash2));
                        // be as a new chain when fork point between mutable range and warning range
                        resetChain(chainID);
                    } else {
                        // 分叉点在mutable range之外，判断是否在3倍的mutable range之内

                        BlockContainerResult result2 = tryToGetBlockContainerFromCache(chainID, immutableBlockHash2);

                        if (TryResult.SUCCESS == result2.tryResult) {
                            BlockContainer blockContainer2 = result2.blockContainer;
                            // 如果有返回，但是数据不为空，继续找下一个immutable block
                            byte[] immutableBlockHash3 = blockContainer2.getBlock().getImmutableBlockHash();

                            long immutableBlockNumber3 = 0;
                            if (blockContainer2.getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                                immutableBlockNumber3 = blockContainer2.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
                            }

                            hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber3);
                            if (null == hash) {
                                // 在此高度没有主链信息
                                if (isSyncUncompleted(chainID)) {
                                    logger.debug("Chain ID[{}] Need to sync in height2:{}",
                                            new String(chainID.getData()), immutableBlockNumber3);
                                    requestSyncBlock(chainID);
                                    return TryResult.REQUEST;
                                } else {
                                    logger.debug("Chain ID[{}] Cannot find main chain info in height2:{}",
                                            new String(chainID.getData()), immutableBlockNumber3);
                                    return TryResult.ERROR;
                                }
                            } else {
                                if (Arrays.equals(hash, immutableBlockHash3)) {
                                    // 在immutable point2哈希一致，说明分叉点在1-3*mutable range之内
                                    logger.debug("Chain ID[{}] Block hash2[{}] fork in mutable range",
                                            new String(chainID.getData()), Hex.toHexString(immutableBlockHash3));
                                    // be as a new chain when fork point between mutable range and warning range
                                    resetChain(chainID);
                                } else {
                                    // fork point out of warning range, maybe it's an attack chain
                                    logger.debug("++ctx-----------------------an attack chain.....");
                                }
                            }
                        } else {
                            return result2.tryResult;
                        }
                    }
                }
            } else {
                // mutable range以内
                // 先与tip - mutable range高度对齐

                long immutableBlockNumber1 = 0;
                if (this.bestBlockContainers.get(chainID).getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                    immutableBlockNumber1 = this.bestBlockContainers.get(chainID).getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
                }

                while (referenceBlockContainer.getBlock().getBlockNum() > immutableBlockNumber1) {
                    BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                            referenceBlockContainer.getVerticalItem().getPreviousHash());

                    if (TryResult.SUCCESS == result.tryResult) {
                        logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        referenceBlockContainer = result.blockContainer;
                    } else {
                        logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                                new String(chainID.getData()),
                                Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                                Hex.toHexString(referenceBlockContainer.getVerticalItem().getPreviousHash()));

                        return result.tryResult;
                    }
                }

                byte[] hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber1);
                byte[] immutableBlockHash1 = referenceBlockContainer.getBlock().getBlockHash();

                if (null == hash) {
                    // 在此高度没有主链信息
                    if (isSyncUncompleted(chainID)) {
                        logger.debug("Chain ID[{}] Need to sync in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber1);
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    } else {
                        logger.debug("Chain ID[{}] Cannot find main chain info in height1:{}",
                                new String(chainID.getData()), immutableBlockNumber1);
                        return TryResult.ERROR;
                    }
                } else {
                    if (Arrays.equals(hash, immutableBlockHash1)) {
                        // 在immutable point哈希一致，说明分叉点在mutable range之内
                        logger.debug("Chain ID[{}] Block hash1[{}] fork in mutable range",
                                new String(chainID.getData()), Hex.toHexString(immutableBlockHash1));
                        // change to more difficult chain
                        TryResult result = reBranch(chainID, blockContainer);
                        if (TryResult.SUCCESS == result) {
                            // 如果成功切换到投票分支，则选择一个immutable block hash和best vote一致的难度值最高的区块
                            // 将其加入最长链待处理的数据集合
                            chooseBestBlockAsTipAfterVoting(chainID);
                        }
                        return result;
                    } else {
                        // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                        // 获取参考点前面第1个immutable block container

                        BlockContainerResult result1 = tryToGetBlockContainerFromCache(chainID, immutableBlockHash1);

                        if (TryResult.SUCCESS == result1.tryResult) {
                            BlockContainer blockContainer1 = result1.blockContainer;
                            // 如果有返回，但是数据不为空，继续找第二个immutable block
                            // 获取前面第2个immutable block hash
                            byte[] immutableBlockHash2 = blockContainer1.getBlock().getImmutableBlockHash();

                            long immutableBlockNumber2 = 0;
                            if (blockContainer1.getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                                immutableBlockNumber2 = blockContainer1.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
                            }

                            hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber2);
                            if (null == hash) {
                                // 在此高度没有主链信息
                                if (isSyncUncompleted(chainID)) {
                                    logger.debug("Chain ID[{}] Need to sync in height2:{}",
                                            new String(chainID.getData()), immutableBlockNumber2);
                                    requestSyncBlock(chainID);
                                    return TryResult.REQUEST;
                                } else {
                                    logger.debug("Chain ID[{}] Cannot find main chain info in height2:{}",
                                            new String(chainID.getData()), immutableBlockNumber2);
                                    return TryResult.ERROR;
                                }
                            } else {
                                if (Arrays.equals(hash, immutableBlockHash2)) {
                                    // 在immutable point2哈希一致，说明分叉点在1-3*mutable range之内
                                    logger.debug("Chain ID[{}] Block hash2[{}] fork in mutable range",
                                            new String(chainID.getData()), Hex.toHexString(immutableBlockHash2));
                                    // be as a new chain when fork point between mutable range and warning range
                                    resetChain(chainID);
                                } else {
                                    // 不在主链上，继续查看第3个immutable block hash
                                    // 先获取前面第2个mutable point block container

                                    BlockContainerResult result2 = tryToGetBlockContainerFromCache(chainID, immutableBlockHash2);

                                    if (TryResult.SUCCESS == result2.tryResult) {
                                        BlockContainer blockContainer2 = result2.blockContainer;
                                        // 查看第3个immutable block hash
                                        byte[] immutableBlockHash3 = blockContainer2.
                                                getBlock().getImmutableBlockHash();

                                        long immutableBlockNumber3 = 0;
                                        if (blockContainer2.getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                                            immutableBlockNumber3 = blockContainer2.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
                                        }

                                        hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber3);
                                        if (null == hash) {
                                            // 在此高度没有主链信息
                                            if (isSyncUncompleted(chainID)) {
                                                logger.debug("Chain ID[{}] Need to sync in height3:{}",
                                                        new String(chainID.getData()), immutableBlockNumber3);
                                                requestSyncBlock(chainID);
                                                return TryResult.REQUEST;
                                            } else {
                                                logger.debug("Chain ID[{}] Cannot find main chain info in height3:{}",
                                                        new String(chainID.getData()), immutableBlockNumber3);
                                                return TryResult.ERROR;
                                            }
                                        } else {
                                            if (Arrays.equals(hash, immutableBlockHash3)) {
                                                // 在immutable point3哈希一致，说明分叉点在1-3*mutable range之内
                                                logger.debug("Chain ID[{}] Block hash3[{}] fork in mutable range",
                                                        new String(chainID.getData()), Hex.toHexString(immutableBlockHash3));
                                                // be as a new chain when fork point between mutable range and warning range
                                                resetChain(chainID);
                                            } else {
                                                // fork point out of warning range, maybe it's an attack chain
                                                logger.debug("++ctx-----------------------an attack chain.....");
                                            }
                                        }
                                    } else {
                                        return result2.tryResult;
                                    }
                                }
                            }
                        } else {
                            return result1.tryResult;
                        }
                    }
                }
            }
        } else {
            // 3. 相等的情况
            BlockInfo blockInfo = this.blockStore.getBlockInfoByHash(chainID.getData(),
                    referenceBlockContainer.getBlock().getBlockHash());

            if (null == blockInfo || !blockInfo.isMainChain()) {
                // 该区块本身不在主链上
                // 3.1 看上一个immutable point block是否在主链上
                IfOnMainChainResult ifOnMainChainResult = checkIfImmutableBlockOnMainChain(chainID, referenceBlockContainer);
                if (TryResult.SUCCESS == ifOnMainChainResult.tryResult) {
                    if (ifOnMainChainResult.isOnMainChain) {
                        // change to more difficult chain
                        TryResult result = reBranch(chainID, blockContainer);
                        if (TryResult.SUCCESS == result) {
                            // 如果成功切换到投票分支，则选择一个immutable block hash和best vote一致的难度值最高的区块
                            // 将其加入最长链待处理的数据集合
                            chooseBestBlockAsTipAfterVoting(chainID);
                        }
                        return result;
                    } else {
                        // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                        // 获取参考点前面第1个immutable block container

                        BlockContainerResult result1 = tryToGetBlockContainerFromCache(chainID,
                                referenceBlockContainer.getBlock().getImmutableBlockHash());

                        if (TryResult.SUCCESS == result1.tryResult) {
                            BlockContainer blockContainer1 = result1.blockContainer;

                            IfOnMainChainResult ifOnMainChainResult1 = checkIfImmutableBlockOnMainChain(chainID, result1.blockContainer);
                            if (TryResult.SUCCESS == ifOnMainChainResult1.tryResult) {
                                if (ifOnMainChainResult1.isOnMainChain) {
                                    // be as a new chain when fork point between mutable range and warning range
                                    resetChain(chainID);
                                } else {
                                    // 不在主链上，继续查看第3个immutable block hash
                                    // 先获取前面第2个mutable point block container

                                    BlockContainerResult result2 = tryToGetBlockContainerFromCache(chainID,
                                            blockContainer1.getBlock().getImmutableBlockHash());

                                    if (TryResult.SUCCESS == result2.tryResult) {
                                        BlockContainer blockContainer2 = result2.blockContainer;

                                        IfOnMainChainResult ifOnMainChainResult2 = checkIfImmutableBlockOnMainChain(chainID, blockContainer2);
                                        if (TryResult.SUCCESS == ifOnMainChainResult2.tryResult) {
                                            if (ifOnMainChainResult2.isOnMainChain) {
                                                // be as a new chain when fork point between mutable range and warning range
                                                resetChain(chainID);
                                            } else {
                                                // fork point out of warning range, maybe it's an attack chain
                                                logger.debug("++ctx-----------------------an attack chain.....");
                                            }
                                        } else {
                                            return ifOnMainChainResult2.tryResult;
                                        }
                                    } else {
                                        return result2.tryResult;
                                    }
                                }
                            } else {
                                return ifOnMainChainResult1.tryResult;
                            }
                        } else {
                            return result1.tryResult;
                        }
                    }
                } else {
                    return ifOnMainChainResult.tryResult;
                }
            } else {
                // 该区块在主链上
                logger.debug("Chain ID[{}] Block [{}] is on main chain, re-branch.",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()));
                // change to more difficult chain
                TryResult result = reBranch(chainID, blockContainer);
                if (TryResult.SUCCESS == result) {
                    // 如果成功切换到投票分支，则选择一个immutable block hash和best vote一致的难度值最高的区块
                    // 将其加入最长链待处理的数据集合
                    chooseBestBlockAsTipAfterVoting(chainID);
                }
                return result;
            }
        }

        return TryResult.SUCCESS;
    }

    /**
     * check if immutable block hash of a block is on main chain
     * @param chainID chain ID
     * @param blockContainer block container
     * @return result
     * @throws DBException data base exception
     */
    private IfOnMainChainResult checkIfImmutableBlockOnMainChain(ByteArrayWrapper chainID, BlockContainer blockContainer) throws DBException {
        IfOnMainChainResult ifOnMainChainResult = new IfOnMainChainResult();
        ifOnMainChainResult.tryResult = TryResult.SUCCESS;

        byte[] immutableBlockHash = blockContainer.getBlock().getImmutableBlockHash();

        long immutableBlockNumber = 0;
        if (blockContainer.getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
            immutableBlockNumber = blockContainer.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
        }

        byte[] hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber);

        if (null == hash) {
            // 在此高度没有主链信息
            if (isSyncUncompleted(chainID)) {
                logger.debug("Chain ID[{}] Need to sync in height:{}",
                        new String(chainID.getData()), immutableBlockNumber);
                requestSyncBlock(chainID);
                ifOnMainChainResult.tryResult = TryResult.REQUEST;
            } else {
                logger.debug("Chain ID[{}] Cannot find main chain info in height:{}",
                        new String(chainID.getData()), immutableBlockNumber);
                ifOnMainChainResult.tryResult = TryResult.ERROR;
            }
        } else {
            if (Arrays.equals(hash, immutableBlockHash)) {
                // 在immutable point哈希一致，说明在主链上
                logger.debug("Chain ID[{}] Immutable block hash[{}] is on main chain",
                        new String(chainID.getData()), Hex.toHexString(immutableBlockHash));
                ifOnMainChainResult.isOnMainChain = true;
            } else {
                logger.debug("Chain ID[{}] Immutable block hash[{}] is not on main chain",
                        new String(chainID.getData()), Hex.toHexString(immutableBlockHash));
                ifOnMainChainResult.isOnMainChain = false;
            }
        }

        return ifOnMainChainResult;
    }

    /**
     * 获取不大于目标高度mutable range高度的block container
     * @param chainID chain ID
     * @param blockContainer block container
     * @param number block number
     * @return result
     */
    private BlockContainerResult tryToGetBlockContainerWithinMutableRange(ByteArrayWrapper chainID, BlockContainer blockContainer, long number) {

        while (blockContainer.getBlock().getBlockNum() - number >= ChainParam.MUTABLE_RANGE) {
            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                    blockContainer.getBlock().getImmutableBlockHash());

            if (TryResult.SUCCESS == result.tryResult) {
                logger.debug("Chain ID[{}] Got in cache block hash[{}] immutable block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(blockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(blockContainer.getBlock().getImmutableBlockHash()));

                blockContainer = result.blockContainer;
            } else {
                logger.debug("Chain ID[{}] Got failed in cache block hash[{}] immutable block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(blockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(blockContainer.getBlock().getImmutableBlockHash()));

                return result;
            }
        }

        BlockContainerResult result = new BlockContainerResult();
        if (blockContainer.getBlock().getBlockNum() < number) {
            result.tryResult = TryResult.ERROR;
        } else {
            result.tryResult = TryResult.SUCCESS;
            result.blockContainer = blockContainer;
        }

        return result;
    }

    /**
     * 在mutable range范围内，向下获取指定高度上的block container
     * @param chainID chain ID
     * @param blockContainer block container
     * @param number given number
     * @return result
     */
    private BlockContainerResult tryToGetBlockContainerOfGivenNumber(ByteArrayWrapper chainID, BlockContainer blockContainer, long number) {

        if (blockContainer.getBlock().getBlockNum() < number ||
                (blockContainer.getBlock().getBlockNum() - number >= ChainParam.MUTABLE_RANGE)) {
            BlockContainerResult result = new BlockContainerResult();
            result.tryResult = TryResult.ERROR;
            return result;
        }

        while (blockContainer.getBlock().getBlockNum() > number) {
            BlockContainerResult result = tryToGetBlockContainerFromCache(chainID,
                    blockContainer.getVerticalItem().getPreviousHash());

            if (TryResult.SUCCESS == result.tryResult) {
                logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(blockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(blockContainer.getVerticalItem().getPreviousHash()));

                blockContainer = result.blockContainer;
            } else {
                logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(blockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(blockContainer.getVerticalItem().getPreviousHash()));

                return result;
            }
        }

        BlockContainerResult result = new BlockContainerResult();
        result.tryResult = TryResult.SUCCESS;
        result.blockContainer = blockContainer;

        return result;
    }

    /**
     * 尝试从本地缓存获取block container，如果没有则请求缺失的部分
     * @param chainID chain ID
     * @param blockHash block hash
     * @return result
     */
    private BlockContainerResult tryToGetBlockContainerFromCache(ByteArrayWrapper chainID, byte[] blockHash) {
        BlockContainerResult blockContainerResult = new BlockContainerResult();
        blockContainerResult.tryResult = TryResult.SUCCESS;

        ByteArrayWrapper blockKey = new ByteArrayWrapper(blockHash);

        if (this.blockContainerMap.get(chainID).containsKey(blockKey)) {
            BlockContainer blockContainer = this.blockContainerMap.get(chainID).get(blockKey);
            if (null == blockContainer) {
                blockContainerResult.tryResult = TryResult.ERROR;
            } else {
                // 发现现成的数据
                logger.info("Find in block container cache:{}", Hex.toHexString(blockHash));
                blockContainerResult.blockContainer = blockContainer;
            }
        } else {
            // 没有现成的数据，试着从缓存中读取
            if (this.blockMap.get(chainID).containsKey(blockKey)) {
                Block block = this.blockMap.get(chainID).get(blockKey);
                if (null != block) {
                    BlockContainer blockContainer = new BlockContainer(block);

                    if (null != block.getVerticalHash()) {
                        ByteArrayWrapper verticalKey = new ByteArrayWrapper(block.getVerticalHash());
                        if (this.verticalItemMap.get(chainID).containsKey(verticalKey)) {
                            VerticalItem verticalItem = this.verticalItemMap.get(chainID).get(verticalKey);
                            if (null != verticalItem) {
                                blockContainer.setVerticalItem(verticalItem);
                            } else {
                                this.verticalItemMap.get(chainID).remove(verticalKey);
                                blockContainerResult.tryResult = TryResult.ERROR;
                            }
                        } else {
                            requestVerticalItemForMining(chainID, block.getVerticalHash(), blockKey);
                            blockContainerResult.tryResult = TryResult.REQUEST;
                        }
                    }

                    if (null != block.getHorizontalHash()) {
                        ByteArrayWrapper horizontalKey = new ByteArrayWrapper(block.getHorizontalHash());
                        if (this.horizontalItemMap.get(chainID).containsKey(horizontalKey)) {
                            HorizontalItem horizontalItem = this.horizontalItemMap.get(chainID).get(horizontalKey);
                            if (null != horizontalItem) {
                                blockContainer.setHorizontalItem(horizontalItem);

                                if (null != horizontalItem.getTxHash()) {
                                    ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());
                                    if (this.txMap.get(chainID).containsKey(txKey)) {
                                        Transaction tx = this.txMap.get(chainID).get(txKey);
                                        if (null != tx) {
                                            blockContainer.setTx(tx);
                                        } else {
                                            this.txMap.get(chainID).remove(txKey);
                                            blockContainerResult.tryResult = TryResult.ERROR;
                                        }
                                    } else {
                                        requestTxForMining(chainID, horizontalItem.getTxHash(), blockKey);
                                        blockContainerResult.tryResult = TryResult.REQUEST;
                                    }
                                } else {
                                    blockContainerResult.tryResult = TryResult.ERROR;
                                }
                            } else {
                                this.horizontalItemMap.get(chainID).remove(horizontalKey);
                                blockContainerResult.tryResult = TryResult.ERROR;
                            }
                        } else {
                            requestHorizontalItemForMining(chainID, block.getHorizontalHash(), blockKey);
                            blockContainerResult.tryResult = TryResult.REQUEST;
                        }
                    }
                } else {
                    this.blockMap.get(chainID).remove(blockKey);
                    blockContainerResult.tryResult = TryResult.ERROR;
                }
            } else {
                logger.info("Request block :{}", Hex.toHexString(blockHash));
                requestBlockForMining(chainID, blockHash);
                blockContainerResult.tryResult = TryResult.REQUEST;
            }
        }

        return blockContainerResult;
    }

    /**
     * request tip item hash from peer
     * @param chainID chain ID
     * @param peer peer
     */
    private void requestTipItemFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = this.tipSalts.get(chainID);
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.TIP_ITEM_FROM_PEER_FOR_MINING, new ByteArrayWrapper(peer));
        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request demand from peer
     * @param chainID chain ID
     * @param peer peer
     */
    private void requestDemandFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = Salt.makeDemandSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.DEMAND_FROM_PEER,
                new ByteArrayWrapper(peer));
        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request block for mining
     * @param chainID chain ID
     * @param blockHash block hash
     */
    private void requestBlockForMining(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_REQUEST_FOR_MINING,
                new ByteArrayWrapper(blockHash));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request block for sync
     * @param chainID chain ID
     * @param blockHash block hash
     */
    private void requestBlockForSync(ByteArrayWrapper chainID, byte[] blockHash) {

        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_BLOCK_REQUEST_FOR_SYNC, new ByteArrayWrapper(blockHash));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request tx for mining
     * @param chainID chain ID
     * @param txid tx hash
     * @param blockHash block hash which tx belongs to
     */
    private void requestTxForMining(ByteArrayWrapper chainID, byte[] txid, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_TX_REQUEST_FOR_MINING, new ByteArrayWrapper(txid), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request tx for sync
     * @param chainID chain ID
     * @param txid tx hash
     * @param blockHash block hash which tx belongs to
     */
    private void requestTxForSync(ByteArrayWrapper chainID, byte[] txid, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_TX_REQUEST_FOR_SYNC, new ByteArrayWrapper(txid), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request vertical item for mining
     * @param chainID chain ID
     * @param hash item hash
     * @param blockHash block hash which this item belongs to
     */
    private void requestVerticalItemForMining(ByteArrayWrapper chainID, byte[] hash, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_VERTICAL_ITEM_REQUEST_FOR_MINING, new ByteArrayWrapper(hash), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request vertical item for sync
     * @param chainID chain ID
     * @param item item hash
     * @param blockHash block hash which this item belongs to
     */
    private void requestVerticalItemForSync(ByteArrayWrapper chainID, byte[] item, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(item);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_VERTICAL_ITEM_REQUEST_FOR_SYNC, new ByteArrayWrapper(item), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request horizontal item for mining
     * @param chainID chain ID
     * @param hash item hash
     * @param blockHash block hash which this item belongs to
     */
    private void requestHorizontalItemForMining(ByteArrayWrapper chainID, byte[] hash, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_MINING, new ByteArrayWrapper(hash), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request horizontal item for sync
     * @param chainID chain ID
     * @param item item hash
     * @param blockHash block hash which this item belongs to
     */
    private void requestHorizontalItemForSync(ByteArrayWrapper chainID, byte[] item, ByteArrayWrapper blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(item);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_SYNC, new ByteArrayWrapper(item), blockHash);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request tx for pool
     * @param chainID chain ID
     * @param txid tx hash
     */
    private void requestTxForPool(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX_REQUEST_FOR_MINING);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request tip block for voting from peer
     * @param chainID chain ID
     * @param peer peer
     */
    private void requestTipBlockForVotingFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = this.tipSalts.get(chainID);
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK_FROM_PEER_FOR_VOTING);
        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request block for voting
     * @param chainID chain ID
     * @param blockHash block hash
     */
    private void requestBlockForVoting(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_REQUEST_FOR_VOTING);

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request block that others demand
     * @param chainID chain ID
     * @param blockHash block hash
     */
    private void requestBlockDemand(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_DEMAND,
                new ByteArrayWrapper(blockHash));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request tx that others demand
     * @param chainID chain ID
     * @param txid tx hash
     */
    private void requestTxDemand(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_TX_DEMAND,
                new ByteArrayWrapper(txid));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request horizontal item that others demand
     * @param chainID chain ID
     * @param horizontalHash horizontal hash
     */
    private void requestHorizontalItemDemand(ByteArrayWrapper chainID, byte[] horizontalHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(horizontalHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_HORIZONTAL_ITEM_DEMAND,
                new ByteArrayWrapper(horizontalHash));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * request vertical item that others demand
     * @param chainID chain ID
     * @param verticalHash vertical hash
     */
    private void requestVerticalItemDemand(ByteArrayWrapper chainID, byte[] verticalHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(verticalHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_VERTICAL_ITEM_DEMAND,
                new ByteArrayWrapper(verticalHash));

        DHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * publish block hash that demand
     * @param chainID chain ID
     * @param localDemand local demand
     */
    public static void publishDemand(ByteArrayWrapper chainID, LocalDemand localDemand) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = Salt.makeDemandSalt(chainID.getData());
        byte[] encode = DemandItem.with(localDemand).getEncoded();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                    keyPair.second, encode, salt);
            DHTEngine.getInstance().distribute(mutableItem);
        }
    }

    /**
     * publish block
     * @param block block
     */
    private void publishBlock(Block block) {
        if (null != block) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(block.getEncoded());
            DHTEngine.getInstance().distribute(immutableItem);
        }
    }

    /**
     * publish transaction
     * @param tx transaction
     */
    private void publishTransaction(Transaction tx) {
        if (null != tx) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
            DHTEngine.getInstance().distribute(immutableItem);
        }
    }

    /**
     * publish hash list
     * @param hashList hash list
     */
    private void publishHashList(HashList hashList) {
        if (null != hashList) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(hashList.getEncoded());
            DHTEngine.getInstance().distribute(immutableItem);
        }
    }

    /**
     * publish block container
     * @param blockContainer block container
     */
    private void publishBlockContainer(BlockContainer blockContainer) {
        publishTransaction(blockContainer.getTx());
        publishHashList(blockContainer.getHorizontalItem());
        publishHashList(blockContainer.getVerticalItem());
        publishBlock(blockContainer.getBlock());
    }

    /**
     * publish tip item: best block and best tx
     * @param chainID chain ID
     */
    private void publishTipItem(ByteArrayWrapper chainID) {
        byte[] blockHash = null;
        byte[] txHash = null;

        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);
        if (null != bestBlockContainer) {
            publishBlockContainer(bestBlockContainer);
            blockHash = bestBlockContainer.getBlock().getBlockHash();
        }

        Transaction tx = this.txPools.get(chainID).getBestTransaction();
        if (null != tx) {
            publishTransaction(tx);

            txHash = tx.getTxID();
        }

        TipItem tipItem = TipItem.with(blockHash, txHash);

        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
        byte[] salt = this.tipSalts.get(chainID);
        byte[] encode = tipItem.getEncoded();
        if (null != encode) {
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    encode, salt);
            DHTEngine.getInstance().distribute(mutableItem);
        }
    }

    /**
     * connect a block
     * @param chainID chain ID
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if connect successfully, false otherwise
     */
    private boolean tryToConnect(ByteArrayWrapper chainID, final BlockContainer blockContainer, StateDB stateDB) {
        // if main chain
        if (Arrays.equals(this.bestBlockContainers.get(chainID).getBlock().getBlockHash(),
                blockContainer.getVerticalItem().getPreviousHash())) {
            // main chain
            // 自己挖的块，不必再做检查
//            if (!isValidBlockContainer(chainID, blockContainer, stateDB)) {
//                return false;
//            }

            ImportResult result = stateProcessors.get(chainID).forwardProcess(blockContainer, stateDB);
            return ImportResult.IMPORTED_BEST == result;
        } else {
            logger.info("Chain ID[{}]: previous hash mis-match", new String(chainID.getData()));
            return false;
        }
    }

    /**
     * set best block of this chain
     * @param chainID chain ID
     * @param blockContainer best block container
     */
    public void setBestBlockContainer(ByteArrayWrapper chainID, BlockContainer blockContainer) {
        this.bestBlockContainers.put(chainID, blockContainer);
    }

    /**
     * get best block of this chain
     * @param chainID chain ID
     * @return best block container
     */
    public BlockContainer getBestBlockContainer(ByteArrayWrapper chainID) {
        return this.bestBlockContainers.get(chainID);
    }

    /**
     * check if a block container valid
     * @param chainID chain ID
     * @param blockContainer block container
     * @param stateDB state db
     * @return try result
     */
    private TryResult isValidBlockContainer(ByteArrayWrapper chainID,
                                          BlockContainer blockContainer, StateDB stateDB) throws DBException {
//                // 是否本链
//        if (!Arrays.equals(chainID.getData(), block.getChainID())) {
//            logger.error("ChainID[{}]: ChainID mismatch!", new String(chainID.getData()));
//            return false;
//        }

        byte[] immutableBlockHash;

        // if current block number is larger than mutable range
        if (blockContainer.getBlock().getBlockNum() >= ChainParam.MUTABLE_RANGE) {
            immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(),
                    blockContainer.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE);
        } else {
            immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), 0);
        }

        if (null == immutableBlockHash) {
            return TryResult.REQUEST;
        }

        if (!Arrays.equals(immutableBlockHash, blockContainer.getBlock().getImmutableBlockHash())) {
            logger.error("ChainID[{}]: Block[{}] immutable block hash mismatch!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return TryResult.ERROR;
        }

        HashListResult hashListResult = getPreviousHashList(chainID, this.bestBlockContainers.get(chainID));
        if (TryResult.SUCCESS == hashListResult.tryResult) {
            VerticalItem verticalItem = new VerticalItem(hashListResult.hashList);
            if (!Arrays.equals(verticalItem.getHash(), blockContainer.getVerticalItem().getHash())) {
                logger.error("ChainID[{}]: Block[{}] vertical item hash mismatch!",
                        new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
                return TryResult.ERROR;
            }
        } else {
            return hashListResult.tryResult;
        }

        // 时间戳检查
        if (blockContainer.getBlock().getTimeStamp() > System.currentTimeMillis() / 1000) {
            logger.error("ChainID[{}]: Block[{}] Time is in the future!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return TryResult.ERROR;
        }

        // 区块内部自检
        if (!blockContainer.getBlock().isBlockParamValidate()) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return TryResult.ERROR;
        }

        // 区块签名检查
        if (!blockContainer.getBlock().verifyBlockSig()) {
            logger.error("ChainID[{}]: Block[{}] Bad Signature!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return TryResult.ERROR;
        }

        // 是否孤块
        if (!this.blockStore.isBlockOnChain(chainID.getData(), blockContainer.getVerticalItem().getPreviousHash())) {
            logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                    new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
            return TryResult.ERROR;
        }

        // POT共识验证
        return verifyPOT(chainID, blockContainer, stateDB);
    }

    /**
     * check pot consensus
     * @param chainID chain ID
     * @param blockContainer block container to check
     * @param stateDB state db
     * @return try result
     */
    private TryResult verifyPOT(ByteArrayWrapper chainID, BlockContainer blockContainer, StateDB stateDB) throws DBException {
        byte[] blockHash = blockContainer.getBlock().getBlockHash();

        ProofOfTransaction pot = this.pots.get(chainID);

        byte[] pubKey = blockContainer.getBlock().getMinerPubkey();

        BigInteger power = stateDB.getNonce(chainID.getData(), pubKey);
        if (null == power) {
            logger.error("ChainID[{}]: Miner[{}] has no power!",
                    new String(chainID.getData()), Hex.toHexString(pubKey));
            return TryResult.ERROR;
        }
        logger.debug("Chain ID[{}]: Address: {}, mining power: {}",
                new String(chainID.getData()), Hex.toHexString(pubKey), power);

        BlockContainer parentBlockContainer = this.blockStore.getBlockContainerByHash(chainID.getData(),
                blockContainer.getVerticalItem().getPreviousHash());
        if (null == parentBlockContainer) {
            logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                    new String(chainID.getData()), Hex.toHexString(blockHash));
            return TryResult.ERROR;
        }

        // check base target
        Block ancestor3 = null;
        if (parentBlockContainer.getBlock().getBlockNum() > 3) {
            BlockContainer ancestor1 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    parentBlockContainer.getVerticalItem().getPreviousHash());
            if (null == ancestor1) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }

            BlockContainer ancestor2 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    ancestor1.getVerticalItem().getPreviousHash());
            if (null == ancestor2) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }

            ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                    ancestor2.getVerticalItem().getPreviousHash());
            if (null == ancestor3) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }
        }

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(parentBlockContainer.getBlock(), ancestor3);
        if (0 != baseTarget.compareTo(blockContainer.getBlock().getBaseTarget())) {
            logger.error("ChainID[{}]: Block[{}] base target error!",
                    new String(chainID.getData()), Hex.toHexString(blockHash));
            return TryResult.ERROR;
        }

        // check generation signature
        byte[] genSig = pot.calculateGenerationSignature(parentBlockContainer.getBlock().getGenerationSignature(), pubKey);
        if (!Arrays.equals(genSig, blockContainer.getBlock().getGenerationSignature())) {
            logger.error("ChainID[{}]: Block[{}] generation signature error!",
                    new String(chainID.getData()), Hex.toHexString(blockHash));
            return TryResult.ERROR;
        }

        // check cumulative difficulty
        BigInteger culDifficulty = pot.calculateCumulativeDifficulty(
                parentBlockContainer.getBlock().getCumulativeDifficulty(), baseTarget);
        if (0 != culDifficulty.compareTo(blockContainer.getBlock().getCumulativeDifficulty())) {
            logger.error("ChainID[{}]: Block[{}] Cumulative difficulty error!",
                    new String(chainID.getData()), Hex.toHexString(blockHash));
            return TryResult.ERROR;
        }

        BigInteger hit = pot.calculateRandomHit(genSig);
        long timeInterval = blockContainer.getBlock().getTimeStamp() - parentBlockContainer.getBlock().getTimeStamp();

        // verify hit
        if (!pot.verifyHit(hit, baseTarget, power, timeInterval)) {
            logger.error("ChainID[{}]: The block[{}] does not meet the pot consensus!!",
                    new String(chainID.getData()), Hex.toHexString(blockHash));
            return TryResult.ERROR;
        }

        return TryResult.SUCCESS;
    }

    /**
     * check if be able to mine now
     * @param chainID chain ID
     * @return true if can mine, false otherwise
     */
    private TryResult minable(ByteArrayWrapper chainID) throws DBException {

        ProofOfTransaction pot = this.pots.get(chainID);
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

        byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

        if (null == pubKey) {
            logger.info("Chain ID[{}]: PubKey is null.", new String(chainID.getData()));
            return TryResult.ERROR;
        }

        BigInteger power = this.stateDB.getNonce(chainID.getData(), pubKey);
        if (null == power || power.longValue() <= 0) {
            // 如果没有power，并且同步未完成，则请求同步，因为有可能自己的状态数据在更早的区块上记载，比如创世区块地址
            if (isSyncUncompleted(chainID)) {
                logger.info("Chain ID[{}]: PubKey[{}]-No mining power, try to sync.",
                        new String(chainID.getData()), Hex.toHexString(pubKey));
                requestSyncBlock(chainID);
                return TryResult.REQUEST;
            }

            return TryResult.ERROR;
        }

        logger.info("ChainID[{}]: PubKey[{}] mining power: {}",
                new String(chainID.getData()), Hex.toHexString(pubKey), power);

        // check base target
        Block ancestor3 = null;
        if (bestBlockContainer.getBlock().getBlockNum() > 3) {
            BlockContainer ancestor1 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    bestBlockContainer.getVerticalItem().getPreviousHash());
            if (null == ancestor1) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }

            BlockContainer ancestor2 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    ancestor1.getVerticalItem().getPreviousHash());
            if (null == ancestor2) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }

            ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                    ancestor2.getVerticalItem().getPreviousHash());
            if (null == ancestor3) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return TryResult.REQUEST;
                }
                return TryResult.ERROR;
            }
        }

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(bestBlockContainer.getBlock(), ancestor3);

        // check generation signature
        byte[] genSig = pot.calculateGenerationSignature(bestBlockContainer.
                getBlock().getGenerationSignature(), pubKey);

        BigInteger hit = pot.calculateRandomHit(genSig);

        long timeInterval = pot.calculateMiningTimeInterval(hit, baseTarget, power);
        if ((System.currentTimeMillis() / 1000 - bestBlockContainer.getBlock().getTimeStamp()) < timeInterval) {
            logger.debug("Chain ID[{}]: It's not the time for the block.", new String(chainID.getData()));
            return TryResult.ERROR;
        }

        return TryResult.SUCCESS;
    }

    /**
     * get previous hash list
     * @param chainID chain ID
     * @param previousBlockContainer previous block container
     * @return hash list
     * @throws DBException database exception
     */
    private HashListResult getPreviousHashList(ByteArrayWrapper chainID, BlockContainer previousBlockContainer) throws DBException {
        HashListResult hashListResult = new HashListResult();
        hashListResult.tryResult = TryResult.SUCCESS;
        hashListResult.hashList = new ArrayList<>();

        int size = ChainParam.MAX_HASH_NUMBER;
        while (size > 0 && previousBlockContainer.getBlock().getBlockNum() >= 0) {
            hashListResult.hashList.add(previousBlockContainer.getBlock().getBlockHash());

            VerticalItem verticalItem = previousBlockContainer.getVerticalItem();
            if (null != verticalItem) {
                if (null != verticalItem.getPreviousHash()) {
                    previousBlockContainer = this.blockStore.getBlockContainerByHash(chainID.getData(),
                            verticalItem.getPreviousHash());
                    if (null == previousBlockContainer) {
                        if (isSyncUncompleted(chainID)) {
                            requestSyncBlock(chainID);
                            hashListResult.tryResult = TryResult.REQUEST;
                        } else {
                            hashListResult.tryResult = TryResult.ERROR;
                        }
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }

            size--;
        }

        return hashListResult;
    }

    /**
     * mine a block
     * @param chainID chain ID
     * @return block container, or null
     */
    private BlockContainer mineBlock(ByteArrayWrapper chainID) throws DBException {

        ProofOfTransaction pot = this.pots.get(chainID);
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

        Block ancestor3 = null;

        if (bestBlockContainer.getBlock().getBlockNum() > 3) {
            BlockContainer ancestor1 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    bestBlockContainer.getVerticalItem().getPreviousHash());

            BlockContainer ancestor2 = this.blockStore.getBlockContainerByHash(chainID.getData(),
                    ancestor1.getVerticalItem().getPreviousHash());

            ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                    ancestor2.getVerticalItem().getPreviousHash());
        }

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(bestBlockContainer.getBlock(), ancestor3);

        byte[] generationSignature = pot.calculateGenerationSignature(
                bestBlockContainer.getBlock().getGenerationSignature(),
                AccountManager.getInstance().getKeyPair().first);

        BigInteger cumulativeDifficulty = pot.calculateCumulativeDifficulty(
                bestBlockContainer.getBlock().getCumulativeDifficulty(),
                baseTarget);

        byte[] immutableBlockHash;

        // if current block number is larger than mutable range
        if (bestBlockContainer.getBlock().getBlockNum() + 1 >= ChainParam.MUTABLE_RANGE) {
            immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(),
                    bestBlockContainer.getBlock().getBlockNum() + 1 - ChainParam.MUTABLE_RANGE);
        } else {
            immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), 0);
        }

        // if block is too less, sync more
        if (null == immutableBlockHash && isSyncUncompleted(chainID)) {
            requestSyncBlock(chainID);
            return null;
        }

        if (null == immutableBlockHash) {
            logger.error("ChainID[{}]-Get immutable block hash error!", new String(chainID.getData()));
            return null;
        }

        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        Transaction tx = this.txPools.get(chainID).getBestTransaction();

        HashListResult hashListResult = getPreviousHashList(chainID, bestBlockContainer);
        if (TryResult.SUCCESS != hashListResult.tryResult) {
            return null;
        }

        VerticalItem verticalItem = new VerticalItem(hashListResult.hashList);

        Block block = new Block(1, System.currentTimeMillis() / 1000,
                    bestBlockContainer.getBlock().getBlockNum() + 1, verticalItem.getHash(),
                null, immutableBlockHash, baseTarget, cumulativeDifficulty, generationSignature,
                    BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, keyPair.first);

        BlockContainer blockContainer = new BlockContainer(block, verticalItem);

        if (null != tx) {
            HorizontalItem horizontalItem = HorizontalItem.with(tx.getTxID());
            block.setHorizontalHash(horizontalItem.getHash());

            blockContainer.setHorizontalItem(horizontalItem);
            blockContainer.setTx(tx);
        }



        // set state
        StateDB miningTrack = this.stateDB.startTracking(chainID.getData());
        this.stateProcessors.get(chainID).forwardProcess(blockContainer, miningTrack);

        // set state
        AccountState minerState = miningTrack.getAccount(chainID.getData(),
                AccountManager.getInstance().getKeyPair().first);
        block.setMinerBalance(minerState.getBalance());

        if (null != tx) {
            AccountState senderState = miningTrack.getAccount(chainID.getData(),
                    tx.getSenderPubkey());
            block.setSenderBalance(senderState.getBalance());
            block.setSenderNonce(senderState.getNonce());

            if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {

                AccountState receiverState = miningTrack.getAccount(chainID.getData(),
                        ((WiringCoinsTx)tx).getReceiver());

                block.setReceiverBalance(receiverState.getBalance());
            }
        }

        // sign
        block.signBlock(keyPair.second);

        return blockContainer;
    }

    /**
     * get all chain ID
     * @return chain ID set
     */
    public Set<ByteArrayWrapper> getAllChainIDs() {
        return new HashSet<>(this.chainIDs);
    }

    /**
     * get transaction pool
     * @param chainID chain ID
     * @return tx pool
     */
    public TransactionPool getTransactionPool(ByteArrayWrapper chainID) {
        return this.txPools.get(chainID);
    }

    /**
     * get state database
     * @return state db
     */
    public StateDB getStateDB() {
        return this.stateDB;
    }

    /**
     * get block store
     * @return block store
     */
    public BlockStore getBlockStore() {
        return this.blockStore;
    }

    /**
     * extract accounts from block container
     * @param blockContainer block container to be extracted
     * @return account set
     */
    private Set<ByteArrayWrapper> extractAccountFromBlockContainer(BlockContainer blockContainer) {
        Set<ByteArrayWrapper> set = new HashSet<>();
        if (null != blockContainer && blockContainer.getBlock().getBlockNum() > 0) {
            set.add(new ByteArrayWrapper(blockContainer.getBlock().getMinerPubkey()));
            Transaction tx = blockContainer.getTx();
            if (null != tx) {
                set.add(new ByteArrayWrapper(tx.getSenderPubkey()));
                if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {
                    set.add(new ByteArrayWrapper(((WiringCoinsTx)tx).getReceiver()));
                }
            }
        }
        return set;
    }

    /**
     * extract accounts from block container list
     * @param list block container list
     * @return account set
     */
    private Set<ByteArrayWrapper> extractAccountFromBlockContainer(List<BlockContainer> list) {
        Set<ByteArrayWrapper> set = new HashSet<>();
        if (null != list) {
            for (BlockContainer blockContainer: list) {
                Set<ByteArrayWrapper> accountSet = extractAccountFromBlockContainer(blockContainer);
                set.addAll(accountSet);
            }
        }
        return set;
    }

    /**
     * start mining
     * @param chainID chain ID
     */
    public void startMining(byte[] chainID) {
        ByteArrayWrapper key = new ByteArrayWrapper(chainID);
        if (this.enableMineForTest.containsKey(key)) {
            this.enableMineForTest.put(key, true);
        }
    }

    /**
     * stop mining, just sync
     * @param chainID chain ID
     */
    public void stopMining(byte[] chainID) {
        ByteArrayWrapper key = new ByteArrayWrapper(chainID);
        if (this.enableMineForTest.containsKey(key)) {
            this.enableMineForTest.put(key, false);
        }
    }

    private static class IfOnMainChainResult {
        TryResult tryResult;
        boolean isOnMainChain;
    }

    private static class BlockContainerResult {
        TryResult tryResult;
        BlockContainer blockContainer;
    }

    private static class HashListResult {
        TryResult tryResult;
        List<byte[]> hashList;
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {

        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case TIP_ITEM_FROM_PEER_FOR_MINING: {
                if (null == item) {
                    logger.error("TIP_ITEM_FROM_PEER_FOR_MINING from peer[{}] is empty.",
                            dataIdentifier.getHash().toString());

                    Long count = this.tipFailure.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                    if (null == count) {
                        count = 1L;
                    } else {
                        count++;
                    }
                    this.tipFailure.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), count);

                    Long total = this.tipSuccess.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                    if (null == total) {
                        total = count;
                    } else {
                        total += count;
                    }
                    logger.info("Block Tip: Address:{}, failure rate: {} / {} = {}",
                            dataIdentifier.getHash().toString(), count, total, ((float)count / (float)total));

                    return;
                }

                Long count = this.tipSuccess.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                if (null == count) {
                    count = 1L;
                } else {
                    count++;
                }
                this.tipSuccess.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), count);

                Long total = this.tipFailure.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                if (null == total) {
                    total = count;
                } else {
                    total += count;
                }
                logger.info("Block Tip: Address:{}, success rate: {} / {} = {}",
                        dataIdentifier.getHash().toString(), count, total, ((float)count / (float)total));

                TipItem tipItem = new TipItem(item);

                byte[] blockHash = tipItem.getBlockHash();
                if (null != blockHash) {
                    logger.debug("Request tip block hash[{}] from peer[{}]", Hex.toHexString(blockHash),
                            dataIdentifier.getHash().toString());
                    requestBlockForMining(dataIdentifier.getChainID(), blockHash);
                }

                byte[] txHash = tipItem.getTxHash();
                if (null != txHash) {
                    logger.debug("Request tip tx hash[{}] from peer[{}]", Hex.toHexString(txHash),
                            dataIdentifier.getHash().toString());
                    requestTxForPool(dataIdentifier.getChainID(), txHash);
                }

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_MINING is empty, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()));
                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setBlockHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 返回区块为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                }

                byte[] blockHash = this.localDemandMap.get(dataIdentifier.getChainID()).getBlockHash();
                if (null != blockHash && Arrays.equals(blockHash, dataIdentifier.getHash().getData())) {
                    this.localDemandMap.get(dataIdentifier.getChainID()).clearBlockHash();
                }

                Block block = new Block(item);

                // 区块加入数据集合作缓存使用，队列满了删除
                this.blockMap.get(dataIdentifier.getChainID()).
                        put(new ByteArrayWrapper(block.getBlockHash()), block);

                BlockContainer blockContainer = new BlockContainer(block);

                boolean success = true;

                if (null != block.getVerticalHash()) {
                    ByteArrayWrapper key = new ByteArrayWrapper(block.getVerticalHash());
                    if (this.verticalItemMap.get(dataIdentifier.getChainID()).containsKey(key)) {
                        VerticalItem verticalItem = this.verticalItemMap.get(dataIdentifier.getChainID()).get(key);
                        if (null != verticalItem) {
                            blockContainer.setVerticalItem(verticalItem);
                        } else {
                            // 移除空item
                            this.verticalItemMap.get(dataIdentifier.getChainID()).remove(key);
                            // 在block container集合里插入空标志
                            this.blockContainerMap.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);
                            success = false;;
                        }
                    } else {
                        requestVerticalItemForMining(dataIdentifier.getChainID(),
                                block.getVerticalHash(), new ByteArrayWrapper(block.getBlockHash()));
                        success = false;
                    }
                }

                if (null != block.getHorizontalHash()) {
                    ByteArrayWrapper key = new ByteArrayWrapper(block.getHorizontalHash());
                    if (this.horizontalItemMap.get(dataIdentifier.getChainID()).containsKey(key)) {
                        HorizontalItem horizontalItem = this.horizontalItemMap.get(dataIdentifier.getChainID()).get(key);
                        if (null != horizontalItem) {
                            if (null != horizontalItem.getTxHash()) {
                                ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());
                                // 先在缓存查找交易
                                if (this.txMap.get(dataIdentifier.getChainID()).containsKey(txKey)) {

                                    Transaction tx = this.txMap.get(dataIdentifier.getChainID()).get(txKey);

                                    if (null != tx) {
                                        blockContainer.setHorizontalItem(horizontalItem);
                                        blockContainer.setTx(tx);
                                    } else {
                                        // 从缓存删掉空交易，非空交易不作删除，等队列满删除
                                        this.txMap.get(dataIdentifier.getChainID()).remove(txKey);
                                        // 在block container集合里插入空标志
                                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                                put(dataIdentifier.getHash(), null);
                                        success = false;;
                                    }
                                } else {
                                    // 缓存没有，则请求
                                    requestTxForMining(dataIdentifier.getChainID(), horizontalItem.getTxHash(), dataIdentifier.getHash());
                                    success = false;
                                }
                            }
                        } else {
                            // 移除空item
                            this.horizontalItemMap.get(dataIdentifier.getChainID()).remove(key);
                            // 在block container集合里插入空标志
                            this.blockContainerMap.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);
                            success = false;;
                        }
                    } else {
                        requestHorizontalItemForMining(dataIdentifier.getChainID(),
                                block.getHorizontalHash(), dataIdentifier.getHash());
                        success = false;
                    }
                }

                if (success) {
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), blockContainer);
                }

                break;
            }
            case HISTORY_TX_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("HISTORY_TX_REQUEST_FOR_MINING is empty, tx hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()));

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setTxHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getBlockHash(), null);

                    return;
                } else {
                    // 数据非空
                    Transaction tx = TransactionFactory.parseTransaction(item);

                    byte[] txHash = this.localDemandMap.get(dataIdentifier.getChainID()).getTxHash();
                    if (null != txHash && Arrays.equals(txHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearTxHash();
                    }

                    // 放入交易集合作缓存
                    this.txMap.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), tx);

                    Block block = this.blockMap.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block);

                        if (null != block.getVerticalHash()) {
                            VerticalItem verticalItem = this.verticalItemMap.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getVerticalHash()));
                            if (null != verticalItem) {
                                blockContainer.setVerticalItem(verticalItem);
                            } else {
                                return;
                            }
                        }

                        if (null != block.getHorizontalHash()) {
                            HorizontalItem horizontalItem = this.horizontalItemMap.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getHorizontalHash()));
                            if (null != horizontalItem) {
                                blockContainer.setHorizontalItem(horizontalItem);
                                blockContainer.setTx(tx);
                            } else {
                                return;
                            }
                        }

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }
                }

                break;
            }
            case HISTORY_VERTICAL_ITEM_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("HISTORY_VERTICAL_ITEM_REQUEST_FOR_MINING is empty, hash:{}, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()), dataIdentifier.getBlockHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setVerticalHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getBlockHash(), null);

                    return;
                } else {
                    byte[] verticalHash = this.localDemandMap.get(dataIdentifier.getChainID()).getVerticalHash();
                    if (null != verticalHash && Arrays.equals(verticalHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearVerticalHash();
                    }

                    VerticalItem verticalItem = new VerticalItem(item);
                    // 放入缓存
                    this.verticalItemMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), verticalItem);

                    Block block = this.blockMap.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block, verticalItem);

                        if (null != block.getHorizontalHash()) {
                            HorizontalItem horizontalItem = this.horizontalItemMap.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getHorizontalHash()));
                            if (null != horizontalItem) {
                                if (null != horizontalItem.getTxHash()) {
                                    ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());

                                    Transaction tx = this.txMap.get(dataIdentifier.getChainID()).get(txKey);

                                    if (null != tx) {
                                        blockContainer.setHorizontalItem(horizontalItem);
                                        blockContainer.setTx(tx);
                                    } else {
                                        return;
                                    }
                                }
                            } else {
                                return;
                            }
                        }

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }

                }
                break;
            }
            case HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_MINING is empty, hash:{}, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()), dataIdentifier.getBlockHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setHorizontalHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getBlockHash(), null);

                    return;
                } else {
                    byte[] horizontalHash = this.localDemandMap.get(dataIdentifier.getChainID()).getHorizontalHash();
                    if (null != horizontalHash && Arrays.equals(horizontalHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearHorizontalHash();
                    }

                    HorizontalItem horizontalItem = new HorizontalItem(item);
                    // 放入缓存
                    this.horizontalItemMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), horizontalItem);

                    Block block = this.blockMap.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block);

                        if (null != block.getVerticalHash()) {
                            VerticalItem verticalItem = this.verticalItemMap.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getVerticalHash()));
                            if (null != verticalItem) {
                                blockContainer.setVerticalItem(verticalItem);
                            } else {
                                return;
                            }
                        }

                        if (null != horizontalItem.getTxHash()) {
                            ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());
                            // 先在缓存查找交易
                            if (this.txMap.get(dataIdentifier.getChainID()).containsKey(txKey)) {

                                Transaction tx = this.txMap.get(dataIdentifier.getChainID()).get(txKey);

                                if (null != tx) {
                                    blockContainer.setHorizontalItem(horizontalItem);
                                    blockContainer.setTx(tx);

                                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                                            put(dataIdentifier.getHash(), blockContainer);
                                } else {
                                    // 从缓存删掉空交易，非空交易不作删除，等队列满删除
                                    this.txMap.get(dataIdentifier.getChainID()).remove(txKey);
                                    // 在block container集合里插入空标志
                                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                                            put(dataIdentifier.getHash(), null);
                                    return;
                                }
                            } else {
                                // 缓存没有，则请求
                                requestTxForMining(dataIdentifier.getChainID(), horizontalItem.getTxHash(), dataIdentifier.getHash());
                                return;
                            }
                        }

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }

                }
                break;
            }
            case DEMAND_FROM_PEER: {
                if (null == item) {
                    logger.error("DEMAND_FROM_PEER is empty");
                    Long count = this.demandFailure.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                    if (null == count) {
                        count = 1L;
                    } else {
                        count++;
                    }
                    this.demandFailure.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), count);

                    Long total = this.demandSuccess.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                    if (null == total) {
                        total = count;
                    } else {
                        total += count;
                    }
                    logger.info("Demand: Address:{}, failure rate: {} / {} = {}",
                            dataIdentifier.getHash().toString(), count, total, ((float)count / (float)total));

                    return;
                }

                Long count = this.demandSuccess.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                if (null == count) {
                    count = 1L;
                } else {
                    count++;
                }
                this.demandSuccess.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), count);

                Long total = this.demandFailure.get(dataIdentifier.getChainID()).get(dataIdentifier.getHash());
                if (null == total) {
                    total = count;
                } else {
                    total += count;
                }
                logger.info("Demand: Address:{}, success rate: {} / {} = {}",
                        dataIdentifier.getHash().toString(), count, total, ((float)count / (float)total));

                DemandItem demandItem = new DemandItem(item);
                if (demandItem.validate()) {
                    byte[] blockHash = demandItem.getBlockHash();
                    if (null != blockHash) {
                        logger.info("Got a demand block hash:{}", Hex.toHexString(blockHash));
                        requestBlockDemand(dataIdentifier.getChainID(), blockHash);
                    }

                    byte[] txHash = demandItem.getTxHash();
                    if (null != txHash) {
                        logger.info("Got a demand tx hash:{}", Hex.toHexString(txHash));
                        requestTxDemand(dataIdentifier.getChainID(), txHash);
                    }

                    byte[] horizontalHash = demandItem.getHorizontalHash();
                    if (null != horizontalHash) {
                        logger.info("Got a demand horizontal hash:{}", Hex.toHexString(horizontalHash));
                        requestHorizontalItemDemand(dataIdentifier.getChainID(), horizontalHash);
                    }

                    byte[] verticalHash = demandItem.getVerticalHash();
                    if (null != verticalHash) {
                        logger.info("Got a demand vertical hash:{}", Hex.toHexString(verticalHash));
                        requestVerticalItemDemand(dataIdentifier.getChainID(), verticalHash);
                    }
                }

                break;
            }
            case TIP_BLOCK_FROM_PEER_FOR_VOTING: {
                if (null == item) {
                    logger.error("TIP_BLOCK_FROM_PEER_FOR_VOTING is empty");
                    return;
                }

                TipItem tipItem = new TipItem(item);
                byte[] hash = tipItem.getBlockHash();
                if (null != hash) {
                    requestBlockForVoting(dataIdentifier.getChainID(), hash);
                }

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_VOTING: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_VOTING is empty");
                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setBlockHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    return;
                }

                byte[] blockHash = this.localDemandMap.get(dataIdentifier.getChainID()).getBlockHash();
                if (null != blockHash && Arrays.equals(blockHash, dataIdentifier.getHash().getData())) {
                    this.localDemandMap.get(dataIdentifier.getChainID()).clearBlockHash();
                }

                Block block = new Block(item);
                this.votingBlocks.get(dataIdentifier.getChainID()).add(block);

                break;
            }
            case TX_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("TX_REQUEST_FOR_MINING is empty");

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setTxHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    return;
                }

                byte[] txHash = this.localDemandMap.get(dataIdentifier.getChainID()).getTxHash();
                if (null != txHash && Arrays.equals(txHash, dataIdentifier.getHash().getData())) {
                    this.localDemandMap.get(dataIdentifier.getChainID()).clearTxHash();
                }

                Transaction tx = TransactionFactory.parseTransaction(item);
                this.txMapForPool.get(dataIdentifier.getChainID()).add(tx);

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_SYNC is empty, sync block hash:{}",
                            dataIdentifier.getHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setBlockHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 返回区块为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                }

                byte[] blockHash = this.localDemandMap.get(dataIdentifier.getChainID()).getBlockHash();
                if (null != blockHash && Arrays.equals(blockHash, dataIdentifier.getHash().getData())) {
                    this.localDemandMap.get(dataIdentifier.getChainID()).clearBlockHash();
                }

                Block block = new Block(item);

                // 区块加入数据集合作缓存使用，队列满了删除
                this.blockMapForSync.get(dataIdentifier.getChainID()).
                        put(new ByteArrayWrapper(block.getBlockHash()), block);

                BlockContainer blockContainer = new BlockContainer(block);

                boolean success = true;

                if (null != block.getVerticalHash()) {
                    ByteArrayWrapper key = new ByteArrayWrapper(block.getVerticalHash());
                    if (this.verticalItemMapForSync.get(dataIdentifier.getChainID()).containsKey(key)) {
                        VerticalItem verticalItem = this.verticalItemMapForSync.get(dataIdentifier.getChainID()).get(key);
                        if (null != verticalItem) {
                            blockContainer.setVerticalItem(verticalItem);
                        } else {
                            // 移除空item
                            this.verticalItemMapForSync.get(dataIdentifier.getChainID()).remove(key);
                            // 在block container集合里插入空标志
                            this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);
                            success = false;;
                        }
                    } else {
                        requestVerticalItemForSync(dataIdentifier.getChainID(),
                                block.getVerticalHash(), new ByteArrayWrapper(block.getBlockHash()));
                        success = false;
                    }
                }

                if (null != block.getHorizontalHash()) {
                    ByteArrayWrapper key = new ByteArrayWrapper(block.getHorizontalHash());
                    if (this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).containsKey(key)) {
                        HorizontalItem horizontalItem = this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).get(key);
                        if (null != horizontalItem) {
                            if (null != horizontalItem.getTxHash()) {
                                ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());
                                // 先在缓存查找交易
                                if (this.txMapForSync.get(dataIdentifier.getChainID()).containsKey(txKey)) {

                                    Transaction tx = this.txMapForSync.get(dataIdentifier.getChainID()).get(txKey);

                                    if (null != tx) {
                                        blockContainer.setHorizontalItem(horizontalItem);
                                        blockContainer.setTx(tx);
                                    } else {
                                        // 从缓存删掉空交易，非空交易不作删除，等队列满删除
                                        this.txMapForSync.get(dataIdentifier.getChainID()).remove(txKey);
                                        // 在block container集合里插入空标志
                                        this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                                put(dataIdentifier.getHash(), null);
                                        success = false;;
                                    }
                                } else {
                                    // 缓存没有，则请求
                                    requestTxForSync(dataIdentifier.getChainID(), horizontalItem.getTxHash(), dataIdentifier.getHash());
                                    success = false;
                                }
                            }
                        } else {
                            // 移除空item
                            this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).remove(key);
                            // 在block container集合里插入空标志
                            this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);
                            success = false;;
                        }
                    } else {
                        requestHorizontalItemForSync(dataIdentifier.getChainID(),
                                block.getHorizontalHash(), dataIdentifier.getHash());
                        success = false;
                    }
                }

                if (success) {
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), blockContainer);
                }

                break;
            }
            case HISTORY_TX_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_TX_REQUEST_FOR_SYNC is empty, sync tx hash:{}",
                            dataIdentifier.getHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setTxHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                } else {

                    // 数据非空
                    Transaction tx = TransactionFactory.parseTransaction(item);

                    byte[] txHash = this.localDemandMap.get(dataIdentifier.getChainID()).getTxHash();
                    if (null != txHash && Arrays.equals(txHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearTxHash();
                    }

                    // 放入交易集合作缓存
                    this.txMapForSync.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), tx);

                    Block block = this.blockMapForSync.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block);

                        if (null != block.getVerticalHash()) {
                            VerticalItem verticalItem = this.verticalItemMapForSync.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getVerticalHash()));
                            if (null != verticalItem) {
                                blockContainer.setVerticalItem(verticalItem);
                            } else {
                                return;
                            }
                        }

                        if (null != block.getHorizontalHash()) {
                            HorizontalItem horizontalItem = this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getHorizontalHash()));
                            if (null != horizontalItem) {
                                blockContainer.setHorizontalItem(horizontalItem);
                                blockContainer.setTx(tx);
                            } else {
                                return;
                            }
                        }

                        this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }
                }

                break;
            }
            case HISTORY_VERTICAL_ITEM_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_VERTICAL_ITEM_REQUEST_FOR_SYNC is empty, hash:{}, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()), dataIdentifier.getBlockHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setVerticalHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getBlockHash(), null);

                    return;
                } else {
                    byte[] verticalHash = this.localDemandMap.get(dataIdentifier.getChainID()).getVerticalHash();
                    if (null != verticalHash && Arrays.equals(verticalHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearVerticalHash();
                    }

                    VerticalItem verticalItem = new VerticalItem(item);
                    // 放入缓存
                    this.verticalItemMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), verticalItem);

                    Block block = this.blockMapForSync.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    int counter = this.syncCounter.get(dataIdentifier.getChainID()) + 1;
                    this.syncCounter.put(dataIdentifier.getChainID(), counter);

                    if (counter < ChainParam.MUTABLE_RANGE && null != verticalItem.getPreviousHash()) {
                        requestBlockForSync(dataIdentifier.getChainID(), verticalItem.getPreviousHash());
                    }

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block, verticalItem);

                        if (null != block.getHorizontalHash()) {
                            HorizontalItem horizontalItem = this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getHorizontalHash()));
                            if (null != horizontalItem) {
                                if (null != horizontalItem.getTxHash()) {
                                    ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());

                                    Transaction tx = this.txMapForSync.get(dataIdentifier.getChainID()).get(txKey);

                                    if (null != tx) {
                                        blockContainer.setHorizontalItem(horizontalItem);
                                        blockContainer.setTx(tx);
                                    } else {
                                        return;
                                    }
                                }
                            } else {
                                return;
                            }
                        }

                        this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }

                }
                break;
            }
            case HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_SYNC is empty, hash:{}, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()), dataIdentifier.getBlockHash().toString());

                    // 向dht请求
                    this.localDemandMap.get(dataIdentifier.getChainID()).setHorizontalHash(dataIdentifier.getHash().getData());
                    publishDemand(dataIdentifier.getChainID(), this.localDemandMap.get(dataIdentifier.getChainID()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getBlockHash(), null);

                    return;
                } else {
                    byte[] horizontalHash = this.localDemandMap.get(dataIdentifier.getChainID()).getHorizontalHash();
                    if (null != horizontalHash && Arrays.equals(horizontalHash, dataIdentifier.getHash().getData())) {
                        this.localDemandMap.get(dataIdentifier.getChainID()).clearHorizontalHash();
                    }

                    HorizontalItem horizontalItem = new HorizontalItem(item);
                    // 放入缓存
                    this.horizontalItemMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), horizontalItem);

                    Block block = this.blockMapForSync.get(dataIdentifier.getChainID()).get(dataIdentifier.getBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block);

                        if (null != block.getVerticalHash()) {
                            VerticalItem verticalItem = this.verticalItemMapForSync.get(dataIdentifier.getChainID()).
                                    get(new ByteArrayWrapper(block.getVerticalHash()));
                            if (null != verticalItem) {
                                blockContainer.setVerticalItem(verticalItem);
                            } else {
                                return;
                            }
                        }

                        if (null != horizontalItem.getTxHash()) {
                            ByteArrayWrapper txKey = new ByteArrayWrapper(horizontalItem.getTxHash());
                            // 先在缓存查找交易
                            if (this.txMapForSync.get(dataIdentifier.getChainID()).containsKey(txKey)) {

                                Transaction tx = this.txMapForSync.get(dataIdentifier.getChainID()).get(txKey);

                                if (null != tx) {
                                    blockContainer.setHorizontalItem(horizontalItem);
                                    blockContainer.setTx(tx);

                                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                            put(dataIdentifier.getHash(), blockContainer);
                                } else {
                                    // 从缓存删掉空交易，非空交易不作删除，等队列满删除
                                    this.txMapForSync.get(dataIdentifier.getChainID()).remove(txKey);
                                    // 在block container集合里插入空标志
                                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                            put(dataIdentifier.getHash(), null);
                                    return;
                                }
                            } else {
                                // 缓存没有，则请求
                                requestTxForSync(dataIdentifier.getChainID(), horizontalItem.getTxHash(), dataIdentifier.getHash());
                                return;
                            }
                        }

                        this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getBlockHash(), blockContainer);
                    }

                }
                break;
            }
            case HISTORY_BLOCK_DEMAND: {
                if (null == item) {
                    logger.debug("HISTORY_BLOCK_DEMAND is empty");
                    this.blockHashMapFromDemand.get(dataIdentifier.getChainID()).add(dataIdentifier.getHash());
                }

                break;
            }
            case HISTORY_TX_DEMAND: {
                if (null == item) {
                    logger.debug("HISTORY_TX_DEMAND is empty");
                    this.txHashMapFromDemand.get(dataIdentifier.getChainID()).add(dataIdentifier.getHash());
                }

                break;
            }
            case HISTORY_HORIZONTAL_ITEM_DEMAND: {
                if (null == item) {
                    logger.debug("HISTORY_HORIZONTAL_ITEM_DEMAND is empty");
                    this.horizontalHashMapFromDemand.get(dataIdentifier.getChainID()).add(dataIdentifier.getHash());
                }

                break;
            }
            case HISTORY_VERTICAL_ITEM_DEMAND: {
                if (null == item) {
                    logger.debug("HISTORY_VERTICAL_ITEM_DEMAND is empty");
                    this.verticalHashMapFromDemand.get(dataIdentifier.getChainID()).add(dataIdentifier.getHash());
                }

                break;
            }
            default: {
                logger.error("Type mismatch.");
            }
        }
    }

}
