package io.taucoin.dht.session;

class Regulator {

    // The time interval for dht operation.
    public static final long DEFAULT_DHTOPInterval = 1000; // milliseconds.

    private volatile long dhtOPInterval;

    public Regulator() {
        this.dhtOPInterval = DEFAULT_DHTOPInterval;
    }

    public long getDHTOPInterval() {
        return dhtOPInterval;
    }

    public void setDHTOPInterval(long value) {
        this.dhtOPInterval = value;
    }
}
