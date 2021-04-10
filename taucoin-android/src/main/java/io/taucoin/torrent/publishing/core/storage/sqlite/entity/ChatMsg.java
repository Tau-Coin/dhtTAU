package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

/**
 * Room: 数据库存储Chat实体类
 */
@Entity(tableName = "ChatMessages", primaryKeys = "hash",
        indices = {
//        @Index(value = {"senderPk", "receiverPk"}),
        @Index(value = {"timestamp", "logicMsgHash", "nonce"})})
public class ChatMsg implements Parcelable {
    @NonNull
    public String hash;                    // 消息的Hash
    @NonNull
    public String senderPk;                // 发送者的公钥
    @NonNull
    public String receiverPk;              // 接收者的公钥
    @NonNull
    public long timestamp;                 // 时间戳
    @NonNull
    public int contentType;                // 消息内容类型
    @NonNull
    public long nonce;                     // 帮助消息排序
    @NonNull
    public String logicMsgHash;            // 逻辑消息Hash, 包含时间戳保证唯一性
    @NonNull
    public int unsent;                     // 0: 未发送， 1: 已发送
    public byte[] content;                 // 消息内容

    public ChatMsg(@NonNull String hash, String senderPk, String receiverPk, int contentType,
                   long timestamp, long nonce, String logicMsgHash){
        this.hash = hash;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.contentType = contentType;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.logicMsgHash = logicMsgHash;
    }

    @Ignore
    public ChatMsg(@NonNull String hash, String senderPk, String receiverPk, byte[] content,
                   int contentType, long timestamp, long nonce, String logicMsgHash){
        this.hash = hash;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.logicMsgHash = logicMsgHash;
    }

    @Ignore
    private ChatMsg(Parcel in) {
        hash = in.readString();
        senderPk = in.readString();
        receiverPk = in.readString();
        contentType = in.readInt();
        timestamp = in.readLong();
        nonce = in.readLong();
        logicMsgHash = in.readString();
        unsent = in.readInt();
        in.readByteArray(content);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeString(senderPk);
        dest.writeString(receiverPk);
        dest.writeInt(contentType);
        dest.writeLong(timestamp);
        dest.writeByteArray(content);
        dest.writeLong(nonce);
        dest.writeString(logicMsgHash);
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
        return hash.hashCode() + senderPk.hashCode() + receiverPk.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatMsg && (o == this || (hash.equals(((ChatMsg)o).hash)
         && senderPk.equals(((ChatMsg)o).senderPk) && receiverPk.equals(((ChatMsg)o).receiverPk)));
    }
}