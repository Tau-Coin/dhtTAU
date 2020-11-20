package io.taucoin.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.db.BlockStore;
import io.taucoin.db.StateDB;
import io.taucoin.dht.DHT;
import io.taucoin.listener.TauListener;

public class Communication implements DHT.GetDHTItemCallback {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    private final TauListener tauListener;

    // block db
    private final BlockStore blockStore;

    // state db
    private final StateDB stateDB;

    // Communication thread.
    private Thread communicationThread;

    public Communication(TauListener tauListener, BlockStore blockStore, StateDB stateDB) {
        this.tauListener = tauListener;
        this.blockStore = blockStore;
        this.stateDB = stateDB;
    }

    /**
     * 死循环
     */
    private void mainLoop() {

    }

    /**
     * Start thread
     *
     * @return boolean successful or not.
     */
    public boolean start() {

        communicationThread = new Thread(this::mainLoop);
        communicationThread.start();

        return true;
    }

    /**
     * Stop thread
     */
    public void stop() {
        if (null != communicationThread) {
            communicationThread.interrupt();
        }
    }

    @Override
    public void onDHTItemGot(byte[] item, Object cbData) {

    }
}
