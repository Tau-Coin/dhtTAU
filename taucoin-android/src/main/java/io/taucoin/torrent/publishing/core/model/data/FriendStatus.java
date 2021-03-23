package io.taucoin.torrent.publishing.core.model.data;

/**
 * 加朋友的状态
 */
public enum FriendStatus {
    DISCOVERED(0, "Discovered"),
    ADDED(1, "Added"),
    CONNECTED(2, "Connected");

    private int status;
    private String statusInfo;
    FriendStatus(int status, String statusInfo) {
        this.status = status;
        this.statusInfo = statusInfo;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public static String getStatusInfo(int status) {
        for (FriendStatus s : FriendStatus.values()) {
            if (s.getStatus() == status) {
                return s.getStatusInfo();
            }
        }
        return null;
    }
}
