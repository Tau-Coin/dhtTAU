package io.taucoin.controller;

import io.taucoin.account.AccountManager;
import io.taucoin.chain.ChainManager;
import io.taucoin.db.KeyValueDataBase;
import io.taucoin.listener.CompositeTauListener;
import io.taucoin.listener.TauListener;
import io.taucoin.rpc.RpcServer;
import io.taucoin.torrent.SessionSettings;
import io.taucoin.torrent.TorrentEngine;
import io.taucoin.util.Repo;

import com.frostwire.jlibtorrent.Pair;
import com.frostwire.jlibtorrent.SessionManager;
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

    // Communicate with torrent dht by TorrentEngine;
    TorrentEngine torrentEngine = TorrentEngine.getInstance();

    private ChainManager chainManager;

    // Enable rpc or not.
    private boolean enableRpc;
    // Rpc server
    private RpcServer rpcServer;

    /**
     * TauController constructor.
     *
     * @param repoPath the directory where data is stored.
     * @param key the pair of public key and private key.
     * @param stateDb state key-value database implementation.
     * @param blockDb block key-value database implementation.
     * @param enableRpc enable rpc server or not.
     */
    public TauController(String repoPath, Pair<byte[], byte[]> key,
            KeyValueDataBase stateDb, KeyValueDataBase blockDb,
	    boolean enableRpc) {

        // set the root directory.
        Repo.setRepoPath(repoPath);
	// store public key and private key.
	this.accountManager.updateKey(key);

	// Add TauListener into TorrentEngine.
	this.torrentEngine.setTauListener(compositeTauListener);

	// create chain manager.
	// ChainManager is responsibling for opening database and
	// loading the prebuilt blockchain data.
        this.chainManager = new ChainManager(compositeTauListener, stateDb, blockDb);

        this.enableRpc = enableRpc;
        if (enableRpc) {
            rpcServer = new RpcServer(this);
	}
    }

    /**
     * Start all the blockchain core components.
     *
     * @param settings SessionSettings
     */
    public void start(SessionSettings settings) {
        // First of all, start torrent engine.
	torrentEngine.start(settings);
    }

    /**
     * Stop all the blockchain core components.
     */
    public void stop() {

        // Lastly, stop torrent engine
	torrentEngine.stop();
    }

    /**
     * Register TauListener.
     *
     * @param listener TauListener implementation.
     */
    public void registerListener(TauListener listener) {
        this.compositeTauListener.addListener(listener);
    }

    /**
     * Unregister TauListener.
     *
     * @param listener TauListener implementation.
     */
    public void unregisterListener(TauListener listener) {
        this.compositeTauListener.removeListener(listener);
    }

    /**
     * Update key pair for the miner.
     *
     * @param key pair of public key and private key.
     */
    public void updateKey(Pair<byte[], byte[]> key) {
        this.accountManager.updateKey(key);
    }

    /**
     * Get ChainManager reference.
     *
     * @return ChainManager
     */
    public ChainManager getChainManager() {
        return this.chainManager;
    }

    /**
     * Follow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean followChain(byte[] chainID) {
        return chainManager.followChain(chainID);
    }

    /**
     * Unfollow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean unfollowChain(byte[] chainID) {
        return chainManager.unfollowChain(chainID);
    }

    /**
     * Get SessionManager from torrect session context.
     *
     * @return SessionManager
     */
    public SessionManager getSessionManager() {
        return torrentEngine.getSessionManager();
    }
}
