package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储被收藏的交易和chat消息实体类
 */
@Entity(tableName = "Favorites")
public class Favorite implements Parcelable {
    @NonNull
    @PrimaryKey
    public String ID;                       // txID 或 msgID
    @NonNull
    public String chainID;                  // 交易所属社区chainID
    @NonNull
    public String senderPk;                 // 交易或消息发送者的公钥
    public long fee;                        // 交易费
    public long timestamp;                  // 加入收藏的时间戳
    public long type;                       // 类型，包含TxType中的所有类型、其中新增-1为消息类型
    public String memo;                     // 交易的备注、描述、bootstraps、评论等

    public String receiverPk;               // 交易接收者的公钥 只针对MsgType.Wiring类型
    public long amount;                     // 交易金额 只针对MsgType.Wiring类型
    public String replyID;                  // 被回复的Chat消息ID
    public int isReply;                     // 来源是否是被回复的Chat消息 0:不是回复，UI显示， 1：是，UI不显示

    public Favorite(@NonNull String ID, @NonNull String chainID, @NonNull String senderPk,
                    String receiverPk, long amount, long fee, long type, String memo){
        this.ID = ID;
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.amount = amount;
        this.fee = fee;
        this.type = type;
        this.memo = memo;
    }

    @Ignore
    public Favorite(@NonNull String ID, @NonNull String chainID, @NonNull String senderPk, long type,
                    String memo, String replyID){
        this.ID = ID;
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.type = type;
        this.memo = memo;
        this.replyID = replyID;
    }

    @Ignore
    public Favorite(@NonNull String ID, @NonNull String chainID, @NonNull String senderPk, long type,
                    String memo, String replyID, int isReply){
        this.ID = ID;
        this.chainID = chainID;
        this.senderPk = senderPk;
        this.type = type;
        this.memo = memo;
        this.replyID = replyID;
        this.isReply = isReply;
    }

    @Ignore
    private Favorite(Parcel in) {
        ID = in.readString();
        chainID = in.readString();
        amount = in.readLong();
        fee = in.readLong();
        senderPk = in.readString();
        receiverPk = in.readString();
        memo = in.readString();
        timestamp = in.readLong();
        type = in.readLong();
        replyID = in.readString();
        isReply = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ID);
        dest.writeString(chainID);
        dest.writeLong(amount);
        dest.writeLong(fee);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeString(memo);
        dest.writeLong(timestamp);
        dest.writeLong(type);
        dest.writeString(replyID);
        dest.writeInt(isReply);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Favorite> CREATOR = new Creator<Favorite>() {
        @Override
        public Favorite createFromParcel(Parcel in) {
            return new Favorite(in);
        }

        @Override
        public Favorite[] newArray(int size) {
            return new Favorite[size];
        }
    };

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Favorite && (o == this || ID.equals(((Favorite)o).ID));
    }
}
