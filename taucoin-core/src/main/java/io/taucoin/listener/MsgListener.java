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
     * 已读消息root通知
     * @param friend 发已读消息的朋友
     * @param root 已读消息的root
     */
    void onReadMessageRoot(byte[] friend, byte[] root);

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
