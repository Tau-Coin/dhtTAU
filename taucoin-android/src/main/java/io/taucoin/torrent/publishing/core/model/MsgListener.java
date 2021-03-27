package io.taucoin.torrent.publishing.core.model;

import java.math.BigInteger;

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
     * 多设备的新朋友通知
     * @param friend 发现的新朋友公钥
     * @param nickname 昵称
     * @param timestamp 起名字的时间戳
     */
    @Override
    public void onNewFriendFromMultiDevice(byte[] friend, byte[] nickname, BigInteger timestamp) {}

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
     * @param timestamp 已读消息的时间
     */
    @Override
    public void onReadMessageRoot(byte[] friend, byte[] hash, BigInteger timestamp) {}

    /**
     * 正在同步的消息
     * @param message 同步的消息
     * @param timestamp 同步时间
     */
    @Override
    public void onSyncMessage(Message message, BigInteger timestamp) {}

    /**
     * 发现朋友通知
     * @param friend 发现的添加我为好友的朋友
     * @param timestamp 朋友在线时间
     */
    @Override
    public void onDiscoveryFriend(byte[] friend, BigInteger timestamp) {}

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    @Override
    public void onMsgError(String errMsg) {}
}
