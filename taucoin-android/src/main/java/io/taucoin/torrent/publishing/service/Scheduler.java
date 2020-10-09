package io.taucoin.torrent.publishing.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.receiver.SchedulerReceiver;

/**
 * Scheduler: APP调度
 */
public class Scheduler {
    public static final String SCHEDULER_WORK_WAKE_UP_APP_SERVICE = "scheduler_work_wake_up_app_service";
    private static final int WAKE_UP_TIME = 10 * 60; // 10 minutes

    /**
     * 利用AlarmManager定时启动SchedulerReceiver
     * @param appContext Context
     * @param workTag 工作标签
     * @param timeMillis 时间点
     */
    private static void setAppAlarm(@NonNull Context appContext, @NonNull String workTag, long timeMillis) {
        Intent intent = new Intent(appContext, SchedulerReceiver.class);
        intent.setAction(workTag);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, workTag.hashCode(), intent, 0);
        AlarmManager am = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            am.set(AlarmManager.RTC_WAKEUP, timeMillis, pi);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            am.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pi);
        } else{
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi);
        }
    }

    /**
     * 设置周期唤醒APP的Alarm
     */
    public static void setWakeUpAppAlarm(@NonNull Context applicationContext) {
        long timeMillis = DateUtil.addTimeDuration(WAKE_UP_TIME);
        setAppAlarm(applicationContext, SCHEDULER_WORK_WAKE_UP_APP_SERVICE, timeMillis);
    }

    /**
     * 关闭周期唤醒APP的Alarm
     */
    public static void cancelWakeUpAppAlarm(@NonNull Context appContext) {
        Intent intent = new Intent(appContext, SchedulerReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, SCHEDULER_WORK_WAKE_UP_APP_SERVICE.hashCode(), intent, 0);
        AlarmManager am = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }
}