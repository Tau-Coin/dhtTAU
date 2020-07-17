package io.taucoin.torrent.publishing.core.storage.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储Transaction实体类
 */
@Entity
public class Tx implements Parcelable {
    @NonNull
    @PrimaryKey
    public String txID;                     // 交易ID
    @NonNull
    public String chainID;                  // 交易所属社区chainID
    @NonNull
    public String senderPk;                 // 交易发送者的公钥
    public long fee;                        // 交易费
    public long timestamp;                  // 交易时间戳
    public long nonce;                      // 交易nonce
    public int txType;                      // 交易类型，同MsgType中枚举类型
    public String memo;                     // 交易的备注、描述、bootstraps、评论等
    public int txStatus;                    // 交易的状态 0：未上链（在交易池中）；1：上链成功 (不上链)

    public String genesisPk;                // 创世区块者的公钥 只针对MsgType.CommunityAnnouncement类型
    public String receiverPk;               // 交易接收者的公钥 只针对MsgType.Wiring类型
    public long amount;                     // 交易金额 只针对MsgType.Wiring类型
    public String name;                     // 用户的链上名字 只针对MsgType.CommunityAnnouncement类型
    public String replyID;                  // 回复的txID 只针对MsgType.ForumComment类型

    public Tx(@NonNull String chainID, String receiverPk, long amount, long fee, int txType, String memo){
        this.chainID = chainID;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    public Tx(@NonNull String chainID, long fee, int txType, String memo){
        this.chainID = chainID;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    public Tx(@NonNull String chainID, String name, long fee, int txType, String memo){
        this.chainID = chainID;
        this.name = name;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
    }

    @Ignore
    private Tx(Parcel in) {
        txID = in.readString();
        chainID = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        senderPk = in.readString();
        receiverPk = in.readString();
        genesisPk = in.readString();
        memo = in.readString();
        timestamp = in.readLong();
        nonce = in.readLong();
        txType = in.readInt();
        txStatus = in.readInt();
        replyID = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(txID);
        dest.writeString(chainID);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeString(genesisPk);
        dest.writeString(memo);
        dest.writeLong(timestamp);
        dest.writeLong(nonce);
        dest.writeInt(txType);
        dest.writeInt(txStatus);
        dest.writeString(replyID);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Tx> CREATOR = new Creator<Tx>() {
        @Override
        public Tx createFromParcel(Parcel in) {
            return new Tx(in);
        }

        @Override
        public Tx[] newArray(int size) {
            return new Tx[size];
        }
    };

    @Override
    public int hashCode() {
        return txID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tx && (o == this || txID.equals(((Tx)o).txID));
    }
}
