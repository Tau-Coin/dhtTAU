package io.taucoin.torrent.publishing.core.model.data;

public class MemoryStatistics {
    private long timeKey;
    private long timestamp;
    private long memoryAvg;

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

    public long getMemoryAvg() {
        return memoryAvg;
    }

    public void setMemoryAvg(long memoryAvg) {
        this.memoryAvg = memoryAvg;
    }
}
