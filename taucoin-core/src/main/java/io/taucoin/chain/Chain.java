package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;
import com.frostwire.jlibtorrent.Sha1Hash;
import io.taucoin.account.AccountManager;
import io.taucoin.config.ChainConfig;
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

    // mutable item salt suffix: block
    private static final String BLOCK_CHANNEL = "#block";

    // mutable item salt suffix: tx
    private static final String TX_CHANNEL = "#tx";

    // Chain id specified by the transaction of creating new blockchain.
    private byte[] chainID;

    // mutable item salt: block
    private byte[] blockSalt;

    // mutable item salt: tx
    private byte[] txSalt;

    // Chain nick name specified by the transaction of creating new blockchain.
    private String nickName;

    private ChainConfig chainConfig;

    // Voting thread.
    private Thread votingThread;

    private Thread txThread;

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

    private byte[] genesisBlockHash;

    // the best block of current chain
    private Block bestBlock;

    // the synced block of current chain
    private Block syncBlock;

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     */
    public Chain(byte[] chainID, TauListener tauListener) {
        this.chainID = chainID;
        this.tauListener = tauListener;
    }

    /**
     * make block salt
     * @return
     */
    private byte[] makeBlockSalt() {
        byte[] salt = new byte[this.chainID.length + BLOCK_CHANNEL.getBytes().length];
        System.arraycopy(this.chainID, 0, salt, 0, this.chainID.length);
        System.arraycopy(BLOCK_CHANNEL.getBytes(), 0, salt, this.chainID.length,
                BLOCK_CHANNEL.getBytes().length);
        return salt;
    }

    /**
     * make tx salt
     * @return
     */
    private byte[] makeTxSalt() {
        byte[] salt = new byte[this.chainID.length + TX_CHANNEL.getBytes().length];
        System.arraycopy(this.chainID, 0, salt, 0, this.chainID.length);
        System.arraycopy(TX_CHANNEL.getBytes(), 0, salt, this.chainID.length,
                TX_CHANNEL.getBytes().length);
        return salt;
    }

    /**
     * init chain
     */
    private boolean init() {
        // init salt
        this.blockSalt = makeBlockSalt();
        this.txSalt = makeTxSalt();

        // init tx pool
        this.txPool = new TransactionPoolImpl(this.chainID,
                AccountManager.getInstance().getKeyPair().first, this.stateDB);
        this.txPool.init();

        // init voting pool
        this.votingPool = new VotingPool();

        // init pot consensus
        this.pot = new ProofOfTransaction();

        // init state processor
        this.stateProcessor = new StateProcessorImpl(this.chainID);

        // init best block
        try {
            byte[] bestBlockHash = this.stateDB.getBestBlockHash(this.chainID);
            if (null != bestBlockHash) {
                this.bestBlock = this.blockStore.getBlockByHash(this.chainID, bestBlockHash);
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

        return true;
    }

    /**
     * block chain main process
     */
    private void blockChainProcess() {
        init();
        loop();
    }

    private void loop() {
        while (!Thread.interrupted()) {
            boolean votingFlag = false;
            boolean miningFlag = false;

            // keep looking for more difficult chain
            while (!Thread.interrupted()) {
                byte[] pubKey = peerManager.popUpOptimalBlockPeer();
                Block tip = getTipBlockFromPeer(pubKey);

                // give up the less difficult chain
                if (null == tip || tip.getCumulativeDifficulty().
                        compareTo(this.bestBlock.getCumulativeDifficulty()) < 0) {
                    txPool.addRemote(tip.getTxMsg());
                    continue;
                }

                // if a more difficult chain
                // download block
                try {
                    if (tip.getBlockNum() > ChainParam.MUTABLE_RANGE) {
                        byte[] immutableBlockHash = tip.getImmutableBlockHash();
                        BlockInfo blockInfo = this.blockStore.getBlockInfoByHash(this.chainID, immutableBlockHash);
                        // cannot found in block store
                        // TODO
                        if (null == blockInfo) {
                            votingFlag = true;
                            break;
                        } else {
                            // found in block store
                            int counter = 0;
                            byte[] previousHash = tip.getPreviousBlockHash();
                            this.blockStore.saveBlock(tip, false);
                            while (!Thread.interrupted() && counter < ChainParam.MUTABLE_RANGE) {
                                Block block = this.blockStore.getBlockByHash(this.chainID, previousHash);
                                if (null != block) {
                                    // found in local
                                    break;
                                }
                                // get from dht
                                Block dhtBlock = getBlockFromDHT(previousHash);
                                if (null != dhtBlock) {
                                    previousHash = dhtBlock.getPreviousBlockHash();
                                    this.blockStore.saveBlock(dhtBlock, false);
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
                        // vote
                        votingFlag = true;
                        break;
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            // vote
            if (votingFlag) {
                vote();
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
     * re-branch chain
     * @param block
     * @param stateDB
     */
    private void reBranch(Block block, StateDB stateDB) {
        //try to roll back and reconnect
        StateDB track = stateDB.startTracking(this.chainID);
        List<Block> undoBlocks = new ArrayList<>();
        List<Block> newBlocks = new ArrayList<>();
        blockStore.getForkBlocksInfo(block, undoBlocks, newBlocks);
        for (Block undoBlock : undoBlocks) {
            this.stateProcessor.rollback(undoBlock, track);
        }
        int size = newBlocks.size();
        for (int i = size - 1; i >= 0; i--) {
            this.stateProcessor.forwardProcess(newBlocks.get(i), track);
        }
        try {
            this.blockStore.reBranchBlocks(undoBlocks, newBlocks);

            track.commit();

            setBestBlock(block);

            publishBestBlock();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Vote vote() {
        // try to use all peers to vote
        int counter = peerManager.getPeerNumber();
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
    private boolean initSync(Vote bestVote) {
        try {
            Block bestVoteBlock = getBlockFromDHT(bestVote.getBlockHash());

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
                block = getBlockFromDHT(this.syncBlock.getPreviousBlockHash());
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
     * get tip block from peer
     * @param peer peer pubKey
     * @return tip block or null
     */
    private Block getTipBlockFromPeer(byte[] peer) {
//        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey, this.blockSalt, TIMEOUT);
//        byte[] blockHash = TorrentDHTEngine.getInstance().dhtGet(spec);
//        if (null != blockHash) {
//            return getBlockFromDHT(blockHash);
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
                return getBlockFromDHT(value.getHash());
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
    private Block getBlockFromDHT(byte[] hash) {
        DHT.GetImmutableItemSpec spec = new DHT.GetImmutableItemSpec(hash, TIMEOUT);

        // when you get a block, you need to put a block simultaneously
        Block block = getBlockRandomlyFromDB();

        if (null == block) {
            block = this.bestBlock;
        }

        DHT.ExchangeImmutableItemResult result;
        if (null != block) {
            DHT.ImmutableItem blockItem = new DHT.ImmutableItem(block.getEncoded());
            result = TorrentDHTEngine.getInstance().dhtTauGet(spec, blockItem);
        } else {
            result = TorrentDHTEngine.getInstance().dhtTauGet(spec, null);
        }

        if (null != result.getData()) {
            return new Block(result.getData());
        }

        return null;
    }

    /**
     * get a tx from dht on tx channel
     * @param peer peer to get
     * @return transaction
     */
    private Transaction getTxFromDHT(byte[] peer) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, this.txSalt, TIMEOUT);
        byte[] rlp = TorrentDHTEngine.getInstance().dhtGet(spec);
        if (null != rlp) {
            MutableItemValue value = new MutableItemValue(rlp);
            if (null != value.getPeer()) {
                this.peerManager.addTxPeer(value.getPeer());
            }
            if (null != value.getHash()) {
                DHT.GetImmutableItemSpec immutableItemSpec = new DHT.GetImmutableItemSpec(value.getHash(), TIMEOUT);
                Transaction tx = this.txPool.getBestTransaction();
                DHT.ExchangeImmutableItemResult result;
                if (null != tx) {
                    DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(tx.getEncoded());
                    result = TorrentDHTEngine.getInstance().dhtTauGet(immutableItemSpec, immutableItem);
                } else {
                    result = TorrentDHTEngine.getInstance().dhtTauGet(immutableItemSpec, null);
                }
                if (null != result) {
                    return new Transaction(result.getData());
                }
            }
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

            if (!processBlock(block, stateDB)) {
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

    private boolean processBlock(Block block, StateDB stateDB) {
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

        byte[] immutableBlockHash;
        if (this.bestBlock.getBlockNum() + 1 >= ChainParam.MUTABLE_RANGE) {
            try {
                immutableBlockHash = blockStore.getMainChainBlockHashByNumber(this.chainID,
                        this.bestBlock.getBlockNum() + 1 - ChainParam.MUTABLE_RANGE);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
            if (null == immutableBlockHash) {
                logger.error("ChainID[{}]-Get immutable block hash error!", this.chainID.toString());
                return null;
            }
        } else {
            immutableBlockHash = genesisBlockHash;
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

    private void txProcess() {
        Transaction lastTx = null;
        while (!Thread.interrupted()) {
            // get tx
            byte[] peer = this.peerManager.popUpOptimalTxPeer();
            Transaction tx = getTxFromDHT(peer);
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
    }
}

