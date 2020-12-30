package io.taucoin.torrent.publishing.core.model;

import io.taucoin.listener.MsgStatus;
import io.taucoin.types.Message;

public abstract class MsgListener implements io.taucoin.listener.MsgListener {
    @Override
    public void onMsgError(String errMsg) {

    }

    @Override
    public void onNewMessage(byte[] friend, Message message) {

    }

    @Override
    public void onReadMessageRoot(byte[] friend, byte[] root) {

    }

    @Override
    public void onDiscoveryFriend(byte[] friend) {

    }

    @Override
    public void onMessageStatus(byte[] root, MsgStatus msgStatus) {

    }
}
