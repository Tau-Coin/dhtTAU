package io.taucoin.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DHTEngineRegulator {

    private static final Logger logger = LoggerFactory.getLogger("DHTEngine-Regulator");

    private long immutableGettingFailCounter;
    private long mutableGettingFailCounter;

    public DHTEngineRegulator() {
        this.immutableGettingFailCounter = 0;
        this.mutableGettingFailCounter = 0;
    }

    public synchronized void immutableGettingFailed() {
        immutableGettingFailCounter++;
        logger.debug("immutable getting fail count:" + immutableGettingFailCounter);
    }

    public synchronized void mutableGettingFailed() {
        mutableGettingFailCounter++;
        logger.debug("mutable getting fail count:" + mutableGettingFailCounter);
    }
}
