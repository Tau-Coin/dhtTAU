package io.taucoin.torrent.publishing.core.storage.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 数据库存储社区Member实体类
 */
@Entity
public class Member implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public long id;                     // 自增id
    @NonNull
    public String chainId;              // 成员所属社区的chainId
    @NonNull
    public String publicKey;            // 成员的公钥

    public Member(@NonNull long id, @NonNull String chainId, @NonNull String publicKey){
        this.id = id;
        this.chainId = chainId;
        this.publicKey = publicKey;
    }

    protected Member(Parcel in) {
        id = in.readLong();
        chainId = in.readString();
        publicKey = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(chainId);
        dest.writeString(publicKey);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Member> CREATOR = new Creator<Member>() {
        @Override
        public Member createFromParcel(Parcel in) {
            return new Member(in);
        }

        @Override
        public Member[] newArray(int size) {
            return new Member[size];
        }
    };

    @Override
    public int hashCode() {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Member && (o == this || id == (((Member)o).id));
    }
}
