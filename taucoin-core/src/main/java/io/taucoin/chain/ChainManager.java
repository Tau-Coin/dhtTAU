package io.taucoin.chain;

import io.taucoin.config.ChainConfig;
import io.taucoin.db.BlockDB;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.db.StateDBImpl;
import io.taucoin.listener.TauListener;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.Repo;
import io.taucoin.types.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.*;

/**
 * ChainManager manages all blockchains for tau multi-chain system.
 * It's responsible for creating chain, following chain, unfollow chain,
 * pause chain, resume chain and so on.
 */
public class ChainManager {

    private static final Logger logger = LoggerFactory.getLogger("ChainManager");

    // Chain maps: chainID -> Chain.
    private Map<ByteArrayWrapper, Chain> chains
            = Collections.synchronizedMap(new HashMap<>());

    private TauListener listener;

    // state database
    private StateDBImpl repositoryImpl;

    // state path
    private static final String STATE_PATH = "state";

    // block database
    private BlockDB blockDB;

    // block store path
    private static final String BLOCK_PATH = "block";

    /**
     * ChainManager constructor.
     *
     * @param listener CompositeTauListener
     */
    public ChainManager(TauListener listener, KeyValueDataBaseFactory dbFactory) {
        this.listener = listener;

        // create state and block database.
        // If database does not exist, directly load.
        // If not exist, create log
        this.repositoryImpl = new StateDBImpl(dbFactory.newDatabase());
        this.blockDB = new BlockDB(dbFactory.newDatabase());
    }

    public void openChainDB() throws Exception {
        try {
            this.repositoryImpl.open(Repo.getRepoPath() + "/" + STATE_PATH);
            this.blockDB.open(Repo.getRepoPath() + "/" + BLOCK_PATH);
        } catch (Exception e) {
            throw e;
        }
    }

    public void closeChainDB() {
        this.repositoryImpl.close();
        this.blockDB.close();
    }

    /**
     * Start all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void start() {

		// Open the db for repo and block
        try {
            openChainDB();

            Set<byte[]> followedChains = new HashSet<byte[]>();
            followedChains= this.repositoryImpl.getAllFollowedChains();

            Iterator<byte[]> chainsItor = followedChains.iterator();
            while (chainsItor.hasNext()) {

                // New single chain
                byte[] chainid = chainsItor.next();
                ByteArrayWrapper wrapperChainID = new ByteArrayWrapper(chainid);

                Chain chain = new Chain(chainid, this.blockDB, this.repositoryImpl, this.listener);

                // Add chain
                this.chains.put(wrapperChainID, chain);
                // start chain process
                chain.start();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // TODO: notify starting blockchains exception.
        }
    }

    /**
     * Stop all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void stop() {
		// stop chains
        Iterator<ByteArrayWrapper> chainsItor = this.chains.keySet().iterator();
        while(chainsItor.hasNext()) {
			chains.get(chainsItor.next()).stop();	
		}

		// close db
		closeChainDB();
    }

    /**
     * Follow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean followChain(byte[] chainID) {
        return false;
    }

    /**
     * Unfollow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean unfollowChain(byte[] chainID) {
        return false;
    }

    /**
     * get chain by chain ID
     * @param chainID chain ID
     * @return chain or null if not found
     */
    public Chain getChain(byte[] chainID) {
        return chains.get(new ByteArrayWrapper(chainID));
    }

    /**
     * create new community.
     * @param cf
     * @return chainid.
     */
    public byte[] createNewCommunity(ChainConfig cf){
        Block genesis = new Block(cf);
        boolean ret = followChain(genesis.getChainID());
        try{
            blockDB.saveBlock(genesis,true);
        }catch (Exception e){
            ret = false;
        }
        return ret == true ? genesis.getChainID(): null;
    }

    /**
     * according to chainid select a chain to sendTransaction.
     * @param tx
     * @return
     * todo
     */
    public byte[] sendTransaction(Transaction tx){
        return null;
    }

    /**
     * make all followed chain start mining.
     * @return
     * todo
     */
    public byte[][] startMining(){
        return null;
    }
}
