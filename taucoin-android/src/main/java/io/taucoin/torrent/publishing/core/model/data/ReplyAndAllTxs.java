package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Ignore;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * Room: 数据库存储Transaction实体类
 */
public class ReplyAndAllTxs  extends Tx{

    @Relation(parentColumn = "replyID", entityColumn = "txID")
    public Tx replyTx;                  // 用户回复的交易数据

    public String replyName;            // 用户回复用户的最新用户名
    public String nickName;             // 用户设置的最新用户名

    public ReplyAndAllTxs(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo) {
        super(chainID, receiverPk, amount, fee, txType, memo);
    }

    @Ignore
    public ReplyAndAllTxs(@NonNull String chainID, long fee, int txType, String memo) {
        super(chainID, fee, txType, memo);
    }

    @Ignore
    public ReplyAndAllTxs(@NonNull String chainID, String name, long fee, int txType, String memo) {
        super(chainID, name, fee, txType, memo);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
