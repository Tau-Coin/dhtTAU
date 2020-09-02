package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import io.taucoin.types.MutableItemValue;
import io.taucoin.types.Transaction;
import io.taucoin.types.TransactionFactory;
import io.taucoin.types.TypesConfig;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;

public class Chains implements DHT.GetDHTItemCallback{
    private static final Logger logger = LoggerFactory.getLogger("Chain");

    private static final int LOOP_LIMIT = 3;

    // Chain id specified by the transaction of creating new blockchain.
    private final byte[] chainID;

    private final Set<ByteArrayWrapper> chainIDs = new HashSet<>();

    // mutable item salt: block
    private final Map<ByteArrayWrapper, byte[]> blockSalts = new HashMap<>();

    // mutable item salt: tx
    private final Map<ByteArrayWrapper, byte[]> txSalts = new HashMap<>();

    // Voting thread.
    private Thread votingThread;

    private final TauListener tauListener;

    // consensus: pot
    private ProofOfTransaction pot;

    private final Map<ByteArrayWrapper, ProofOfTransaction> pots = new HashMap<>();

    // tx pool
    private TransactionPool txPool;

    private final Map<ByteArrayWrapper, TransactionPool> txPools = new HashMap<>();

    // voting pool
    private VotingPool votingPool;

    private final Map<ByteArrayWrapper, VotingPool> votingPools = new HashMap<>();

    // peer manager
    private PeerManager peerManager;

    private final Map<ByteArrayWrapper, PeerManager> peerManagers = new HashMap<>();

    // block db
    private final BlockStore blockStore;

    // state db
    private final StateDB stateDB;

    // state processor: process and roll back block
    private StateProcessor stateProcessor;

    private final Map<ByteArrayWrapper, StateProcessor> stateProcessors = new HashMap<>();

    // the best block container of current chain
    private BlockContainer bestBlockContainer;

    private final Map<ByteArrayWrapper, BlockContainer> bestBlockContainers = new HashMap<>();

    // the synced block of current chain
    private Block syncBlock;

    private final Map<ByteArrayWrapper, Block> syncBlocks = new HashMap<>();



    private final Map<ByteArrayWrapper, List<Block>> votingBlocks = new HashMap<>();

    private final Map<ByteArrayWrapper, List<Transaction>> txMapForPool = new HashMap<>();

    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, BlockContainer>> blockContainerMap = new HashMap<>();

    private final Map<ByteArrayWrapper, List<Block>> blockMap = new HashMap<>();

    private final Map<ByteArrayWrapper, List<Transaction>> txMap = new HashMap<>();

    private final Map<ByteArrayWrapper, Map<ByteArrayWrapper, BlockContainer>> blockContainerMapForSync = new HashMap<>();

    private final Map<ByteArrayWrapper, List<Block>> blockMapForSync = new HashMap<>();

    private final Map<ByteArrayWrapper, List<Transaction>> txMapForSync = new HashMap<>();

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     * @param blockStore block store
     * @param stateDB state db
     */
    public Chains(byte[] chainID, BlockStore blockStore, StateDB stateDB, TauListener tauListener) {
        this.chainID = chainID;
        this.blockStore = blockStore;
        this.stateDB = stateDB;
        this.tauListener = tauListener;
    }

    /**
     * follow a new chain
     * @param chainID chain ID
     * @return true if succeed, or false
     */
    public boolean followChain(byte[] chainID) {
        this.blockSalts.put(new ByteArrayWrapper(chainID), makeBlockSalt(chainID));

        this.txSalts.put(new ByteArrayWrapper(chainID), makeTxSalt(chainID));

        this.chainIDs.add(new ByteArrayWrapper(chainID));

        // init voting pool
        this.votingPools.put(new ByteArrayWrapper(chainID), new VotingPool(chainID));

        // init pot consensus
        this.pots.put(new ByteArrayWrapper(chainID), new ProofOfTransaction(chainID));

        // init state processor
        this.stateProcessors.put(new ByteArrayWrapper(chainID), new StateProcessorImpl(chainID));

        // init best block and sync block
        try {
            byte[] bestBlockHash = this.stateDB.getBestBlockHash(chainID);
            if (null != bestBlockHash) {
                logger.info("Chain ID[{}]: Best block hash[{}]",
                        new String(chainID), Hex.toHexString(bestBlockHash));
                this.bestBlockContainers.put(new ByteArrayWrapper(chainID),
                        this.blockStore.getBlockContainerByHash(chainID, bestBlockHash));
            }

            byte[] syncBlockHash = this.stateDB.getSyncBlockHash(chainID);
            if (null != syncBlockHash) {
                logger.info("Chain ID[{}]: Sync block hash[{}]",
                        new String(chainID), Hex.toHexString(syncBlockHash));
                this.syncBlocks.put(new ByteArrayWrapper(chainID),
                        this.blockStore.getBlockByHash(chainID, syncBlockHash));
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
            if (null != this.bestBlockContainer && this.bestBlockContainer.getBlock().getBlockNum() > 0) {
                // get priority peers in mutable range
                priorityPeers.add(new ByteArrayWrapper(this.bestBlockContainer.getBlock().getMinerPubkey()));
                byte[] previousHash = this.bestBlockContainer.getBlock().getPreviousBlockHash();
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

        this.peerManagers.put(new ByteArrayWrapper(chainID), peerManager);

        // init tx pool
        TransactionPool txPool = new TransactionPoolImpl(chainID,
                AccountManager.getInstance().getKeyPair().first, this.stateDB);
        txPool.init();

        this.txPools.put(new ByteArrayWrapper(chainID), txPool);

        List<Block> votingBlocks = new ArrayList<>();
        this.votingBlocks.put(new ByteArrayWrapper(chainID), votingBlocks);

        List<Transaction> txForPool = new ArrayList<>();
        this.txMapForPool.put(new ByteArrayWrapper(chainID), txForPool);

        Map<ByteArrayWrapper, BlockContainer> blockContainers = new HashMap<>();
        this.blockContainerMap.put(new ByteArrayWrapper(chainID), blockContainers);

        List<Block> blocks = new ArrayList<>();
        this.blockMap.put(new ByteArrayWrapper(chainID), blocks);

        List<Transaction> txs = new ArrayList<>();
        this.txMap.put(new ByteArrayWrapper(chainID), txs);

        Map<ByteArrayWrapper, BlockContainer> blockContainersForSync = new HashMap<>();
        this.blockContainerMapForSync.put(new ByteArrayWrapper(chainID), blockContainersForSync);

        List<Block> blocksForSync = new ArrayList<>();
        this.blockMapForSync.put(new ByteArrayWrapper(chainID), blocksForSync);

        List<Transaction> txsForSync = new ArrayList<>();
        this.txMapForSync.put(new ByteArrayWrapper(chainID), txsForSync);


        return true;
    }

    private boolean isEmptyChain(ByteArrayWrapper chainID) {
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);
        return null == bestBlockContainer || (
                (System.currentTimeMillis() / 1000 - bestBlockContainer.getBlock().getTimeStamp()) >
                        ChainParam.WARNING_RANGE * ChainParam.DEFAULT_BLOCK_TIME);
    }

    private void multiChain() {
        while (true) {
            for (ByteArrayWrapper chainID : this.chainIDs) {

                logger.debug("Chain ID:{}", new String(chainID.getData()));

                if (isEmptyChain(chainID)) {
                    // empty chain
                    Iterator<Map.Entry<ByteArrayWrapper, BlockContainer>> iterator =
                            this.blockContainerMap.get(chainID).entrySet().iterator();
                    if (iterator.hasNext()) {
                        BlockContainer blockContainer = iterator.next().getValue();
                        initChain(chainID, blockContainer);

                        iterator.remove();
                    } else {
                        byte[] peer = peerManager.getBlockPeerRandomly();
                        requestTipBlockFromPeer(chainID, peer);
                    }
                }

                if (!isEmptyChain(chainID)) {
                    //
                    Map<ByteArrayWrapper, BlockContainer> blockContainers = this.blockContainerMap.get(chainID);
                    PeerManager peerManager = this.peerManagers.get(chainID);
                    Block syncBlock = this.syncBlocks.get(chainID);
                    BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

                    if (blockContainers.isEmpty()) {

                        byte[] peer = peerManager.getBlockPeerRandomly();
                        requestTipBlockFromPeer(chainID, peer);
                    } else {
                        Iterator<Map.Entry<ByteArrayWrapper, BlockContainer>> iterator =
                                blockContainers.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<ByteArrayWrapper, BlockContainer> entry = iterator.next();
                            BlockContainer blockContainer = entry.getValue();

                            if (Arrays.equals(syncBlock.getPreviousBlockHash(),
                                    blockContainer.getBlock().getBlockHash())) {
                                // sync block
                                SyncBlock(chainID, blockContainer);
                                // remove block container
                                iterator.remove();
                            } else if (blockContainer.getBlock().getCumulativeDifficulty().
                                    compareTo(bestBlockContainer.getBlock().getCumulativeDifficulty()) > 0) {
                                // find a more difficulty chain
                                if (checkBranch(chainID, blockContainer)) {
                                    tryToRebranch(chainID, blockContainer);
                                }
                            }
                        }
                    }

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
                                    peerManager.addNewBlockPeer(blockContainer.getBlock().getMinerPubkey());
                                } catch (Exception e) {
                                    logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
                                }

                                Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);
                                txPools.get(chainID).recheckAccoutTx(accounts);

                                publishBestBlock(chainID);
                            }
                        }
                    }

