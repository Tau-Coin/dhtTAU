package io.taucoin.listener;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.types.Message;

public class CompositeMsgListener implements MsgListener {
    List<MsgListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(MsgListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MsgListener listener) {
        listeners.remove(listener);
    }

    /**
     * 新消息通知
     * @param friend 发消息的朋友
     * @param messageList 消息列表
     */
    @Override
    public void onNewMessage(byte[] friend, List<Message> messageList) {
        for (MsgListener listener : listeners) {
            listener.onNewMessage(friend, messageList);
        }
    }

    /**
     * 新device ID通知
     * @param deviceID device id
     */
    @Override
    public void onNewDeviceID(byte[] deviceID) {
        for (MsgListener listener : listeners) {
            listener.onNewDeviceID(deviceID);
        }
    }

    /**
     * 发现的新朋友通知
     *
     * @param friend 发现的新朋友
     * @param nickname 昵称
     * @param timestamp 起名字的时间戳
     */
    @Override
    public void onNewFriendFromMultiDevice(byte[] friend, byte[] nickname, BigInteger timestamp) {
        for (MsgListener listener : listeners) {
            listener.onNewFriendFromMultiDevice(friend, nickname, timestamp);
        }
    }

    /**
     * 通知消息状态
     *
     * @param root      消息哈希
     * @param msgStatus 消息当前状态
     */
    @Override
    public void onMessageStatus(byte[] friend, byte[] root, MsgStatus msgStatus) {
        for (MsgListener listener : listeners) {
            listener.onMessageStatus(friend, root, msgStatus);
        }
    }

    /**
     * 正在同步的消息
     *
     * @param message 同步的消息
     * @param timestamp 同步时间
     */
    @Override
    public void onSyncMessage(Message message, BigInteger timestamp) {
        for (MsgListener listener : listeners) {
            listener.onSyncMessage(message, timestamp);
        }
    }

    /**
     * 已读消息root通知
     *
     * @param friend 发已读消息的朋友
     * @param confirmationRootList   已读消息的root list
     * @param timestamp 对方收到消息的时间
     */
    @Override
    public void onReadMessageRoot(byte[] friend, List<byte[]> confirmationRootList, BigInteger timestamp) {
        for (MsgListener listener : listeners) {
            listener.onReadMessageRoot(friend, confirmationRootList, timestamp);
        }
    }

    /**
     * 发现朋友通知
     *
     * @param friend 发现的添加我为好友的朋友
     * @param timestamp 发现朋友的在线时间
     */
    public void onDiscoveryFriend(byte[] friend, BigInteger timestamp) {
        for (MsgListener listener : listeners) {
            listener.onDiscoveryFriend(friend, timestamp);
        }
    }

    /**
     * This event will be notified if error happens during running time.
     * Note: this event won't be notified if error happens
     * during starting TauController.
     *
     * @param errMsg error msg
     */
    @Override
    public void onMsgError(String errMsg) {
        for (MsgListener listener : listeners) {
            listener.onMsgError(errMsg);
        }
    }
}
