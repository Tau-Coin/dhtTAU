package io.taucoin.listener;

import io.taucoin.types.Message;

public interface MsgListener {
    /**
     * 新消息通知
     * @param friend 发消息的朋友
     * @param message 消息
     */
    void onNewMessage(byte[] friend, Message message);

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    void onMsgError(String errMsg);
}
