package io.taucoin.dht2;

public class SessionStats {

    public long dhtNodes;
    public long downloadSpeed;
    public long uploadSpeed;
    public long totalDownload;
    public long totalUpload;

    public SessionStats() {}

    public SessionStats(long dhtNodes, long downloadSpeed, long uploadSpeed,
            long totalDownload, long totalUpload) {
        this.dhtNodes = dhtNodes;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.totalDownload = totalDownload;
        this.totalUpload = totalUpload;
    }

    public void update(long dhtNodes, long downloadSpeed, long uploadSpeed,
            long totalDownload, long totalUpload) {
        this.dhtNodes = dhtNodes;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.totalDownload = totalDownload;
        this.totalUpload = totalUpload;
    }

    public long dhtNodes() {
        return dhtNodes;
    }

    public long downloadRate() {
        return downloadSpeed;
    }

    public long uploadRate() {
        return uploadSpeed;
    }

    public long totalDownload() {
        return totalDownload;
    }

    public long totalUpload() {
        return totalUpload;
    }

    @Override
    public String toString() {
        return "(dhtNodes:" + dhtNodes + ", downloadSpeed:" + downloadSpeed
                + ", uploadSpeed:" + uploadSpeed + ", totalDownload:" + totalDownload
                + ", totalUpload:" + totalUpload + ")";
    }
}
