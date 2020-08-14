package io.taucoin.jtau.event;

import io.taucoin.controller.TauController;
import io.taucoin.listener.TauListener;
import io.taucoin.torrent.SessionStats;
import io.taucoin.utypes.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.jtau.cmd.ExitCode.*;

public class TauListenerImpl implements TauListener {

    private static final Logger logger = LoggerFactory.getLogger("TauListenerImpl");

    // TauController
    private TauController tauController;

    /**
     * TauListenerImpl constructor
     *
     * @param controller TauController
     */
    public TauListenerImpl(TauController controller) {
        this.tauController = controller;
    }

    @Override
    public void onNewChain(String chainId, String nickName) {}

    @Override
    public void onTauStarted(boolean success, String errMsg) {
        if (success) {
            logger.info("Starting Tau successfully");
        } else {
            logger.error("Starting Tau error: " + errMsg);
            System.exit(START_TAU_ERROR);
        }
    }

    @Override
    public void onTauStopped() {
        // Ignore this event
    }

    @Override
    public void onTauError(String errMsg) {
        logger.error("Tau runtime error:" + errMsg);
        System.exit(TAU_RUNTIME_ERROR);
    }

    @Override
    public void onDHTStarted(boolean success, String errMsg) {
        // Ignore this event
    }

    @Override
    public void onChainManagerStarted(boolean success, String errMsg) {
        // Ignore this event
    }

    @Override
    public void onDHTStopped() {
        // Ignore this event
    }

    @Override
    public void onChainManagerStopped() {
        // Ignore this event
    }

    @Override
    public void onSessionStats(SessionStats newStats) {
        logger.debug("session stats:" + newStats);
    }

    @Override
    public void onNewBlock(Block block) {}

    @Override
    public void onRollBack(Block block) {}

    @Override
    public void onSyncBlock(Block block) {}

}
