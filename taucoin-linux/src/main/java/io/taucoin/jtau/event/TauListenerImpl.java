package io.taucoin.jtau.event;

import io.taucoin.controller.TauController;
import io.taucoin.listener.TauListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            // TODO: exit application
        }
    }

    @Override
    public void onTauStopped() {
        // Ignore this event
    }

    @Override
    public void onTauError(String errMsg) {
        logger.error("Tau runtime error:" + errMsg);
        // TODO: exit application
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

}
