package io.taucoin.chain;

import io.taucoin.db.BlockDB;
import io.taucoin.db.KeyValueDataBase;
import io.taucoin.db.StateDB;
import io.taucoin.listener.CompositeTauListener;
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

    // Chain maps.
    private Map<String, Chain> chains
            = Collections.synchronizedMap(new HashMap<String, Chain>());

    private TauListener listener;

    // state database
    private StateDB stateDB;

    // block database
    private BlockDB blockDB;

    /**
     * ChainManager constructor.
     *
     * @param listener CompositeTauListener
     */
    public ChainManager(TauListener listener, KeyValueDataBase stateDb,
            KeyValueDataBase blockDb) {
        this.listener = listener;

        // create state and block database.
        // If database does not exist, directly load.
        // If not exist, create new database.
        this.stateDB = new StateDB(stateDb);
	this.blockDB = new BlockDB(blockDb);
    }
}
