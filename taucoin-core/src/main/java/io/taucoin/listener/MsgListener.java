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
     * 新device ID通知
     * @param deviceID device id
     */
    void onNewDeviceID(byte[] deviceID);

    /**
     * 通知消息状态
     * @param root 消息哈希
     * @param msgStatus 消息当前状态
     */
    void onMessageStatus(byte[] root, MsgStatus msgStatus);

    /**
     * 已读消息root通知
     * @param friend 发已读消息的朋友
     * @param hash 已读消息的hash
     */
    void onReadMessageRoot(byte[] friend, byte[] hash);

    /**
     * 发现朋友通知
     * @param friend 发现的添加我为好友的朋友
     */
    void onDiscoveryFriend(byte[] friend);

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    void onMsgError(String errMsg);
}
