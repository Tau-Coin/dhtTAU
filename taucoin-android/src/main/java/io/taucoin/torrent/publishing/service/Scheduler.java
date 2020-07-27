package io.taucoin.torrent.publishing.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.receiver.SchedulerReceiver;

/**
 * Scheduler: APP启动和停止调度
 */
public class Scheduler {
    public static final String SCHEDULER_WORK_SWITCH_WIFI_ONLY = "scheduler_work_switch_wifi_only";
    public static final String SCHEDULER_WORK_WAKE_UP_APP = "SCHEDULER_WORK_WAKE_UP_APP";
    public static final int WAKE_UP_TIME = 10 * 60;

    private static void setStartStopAppAlarm(@NonNull Context appContext, @NonNull String workTag, Calendar calendar) {
        Intent intent = new Intent(appContext, SchedulerReceiver.class);
        intent.setAction(workTag);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, workTag.hashCode(), intent, 0);
        AlarmManager am = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else{
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        }
    }

    /**
     * 设置切换wifi only的Alarm
     */
    public static void setSwitchWifiOnlyAlarm(@NonNull Context applicationContext, long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        setStartStopAppAlarm(applicationContext, SCHEDULER_WORK_SWITCH_WIFI_ONLY, calendar);
    }

    /**
     * 关闭切换wifi only的Alarm
     */
    public static void cancelSwitchWifiOnlyAlarm(@NonNull Context appContext) {
        Intent intent = new Intent(appContext, SchedulerReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, SCHEDULER_WORK_SWITCH_WIFI_ONLY.hashCode(), intent, 0);
        AlarmManager am = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    /**
     * 设置周期唤醒APP的Alarm
     */
    public static void setWakeUpAppAlarm(@NonNull Context applicationContext) {
        long timeMillis = DateUtil.addTimeDuration(WAKE_UP_TIME);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        setStartStopAppAlarm(applicationContext, SCHEDULER_WORK_WAKE_UP_APP, calendar);
    }

    /**
     * 关闭周期唤醒APP的Alarm
     */
    public static void cancelWakeUpAppAlarm(@NonNull Context appContext) {
        Intent intent = new Intent(appContext, SchedulerReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, SCHEDULER_WORK_WAKE_UP_APP.hashCode(), intent, 0);
        AlarmManager am = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }
}