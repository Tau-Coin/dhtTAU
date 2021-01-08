package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.listener.MsgStatus;

/**
 * 消息状态枚举
 */
public enum ChatMsgStatus {
    UNSENT(-1,
            "Message built"),
    TO_COMMUNICATION_QUEUE(MsgStatus.TO_COMMUNICATION_QUEUE.ordinal(),
            "The message has entered the communication queue"),
    TO_DHT_QUEUE(MsgStatus.TO_DHT_QUEUE.ordinal(),
            "The message has entered the DHT queue"),
    PUT_SUCCESS(MsgStatus.PUT_SUCCESS.ordinal(),
            "DHT put message succeeded"),
    PUT_FAIL(MsgStatus.PUT_FAIL.ordinal(),
            "DHT put message failed"),
    RECEIVED_CONFIRMATION(100,
            "Message arrived");

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
