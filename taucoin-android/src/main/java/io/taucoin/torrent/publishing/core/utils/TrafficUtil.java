package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import java.util.Date;

import androidx.annotation.NonNull;
import io.taucoin.torrent.SessionStats;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.service.SystemServiceManager;

/**
 * 流量统计工具类
 */
public class TrafficUtil {
    private static final String TRAFFIC_DOWN = "download";
    private static final String TRAFFIC_UP = "upload";
    private static final String TRAFFIC_METERED = "metered";

    private static final String TRAFFIC_VALUE_OLD = "pref_key_traffic_old_";
    private static final String TRAFFIC_VALUE = "pref_key_traffic_";
    private static final String TRAFFIC_TIME = "pref_key_traffic_time";

    private static SettingsRepository settingsRepo;
    static {
        Context context = MainApplication.getInstance();
        settingsRepo = RepositoryHelper.getSettingsRepository(context);
    }

    public static void saveTrafficTotal(@NonNull SessionStats newStats) {
        Context context = MainApplication.getInstance();
        saveTrafficTotal(TRAFFIC_DOWN, newStats.totalDownload());
        saveTrafficTotal(TRAFFIC_UP, newStats.totalUpload());
        if (SystemServiceManager.getInstance(context).isNetworkMetered()) {
            long total = newStats.totalDownload() + newStats.totalUpload();
            saveTrafficTotal(TRAFFIC_METERED, total);
        }
    }

    private static void saveTrafficTotal(String trafficType, long byteSize) {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        resetTrafficInfo();
        String trafficValueOld = TRAFFIC_VALUE_OLD + trafficType;
        long incrementalSize = parseIncrementalSize(trafficType, byteSize);
        settingsRepo.setLongValue(trafficValueOld, byteSize);
        String trafficValue = TRAFFIC_VALUE + trafficType;
        long trafficTotal = incrementalSize + settingsRepo.getLongValue(trafficValue);
        settingsRepo.setLongValue(trafficValue, trafficTotal);
    }

    public static long parseIncrementalSize(String trafficType, long byteSize) {
        resetTrafficInfo();
        String trafficValueOld = TRAFFIC_VALUE_OLD + trafficType;
        long oldTraffic = settingsRepo.getLongValue(trafficValueOld);
        if (oldTraffic >= 0 && byteSize > oldTraffic) {
            byteSize = byteSize - oldTraffic;
        } else {
            byteSize = 0;
        }
        return byteSize;
    }

    public static void resetTrafficTotalOld() {
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_DOWN, 0);
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_UP, 0);
        settingsRepo.setLongValue(TRAFFIC_VALUE_OLD + TRAFFIC_METERED, 0);
    }

    private synchronized static void resetTrafficInfo() {
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = settingsRepo.getLongValue(TRAFFIC_TIME);
        if (oldTrafficTime == 0 || DateUtil.compareDay(oldTrafficTime, currentTrafficTime) > 0) {
            settingsRepo.setLongValue(TRAFFIC_TIME, currentTrafficTime);
            settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_DOWN, 0);
            settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_UP, 0);
            settingsRepo.setLongValue(TRAFFIC_VALUE + TRAFFIC_METERED, 0);
        }
    }

    public static long getMeteredTrafficTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_METERED);
    }

    public static String getMeteredType() {
        return TRAFFIC_METERED;
    }

    public static String getMeteredKey() {
        return TRAFFIC_VALUE + TRAFFIC_METERED;
    }

    public static long getTrafficDownloadTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_DOWN);
    }

    public static long getTrafficUploadTotal() {
        resetTrafficInfo();
        return settingsRepo.getLongValue(TRAFFIC_VALUE + TRAFFIC_UP);
    }
}
