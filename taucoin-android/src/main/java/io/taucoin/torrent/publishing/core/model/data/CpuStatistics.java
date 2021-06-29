package io.taucoin.torrent.publishing.core.model.data;

public class CpuStatistics {
    private long timeKey;
    private long timestamp;
    private double cpuUsageRateAvg;

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

    public double getCpuUsageRateAvg() {
        return cpuUsageRateAvg;
    }

    public void setCpuUsageRateAvg(double cpuUsageRateAvg) {
        this.cpuUsageRateAvg = cpuUsageRateAvg;
    }
}
