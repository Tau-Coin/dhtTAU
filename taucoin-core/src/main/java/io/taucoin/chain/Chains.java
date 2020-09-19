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
    private final int LOOP_INTERVAL_TIME = 50; // 50 ms

//    // mutable item salt: block tip channel
//    private final Map<ByteArrayWrapper, byte[]> blockTipSalts = Collections.synchronizedMap(new HashMap<>());
//
//    // mutable item salt: block request channel
//    private final Map<ByteArrayWrapper, byte[]> blockDemandSalts = Collections.synchronizedMap(new HashMap<>());
//
//    // mutable item salt: tx tip channel
//    private final Map<ByteArrayWrapper, byte[]> txTipSalts = Collections.synchronizedMap(new HashMap<>());
//
//    // mutable item salt: tx request channel
//    private final Map<ByteArrayWrapper, byte[]> txDemandSalts = Collections.synchronizedMap(new HashMap<>());

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

    // 控制是否自己挖矿还是只同步
    private final Map<ByteArrayWrapper, Boolean> enableMineForTest = Collections.synchronizedMap(new HashMap<>());

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
    public boolean followChain(byte[] chainID, List<byte[]> peerList) {

        ByteArrayWrapper wChainID = new ByteArrayWrapper(chainID);

        if (null == peerList || peerList.isEmpty()) {
            logger.info("Chain:{} no peers.", wChainID.toString());
            return false;
        }

        try {
            for (byte[] peer : peerList) {
                this.stateDB.addPeer(chainID, peer);
            }
        } catch (Exception e) {
            logger.error(new String(chainID) + ":" + e.getMessage(), e);
            return false;
        }

        startChain(chainID);

        return true;
    }

    /**
     * follow a new chain
     * @param chainID chain ID
     * @return true if succeed, false otherwise
     */
    public boolean startChain(byte[] chainID) {
        ByteArrayWrapper wChainID = new ByteArrayWrapper(chainID);

        if (this.chainIDs.contains(wChainID)) {
            logger.info("Chain:{} is followed.", wChainID.toString());
            return true;
        }

//        this.blockTipSalts.put(wChainID, makeBlockTipSalt(chainID));
//
//        this.blockDemandSalts.put(wChainID, makeBlockDemandSalt(chainID));
//
//        this.txTipSalts.put(wChainID, makeTxTipSalt(chainID));
//
//        this.txDemandSalts.put(wChainID, makeTxDemandSalt(chainID));

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

        this.findingTime.put(wChainID, 0L);

        this.miningFlag.put(wChainID, true);

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

        this.enableMineForTest.put(wChainID, true);

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
     * remove all chain info in database
     * @param chainID chain ID
     */
    private void removeAllChainInfoInDB(ByteArrayWrapper chainID) {
        try {
            this.blockStore.removeChainInfo(chainID.getData());
            this.stateDB.clearAllState(chainID.getData());
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + e.getMessage(), e);
        }
    }

    /**
     * 移除链相关的各个组件
     * @param chainID chain ID
     */
    private void removeChainComponent(ByteArrayWrapper chainID) {
//        this.blockTipSalts.remove(chainID);
//
//        this.blockDemandSalts.remove(chainID);
//
//        this.txTipSalts.remove(chainID);
//
//        this.txDemandSalts.remove(chainID);

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

        this.findingTime.remove(chainID);

        this.miningFlag.remove(chainID);

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

        this.enableMineForTest.remove(chainID);
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
    private void traverseMultiChain(Set<ByteArrayWrapper> chainIDs) {
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
                    requestTipBlockFromPeer(chainID, peer);
                }
            }

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
                            requestTipBlockFromPeer(chainID, peer);
                        }
                    }
                }
            }

            // 2. 如果是非空链，并且允许挖矿，进行挖矿等一系列操作
            if (this.miningFlag.get(chainID) && !isEmptyChain(chainID)) {

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
                        tryToChangeToBestVote(chainID, bestVote);
                    }
                }

                // 2.4 查看是否有请求的交易回到交易池队列，有则把交易放入交易池入池
                TransactionPool txPool = this.txPools.get(chainID);
                for (Transaction tx: this.txMapForPool.get(chainID)) {
                    txPool.addTx(tx);
                }
                this.txMapForPool.get(chainID).clear();

                // 2.5 尝试挖矿
                tryToMine(chainID);

                // 定时操作，检查是否到时间
                if (System.currentTimeMillis() / 1000 - this.timeRecorders.get(chainID) >= ChainParam.DEFAULT_BLOCK_TIME) {
                    // 2.6 传播最佳区块
                    publishBestBlock(chainID);

                    // 2.7 传播最佳交易
                    publishBestTx(chainID);

//                    // 2.8 随机挑选logN个peer
//                    int counter = this.peerManagers.get(chainID).getPeerNumber();
//                    counter = (int)Math.log(counter);
//                    if (counter < 1) {
//                        counter = 1;
//                    }
//
//                    for (int i = 0; i < counter; i++) {
//
//                        byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
//                        logger.debug("Chain ID:{} get a peer:{}",
//                                new String(chainID.getData()), Hex.toHexString(peer));
//
//                        // 2.8.1 请求最佳交易
//                        requestTipTxForMining(chainID, peer);
//
//                        // 2.8.2 请求远端区块需求
//                        requestDemandBlockFromPeer(chainID, peer);
//
//                        // 2.8.3 请求远端交易需求
//                        requestDemandTxFromPeer(chainID, peer);
//                    }

                    //设定新时间起点
                    this.timeRecorders.put(chainID, System.currentTimeMillis() / 1000);
                }

                byte[] peer = this.peerManagers.get(chainID).getBlockPeerRandomly();
                // 2.8.1 请求最佳交易
                requestTipTxForMining(chainID, peer);

                // 2.8.2 请求远端区块需求
                requestDemandBlockFromPeer(chainID, peer);

                // 2.8.3 请求远端交易需求
                requestDemandTxFromPeer(chainID, peer);

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
    private TryResult tryToReBranch(ByteArrayWrapper chainID, BlockContainer blockContainer) {

        try {
            if (blockContainer.getBlock().getBlockNum() <
                    this.bestBlockContainers.get(chainID).getBlock().getBlockNum()) {
                logger.error("Chain ID:{}, best block number is bigger than given block", new String(chainID.getData()));
                return TryResult.ERROR;
            }

            // 先对齐区块号
            // 1. 高度差先缩小到mutable range之内
            BlockContainer referenceBlockContainer = blockContainer;
            BlockContainer immutableBlockContainer = new BlockContainer();
            while (referenceBlockContainer.getBlock().getBlockNum() - this.bestBlockContainers.get(chainID).
                    getBlock().getBlockNum() >= ChainParam.MUTABLE_RANGE) {
                TryResult result = tryToGetBlockContainerFromCache(chainID,
                        referenceBlockContainer.getBlock().getImmutableBlockHash(), immutableBlockContainer);

                if (TryResult.SUCCESS == result) {
                    logger.debug("Chain ID[{}] Got in cache block hash[{}] immutable block[{}]",
                            new String(chainID.getData()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getImmutableBlockHash()));

                    referenceBlockContainer = immutableBlockContainer;
                    immutableBlockContainer = new BlockContainer();
                } else {
                    logger.debug("Chain ID[{}] Got failed in cache block hash[{}] immutable block[{}]",
                            new String(chainID.getData()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getImmutableBlockHash()));

                    return result;
                }
            }

            // 2. 在mutable range范围内高度对齐
            BlockContainer previousBlockContainer = new BlockContainer();
            while (referenceBlockContainer.getBlock().getBlockNum() >
                    this.bestBlockContainers.get(chainID).getBlock().getBlockNum()) {
                TryResult result = tryToGetBlockContainerFromCache(chainID,
                        referenceBlockContainer.getBlock().getPreviousBlockHash(), previousBlockContainer);

                if (TryResult.SUCCESS == result) {
                    logger.debug("Chain ID[{}] Got in cache block hash[{}] previous block[{}]",
                            new String(chainID.getData()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getPreviousBlockHash()));

                    referenceBlockContainer = previousBlockContainer;
                    previousBlockContainer = new BlockContainer();
                } else {
                    logger.debug("Chain ID[{}] Got failed in cache block hash[{}] previous block[{}]",
                            new String(chainID.getData()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()),
                            Hex.toHexString(referenceBlockContainer.getBlock().getPreviousBlockHash()));

                    return result;
                }
            }

            // 3. 判断对齐之后的区块是否在主链上
            BlockInfo blockInfo = this.blockStore.getBlockInfoByHash(chainID.getData(),
                    referenceBlockContainer.getBlock().getBlockHash());

            if (null == blockInfo || !blockInfo.isMainChain()) {
                // 该区块本身不在主链上
                // 3.1 看上一个immutable point block是否在主链上
                byte[] immutableBlockHash1 = referenceBlockContainer.getBlock().getImmutableBlockHash();

                long immutableBlockNumber1 = 0;
                if (referenceBlockContainer.getBlock().getBlockNum() > ChainParam.MUTABLE_RANGE) {
                    immutableBlockNumber1 = referenceBlockContainer.getBlock().getBlockNum() - ChainParam.MUTABLE_RANGE;
                }

                byte[] hash = this.blockStore.getMainChainBlockHashByNumber(chainID.getData(), immutableBlockNumber1);

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
                        return reBranch(chainID, blockContainer);
                    } else {
                        // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                        // 获取参考点前面第1个immutable block container
                        BlockContainer blockContainer1 = new BlockContainer();

                        TryResult result1 = tryToGetBlockContainerFromCache(chainID, immutableBlockHash1, blockContainer1);

                        if (TryResult.SUCCESS == result1) {
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
                                    this.votingFlag.put(chainID, true);
                                } else {
                                    // 不在主链上，继续查看第3个immutable block hash
                                    // 先获取前面第2个mutable point block container
                                    BlockContainer blockContainer2 = new BlockContainer();

                                    TryResult result2 = tryToGetBlockContainerFromCache(chainID, immutableBlockHash2, blockContainer2);

                                    if (TryResult.SUCCESS == result2) {
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
                                                this.votingFlag.put(chainID, true);
                                            } else {
                                                // fork point out of warning range, maybe it's an attack chain
                                                logger.debug("++ctx-----------------------an attack chain.....");
                                            }
                                        }
                                    } else {
                                        return result2;
                                    }
                                }
                            }
                        } else {
                            return result1;
                        }
                    }
                }
            } else {
                // 该区块在主链上
                logger.debug("Chain ID[{}] Block [{}] is on main chain, re-branch.",
                        new String(chainID.getData()),
                        Hex.toHexString(referenceBlockContainer.getBlock().getBlockHash()));
                return reBranch(chainID, blockContainer);
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
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

        if (this.blockContainerMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.blockContainerMapForSync.get(chainID).clear();
        }

        if (this.blockMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.blockMapForSync.get(chainID).clear();
        }

        if (this.txMapForSync.get(chainID).size() > ChainParam.MUTABLE_RANGE) {
            this.txMapForSync.get(chainID).clear();
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

                byte[] previousHash = blockHash.getData();
                logger.debug("Chain ID:{} Response from block hash:{}",
                        new String(chainID.getData()), Hex.toHexString(previousHash));
                for (int i = 0; i < ChainParam.MUTABLE_RANGE; i++) {
                    Block block = this.blockStore.getBlockByHash(chainID.getData(), previousHash);
                    if (null != block) {
                        publishBlock(block);
                        previousHash = block.getPreviousBlockHash();
                    } else {
                        break;
                    }
                }
            }
            this.blockHashMapFromDemand.get(chainID).clear();

            // tx
            for (ByteArrayWrapper txid : this.txHashMapFromDemand.get(chainID)) {
                logger.debug("Chain ID:{} Response tx hash:{}",
                        new String(chainID.getData()), txid.toString());
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
    public static byte[] makeBlockTipSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_TIP_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_TIP_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.BLOCK_TIP_CHANNEL.length, timeBytes.length);
        return salt;
    }

    /**
     * make block demand salt
     * @param chainID chain ID
     * @return block demand salt
     */
    public static byte[] makeBlockDemandSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_DEMAND_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_DEMAND_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.BLOCK_DEMAND_CHANNEL.length, timeBytes.length);
        return salt;
    }

    /**
     * make tx salt
     * @param chainID chain ID
     * @return tx tip salt
     */
    public static byte[] makeTxTipSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.TX_TIP_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_TIP_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_TIP_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.TX_TIP_CHANNEL.length, timeBytes.length);
        return salt;
    }

    /**
     * make tx demand salt
     * @param chainID chain ID
     * @return tx demand salt
     */
    public static byte[] makeTxDemandSalt(byte[] chainID) {
        long time = System.currentTimeMillis() / 1000 / ChainParam.DEFAULT_BLOCK_TIME;
        byte[] timeBytes = ByteUtil.longToBytes(time);

        byte[] salt = new byte[chainID.length + ChainParam.TX_DEMAND_CHANNEL.length + timeBytes.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_DEMAND_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_DEMAND_CHANNEL.length);
        System.arraycopy(timeBytes, 0, salt, chainID.length + ChainParam.TX_DEMAND_CHANNEL.length, timeBytes.length);
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
        logger.debug("Request sync block hash:{}, current block number:{}",
                Hex.toHexString(this.syncBlocks.get(chainID).getPreviousBlockHash()),
                this.syncBlocks.get(chainID).getBlockNum());
        requestBlockForSync(chainID, this.syncBlocks.get(chainID).getPreviousBlockHash());
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

            if (null != blockContainer) {
                // 如果同步遇到非法区块，放弃这条链
                if (ImportResult.INVALID_BLOCK == syncBlock(chainID, blockContainer)) {
                    logger.error("Chain ID:{}, Throw this chain away, invalid block:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(blockContainer.getBlock().getBlockHash()));
                    resetChain(chainID);
                }
            }
        }
    }

    /**
     * 同步区块
     * @param chainID chain ID
     * @param blockContainer block container
     * @return import result
     */
    private ImportResult syncBlock(ByteArrayWrapper chainID, BlockContainer blockContainer) {

        ImportResult result;

        try {
            StateDB track = this.stateDB.startTracking(chainID.getData());

            result = this.stateProcessors.get(chainID).backwardProcess(blockContainer, track);

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

                this.tauListener.onSyncBlock(blockContainer.getBlock());

                this.syncBlocks.put(chainID, blockContainer.getBlock());

                this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return ImportResult.EXCEPTION;
        }

        return result;
    }

    /**
     * 尝试进行一次挖矿
     * @param chainID chain ID
     */
    private void tryToMine(ByteArrayWrapper chainID) {
        if (TryResult.SUCCESS == minable(chainID)) {
            BlockContainer blockContainer = mineBlock(chainID);

            if (this.enableMineForTest.get(chainID) && null != blockContainer) {
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

                        Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);

                        txPools.get(chainID).recheckAccoutTx(accounts);

                        for (ByteArrayWrapper account: accounts) {
                            this.stateDB.addPeer(chainID.getData(), account.getData());
                        }

                        this.tauListener.onNewBlock(blockContainer.getBlock());

                        publishBestBlock(chainID);
                    } catch (Exception e) {
                        logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * re-branch chain
     * @param targetBlockContainer block that chain will change to
     */
    private TryResult reBranch(ByteArrayWrapper chainID, BlockContainer targetBlockContainer) {
        try {

            byte[] previousHash = targetBlockContainer.getBlock().getPreviousBlockHash();
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
                BlockContainer previousBlockContainer = new BlockContainer();

                TryResult result = tryToGetBlockContainerFromCache(chainID, previousHash, previousBlockContainer);

                if (TryResult.SUCCESS == result) {
                    logger.debug("ChainID:{}, Find block:{} in cache.", new String(chainID.getData()),
                            Hex.toHexString(key.getData()));
                    // 如果有返回，但是数据不为空
                    containerList.add(previousBlockContainer);
                    previousHash = previousBlockContainer.getBlock().getPreviousBlockHash();

                    if (previousBlockContainer.getBlock().getBlockNum() <= 0) {
                        break;
                    }
                } else if (TryResult.ERROR == result) {
                    // 如果有返回数据，但是数据为空
                    return TryResult.ERROR;
                } else if (TryResult.REQUEST == result) {
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

            publishBestBlock(chainID);

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
                this.tauListener.onRollBack(undoBlockContainer.getBlock());
            }

            size = newBlockContainers.size();
            for (int i = size - 1; i >= 0; i--) {
                this.tauListener.onNewBlock(newBlockContainers.get(i).getBlock());
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
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

        logger.debug("Chain ID[{}]: Init Chain.", new String(chainID.getData()));

        try {
            this.blockStore.removeChainBlockInfo(chainID.getData());
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
            this.syncBlocks.put(chainID, blockContainer.getBlock());

            Set<ByteArrayWrapper> accounts= extractAccountFromBlockContainer(blockContainer);
            for (ByteArrayWrapper account: accounts) {
                this.stateDB.addPeer(chainID.getData(), account.getData());
            }

            this.tauListener.onSyncBlock(blockContainer.getBlock());

            this.peerManagers.get(chainID).addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
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
            this.blockStore.removeChainBlockInfo(chainID.getData());
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

            BlockInfo blockInfo1 = this.blockStore.getBlockInfoByHash(chainID.getData(), immutableBlockHash1);
            if (null == blockInfo1) {
                if (isSyncUncompleted(chainID)) {
                    requestSyncBlock(chainID);
                } else {
                    resetAfterVoting(chainID);
                }
                return false;
            }

            if (blockInfo1.isMainChain()) {
                // 分叉点在mutable range之内
                int counter = 0;
                byte[] previousHash = blockContainer1.getBlock().getPreviousBlockHash();
                List<BlockContainer> containerList = new ArrayList<>();

                containerList.add(blockContainer1);

                boolean findAll = true;
                while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {

                    // 先查看数据库是否存在
                    if (this.blockStore.isBlockOnChain(chainID.getData(), previousHash)) {
                        // found in local
                        logger.debug("+ctx--------found in local, hash:{}",
                                Hex.toHexString(previousHash));
                        break;
                    }

                    ByteArrayWrapper key = new ByteArrayWrapper(previousHash);

                    // 再从缓存查找
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

                    // 仍然找不到，则向dht请求
                    requestBlockForMining(chainID, previousHash);
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
                    if (TryResult.SUCCESS == reBranch(chainID, blockContainer1)) {
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
                        BlockInfo blockInfo2 = this.blockStore.getBlockInfoByHash(chainID.getData(), immutableBlockHash2);
                        if (null == blockInfo2) {
                            if (isSyncUncompleted(chainID)) {
                                requestSyncBlock(chainID);
                            } else {
                                resetAfterVoting(chainID);
                            }
                            return false;
                        }

                        if (blockInfo2.isMainChain()) {
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
                                    BlockInfo blockInfo3 = this.blockStore.getBlockInfoByHash(chainID.getData(), immutableBlockHash3);
                                    if (null == blockInfo3) {
                                        if (isSyncUncompleted(chainID)) {
                                            requestSyncBlock(chainID);
                                        } else {
                                            resetAfterVoting(chainID);
                                        }
                                        return false;
                                    }

                                    if (blockInfo3.isMainChain()) {
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
     * 尝试从本地缓存获取block container，如果没有则请求缺失的部分
     * @param chainID chain ID
     * @param blockHash block hash
     * @param blockContainer block container to get
     * @return result
     */
    private TryResult tryToGetBlockContainerFromCache(ByteArrayWrapper chainID, byte[] blockHash,
                                                    BlockContainer blockContainer) {

        try {
            ByteArrayWrapper blockKey = new ByteArrayWrapper(blockHash);

            BlockContainer blockContainerInCache = this.blockContainerMap.get(chainID).get(blockKey);
            if (this.blockContainerMap.get(chainID).containsKey(blockKey)) {
                if (null == blockContainerInCache) {
                    return TryResult.ERROR;
                }
                // 发现现成的数据
                logger.debug("Find in block container cache:{}", Hex.toHexString(blockHash));
                blockContainer.copy(blockContainerInCache);
            } else {
                // 没有现成的数据，试着从缓存中读取
                if (this.blockMap.get(chainID).containsKey(blockKey)) {
                    Block block = this.blockMap.get(chainID).get(blockKey);
                    if (null != block) {
                        if (null != block.getTxHash()) {
                            // 区块带有交易
                            ByteArrayWrapper txKey = new ByteArrayWrapper(block.getTxHash());
                            if (this.txMap.get(chainID).containsKey(txKey)) {
                                Transaction tx = this.txMap.get(chainID).get(txKey);
                                if (null != tx) {
                                    blockContainer.setBlock(block);
                                    blockContainer.setTx(tx);
                                    this.blockContainerMap.get(chainID).put(blockKey,
                                            BlockContainer.with(blockContainer));
                                } else {
                                    return TryResult.ERROR;
                                }
                            } else {
                                // 本地没找到， 请求交易
                                logger.debug("Request tx :{}", Hex.toHexString(block.getTxHash()));
                                requestTxForMining(chainID, block.getTxHash(), blockKey);
                                return TryResult.REQUEST;
                            }
                        } else {
                            // 区块无交易
                            blockContainer.setBlock(block);
                            this.blockContainerMap.get(chainID).put(blockKey,
                                    BlockContainer.with(blockContainer));
                        }
                    } else {
                        return TryResult.ERROR;
                    }
                } else {
                    logger.debug("Request block :{}", Hex.toHexString(blockHash));
                    requestBlockForMining(chainID, blockHash);
                    return TryResult.REQUEST;
                }
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
        }

        return TryResult.SUCCESS;
    }

    private void requestTipBlockFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = makeBlockTipSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK_FROM_PEER_FOR_MINING);
        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestDemandBlockFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = makeBlockDemandSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
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
        DataIdentifier dataIdentifier = new DataIdentifier(chainID,
                DataType.HISTORY_BLOCK_REQUEST_FOR_SYNC, new ByteArrayWrapper(blockHash));

        publishBlockRequest(chainID, blockHash);

        TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private void requestDemandTxFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        byte[] salt = makeTxDemandSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
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
        byte[] salt = makeTxTipSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
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
        byte[] salt = makeBlockTipSalt(chainID.getData());
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, salt);
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

        byte[] salt = makeBlockDemandSalt(chainID.getData());
        DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                keyPair.second, ByteUtil.getHashEncoded(blockHash), salt);
        TorrentDHTEngine.getInstance().distribute(mutableItem);
    }

    private void publishTxRequest(ByteArrayWrapper chainID, byte[] txHash) {
        // put mutable item
        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        byte[] salt = makeTxDemandSalt(chainID.getData());
        DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first,
                keyPair.second, ByteUtil.getHashEncoded(txHash), salt);
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
            byte[] salt = makeBlockTipSalt(chainID.getData());
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    ByteUtil.getHashEncoded(bestBlockContainer.getBlock().getBlockHash()),
                    salt);
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

            byte[] salt = makeTxTipSalt(chainID.getData());
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    ByteUtil.getHashEncoded(tx.getTxID()), salt);
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
     * check if a block valid
     * @param chainID chain ID
     * @param block block to check
     * @param stateDB state db
     * @return try result
     */
    private TryResult isValidBlock(ByteArrayWrapper chainID, Block block, StateDB stateDB) {
//        // 是否本链
//        if (!Arrays.equals(chainID.getData(), block.getChainID())) {
//            logger.error("ChainID[{}]: ChainID mismatch!", new String(chainID.getData()));
//            return false;
//        }
        // TODO:: 检查immutable block hash

        // 时间戳检查
        if (block.getTimeStamp() > System.currentTimeMillis() / 1000) {
            logger.error("ChainID[{}]: Block[{}] Time is in the future!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return TryResult.ERROR;
        }

        // 区块内部自检
        if (!block.isBlockParamValidate()) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return TryResult.ERROR;
        }

        // 区块签名检查
        if (!block.verifyBlockSig()) {
            logger.error("ChainID[{}]: Block[{}] Bad Signature!",
                    new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
            return TryResult.ERROR;
        }

        // 是否孤块
        try {
            if (!this.blockStore.isBlockOnChain(chainID.getData(), block.getPreviousBlockHash())) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
        }

        // POT共识验证
        return verifyPOT(chainID, block, stateDB);
    }

    /**
     * check if a block container valid
     * @param chainID chain ID
     * @param blockContainer block container
     * @param stateDB state db
     * @return try result
     */
    private TryResult isValidBlockContainer(ByteArrayWrapper chainID,
                                          BlockContainer blockContainer, StateDB stateDB) {
        return isValidBlock(chainID, blockContainer.getBlock(), stateDB);
    }

    /**
     * check pot consensus
     * @param chainID chain ID
     * @param block block to check
     * @param stateDB state db
     * @return try result
     */
    private TryResult verifyPOT(ByteArrayWrapper chainID, Block block, StateDB stateDB) {
        try {
            ProofOfTransaction pot = this.pots.get(chainID);

            byte[] pubKey = block.getMinerPubkey();

            BigInteger power = stateDB.getNonce(chainID.getData(), pubKey);
            if (null == power) {
                logger.error("ChainID[{}]: Miner[{}] has no power!",
                        new String(chainID.getData()), Hex.toHexString(pubKey));
                return TryResult.ERROR;
            }
            logger.info("Chain ID[{}]: Address: {}, mining power: {}",
                    new String(chainID.getData()), Hex.toHexString(pubKey), power);

            Block parentBlock = this.blockStore.getBlockByHash(chainID.getData(), block.getPreviousBlockHash());
            if (null == parentBlock) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }

            // check base target
            Block ancestor3 = null;
            if (parentBlock.getBlockNum() > 3) {
                Block ancestor1 = this.blockStore.getBlockByHash(chainID.getData(),
                        parentBlock.getPreviousBlockHash());
                if (null == ancestor1) {
                    if (isSyncUncompleted(chainID)) {
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    }
                    return TryResult.ERROR;
                }

                Block ancestor2 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor1.getPreviousBlockHash());
                if (null == ancestor2) {
                    if (isSyncUncompleted(chainID)) {
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    }
                    return TryResult.ERROR;
                }

                ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor2.getPreviousBlockHash());
                if (null == ancestor3) {
                    if (isSyncUncompleted(chainID)) {
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    }
                    return TryResult.ERROR;
                }
            }

            BigInteger baseTarget = pot.calculateRequiredBaseTarget(parentBlock, ancestor3);
            if (0 != baseTarget.compareTo(block.getBaseTarget())) {
                logger.error("ChainID[{}]: Block[{}] base target error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }

            // check generation signature
            byte[] genSig = pot.calculateGenerationSignature(parentBlock.getGenerationSignature(), pubKey);
            if (!Arrays.equals(genSig, block.getGenerationSignature())) {
                logger.error("ChainID[{}]: Block[{}] generation signature error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }

            // check cumulative difficulty
            BigInteger culDifficulty = pot.calculateCumulativeDifficulty(
                    parentBlock.getCumulativeDifficulty(), baseTarget);
            if (0 != culDifficulty.compareTo(block.getCumulativeDifficulty())) {
                logger.error("ChainID[{}]: Block[{}] Cumulative difficulty error!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }

            BigInteger hit = pot.calculateRandomHit(genSig);
            long timeInterval = block.getTimeStamp() - parentBlock.getTimeStamp();

            // verify hit
            if (!pot.verifyHit(hit, baseTarget, power, timeInterval)) {
                logger.error("ChainID[{}]: The block[{}] does not meet the pot consensus!!",
                        new String(chainID.getData()), Hex.toHexString(block.getBlockHash()));
                return TryResult.ERROR;
            }

        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
        }

        return TryResult.SUCCESS;
    }

    /**
     * check if be able to mine now
     * @param chainID chain ID
     * @return true if can mine, false otherwise
     */
    private TryResult minable(ByteArrayWrapper chainID) {
        try {
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
                Block ancestor1 = this.blockStore.getBlockByHash(chainID.getData(),
                        bestBlockContainer.getBlock().getPreviousBlockHash());
                if (null == ancestor1) {
                    if (isSyncUncompleted(chainID)) {
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    }
                    return TryResult.ERROR;
                }

                Block ancestor2 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor1.getPreviousBlockHash());
                if (null == ancestor2) {
                    if (isSyncUncompleted(chainID)) {
                        requestSyncBlock(chainID);
                        return TryResult.REQUEST;
                    }
                    return TryResult.ERROR;
                }

                ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor2.getPreviousBlockHash());
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

            // check if target >= hit
//            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
//                    System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp());

            BigInteger hit = pot.calculateRandomHit(genSig);

            long timeInterval = pot.calculateMiningTimeInterval(hit, baseTarget, power);
            if ((System.currentTimeMillis() / 1000 - bestBlockContainer.getBlock().getTimeStamp()) < timeInterval) {
                logger.info("Chain ID[{}]: It's not time for the block.", new String(chainID.getData()));
                return TryResult.ERROR;
            }

//            if (target.compareTo(hit) < 0) {
//                logger.info("ChainID[{}]: Target[{}] value is smaller than hit[{}]!!",
//                        new String(this.chainID), target, hit);
//                return false;
//            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return TryResult.ERROR;
        }

        return TryResult.SUCCESS;
    }

    /**
     * mine a block
     * @param chainID chain ID
     * @return block container, or null
     */
    private BlockContainer mineBlock(ByteArrayWrapper chainID) {
        ProofOfTransaction pot = this.pots.get(chainID);
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

        Block ancestor3 = null;
        try {
            if (bestBlockContainer.getBlock().getBlockNum() > 3) {
                Block ancestor1 = this.blockStore.getBlockByHash(chainID.getData(),
                        bestBlockContainer.getBlock().getPreviousBlockHash());

                Block ancestor2 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor1.getPreviousBlockHash());

                ancestor3 = this.blockStore.getBlockByHash(chainID.getData(),
                        ancestor2.getPreviousBlockHash());
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return null;
        }

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(bestBlockContainer.getBlock(), ancestor3);

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

        Transaction tx = this.txPools.get(chainID).getBestTransaction();

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
        this.stateProcessors.get(chainID).forwardProcess(blockContainer, miningTrack);

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

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {

        DataIdentifier dataIdentifier = (DataIdentifier) cbData;
        switch (dataIdentifier.getDataType()) {
            case TIP_BLOCK_FROM_PEER_FOR_MINING: {
                if (null == item) {
                    logger.error("TIP_BLOCK_FROM_PEER_FOR_MINING is empty.");
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                logger.debug("Request tip block hash:{}", Hex.toHexString(hash));
                requestBlockForMining(dataIdentifier.getChainID(), hash);

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_MINING is empty, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()));
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

                            // 从缓存删掉空交易，非空交易不作删除，等队列满删除
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
                    logger.error("HISTORY_TX_REQUEST_FOR_MINING is empty, block hash:{}",
                            Hex.toHexString(dataIdentifier.getHash().getData()));

                    // 区块对应交易为空，在block container集合里插入空标志
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getTxBlockHash(), null);

                    return;
                } else {
                    // 数据非空
                    Transaction tx = TransactionFactory.parseTransaction(item);

                    Block block = this.blockMap.get(dataIdentifier.getChainID()).get(dataIdentifier.getTxBlockHash());

                    if (null != block) {
                        BlockContainer blockContainer = new BlockContainer(block, tx);

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(dataIdentifier.getTxBlockHash(), blockContainer);
                    } else {
                        // 可能区块被缓存删掉，重新请求区块
                        requestBlockForMining(dataIdentifier.getChainID(), dataIdentifier.getTxBlockHash().getData());
                    }

                    // 放入交易集合作缓存
                    this.txMap.get(dataIdentifier.getChainID()).put(dataIdentifier.getHash(), tx);
                }

                break;
            }
            case BLOCK_DEMAND_FROM_PEER: {
                if (null == item) {
                    logger.error("BLOCK_DEMAND_FROM_PEER is empty");
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                logger.debug("Got a demand block hash:{}", Hex.toHexString(hash));
                this.blockHashMapFromDemand.get(dataIdentifier.getChainID()).add(new ByteArrayWrapper(hash));

                break;
            }
            case TX_DEMAND_FROM_PEER: {
                if (null == item) {
                    logger.error("TX_DEMAND_FROM_PEER is empty");
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                logger.debug("Got a demand tx hash:{}", Hex.toHexString(hash));
                this.txHashMapFromDemand.get(dataIdentifier.getChainID()).add(new ByteArrayWrapper(hash));

                break;
            }
            case TIP_BLOCK_FROM_PEER_FOR_VOTING: {
                if (null == item) {
                    logger.error("TIP_BLOCK_FROM_PEER_FOR_VOTING is empty");
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                requestBlockForVoting(dataIdentifier.getChainID(), hash);

                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_VOTING: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_VOTING is empty");
                    return;
                }

                Block block = new Block(item);
                this.votingBlocks.get(dataIdentifier.getChainID()).add(block);

                break;
            }
            case TIP_TX_FOR_MINING: {
                if (null == item) {
                    logger.error("TIP_TX_FOR_MINING is empty");
                    return;
                }

                byte[] hash = ByteUtil.getHashFromEncode(item);
                requestTxForPool(dataIdentifier.getChainID(), hash);

                break;
            }
            case TX_REQUEST_FOR_MINING: {
                if (null == item) {
                    logger.error("TX_REQUEST_FOR_MINING is empty");
                    return;
                }

                Transaction tx = TransactionFactory.parseTransaction(item);
                this.txMapForPool.get(dataIdentifier.getChainID()).add(tx);
                break;
            }
            case HISTORY_BLOCK_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_BLOCK_REQUEST_FOR_SYNC is empty, sync block hash:{}",
                            dataIdentifier.getHash().toString());
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
                        put(dataIdentifier.getHash(), block);

                break;
            }
            case HISTORY_TX_REQUEST_FOR_SYNC: {
                if (null == item) {
                    logger.error("HISTORY_TX_REQUEST_FOR_SYNC is empty, sync tx hash:{}",
                            dataIdentifier.getHash().toString());
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
                                put(dataIdentifier.getTxBlockHash(), blockContainer);
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
