package io.taucoin.torrent.publishing.storage.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储User实体类
 */
@Entity
public class User implements Parcelable {
    @NonNull
    @PrimaryKey
    public String publicKey;                // 用户的公钥
    @NonNull
    public String seed;                     // 用户的seed
    public String noteName;                 // 用户本地备注名
    public boolean isCurrentUser = false;   // 是否是当前用户

    public User(@NonNull String publicKey, @NonNull String seed, String noteName, boolean isCurrentUser){
        this.publicKey = publicKey;
        this.seed = seed;
        this.noteName = noteName;
        this.isCurrentUser = isCurrentUser;
    }

    @Ignore
    protected User(Parcel in) {
        publicKey = in.readString();
        seed = in.readString();
        noteName = in.readString();
        isCurrentUser = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(publicKey);
        dest.writeString(seed);
        dest.writeString(noteName);
        dest.writeByte((byte) (isCurrentUser ? 1 : 0));
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
