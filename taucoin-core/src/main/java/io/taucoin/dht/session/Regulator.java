package io.taucoin.dht.session;

class Regulator {

    // The time interval for dht operation.
    public static final long DEFAULT_DHTOPInterval = 1000; // milliseconds.

    public static final long DHTOPInterval_MIN = DEFAULT_DHTOPInterval; // milliseconds.

    public static final long DHTOPInterval_MAX = 3600 * 1000; // milliseconds.

    private static final long STEP = 1000; // milliseconds.

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

    public void increase() {
        dhtOPInterval += STEP;

        if (dhtOPInterval > DHTOPInterval_MAX) {
            dhtOPInterval = DHTOPInterval_MAX;
        }
    }

    public void decrease() {
        dhtOPInterval -= STEP;

        if (dhtOPInterval < DHTOPInterval_MIN) {
            dhtOPInterval = DHTOPInterval_MIN;
        }
    }
}
