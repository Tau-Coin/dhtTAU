package io.taucoin.chain;

import io.taucoin.account.AccountManager;
import io.taucoin.config.ChainConfig;
import io.taucoin.core.ProofOfTransaction;
import io.taucoin.core.StateProcessor;
import io.taucoin.core.TransactionPool;
import io.taucoin.core.VotingPool;
import io.taucoin.db.BlockStore;
import io.taucoin.db.Repository;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Chain represents one blockchain for tau multi-chain system.
 * It manages blockchain core actvity, etc. voting process.
 */
public class Chain {

    private static final Logger logger = LoggerFactory.getLogger("Chain");

    // Chain id specified by the transaction of creating new blockchain.
    private byte[] chainID;

    // Chain nick name specified by the transaction of creating new blockchain.
    private String nickName;

    private ChainConfig chainConfig;

    // Voting thread.
    private Thread votingThread;

    private TauListener tauListener;

    private ProofOfTransaction pot;

    private TransactionPool txPool;

    private VotingPool votingPool;

    private BlockStore blockStore;

    private Repository repository;

    private Repository track;

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
    }

    /**
     * init chain
     */
    private void init() {
        this.track = this.repository.startTracking(this.chainID);

        // get best block
        try {
            byte[] bestBlockHash = this.track.getBestBlockHash(this.chainID);
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
            Set<byte[]> pubKeys = this.track.getPeers(this.chainID);
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
        while (true) {
            if (minable()) {
                Block block = mineBlock();
                tryToConnect(block);
            } else {

            }
        }
    }

    private void vote() {}

    private Block GetBlockrandomlyFormDB() {
        return null;
    }

    private boolean tryToConnect(final Block block) {
        // if main chain
        if (Arrays.equals(bestBlock.getBlockHash(), block.getPreviousBlockHash())) {
            // main chain
        } else {
            // if has parent
            try {
                Block parent = this.blockStore.getBlockByHash(this.chainID, block.getPreviousBlockHash());
                if (null == parent) {
                    logger.error("ChainID[{}]: Cannot find parent!", this.chainID.toString());
                    return false;
                }
                // TODO:: simply check
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
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
     * @return
     */
    private boolean isValidBlock(Block block) {
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
        if (!verifyPOT(block)) {
            logger.error("ChainID[{}]: Validate block param error!", this.chainID.toString());
            return false;
        }

        return true;
    }

    /**
     * check pot consensus
     * @param block
     * @return
     */
    private boolean verifyPOT(Block block) {
        try {
            byte[] pubKey = block.getMinerPubkey();

            BigInteger power = this.track.getNonce(this.chainID, pubKey);
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

    private boolean processBlock(Block block, Repository repository) {
        return true;
    }

    /**
     * check if be able to mine now
     * @return
     */
    private boolean minable() {
        try {
            byte[] pubKey = AccountManager.getInstance().getKeyPair().first;

            BigInteger power = this.track.getNonce(this.chainID, pubKey);
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
        Repository miningTrack = this.repository.startTracking(this.chainID);
        this.stateProcessor.process(block, miningTrack);
        // TODO:: wait for setting interface

        return null;
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
