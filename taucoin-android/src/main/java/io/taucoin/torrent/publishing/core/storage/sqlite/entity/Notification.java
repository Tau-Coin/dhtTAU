package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * 数据库存储Community实体类
 */
@Entity(tableName = "Notifications", primaryKeys = {"senderPk", "chainID"})
public class Notification implements Parcelable {
    @NonNull
    public String senderPk;                 // 发送者的公钥
    @NonNull
    public String chainID;                  // 邀请加入的chainID
    @NonNull
    public String chainLink;                // 邀请加入的chain link
    public long timestamp;                  // 接收到邀请的时间
    public boolean isRead;                  // 接收到的邀请是否已读

    public Notification(@NonNull String senderPk, @NonNull String chainLink,
                        @NonNull String chainID, long timestamp){
        this.senderPk = senderPk;
        this.chainLink = chainLink;
        this.chainID = chainID;
        this.timestamp = timestamp;
    }

    @Ignore
    protected Notification(Parcel in) {
        chainID = in.readString();
        senderPk = in.readString();
        chainLink = in.readString();
        timestamp = in.readLong();
        isRead = in.readInt() == 1 ? true : false;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(senderPk);
        dest.writeString(chainLink);
        dest.writeLong(timestamp);
        dest.writeInt(isRead ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    @Override
    public int hashCode() {
        return chainID.hashCode() + senderPk.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Notification && (o == this || (chainID.equals(((Notification)o).chainID)
                && senderPk.equals(((Notification)o).senderPk)));
    }
}
