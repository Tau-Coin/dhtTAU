package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询用户msg和reply
 */
public class MsgAndReply extends Message {
    @Relation(parentColumn = "replyID",
            entityColumn = "msgID")
    public Message replyMsg;

    public String replyName;

    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User sender;

    public long senderBalance;

    public MsgAndReply(@NonNull String chainID, String content) {
        super(chainID, content);
    }
}
