package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储Chat实体类
 */
@Entity(tableName = "Chats")
public class Chat implements Parcelable {
    @NonNull
    @PrimaryKey
    public String hash;                    // 消息的Hash
    @NonNull
    public String senderPk;                // 发送者的公钥
    @NonNull
    public String friendPk;                // 朋友公钥
    @NonNull
    public long timestamp;                 // 时间戳
    @NonNull
    public String contextLink;             // 消息内容Link
    @NonNull
    public int contextType;                // 消息内容类型

    public Chat(@NonNull String hash, String senderPk, String friendPk,
                String contextLink, int contextType, long timestamp){
        this.hash = hash;
        this.senderPk = senderPk;
        this.friendPk = friendPk;
        this.contextLink = contextLink;
        this.contextType = contextType;
        this.timestamp = timestamp;
    }

    @Ignore
    private Chat(Parcel in) {
        hash = in.readString();
        senderPk = in.readString();
        friendPk = in.readString();
        contextType = in.readInt();
        timestamp = in.readLong();
        contextLink = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeString(senderPk);
        dest.writeString(friendPk);
        dest.writeInt(contextType);
        dest.writeLong(timestamp);
        dest.writeString(contextLink);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Chat> CREATOR = new Creator<Chat>() {
        @Override
        public Chat createFromParcel(Parcel in) {
            return new Chat(in);
        }

        @Override
        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Chat && (o == this || hash.equals(((Chat)o).hash));
    }
}