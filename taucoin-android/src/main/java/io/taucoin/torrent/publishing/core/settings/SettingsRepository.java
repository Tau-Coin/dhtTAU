package io.taucoin.torrent.publishing.core.settings;

import io.reactivex.Flowable;

/**
 * SettingsRepository: 提供用户设置的接口
 */
public interface SettingsRepository {
    /**
     *
     * @return Flowable
     */
    Flowable<String> observeSettingsChanged();

    /**
     * 随设备启动
     * @return boolean 是否启动
     */
    boolean bootStart();

    /**
     * 设置随设备启动
     * @param val 是否启动
     */
    void bootStart(boolean val);

    /**
     * 服务器模式
     * @return boolean 是否开启
     */
    boolean serverMode();

    /**
     * 设置服务器模式
     * @param val 是否开启
     */
    void serverMode(boolean val);

    /**
     * 只能wifi环境下运行
     * @return boolean 是否开启
     */
    boolean wifiOnly();

    /**
     * 设置使用电信数据结束时间
     * @param time 结束时间
     */
    void telecomDataEndTime(long time);

    /**
     * 使用电信数据结束时间
     * @return long 结束时间
     */
    long telecomDataEndTime();

    /**
     * 设置只能wifi环境下运行
     * @param val 是否开启
     */
    void wifiOnly(boolean val);

    /**
     * 设置充电状态
     * @param val 是否在充电
     */
    void chargingState(boolean val);

    /**
     * 充电状态
     * @return 是否在充电
     */
    boolean chargingState();

    /**
     * 网络状态
     * @return 是否联网
     */
    boolean internetState();

    /**
     * 网络状态
     * @param  val 是否联网
     */
    void internetState(boolean val);

    /**
     * CPU WakeLock
     * @return 是否持有
     */
    boolean wakeLock();

    /**
     * 设置CPU WakeLock
     * @param val 是否持有
     */
    void wakeLock(boolean val);
}
