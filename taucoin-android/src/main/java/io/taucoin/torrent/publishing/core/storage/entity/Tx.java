package io.taucoin.torrent.publishing.core.storage.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储Transaction实体类
 */
@Entity
public class Tx implements Parcelable {
    @NonNull
    @PrimaryKey
    public String txID;                     // 交易ID
    @NonNull
    public String chainID;                  // 交易所属社区
    @NonNull
    public String senderPubKey;             // 交易发送者的公钥
    public String receiverPubKey;           // 交易接收者的公钥
    public long timestamp;                  // 交易时间戳
    public long amount;                     // 交易金额
    public long fee;                        // 交易费
    public String memo;                     // 交易的备注、介绍等
    public long nonce;                      // nonce
    public int txType;                      // 交易类型，同MsgType中枚举类型
    public int chat;                        // 0: 链上交易; 1: 聊天交易

    public Tx(@NonNull String chainID, String receiverPubKey, long amount, long fee, int txType, String memo, int chat){
        this.chainID = chainID;
        this.receiverPubKey = receiverPubKey;
        this.amount = amount;
        this.fee = fee;
        this.txType = txType;
        this.memo = memo;
        this.chat = chat;
    }

    @Ignore
    private Tx(Parcel in) {
        txID = in.readString();
        chainID = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        senderPubKey = in.readString();
        receiverPubKey = in.readString();
        memo = in.readString();
        timestamp = in.readLong();
        nonce = in.readLong();
        txType = in.readInt();
        chat = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(txID);
        dest.writeString(chainID);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeString(senderPubKey);
        dest.writeString(receiverPubKey);
        dest.writeString(memo);
        dest.writeLong(timestamp);
        dest.writeLong(nonce);
        dest.writeInt(txType);
        dest.writeInt(chat);
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
