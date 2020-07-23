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
}
