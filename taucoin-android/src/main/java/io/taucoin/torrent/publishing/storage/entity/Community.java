package io.taucoin.torrent.publishing.storage.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 数据库存储Community实体类
 */
@Entity
public class Community implements Parcelable {
    @NonNull
    @PrimaryKey
    public String chainId;
    @NonNull
    public String communityName;
    @NonNull
    public String coinName;
    @NonNull
    public String publicKey;
    @NonNull
    public long totalCoin;
    @NonNull
    public int blockInAvg;
    public String intro;
    public String telegramId;
    public boolean mute = false;
    public boolean blocked = false;

    public Community(@NonNull String communityName, @NonNull String coinName, String publicKey, long totalCoin, int blockInAvg, String intro, String telegramId){
        this.communityName = communityName;
        this.coinName = coinName;
        this.publicKey = publicKey;
        this.totalCoin = totalCoin;
        this.blockInAvg = blockInAvg;
        this.intro = intro;
        this.telegramId = telegramId;
    }

    protected Community(Parcel in) {
        chainId = in.readString();
        communityName = in.readString();
        coinName = in.readString();
        publicKey = in.readString();
        totalCoin = in.readLong();
        blockInAvg = in.readInt();
        intro = in.readString();
        telegramId = in.readString();
        mute = in.readByte() != 0;
        blocked = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainId);
        dest.writeString(communityName);
        dest.writeString(coinName);
        dest.writeString(publicKey);
        dest.writeLong(totalCoin);
        dest.writeInt(blockInAvg);
        dest.writeString(intro);
        dest.writeString(telegramId);
        dest.writeByte((byte) (mute ? 1 : 0));
        dest.writeByte((byte) (blocked ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Community> CREATOR = new Creator<Community>() {
        @Override
        public Community createFromParcel(Parcel in) {
            return new Community(in);
        }

        @Override
        public Community[] newArray(int size) {
            return new Community[size];
        }
    };

    @Override
    public int hashCode() {
        return chainId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Community && (o == this || chainId.equals(((Community)o).chainId));
    }
}
