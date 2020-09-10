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
import io.taucoin.db.BlockStore;
import io.taucoin.db.StateDB;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.processor.StateProcessor;
import io.taucoin.processor.StateProcessorImpl;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.types.Block;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
import io.taucoin.types.TypesConfig;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

public class Chains implements DHT.GetDHTItemCallback{
    private static final Logger logger = LoggerFactory.getLogger("Chains");

    // 当前follow的chain ID集合
    private final Set<ByteArrayWrapper> chainIDs = Collections.synchronizedSet(new HashSet<>());

    // 记录等待停止follow的chain ID集合
    private final Set<ByteArrayWrapper> unFollowChainIDs = Collections.synchronizedSet(new HashSet<>());

    // 循环间隔时间
    private final int LOOP_INTERVAL_TIME = 100; // 100 ms

    // mutable item salt: block tip channel
    private final Map<ByteArrayWrapper, byte[]> blockTipSalts = Collections.synchronizedMap(new HashMap<>());

    // mutable item salt: block request channel
    private final Map<ByteArrayWrapper, byte[]> blockDemandSalts = Collections.synchronizedMap(new HashMap<>());

    // mutable item salt: tx tip channel
    private final Map<ByteArrayWrapper, byte[]> txTipSalts = Collections.synchronizedMap(new HashMap<>());

    // mutable item salt: tx request channel
    private final Map<ByteArrayWrapper, byte[]> txDemandSalts = Collections.synchronizedMap(new HashMap<>());

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

    // the synced block of current chain
    private final Map<ByteArrayWrapper, Block> syncBlocks = Collections.synchronizedMap(new HashMap<>());

    // 时间记录器，用于处理定时事件
    private final Map<ByteArrayWrapper, Long> timeRecorders = Collections.synchronizedMap(new HashMap<>());

    // voting pool
    private final Map<ByteArrayWrapper, VotingPool> votingPools = Collections.synchronizedMap(new HashMap<>());

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

    // 同步所用区块容器数据集合: {key: chain ID, value: {key: block hash, value: block container} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, BlockContainer>> blockContainerMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步所用区块数据集合: {key: chain ID, value: {key: block hash, value: block} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Block>> blockMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 同步所用交易数据集合: {key: chain ID, value: {key: tx hash, value: Transaction} }
    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, Transaction>> txMapForSync = Collections.synchronizedMap(new HashMap<>());

    // 远端请求区块哈希数据集合: {key: chain ID, value: block hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> blockHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