                    int counter = peerManager.getPeerNumber();
                    counter = counter > 0 ? (int)Math.log(counter) : 0;

                    for (int i = 0; i < counter; i++) {
                        byte[] peer = peerManager.getBlockPeerRandomly();
                        requestTipTxForPoolFromPeer(chainID, peer);
                    }

                    for (int i = 0; i < counter; i++) {
                        byte[] peer = peerManager.getBlockPeerRandomly();
                        responseOrRepublic(chainID, peer);
                    }
                }

            }
        }
    }

    private void responseOrRepublic(ByteArrayWrapper chainID, byte[] peer) {
        // block
        // tx
    }

    /**
     * make block salt
     * @return block salt
     */
    private byte[] makeBlockSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_CHANNEL.length);
        return salt;
    }

    /**
     * make tx salt
     * @return tx salt
     */
    private byte[] makeTxSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.TX_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.TX_CHANNEL, 0, salt, chainID.length,
                ChainParam.TX_CHANNEL.length);
        return salt;
    }

    /**
     * init chain
     *
     * @return true if succeed, false otherwise
     */
    private boolean init() {
        return true;
    }

    /**
     * block chain main process
     */
    private void blockChainProcess() {
        // vote for new chain, when there is nothing in local
        if (null == this.bestBlockContainer || null == this.syncBlock) {
            Vote bestVote = vote();

        }

        // if offline too long, vote as a new chain
        if (null != this.bestBlockContainer &&
                (System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp()) >
                        ChainParam.WARNING_RANGE * ChainParam.DEFAULT_BLOCK_TIME) {
            Vote bestVote = vote();

        }

        chainLoop();
    }

    /**
     * main chain loop
     */
    private void chainLoop() {
        long startMiningTime = 0;
        long lastMiningTime;

        long startVotingTime = 0;
        long lastVotingTime;

        while (!Thread.interrupted()) {
            boolean votingFlag = false;
            boolean miningFlag = false;

            // keep looking for more difficult chain
            long startLookingTime = 0;
            long lastLookingTime;
            while (!Thread.interrupted()) {
                lastLookingTime = startLookingTime;
                startLookingTime = System.currentTimeMillis() / 1000;
                if (startLookingTime - lastLookingTime < 1) {
                    logger.info("Chain ID[{}]: Sleep 1 s", new String(this.chainID));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.info(new String(this.chainID) + ":" + e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }

                byte[] pubKey = this.peerManager.getBlockPeerRandomly();
                logger.debug("Chain ID[{}]: peer public key:{}",
                        new String(this.chainID), Hex.toHexString(pubKey));

                // if last visiting time is letter default block time, jump to mine
                long lastVisitTime = this.peerManager.getPeerVisitTime(pubKey);
                logger.debug("Chain ID[{}]: last visiting time:{}",
                        new String(this.chainID), lastVisitTime);
                if ((System.currentTimeMillis() / 1000 - lastVisitTime) < ChainParam.DEFAULT_MIN_BLOCK_TIME) {
                    logger.debug("++ctx-----------------------go to mine.");
                    miningFlag = true;
                    break;
                }

                BlockContainer tip = getTipBlockContainerFromPeer(pubKey);
                this.peerManager.updateVisitTime(pubKey);

                // if tip block is null, jump to mine
                if (null == tip) {
                    logger.debug("++ctx-----------------------tip is null, go to mine.");
                    miningFlag = true;
                    break;
//                    continue;
                }

                // if a less difficult chain, jump to mine
                if (tip.getBlock().getCumulativeDifficulty().
                        compareTo(this.bestBlockContainer.getBlock().getCumulativeDifficulty()) <= 0) {
                    if (null != tip.getTx()) {
                        txPool.addTx(tip.getTx());
                    }
                    miningFlag = true;
                    break;
//                    continue;
                }

                // if found a more difficult chain
                try {
                    byte[] immutableBlockHash = tip.getBlock().getImmutableBlockHash();
                    if (this.blockStore.isMainChainBlock(this.chainID, immutableBlockHash)) {
                        // 分叉点在mutable range之内
                        // download block first
                        int counter = 0;
                        byte[] previousHash = tip.getBlock().getPreviousBlockHash();
                        List<BlockContainer> containerList = new ArrayList<>();

                        containerList.add(tip);

                        boolean findAll = true;
                        while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {

                            Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                            if (null != block) {
                                // found in local
                                logger.debug("+ctx--------found in local, hash:{}",
                                        Hex.toHexString(previousHash));
                                break;
                            }
                            // get from dht
                            BlockContainer container = getBlockContainerFromDHTByHash(previousHash);

                            if (null == container) {
                                logger.debug("+ctx--------Cannot get container from dht, hash:{}",
                                        Hex.toHexString(previousHash));
                                findAll = false;
                                break;
                            }

                            previousHash = container.getBlock().getPreviousBlockHash();

                            logger.debug("+ctx-------download block number:{}, hash:{}, previous hash:{}",
                                    container.getBlock().getBlockNum(),
                                    Hex.toHexString(container.getBlock().getBlockHash()),
                                    Hex.toHexString(previousHash));

                            containerList.add(container);

                            if (container.getBlock().getBlockNum() <= 0) {
                                break;
                            }
                            counter++;
                        }

                        if (findAll) {
                            for (BlockContainer container: containerList) {
                                this.blockStore.saveBlockContainer(this.chainID, container, false);
                            }
                            logger.debug("++ctx-----------------------re-branch.....");
                            // change to more difficult chain
                            reBranch(tip);
                            miningFlag = true;
                            break;
                        } else {
                            containerList.clear();
                        }
                    } else {
                        // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                        // get from dht
                        BlockContainer immutableContainer1 = getBlockContainerFromDHTByHash(immutableBlockHash);

                        if (null == immutableContainer1) {
                            logger.info("Chain ID[{}]: Cannot find immutable block container1", new String(this.chainID));
                            continue;
                        }

                        byte[] immutableBlockHash1 = immutableContainer1.getBlock().getImmutableBlockHash();
                        BlockContainer immutableContainer2 = getBlockContainerFromDHTByHash(immutableBlockHash1);

                        if (null == immutableContainer2) {
                            logger.info("Chain ID[{}]: Cannot find immutable block container1", new String(this.chainID));
                            continue;
                        }

                        byte[] warningRangeHash = immutableContainer2.getBlock().getImmutableBlockHash();

                        if (this.blockStore.isMainChainBlock(this.chainID, warningRangeHash)) {
                            // fork point in warning range
                            // vote when fork point between mutable range and warning range
                            logger.debug("++ctx-----------------------go to vote.....");
                            votingFlag = true;
                            break;
                        } else {
                            // fork point out of warning range, maybe it's an attack chain
                            logger.debug("++ctx-----------------------an attack chain.....");
                            continue;
                        }
                    }
                } catch (Exception e) {
                    logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
                }
            }

            // vote for new chain
            if (votingFlag) {
                lastVotingTime = startVotingTime;
                startVotingTime = System.currentTimeMillis() / 1000;
                if (startVotingTime - lastVotingTime < 1) {
                    logger.info("Chain ID[{}]: Voting Sleep 1 s", new String(this.chainID));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.info(new String(this.chainID) + ":" + e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }

                Vote bestVote = vote();
                tryChangeToBestVote(bestVote);
                continue;
            }

            // mine
            if (miningFlag) {
                lastMiningTime = startMiningTime;
                startMiningTime = System.currentTimeMillis() / 1000;
                if (startMiningTime - lastMiningTime < 1) {
                    logger.info("Chain ID[{}]: Mining Sleep 1 s", new String(this.chainID));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.info(new String(this.chainID) + ":" + e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }

                if (minable(new ByteArrayWrapper(this.chainID))) {
                    BlockContainer blockContainer = mineBlock(new ByteArrayWrapper(chainID));
                    // the best block is parent block of the tip
                    if (null != blockContainer) {
                        StateDB track = this.stateDB.startTracking(this.chainID);

                        if (tryToConnect(new ByteArrayWrapper(chainID), blockContainer, track)) {
                            try {
                                // after chain change
                                // 1. save block
                                // 2. save best block hash
                                // 3. commit new state
                                // 4. set best block
                                // 5. add new block peer to peer pool
                                // 6. update tx pool
                                // 7. publish new block
                                this.blockStore.saveBlockContainer(this.chainID, blockContainer, true);
                                track.setBestBlockHash(this.chainID, blockContainer.getBlock().getBlockHash());
                                track.commit();
                                setBestBlockContainer(new ByteArrayWrapper(chainID), blockContainer);
                                this.peerManager.addNewBlockPeer(blockContainer.getBlock().getMinerPubkey());
                            } catch (Exception e) {
                                logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
                            }

                            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);
                            this.txPool.recheckAccoutTx(accounts);

                            publishBestBlock(new ByteArrayWrapper(chainID));
                        }
                    }
                }
            }
        }
    }

    /**
     * Is block synchronization complete
     * @return
     */
    private boolean isSyncComplete() {
        if (null != this.syncBlock && this.syncBlock.getBlockNum() == 0) {
            return true;
        }
        return false;
    }

    private void requestSyncBlock(ByteArrayWrapper chainID) {
        Block syncBlock = this.syncBlocks.get(chainID);
        requestBlock(chainID, syncBlock.getBlockHash());
    }

    private void tryToSync(ByteArrayWrapper chainID) {
        Block syncBlock = this.syncBlocks.get(chainID);
        BlockContainer blockContainer = this.blockContainerMap.get(chainID).
                get(new ByteArrayWrapper(syncBlock.getPreviousBlockHash()));

        if (null != blockContainer) {
            PeerManager peerManager = this.peerManagers.get(chainID);
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
                    peerManager.addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
                }
            } catch (Exception e) {
                logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            }
        }
    }

    private boolean SyncBlock(ByteArrayWrapper chainID, BlockContainer blockContainer) {
        PeerManager peerManager = this.peerManagers.get(chainID);
        Block syncBlock = this.syncBlocks.get(chainID);

        if (null == blockContainer) {
            return false;
        }

        if (null == syncBlock || syncBlock.getBlockNum() <= 0) {
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
                peerManager.addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * sync a block for more state when needed
     */
    private void syncBlockForMoreState(ByteArrayWrapper chainID) {
        if (null != this.syncBlock && this.syncBlock.getBlockNum() >0) {
            BlockContainer blockContainer = getBlockContainerFromDHTByHash(this.syncBlock.getPreviousBlockHash());
            if (null != blockContainer) {
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
                        this.syncBlock = blockContainer.getBlock();
                        this.peerManager.addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
                    }
                } catch (Exception e) {
                    logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 检测分叉点和连续性
     * @param chainID
     * @param blockContainer
     * @return
     */
    private boolean checkBranch(ByteArrayWrapper chainID, BlockContainer blockContainer) {
        // download block first
//        int counter = 0;
//        byte[] previousHash = blockContainer.getBlock().getPreviousBlockHash();
//        List<BlockContainer> containerList = new ArrayList<>();
//
//        containerList.add(blockContainer);
//
//        boolean findAll = true;
//        while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {
//
//            Block block = this.blockStore.getBlockByHash(chainID.getData(), previousHash);
//            if (null != block) {
//                // found in local
//                logger.debug("+ctx--------found in local, hash:{}",
//                        Hex.toHexString(previousHash));
//                break;
//            }
//            // get from dht
//            BlockContainer container = getBlockContainerFromDHTByHash(previousHash);
//
//            if (null == container) {
//                logger.debug("+ctx--------Cannot get container from dht, hash:{}",
//                        Hex.toHexString(previousHash));
//                findAll = false;
//                break;
//            }
//
//            previousHash = container.getBlock().getPreviousBlockHash();
//
//            logger.debug("+ctx-------download block number:{}, hash:{}, previous hash:{}",
//                    container.getBlock().getBlockNum(),
//                    Hex.toHexString(container.getBlock().getBlockHash()),
//                    Hex.toHexString(previousHash));
//
//            containerList.add(container);
//
//            if (container.getBlock().getBlockNum() <= 0) {
//                break;
//            }
//            counter++;
//        }
//
//        if (findAll) {
//            for (BlockContainer container: containerList) {
//                this.blockStore.saveBlockContainer(this.chainID, container, false);
//            }
//            logger.debug("++ctx-----------------------re-branch.....");
//            // change to more difficult chain
//            reBranch(tip);
//            miningFlag = true;
//            break;
//        } else {
//            containerList.clear();
//        }

        return true;
    }

    private boolean tryToRebranch(ByteArrayWrapper chainID, BlockContainer targetBlockContainer) {
        try {
            //try to roll back and reconnect
            StateDB track = stateDB.startTracking(chainID.getData());

            List<BlockContainer> undoBlockContainers = new ArrayList<>();
            List<BlockContainer> newBlockContainers = new ArrayList<>();
            if (!blockStore.getForkBlockContainersInfo(chainID.getData(), targetBlockContainer,
                    this.bestBlockContainer, undoBlockContainers, newBlockContainers)) {
                logger.error("Chain ID[{}]: Cannot get fork block, best block[{}], target block[{}]",
                        new String(chainID.getData()),
                        Hex.toHexString(this.bestBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(targetBlockContainer.getBlock().getBlockHash()));

                return false;
            }

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (!this.stateProcessor.rollback(undoBlockContainer, track)) {
                    logger.error("Chain ID[{}]: Roll back fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(undoBlockContainer.getBlock().getBlockHash()));
                    return false;
                }
            }

            int size = newBlockContainers.size();
            for (int i = size - 1; i >= 0; i--) {

                if (!isValidBlockContainer(newBlockContainers.get(i), track)) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
                }

                ImportResult result = this.stateProcessor.forwardProcess(newBlockContainers.get(i), track);
                // if need sync more block
                if (result == ImportResult.NO_ACCOUNT_INFO && !isSyncComplete()) {
                    syncBlockForMoreState(new ByteArrayWrapper(chainID.getData()));
                    i++;
                    continue;
                }

                if (result != ImportResult.IMPORTED_BEST) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(chainID.getData()),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
                }

                peerManagers.get(chainID).addNewBlockPeer(newBlockContainers.get(i).getBlock().getMinerPubkey());
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
            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(undoBlockContainers);
            accounts.addAll(extractAccountFromBlockContainer(newBlockContainers));
            this.txPool.recheckAccoutTx(accounts);

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (null != undoBlockContainer.getTx()) {
                    this.txPool.addTx(undoBlockContainer.getTx());
                }
            }
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * re-branch chain
     * @param targetBlockContainer block that chain will change to
     */
    private boolean reBranch(BlockContainer targetBlockContainer) {
        try {
            //try to roll back and reconnect
            StateDB track = stateDB.startTracking(this.chainID);

            List<BlockContainer> undoBlockContainers = new ArrayList<>();
            List<BlockContainer> newBlockContainers = new ArrayList<>();
            if (!blockStore.getForkBlockContainersInfo(this.chainID, targetBlockContainer,
                    this.bestBlockContainer, undoBlockContainers, newBlockContainers)) {
                logger.error("Chain ID[{}]: Cannot get fork block, best block[{}], target block[{}]",
                        new String(this.chainID),
                        Hex.toHexString(this.bestBlockContainer.getBlock().getBlockHash()),
                        Hex.toHexString(targetBlockContainer.getBlock().getBlockHash()));

                return false;
            }

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (!this.stateProcessor.rollback(undoBlockContainer, track)) {
                    logger.error("Chain ID[{}]: Roll back fail, block hash:{}",
                            new String(this.chainID),
                            Hex.toHexString(undoBlockContainer.getBlock().getBlockHash()));
                    return false;
                }
            }

            int size = newBlockContainers.size();
            for (int i = size - 1; i >= 0; i--) {

                if (!isValidBlockContainer(newBlockContainers.get(i), track)) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(this.chainID),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
                }

                ImportResult result = this.stateProcessor.forwardProcess(newBlockContainers.get(i), track);
                // if need sync more block
                if (result == ImportResult.NO_ACCOUNT_INFO && !isSyncComplete()) {
                    syncBlockForMoreState(new ByteArrayWrapper(this.chainID));
                    i++;
                    continue;
                }

                if (result != ImportResult.IMPORTED_BEST) {
                    logger.error("Chain ID[{}]: Import block fail, block hash:{}",
                            new String(this.chainID),
                            Hex.toHexString(newBlockContainers.get(i).getBlock().getBlockHash()));
                    return false;
                }

                this.peerManager.addNewBlockPeer(newBlockContainers.get(i).getBlock().getMinerPubkey());
            }

            // after chain change
            // 1. save block
            // 2. save best block hash
            // 3. commit new state
            // 4. set best block
            // 5. save and add new block peer to peer pool
            // 6. update tx pool
            // 7. publish new block

            this.blockStore.reBranchBlocksWithContainers(this.chainID, undoBlockContainers, newBlockContainers);

            track.setBestBlockHash(this.chainID, targetBlockContainer.getBlock().getBlockHash());

            track.commit();

            setBestBlockContainer(new ByteArrayWrapper(chainID), targetBlockContainer);

            publishBestBlock(new ByteArrayWrapper(chainID));

            // update tx pool
            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(undoBlockContainers);
            accounts.addAll(extractAccountFromBlockContainer(newBlockContainers));
            this.txPool.recheckAccoutTx(accounts);

            for (BlockContainer undoBlockContainer : undoBlockContainers) {
                if (null != undoBlockContainer.getTx()) {
                    this.txPool.addTx(undoBlockContainer.getTx());
                }
            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    private Vote tryToVote(ByteArrayWrapper chainID) {
        // try to use all peers to vote
        PeerManager peerManager = this.peerManagers.get(chainID);

        int counter = peerManager.getPeerNumber();

        counter = counter > 0 ? (int)Math.log(counter) : 0;

        List<Block> blocks = this.votingBlocks.get(chainID);

        int size = blocks.size();

        if (counter > size) {
            counter -= size;

            while (!Thread.interrupted() && counter > 0) {
                byte[] peer = peerManager.getBlockPeerRandomly();

                if (null != peer) {
                    requestTipBlockForVotingFromPeer(chainID, peer);
                    counter--;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.info(new String(this.chainID) + ":" + e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }
            }

            return null;
        } else {

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
            votingPool.clearVotingPool();

            blocks.clear();

            return bestVote;
        }
    }

    /**
     * vote for best chain
     * @return voting result
     */
    private Vote vote() {
        logger.debug("---ctx---------------voting-----------");
        // try to use all peers to vote
        int counter = peerManager.getPeerNumber();

        counter = counter > 0 ? (int)Math.log(counter) : 0;

        while (!Thread.interrupted() && counter > 0) {
            byte[] peer = peerManager.getBlockPeerRandomly();
            if (null != peer) {
                logger.debug("---ctx---------------voting peer:{}", Hex.toHexString(peer));
                Block block = getTipBlockFromPeer(peer);
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

            counter--;
        }

        Vote bestVote = votingPool.getBestVote();

        if (null != bestVote) {
            logger.debug("Chain ID[{}]: Best vote:{}", new String(chainID), bestVote.toString());
        }

        // clear voting pool for next time when voting end
        votingPool.clearVotingPool();

        return bestVote;
    }

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
     * first sync when follow a chain
     * @param bestVote best vote
     * @return true[success]/false[fail]
     */
    private boolean initialSync(ByteArrayWrapper chainID, Vote bestVote) {
        if (null == bestVote) {
            logger.error("Chain ID[{}]: Best vote is null.", new String(chainID.getData()));
            return false;
        }

        try {
            this.blockStore.removeChain(this.chainID);
            this.stateDB.clearAllState(this.chainID);

            Map<ByteArrayWrapper, BlockContainer> blockContainers = this.blockContainerMap.get(chainID);
            BlockContainer bestVoteBlockContainer = blockContainers.get(new ByteArrayWrapper(bestVote.getBlockHash()));

            if (null == bestVoteBlockContainer) {
                logger.error("Chain ID[{}]: Best vote block is null.", new String(this.chainID));

                requestBlock(chainID, bestVote.getBlockHash());

                return false;
            }

            // after sync
            // 1. save block
            // 2. save best and sync block hash
            // 3. commit new state
            // 4. set best and sync block
            // 5. add old block peer to peer pool

            // initial sync from best vote
            StateDB track = this.stateDB.startTracking(chainID.getData());
            StateProcessor stateProcessor = this.stateProcessors.get(chainID);
            if (!stateProcessor.backwardProcess(bestVoteBlockContainer, track)) {
                logger.error("Chain ID[{}]: Process block[{}] fail!",
                        new String(chainID.getData()), Hex.toHexString(bestVoteBlockContainer.getBlock().getBlockHash()));
                return false;
            }

            this.blockStore.saveBlockContainer(chainID.getData(), bestVoteBlockContainer, true);

            track.setBestBlockHash(chainID.getData(), bestVoteBlockContainer.getBlock().getBlockHash());
            track.setSyncBlockHash(chainID.getData(), bestVoteBlockContainer.getBlock().getBlockHash());

            this.bestBlockContainers.put(chainID, bestVoteBlockContainer);
            this.syncBlocks.put(chainID, bestVoteBlockContainer.getBlock());

            PeerManager peerManager = this.peerManagers.get(chainID);
            peerManager.addOldBlockPeer(bestVoteBlockContainer.getBlock().getMinerPubkey());

            track.commit();
        } catch (Exception e) {
            logger.error(new String(chainID.getData()) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    private boolean tryToChangeToBestVote(ByteArrayWrapper chainID, Vote bestVote) {
        if (null != bestVote) {
            return false;
        }

        try {
            BlockContainer blockContainer = this.blockStore.
                    getBlockContainerByHash(chainID.getData(), bestVote.getBlockHash());

            Map<ByteArrayWrapper, BlockContainer> blockContainers = this.blockContainerMap.get(chainID);

            if (null == blockContainer) {

            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
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
    private boolean tryChangeToBestVote(Vote bestVote) {
        try {
            BlockContainer bestContainer = getBlockContainerFromDHTByHash(bestVote.getBlockHash());

            if (null == bestContainer) {
                logger.info("Chain ID[{}]: Cannot find best block container, hash[{}]",
                        new String(this.chainID), Hex.toHexString(bestVote.getBlockHash()));
                return false;
            }

            // if best vote is genesis block, immutable hash is not on main chain, also we cannot
            // get block container by immutable hash from genesis block

            byte[] immutableBlockHash = bestContainer.getBlock().getImmutableBlockHash();
            if (this.blockStore.isMainChainBlock(this.chainID, immutableBlockHash)) {
                // 分叉点在mutable range之内
                // download block first
                int counter = 0;
                byte[] previousHash = bestContainer.getBlock().getPreviousBlockHash();
                List<BlockContainer> containerList = new ArrayList<>();

                containerList.add(bestContainer);

                while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {
                    Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                    if (null != block) {
                        // found in local
                        logger.debug("+ctx--------found in local, hash:{}",
                                Hex.toHexString(previousHash));
                        break;
                    }
                    // get from dht
                    BlockContainer container = getBlockContainerFromDHTByHash(previousHash);

                    if (null == container) {
                        logger.debug("+ctx--------Cannot get container from dht, hash:{}",
                                Hex.toHexString(previousHash));
                        return false;
                    }

                    previousHash = container.getBlock().getPreviousBlockHash();

                    logger.debug("----vote---download block number:{}, hash:{}, previous hash:{}",
                            container.getBlock().getBlockNum(),
                            Hex.toHexString(container.getBlock().getBlockHash()),
                            Hex.toHexString(previousHash));

                    containerList.add(container);

                    if (container.getBlock().getBlockNum() <= 0) {
                        break;
                    }
                    counter++;
                }


                for (BlockContainer container: containerList) {
                    this.blockStore.saveBlockContainer(this.chainID, container, false);
                }
                logger.debug("++ctx----------------------vote---re-branch.....");
                // change to more difficult chain
                reBranch(bestBlockContainer);
            } else {
                // 分叉点在mutable range之外，判断是否在3倍的mutable range之内
                // get from dht
                BlockContainer immutableContainer1 = getBlockContainerFromDHTByHash(immutableBlockHash);

                if (null == immutableContainer1) {
                    logger.info("Chain ID[{}]: Cannot find immutable block container1", new String(this.chainID));
                    return false;
                }

                byte[] immutableBlockHash1 = immutableContainer1.getBlock().getImmutableBlockHash();
                BlockContainer immutableContainer2 = getBlockContainerFromDHTByHash(immutableBlockHash1);

                if (null == immutableContainer2) {
                    logger.info("Chain ID[{}]: Cannot find immutable block container1", new String(this.chainID));
                    return false;
                }

                byte[] warningRangeHash = immutableContainer2.getBlock().getImmutableBlockHash();

                if (this.blockStore.isMainChainBlock(this.chainID, warningRangeHash)) {
                    // fork point in warning range
                    // vote when fork point between mutable range and warning range
                    logger.debug("++ctx-----------------------go to vote.....");
                    // be as a new chain when fork point between mutable range and warning range
                    // re-init tx pool and chain
                    this.txPool.reinit();
                    initialSync(new ByteArrayWrapper(this.chainID), bestVote);
                    return true;
                } else {
                    // fork point out of warning range, maybe it's an attack chain
                    logger.debug("++ctx-----------------------an attack chain.....");
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    private boolean requestTipBlockFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestBlock(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.BLOCK,
                new ByteArrayWrapper(blockHash));
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestBlockForSync(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.BLOCK_FOR_SYNC);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTipTxFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_TX);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTx(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX,
                new ByteArrayWrapper(txid));
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTxForSync(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX_FOR_SYNC);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTipTxForPoolFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_TX_FOR_POOL);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTxForPool(ByteArrayWrapper chainID, byte[] txid) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(txid);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TX_FOR_POOL);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestTipBlockForVotingFromPeer(ByteArrayWrapper chainID, byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(chainID));
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.TIP_BLOCK_FOR_VOTING);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    private boolean requestBlockForVoting(ByteArrayWrapper chainID, byte[] blockHash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(blockHash);
        DataIdentifier dataIdentifier = new DataIdentifier(chainID, DataType.BLOCK_FOR_VOTING);
        return TorrentDHTEngine.getInstance().request(spec, this, dataIdentifier);
    }

    /**
     * get tip block from peer
     * @param peer peer pubKey
     * @return tip block or null
     */
    private Block getTipBlockFromPeer(byte[] peer) {
        try {
            logger.debug("+ctx----------------peer: {}", Hex.toHexString(peer));
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(new ByteArrayWrapper(this.chainID)));
            byte[] encode = TorrentDHTEngine.getInstance().dhtGet(spec);

            if (null != encode) {
                logger.debug("+ctx----------------encode: {}", Hex.toHexString(encode));
                MutableItemValue value = new MutableItemValue(encode);
                if (null != value.getPeer()) {
                    logger.debug("-ctx----------------get peer: {}", Hex.toHexString(value.getPeer()));
                    this.peerManager.addBlockPeer(value.getPeer());
                }
                if (null != value.getHash()) {
                    logger.debug("-ctx----------------get hash: {}", Hex.toHexString(value.getHash()));
                    return getBlockFromDHTByHash(value.getHash());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * get tip block container from peer
     * @param peer peer pubKey
     * @return tip block container or null
     */
    private BlockContainer getTipBlockContainerFromPeer(byte[] peer) {

        logger.debug("++ctx----------------peer: {}", Hex.toHexString(peer));
        try {
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(new ByteArrayWrapper(this.chainID)));

            int i = 0;
            byte[] encode = null;
            while (null == encode && i < LOOP_LIMIT) {
                encode = TorrentDHTEngine.getInstance().dhtGet(spec);
                i++;
                logger.debug("+ctx--try to get mutable item {} time", i);
            }

            if (null != encode) {
                logger.debug("++ctx----------------encode: {}", Hex.toHexString(encode));
                MutableItemValue value = new MutableItemValue(encode);
                if (null != value.getPeer()) {
                    logger.debug("++ctx----------------get peer: {}", Hex.toHexString(value.getPeer()));
                    this.peerManager.addBlockPeer(value.getPeer());
                }
                if (null != value.getHash()) {
                    logger.debug("++ctx----------------get hash: {}", Hex.toHexString(value.getHash()));
                    return getBlockContainerFromDHTByHash(value.getHash());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * publish tip block on main chain to dht
     */
    private void publishBestBlock(ByteArrayWrapper chainID) {
        BlockContainer bestBlockContainer = this.bestBlockContainers.get(chainID);

        if (null != bestBlockContainer) {
            if (null != bestBlockContainer.getTx()) {
                // put immutable tx
                DHT.ImmutableItem immutableItem =
                        new DHT.ImmutableItem(bestBlockContainer.getTx().getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }

            // put immutable block
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(bestBlockContainer.getBlock().getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            byte[] peer = peerManagers.get(chainID).getMutableRangePeerRandomly();
            MutableItemValue mutableItemValue = new MutableItemValue(bestBlockContainer.getBlock().getBlockHash(), peer);

            // put mutable item
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    mutableItemValue.getEncoded(), this.blockSalts.get(chainID));
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * get a block from dht
     * @param hash block hash
     * @return block get from dht, null otherwise
     */
    private Block getBlockFromDHTByHash(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);

        int i = 0;
        byte[] blockEncode = null;
        while (null == blockEncode && i < LOOP_LIMIT) {
            blockEncode = TorrentDHTEngine.getInstance().dhtGet(spec);
            i++;
            logger.debug("+ctx--try to get block {} time", i);
        }

//        byte[] blockEncode = TorrentDHTEngine.getInstance().dhtGet(spec);

        if (null == blockEncode) {
            logger.info("Chain ID[{}]: block encode still is null", new String(this.chainID));
            return null;
        }

        return new Block(blockEncode);
    }

    /**
     * get a block container from dht
     * @param blockHash block hash
     * @return block container get from dht, null otherwise
     */
    private BlockContainer getBlockContainerFromDHTByHash(byte[] blockHash) {
        Block block = getBlockFromDHTByHash(blockHash);

        if (null == block) {
            return null;
        }

        BlockContainer blockContainer = new BlockContainer(block);

        if (null != block.getTxHash()) {
            Transaction tx = getTxFromDHTByHash(block.getTxHash());

            if (null == tx) {
                return null;
            }

            blockContainer.setTx(tx);
        }

        return blockContainer;
    }

    /**
     * get a tx from dht on tx channel
     * @param peer peer to get
     * @return transaction
     */
    private Transaction getTxFromPeer(byte[] peer) {
        try {
            logger.debug("Chain ID[{}]: get tx from peer[{}]",
                    new String(this.chainID), Hex.toHexString(peer));

            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalts.get(new ByteArrayWrapper(this.chainID)));
            byte[] encode = TorrentDHTEngine.getInstance().dhtGet(spec);

            if (null != encode) {
                MutableItemValue value = new MutableItemValue(encode);
                if (null != value.getPeer()) {
                    this.peerManager.addTxPeer(value.getPeer());
                }

                if (null != value.getHash()) {
                    return getTxFromDHTByHash(value.getHash());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * get a tx by hash from dht
     * @param hash tx hash
     * @return tx get from dht, null otherwise
     */
    private Transaction getTxFromDHTByHash(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash);

        int i = 0;
        byte[] txEncode = null;
        while (null == txEncode && i < LOOP_LIMIT) {
            txEncode = TorrentDHTEngine.getInstance().dhtGet(spec);
            i++;
            logger.debug("+ctx--try to get tx {} time", i);
        }

//            byte[] txEncode = TorrentDHTEngine.getInstance().dhtGet(spec);

        if (null == txEncode) {
            logger.info("Chain ID[{}]: tx encode still is null", new String(this.chainID));
            return null;
        }

        return TransactionFactory.parseTransaction(txEncode);
    }

    /**
     * publish tip block on main chain to dht
     */
    private void publishBestTx() {
        Transaction tx = this.txPool.getBestTransaction();
        if (null != tx) {
            // put mutable tx
            publishTransaction(tx);
        }
    }

    /**
     * put a tx in mutable item
     * @param tx tx to publish
     */
    private void publishTransaction(Transaction tx) {
        if (null != tx) {
            // put immutable tx first
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            // put mutable item
            // get max fee peer
            byte[] peer = this.txPool.getOptimalPeer();
            if (null == peer) {
                // get active peer from other peer
                peer = this.peerManager.getOptimalTxPeer();
            }
            if (null == peer) {
                // get myself
                peer = AccountManager.getInstance().getKeyPair().first;
            }

            MutableItemValue value = new MutableItemValue(tx.getTxID(), peer);
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second, value.getEncoded(), this.txSalts.get(new ByteArrayWrapper(this.chainID)));
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * get block container randomly from block store
     * @return block container, null otherwise
     */
    private BlockContainer getBlockContainerRandomlyFromDB() {
        if (null != this.bestBlockContainer) {
            int currentNumber = (int) this.bestBlockContainer.getBlock().getBlockNum();
            Random random = new Random(System.currentTimeMillis());
            try {
                return this.blockStore.getMainChainBlockContainerByNumber(this.chainID,
                        random.nextInt(currentNumber + 1));
            } catch (Exception e) {
                logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * connect a block
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if connect successfully, false otherwise
     */
    private boolean tryToConnect(ByteArrayWrapper chainID, final BlockContainer blockContainer, StateDB stateDB) {
        // if main chain
        if (Arrays.equals(this.bestBlockContainers.get(chainID).getBlock().getBlockHash(),
                blockContainer.getBlock().getPreviousBlockHash())) {
            // main chain
            if (!isValidBlockContainer(blockContainer, stateDB)) {
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
     * @param blockContainer best block container
     */
    public void setBestBlockContainer(ByteArrayWrapper chainID, BlockContainer blockContainer) {
        this.bestBlockContainers.put(chainID, blockContainer);
    }

    /**
     * get best block of this chain
     * @return best block container
     */
    public BlockContainer getBestBlockContainer() {
        return this.bestBlockContainers.get(chainID);
    }

    /**
     * check if a block valid
     * @param block block to check
     * @param stateDB state db
     * @return true if valid, false otherwise
     */
    private boolean isValidBlock(Block block, StateDB stateDB) {
//        // 是否本链
//        if (!Arrays.equals(this.chainID, block.getChainID())) {
//            logger.error("ChainID[{}]: ChainID mismatch!", new String(this.chainID));
//            return false;
//        }

        // 时间戳检查
        if (block.getTimeStamp() > System.currentTimeMillis() / 1000) {
            logger.error("ChainID[{}]: Block[{}] Time is in the future!",
                    new String(this.chainID), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 区块内部自检
        if (!block.isBlockParamValidate()) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(this.chainID), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 区块签名检查
        if (!block.verifyBlockSig()) {
            logger.error("ChainID[{}]: Block[{}] Bad Signature!",
                    new String(this.chainID), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        // 是否孤块
        try {
            if (null == this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash())) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        // POT共识验证
        if (!verifyPOT(block, stateDB)) {
            logger.error("ChainID[{}]: Block[{}] Validate block param error!",
                    new String(this.chainID), Hex.toHexString(block.getBlockHash()));
            return false;
        }

        return true;
    }

    /**
     * check if a block container valid
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if valid, false otherwise
     */
    private boolean isValidBlockContainer(BlockContainer blockContainer, StateDB stateDB) {
        return isValidBlock(blockContainer.getBlock(), stateDB);
    }

    /**
     * check pot consensus
     * @param block block to check
     * @param stateDB state db
     * @return true if ok, false otherwise
     */
    private boolean verifyPOT(Block block, StateDB stateDB) {
        try {
            byte[] pubKey = block.getMinerPubkey();

            BigInteger power = stateDB.getNonce(this.chainID, pubKey);
            if (null == power) {
                logger.error("ChainID[{}]: Miner[{}] has no power!",
                        new String(this.chainID), Hex.toHexString(pubKey));
                return false;
            }
            logger.info("Chain ID[{}]: Address: {}, mining power: {}",
                    new String(this.chainID), Hex.toHexString(pubKey), power);

            Block parentBlock = this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash());
            if (null == parentBlock) {
                logger.error("ChainID[{}]: Block[{}] Cannot find parent!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check base target
            BigInteger baseTarget = this.pot.calculateRequiredBaseTarget(this.chainID, parentBlock, this.blockStore);
            if (0 != baseTarget.compareTo(block.getBaseTarget())) {
                logger.error("ChainID[{}]: Block[{}] base target error!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check generation signature
            byte[] genSig = this.pot.calculateGenerationSignature(parentBlock.getGenerationSignature(), pubKey);
            if (!Arrays.equals(genSig, block.getGenerationSignature())) {
                logger.error("ChainID[{}]: Block[{}] generation signature error!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check cumulative difficulty
            BigInteger culDifficulty = this.pot.calculateCumulativeDifficulty(
                    parentBlock.getCumulativeDifficulty(), baseTarget);
            if (0 != culDifficulty.compareTo(block.getCumulativeDifficulty())) {
                logger.error("ChainID[{}]: Block[{}] Cumulative difficulty error!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }

            // check if target >= hit
//            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
//                    block.getTimeStamp() - parentBlock.getTimeStamp());

            BigInteger hit = this.pot.calculateRandomHit(genSig);
            long timeInterval = block.getTimeStamp() - parentBlock.getTimeStamp();

            // verify hit
            if (!this.pot.verifyHit(hit, baseTarget, power, timeInterval)) {
                logger.error("ChainID[{}]: The block[{}] does not meet the pot consensus!!",
                        new String(this.chainID), Hex.toHexString(block.getBlockHash()));
                return false;
            }

        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * check if be able to mine now
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

        if (null == immutableBlockHash && !isSyncComplete()) {
            tryToSync(chainID);
        } else {
            return null;
        }

        // if block is too less, sync more
        if (null == immutableBlockHash && !isSyncComplete()) {
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
     * Start activities of this chain, mainly including votint and mining.
     *
     * @return boolean successful or not.
     */
    public boolean start() {
        // chain init
        if (!init()) {
            return false;
        }

        logger.info("Chain ID[{}]: Start voting and tx thread...", new String(this.chainID));
        votingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                blockChainProcess();
            }
        }, new String(this.chainID) + "BlockThread");
        votingThread.start();

        return true;
    }

    /**
     * Stop all activities of this chain.
     */
    public void stop() {
        if (null != votingThread) {
            logger.info("Chain ID[{}]: Stop voting thread.", new String(this.chainID));
            votingThread.interrupt();
        }
    }

    /**
     * get transaction pool
     * @return tx pool
     */
    public TransactionPool getTransactionPool() {
        return this.txPool;
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
            case TIP_BLOCK: {
                if (null == item) {
                    return;
                }

                MutableItemValue mutableItemValue = new MutableItemValue(item);
                requestBlock(dataIdentifier.getChainID(), mutableItemValue.getHash());
                break;
            }
            case TIP_TX: {
                if (null == item) {
                    return;
                }

                MutableItemValue mutableItemValue = new MutableItemValue(item);
                requestTx(dataIdentifier.getChainID(), mutableItemValue.getHash());
                break;
            }
            case BLOCK: {
                if (null == item) {
                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(dataIdentifier.getHash(), null);
                    return;
                }

                Block block = new Block(item);
                if (null != block.getTxHash()) {
                    List<Transaction> txs = this.txMap.get(dataIdentifier.getChainID());
                    for (Transaction tx: txs) {
                        if (Arrays.equals(block.getTxHash(), tx.getTxID())) {
                            List<Block> blocks = this.blockMap.get(dataIdentifier.getChainID());

                            BlockContainer blockContainer = new BlockContainer(block, tx);

                            Map<ByteArrayWrapper, BlockContainer> blockContainers =
                                    this.blockContainerMap.get(dataIdentifier.getChainID());
                            blockContainers.put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);

                            blocks.remove(block);
                            return;
                        }
                    }

                    requestTx(dataIdentifier.getChainID(), block.getTxHash());

                    this.blockMap.get(dataIdentifier.getChainID()).add(block);
                } else {
                    BlockContainer blockContainer = new BlockContainer(block);

                    this.blockContainerMap.get(dataIdentifier.getChainID()).
                            put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);
                }
            }
            case TX: {
                List<Block> blocks = this.blockMap.get(dataIdentifier.getChainID());

                if (null == item) {
                    for (Block block: blocks) {
                        if (Arrays.equals(block.getTxHash(), dataIdentifier.getHash().getData())) {
                            this.blockContainerMap.get(dataIdentifier.getChainID()).
                                    put(dataIdentifier.getHash(), null);
                        }
                    }
                    return;
                }

                Transaction tx = TransactionFactory.parseTransaction(item);

                for (Block block: blocks) {
                    if (Arrays.equals(block.getTxHash(), tx.getTxID())) {
                        BlockContainer blockContainer = new BlockContainer(block, tx);

                        Map<ByteArrayWrapper, BlockContainer> blockContainers = this.blockContainerMap.get(dataIdentifier.getChainID());
                        blockContainers.put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);

                        blocks.remove(block);
                        return;
                    }
                }

                List<Transaction> txs = this.txMap.get(dataIdentifier.getChainID());

                txs.add(tx);

                break;
            }
            case TIP_BLOCK_FOR_VOTING: {
                if (null == item) {
                    return;
                }

                MutableItemValue mutableItemValue = new MutableItemValue(item);
                requestBlockForVoting(dataIdentifier.getChainID(), mutableItemValue.getHash());
                break;
            }
            case BLOCK_FOR_VOTING: {
                if (null == item) {
                    return;
                }

                Block block = new Block(item);
                this.votingBlocks.get(dataIdentifier.getChainID()).add(block);
                break;
            }
            case TIP_TX_FOR_POOL: {
                if (null == item) {
                    return;
                }

                MutableItemValue mutableItemValue = new MutableItemValue(item);
                requestTxForPool(dataIdentifier.getChainID(), mutableItemValue.getHash());
                break;
            }
            case TX_FOR_POOL: {
                if (null == item) {
                    return;
                }

                Transaction tx = TransactionFactory.parseTransaction(item);
                this.txMapForPool.get(dataIdentifier.getChainID()).add(tx);
                break;
            }
            case BLOCK_FOR_SYNC: {
                if (null == item) {
                    return;
                }

                Block block = new Block(item);
                if (null != block.getTxHash()) {
                    List<Transaction> txs = this.txMapForSync.get(dataIdentifier.getChainID());
                    for (Transaction tx: txs) {
                        if (Arrays.equals(block.getTxHash(), tx.getTxID())) {
                            List<Block> blocks = this.blockMapForSync.get(dataIdentifier.getChainID());

                            BlockContainer blockContainer = new BlockContainer(block, tx);

                            Map<ByteArrayWrapper, BlockContainer> blockContainers =
                                    this.blockContainerMapForSync.get(dataIdentifier.getChainID());
                            blockContainers.put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);

                            blocks.remove(block);
                            return;
                        }
                    }

                    requestTxForSync(dataIdentifier.getChainID(), block.getTxHash());

                    this.blockMapForSync.get(dataIdentifier.getChainID()).add(block);
                } else {
                    BlockContainer blockContainer = new BlockContainer(block);

                    this.blockContainerMapForSync.get(dataIdentifier.getChainID()).
                            put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);
                }
            }
            case TX_FOR_SYNC: {
                if (null == item) {
                    return;
                }

                Transaction tx = TransactionFactory.parseTransaction(item);

                List<Block> blocks = this.blockMapForSync.get(dataIdentifier.getChainID());

                for (Block block: blocks) {
                    if (Arrays.equals(block.getTxHash(), tx.getTxID())) {
                        BlockContainer blockContainer = new BlockContainer(block, tx);

                        this.blockContainerMap.get(dataIdentifier.getChainID()).
                                put(new ByteArrayWrapper(block.getBlockHash()), blockContainer);

                        blocks.remove(block);
                        return;
                    }
                }

                this.txMapForSync.get(dataIdentifier.getChainID()).add(tx);

                break;
            }
            default: {

            }
        }
    }

}
