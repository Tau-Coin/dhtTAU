package io.taucoin.dht2.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Counter {

    private static final Logger logger = LoggerFactory.getLogger("DHT-Counter");

    private long immutableGettingCounter;
    private long mutableGettingCounter;

    private long immutableGettingFailCounter;
    private long mutableGettingFailCounter;

    public Counter() {
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
    }

    public synchronized void mutableGettingFailed() {
        mutableGettingFailCounter++;
    }

    public synchronized long getImmutableGettingCounter() {
        return immutableGettingCounter;
    }

    public synchronized long getImmutableGettingFailCounter() {
        return immutableGettingFailCounter;
    }

    public synchronized double getImmutableGettingFailRate() {
        return immutableGettingFailCounter / (double)immutableGettingCounter;
    }

    public synchronized long getMutableGettingCounter() {
        return mutableGettingCounter;
    }

    public synchronized long getMutableGettingFailCounter() {
        return mutableGettingFailCounter;
    }

    public synchronized double getMutableGettingFailRate() {
        return mutableGettingFailCounter / (double)mutableGettingCounter;
    }
}
