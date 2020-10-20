package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.service.SystemServiceManager;

/**
 * 网络流量设置相关工具类
 */
public class NetworkSetting {
    private static final long meteredLimited = 50 * 1024 * 1024; // 50MB
    private static final long speed_sample = 60; // 单位s
    private static final boolean autoMode = true;
    private static final long NO_SESSIONS = 0;
    private static final long MIN_SESSIONS = 1;
    private static final long MAX_SESSIONS = 32;

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    /**
     * 返回是否是自动模式
     * @return
     */
    public static boolean autoMode() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_auto_mode), autoMode);
    }

    /**
     * 设置是否为自动模式
     * @param autoMode
     */
    public static void setAutoMode(boolean autoMode) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_auto_mode), autoMode);
    }

    /**
     * 获取计费网络流量限制值
     * @return long
     */
    public static long meteredLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_limit), meteredLimited);
    }

    /**
     * 设置计费网络流量限制值
     * @param limited
     */
    public static void setMeteredLimit(long limited) {
        Context context = MainApplication.getInstance();
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_limit), limited);
    }

    /**
     * 设置自动模式计费网络流量限制值
     */
    public static void setAutoModeMeteredLimit() {
        Context context = MainApplication.getInstance();
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_limit), meteredLimited);
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
        if (!SystemServiceManager.getInstance().isNetworkMetered()) {
            clearSpeedList();
           return;
        }
        long total = statistics.getRxBytes() + statistics.getTxBytes();
        long size = TrafficUtil.calculateIncrementalSize(TrafficUtil.getMeteredType(), total);
        List<Long> list = settingsRepo.getListData(context.getString(R.string.pref_key_metered_speed_list),
                Long.class);
        if (list.size() >= speed_sample) {
            list.remove(0);
        }
        list.add(size);
        settingsRepo.setListData(context.getString(R.string.pref_key_metered_speed_list), list);

        updateMeteredSpeedLimit();
    }

    /**
     * 清除网络速度采样值列表
     */
    public static void clearSpeedList() {
        Context context = MainApplication.getInstance();
        settingsRepo.setListData(context.getString(R.string.pref_key_metered_speed_list),
                new ArrayList<>());
    }

    /**
     * 获取计费网络网速
     */
    public static long getMeteredSpeed() {
        Context context = MainApplication.getInstance();
        List<Long> list = settingsRepo.getListData(context.getString(R.string.pref_key_metered_speed_list),
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
        long limit =  meteredLimit();
        long speedLimit = 0;
        if (limit > usage) {
            long todayLastSeconds = DateUtil.getTodayLastSeconds();
            if (todayLastSeconds > 0) {
                speedLimit = (limit - usage) / todayLastSeconds;
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
     * 获取DHT Sessions数
     */
    public static long getDHTSessions() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_sessions),
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
    public static void updateDHTSessions(long sessions) {
        Context context = MainApplication.getInstance();
        settingsRepo.setLongValue(context.getString(R.string.pref_key_sessions),
                sessions);
    }

    /**
     * 计算DHT Session个数
     */
    public static long calculateDHTSessions() {
        long sessions = getDHTSessions();
        long meteredLimit = meteredLimit();
        // 当前网络为计费网络，并且有流量控制
        if (SystemServiceManager.getInstance().isNetworkMetered() && meteredLimit != 0) {
            long currentSpeed = NetworkSetting.getMeteredSpeed();
            long speedLimit = NetworkSetting.getMeteredSpeedLimit();
            if (speedLimit > 0) {
                if (currentSpeed > speedLimit) {
                    if (sessions > MIN_SESSIONS) {
                        sessions --;
                    } else {
                        sessions = MIN_SESSIONS;
                    }
                } else {
                    if (sessions < MAX_SESSIONS) {
                        sessions ++;
                    } else {
                        sessions = MAX_SESSIONS;
                    }
                }
            } else {
                // 超出流量控制范围
                sessions = NO_SESSIONS;
            }
        } else {
            // 不限制DHT Sessions
            sessions = MAX_SESSIONS;
        }
        return sessions;
    }
}