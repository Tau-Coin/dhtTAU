package io.taucoin.chain;

import io.taucoin.account.AccountManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

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

        try {
            byte[] bestBlockHash = this.track.getBestBlockHash(this.chainID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            loadGenesisBlock();
        }

    }

    /**
     * load genesis block when chain is empty
     */
    private void loadGenesisBlock() {

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

    private boolean processBlock(Block block, Repository repository) {
        return true;
    }

    private Block mineBlock() {
        Transaction tx = txPool.getBestTransaction();

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(this.bestBlock, this.blockStore);
        byte[] generationSignature = pot.calculateGenerationSignature(this.bestBlock.getGenerationSignature(),
                AccountManager.getInstance().getPublickey());
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
                AccountManager.getInstance().getPublickey());
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
