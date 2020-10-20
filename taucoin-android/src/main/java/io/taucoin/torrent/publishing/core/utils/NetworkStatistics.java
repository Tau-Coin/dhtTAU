package io.taucoin.torrent.publishing.core.utils;

public class NetworkStatistics {
    private long txBytes; // 发送
    private long rxBytes; // 接受

    NetworkStatistics(long txBytes, long rxBytes) {
        this.txBytes = txBytes;
        this.rxBytes = rxBytes;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public void setTxBytes(long txBytes) {
        this.txBytes = txBytes;
    }

    public void setRxBytes(long rxBytes) {
        this.rxBytes = rxBytes;
    }
}
