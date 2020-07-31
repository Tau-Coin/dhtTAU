package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 包含被回复的交易信息的实体类
 */
public class ReplyAndTx extends Tx{

    @Relation(parentColumn = "replyID",
            entityColumn = "txID")
    public Tx replyTx;                      // 用户回复的交易数据

    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User sender;                     // 交易发送者对应的用户信息

    @Relation(parentColumn = "replyPk",
            entityColumn = "publicKey")
    public User reply;                      // 被回复者对应的用户信息

    public ReplyAndTx(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo) {
        super(chainID, receiverPk, amount, fee, txType, memo);
    }

    @Ignore
    public ReplyAndTx(@NonNull String chainID, long fee, int txType, String memo) {
        super(chainID, fee, txType, memo);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
