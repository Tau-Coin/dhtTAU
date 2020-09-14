package io.taucoin.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DHTEngineRegulator {

    private static final Logger logger = LoggerFactory.getLogger("DHTEngine-Regulator");

    private long immutableGettingCounter;
    private long mutableGettingCounter;

    private long immutableGettingFailCounter;
    private long mutableGettingFailCounter;

    public DHTEngineRegulator() {
        this.immutableGettingCounter = 0;
        this.mutableGettingCounter = 0;
        this.immutableGettingFailCounter = 0;
        this.mutableGettingFailCounter = 0;
    }

    public synchronized void immutableItemRequest() {
        immutableGettingCounter++;
    }

    public synchronized void mutableItemRequest() {
        mutableGettingCounter++;
    }

    public synchronized void immutableGettingFailed() {
        immutableGettingFailCounter++;
        logger.debug(String.format("immutable getting fail rate:(%d/%d=%.4f)",
                immutableGettingFailCounter, immutableGettingCounter,
                immutableGettingFailCounter / (double)immutableGettingCounter));
    }

    public synchronized void mutableGettingFailed() {
        mutableGettingFailCounter++;
        logger.debug(String.format("mutable getting fail rate:(%d/%d=%.4f)",
                mutableGettingFailCounter, mutableGettingCounter,
                mutableGettingFailCounter / (double)mutableGettingCounter));
    }
}
