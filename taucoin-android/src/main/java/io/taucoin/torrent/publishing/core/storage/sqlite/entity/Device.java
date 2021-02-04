package io.taucoin.torrent.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * 数据库存储用户使用的设备Devices实体类
 */
@Entity(tableName = "Devices", primaryKeys = {"userPK", "deviceID"})
public class Device implements Parcelable {
    @NonNull
    public String userPK;              // 用户的公钥
    @NonNull
    public String deviceID;            // 设备ID
    public long loginTime;             // 设备登录时间

    public Device(@NonNull String userPK, @NonNull String deviceID, long loginTime){
        this.userPK = userPK;
        this.deviceID = deviceID;
        this.loginTime = loginTime;
    }

    protected Device(Parcel in) {
        userPK = in.readString();
        deviceID = in.readString();
        loginTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userPK);
        dest.writeString(deviceID);
        dest.writeLong(loginTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    @Override
    public int hashCode() {
        return userPK.hashCode() + deviceID.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        return o instanceof Device && (o == this || (
                userPK.equals(((Device)o).userPK) &&
                        deviceID.equals(((Device)o).deviceID)));
    }
}
