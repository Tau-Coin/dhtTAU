package io.taucoin.chain;

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

    /**
     * ChainManager constructor.
     *
     * @param listener CompositeTauListener
     */
    public ChainManager(TauListener listener) {
        this.listener = listener;
    }
}
