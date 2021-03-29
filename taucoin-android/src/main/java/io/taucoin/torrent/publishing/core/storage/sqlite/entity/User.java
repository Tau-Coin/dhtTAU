package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储User实体类
 */
@Entity(tableName = "Users")
public class User implements Parcelable {
    @NonNull
    @PrimaryKey
    public String publicKey;                // 用户的公钥
    public String seed;                     // 用户的seed
    public String nickname;                 // 用户昵称
    public long updateTime;               // 用户昵称更新时间
    public boolean isCurrentUser = false;   // 是否是当前用户
    public boolean isBanned = false;        // 用户是否被用户拉入黑名单

    public User(@NonNull String publicKey, String seed, String nickname, boolean isCurrentUser){
        this.publicKey = publicKey;
        this.seed = seed;
        this.nickname = nickname;
        this.isCurrentUser = isCurrentUser;
    }

    @Ignore
    public User(@NonNull String publicKey, String nickname, long updateTime){
        this.publicKey = publicKey;
        this.nickname = nickname;
        this.updateTime = updateTime;
    }

    @Ignore
    public User(@NonNull String publicKey){
        this.publicKey = publicKey;
    }

    @Ignore
    protected User(Parcel in) {
        publicKey = in.readString();
        seed = in.readString();
        nickname = in.readString();
        isCurrentUser = in.readByte() != 0;
        isBanned = in.readByte() != 0;
        updateTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(publicKey);
        dest.writeString(seed);
        dest.writeString(nickname);
        dest.writeByte((byte) (isCurrentUser ? 1 : 0));
        dest.writeByte((byte) (isBanned ? 1 : 0));
        dest.writeLong(updateTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int hashCode() {
        return publicKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && (o == this || publicKey.equals(((User)o).publicKey));
    }
}
