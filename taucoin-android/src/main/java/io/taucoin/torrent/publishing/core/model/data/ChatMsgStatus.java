package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.listener.MsgStatus;

/**
 * 消息状态枚举
 */
public enum ChatMsgStatus {
    UNSENT(-1,
            "Message Built"),
    TO_COMMUNICATION_QUEUE(MsgStatus.TO_COMMUNICATION_QUEUE.ordinal(),
            "Message Processing in Protocol Pool"),
    TO_DHT_QUEUE(MsgStatus.TO_DHT_QUEUE.ordinal(),
            "Message in Transfer Queue"),
    PUT_SUCCESS(MsgStatus.PUT_SUCCESS.ordinal(),
            "DHT_PUT success"),
    PUT_FAIL(MsgStatus.PUT_FAIL.ordinal(),
            "DHT_PUT fail"),
    RECEIVED_CONFIRMATION(100,
            "Message Received by Peer");

    private int status;
    private String statusInfo;
    ChatMsgStatus(int status, String statusInfo) {
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
        for (ChatMsgStatus s : ChatMsgStatus.values()) {
            if (s.getStatus() == status) {
                return s.getStatusInfo();
            }
        }
        return null;
    }
}
