package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * 数据库存储APP使用的流量和内存数据的实体类
 */
@Entity(tableName = "Statistics", primaryKeys = {"timestamp"})
public class Statistic implements Parcelable {
    public long timestamp;
    public long dataSize;
    public long memorySize;

    public Statistic(){
    }

    @Ignore
    public Statistic(long timestamp, long dataSize, long memorySize){
        this.timestamp = timestamp;
        this.dataSize = dataSize;
        this.memorySize = memorySize;
    }

    protected Statistic(Parcel in) {
        timestamp = in.readLong();
        dataSize = in.readLong();
        memorySize = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp);
        dest.writeLong(dataSize);
        dest.writeLong(memorySize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Statistic> CREATOR = new Creator<Statistic>() {
        @Override
        public Statistic createFromParcel(Parcel in) {
            return new Statistic(in);
        }

        @Override
        public Statistic[] newArray(int size) {
            return new Statistic[size];
        }
    };

    @Override
    public int hashCode() {
        return String.valueOf(timestamp).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Statistic && (o == this || (
                timestamp == ((Statistic)o).timestamp));
    }
}
