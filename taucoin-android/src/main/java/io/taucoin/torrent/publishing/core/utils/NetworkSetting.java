package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import com.google.common.primitives.Ints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.data.DataMode;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 网络流量设置相关工具类
 */
public class NetworkSetting {
    private static final Logger logger = LoggerFactory.getLogger("NetworkSetting");
    private static final int METERED_LIMITED;                  // 单位MB
    private static final int WIFI_LIMITED;                     // 单位MB
    private static final int SURVIVAL_SPEED_LIMIT = 10;        // 单位B

    private static SettingsRepository settingsRepo;
    private static long lastStatisticsTime = 0;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
        METERED_LIMITED = context.getResources().getIntArray(R.array.metered_limit)[1];
        WIFI_LIMITED = context.getResources().getIntArray(R.array.wifi_limit)[1];
    }

    /**
     * 获取计费网络流量限制值
     * @return long
     */
    public static int getMeteredLimit() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_metered_limit), METERED_LIMITED);
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
        return settingsRepo.getIntValue(context.getString(R.string.pref_key_wifi_limit), WIFI_LIMITED);
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
     * 更新网络速度
     */

    public synchronized static void updateNetworkSpeed(@NonNull SessionStatistics statistics) {
        Context context = MainApplication.getInstance();
        long currentSpeed = statistics.getDownloadRate() + statistics.getUploadRate();
        settingsRepo.setLongValue(context.getString(R.string.pref_key_current_speed), currentSpeed);

        // 更新Mode运行时间
        updateModeRunningTime();
        updateMeteredSpeedLimit();
        updateWiFiSpeedLimit();
        logger.trace("updateSpeed, CurrentSpeed::{}/s",
                Formatter.formatFileSize(context, currentSpeed).toUpperCase());
    }

    /**
     * APP是否在前台运行
     */
    private static boolean isForegroundRunning() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningKey = appContext.getString(R.string.pref_key_foreground_running);
        return settingsRepo.getBooleanValue(foregroundRunningKey);
    }

    /**
     * 根据APP前后台和流量使用情况，调节运行模式
     */
    private static DataMode regulateRunningMode() {
        DataMode mode;
        boolean enableBgMode = isEnableBackgroundMode();
        // 如何在Debug下启动后台模式，则无论前台和后台直接为后台模式
        if (BuildConfig.DEBUG && enableBgMode) {
            mode = DataMode.BACKGROUND;
        } else {
            // 前台运行，同时每天的前台模式的限制时间没有用完
            long foregroundModeTime = getForegroundModeTime();
            long foregroundModeTimeLimit = getScreenTimeLimitHours() * 60 * 60;
            boolean isOverForegroundModeLimit = foregroundModeTime + 1 > foregroundModeTimeLimit;
            if (isForegroundRunning() && !isOverForegroundModeLimit) {
                mode = DataMode.FOREGROUND;
            } else {
                mode = DataMode.BACKGROUND;
            }
        }
        return mode;
    }

    /**
     * 更新Mode运行时间
     */
    private static void updateModeRunningTime() {
        long currentTime = DateUtil.getMillisTime();
        DataMode mode = regulateRunningMode();
        updateRunningMode(mode);
        if (mode == DataMode.FOREGROUND) {
            int foregroundRunningTime = getForegroundModeTime() + 1;
            updateForegroundModeTime(foregroundRunningTime);
        } else {
            int backgroundRunningTime = getBackgroundModeTime() + 1;
            updateBackgroundModeTime(backgroundRunningTime);

            int dozeTime = (int) ((currentTime - lastStatisticsTime) / 1000 - 1);
            if (lastStatisticsTime > 0 && dozeTime > 0 && !isForegroundRunning()) {
                dozeTime += getDozeModeTime();
                updateDozeModeTime(dozeTime);
            }
        }
        lastStatisticsTime = currentTime;
    }

    /**
     * 获取APP前台运行时间
     */
    public static int getRunningMode() {
        Context appContext = MainApplication.getInstance();
        String runningModeKey = appContext.getString(R.string.pref_key_running_mode);
        return settingsRepo.getIntValue(runningModeKey, 0);
    }

    /**
     * 更新APP前台模式时间
     */
    private static void updateRunningMode(DataMode mode) {
        int dataMode = mode.getMode();
        Context appContext = MainApplication.getInstance();
        String runningModeKey = appContext.getString(R.string.pref_key_running_mode);
        settingsRepo.setIntValue(runningModeKey, dataMode);
    }

    /**
     * 获取APP前台运行时间
     */
    public static int getForegroundModeTime() {
        if (isMeteredNetwork()) {
            return getMeteredForegroundModeTime();
        } else {
            return getWifiForegroundModeTime();
        }
    }

    /**
     * 更新APP前台模式时间
     */
    private static void updateForegroundModeTime(int foregroundRunningTime) {
        if (isMeteredNetwork()) {
            updateMeteredForegroundModeTime(foregroundRunningTime);
        } else {
            updateWifiForegroundModeTime(foregroundRunningTime);
        }
    }

    /**
     * 获取APP计费网络前台运行时间
     */
    private static int getMeteredForegroundModeTime() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_metered_foreground_running_time);
        return settingsRepo.getIntValue(foregroundRunningTimeKey, 0);
    }

    /**
     * 更新APP计费网络前台模式时间
     */
    static void updateMeteredForegroundModeTime(int foregroundRunningTime) {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_metered_foreground_running_time);
        settingsRepo.setIntValue(foregroundRunningTimeKey, foregroundRunningTime);
    }

    /**
     * 获取APP Wifi网络前台运行时间
     */
    private static int getWifiForegroundModeTime() {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_wifi_foreground_running_time);
        return settingsRepo.getIntValue(foregroundRunningTimeKey, 0);
    }

    /**
     * 更新APP Wifi网络前台模式时间
     */
    static void updateWifiForegroundModeTime(int foregroundRunningTime) {
        Context appContext = MainApplication.getInstance();
        String foregroundRunningTimeKey = appContext.getString(R.string.pref_key_wifi_foreground_running_time);
        settingsRepo.setIntValue(foregroundRunningTimeKey, foregroundRunningTime);
    }

    /**
     * 获取APP后台模式时间
     */
    private static int getBackgroundModeTime() {
        Context appContext = MainApplication.getInstance();
        String backgroundRunningTimeKey = appContext.getString(R.string.pref_key_background_running_time);
        return settingsRepo.getIntValue(backgroundRunningTimeKey, 0);
    }

    /**
     * 更新APP后台模式时间
     */
    public static void updateBackgroundModeTime(int backgroundRunningTime) {
        Context appContext = MainApplication.getInstance();
        String backgroundRunningTimeKey = appContext.getString(R.string.pref_key_background_running_time);
        settingsRepo.setIntValue(backgroundRunningTimeKey, backgroundRunningTime);
    }

    /**
     * 获取APP后台模式时间
     */
    private static int getDozeModeTime() {
        Context appContext = MainApplication.getInstance();
        String dozeRunningTimeKey = appContext.getString(R.string.pref_key_doze_running_time);
        return settingsRepo.getIntValue(dozeRunningTimeKey, 0);
    }

    /**
     * 更新APP后台模式时间
     */
    public static void updateDozeModeTime(int dozeTime) {
        Context appContext = MainApplication.getInstance();
        String dozeRunningTimeKey = appContext.getString(R.string.pref_key_doze_running_time);
        settingsRepo.setIntValue(dozeRunningTimeKey, dozeTime);
    }

    /**
     * 获取当前网络网速
     */
    public static long getCurrentSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_current_speed),
                0);
    }

    /**
     * 更新计费网络网速限制值
     */
    public static void updateMeteredSpeedLimit() {
        Context context = MainApplication.getInstance();
        long usage = TrafficUtil.getMeteredTrafficTotal();
        long limit =  getMeteredLimit();
        long screenTimeAverageSpeed = 0;
        long backgroundAverageSpeed = 0;
        long availableData = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);

        // 今天剩余的秒数(24h),到第二天凌晨4点
        long today24HLastSeconds = DateUtil.getTomorrowLastSeconds(TrafficUtil.TRAFFIC_UPDATE_TIME);
        today24HLastSeconds += getDozeModeTime();
        // 今天剩余的秒数
        long todayLastSeconds = getScreenTimeLimitSecond(true) - getMeteredForegroundModeTime();
        // 前台时间比后台时间大，直接使用后台时间，防止前台网速比后台小
        todayLastSeconds = Math.min(todayLastSeconds, today24HLastSeconds);
        if (todayLastSeconds <= 0) {
            todayLastSeconds = today24HLastSeconds;
        }
        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                screenTimeAverageSpeed = availableData / todayLastSeconds;
            }
            if (today24HLastSeconds > 0) {
                backgroundAverageSpeed = availableData / today24HLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_screen_time_average_speed),
                screenTimeAverageSpeed);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_metered_background_average_speed),
                backgroundAverageSpeed);
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
        long limit = getWiFiLimit();
        long screenTimeAverageSpeed = 0;
        long backgroundAverageSpeed = 0;
        long availableData = 0;

        BigInteger bigUnit = new BigInteger("1024");
        BigInteger bigLimit = BigInteger.valueOf(limit).multiply(bigUnit).multiply(bigUnit);
        BigInteger bigUsage = BigInteger.valueOf(usage);
        logger.trace("updateWiFiSpeedLimit bigLimit::{}, bigUsage::{}, compareTo::{}",
                bigLimit.longValue(),
                bigUsage.longValue(),
                bigLimit.compareTo(bigUsage));

        // 今天剩余的秒数(24h),到第二天凌晨4点
        long today24HLastSeconds = DateUtil.getTomorrowLastSeconds(TrafficUtil.TRAFFIC_UPDATE_TIME);
        today24HLastSeconds += getDozeModeTime();
        // 今天剩余的秒数
        long todayLastSeconds = getScreenTimeLimitSecond(false) - getWifiForegroundModeTime();
        // 前台时间比后台时间大，直接使用后台时间，防止前台网速比后台小
        todayLastSeconds = Math.min(todayLastSeconds, today24HLastSeconds);
        if (todayLastSeconds <= 0) {
            todayLastSeconds = today24HLastSeconds;
        }
        if (bigLimit.compareTo(bigUsage) > 0) {
            availableData = bigLimit.subtract(bigUsage).longValue();
            if (todayLastSeconds > 0) {
                screenTimeAverageSpeed = availableData / todayLastSeconds;
            }
            if (today24HLastSeconds > 0) {
                backgroundAverageSpeed = availableData / today24HLastSeconds;
            }
        }
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_available_data), availableData);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_screen_time_average_speed),
                screenTimeAverageSpeed);
        settingsRepo.setLongValue(context.getString(R.string.pref_key_wifi_background_average_speed),
                backgroundAverageSpeed);
    }

    /**
     * 获取每天APP高速前台时间限制 单位h
     */
    private static int getScreenTimeLimitSecond(boolean isMeteredNetwork) {
        return getScreenTimeLimitHours(isMeteredNetwork) * 60 * 60;
    }

    /**
     * 获取每天APP高速前台时间限制 单位H
     */
    public static int getScreenTimeLimitHours() {
        boolean isMeteredNetwork = NetworkSetting.isMeteredNetwork();
        return getScreenTimeLimitHours(isMeteredNetwork);
    }

    /**
     * 获取每天APP高速前台时间限制 单位H
     */
    private static int getScreenTimeLimitHours(boolean isMeteredNetwork) {
        Context context = MainApplication.getInstance();
        int selectIndex;
        int[] screenTimes;
        if (isMeteredNetwork) {
            int selectLimit = NetworkSetting.getMeteredLimit();
            int[] meteredLimits = context.getResources().getIntArray(R.array.metered_limit);
            List<Integer> meteredList = Ints.asList(meteredLimits);
            selectIndex = meteredList.indexOf(selectLimit);
            screenTimes = context.getResources().getIntArray(R.array.metered_screen_time);
        } else {
            int selectLimit = NetworkSetting.getWiFiLimit();
            int[] wifiLimits = context.getResources().getIntArray(R.array.wifi_limit);
            List<Integer> wifiList = Ints.asList(wifiLimits);
            selectIndex = wifiList.indexOf(selectLimit);
            screenTimes = context.getResources().getIntArray(R.array.wifi_screen_time);
        }
        if (selectIndex >= screenTimes.length) {
            selectIndex = 0;
        }
        return screenTimes[selectIndex];
    }

    /**
     * 获取计费网络可用数据
     */
    public static long getMeteredAvailableData() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_available_data));
    }

    /**
     * 获取计费网络在前台平均网速
     */
    public static long getMeteredScreenTimeAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_screen_time_average_speed));
    }

    /**
     * 获取计费网络在后台平均网速
     */
    public static long getMeteredBackgroundAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_metered_background_average_speed));
    }

    /**
     * 获取WiFi网络在前台平均网速
     */
    public static long getWifiScreenTimeAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_screen_time_average_speed));
    }

    /**
     * 获取计费网络在后台平均网速
     */
    public static long getWifiBackgroundAverageSpeed() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getLongValue(context.getString(R.string.pref_key_wifi_background_average_speed));
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
     * 设置是否启动后台数据模式
     */
    public static void enableBackgroundMode(boolean enable) {
        Context context = MainApplication.getInstance();
        settingsRepo.setBooleanValue(context.getString(R.string.pref_key_bg_data_mode), enable);
    }

    /**
     * 获取是否启动后台数据模式
     */
    public static boolean isEnableBackgroundMode() {
        Context context = MainApplication.getInstance();
        return settingsRepo.getBooleanValue(context.getString(R.string.pref_key_bg_data_mode),
                false);
    }

    /**
     * 当前网络是否还有剩余可用流量
     */
    public static boolean isHaveAvailableData() {
        boolean isHaveAvailableData;
        if (NetworkSetting.isMeteredNetwork()) {
            isHaveAvailableData = getMeteredAvailableData() > 0;
        } else {
            isHaveAvailableData = getWiFiAvailableData() > 0;
        }
        return isHaveAvailableData;
    }

    /**
     * 计算主循环时间间隔
     * @return 返回计算的时间间隔
     */
    public static void calculateMainLoopInterval() {
        Interval mainLoopMin;
        Interval mainLoopMax;

        int runningMode = getRunningMode();
        boolean foregroundMode = runningMode == DataMode.FOREGROUND.getMode();
        if (foregroundMode) {
            mainLoopMin = Interval.FORE_MAIN_LOOP_MIN;
            mainLoopMax = Interval.FORE_MAIN_LOOP_MAX;
        } else {
            mainLoopMin = Interval.BACK_MAIN_LOOP_MIN;
            mainLoopMax = Interval.BACK_MAIN_LOOP_MAX;
        }
        if (!isHaveAvailableData()) {
            int timeInterval = mainLoopMax.getInterval();
            logger.trace("calculateMainLoopInterval timeInterval::{}, isHaveAvailableData::false",
                    timeInterval);
            FrequencyUtil.updateMainLoopInterval(timeInterval);
            return;
        } else if (getCurrentSpeed() < SURVIVAL_SPEED_LIMIT) {
            int timeInterval = (mainLoopMin.getInterval() + mainLoopMax.getInterval()) / 2;
            logger.trace("calculateMainLoopInterval timeInterval::{}, currentSpeed::{}",
                    timeInterval, getCurrentSpeed());
            FrequencyUtil.updateMainLoopInterval(timeInterval);
            return;
        }
        long averageSpeed;
        if (isMeteredNetwork()) {
            // 当前网络为计费网络
            // 是否启动后台数据模式
            if (foregroundMode) {
                averageSpeed = NetworkSetting.getMeteredScreenTimeAverageSpeed();
            } else {
                averageSpeed = NetworkSetting.getMeteredBackgroundAverageSpeed();
            }
        } else {
            // 当前网络为非计费网络
            // 是否启动后台数据模式
            if (foregroundMode) {
                averageSpeed = NetworkSetting.getWifiScreenTimeAverageSpeed();
            } else {
                averageSpeed = NetworkSetting.getWifiBackgroundAverageSpeed();;
            }
        }
        long currentSpeed = NetworkSetting.getCurrentSpeed();
        if (averageSpeed > 0) {
            double rate = currentSpeed * 1.0f / averageSpeed;
            int timeInterval = calculateTimeInterval(rate, mainLoopMin, mainLoopMax);
            int lastTimeInterval = FrequencyUtil.getMainLoopAverageInterval();
            logger.debug("calculateMainLoopInterval currentSpeed::{}, averageSpeed::{}, " +
                            "rate::{}, timeInterval::{}, lastTimeInterval::{}, mainLoopMax::{}",
                    currentSpeed, averageSpeed, rate, timeInterval, lastTimeInterval,
                    mainLoopMax.getInterval());
            FrequencyUtil.updateMainLoopInterval(timeInterval);
        }
    }

    /**
     * 计算时间间隔
     * @param rate 平均网速和网速限制的比率
     * @param min 最小值
     * @param max 最大值
     * @return 返回计算的时间间隔
     */
    private static int calculateTimeInterval(double rate, Interval min, Interval max) {
        int timeInterval;
        if (getCurrentSpeed() < SURVIVAL_SPEED_LIMIT) {
            timeInterval = (min.getInterval() + max.getInterval()) / 2;
        } else {
            int lastTimeInterval = FrequencyUtil.getMainLoopAverageInterval();
            timeInterval = Math.max(min.getInterval(), (int)(lastTimeInterval * rate));
            timeInterval = Math.min(timeInterval, max.getInterval());
        }
        return timeInterval;
    }
}