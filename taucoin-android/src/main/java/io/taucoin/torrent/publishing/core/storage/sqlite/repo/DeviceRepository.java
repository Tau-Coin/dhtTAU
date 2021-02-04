package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;

/**
 * 提供操作Friend数据的接口
 */
public interface DeviceRepository {

    /**
     * 添加用户设备信息
     * @param device
     */
   void addDevice(Device device);

    /**
     * 更新用户设备信息
     * @param device
     */
    void updateDevice(Device device);

    /**
     * 观察用户所用的设备变化
     * @param userPK
     * @return
     */
    Observable<List<Device>> observerDevices(String userPK);
}
