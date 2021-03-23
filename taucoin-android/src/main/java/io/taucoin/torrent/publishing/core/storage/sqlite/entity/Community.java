package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储Community实体类
 */
@Entity(tableName = "Communities")
public class Community implements Parcelable {
    @NonNull
    @PrimaryKey
    public String chainID;                  // 社区的chainID
    @NonNull
    public String communityName;            // 社区名字

    public long totalBlocks;                // 社区总区块数（不上链）
    public long syncBlock;                  // 已同步到区块数（不上链）
    public boolean isBanned = false;        // 社区是否被用户拉入黑名单（不上链）
    @Ignore
    public long totalCoin;                  // 社区总的币量（不上链，不入数据库）
    @Ignore
    public int blockInAvg;                  // 社区创建者的公钥平均出块时间（不上链，不入数据库）
    public String publicKey;                // 社区创建者的公钥

    public Community(@NonNull String chainID, @NonNull String communityName){
        this.communityName = communityName;
        this.chainID = chainID;
    }

    @Ignore
    public Community(@NonNull String communityName){
        this.communityName = communityName;
    }

    @Ignore
    public Community(@NonNull String communityName, String publicKey, long totalCoin, int blockInAvg){
        this.communityName = communityName;
        this.publicKey = publicKey;
        this.totalCoin = totalCoin;
        this.blockInAvg = blockInAvg;
    }

    @Ignore
    public Community(){
    }

    @Ignore
    protected Community(Parcel in) {
        chainID = in.readString();
        communityName = in.readString();
        totalBlocks = in.readLong();
        syncBlock = in.readLong();
        isBanned = in.readByte() != 0;

        totalCoin = in.readLong();
        blockInAvg = in.readInt();
        publicKey = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chainID);
        dest.writeString(communityName);
        dest.writeLong(totalBlocks);
        dest.writeLong(syncBlock);
        dest.writeByte((byte) (isBanned ? 1 : 0));
        dest.writeLong(totalCoin);
        dest.writeInt(blockInAvg);
        dest.writeString(publicKey);
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
        return chainID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Community && (o == this || chainID.equals(((Community)o).chainID));
    }
}
