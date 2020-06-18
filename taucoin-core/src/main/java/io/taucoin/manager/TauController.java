package io.taucoin.manager;

import io.taucoin.chain.ChainManager;
import io.taucoin.listener.CompositeTauListener;
import io.taucoin.listener.TauListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TauController is the core controller of tau blockchain.
 * It manages all the components including ChainManager, RpcServer,
 * TorrentEngine and so on.
 */
public class TauController {

    private static final Logger logger = LoggerFactory.getLogger(TauController.class);

    // Tau listeners registery.
    private CompositeTauListener compositeTauListener
            = new CompositeTauListener();

    private ChainManager chainManager;

    /**
     * TauController constructor.
     *
     * @param repoPath the directory where data is stored.
     */
    public TauController(String repoPath) {
    }

    /**
     * Register TauListener.
     *
     * @param listener TauListener implementation.
     */
    public void registerListener(TauListener listener) {
        compositeTauListener.addListener(listener);
    }

    /**
     * Unregister TauListener.
     *
     * @param listener TauListener implementation.
     */
    public void unregisterListener(TauListener listener) {
        compositeTauListener.removeListener(listener);
    }
}
