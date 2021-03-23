package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * 数据库存储朋友关系状态Friends实体类
 */
@Entity(tableName = "Friends", primaryKeys = {"userPK", "friendPK"})
public class Friend implements Parcelable {
    @NonNull
    public String userPK;              // 用户的公钥
    @NonNull
    public String friendPK;            // 朋友的公钥
    public long lastCommTime;          // 朋友之间上次交流时间
    public long lastSeenTime;          // 上次看到朋友的时间
    public int status;                 // 对应枚举类FriendStatus中状态
    public int msgUnread;              // 是否存在消息未读 0：已读，1：未读

    public Friend(@NonNull String userPK, @NonNull String friendPK){
        this.userPK = userPK;
        this.friendPK = friendPK;
    }

    @Ignore
    public Friend(@NonNull String userPK, @NonNull String friendPK, int status){
        this.userPK = userPK;
        this.friendPK = friendPK;
        this.status = status;
    }

    protected Friend(Parcel in) {
        userPK = in.readString();
        friendPK = in.readString();
        lastCommTime = in.readLong();
        lastSeenTime = in.readLong();
        status = in.readInt();
        msgUnread = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userPK);
        dest.writeString(friendPK);
        dest.writeInt(status);
        dest.writeLong(lastCommTime);
        dest.writeLong(lastSeenTime);
        dest.writeInt(status);
        dest.writeInt(msgUnread);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Friend> CREATOR = new Creator<Friend>() {
        @Override
        public Friend createFromParcel(Parcel in) {
            return new Friend(in);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };

    @Override
    public int hashCode() {
        return userPK.hashCode() + friendPK.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        return o instanceof Friend && (o == this || (
                userPK.equals(((Friend)o).userPK) &&
                        friendPK.equals(((Friend)o).friendPK)));
    }
}
