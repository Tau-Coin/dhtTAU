package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class WorkloadManager {
    private static final Logger logger = LoggerFactory.getLogger("WorkloadManager");
    private static final String TAG_WAKE_UP_WORK = "tag_wake_up_work";
    /**
     * 启动WakeUpWorker
     */
    public static void startWakeUpWorker(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelAllWorkByTag(TAG_WAKE_UP_WORK);
        // 限制条件
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // 网络连接
                .setRequiresBatteryNotLow(true)                 // 不在电量不足时执行
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(WakeUpWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(TAG_WAKE_UP_WORK)
                .build();
        wm.enqueue(request);
        logger.debug("start WakeUpWorker");
    }

    /**
     * 停止WakeUpWorker
     */
    static void stopWakeUpWorker(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelAllWorkByTag(TAG_WAKE_UP_WORK);
        logger.debug("stop WakeUpWorker");
    }
}
