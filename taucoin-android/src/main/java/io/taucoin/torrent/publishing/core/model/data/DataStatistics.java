package io.taucoin.torrent.publishing.core.model.data;

public class DataStatistics {
    private long timeKey;
    private long timestamp;
    private long meteredDataAvg;
    private long unMeteredDataAvg;

    public long getTimeKey() {
        return timeKey;
    }

    public void setTimeKey(long timeKey) {
        this.timeKey = timeKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getMeteredDataAvg() {
        return meteredDataAvg;
    }

    public void setMeteredDataAvg(long meteredDataAvg) {
        this.meteredDataAvg = meteredDataAvg;
    }

    public long getUnMeteredDataAvg() {
        return unMeteredDataAvg;
    }

    public void setUnMeteredDataAvg(long unMeteredDataAvg) {
        this.unMeteredDataAvg = unMeteredDataAvg;
    }
}
