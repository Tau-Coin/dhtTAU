package io.taucoin.listener;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.types.Message;

public interface MsgListener {
    /**
     * 新消息通知
     * @param friend 发消息的朋友
     * @param messageList 消息列表
     */
    void onNewMessage(byte[] friend, List<Message> messageList);

    /**
     * 新device ID通知
     * @param deviceID device id
     */
    void onNewDeviceID(byte[] deviceID);

    /**
     * 多设备的新朋友通知
     * @param friend 发现的新朋友公钥
     * @param nickname 昵称
     * @param timestamp 起名字的时间戳
     */
    void onNewFriendFromMultiDevice(byte[] friend, byte[] nickname, BigInteger timestamp);

    /**
     * 通知消息状态
     * @param friend 消息接收的俄朋友
     * @param root 消息哈希
     * @param msgStatus 消息当前状态
     */
    void onMessageStatus(byte[] friend, byte[] root, MsgStatus msgStatus);

    /**
     * 正在同步的消息
     * @param message 同步的消息
     * @param timestamp 同步时间
     */
    void onSyncMessage(Message message, BigInteger timestamp);

    /**
     * 已读消息root通知
     * @param friend 发已读消息的朋友
     * @param confirmationRootList 已读消息的root list
     * @param timestamp 对方收到消息的时间
     */
    void onReadMessageRoot(byte[] friend, List<byte[]> confirmationRootList, BigInteger timestamp);

    /**
     * 发现朋友通知
     * @param friend 发现的添加我为好友的朋友
     * @param timestamp 发现朋友的在线时间
     */
    void onDiscoveryFriend(byte[] friend, BigInteger timestamp);

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     */
    void onMsgError(String errMsg);
}
