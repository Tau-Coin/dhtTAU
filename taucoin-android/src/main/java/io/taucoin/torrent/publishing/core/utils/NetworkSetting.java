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
    public static final long average_speed_sample = 86400;    // 单位s，24小时
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
        if (list.size() >= average_speed_sample) {
            list.remove(0);
        }
        list.add(size);
        settingsRepo.setListData(context.getString(R.string.pref_key_current_speed_list), list);

        updateMeteredSpeedLimit();
        updateWiFiSpeedLimit();
        logger.trace("updateSpeed AverageSpeed::{}s, CurrentSpeed::{}s",
                getAverageSpeed(), getCurrentSpeed());
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
            if (listSize > current_speed_sample && i <= listSize - current_speed_sample) {
                break;
            }
        }
        if (list.size() == 0) {
            return 0;
        }
        if (list.size() >= current_speed_sample) {
            return totalSpeed / current_speed_sample;
        } else {
            return totalSpeed / list.size();
        }
    }

    /**
     * 获取网络平均网速
     */
    public static long getAverageSpeed() {
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
        long availableData = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        if (bigLimit.compareTo(bigUsage) > 0) {
            long todayLastSeconds = DateUtil.getTodayLastSeconds();
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                speedLimit = availableData / todayLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_speed_limit), speedLimit);
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

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        logger.trace("updateWiFiSpeedLimit bigLimit::{}, bigUsage::{}, compareTo::{}",
                bigLimit.longValue(),
                bigUsage.longValue(),
                bigLimit.compareTo(bigUsage));
        if (bigLimit.compareTo(bigUsage) > 0) {
            long todayLastSeconds = DateUtil.getTodayLastSeconds();
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                speedLimit = availableData / todayLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_available_data), availableData);
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
     * 计算时间间隔时间
     */
    public static float calculateIntervalRate() {
        long speedLimit;
        // 无网络，返回0；不更新链端时间间隔
        if (!settingsRepo.internetState()) {
            return 0;
        }
        if (isMeteredNetwork()) {
            // 当前网络为计费网络
            speedLimit = NetworkSetting.getMeteredSpeedLimit();
        } else {
            // 当前网络为非计费网络
            speedLimit = NetworkSetting.getWiFiSpeedLimit();
        }
        if (speedLimit > 0) {
            long averageSpeed = NetworkSetting.getCurrentSpeed();
            return averageSpeed * 1.0f / speedLimit;
        } else {
            return -1;
        }
    }

//    /**
//     * 计算Gossip时间间隔
//     * @param rate 平均网速和网速限制的比率
//     * @return 返回计算的时间间隔
//     */
//    @Deprecated
//    public static int calculateGossipInterval(float rate, long sessionNodes) {
//        Context context = MainApplication.getInstance();
//        boolean isOnForeground = AppUtil.isOnForeground(context);
//        if (isMeteredNetwork()) {
//            if (isOnForeground) {
//                return calculateTimeInterval(rate, Interval.GOSSIP_FORE_METERED_MIN,
//                        Interval.GOSSIP_FORE_METERED_MAX, sessionNodes);
//            } else {
//                return calculateTimeInterval(rate, Interval.GOSSIP_BACK_METERED_MIN,
//                        Interval.GOSSIP_BACK_METERED_MAX, sessionNodes);
//            }
//        } else {
//            if (isOnForeground) {
//                return calculateTimeInterval(rate, Interval.GOSSIP_FORE_WIFI_MIN,
//                        Interval.GOSSIP_FORE_WIFI_MAX, sessionNodes);
//            } else {
//                return calculateTimeInterval(rate, Interval.GOSSIP_BACK_WIFI_MIN,
//                        Interval.GOSSIP_BACK_WIFI_MAX, sessionNodes);
//            }
//        }
//    }

    /**
     * 计算主循环时间间隔
     * @param rate 平均网速和网速限制的比率
     * @return 返回计算的时间间隔
     */
    public static int calculateMainLoopInterval(float rate, long sessionNodes) {
        return calculateTimeInterval(rate, Interval.MAIN_LOOP_MIN,
                Interval.MAIN_LOOP_MAX, sessionNodes);
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