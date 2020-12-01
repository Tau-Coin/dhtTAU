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
