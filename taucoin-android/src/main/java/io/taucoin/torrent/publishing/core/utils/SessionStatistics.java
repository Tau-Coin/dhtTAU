package io.taucoin.torrent.publishing.core.utils;

/**
 * 网络统计值
 */
public class SessionStatistics {
    private long totalUpload;     // 总上传流量
    private long totalDownload;   // 总下载流量
    private long downloadRate;    // 下载速率
    private long uploadRate;      // 上传速率

    public long getTotalUpload() {
        return totalUpload;
    }

    public void setTotalUpload(long totalUpload) {
        this.totalUpload = totalUpload;
    }

    public long getTotalDownload() {
        return totalDownload;
    }

    public void setTotalDownload(long totalDownload) {
        this.totalDownload = totalDownload;
    }

    public long getDownloadRate() {
        return downloadRate;
    }

    public void setDownloadRate(long downloadRate) {
        this.downloadRate = downloadRate;
    }

    public long getUploadRate() {
        return uploadRate;
    }

    public void setUploadRate(long uploadRate) {
        this.uploadRate = uploadRate;
    }
}
