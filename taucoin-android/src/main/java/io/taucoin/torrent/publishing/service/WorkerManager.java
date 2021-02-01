package io.taucoin.torrent.publishing.service;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.taucoin.torrent.publishing.MainApplication;

/**
 * Worker管理
 */
public class WorkerManager {
    private static final String PUBLISH_NEW_MSG_WORK_NAME = "PublishNewMsg";

    /**
     * 启动PublishNewMsgWorker
     */
    public static void startPublishNewMsgWorker() {
        // 限制条件
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Context context = MainApplication.getInstance();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PublishNewMsgWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context)
                .beginUniqueWork(PUBLISH_NEW_MSG_WORK_NAME,
                    ExistingWorkPolicy.KEEP, request)
                .enqueue();
    }

    /**
     * 启动所有的Worker
     */
    public static void startAllWorker() {
        startPublishNewMsgWorker();
    }

    /**
     * 关闭所有Work
     */
    public static void cancelAllWork() {
        Context context = MainApplication.getInstance();
        WorkManager.getInstance(context).cancelUniqueWork(PUBLISH_NEW_MSG_WORK_NAME);
    }
}