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

    private AccountManager accountManager;

    private ProofOfTransaction pot;

    private TransactionPool txPool;

    private VotingPool votingPool;

    private BlockStore blockStore;

    private Repository repository;

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

    private void init() {

    }

    private void processBlock(Block block) {

    }

    private Block mineBlock() {
        Transaction tx = txPool.getBestTransaction();

        BigInteger baseTarget = pot.calculateRequiredBaseTarget(this.bestBlock, this.blockStore);
        byte[] generationSignature = pot.calculateGenerationSignature(this.bestBlock.getGenerationSignature(),
                accountManager.getPublickey());
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
