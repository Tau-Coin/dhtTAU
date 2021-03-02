package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * 查询ChatMsg, 返回消息发送者的信息
 */
public class ChatMsgAndUser extends ChatMsg {

    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User sender;              // 发送者的信息

    public ChatMsgAndUser(@NonNull String hash, String senderPk, String friendPk, int contentType, long timestamp, long nonce, String previousHash) {
        super(hash, senderPk, friendPk, contentType, timestamp, nonce, previousHash);
    }
}