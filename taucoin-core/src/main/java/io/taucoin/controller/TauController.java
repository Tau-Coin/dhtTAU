package io.taucoin.controller;

import io.taucoin.communication.CommunicationManager;
import io.taucoin.account.AccountManager;
import io.taucoin.chain.ChainManager;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.listener.CompositeMsgListener;
import io.taucoin.listener.CompositeTauListener;
import io.taucoin.listener.MsgListener;
import io.taucoin.listener.TauListener;
import io.taucoin.dht.DHTEngine;
import io.taucoin.dht.session.SessionController;
import io.taucoin.util.Repo;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * TauController is the core controller of tau blockchain.
 * It manages all the components including ChainManager, RpcServer,
 * TorrentDHTEngine and so on.
 */
public class TauController {

    private static final Logger logger = LoggerFactory.getLogger(TauController.class);

    // Tau listeners registery.
    private CompositeTauListener compositeTauListener
            = new CompositeTauListener();

    private CompositeMsgListener compositeMsgListener
            = new CompositeMsgListener();

    // AccountManager manages the key pair for the miner.
    private AccountManager accountManager = AccountManager.getInstance();

    // Communicate with torrent dht by DHTEngine;
    DHTEngine dhtEngine = DHTEngine.getInstance();

    private ChainManager chainManager;
    private CommunicationManager communicationManager;

    /**
     * TauController constructor.
     *
     * @param repoPath the directory where data is stored.
     * @param dbFactory key-value database factory implementation.
     * @param deviceID device id
     */
    public TauController(String repoPath, KeyValueDataBaseFactory dbFactory,
            byte[] deviceID) {

        // set the root directory.
        Repo.setRepoPath(repoPath);

        // Add TauListener into DHTEngine.
        this.dhtEngine.setTauListener(compositeTauListener);

        // create chain manager.
        // ChainManager is responsibling for opening database and
        // loading the prebuilt blockchain data.
        this.chainManager = new ChainManager(compositeTauListener, dbFactory);

        this.communicationManager = new CommunicationManager(deviceID,
                compositeMsgListener, dbFactory);
    }

    /**
     * TauController constructor.
     *
     * @param repoPath the directory where data is stored.
     * @param keySeed seed to generate the pair of public key and private key.
     * @param dbFactory key-value database factory implementation.
     */
    public TauController(String repoPath, byte[] keySeed,
            KeyValueDataBaseFactory dbFactory, byte[] deviceID) {

        this(repoPath, dbFactory, deviceID);

        // store public key and private key.
        this.accountManager.updateKey(keySeed);
    }

    /**
     * Start all the blockchain core components.
     *
     * @param sessionsQuota sessions quoto
     */
    public void start(int sessionsQuota) {
        start(sessionsQuota, SessionController.DEFUALT_INTERFACES);
    }

    /**
     * Start all the blockchain core components.
     *
     * @param sessionsQuota sessions quoto
     * @param interfacesQuota interfaces quota
     */
    public void start(int sessionsQuota, int interfacesQuota) {

        registerListener(new StartListener(this, compositeTauListener));

        // First of all, start torrent engine.
        dhtEngine.start(sessionsQuota, interfacesQuota);

        // And then start chain manager.
        // chain manager will start followed and mined blockchains.
        chainManager.start();

        communicationManager.start();
    }

    /**
     * Stop all the blockchain core components.
     */
    public void stop() {

        registerListener(new StopListener(this, compositeTauListener));

        // Stop all blockchains.
        chainManager.stop();

        communicationManager.stop();

        // Lastly, stop torrent engine
        dhtEngine.stop();
    }

    /**
     * Restart dht sessions.
     *
     * @param sessionsQuota sessions quota
     */
    public boolean restartSessions(int sessionsQuota) {
        return dhtEngine.restart(sessionsQuota, SessionController.DEFUALT_INTERFACES);
    }

    /**
     * Restart dht sessions.
     *
     * @param sessionsQuota sessions quota
     * @param interfacesQuota interfaces quota
     */
    public boolean restartSessions(int sessionsQuota, int interfacesQuota) {
        return dhtEngine.restart(sessionsQuota, interfacesQuota);
    }

    /**
     * Increase dht session.
     *
     * @return boolean true indicates starting successfully, or else false.
     */
    public boolean increaseSession() {
        return dhtEngine.increaseSession();
    }

    /**
     * Decrease dht session.
     *
     * @return boolean true indicates decreasing successfully, or else false.
     */
    public boolean decreaseSession() {
        return dhtEngine.decreaseSession();
    }

