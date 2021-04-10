package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * 查询ChatMsg, 返回消息发送者的信息
 */
public class ChatMsgAndUser extends ChatMsg {

    @Ignore
    public byte[] rawContent;          // 原始数据，未加密

    public ChatMsgAndUser(@NonNull String hash, String senderPk, String receiverPk, int contentType,
                          long timestamp, long nonce, String logicMsgHash) {
        super(hash, senderPk, receiverPk, contentType, timestamp, nonce, logicMsgHash);
    }
}