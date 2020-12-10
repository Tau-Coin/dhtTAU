package io.taucoin.torrent.publishing.service;

import android.content.Context;

import java.util.UUID;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.taucoin.torrent.publishing.MainApplication;

/**
 * 发布消息管理
 */
public class PublishManager {
    private static UUID publishWorkID;
    private static UUID confirmationWorkID;

    /**
     * 启动PublishWorker
     */
    public static void startPublishWorker() {
        // 限制条件
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Context context = MainApplication.getInstance();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PublishWorker.class)
                .setConstraints(constraints)
                .build();
        if (null == publishWorkID) {
            publishWorkID = request.getId();
        }
        WorkManager.getInstance(context)
                .beginUniqueWork("PublishMessage",
                        ExistingWorkPolicy.KEEP, request)
                .enqueue();
    }

    /**
     * 启动ReceivedConfirmationWorker
     */
    static void startReceivedConfirmationWorker() {
        // 限制条件
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Context context = MainApplication.getInstance();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReceivedConfirmationWorker.class)
                .setConstraints(constraints)
                .build();
        if (null == confirmationWorkID) {
            confirmationWorkID = request.getId();
        }
        WorkManager.getInstance(context)
                .beginUniqueWork("ReceivedConfirmation",
                        ExistingWorkPolicy.KEEP, request)
                .enqueue();
    }

    /**
     * 启动PublishWorker
     */
    public static void startAllWork() {
        startPublishWorker();
        startReceivedConfirmationWorker();
    }

    /**
     * 关闭所有Work
     */
    public static void cancelAllWork() {
        Context context = MainApplication.getInstance();
        if (publishWorkID != null) {
            WorkManager.getInstance(context).cancelWorkById(publishWorkID);
        }
        if (confirmationWorkID != null) {
            WorkManager.getInstance(context).cancelWorkById(confirmationWorkID);
        }
    }
}