package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;
import io.taucoin.account.AccountManager;
import io.taucoin.core.*;
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
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

/**
 * Chain represents one blockchain for tau multi-chain system.
 * It manages blockchain core actvity, etc. voting process.
 */
public class Chain {

    private static final Logger logger = LoggerFactory.getLogger("Chain");

    private static final int TIMEOUT = 10;

    // Chain id specified by the transaction of creating new blockchain.
    private byte[] chainID;

    // mutable item salt: block
    private byte[] blockSalt;

    // mutable item salt: tx
    private byte[] txSalt;

    // Chain nick name specified by the transaction of creating new blockchain.
    private String nickName;

    // Voting thread.
    private Thread votingThread;

    // collect thread
    private Thread txThread;

    // publish thread
    private Timer timer;

    private TauListener tauListener;

    // consensus: pot
    private ProofOfTransaction pot;

    // tx pool
    private TransactionPool txPool;

    // voting pool
    private VotingPool votingPool;

    // peer manager
    private PeerManager peerManager;

    // block db
    private BlockStore blockStore;

    // state db
    private StateDB stateDB;

    // state processor: process and roll back block
    private StateProcessor stateProcessor;

    // the best block of current chain
    private Block bestBlock;

    // the synced block of current chain
    private Block syncBlock;

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     * @param blockStore block store
     * @param stateDB state db
     */
    public Chain(byte[] chainID, BlockStore blockStore, StateDB stateDB, TauListener tauListener) {
        this.chainID = chainID;
        this.blockStore = blockStore;
        this.stateDB = stateDB;
        this.tauListener = tauListener;
    }

    /**
     * make block salt
     * @return
     */
    private byte[] makeBlockSalt() {
        byte[] salt = new byte[this.chainID.length + ChainParam.BLOCK_CHANNEL.length];
        System.arraycopy(this.chainID, 0, salt, 0, this.chainID.length);
        System.arraycopy(ChainParam.BLOCK_CHANNEL, 0, salt, this.chainID.length,
                ChainParam.BLOCK_CHANNEL.length);
        return salt;
    }

    /**
     * make tx salt
     * @return
     */
    private byte[] makeTxSalt() {
        byte[] salt = new byte[this.chainID.length + ChainParam.TX_CHANNEL.length];
        System.arraycopy(this.chainID, 0, salt, 0, this.chainID.length);
        System.arraycopy(ChainParam.TX_CHANNEL, 0, salt, this.chainID.length,
                ChainParam.TX_CHANNEL.length);
        return salt;
    }