    // 远端请求交易哈希数据集合: {key: chain ID, value: tx hash set}
    private final Map<ByteArrayWrapper, Set<ByteArrayWrapper>> txHashMapFromDemand = Collections.synchronizedMap(new HashMap<>());

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
                }

                chainIDs.addAll(this.chainIDs);

                traverseMultiChain(chainIDs);

                chainIDs.clear();

                // TODO:: 研究使用全局chain ID,响应UI操作步骤，延缓统一执行

                try {
                    Thread.sleep(LOOP_INTERVAL_TIME);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * follow a new chain
     * @param chainID chain ID
     * @return true if succeed, false otherwise
     */
    public boolean followChain(byte[] chainID) {
        ByteArrayWrapper wChainID = new ByteArrayWrapper(chainID);

        if (this.chainIDs.contains(wChainID)) {
            logger.info("Chain:{} is followed.", wChainID.toString());
            return true;
        }

        this.blockTipSalts.put(wChainID, makeBlockTipSalt(chainID));

        this.blockDemandSalts.put(wChainID, makeBlockDemandSalt(chainID));

        this.txTipSalts.put(wChainID, makeTxTipSalt(chainID));

        this.txDemandSalts.put(wChainID, makeTxDemandSalt(chainID));

        this.timeRecorders.put(wChainID, 0L);

        // init voting pool
        this.votingPools.put(wChainID, new VotingPool(chainID));

        // init pot consensus
        this.pots.put(wChainID, new ProofOfTransaction(chainID));

        // init state processor
        this.stateProcessors.put(wChainID, new StateProcessorImpl(chainID));

        // init best block and sync block
        try {
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
                this.syncBlocks.put(wChainID, this.blockStore.getBlockByHash(chainID, syncBlockHash));
            }
        } catch (Exception e) {
            logger.error(new String(chainID) + ":" + e.getMessage(), e);
            return false;
        }

        // init peer manager
        PeerManager peerManager = new PeerManager(chainID);
        // get peers form db
        try {
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
                byte[] previousHash = bestBlockContainer.getBlock().getPreviousBlockHash();
                for (int i = 0; i < ChainParam.MUTABLE_RANGE; i++) {
                    Block block = this.blockStore.getBlockByHash(chainID, previousHash);
                    if (null != block) {
                        if (block.getBlockNum() <= 0) {
                            break;
                        }
                        priorityPeers.add(new ByteArrayWrapper(block.getMinerPubkey()));
                        previousHash = block.getPreviousBlockHash();
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
        } catch (Exception e) {
            logger.error(new String(chainID) + ":" + e.getMessage(), e);
            return false;
        }

        this.peerManagers.put(wChainID, peerManager);

        // init tx pool
        TransactionPool txPool = new TransactionPoolImpl(chainID,
                AccountManager.getInstance().getKeyPair().first, this.stateDB);
        txPool.init();

        this.txPools.put(wChainID, txPool);

        this.votingTime.put(wChainID, 0L);

        this.votingFlag.put(wChainID, false);

        this.votingBlocks.put(wChainID, new ArrayList<>());

        this.txMapForPool.put(wChainID, new HashSet<>());

        this.blockContainerMap.put(wChainID, new HashMap<>());

        this.blockMap.put(wChainID, new HashMap<>());

        this.txMap.put(wChainID, new HashMap<>());

        this.blockContainerMapForSync.put(wChainID, new HashMap<>());

        this.blockMapForSync.put(wChainID, new HashMap<>());

        this.txMapForSync.put(wChainID, new HashMap<>());

        this.blockHashMapFromDemand.put(wChainID, new HashSet<>());

        this.txHashMapFromDemand.put(wChainID, new HashSet<>());

        // 把新链放入数据库
        try{
            this.stateDB.followChain(chainID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        // 最后增加添加标记
        this.chainIDs.add(wChainID);

        return true;
    }

    /**
     * 移除链相关的各个组件
     * @param chainID chain ID
     */
    private void removeChainComponent(ByteArrayWrapper chainID) {
        this.blockTipSalts.remove(chainID);

        this.blockDemandSalts.remove(chainID);

        this.txTipSalts.remove(chainID);

        this.txDemandSalts.remove(chainID);

        this.timeRecorders.remove(chainID);

        // init voting pool
        this.votingPools.remove(chainID);

        // init pot consensus
        this.pots.remove(chainID);

        // init state processor
        this.stateProcessors.remove(chainID);

        // init best block and sync block
        this.bestBlockContainers.remove(chainID);
        this.syncBlocks.remove(chainID);

        this.peerManagers.remove(chainID);

        this.txPools.remove(chainID);

        this.votingTime.remove(chainID);

        this.votingFlag.remove(chainID);

        this.votingBlocks.remove(chainID);

        this.txMapForPool.remove(chainID);

        this.blockContainerMap.remove(chainID);

        this.blockMap.remove(chainID);

        this.txMap.remove(chainID);

        this.blockContainerMapForSync.remove(chainID);

        this.blockMapForSync.remove(chainID);

        this.txMapForSync.remove(chainID);

        this.blockHashMapFromDemand.remove(chainID);

        this.txHashMapFromDemand.remove(chainID);
    }

    /**
     * 停止follow一条链
     * @param chainID chain ID
     * @return true if success, false otherwise
     */
    public boolean unFollowChain(byte[] chainID) {

        // 先把停止信息写入数据库
        try{
            this.stateDB.unfollowChain(chainID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        // 再加入待处理集合
        this.unFollowChainIDs.add(new ByteArrayWrapper(chainID));

        return true;
    }

    /**
     * 是否空链
     * @param chainID chain ID
     * @return true if empty chain or off-line for warning range time, false otherwise
     */
    private boolean isEmptyChain(ByteArrayWrapper chainID) {
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);
        // TODO:: if only genesis
        return null == bestBlockContainer || (bestBlockContainer.getBlock().getBlockNum() != 0 &&
                (System.currentTimeMillis() / 1000 - bestBlockContainer.getBlock().getTimeStamp()) >
                        ChainParam.WARNING_RANGE * ChainParam.DEFAULT_BLOCK_TIME);
    }

    /**
     * 对多条链进行一次遍历
     */
    private void traverseMultiChain(Set<ByteArrayWrapper> chainIDs) {
        for (ByteArrayWrapper chainID : chainIDs) {

            logger.debug("Chain ID:{}", new String(chainID.getData()));


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
                    requestTipBlockFromPeer(chainID, peer);
                }
            }

            if (!isEmptyChain(chainID)) {
                // 2. 如果是非空链，进行挖矿等一系列操作

                // 2.1 首先尝试进行一次状态同步，查看是否有之前轮次请求回来的需要同步的数据，有数据则进行链的同步
                tryToSync(chainID);

                // 2.2 判断当前是处在投票阶段
                if (!this.votingFlag.get(chainID)) {
                    // 2.2.1 如果不在投票阶段，则尝试用之前请求回来数据进行切换最难链的操作，
                    // 或者没有之前的数据的情况下，开始请求查找最难链
                    tryToReBranchOrRequest(chainID);
                }

                // 2.3 如果处于投票阶段, 可能是接着之前的轮次投票，或者在查找最难链的过程第一次触发投票，则尝试投票并切换
                if (this.votingFlag.get(chainID)) {
                    Vote bestVote = tryToVote(chainID);
                    if (null != bestVote) {
                        tryToChangeToBestVote(chainID, bestVote);
                    }
                }

                // 2.4 查看是否有请求的交易回到交易池队列，有则把交易放入交易池入池
                TransactionPool txPool = this.txPools.get(chainID);
                for (Transaction tx: this.txMapForPool.get(chainID)) {
                    txPool.addTx(tx);
                }

                // 2.5 尝试挖矿
                tryToMine(chainID);

                // 定时操作，检查是否到时间
                if (System.currentTimeMillis() / 1000 - this.timeRecorders.get(chainID) >= ChainParam.DEFAULT_MAX_BLOCK_TIME) {
                    // 2.6 传播最佳区块
                    publishBestBlock(chainID);

                    // 2.7 传播最佳交易
                    publishBestTx(chainID);

                    // 2.8 随机挑选logN个peer
                    int counter = this.peerManagers.get(chainID).getPeerNumber();
                    counter = counter > 0 ? (int)Math.log(counter) : 0;

                    for (int i = 0; i < counter; i++) {
                        byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();

                        // 2.8.1 请求最佳交易
                        requestTipTxForMining(chainID, peer);

                        // 2.8.2 请求远端区块需求
                        requestDemandBlockFromPeer(chainID, peer);

                        // 2.8.3 请求远端交易需求
                        requestDemandTxFromPeer(chainID, peer);
                    }

                    // 设定新时间起点
                    this.timeRecorders.put(chainID, System.currentTimeMillis() / 1000);
                }

                // 2.9 回应远端需求
                responseDemand(chainID);

                // 2.10 尝试缓存瘦身
                tryToSlimDownCache(chainID);
            }

            try {
                Thread.sleep(LOOP_INTERVAL_TIME);
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
    private void tryToReBranchOrRequest(ByteArrayWrapper chainID) {
        if (this.blockContainerMap.get(chainID).isEmpty()) {
            // 随机挑选一个peer请求最难链
            byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
            requestTipBlockFromPeer(chainID, peer);
        } else {

            boolean clearContainer = true;
            for (Map.Entry<ByteArrayWrapper, BlockContainer> entry : this.blockContainerMap.get(chainID).entrySet()) {
                BlockContainer blockContainer = entry.getValue();

                if (null != blockContainer && blockContainer.getBlock().getCumulativeDifficulty().
                        compareTo(this.bestBlockContainers.get(chainID).getBlock().
                                getCumulativeDifficulty()) > 0) {
                    // 是否需要清除数据，以备下一轮
                    if (!tryToReBranch(chainID, blockContainer)) {
                        clearContainer = false;
                    }
                }
            }

            if (clearContainer) {
                this.blockContainerMap.get(chainID).clear();
            }
        }
    }

    /**
     * 尝试切换分支
     * @param chainID chain ID
     * @param blockContainer block container
     * @return true if need to clear data, such as voting, re-branch whether success or not,
     *          false if need to keep data such as request new data
     */
    private boolean tryToReBranch(ByteArrayWrapper chainID, BlockContainer blockContainer) {

        // if found a more difficult chain
        try {
            byte[] immutableBlockHash1 = blockContainer.getBlock().getImmutableBlockHash();
            if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash1)) {
                // 分叉点在mutable range之内
                int counter = 0;
                byte[] previousHash = blockContainer.getBlock().getPreviousBlockHash();
                List<BlockContainer> containerList = new ArrayList<>();

                containerList.add(blockContainer);

                boolean findAll = true;
                while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {

                    ByteArrayWrapper key = new ByteArrayWrapper(previousHash);

                    // 先从队列查找
                    if (this.blockContainerMap.get(chainID).containsKey(key)) {
                        BlockContainer previousBlockContainer = this.blockContainerMap.get(chainID).get(key);

                        if (null != previousBlockContainer) {
                            // 如果有返回，但是数据不为空
                            containerList.add(previousBlockContainer);
                            previousHash = previousBlockContainer.getBlock().getPreviousBlockHash();

                            if (previousBlockContainer.getBlock().getBlockNum() <= 0) {
                                break;
                            }

                            counter++;

                            continue;
                        } else {
                            // 如果有返回，但是数据为空
                            return true;
                        }
                    }

                    // 再从数据库查找
                    Block block = this.blockStore.getBlockByHash(chainID.getData(), previousHash);
                    if (null != block) {
                        // found in local
                        logger.debug("+ctx--------found in local, hash:{}",
                                Hex.toHexString(previousHash));
                        break;
                    }

                    // 仍然找不到，则向dht请求
                    requestBlockForSync(chainID, previousHash);
                    return false;
                }

                if (findAll) {
                    for (BlockContainer container : containerList) {
                        this.blockStore.saveBlockContainer(chainID.getData(),
                                container, false);
                    }

                    logger.debug("++ctx-----------------------re-branch.....");
                    // change to more difficult chain
                    reBranch(chainID, blockContainer);
                }
            } else {
                // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                ByteArrayWrapper key1 = new ByteArrayWrapper(immutableBlockHash1);

                if (this.blockContainerMap.get(chainID).containsKey(key1)) {
                    BlockContainer blockContainer2 = this.blockContainerMap.get(chainID).get(key1);
                    if (null != blockContainer2) {
                        // 如果有返回，但是数据不为空，继续找第二个immutable block
                        // 获取第二个immutable block hash
                        byte[] immutableBlockHash2 = blockContainer2.
                                getBlock().getImmutableBlockHash();

                        // 第二个immutable block hash是否在主链上
                        if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash2)) {
                            // 在主链上
                            this.votingFlag.put(chainID, true);
                        } else {
                            // 不在主链上，继续查看第三个immutable block hash
                            ByteArrayWrapper key2 = new ByteArrayWrapper(immutableBlockHash2);

                            if (this.blockContainerMap.get(chainID).containsKey(key2)) {
                                BlockContainer blockContainer3 = this.blockContainerMap.get(chainID).get(key2);
                                if (null != blockContainer3) {
                                    // 如果有返回，但是数据不为空
                                    byte[] immutableBlockHash3 = blockContainer3.
                                            getBlock().getImmutableBlockHash();
                                    if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash3)) {
                                        // 在主链上
                                        this.votingFlag.put(chainID, true);
                                    } else {
                                        // fork point out of warning range, maybe it's an attack chain
                                        logger.debug("++ctx-----------------------an attack chain.....");
                                    }
                                }
                            } else {
                                requestBlockForMining(chainID, immutableBlockHash2);
                                return false;
                            }
                        }
                    }
                } else {
                    requestBlockForMining(chainID, immutableBlockHash1);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
        }

        return true;
    }

    /**
     * 尝试对缓存数据集合(block & tx)进行瘦身
     * 瘦身策略：数量超过WARNING RANGE，则删除保留MUTABLE RANGE数量的数据
     * @param chainID chain ID
     */
    private void tryToSlimDownCache(ByteArrayWrapper chainID) {

        // block
        if (this.blockMap.get(chainID).size() > ChainParam.WARNING_RANGE) {
            Map<ByteArrayWrapper, Block> oldBlockMap = this.blockMap.get(chainID);
            Map<ByteArrayWrapper, Block> newBlockMap = new HashMap<>(ChainParam.MUTABLE_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, Block> entry: oldBlockMap.entrySet()) {
                newBlockMap.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.MUTABLE_RANGE) {
                    break;
                }

                i++;
            }

            this.blockMap.put(chainID, newBlockMap);
            oldBlockMap.clear();
        }

        // tx
        if (this.txMap.get(chainID).size() > ChainParam.WARNING_RANGE) {
            Map<ByteArrayWrapper, Transaction> oldTxs = this.txMap.get(chainID);
            Map<ByteArrayWrapper, Transaction> newTxs = new HashMap<>(ChainParam.MUTABLE_RANGE);

            int i = 0;
            for (Map.Entry<ByteArrayWrapper, Transaction> entry: oldTxs.entrySet()) {
                newTxs.put(entry.getKey(), entry.getValue());

                if (i >= ChainParam.MUTABLE_RANGE) {
                    break;
                }

                i++;
            }

            this.txMap.put(chainID, newTxs);
            oldTxs.clear();
        }
    }

    /**
     * 回应发现的需求
     * @param chainID chain ID
     */
    private void responseDemand(ByteArrayWrapper chainID) {
        try {
            // block
            for (ByteArrayWrapper blockHash : this.blockHashMapFromDemand.get(chainID)) {
                Block block = this.blockStore.getBlockByHash(chainID.getData(), blockHash.getData());
                publishBlock(block);
            }
            this.blockHashMapFromDemand.get(chainID).clear();

            // tx
            for (ByteArrayWrapper txid : this.txHashMapFromDemand.get(chainID)) {
                Transaction tx = this.blockStore.getTransactionByHash(chainID.getData(), txid.getData());
                publishTransaction(tx);
            }
            this.txHashMapFromDemand.get(chainID).clear();
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
        }
    }

    /**
     * make block tip salt
     * @param chainID chain ID
     * @return block tip salt
     */
    private byte[] makeBlockTipSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_TIP_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_TIP_CHANNEL.length);
        return salt;
    }

    /**
     * make block demand salt
     * @param chainID chain ID
     * @return block demand salt
     */
    private byte[] makeBlockDemandSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_DEMAND_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_DEMAND_CHANNEL.length);
        return salt;
    }

    /**
     * make tx salt
     * @param chainID chain ID
     * @return tx tip salt
     */
    private byte[] makeTxTipSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.TX_TIP_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_TIP_CHANNEL.length);
        return salt;
    }

    /**
     * make tx demand salt
     * @param chainID chain ID
     * @return tx demand salt
     */
    private byte[] makeTxDemandSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.TX_DEMAND_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_DEMAND_CHANNEL.length);
        return salt;
    }

    /**
     * Is block synchronization uncompleted
     * @param chainID chain ID
     * @return true if uncompleted, false otherwise
     */
    private boolean isSyncUncompleted(ByteArrayWrapper chainID) {
        return null != this.syncBlocks.get(chainID) && this.syncBlocks.get(chainID).getBlockNum() > 0;
    }

    /**
     * 请求同步区块
     * @param chainID chain ID
     */
    private void requestSyncBlock(ByteArrayWrapper chainID) {
        requestBlockForMining(chainID, this.syncBlocks.get(chainID).getBlockHash());
    }

    /**
     * 尝试同步，有同步数据返回则同步，否则，不同步
     * @param chainID chain ID
     */
    private void tryToSync(ByteArrayWrapper chainID) {
        // 合法性判断
        if (isSyncUncompleted(chainID)) {
            BlockContainer blockContainer = this.blockContainerMapForSync.get(chainID).
                    get(new ByteArrayWrapper(this.syncBlocks.get(chainID).getPreviousBlockHash()));

            syncBlock(chainID, blockContainer);
        }
    }

    /**
     * 同步区块
     * @param chainID chain ID
     * @param blockContainer block container
     * @return true if succeed, false otherwise
     */
    private boolean syncBlock(ByteArrayWrapper chainID, BlockContainer blockContainer) {

        if (null == blockContainer) {
            return false;
        }

        try {
            StateDB track = this.stateDB.startTracking(chainID.getData());
            StateProcessor stateProcessor = this.stateProcessors.get(chainID);
            if (stateProcessor.backwardProcess(blockContainer, track)) {
                // after sync
                // 1. save block
                // 2. save sync block hash
                // 3. commit new state
                // 4. set sync block
                // 5. add old block peer to peer pool

                this.blockStore.saveBlockContainer(chainID.getData(), blockContainer, true);
                track.setSyncBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());
                track.commit();
                this.syncBlocks.put(chainID, blockContainer.getBlock());
                this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 尝试进行一次挖矿
     * @param chainID chain ID
     */
    private void tryToMine(ByteArrayWrapper chainID) {
        if (minable(chainID)) {
            BlockContainer blockContainer = mineBlock(chainID);

            if (null != blockContainer) {
                StateDB track = this.stateDB.startTracking(chainID.getData());

                if (tryToConnect(chainID, blockContainer, track)) {
                    try {
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
                    } catch (Exception e) {
                        logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
                    }

                    Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);
                    txPools.get(chainID).recheckAccoutTx(accounts);

                    publishBestBlock(chainID);
                }
            }
        }
    }

    /**
     * re-branch chain
     * @param targetBlockContainer block that chain will change to
     */
    private boolean reBranch(ByteArrayWrapper chainID, BlockContainer targetBlockContainer) {
        try {
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

                return false;
            }

            StateProcessor stateProcessor = this.stateProcessors.get(chainID);

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (!stateProcessor.rollback(undoBlockContainer, track)) {
                    logger.error("Chain ID[{}]: Roll back fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(undoBlockContainer.getBlock().getBlockHash()));
                    return false;
                }
            }

            int size = newBlockContainers.size();
            for (int i = size - 1; i >= 0; i--) {

                if (!isValidBlockContainer(chainID, newBlockContainers.get(i), track)) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
                }

                ImportResult result = stateProcessor.forwardProcess(newBlockContainers.get(i), track);
                // if need sync more block
                if (result == ImportResult.NO_ACCOUNT_INFO && isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                    return false;
                }

                if (result != ImportResult.IMPORTED_BEST) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
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

            publishBestBlock(chainID);

            // update tx pool
            TransactionPool txPool = this.txPools.get(chainID);

            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(undoBlockContainers);
            accounts.addAll(extractAccountFromBlockContainer(newBlockContainers));
            txPool.recheckAccoutTx(accounts);

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (null != undoBlockContainer.getTx()) {
                    txPool.addTx(undoBlockContainer.getTx());
                }
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
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

        ByteArrayWrapper blockKey = new ByteArrayWrapper(bestTipBlock.getBlockHash());
        this.blockMap.get(chainID).put(blockKey, bestTipBlock);

        if (null != bestTipBlock.getTxHash()) {
            ByteArrayWrapper key = new ByteArrayWrapper(bestTipBlock.getTxHash());
            Transaction tx = this.txMap.get(chainID).get(key);
            if (null != tx) {
                this.blockContainerMap.get(chainID).
                        put(key, new BlockContainer(bestTipBlock, tx));
            } else {
                requestTxForMining(chainID, bestTipBlock.getTxHash(), blockKey);
            }
        } else {
            this.blockContainerMap.get(chainID).
                    put(new ByteArrayWrapper(bestTipBlock.getBlockHash()),
                            new BlockContainer(bestTipBlock));
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

        counter = counter > 0 ? (int)Math.log(counter) : 0;

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

                    try {
                        Thread.sleep(LOOP_INTERVAL_TIME);
                    } catch (InterruptedException e) {
                        logger.info(new String(chainID.getData()) + ":" + e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
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
    private boolean initChain(ByteArrayWrapper chainID, BlockContainer blockContainer) {
        if (null == blockContainer) {
            logger.error("Chain ID[{}]: Block container is null.", new String(chainID.getData()));
            return false;
        }

        try {
            this.blockStore.removeChain(chainID.getData());
            this.stateDB.clearAllState(chainID.getData());

            // after sync
            // 1. save block
            // 2. save best and sync block hash
            // 3. commit new state
            // 4. set best and sync block
            // 5. add old block peer to peer pool

            // initial sync from best vote
            StateDB track = this.stateDB.startTracking(chainID.getData());
            StateProcessor stateProcessor = this.stateProcessors.get(chainID);
            if (!stateProcessor.backwardProcess(blockContainer, track)) {
                logger.error("Chain ID[{}]: Process block[{}] fail!",
                        new String(chainID.getData()), Hex.toHexString(blockContainer.getBlock().getBlockHash()));
                return false;
            }

            this.blockStore.saveBlockContainer(chainID.getData(), blockContainer, true);

            track.setBestBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());
            track.setSyncBlockHash(chainID.getData(), blockContainer.getBlock().getBlockHash());

            this.bestBlockContainers.put(chainID, blockContainer);
            this.syncBlocks.put(chainID, blockContainer.getBlock());

            this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());

            track.commit();
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 重置链，将状态归零，需要清楚的组件相应重置
     * @param chainID chain ID
     */
    private void resetChain(ByteArrayWrapper chainID) {
        try {
            this.blockStore.removeChain(chainID.getData());
            this.stateDB.clearAllState(chainID.getData());

            this.txPools.get(chainID).reinit();
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
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
    private boolean tryToChangeToBestVote(ByteArrayWrapper chainID, Vote bestVote) {
        try {
            Map<ByteArrayWrapper, BlockContainer> blockContainers = this.blockContainerMap.get(chainID);

            BlockContainer blockContainer1 = this.blockStore.
                    getBlockContainerByHash(chainID.getData(), bestVote.getBlockHash());

            if (null == blockContainer1) {

                ByteArrayWrapper key = new ByteArrayWrapper(bestVote.getBlockHash());

                if (blockContainers.containsKey(key)) {

                    blockContainer1 = blockContainers.get(key);

                    if (null == blockContainer1) {

                        resetAfterVoting(chainID);

                        return false;
                    }
                } else {
                    requestBlockForMining(chainID, bestVote.getBlockHash());
                    return false;
                }
            }

            // if best vote is genesis block, immutable hash is not on main chain, also we cannot
            // get block container by immutable hash from genesis block

            byte[] immutableBlockHash1 = blockContainer1.getBlock().getImmutableBlockHash();

            if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash1)) {
                // 分叉点在mutable range之内
                int counter = 0;
                byte[] previousHash = blockContainer1.getBlock().getPreviousBlockHash();
                List<BlockContainer> containerList = new ArrayList<>();

                containerList.add(blockContainer1);

                boolean findAll = true;
                while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {

                    ByteArrayWrapper key = new ByteArrayWrapper(previousHash);

                    // 先从队列查找
                    if (blockContainers.containsKey(key)) {
                        BlockContainer previousBlockContainer = blockContainers.get(key);

                        if (null != previousBlockContainer) {
                            // 如果有返回，但是数据不为空
                            containerList.add(previousBlockContainer);
                            previousHash = previousBlockContainer.getBlock().getPreviousBlockHash();

                            if (previousBlockContainer.getBlock().getBlockNum() <= 0) {
                                break;
                            }

                            counter++;

                            continue;
                        } else {
                            // 如果有返回，但是数据为空
                            resetAfterVoting(chainID);
                            return false;
                        }
                    }

                    // 再从数据库查找
                    Block block = this.blockStore.getBlockByHash(chainID.getData(), previousHash);
                    if (null != block) {
                        // found in local
                        logger.debug("+ctx--------found in local, hash:{}",
                                Hex.toHexString(previousHash));
                        break;
                    }

                    // 仍然找不到，则向dht请求
                    requestBlockForSync(chainID, previousHash);
                    findAll = false;
                    break;
                }

                if (findAll) {
                    for (BlockContainer container : containerList) {
                        this.blockStore.saveBlockContainer(chainID.getData(),
                                container, false);
                    }

                    logger.debug("++ctx-----------------------re-branch.....");
                    // change to more difficult chain
                    if (reBranch(chainID, blockContainer1)) {
                        // 如果成功切换到投票分支，则选择一个immutable block hash和best vote一致的难度值最高的区块
                        // 将其加入最长链待处理的数据集合
                        chooseBestBlockAsTipAfterVoting(chainID);
                    }

                    // 重置状态数据等，以待下次使用
                    resetAfterVoting(chainID);
                } else {
                    containerList.clear();
                }
            } else {
                // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                ByteArrayWrapper key1 = new ByteArrayWrapper(immutableBlockHash1);

                if (blockContainers.containsKey(key1)) {

                    BlockContainer blockContainer2 = blockContainers.get(key1);

                    if (null != blockContainer2) {
                        // 如果有返回，但是数据不为空，继续找第二个immutable block
                        // 获取第二个immutable block hash
                        byte[] immutableBlockHash2 = blockContainer2.
                                getBlock().getImmutableBlockHash();

                        // 第二个immutable block hash是否在主链上
                        if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash2)) {
                            // 在主链上
                            // be as a new chain when fork point between mutable range and warning range
                            resetChain(chainID);
                            return true;
                        } else {
                            // 不在主链上，继续查看第三个immutable block hash
                            ByteArrayWrapper key2 = new ByteArrayWrapper(immutableBlockHash2);

                            if (blockContainers.containsKey(key2)) {
                                BlockContainer blockContainer3 = blockContainers.get(key2);
                                if (null != blockContainer3) {
                                    // 如果有返回，但是数据不为空
                                    byte[] immutableBlockHash3 = blockContainer3.
                                            getBlock().getImmutableBlockHash();
                                    if (this.blockStore.isMainChainBlock(chainID.getData(), immutableBlockHash3)) {
                                        // 在主链上
                                        // be as a new chain when fork point between mutable range and warning range
                                        resetChain(chainID);
                                        return true;
                                    } else {
                                        // fork point out of warning range, maybe it's an attack chain
                                        logger.debug("++ctx-----------------------an attack chain.....");
                                    }
                                } else {
                                    // 如果有返回，但是数据为空
                                    resetAfterVoting(chainID);
                                }
                            } else {
                                requestBlockForMining(chainID, immutableBlockHash2);
                            }
                        }
                    } else {
                        // 如果有返回，但是数据为空
                        resetAfterVoting(chainID);
                    }
                } else {
                    requestBlockForMining(chainID, immutableBlockHash1);
                }
            }

        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 尝试从本地缓存或者数据库获取block container，如果没有则请求
     * @param chainID chain ID
     * @return block container or null
     */
    private BlockContainer tryToGetBlockContainerFromLocal(ByteArrayWrapper chainID, byte[] blockHash) {
        BlockContainer blockContainer = null;

        try {
            blockContainer = this.blockStore.getBlockContainerByHash(chainID.getData(), blockHash);

            if (null == blockContainer) {
                ByteArrayWrapper key = new ByteArrayWrapper(blockHash);
                blockContainer = this.blockContainerMap.get(chainID).get(key);

                if (null == blockContainer) {
                    Block block = this.blockMap.get(chainID).get(key);
                    if (null != block) {
                        if (null != block.getTxHash()) {
                            // 区块带有交易
                            Transaction tx = this.txMap.get(chainID).get(new ByteArrayWrapper(block.getTxHash()));
                            if (null != tx) { // 本地找到了交易
                                blockContainer = new BlockContainer(block, tx);
                            } else {
                                // 本地没找到， 请求交易
                                requestTxForMining(chainID, block.getTxHash(), key);
                            }
                        } else {
                            // 空区块
                            blockContainer = new BlockContainer(block);
                        }
                    } else {
                        requestBlockForMining(chainID, blockHash);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
        }

        return blockContainer;
    }

    private void requestTipBlockFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockTipSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK_FROM_PEER_FOR_MINING);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestDemandBlockFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockDemandSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.BLOCK_DEMAND_FROM_PEER);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestBlockForMining(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_REQUEST_FOR_MINING,
                new ByteArrayWrapper(blockHash));

        publishBlockRequest(chainID, blockHash);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestBlockForSync(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_REQUEST_FOR_SYNC);

        publishBlockRequest(chainID, blockHash);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestDemandTxFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txDemandSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX_DEMAND_FROM_PEER);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestTxForMining(ByteArrayWrapper chainID, byte[] txid, ByteArrayWrapper txBlockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_TX_REQUEST_FOR_MINING, new ByteArrayWrapper(txid), txBlockHash);

        publishTxRequest(chainID, txid);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestTxForSync(ByteArrayWrapper chainID, byte[] txid, ByteArrayWrapper txBlockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_TX_REQUEST_FOR_SYNC, new ByteArrayWrapper(txid), txBlockHash);

        publishTxRequest(chainID, txid);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestTipTxForMining(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txTipSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_TX_FOR_MINING);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestTxForPool(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX_REQUEST_FOR_MINING);

        publishTxRequest(chainID, txid);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestTipBlockForVotingFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockTipSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK_FROM_PEER_FOR_VOTING);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestBlockForVoting(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.HISTORY_BLOCK_REQUEST_FOR_VOTING);

        publishBlockRequest(chainID, blockHash);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void publishBlockRequest(ByteArrayWrapper chainID, byte[] blockHash) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                keyPair.second, ByteUtil.getHashEncoded(blockHash), this.blockDemandSalts.get(chainID));
        TorrentDHTEngine.getInstance().distribute(mutableItem);
    }

    private void publishTxRequest(ByteArrayWrapper chainID, byte[] txHash) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                keyPair.second, ByteUtil.getHashEncoded(txHash), this.txDemandSalts.get(chainID));
        TorrentDHTEngine.getInstance().distribute(mutableItem);
    }

    private void publishBlock(Block block) {
        if (null != block) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(block.getEncoded());
            TorrentDHTEngine.getInstance().distribute(immutableItem);
        }
    }

    private void publishTransaction(Transaction tx) {
        if (null != tx) {
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
            TorrentDHTEngine.getInstance().distribute(immutableItem);
        }
    }

    /**
     * publish tip block on main chain to dht
     * @param chainID chain ID
     */
    private void publishBestBlock(ByteArrayWrapper chainID) {
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

        if (null != bestBlockContainer) {
            if (null != bestBlockContainer.getTx()) {
                // put immutable tx
                DHT.ImmutableItem immutableItem =
                        new DHT.ImmutableItem(bestBlockContainer.getTx().getEncoded());
                TorrentDHTEngine.getInstance().distribute(immutableItem);
            }

            // put immutable block
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(bestBlockContainer.getBlock().getEncoded());
            TorrentDHTEngine.getInstance().distribute(immutableItem);

            // put mutable item
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    ByteUtil.getHashEncoded(bestBlockContainer.getBlock().getBlockHash()),
                    this.blockTipSalts.get(chainID));
            TorrentDHTEngine.getInstance().distribute(mutableItem);
        }
    }

    /**
     * publish tip block on main chain to dht
     * @param chainID chain ID
     */
    private void publishBestTx(ByteArrayWrapper chainID) {
        Transaction tx = this.txPools.get(chainID).getBestTransaction();
        if (null != tx) {
            // put mutable tx
            publishTipTransaction(chainID, tx);
        }
    }

    /**
     * put a tx in mutable item
     * @param chainID chain ID
     * @param tx tx to publish
     */
    private void publishTipTransaction(ByteArrayWrapper chainID, Transaction tx) {
        if (null != tx) {
            // put immutable tx first
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
            TorrentDHTEngine.getInstance().distribute(immutableItem);

            // put mutable item

            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    ByteUtil.getHashEncoded(tx.getTxID()), this.txTipSalts.get(chainID));
            TorrentDHTEngine.getInstance().distribute(mutableItem);
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
                blockContainer.getBlock().getPreviousBlockHash())) {
            // main chain
            if (!isValidBlockContainer(chainID, blockContainer, stateDB)) {
                return false;
            }

            ImportResult result = stateProcessors.get(chainID).forwardProcess(blockContainer, stateDB);
            return result.isSuccessful();
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
     * check if a block valid
     * @param chainID chain ID
     * @param block block to check
     * @param stateDB state db
     * @return true if valid, false otherwise
     */
    private boolean isValidBlock(ByteArrayWrapper chainID, Block block, StateDB stateDB) {
//        // 是否本链
//        if (!Arrays.equals(chainID.getData(), block.getChainID())) {
//            logger.error("ChainID[{}]: ChainID mismatch!", new String(chainID.getData()));
//            return false;
//        }

        // 时间戳检查
        if (block.getTimeStamp() > System.currentTimeMillis() / 1000) {
            logger.error("ChainID[{}]: Block[{}] Time is in the future!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 区块内部自检
        if (!block.isBlockParamValidate()) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 区块签名检查
        if (!block.verifyBlockSig()) {
            logger.error("ChainID[{}]: Block[{}] Bad Signature!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 是否孤块
        try {
            if (null == this.blockStore.getBlockByHash(chainID.getData(), block.getPreviousBlockHash())) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        // POT共识验证
        if (!verifyPOT(chainID, block, stateDB)) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        return true;
    }

    /**
     * check if a block container valid
     * @param chainID chain ID
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if valid, false otherwise
     */
    private boolean isValidBlockContainer(ByteArrayWrapper chainID,
                                          BlockContainer blockContainer, StateDB stateDB) {
        return isValidBlock(chainID, blockContainer.getBlock(), stateDB);
    }

    /**
     * check pot consensus
     * @param chainID chain ID
     * @param block block to check
     * @param stateDB state db
     * @return true if ok, false otherwise
     */
    private boolean verifyPOT(ByteArrayWrapper chainID, Block block, StateDB stateDB) {
        try {
            ProofOfTransaction pot = this.pots.get(chainID);

            byte[] pubKey = block.getMinerPubkey();

            BigInteger power = stateDB.getNonce(chainID.getData(), pubKey);
            if (null == power) {
                logger.error("ChainID[{}]: Miner[{}] has no power!",
                        new String(chainID.getData()), Hex.toHexString(pubKey));
                return false;
            }
            logger.info("Chain ID[{}]: Address: {}, mining power: {}",
                    new String(chainID.getData()), Hex.toHexString(pubKey), power);

            Block parentBlock = this.blockStore.getBlockByHash(chainID.getData(), block.getPreviousBlockHash());
            if (null == parentBlock) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check base target
            BigInteger baseTarget = pot.calculateRequiredBaseTarget(chainID.getData(), parentBlock, this.blockStore);
            if (0 != baseTarget.compareTo(block.getBaseTarget())) {
                logger.error("ChainID[{}]: Block[{}] base target error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check generation signature
            byte[] genSig = pot.calculateGenerationSignature(parentBlock.getGenerationSignature(), pubKey);
            if (!Arrays.equals(genSig, block.getGenerationSignature())) {
                logger.error("ChainID[{}]: Block[{}] generation signature error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check cumulative difficulty
            BigInteger culDifficulty = pot.calculateCumulativeDifficulty(
                    parentBlock.getCumulativeDifficulty(), baseTarget);
            if (0 != culDifficulty.compareTo(block.getCumulativeDifficulty())) {
                logger.error("ChainID[{}]: Block[{}] Cumulative difficulty error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check if target >= hit
//            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
//                    block.getTimeStamp() - parentBlock.getTimeStamp());

            BigInteger hit = pot.calculateRandomHit(genSig);
            long timeInterval = block.getTimeStamp() - parentBlock.getTimeStamp();

            // verify hit
            if (!pot.verifyHit(hit, baseTarget, power, timeInterval)) {
                logger.error("ChainID[{}]: The block[{}] does not meet the pot consensus!!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return false;
            }

        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * check if be able to mine now
     * @param chainID chain ID
     * @return true if can mine, false otherwise
     */
    private boolean minable(ByteArrayWrapper chainID) {
        try {
            ProofOfTransaction pot = this.pots.get(chainID);
            BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            if (null == pubKey) {
                logger.info("Chain ID[{}]: PubKey is null.", new String(chainID.getData()));
                return false;
            }

            BigInteger power = this.stateDB.getNonce(chainID.getData(), pubKey);
            if (null == power || power.longValue() <= 0) {
                logger.info("Chain ID[{}]: PubKey[{}]-No mining power.",
                        new String(chainID.getData()), Hex.toHexString(pubKey));
                return false;
            }

            logger.info("ChainID[{}]: PubKey[{}] mining power: {}",
                    new String(chainID.getData()), Hex.toHexString(pubKey), power);

            // check base target
            BigInteger baseTarget = pot.calculateRequiredBaseTarget(chainID.getData(),
                    bestBlockContainer.getBlock(), this.blockStore);

            // check generation signature
            byte[] genSig = pot.calculateGenerationSignature(bestBlockContainer.
                    getBlock().getGenerationSignature(), pubKey);

            // check if target >= hit
//            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
//                    System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp());

            BigInteger hit = pot.calculateRandomHit(genSig);

            long timeInterval = pot.calculateMiningTimeInterval(hit, baseTarget, power);
            if ((System.currentTimeMillis() / 1000 - bestBlockContainer.getBlock().getTimeStamp()) < timeInterval) {
                logger.info("Chain ID[{}]: It's not time for the block.", new String(chainID.getData()));
                return false;
            }

//            if (target.compareTo(hit) < 0) {
//                logger.info("ChainID[{}]: Target[{}] value is smaller than hit[{}]!!",
//                        new String(this.chainID), target, hit);
//                return false;
//            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * mine a block
     * @param chainID chain ID
     * @return block container, or null
     */
    private BlockContainer mineBlock(ByteArrayWrapper chainID) {
        ProofOfTransaction pot = this.pots.get(chainID);
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);
        TransactionPool txPool = this.txPools.get(chainID);
        StateProcessor stateProcessor = this.stateProcessors.get(chainID);

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(chainID.getData(),
                bestBlockContainer.getBlock(), this.blockStore);
        byte[] generationSignature = pot.calculateGenerationSignature(
                bestBlockContainer.getBlock().getGenerationSignature(),
                AccountManager.getInstance().getKeyPair().first);
        BigInteger cumulativeDifficulty = pot.calculateCumulativeDifficulty(
                bestBlockContainer.getBlock().getCumulativeDifficulty(),
                baseTarget);

        byte[] immutableBlockHash;
        try {
            // if current block number is larger than mutable range
            if (bestBlockContainer.getBlock().getBlockNum() + 1 >= ChainParam.MUTABLE_RANGE) {
                immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(),
                        bestBlockContainer.getBlock().getBlockNum() + 1 - ChainParam.MUTABLE_RANGE);
            } else {
                immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), 0);
//                immutableBlockHash = new byte[ChainParam.HashLength];
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return null;
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

        Transaction tx = txPool.getBestTransaction();

        Block block;
        logger.debug("-------------Previous hash:{}", Hex.toHexString(bestBlockContainer.getBlock().getBlockHash()));
        if (null != tx) {
            block = new Block((byte) 1, System.currentTimeMillis() / 1000,
                    bestBlockContainer.getBlock().getBlockNum() + 1,
                    bestBlockContainer.getBlock().getBlockHash(), immutableBlockHash,
                    baseTarget, cumulativeDifficulty, generationSignature, tx.getTxID(),
                    0, 0, 0, 0, keyPair.first);
        } else {
            block = new Block((byte) 1, System.currentTimeMillis() / 1000,
                    bestBlockContainer.getBlock().getBlockNum() + 1,
                    bestBlockContainer.getBlock().getBlockHash(), immutableBlockHash,
                    baseTarget, cumulativeDifficulty, generationSignature, null,
                    0, 0, 0, 0, keyPair.first);
        }

        BlockContainer blockContainer = new BlockContainer(block, tx);

        // set state
        StateDB miningTrack = this.stateDB.startTracking(chainID.getData());
        stateProcessor.forwardProcess(blockContainer, miningTrack);

        try {
            // set state
            AccountState minerState = miningTrack.getAccount(chainID.getData(),
                    AccountManager.getInstance().getKeyPair().first);
            block.setMinerBalance(minerState.getBalance().longValue());

            if (null != tx) {
                AccountState senderState = miningTrack.getAccount(chainID.getData(),
                        tx.getSenderPubkey());
                block.setSenderBalance(senderState.getBalance().longValue());
                block.setSenderNonce(senderState.getNonce().longValue());

                if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {

                    AccountState receiverState = miningTrack.getAccount(chainID.getData(),
                            ((WiringCoinsTx)tx).getReceiver());

                    block.setReceiverBalance(receiverState.getBalance().longValue());
                }
            }

            // sign
            block.signBlock(keyPair.second);
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return null;
        }

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

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {

        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case TIP_BLOCK_FROM_PEER_FOR_MINING: {
                if (null == item) {
                    return;
                }

                requestBlockForMining(dataIdentifier.getChainID(), ByteUtil.getHashFromEncode(item));

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_MINING: {
                if (null == item) {
                    // 返回区块为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                }

                Block block = new Block(item);

                if (null != block.getTxHash()) {

                    // 如果区块带有交易
                    ByteArrayWrapper key = new ByteArrayWrapper(block.getTxHash());

                    // 先在缓存查找交易
                    if (this.txMap.get(dataIdentifier.getChainID()).containsKey(key)) {

                        Transaction tx = this.txMap.get(dataIdentifier.getChainID()).get(key);

                        if (null != tx) {
                            BlockContainer blockContainer = new BlockContainer(block, tx);

                            this.blockContainerMap.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), blockContainer);
                        } else {
                            // 如果是空交易
                            // 在block container集合里插入空标志
                            this.blockContainerMap.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);

                            // 删掉空交易，非空交易不作删除，等队列满删除
                            this.txMap.remove(key);
                        }
                    } else {
                        // 缓存没有，则请求
                        requestTxForMining(dataIdentifier.getChainID(), block.getTxHash(), dataIdentifier.getHash());
                    }
                } else {
                    // 无交易区块，直接加入block container集合等待处理
                    BlockContainer blockContainer = new BlockContainer(block);

                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), blockContainer);
                }

                // 区块加入数据集合作缓存使用，队列满了删除
                this.blockMap.get(dataIdentifier.getChainID()).
                        put(new ByteArrayWrapper(block.getBlockHash()), block);

                break;
            }
            case HISTORY_TX_REQUEST_FOR_MINING: {
                if (null == item) {
                    // 如果区块也是空， 目前不可能
//                    Map<ByteArrayWrapper, Block> blockMap = this.blockMap.get(dataIdentifier.getChainID());
//                    if (blockMap.containsKey(dataIdentifier.getTxBlockHash())) {
//                        if (null == blockMap.get(dataIdentifier.getTxBlockHash())) {
//                            blockMap.remove(dataIdentifier.getTxBlockHash());
//                        }
//                    }

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);

                    return;
                } else {
                    // 数据非空

                    Transaction tx = TransactionFactory.parseTransaction(item);

                    Block block = this.blockMap.get(dataIdentifier.getChainID()).get(dataIdentifier.getTxBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block, tx);

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);
                    }

                    // 放入交易集合作缓存
                    this.txMap.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), tx);
                }

                break;
            }
            case BLOCK_DEMAND_FROM_PEER: {
                if (null == item) {
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                this.blockHashMapFromDemand.get(dataIdentifier.getChainID()).
                        add(new ByteArrayWrapper(hash));
                break;
            }
            case TX_DEMAND_FROM_PEER: {
                if (null == item) {
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                this.txHashMapFromDemand.get(dataIdentifier.getChainID()).
                        add(new ByteArrayWrapper(hash));
                break;
            }
            case TIP_BLOCK_FROM_PEER_FOR_VOTING: {
                if (null == item) {
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                requestBlockForVoting(dataIdentifier.getChainID(), hash);
                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_VOTING: {
                if (null == item) {
                    return;
                }

                Block block = new Block(item);
                this.votingBlocks.get(dataIdentifier.getChainID()).add(block);
                break;
            }
            case TIP_TX_FOR_MINING: {
                if (null == item) {
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                requestTxForPool(dataIdentifier.getChainID(), hash);
                break;
            }
            case TX_REQUEST_FOR_MINING: {
                if (null == item) {
                    return;
                }

                Transaction tx = TransactionFactory.parseTransaction(item);
                this.txMapForPool.get(dataIdentifier.getChainID()).add(tx);
                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_SYNC: {
                if (null == item) {
                    // 返回区块为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                }

                Block block = new Block(item);

                if (null != block.getTxHash()) {

                    ByteArrayWrapper key = new ByteArrayWrapper(block.getTxHash());

                    if (this.txMapForSync.get(dataIdentifier.getChainID()).containsKey(key)) {

                        Transaction tx = this.txMapForSync.get(dataIdentifier.getChainID()).get(key);

                        if (null != tx) {
                            BlockContainer blockContainer = new BlockContainer(block, tx);

                            this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), blockContainer);
                        } else {
                            // 如果是空交易
                            // 在block container集合里插入空标志
                            this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);

                            // 删掉空交易，非空交易不作删除，等队列满删除
                            this.txMapForSync.remove(key);
                        }
                    } else {
                        requestTxForSync(dataIdentifier.getChainID(), block.getTxHash(), dataIdentifier.getHash());
                    }
                } else {
                    // 无交易区块，直接加入block container集合等待处理
                    BlockContainer blockContainer = new BlockContainer(block);

                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), blockContainer);
                }

                // 区块加入数据集合作缓存使用，队列满了删除
                this.blockMapForSync.get(dataIdentifier.getChainID()).
                        put(new ByteArrayWrapper(block.getBlockHash()), block);

                break;
            }
            case HISTORY_TX_REQUEST_FOR_SYNC: {
                if (null == item) {
                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                } else {

                    Transaction tx = TransactionFactory.parseTransaction(item);

                    Block block = this.blockMapForSync.get(dataIdentifier.getChainID()).
                            get(dataIdentifier.getTxBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block, tx);

                        this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                                put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);
                    }

                    // 放入交易集合作缓存
                    this.txMapForSync.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), tx);
                }

                break;
            }
            default: {
                logger.error("Type mismatch.");
            }
        }
    }

}
