package io.taucoin.chain;

import io.taucoin.db.BlockDB;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.db.RepositoryImpl;
import io.taucoin.listener.TauListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ChainManager manages all blockchains for tau multi-chain system.
 * It's responsible for creating chain, following chain, unfollow chain,
 * pause chain, resume chain and so on.
 */
public class ChainManager {

    // Chain maps: chainID -> Chain.
    private Map<byte[], Chain> chains
            = Collections.synchronizedMap(new HashMap<byte[], Chain>());

    private TauListener listener;

    // state database
    private RepositoryImpl repositoryImpl;

    // block database
    private BlockDB blockDB;

    /**
     * ChainManager constructor.
     *
     * @param listener CompositeTauListener
     */
    public ChainManager(TauListener listener, KeyValueDataBaseFactory dbFactory) {
        this.listener = listener;

        // create state and block database.
        // If database does not exist, directly load.
        // If not exist, create new database.
        this.repositoryImpl = new RepositoryImpl(dbFactory.newDatabase());
        this.blockDB = new BlockDB(dbFactory.newDatabase());
    }

    /**
     * Start all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void start() {
    }

    /**
     * Stop all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void stop() {
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
}
