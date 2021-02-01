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
@Entity(tableName = "ChatMessages")
public class ChatMsg implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String hash;                    // 消息的Hash
    @NonNull
    public String senderPk;                // 发送者的公钥
    @NonNull
    public String friendPk;                // 朋友公钥
    @NonNull
    public long timestamp;                 // 时间戳
    public String content;                 // 消息内容
    @NonNull
    public int contentType;                // 消息内容类型
    @NonNull
    public long nonce;                     // 帮助消息排序
    @NonNull
    public int unsent;                     // 0: 未发送， 1: 已发送

    public ChatMsg(@NonNull String hash, String senderPk, String friendPk,
                   String content, int contentType, long timestamp){
        this.hash = hash;
        this.senderPk = senderPk;
        this.friendPk = friendPk;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
    }

    @Ignore
    public ChatMsg(@NonNull String hash, String senderPk, String friendPk,
                   String content, int contentType, long timestamp, long nonce){
        this.hash = hash;
        this.senderPk = senderPk;
        this.friendPk = friendPk;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    @Ignore
    public ChatMsg(String senderPk, String friendPk,
                   String content, int contentType, long timestamp){
        this.senderPk = senderPk;
        this.friendPk = friendPk;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
    }

    @Ignore
    private ChatMsg(Parcel in) {
        id = in.readLong();
        hash = in.readString();
        senderPk = in.readString();
        friendPk = in.readString();
        contentType = in.readInt();
        timestamp = in.readLong();
        content = in.readString();
        nonce = in.readLong();
        unsent = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(hash);
        dest.writeString(senderPk);
        dest.writeString(friendPk);
        dest.writeInt(contentType);
        dest.writeLong(timestamp);
        dest.writeString(content);
        dest.writeLong(nonce);
        dest.writeInt(unsent);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChatMsg> CREATOR = new Creator<ChatMsg>() {
        @Override
        public ChatMsg createFromParcel(Parcel in) {
            return new ChatMsg(in);
        }

        @Override
        public ChatMsg[] newArray(int size) {
            return new ChatMsg[size];
        }
    };

    @Override
    public int hashCode() {
        return String.valueOf(id).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatMsg && (o == this || id == (((ChatMsg)o).id));
    }
}