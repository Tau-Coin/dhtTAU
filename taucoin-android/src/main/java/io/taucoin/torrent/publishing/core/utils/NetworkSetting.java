package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class NetworkSetting {
    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");
    private static final int meteredLimited;                  // 单位MB
    private static final int wifiLimited;                     // 单位MB
    public static final long current_speed_sample = 10;       // 单位s
    public static final float min_threshold = 1.0f / 4;       // 比率最小阀值
    public static final float max_threshold = 3.0f / 4;       // 比率最大阀值
    public static final int less_nodes_threshold = 50;        // 节点少阀值，可能网络状况差

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
        if (list.size() >= current_speed_sample) {
            list.remove(0);
        }
        list.add(size);
        settingsRepo.setListData(context.getString(R.string.pref_key_current_speed_list), list);

        updateMeteredSpeedLimit();
        updateWiFiSpeedLimit();
        logger.trace("updateSpeed WiFiAverageSpeed::{}s, MeteredAverageSpeed::{}s, CurrentSpeed::{}s",
                getWiFiAverageSpeed(), getMeteredAverageSpeed(), getCurrentSpeed());
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
        int listSize = list.size();
        for (int i = listSize - 1; i >= 0; i--) {
            totalSpeed += list.get(i);
        }
        if (list.size() == 0) {
            return 0;
        }
        return totalSpeed / list.size();
    }

    /**
     * 获取计费网络网络平均网速
     */
    public static long getMeteredAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_average_speed));
    }

    /**
     * 获取WiFi网络平均网速
     */
    public static long getWiFiAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_average_speed));
    }

    /**
     * 更新计费网络网速限制值
     */
    public static void updateMeteredSpeedLimit() {
        Context context = MainApplication.getInstance();
        long usage = TrafficUtil.getMeteredTrafficTotal();
        long limit =  getMeteredLimit();
        long speedLimit = 0;
        long availableData = 0;
        long averageSpeed = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);

        long todayAllSeconds = 24 * 60 * 60;  // 全天所有的秒数
        long todayLastSeconds = DateUtil.getTodayLastSeconds();  // 今天剩余的秒数
        long todayPassedSeconds = todayAllSeconds - todayLastSeconds;  // 今天过去的秒数
        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                speedLimit = availableData / todayLastSeconds;
            }
        }
        if (todayPassedSeconds > 0) {
            averageSpeed = usage / todayPassedSeconds;
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_speed_limit), speedLimit);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_average_speed), averageSpeed);
    }

    /**
     * 获取计费网络可用数据
     */
    public static long getMeteredAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_available_data));
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
        logger.trace("updateWiFiSpeedLimit total::{}, MeteredTotal::{}, wifiUsage::{}", total,
                TrafficUtil.getMeteredTrafficTotal(), usage);
        long limit =  getWiFiLimit();
        long speedLimit = 0;
        long availableData = 0;
        long averageSpeed = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        logger.trace("updateWiFiSpeedLimit bigLimit::{}, bigUsage::{}, compareTo::{}",
                bigLimit.longValue(),
                bigUsage.longValue(),
                bigLimit.compareTo(bigUsage));
        long todayAllSeconds = 24 * 60 * 60;  // 全天所有的秒数
        long todayLastSeconds = DateUtil.getTodayLastSeconds();  // 今天剩余的秒数
        long todayPassedSeconds = todayAllSeconds - todayLastSeconds;  // 今天过去的秒数
        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                speedLimit = availableData / todayLastSeconds;
            }
        }
        if (todayPassedSeconds > 0) {
            averageSpeed = usage / todayPassedSeconds;
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_speed_limit), speedLimit);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_average_speed), averageSpeed);
    }

    /**
     * 获取WiFi网络网速限制值
     */
    public static long getWiFiSpeedLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_speed_limit));
    }

    /**
     * 获取WiFi网络可用数据
     */
    public static long getWiFiAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_available_data));
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
     * 更新DHT Sessions数
     */
    public static void updateDHTSessions(int sessions) {
        Context context = MainApplication.getInstance();
        settingsRepo.setIntValue(context.getString(R.string.pref_key_sessions),
                sessions);
    }

    /**
     * 计算主循环时间间隔
     * @param sessionNodes session个数
     * @return 返回计算的时间间隔
     */
    public static int calculateMainLoopInterval(long sessionNodes) {
        // 无网络，返回0；不更新链端时间间隔
        if (!settingsRepo.internetState()) {
            return 0;
        }
        long speedLimit;
        long averageSpeed;
        if (isMeteredNetwork()) {
            // 当前网络为计费网络
            speedLimit = NetworkSetting.getMeteredSpeedLimit();
            averageSpeed = NetworkSetting.getMeteredAverageSpeed();
        } else {
            // 当前网络为非计费网络
            speedLimit = NetworkSetting.getWiFiSpeedLimit();
            averageSpeed = NetworkSetting.getWiFiAverageSpeed();
        }
        Context appContext = MainApplication.getInstance();
        String foregroundRunningKey = appContext.getString(R.string.pref_key_foreground_running);
        Interval mainLoopMin;
        Interval mainLoopMax;
        // 时间间隔的大小，根据APP在前后台而不同
        if (settingsRepo.getBooleanValue(foregroundRunningKey)) {
            mainLoopMin = Interval.FORE_MAIN_LOOP_MIN;
            mainLoopMax = Interval.FORE_MAIN_LOOP_MAX;
        } else {
            mainLoopMin = Interval.BACK_MAIN_LOOP_MIN;
            mainLoopMax = Interval.BACK_MAIN_LOOP_MAX;
        }
        if (speedLimit > 0) {
            float rate = averageSpeed * 1.0f / speedLimit;
            return calculateTimeInterval(rate, mainLoopMin,
                    mainLoopMax, sessionNodes);
        } else {
            return -1;
        }
    }

    /**
     * 计算时间间隔
     * @param rate 平均网速和网速限制的比率
     * @param min 最小值
     * @param max 最大值
     * @return 返回计算的时间间隔
     */
    private static int calculateTimeInterval(float rate, Interval min, Interval max, long sessionNodes) {
        int timeInterval;
        // 比率大于等于最大阀值或sessionNodes小于等于阀值限制，使用最大时间间隔
        if (rate >= max_threshold || sessionNodes <= less_nodes_threshold) {
            timeInterval = max.getInterval();
        } else if (rate <= min_threshold) {
            timeInterval = min.getInterval();
        } else {
            timeInterval = min.getInterval();
            timeInterval += (int)((rate - min_threshold) / (max_threshold - min_threshold)
                    * (max.getInterval() - min.getInterval()));
        }
        return timeInterval;
    }
}