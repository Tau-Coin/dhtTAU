package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.dht.session.SessionController;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class NetworkSetting {
    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");
    private static final int meteredLimited; // 单位MB
    private static final int wifiLimited;   // 单位MB
    private static final long maxSpeedLimited = 50 * 1024;       // 50KB
    // 网速在限制内浮动范围，超过限制，立即减sessions - 1
    private static final long speedRange = 512;                  // 0.5KB
    private static final long speed_sample = 60; // 单位s
    private static final int minSessions = SessionController.MIN_SESSIONS;
    private static final int maxSessions = SessionController.MAX_SESSIONS;

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
        meteredLimited = context.getResources().getIntArray(R.array.metered_limit)[0];
        wifiLimited = context.getResources().getIntArray(R.array.wifi_limit)[0];
    }

    /**
     * 获取计费网络流量限制值
     * @return long
     */
    public static int getMeteredLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_limit), meteredLimited);
    }

    /**
     * 设置计费网络流量限制值
     * @param limited
     */
    public static void setMeteredLimit(int limited) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_metered_limit), limited);
    }

    /**
     * 获取WiFi网络流量限制值
     * @return long
     */
    public static int getWiFiLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_limit), wifiLimited);
    }

    /**
     * 设置WiFi网络流量限制值
     * @param limited
     */
    public static void setWiFiLimit(int limited) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_wifi_limit), limited);
    }

    /**
     * 设置当前是否为计费网络
     */
    public static void setMeteredNetwork(boolean isMetered) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_is_metered_network), isMetered);
    }

    /**
     * 返回当前是否为计费网络
     */
    public static boolean isMeteredNetwork() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_is_metered_network),
                false);
    }

    /**
     * 更新网络速度采样值
     */
    public synchronized static void updateSpeedSample(@NonNull NetworkStatistics statistics) {
        Context context = MainApplication.getInstance();
        long total = statistics.getRxBytes() + statistics.getTxBytes();
        long size;
        if (!isMeteredNetwork()) {
            long upTotalSize = TrafficUtil.calculateIncrementalSize(TrafficUtil.getUpType(),
                    statistics.getTxBytes());
            long downTotalSize = TrafficUtil.calculateIncrementalSize(TrafficUtil.getDownType(),
                    statistics.getRxBytes());
            size = upTotalSize + downTotalSize;
        } else {
            size = TrafficUtil.calculateIncrementalSize(TrafficUtil.getMeteredType(), total);
        }
        List<Long> list = settingsRepo.getListData(context.getString(R.string.pref_key_current_speed_list),
                Long.class);
        if (list.size() >= speed_sample) {
            list.remove(0);
        }
        list.add(size);
        settingsRepo.setListData(context.getString(R.string.pref_key_current_speed_list), list);

        updateMeteredSpeedLimit();
    }

    /**
     * 清除网络速度采样值列表
     */
    public static void clearSpeedList() {
        Context context = MainApplication.getInstance();
        settingsRepo.setListData(context.getString(R.string.pref_key_current_speed_list),
                new ArrayList<>());
    }

    /**
     * 获取当前网络网速
     */
    public static long getCurrentSpeed() {
        Context context = MainApplication.getInstance();
        List<Long> list = settingsRepo.getListData(context.getString(R.string.pref_key_current_speed_list),
                Long.class);
        long totalSpeed = 0;
        for (long speed : list) {
            totalSpeed += speed;
        }
        if (list.size() == 0) {
            return 0;
        }
        return totalSpeed / list.size();
    }

    /**
     * 更新计费网络网速限制值
     */
    public static void updateMeteredSpeedLimit() {
        Context context = MainApplication.getInstance();
        long usage = TrafficUtil.getMeteredTrafficTotal();
        long limit =  getMeteredLimit();
        long speedLimit = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        if (bigLimit.compareTo(bigUsage) > 0) {
            long todayLastSeconds = DateUtil.getTodayLastSeconds();
            if (todayLastSeconds > 0) {
                speedLimit = bigLimit.subtract(bigUsage).longValue() / todayLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_speed_limit), speedLimit);
    }

    /**
     * 获取计费网络网速限制值
     */
    public static long getMeteredSpeedLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_speed_limit));
    }

    /**
     * 更新WiFi网络网速限制值
     */
    public static void updateWiFiSpeedLimit() {
        Context context = MainApplication.getInstance();
        long total = TrafficUtil.getTrafficUploadTotal() + TrafficUtil.getTrafficDownloadTotal();
        long usage = total - TrafficUtil.getMeteredTrafficTotal();
        long limit =  getWiFiLimit();
        long speedLimit = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        if (bigLimit.compareTo(bigUsage) > 0) {
            long todayLastSeconds = DateUtil.getTodayLastSeconds();
            if (todayLastSeconds > 0) {
                speedLimit = bigLimit.subtract(bigUsage).longValue() / todayLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_speed_limit), speedLimit);
    }

    /**
     * 获取WiFi网络网速限制值
     */
    public static long getWiFiSpeedLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_speed_limit));
    }

    /**
     * 获取DHT Sessions数
     */
    public static int getDHTSessions() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_sessions),
                0);
    }

    /**
     * 清除DHT Sessions数
     */
    public static void clearDHTSessions() {
        updateDHTSessions(0);
    }

    /**
     * 更新DHT Sessions数
     */
    public static void updateDHTSessions(int sessions) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_sessions),
                sessions);
    }

    /**
     * 计算DHT Session个数
     */
    public static int calculateDHTSessions() {
        int sessions;
        // 当前网络为计费网络
        if (isMeteredNetwork()) {
            long speedLimit = NetworkSetting.getMeteredSpeedLimit();
            sessions = calculateDHTSessions(speedLimit);
        } else {
            // 当前网络为非计费网络
            sessions = calculateDHTSessions(maxSpeedLimited);
        }
        return sessions;
    }

    /**
     * 根据网速限制计算DHT Session个数
     */
    private static int calculateDHTSessions(long speedLimit) {
        long currentSpeed = NetworkSetting.getCurrentSpeed();
        int sessions = getDHTSessions();
        if (speedLimit > 0) {
            if (currentSpeed > speedLimit) {
                if (sessions > minSessions + 1) {
                    sessions --;
                } else {
                    sessions = minSessions + 1;
                }
            } else if (currentSpeed < speedLimit - speedRange){
                if (sessions < maxSessions) {
                    sessions ++;
                } else {
                    sessions = maxSessions;
                }
            }
        } else {
            // 超出流量控制范围
            sessions = minSessions;
        }
        return sessions;
    }
}