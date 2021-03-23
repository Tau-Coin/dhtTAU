package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * Room: 数据库存储ChatMsgLog实体类
 * 聊天消息状态
 */
@Entity(tableName = "ChatMsgLogs", primaryKeys = {"hash", "status"})
public class ChatMsgLog implements Parcelable {
    @NonNull
    public String hash;                    // 消息的Hash
    @NonNull
    public int status;                     // 消息状态 0: 未进入队列，1: 已进入队列，2: 已送达
    @NonNull
    public long timestamp;                 // 消息状态对应的时间

    public ChatMsgLog(@NonNull String hash, int status, long timestamp){
        this.hash = hash;
        this.status = status;
        this.timestamp = timestamp;
    }

    @Ignore
    private ChatMsgLog(Parcel in) {
        hash = in.readString();
        status = in.readInt();
        timestamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeInt(status);
        dest.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChatMsgLog> CREATOR = new Creator<ChatMsgLog>() {
        @Override
        public ChatMsgLog createFromParcel(Parcel in) {
            return new ChatMsgLog(in);
        }

        @Override
        public ChatMsgLog[] newArray(int size) {
            return new ChatMsgLog[size];
        }
    };

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatMsgLog && (o == this || hash .equals (((ChatMsgLog)o).hash));
    }
}