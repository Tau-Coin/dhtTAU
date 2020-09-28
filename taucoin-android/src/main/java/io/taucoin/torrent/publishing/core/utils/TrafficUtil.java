package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.text.format.Formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

public class TrafficUtil {
    private static final Logger logger = LoggerFactory.getLogger("TrafficUtil");
    public static void saveTrafficTotal(long byteSize){
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        resetTrafficInfo();
        long oldTraffic = settingsRepo.trafficTotalOld();
        settingsRepo.setTrafficTotalOld(byteSize);
        if(oldTraffic > 0 && byteSize > oldTraffic){
            byteSize = byteSize - oldTraffic;
        }else {
            byteSize = 0;
        }
        long trafficTotal = byteSize + settingsRepo.trafficTotal();
        settingsRepo.setTrafficTotal(trafficTotal);
        logger.debug("saveTrafficTotal oldTraffic::{}, trafficTotal::{}, bytesSize::{}",
                Formatter.formatFileSize(context, oldTraffic),
                Formatter.formatFileSize(context, trafficTotal),
                Formatter.formatFileSize(context, byteSize));
    }

    public static void saveTrafficSummaryTotal(long byteSize){
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        resetTrafficInfo();
        settingsRepo.setTrafficTotal(byteSize);
        logger.debug("saveTrafficSummaryTotal, trafficTotal::{}",
                Formatter.formatFileSize(context, byteSize));
    }

    public static long getTrafficTotal() {
        resetTrafficInfo();
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        return settingsRepo.trafficTotal();
    }

    public static void resetTrafficTotalOld() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        settingsRepo.setTrafficTotalOld(0);
    }

    private static void resetTrafficInfo() {
        Context context = MainApplication.getInstance();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(context);
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = settingsRepo.trafficTime();
        if(oldTrafficTime == 0 || compareDay(oldTrafficTime, currentTrafficTime) > 0){
            settingsRepo.setTrafficTime(currentTrafficTime);
            settingsRepo.setTrafficTotalOld(0);
            settingsRepo.setTrafficTotal(0);
        }
    }

    private static int compareDay(long formerTime, long latterTime) {
        int day = 0;
        if(latterTime > formerTime){
            try {
                Date date1 = new Date(formerTime);
                Date date2 = new Date(latterTime);
                day = differentDays(date1, date2);
            }catch (Exception ignore){
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
        if(year1 != year2) {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++) {
                if(i%4==0 && i%100!=0 || i%400==0){
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
