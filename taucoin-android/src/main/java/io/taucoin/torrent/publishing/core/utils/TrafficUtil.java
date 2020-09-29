package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import java.util.Calendar;
import java.util.Date;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 流量统计工具类
 */
public class TrafficUtil {
    public static final String TRAFFIC_DOWN = "download";
    public static final String TRAFFIC_UP = "upload";

    private static final String TRAFFIC_VALUE_OLD = "pref_key_traffic_old_";
    private static final String TRAFFIC_VALUE = "pref_key_traffic_";
    private static final String TRAFFIC_TIME = "pref_key_traffic_time";

    public static void saveTrafficTotal(String trafficType, long byteSize) {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        resetTrafficInfo();
        String trafficValueOld = TRAFFIC_VALUE_OLD + trafficType;
        long oldTraffic = settingsRepo.getValue(trafficValueOld);
        settingsRepo.setValue(trafficValueOld, byteSize);
        if (oldTraffic >= 0 && byteSize > oldTraffic) {
            byteSize = byteSize - oldTraffic;
        } else {
            byteSize = 0;
        }
        String trafficValue = TRAFFIC_VALUE + trafficType;
        long trafficTotal = byteSize + settingsRepo.getValue(trafficValue);
        settingsRepo.setValue(trafficValue, trafficTotal);
    }

    public static void resetTrafficTotalOld() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        settingsRepo.setValue(TRAFFIC_VALUE_OLD + TRAFFIC_DOWN, 0);
        settingsRepo.setValue(TRAFFIC_VALUE_OLD + TRAFFIC_UP, 0);
    }

    private synchronized static void resetTrafficInfo() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = settingsRepo.getValue(TRAFFIC_TIME);
        if (oldTrafficTime == 0 || compareDay(oldTrafficTime, currentTrafficTime) > 0) {
            settingsRepo.setValue(TRAFFIC_TIME, currentTrafficTime);
            settingsRepo.setValue(TRAFFIC_VALUE + TRAFFIC_DOWN, 0);
            settingsRepo.setValue(TRAFFIC_VALUE + TRAFFIC_UP, 0);
        }
    }

    public static long getTrafficDownloadTotal() {
        resetTrafficInfo();
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        return settingsRepo.getValue(TRAFFIC_VALUE + TRAFFIC_DOWN);
    }

    public static long getTrafficUploadTotal() {
        resetTrafficInfo();
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        return settingsRepo.getValue(TRAFFIC_VALUE + TRAFFIC_UP);
    }

    private static int compareDay(long formerTime, long latterTime) {
        int day = 0;
        if (latterTime > formerTime) {
            try {
                Date date1 = new Date(formerTime);
                Date date2 = new Date(latterTime);
                day = differentDays(date1, date2);
            }catch (Exception ignore) {
            }
        }
        return day;
    }

    private static int differentDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if (year1 != year2) {
            int timeDistance = 0 ;
            for (int i = year1 ; i < year2 ; i ++) {
                if (i%4==0 && i%100!=0 || i%400==0) {
                    timeDistance += 366;
                } else {
                    timeDistance += 365;
                }
            }
            return timeDistance + (day2 - day1) ;
        } else {
            return day2 - day1;
        }
    }
}
