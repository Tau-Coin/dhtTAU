package io.taucoin.torrent.publishing.core.model;

import io.taucoin.types.Message;

public abstract class MsgListener implements io.taucoin.listener.MsgListener {
    @Override
    public void onMsgError(String errMsg) {

    }

    @Override
    public void onNewMessage(byte[] friend, Message message) {

    }
}
