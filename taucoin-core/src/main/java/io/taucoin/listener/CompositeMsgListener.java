package io.taucoin.listener;

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
     * @param message 消息
     */
    @Override
    public void onNewMessage(byte[] friend, Message message) {
        for (MsgListener listener : listeners) {
            listener.onNewMessage(friend, message);
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
     * 已读消息root通知
     *
     * @param friend 发已读消息的朋友
     * @param hash   已读消息的root
     */
    @Override
    public void onReadMessageRoot(byte[] friend, byte[] hash) {
        for (MsgListener listener : listeners) {
            listener.onReadMessageRoot(friend, hash);
        }
    }

    /**
     * 发现朋友通知
     *
     * @param friend 发现的添加我为好友的朋友
     */
    @Override
    public void onDiscoveryFriend(byte[] friend) {
        for (MsgListener listener : listeners) {
            listener.onDiscoveryFriend(friend);
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
