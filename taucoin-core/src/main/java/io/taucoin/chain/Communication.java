package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.taucoin.db.BlockStore;
import io.taucoin.db.DBException;
import io.taucoin.db.StateDB;
import io.taucoin.dht.DHT;
import io.taucoin.listener.TauListener;
import io.taucoin.util.ByteArrayWrapper;

public class Communication implements DHT.GetDHTItemCallback {
    private static final Logger logger = LoggerFactory.getLogger("Communication");

    // 循环间隔最小时间
    private final int MIN_LOOP_INTERVAL_TIME = 50; // 50 ms

    private final TauListener tauListener;

    // block db
    private final BlockStore blockStore;

    // state db
    private final StateDB stateDB;

    // peer
    private final Set<ByteArrayWrapper> peer = new HashSet<>();

    // 最新时间
    private final Map<Pair<byte[], byte[]>, Long> timeStamp = Collections.synchronizedMap(new HashMap<>());

    // message root hash
    private final Map<Pair<byte[], byte[]>, byte[]> rootHash = Collections.synchronizedMap(new HashMap<>());

    // Communication thread.
    private Thread communicationThread;

    public Communication(BlockStore blockStore, StateDB stateDB, TauListener tauListener) {
        this.blockStore = blockStore;
        this.stateDB = stateDB;
        this.tauListener = tauListener;
    }

    /**
     * 死循环
     */
    private void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

            }/* catch (DBException e) {
                this.tauListener.onTauError("Data Base Exception!");
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }*/ catch (Exception e) {
                logger.error(e.getMessage(), e);

                try {
                    Thread.sleep(this.MIN_LOOP_INTERVAL_TIME);
                } catch (InterruptedException ex) {
                    logger.info(ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
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