    /**
     * init chain
     */
    private boolean init() {
        // init salt
        this.blockSalt = makeBlockSalt();
        this.txSalt = makeTxSalt();

        // init voting pool
        this.votingPool = new VotingPool();

        // init pot consensus
        this.pot = new ProofOfTransaction();

        // init state processor
        this.stateProcessor = new StateProcessorImpl(this.chainID);

        // init best block and sync block
        try {
            byte[] bestBlockHash = this.stateDB.getBestBlockHash(this.chainID);
            if (null != bestBlockHash) {
                this.bestBlock = this.blockStore.getBlockByHash(this.chainID, bestBlockHash);
            }

            byte[] syncBlockHash = this.stateDB.getSyncBlockHash(this.chainID);
            if (null != syncBlockHash) {
                this.syncBlock = this.blockStore.getBlockByHash(this.chainID, syncBlockHash);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        // init peer manager
        this.peerManager = new PeerManager(this.chainID);
        // get peers form db
        try {
            Set<byte[]> peers = this.stateDB.getPeers(this.chainID);
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
            if (null != bestBlock) {
                // get priority peers in mutable range
                priorityPeers.add(new ByteArrayWrapper(bestBlock.getMinerPubkey()));
                byte[] previousHash = bestBlock.getPreviousBlockHash();
                for (int i = 0; i < ChainParam.MUTABLE_RANGE; i++) {
                    Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                    if (null != block) {
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

            this.peerManager.init(allPeers, priorityPeers);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        // vote for new chain, when there is nothing in local
        if (null == this.bestBlock || null == this.syncBlock) {
            Vote bestVote = vote();
            if (!initialSync(bestVote)) {
                logger.error("Initial sync fail!");
                return false;
            }
        }

        // if offline too long, vote as a new chain
        if (null != this.bestBlock &&
                (System.currentTimeMillis() / 1000 - this.bestBlock.getTimeStamp()) >
                        ChainParam.WARNING_RANGE * ChainParam.DefaultBlockTimeInterval) {
            Vote bestVote = vote();
            if (!initialSync(bestVote)) {
                logger.error("Initial sync fail!");
                return false;
            }
        }

        // init tx pool
        this.txPool = new TransactionPoolImpl(this.chainID,
                AccountManager.getInstance().getKeyPair().first, this.stateDB);
        this.txPool.init();

        return true;
    }

    /**
     * block chain main process
     */
    private void blockChainProcess() {
        chainLoop();
    }

    /**
     * main chain loop
     */
    private void chainLoop() {
        while (!Thread.interrupted()) {
            boolean votingFlag = false;
            boolean miningFlag = false;

            // keep looking for more difficult chain
            while (!Thread.interrupted()) {
                byte[] pubKey = peerManager.popUpOptimalBlockPeer();
                Block tip = getTipBlockFromPeer(pubKey);

                if (null == tip) {
                    continue;
                }

                // give up the less difficult chain
                if (tip.getCumulativeDifficulty().compareTo(this.bestBlock.getCumulativeDifficulty()) < 0) {
                    txPool.addRemote(tip.getTxMsg());
                    continue;
                }

                // if a more difficult chain
                // download block
                try {
                    if (tip.getBlockNum() > ChainParam.MUTABLE_RANGE) {
                        byte[] immutableBlockHash = tip.getImmutableBlockHash();
                        BlockInfo blockInfo = this.blockStore.getBlockInfoByHash(this.chainID, immutableBlockHash);
                        // immutable block cannot found in block store, vote
                        if (null == blockInfo) {
                            votingFlag = true;
                            break;
                        } else {
                            // found in block store
                            int counter = 0;
                            byte[] previousHash = tip.getPreviousBlockHash();
                            this.blockStore.saveBlock(tip, false);
                            while (!Thread.interrupted() && counter < ChainParam.WARNING_RANGE) {
                                Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                                if (null != block) {
                                    // found in local
                                    break;
                                }
                                // get from dht
                                Block dhtBlock = getBlockFromDHTByHash(previousHash);
                                if (null != dhtBlock) {
                                    previousHash = dhtBlock.getPreviousBlockHash();
                                    this.blockStore.saveBlock(dhtBlock, false);
                                }

                                if (dhtBlock.getBlockNum() <= 0) {
                                    break;
                                }
                                counter++;
                            }
                        }
                    }

                    // find fork point
                    Block forkPointBlock = this.blockStore.getForkPointBlock(tip);
                    if (null == forkPointBlock) {
                        // cannot find fork point
                        continue;
                    }

                    // calc fork range
                    long forkRange = this.bestBlock.getBlockNum() - forkPointBlock.getBlockNum();

                    if (forkRange > ChainParam.WARNING_RANGE) {
                        // an attack chain, ignore it
                        continue;
                    } else if (forkRange < ChainParam.MUTABLE_RANGE){
                        // change to more difficult chain
                        reBranch(tip, stateDB);
                        miningFlag = true;
                        break;
                    } else {
                        // vote when fork point between mutable range and warning range
                        votingFlag = true;
                        break;
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            // vote for new chain
            if (votingFlag) {
                Vote bestVote = vote();
                syncFromVote(bestVote);
                continue;
            }

            // mine
            if (miningFlag) {
                StateDB track = this.stateDB.startTracking(this.chainID);
                if (minable(track)) {
                    Block block = mineBlock();
                    // the best block is parent block of the tip
                    if (tryToConnect(block, track)) {
                        try {
                            track.commit();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        setBestBlock(block);

                        publishBestBlock();
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
     * @param stateDB
     */
    private void syncBlockForMoreState(StateDB stateDB) {
        if (null != this.syncBlock) {
            Block block = getBlockFromDHTByHash(this.syncBlock.getPreviousBlockHash());
            if (null != block) {
                try {
                    StateDB track = stateDB.startTracking(this.chainID);
                    if (this.stateProcessor.backwardProcess(block, track)) {
                        track.commit();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * re-branch chain
     * @param block
     * @param stateDB
     */
    private void reBranch(Block block, StateDB stateDB) {
        try {
            //try to roll back and reconnect
            StateDB track = stateDB.startTracking(this.chainID);
            List<Block> undoBlocks = new ArrayList<>();
            List<Block> newBlocks = new ArrayList<>();
            blockStore.getForkBlocksInfo(block, this.bestBlock, undoBlocks, newBlocks);
            for (Block undoBlock : undoBlocks) {
                this.stateProcessor.rollback(undoBlock, track);
            }
            int size = newBlocks.size();
            for (int i = size - 1; i >= 0; i--) {
                ImportResult result = this.stateProcessor.forwardProcess(newBlocks.get(i), track);
                if (result == ImportResult.NO_ACCOUNT_INFO && !isSyncComplete()) {
                    syncBlockForMoreState(stateDB);
                    i++;
                }
            }

            this.blockStore.reBranchBlocks(undoBlocks, newBlocks);

            track.commit();

            setBestBlock(block);

            publishBestBlock();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * vote for best chain
     * @return
     */
    private Vote vote() {
        // try to use all peers to vote
        int counter = peerManager.getPeerNumber();

        counter = Math.max(counter, (int)Math.log(counter));

        while (!Thread.interrupted() && counter > 0) {
            byte[] peer = peerManager.popUpOptimalBlockPeer();
            Block block = getTipBlockFromPeer(peer);
            if (null != block) {
                // vote on immutable point
                votingPool.putIntoVotingPool(block.getImmutableBlockHash(),
                        (int) block.getBlockNum() - ChainParam.MUTABLE_RANGE);
            }

            counter--;
        }

        return votingPool.getBestVote();
    }

    /**
     * first sync when follow a chain
     * @param bestVote best vote
     * @return true[success]/false[fail]
     */
    private boolean initialSync(Vote bestVote) {

        try {
            this.blockStore.removeChain(this.chainID);
            this.stateDB.clearAllState(this.chainID);

            Block bestVoteBlock = getBlockFromDHTByHash(bestVote.getBlockHash());

            // initial sync from best vote
            StateDB track = this.stateDB.startTracking(this.chainID);
            if (this.stateProcessor.backwardProcess(bestVoteBlock, track)) {
                this.bestBlock = bestVoteBlock;
                this.syncBlock = bestVoteBlock;
            } else {
                return false;
            }

            Block block = bestVoteBlock;
            int counter = 0;
            while (!Thread.interrupted() && block.getBlockNum() > 0 && counter < ChainParam.MUTABLE_RANGE) {
                block = getBlockFromDHTByHash(this.syncBlock.getPreviousBlockHash());
                if (this.stateProcessor.backwardProcess(block, track)) {
                    this.blockStore.saveBlock(block, true);
                    this.syncBlock = block;
                } else {
                    return false;
                }
                counter++;
            }

//        if (null != bestVoteBlock) {
//            try {
//                Block forkPointBlock = this.blockStore.getForkPointBlock(bestVoteBlock);
//                // if cannot find fork point block, clear state and block database, restart
//                if (null != forkPointBlock) {
//                    reBranch(bestVoteBlock, stateDB);
//                } else {
//                    // if cannot find fork point block, clear state and block database,
//                    // then sync block with mutable range
//                    this.blockStore.removeChain(this.chainID);
//                }
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//            }
//        }

            track.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * sync block from given vote
     * @param bestVote
     * @return
     */
    private boolean syncFromVote(Vote bestVote) {
        try {
            // download block first
            int counter = 0;
            byte[] previousHash = bestVote.getBlockHash();
            while (!Thread.interrupted() && counter < ChainParam.WARNING_RANGE) {
                Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                if (null != block) {
                    // found in local
                    break;
                }
                // get from dht
                Block dhtBlock = getBlockFromDHTByHash(previousHash);
                if (null != dhtBlock) {
                    previousHash = dhtBlock.getPreviousBlockHash();
                    this.blockStore.saveBlock(dhtBlock, false);
                }

                if (dhtBlock.getBlockNum() <= 0) {
                    break;
                }
                counter++;
            }

            Block bestVoteBlock = this.blockStore.getBlockByHash(this.chainID, bestVote.getBlockHash());
            reBranch(bestVoteBlock, this.stateDB);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
//        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey, this.blockSalt, TIMEOUT);
//        byte[] blockHash = TorrentDHTEngine.getInstance().dhtGet(spec);
//        if (null != blockHash) {
//            return getBlockFromDHTByHash(blockHash);
//        }
//        return null;

        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.blockSalt, TIMEOUT);
        byte[] rlp = TorrentDHTEngine.getInstance().dhtGet(spec);
        if (null != rlp) {
            MutableItemValue value = new MutableItemValue(rlp);
            if (null != value.getPeer()) {
                this.peerManager.addBlockPeer(value.getPeer());
            }
            if (null != value.getHash()) {
                return getBlockFromDHTByHash(value.getHash());
            }
        }

        return null;
    }

    /**
     * publish tip block on main chain to dht
     */
    private void publishBestBlock() {
        if (null != this.bestBlock) {
            // put immutable block first
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(this.bestBlock.getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            MutableItemValue mutableItemValue = new MutableItemValue(this.bestBlock.getBlockHash(), null);

            // put mutable item
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    mutableItemValue.getEncoded(), blockSalt);
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * get a block from dht
     * @param hash
     * @return
     */
    private Block getBlockFromDHTByHash(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);

        // when you get a block, you need to put a block simultaneously
//        Block block = getBlockRandomlyFromDB();
//
//        if (null == block) {
//            block = this.bestBlock;
//        }
//
//        DHT.ExchangeImmutableItemResult result;
//        if (null != block) {
//            DHT.ImmutableItem blockItem = new DHT.ImmutableItem(block.getEncoded());
//            result = TorrentDHTEngine.getInstance().dhtTauGet(spec, blockItem);
//        } else {
//            result = TorrentDHTEngine.getInstance().dhtTauGet(spec, null);
//        }

        byte[] result = TorrentDHTEngine.getInstance().dhtGet(spec);

        if (null != result) {
            return new Block(result);
        }

        return null;
    }

    /**
     * get a tx from dht on tx channel
     * @param peer peer to get
     * @return transaction
     */
    private Transaction getTxFromPeer(byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalt, TIMEOUT);
        byte[] data = TorrentDHTEngine.getInstance().dhtGet(spec);
        if (null != data) {
            MutableItemValue value = new MutableItemValue(data);
            if (null != value.getPeer()) {
                this.peerManager.addTxPeer(value.getPeer());
            }
            if (null != value.getHash()) {
                return getTxFromDHTByHash(value.getHash());
            }
        }

        return null;
    }

    /**
     * get a tx by hash from dht
     * @param hash
     * @return
     */
    private Transaction getTxFromDHTByHash(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);

        // when you get a tx, you need to put a tx simultaneously
//        DHT.GetImmutableItemSpec immutableItemSpec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);
//        Transaction tx = this.txPool.getBestTransaction();
//        DHT.ExchangeImmutableItemResult result;
//        if (null != tx) {
//            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
//            result = TorrentDHTEngine.getInstance().dhtTauGet(immutableItemSpec, immutableItem);
//        } else {
//            result = TorrentDHTEngine.getInstance().dhtTauGet(immutableItemSpec, null);
//        }

        byte[] result = TorrentDHTEngine.getInstance().dhtGet(spec);

        if (null != result) {
            return new Transaction(result);
        }

        return null;
    }

    /**
     * put a tx in mutable item
     * @param tx
     */
    private void publishTransaction(Transaction tx) {
        if (null != tx) {
            // put immutable tx first
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            // put mutable item
            byte[] peer = this.txPool.getOptimalPeer();
            MutableItemValue value = new MutableItemValue(tx.getTxID(), peer);
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second, value.getEncoded(), txSalt);
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * get block randomly from block store
     * @return
     */
    private Block getBlockRandomlyFromDB() {
        int currentNumber = (int) this.bestBlock.getBlockNum();
        Random random = new Random(System.currentTimeMillis());
        try {
            Block block = this.blockStore.getMainChainBlockByNumber(this.chainID, random.nextInt(currentNumber));
            return block;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * connect a block
     * @param block
     * @param stateDB
     * @return
     */
    private boolean tryToConnect(final Block block, StateDB stateDB) {
        // if main chain
        if (Arrays.equals(bestBlock.getBlockHash(), block.getPreviousBlockHash())) {
            // main chain
            if (!isValidBlock(block, stateDB)) {
                return false;
            }

            ImportResult result = this.stateProcessor.forwardProcess(block, stateDB);
            if (!result.isSuccessful()) {
                return false;
            }

            return true;
        } else {
            return false;
//            // if has parent
//            try {
//                Block parent = this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash());
//                if (null == parent) {
//                    logger.error("ChainID[{}]: Cannot find parent!", this.chainID.toString());
//                    return false;
//                }
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//                return false;
//            }
        }
//        return true;
    }

    /**
     * set best block of this chain
     * @param block best block
     */
    public void setBestBlock(Block block) {
        this.bestBlock = block;
    }

    /**
     * get best block of this chain
     * @return
     */
    public Block getBestBlock() {
        return this.bestBlock;
    }

    /**
     * check if a block valid
     * @param block
     * @param stateDB
     * @return
     */
    private boolean isValidBlock(Block block, StateDB stateDB) {
        // 是否本链
        if (!Arrays.equals(this.chainID, block.getChainID())) {
            logger.error("ChainID[{}]: ChainID mismatch!", this.chainID.toString());
            return false;
        }

        // 时间戳检查
        if (block.getTimeStamp() > System.currentTimeMillis() / 1000) {
            logger.error("ChainID[{}]: Time is in the future!", this.chainID.toString());
            return false;
        }

        // 区块内部自检
        if (!block.isBlockParamValidate()) {
            logger.error("ChainID[{}]: Validate block param error!", this.chainID.toString());
            return false;
        }

        // 区块签名检查
        if (!block.verifyBlockSig()) {
            logger.error("ChainID[{}]: Bad Signature!", this.chainID.toString());
            return false;
        }

        // 是否孤块
        try {
            if (null == this.blockStore.getBlockByHash(block.getChainID(), block.getPreviousBlockHash())) {
                logger.error("ChainID[{}]: Cannot find parent!", this.chainID.toString());
                return false;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        // POT共识验证
        if (!verifyPOT(block, stateDB)) {
            logger.error("ChainID[{}]: Validate block param error!", this.chainID.toString());
            return false;
        }

        return true;
    }

    /**
     * check pot consensus
     * @param block
     * @param stateDB
     * @return
     */
    private boolean verifyPOT(Block block, StateDB stateDB) {
        try {
            byte[] pubKey = block.getMinerPubkey();

            BigInteger power = stateDB.getNonce(this.chainID, pubKey);
            logger.info("Address: {}, mining power: {}", Hex.toHexString(pubKey), power);

            Block parentBlock = this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash());
            if (null == parentBlock) {
                logger.error("ChainID[{}]: Cannot find parent!", this.chainID.toString());
                return false;
            }

            // check base target
            BigInteger baseTarget = this.pot.calculateRequiredBaseTarget(parentBlock, this.blockStore);
            if (0 != baseTarget.compareTo(block.getBaseTarget())) {
                logger.error("ChainID[{}]: Block base target error!", this.chainID.toString());
                return false;
            }

            // check generation signature
            byte[] genSig = this.pot.calculateGenerationSignature(parentBlock.getGenerationSignature(), pubKey);
            if (!Arrays.equals(genSig, block.getGenerationSignature())) {
                logger.error("ChainID[{}]: Block base target error!", this.chainID.toString());
                return false;
            }

            // check cumulative difficulty
            BigInteger culDifficulty = this.pot.calculateCumulativeDifficulty(
                    parentBlock.getCumulativeDifficulty(), baseTarget);
            if (0 != culDifficulty.compareTo(block.getCumulativeDifficulty())) {
                logger.error("ChainID[{}]: Cumulative difficulty error!", this.chainID.toString());
                return false;
            }

            // check if target >= hit
            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
                    block.getTimeStamp() - parentBlock.getTimeStamp());
            BigInteger hit = this.pot.calculateRandomHit(genSig);
            if (target.compareTo(hit) < 0) {
                logger.error("ChainID[{}]: Target[{}] value is smaller than hit[{}]!!",
                        this.chainID.toString(), target, hit);
                return false;
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * check if be able to mine now
     * @param stateDB
     * @return
     */
    private boolean minable(StateDB stateDB) {
        try {
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            BigInteger power = stateDB.getNonce(this.chainID, pubKey);
            if (null == power || power.longValue() <= 0) {
                logger.error("No mining power.");
                return false;
            }

            logger.info("ChainID[{}]: My mining power: {}", this.chainID.toString(), power);

            // check base target
            BigInteger baseTarget = this.pot.calculateRequiredBaseTarget(this.bestBlock, this.blockStore);

            // check generation signature
            byte[] genSig = this.pot.calculateGenerationSignature(this.bestBlock.getGenerationSignature(), pubKey);

            // check if target >= hit
            BigInteger target = this.pot.calculateMinerTargetValue(baseTarget, power,
                    System.currentTimeMillis() / 1000 - this.bestBlock.getTimeStamp());

            BigInteger hit = this.pot.calculateRandomHit(genSig);
            if (target.compareTo(hit) < 0) {
                logger.error("ChainID[{}]: Target[{}] value is smaller than hit[{}]!!",
                        this.chainID.toString(), target, hit);
                return false;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * mine a block
     * @return
     */
    private Block mineBlock() {
        Transaction tx = txPool.getBestTransaction();

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(this.bestBlock, this.blockStore);
        byte[] generationSignature = pot.calculateGenerationSignature(this.bestBlock.getGenerationSignature(),
                AccountManager.getInstance().getKeyPair().first);
        BigInteger cumulativeDifficulty = pot.calculateCumulativeDifficulty(this.bestBlock.getCumulativeDifficulty(),
                baseTarget);

        byte[] immutableBlockHash = null;
        try {
            if (this.bestBlock.getBlockNum() + 1 >= ChainParam.MUTABLE_RANGE) {
                immutableBlockHash = blockStore.getMainChainBlockHashByNumber(this.chainID,
                        this.bestBlock.getBlockNum() + 1 - ChainParam.MUTABLE_RANGE);
            } else {
                Block genesisBlock = this.blockStore.getMainChainBlockByNumber(this.chainID, 0);
                if (null != genesisBlock) {
                    immutableBlockHash = genesisBlock.getBlockHash();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        if (null == immutableBlockHash) {
            logger.error("ChainID[{}]-Get immutable block hash error!", this.chainID.toString());
            return null;
        }

        Block block = new Block((byte)1, this.chainID, 0, this.bestBlock.getBlockNum() + 1, this.bestBlock.getBlockHash(),
                immutableBlockHash, baseTarget, cumulativeDifficulty, generationSignature, tx, 0, 0, 0, 0,
                AccountManager.getInstance().getKeyPair().first);
        // get state
        StateDB miningTrack = this.stateDB.startTracking(this.chainID);
        this.stateProcessor.forwardProcess(block, miningTrack);

        try {
            // set state
            AccountState minerState = miningTrack.getAccount(this.chainID,
                    AccountManager.getInstance().getKeyPair().first);
            block.setMinerBalance(minerState.getBalance().longValue());

            AccountState senderState = miningTrack.getAccount(this.chainID,
                    block.getTxMsg().getSenderPubkey());
            block.setSenderBalance(senderState.getBalance().longValue());
            block.setSenderNonce(senderState.getNonce().longValue());

            if (MsgType.Wiring == block.getTxMsg().getTxData().getMsgType()) {
                AccountState receiverState = miningTrack.getAccount(this.chainID,
                        block.getTxMsg().getTxData().getReceiver());
                block.setSenderBalance(receiverState.getBalance().longValue());
                block.setSenderNonce(receiverState.getNonce().longValue());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        return block;
    }

    /**
     * 1. get tx from peer
     * 2. publish self tx
     */
    private void txProcess() {
        Transaction lastTx = null;
        while (!Thread.interrupted()) {
            // get tx
            byte[] peer = this.peerManager.popUpOptimalTxPeer();
            Transaction tx = getTxFromPeer(peer);
            if (null != tx) {
                this.txPool.addRemote(tx);
            }

            // publish myself tx
            Transaction myselfTx = this.txPool.getLocalBestTransaction();
            if (null != myselfTx && myselfTx != lastTx) {
                publishTransaction(myselfTx);
                lastTx = myselfTx;
            }
        }
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

        logger.info("Start voting and tx thread...");
        votingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                blockChainProcess();
            }
        }, "blockChain");
        votingThread.start();

        txThread = new Thread(new Runnable() {
            @Override
            public void run() {
                txProcess();
            }
        });
        txThread.start();

        timer = new Timer();
        TimerTask timerTask = new PublishTask();
        timer.schedule(timerTask, 30, ChainParam.DefaultBlockTimeInterval);

        return true;
    }

    /**
     * Stop all activities of this chain.
     */
    public void stop() {
        if (null != votingThread) {
            logger.info("Stop voting thread.");
            votingThread.interrupt();
        }

        if (null != txThread) {
            logger.info("Stop tx thread.");
            txThread.interrupt();
        }

        if (null != timer) {
            logger.info("Stop publish thread.");
            timer.cancel();
        }
    }

    /**
     * get transaction pool
     * @return
     */
    public TransactionPool getTransactionPool() {
        return this.txPool;
    }

    /**
     * get state database
     * @return
     */
    public StateDB getStateDB() {
        return this.stateDB;
    }

    /**
     * get block store
     * @return
     */
    public BlockStore getBlockStore() {
        return this.blockStore;
    }

    /**
     * publish block/tx/message and so on
     */
    public class PublishTask extends TimerTask {
        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            // publish block randomly
            Block block = getBlockRandomlyFromDB();
            if (null != block) {
                DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(block.getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }

            // publish tx
            Transaction tx = txPool.getLocalBestTransaction();
            if (null != tx) {
                DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }
            List<Transaction> list = txPool.getLocals();
            if (list.size() > 1) {
                Random random = new Random(System.currentTimeMillis());
                int i = random.nextInt(list.size());
                DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(list.get(i).getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }
        }
    }

}

