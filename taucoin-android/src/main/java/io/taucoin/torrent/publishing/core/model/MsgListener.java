package io.taucoin.torrent.publishing.core.model;

import io.taucoin.listener.MsgStatus;
import io.taucoin.types.Message;

public abstract class MsgListener implements io.taucoin.listener.MsgListener {

    /**
     * 新消息通知
     * @param friend 发消息的朋友
     * @param message 消息
     */
    @Override
    public void onNewMessage(byte[] friend, Message message) {}

    /**
     * 新device ID通知
     * @param deviceID device id
     */
    @Override
    public void onNewDeviceID(byte[] deviceID) {}

    /**
     * 发现的新朋友通知
     * @param friend 发现的新朋友
     */
    @Override
    public void onNewFriend(byte[] friend) {}

    /**
     * 通知消息状态
     * @param friend 消息接收的俄朋友
     * @param root 消息哈希
     * @param msgStatus 消息当前状态
     */
    @Override
    public void onMessageStatus(byte[] friend, byte[] root, MsgStatus msgStatus) {}

    /**
     * 已读消息root通知
     * @param friend 发已读消息的朋友
     * @param hash 已读消息的hash
     */
    @Override
    public void onReadMessageRoot(byte[] friend, byte[] hash) {}

    /**
     * 发现朋友通知
     * @param friend 发现的添加我为好友的朋友
     */
    @Override
    public void onDiscoveryFriend(byte[] friend) {}

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    @Override
    public void onMsgError(String errMsg) {}
}
