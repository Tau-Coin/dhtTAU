package io.taucoin.chain;

import io.taucoin.account.AccountManager;
import io.taucoin.config.ChainConfig;
import io.taucoin.core.*;
import io.taucoin.db.BlockInfo;
import io.taucoin.db.BlockStore;
import io.taucoin.db.StateDB;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.processor.StateProcessor;
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

    private TauListener tauListener;

    // consensus: pot
    private ProofOfTransaction pot;

    // tx pool
    private TransactionPool txPool;

    // voting pool
    private VotingPool votingPool;

    // block db
    private BlockStore blockStore;

    // state db
    private StateDB stateDB;

    // state processor: process and roll back block
    private StateProcessor stateProcessor;

    private byte[] genesisBlockHash;

    private Block bestBlock;

    private Map<ByteArrayWrapper, Long> peers;

    /**
     * Chain constructor.
     *
     * @param chainID chain identity.
     */
    public Chain(byte[] chainID, TauListener tauListener) {
        this.chainID = chainID;
        this.tauListener = tauListener;
        this.blockSalt = makeBlockSalt();
        this.txSalt = makeTxSalt();
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
    private void init() {
        // get best block
        try {
            byte[] bestBlockHash = this.stateDB.getBestBlockHash(this.chainID);
            this.bestBlock = this.blockStore.getBlockByHash(this.chainID, bestBlockHash);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("ChainID[{}]: Exception, load genesis.", this.chainID.toString());
            loadGenesisBlock();
        }

        if (null == this.bestBlock) {
            logger.error("ChainID[{}]: Best block is empty, load genesis.", this.chainID.toString());
            loadGenesisBlock();
        }

        // get peers form db
        try {
            Set<byte[]> pubKeys = this.stateDB.getPeers(this.chainID);
            if (null != pubKeys && !pubKeys.isEmpty()) {
                for (byte[] pubKey: pubKeys) {
                    this.peers.put(new ByteArrayWrapper(pubKey), (long) 0);
                }
            } else {
                this.peers.put(new ByteArrayWrapper(this.chainConfig.getGenesisMinerPubkey()), (long) 0);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * load genesis block when chain is empty
     */
    private void loadGenesisBlock() {

    }

    private void processChain() {
        init();
        loop();
    }

    private void loop() {
        while (!Thread.interrupted()) {
            boolean votingFlag = false;
            boolean miningFlag = false;

            // keep looking for more difficult chain
            while (!Thread.interrupted()) {
                byte[] pubKey = getOptimalPeer();
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
                        setBestBlock(block);
                        try {
                            track.commit();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
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
            this.stateProcessor.process(newBlocks.get(i), track);
        }
        try {
            this.blockStore.reBranchBlocks(undoBlocks, newBlocks);

            setBestBlock(block);

            track.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void vote() {
        while (!Thread.interrupted()) {
            byte[] peer = getOptimalPeer();
            DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(peer, blockSalt, TIMEOUT);
            byte[] data = TorrentDHTEngine.getInstance().dhtGet(spec);
            if (null != data) {
                Block block = new Block(data);
                // vote on immutable point
                votingPool.putIntoVotingPool(block.getImmutableBlockHash(),
                        (int) block.getBlockNum() - ChainParam.MUTABLE_RANGE);
            }
        }
        Vote bestVote = votingPool.getBestVote();
        // sync from best vote
        // TODO
    }

    private byte[] getOptimalPeer() {
        return null;
    }

    /**
     * get tip block from peer
     * @param pubKey
     * @return
     */
    private Block getTipBlockFromPeer(byte[] pubKey) {
        DHT.GetMutableItemSpec spec = new DHT.GetMutableItemSpec(pubKey, this.blockSalt, TIMEOUT);
        byte[] data = TorrentDHTEngine.getInstance().dhtGet(spec);
        if (null != data) {
            return new Block(data);
        }
        return null;
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
        DHT.ImmutableItem item = new DHT.ImmutableItem(block.getEncoded());

        DHT.ExchangeImmutableItemResult result = TorrentDHTEngine.getInstance().dhtTauGet(spec, item);

        if (null != result.getData()) {
            new Block(result.getData());
        }

        return null;
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
        this.stateProcessor.process(block, miningTrack);

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
     * Start activities of this chain, mainly including votint and mining.
     *
     * @return boolean successful or not.
     */
    public boolean start() {
        return false;
    }

    /**
     * Stop all activities of this chain.
     */
    public void stop() {
    }
}
