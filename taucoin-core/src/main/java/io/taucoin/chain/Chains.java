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

    private static final int TIMEOUT = 10;

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

        return true;
    }

    private void multiChain() {
        for (ByteArrayWrapper chainID: this.chainIDs) {
            logger.debug("Chain ID:{}", new String(chainID.getData()));
            if (null == this.syncBlocks.get(chainID)) {
                // empty chain
            } else {
                //
            }
        }
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
            if (!initialSync(bestVote)) {
                logger.error("Chain ID[{}]: Initial sync fail!", new String(this.chainID));
            }
        }

        // if offline too long, vote as a new chain
        if (null != this.bestBlockContainer &&
                (System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp()) >
                        ChainParam.WARNING_RANGE * ChainParam.DEFAULT_BLOCK_TIME) {
            Vote bestVote = vote();
            if (!initialSync(bestVote)) {
                logger.error("Chain ID[{}]: Initial sync fail!", new String(this.chainID));
            }
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

                if (minable()) {
                    BlockContainer blockContainer = mineBlock();
                    // the best block is parent block of the tip
                    if (null != blockContainer) {
                        StateDB track = this.stateDB.startTracking(this.chainID);

                        if (tryToConnect(blockContainer, track)) {
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
                                setBestBlockContainer(blockContainer);
                                this.peerManager.addNewBlockPeer(blockContainer.getBlock().getMinerPubkey());
                            } catch (Exception e) {
                                logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
                            }

                            Set<ByteArrayWrapper> accounts = extractAccountFromBlockContainer(blockContainer);
                            this.txPool.recheckAccoutTx(accounts);

                            publishBestBlock();
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

    /**
     * sync a block for more state when needed
     */
    private void syncBlockForMoreState() {
        if (null != this.syncBlock && this.syncBlock.getBlockNum() >0) {
            BlockContainer blockContainer = getBlockContainerFromDHTByHash(this.syncBlock.getPreviousBlockHash());
            if (null != blockContainer) {
                try {
                    StateDB track = this.stateDB.startTracking(this.chainID);
                    if (this.stateProcessor.backwardProcess(blockContainer, track)) {
                        // after sync
                        // 1. save block
                        // 2. save sync block hash
                        // 3. commit new state
                        // 4. set sync block
                        // 5. add old block peer to peer pool

                        this.blockStore.saveBlockContainer(this.chainID, blockContainer, true);
                        track.setSyncBlockHash(this.chainID, blockContainer.getBlock().getBlockHash());
                        track.commit();
                        this.syncBlock = blockContainer.getBlock();
                        this.peerManager.addOldBlockPeer(blockContainer.getBlock().getMinerPubkey());
                    }
                } catch (Exception e) {
                    logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
                }
            }
        }
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
                    syncBlockForMoreState();
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

            setBestBlockContainer(targetBlockContainer);

            publishBestBlock();

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

    /**
     * first sync when follow a chain
     * @param bestVote best vote
     * @return true[success]/false[fail]
     */
    private boolean initialSync(Vote bestVote) {
        if (null == bestVote) {
            logger.error("Chain ID[{}]: Best vote is null.", new String(this.chainID));
            return false;
        }

        try {
            this.blockStore.removeChain(this.chainID);
            this.stateDB.clearAllState(this.chainID);

            BlockContainer bestVoteBlockContainer = getBlockContainerFromDHTByHash(bestVote.getBlockHash());

            if (null == bestVoteBlockContainer) {
                logger.error("Chain ID[{}]: Best vote block is null.", new String(this.chainID));
                return false;
            }

            // after sync
            // 1. save block
            // 2. save best and sync block hash
            // 3. commit new state
            // 4. set best and sync block
            // 5. add old block peer to peer pool

            // initial sync from best vote
            StateDB track = this.stateDB.startTracking(this.chainID);
            if (!this.stateProcessor.backwardProcess(bestVoteBlockContainer, track)) {
                logger.error("Chain ID[{}]: Process block[{}] fail!",
                        new String(this.chainID), Hex.toHexString(bestVoteBlockContainer.getBlock().getBlockHash()));
                return false;
            }

            this.blockStore.saveBlockContainer(this.chainID, bestVoteBlockContainer, true);

            track.setBestBlockHash(this.chainID, bestVoteBlockContainer.getBlock().getBlockHash());
            track.setSyncBlockHash(this.chainID, bestVoteBlockContainer.getBlock().getBlockHash());

            this.bestBlockContainer = bestVoteBlockContainer;
            this.syncBlock = bestVoteBlockContainer.getBlock();

            this.peerManager.addOldBlockPeer(bestVoteBlockContainer.getBlock().getMinerPubkey());

            track.commit();
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
                    initialSync(bestVote);
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

    /**
     * get tip block from peer
     * @param peer peer pubKey
     * @return tip block or null
     */
    private Block getTipBlockFromPeer(byte[] peer) {
        try {
            logger.debug("+ctx----------------peer: {}", Hex.toHexString(peer));
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(new ByteArrayWrapper(this.chainID)), TIMEOUT);
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
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalts.get(new ByteArrayWrapper(this.chainID)), TIMEOUT);

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
    private void publishBestBlock() {
        if (null != this.bestBlockContainer) {
            if (null != this.bestBlockContainer.getTx()) {
                // put immutable tx
                DHT.ImmutableItem immutableItem =
                        new DHT.ImmutableItem(this.bestBlockContainer.getTx().getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }

            // put immutable block
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(this.bestBlockContainer.getBlock().getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            byte[] peer = this.peerManager.getMutableRangePeerRandomly();
            MutableItemValue mutableItemValue = new MutableItemValue(this.bestBlockContainer.getBlock().getBlockHash(), peer);

            // put mutable item
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    mutableItemValue.getEncoded(), this.blockSalts.get(new ByteArrayWrapper(this.chainID)));
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * get a block from dht
     * @param hash block hash
     * @return block get from dht, null otherwise
     */
    private Block getBlockFromDHTByHash(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);

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

            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalts.get(new ByteArrayWrapper(this.chainID)), TIMEOUT);
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
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);

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
    private boolean tryToConnect(final BlockContainer blockContainer, StateDB stateDB) {
        // if main chain
        logger.debug("best hash:{}, previous hash:{}",
                Hex.toHexString(this.bestBlockContainer.getBlock().getBlockHash()),
                Hex.toHexString(blockContainer.getBlock().getPreviousBlockHash()));
        if (Arrays.equals(this.bestBlockContainer.getBlock().getBlockHash(),
                blockContainer.getBlock().getPreviousBlockHash())) {
            // main chain
            if (!isValidBlockContainer(blockContainer, stateDB)) {
                return false;
            }

            ImportResult result = this.stateProcessor.forwardProcess(blockContainer, stateDB);
            return result.isSuccessful();
        } else {
            logger.info("Chain ID[{}]: previous hash mis-match", new String(this.chainID));
            return false;
//            // if has parent
//            try {
//                Block parent = this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash());
//                if (null == parent) {
//                    logger.error("ChainID[{}]: Cannot find parent!", new String(this.chainID));
//                    return false;
//                }
//            } catch (Exception e) {
//                logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
//                return false;
//            }
        }
//        return true;
    }

    /**
     * set best block of this chain
     * @param blockContainer best block container
     */
    public void setBestBlockContainer(BlockContainer blockContainer) {
        this.bestBlockContainer = blockContainer;
    }

    /**
     * get best block of this chain
     * @return best block container
     */
    public BlockContainer getBestBlockContainer() {
        return this.bestBlockContainer;
    }

    /**
     * set synced block of this chain
     * @param block synced block
     */
    public void setSyncBlock(Block block) {
        this.syncBlock = block;
    }

    /**
     * get synced block of this chain
     * @return sync block
     */
    public Block getSyncBlock() {
        return this.syncBlock;
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
    private boolean minable() {
        try {
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            if (null == pubKey) {
                logger.info("Chain ID[{}]: PubKey is null.", new String(this.chainID));
                return false;
            }

            BigInteger power = this.stateDB.getNonce(this.chainID, pubKey);
            if (null == power || power.longValue() <= 0) {
                logger.info("Chain ID[{}]: PubKey[{}]-No mining power.",
                        new String(this.chainID), Hex.toHexString(pubKey));
                return false;
            }

            logger.info("ChainID[{}]: PubKey[{}] mining power: {}",
                    new String(this.chainID), Hex.toHexString(pubKey), power);

            // check base target
            BigInteger baseTarget = this.pot.calculateRequiredBaseTarget(this.chainID,
                    this.bestBlockContainer.getBlock(), this.blockStore);

            // check generation signature
            byte[] genSig = this.pot.calculateGenerationSignature(this.bestBlockContainer.
                    getBlock().getGenerationSignature(), pubKey);

            // check if target >= hit
//            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
//                    System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp());

            BigInteger hit = this.pot.calculateRandomHit(genSig);

            long timeInterval = this.pot.calculateMiningTimeInterval(hit, baseTarget, power);
            if ((System.currentTimeMillis() / 1000 - this.bestBlockContainer.getBlock().getTimeStamp()) < timeInterval) {
                logger.info("Chain ID[{}]: It's not time for the block.", new String(this.chainID));
                return false;
            }

//            if (target.compareTo(hit) < 0) {
//                logger.info("ChainID[{}]: Target[{}] value is smaller than hit[{}]!!",
//                        new String(this.chainID), target, hit);
//                return false;
//            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * mine a block
     * @return block container, or null
     */
    private BlockContainer mineBlock() {

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(this.chainID,
                this.bestBlockContainer.getBlock(), this.blockStore);
        byte[] generationSignature = pot.calculateGenerationSignature(
                this.bestBlockContainer.getBlock().getGenerationSignature(),
                AccountManager.getInstance().getKeyPair().first);
        BigInteger cumulativeDifficulty = pot.calculateCumulativeDifficulty(
                this.bestBlockContainer.getBlock().getCumulativeDifficulty(),
                baseTarget);

        byte[] immutableBlockHash;
        try {
            // if current block number is larger than mutable range
            if (this.bestBlockContainer.getBlock().getBlockNum() + 1 >= ChainParam.MUTABLE_RANGE) {
                immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(this.chainID,
                        this.bestBlockContainer.getBlock().getBlockNum() + 1 - ChainParam.MUTABLE_RANGE);
            } else {
                immutableBlockHash = this.blockStore.getMainChainBlockHashByNumber(this.chainID, 0);
//                immutableBlockHash = new byte[ChainParam.HashLength];
            }
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
            return null;
        }

        // if block is too less, sync more
        while (null == immutableBlockHash && !isSyncComplete()) {
            logger.info("ChainID[{}]-Sync for more state!", new String(this.chainID));
            syncBlockForMoreState();
        }

        if (null == immutableBlockHash) {
            logger.error("ChainID[{}]-Get immutable block hash error!", new String(this.chainID));
            return null;
        }

        Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();

        Transaction tx = txPool.getBestTransaction();

        Block block;
        logger.debug("-------------Previous hash:{}", Hex.toHexString(this.bestBlockContainer.getBlock().getBlockHash()));
        if (null != tx) {
            block = new Block((byte) 1, System.currentTimeMillis() / 1000,
                    this.bestBlockContainer.getBlock().getBlockNum() + 1,
                    this.bestBlockContainer.getBlock().getBlockHash(), immutableBlockHash,
                    baseTarget, cumulativeDifficulty, generationSignature, tx.getTxID(),
                    0, 0, 0, 0, keyPair.first);
        } else {
            block = new Block((byte) 1, System.currentTimeMillis() / 1000,
                    this.bestBlockContainer.getBlock().getBlockNum() + 1,
                    this.bestBlockContainer.getBlock().getBlockHash(), immutableBlockHash,
                    baseTarget, cumulativeDifficulty, generationSignature, null,
                    0, 0, 0, 0, keyPair.first);
        }

        BlockContainer blockContainer = new BlockContainer(block, tx);

        // set state
        StateDB miningTrack = this.stateDB.startTracking(this.chainID);
        this.stateProcessor.forwardProcess(blockContainer, miningTrack);

        try {
            // set state
            AccountState minerState = miningTrack.getAccount(this.chainID,
                    AccountManager.getInstance().getKeyPair().first);
            block.setMinerBalance(minerState.getBalance().longValue());

            if (null != tx) {
                AccountState senderState = miningTrack.getAccount(this.chainID,
                        tx.getSenderPubkey());
                block.setSenderBalance(senderState.getBalance().longValue());
                block.setSenderNonce(senderState.getNonce().longValue());

                if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {

                    AccountState receiverState = miningTrack.getAccount(this.chainID,
                            ((WiringCoinsTx)tx).getReceiver());

                    block.setReceiverBalance(receiverState.getBalance().longValue());
                }
            }

            // sign
            block.signBlock(keyPair.second);
        } catch (Exception e) {
            logger.error(new String(this.chainID) + ":" + e.getMessage(), e);
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
            case BLOCK_MUTABLE_ITEM_VALUE: {
                break;
            }
            case TX_MUTABLE_ITEM_VALUE: {
                break;
            }
            case MSG_MUTABLE_ITEM_VALUE: {
                break;
            }
            case BLOCK: {

            }
            case TX: {
                break;
            }
            default: {

            }
        }
    }

}