    /**
     * Get dht engine.
     *
     * @return DHTEngine
     */
    public DHTEngine getDHTEngine() {
        return dhtEngine;
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
     * Register MsgListenter.
     *
     * @param listener MsgListenter implementation.
     */
    public void registerMsgListener(MsgListener listener) {
        this.compositeMsgListener.addListener(listener);
    }

    /**
     * Unregister MsgListenter.
     *
     * @param listener MsgListenter implementation.
     */
    public void unregisterMsgListener(MsgListener listener) {
        this.compositeMsgListener.removeListener(listener);
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
     * Update key pair for the miner.
     *
     * @param seed the seed of key pair
     */
    public void updateKey(byte[] seed) {
        this.accountManager.updateKey(seed);
        // TODO:: use listener
//        this.chainManager.updateKey(this.accountManager.getKeyPair().first);
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
     * Get CommunicationManager reference.
     *
     * @return CommunicationManager
     */
    public CommunicationManager getCommunicationManager() {
        return this.communicationManager;
    }

    /**
     * Follow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean followChain(byte[] chainID, List<byte[]> peerList) {
        return chainManager.followChain(chainID, peerList);
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
     * start mining
     * @param chainID chain ID
     */
    public void startMining(byte[] chainID) {
        this.chainManager.startMining(chainID);
    }

    /**
     * stop mining, just sync
     * @param chainID chain ID
     */
    public void stopMining(byte[] chainID) {
        this.chainManager.stopMining(chainID);
    }

    public static class StartListener extends StartstopListener {

        private TauController tauController;
        private TauListener compositeTauListener;

        private boolean dhtStartedReceived;
        private boolean dhtStartedResult;
        private String dhtStartedErrMsg;

        private boolean chainMgrStartedReceived;
        private boolean chainMgrStartedResult;
        private String chainMgrStartedErrMsg;

        public StartListener(TauController tauController,
                TauListener compositeTauListener) {

            this.tauController = tauController;
            this.compositeTauListener = compositeTauListener;

            this.dhtStartedReceived = false;
            this.dhtStartedResult = false;
            this.dhtStartedErrMsg = null;

            this.chainMgrStartedReceived = false;
            this.chainMgrStartedResult = false;
            this.chainMgrStartedErrMsg = null;
        }

        @Override
        public void onDHTStarted(boolean success, String errMsg) {
            dhtStartedReceived = true;
            dhtStartedResult = success;
            dhtStartedErrMsg = errMsg;

            tryToNotifyStatrtedResult();
        }

        @Override
        public void onChainManagerStarted(boolean success, String errMsg) {
            chainMgrStartedReceived = true;
            chainMgrStartedResult = success;
            chainMgrStartedErrMsg = errMsg;

            tryToNotifyStatrtedResult();
        }

        @Override
        public void onDHTStopped() {}

        @Override
        public void onChainManagerStopped() {}

        private void tryToNotifyStatrtedResult() {
            if (!dhtStartedReceived || !chainMgrStartedReceived) {
                return;
            }

            // Note: unregister this listener to prevent memory leaks.
            tauController.unregisterListener(this);

            boolean success = dhtStartedResult && chainMgrStartedResult;
            String errMsg = "";

            if (!dhtStartedResult) {
                errMsg += dhtStartedErrMsg;
            }
            if (!chainMgrStartedResult) {
                errMsg += chainMgrStartedErrMsg;
            }

            logger.info("notify start TauController result: OK:" + success
                   + ", err:" + errMsg);
            compositeTauListener.onTauStarted(success, errMsg);
        }
    }

    public static class StopListener extends StartstopListener {

        private TauController tauController;
        private TauListener compositeTauListener;

        private boolean dhtStoppedReceived;

        private boolean chainMgrStoppedReceived;

        public StopListener(TauController tauController,
                TauListener compositeTauListener) {

            this.tauController = tauController;
            this.compositeTauListener = compositeTauListener;

            this.dhtStoppedReceived = false;

            this.chainMgrStoppedReceived = false;
        }

        @Override
        public void onDHTStarted(boolean success, String errMsg) {}

        @Override
        public void onChainManagerStarted(boolean success, String errMsg) {}

        @Override
        public void onDHTStopped() {
            dhtStoppedReceived = true;

            tryToNotifyStoppedResult();
        }

        @Override
        public void onChainManagerStopped() {
            chainMgrStoppedReceived = true;

            tryToNotifyStoppedResult();
        }

        private void tryToNotifyStoppedResult() {
            if (!dhtStoppedReceived || !chainMgrStoppedReceived) {
                return;
            }

            // Note: unregister this listener to prevent memory leaks.
            tauController.unregisterListener(this);

            logger.info("notify TauController stopped");
            compositeTauListener.onTauStopped();
        }
    }
}
