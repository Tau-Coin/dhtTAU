package io.taucoin.torrent.publishing.core.settings;

import java.util.List;

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
     * 设置网络状态
     * @param  type 网络类型
     */
    void setInternetType(int type);

    /**
     * 获取网络状态
     * @return type 网络类型
     */
    int getInternetType();

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

    /**
     * 获取社区发送的最后一次交易费
     */
    String lastTxFee(String chainID);

    /**
     * 设置社区发送的最后一次交易费
     */
    void lastTxFee(String chainID, String fee);

    /**
     * 设置不显示ban对话框
     */
    void doNotShowBanDialog(boolean b);

    /**
     * 获取不显示ban对话框
     */
    boolean doNotShowBanDialog();

    /**
     * 设置APK下载任务的ID
     */
    void setApkDownloadID(long downloadID);

    /**
     * 获取APK下载任务的ID
     */
    long getApkDownloadID();

    /**
     * 是否需要提示用户升级
     */
    boolean isNeedPromptUser();

    /**
     * 设置是否需要提示用户升级
     * @param isNeed
     */
    void setNeedPromptUser(boolean isNeed);

    /**
     * UPnP连接是否开启
     */
    boolean isUPnpMapped();

    /**
     * 设置UPnP连接是否开启
     * @param isMapped
     */
    void setUPnpMapped(boolean isMapped);

    /**
     * NAT-PMP连接是否开启
     */
    boolean isNATPMPMapped();

    /**
     * 设置NAT-PMP连接是否开启
     * @param isMapped
     */
    void setNATPMPMapped(boolean isMapped);

    /**
     * 设置正在聊天的朋友
     * @param friend 朋友公钥
     */
    void setChattingFriend(String friend);

    /**
     * 获取正在聊天的朋友
     */
    String getChattingFriend();

    /**
     * 设置CPU使用率
     * @param usage 使用率
     */
    void setCpuUsage(String usage);

    /**
     * 获取CPU使用率
     */
    String getCpuUsage();

    /**
     * 设置内存使用大小
     * @param usage 使用率
     */
    void setMemoryUsage(long usage);

    /**
     * 获取内使用大小
     */
    long getMemoryUsage();

    long getLongValue(String key);

    long getLongValue(String key, long defValue);

    void setLongValue(String key, long value);


    int getIntValue(String key);

    int getIntValue(String key, int defValue);

    void setIntValue(String key, int value);

    boolean getBooleanValue(String key);

    boolean getBooleanValue(String key, boolean defValue);

    void setBooleanValue(String key, boolean value);

    <T> void setListData(String key, List<T> list);

    <T> List<T> getListData(String key, Class<T> cls);
}
