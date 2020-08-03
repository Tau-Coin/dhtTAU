package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room: 数据库存储Transaction实体类
 */
@Entity(tableName = "Messages")
public class Message implements Parcelable {
    @NonNull
    @PrimaryKey
    public String msgHash;                  // 消息的Hash
    @NonNull
    public String chainID;                  // 消息所属社区chainID
    @NonNull
    public String senderPk;                 // 消息发送者的公钥
    @NonNull
    public long timestamp;                  // 消息时间戳
    @NonNull
    public String context;                  // 消息内容

    public Message(@NonNull String chainID, String context){
        this.chainID = chainID;
        this.context = context;
    }

    @Ignore
    private Message(Parcel in) {
        chainID = in.readString();
        senderPk = in.readString();
        context = in.readString();
        timestamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(senderPk);
        dest.writeString(context);
        dest.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @Override
    public int hashCode() {
        return msgHash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Message && (o == this || msgHash.equals(((Message)o).msgHash));
    }
}
