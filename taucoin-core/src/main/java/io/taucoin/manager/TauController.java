package io.taucoin.manager;

import io.taucoin.account.AccountManager;
import io.taucoin.chain.ChainManager;
import io.taucoin.listener.CompositeTauListener;
import io.taucoin.listener.TauListener;
import io.taucoin.util.Repo;

import com.frostwire.jlibtorrent.Pair;
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

    // AccountManager manages the key pair for the miner.
    private AccountManager accountManager = AccountManager.getInstance();

    private ChainManager chainManager;

    /**
     * TauController constructor.
     *
     * @param repoPath the directory where data is stored.
     */
    public TauController(String repoPath, Pair<byte[], byte[]> key) {

        Repo.setRepoPath(repoPath);
	accountManager.updateKey(key);
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

    /**
     * Update key pair for the miner.
     *
     * @param key pair of public key and private key.
     */
    public void updateKey(Pair<byte[], byte[]> key) {
        accountManager.updateKey(key);
    }

    /**
     * Get ChainManager reference.
     *
     * @return ChainManager
     */
    public ChainManager getChainManager() {
        return chainManager;
    }
}
