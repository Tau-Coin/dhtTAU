package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;

/**
 * Room:Devices操作接口
 */
@Dao
public interface DeviceDao {

    // 查询用户所使用的设备列表
    String QUERY_DEVICES_BY_USER_PK = "SELECT * FROM Devices WHERE userPK = :userPK" +
            " ORDER BY loginTime DESC";
    /**
     * 添加用户设备信息
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long addDevice(Device device);

    /**
     * 更新用户设备信息
     */
    @Update
    int updateDevice(Device device);

    /**
     * 观察用户所用的设备变化
     * @param userPK
     * @return
     */
    @Query(QUERY_DEVICES_BY_USER_PK)
    Observable<List<Device>> observerDevices(String userPK);
}
