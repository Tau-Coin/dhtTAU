package io.taucoin.torrent.publishing.core.model.data;

public class DataStatistics {
    private long timeKey;
    private long timestamp;
    private long dataAvg;

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

    public long getDataAvg() {
        return dataAvg;
    }

    public void setDataAvg(long dataAvg) {
        this.dataAvg = dataAvg;
    }
}
