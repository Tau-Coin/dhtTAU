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
    private static final String MSG_LISTEN_HANDLER_WORK_NAME = "MsgListenHandler";
    private static final String PUBLISH_NEW_MSG_WORK_NAME = "PublishNewMsg";
    private static final String RECEIVED_CONFIRMATION_WORK_NAME = "ReceivedConfirmation";

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
        WorkManager.getInstance(context)
                .beginUniqueWork(RECEIVED_CONFIRMATION_WORK_NAME,
                    ExistingWorkPolicy.KEEP, request)
                .enqueue();
    }

    /**
     * 启动MsgListenHandlerWorker
     */
    public static void startMsgListenHandlerWorker(Data data) {
        Context context = MainApplication.getInstance();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MsgListenHandlerWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(context)
                .beginUniqueWork(MSG_LISTEN_HANDLER_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE, request)
                .enqueue();
    }

    /**
     * 启动所有的Worker
     */
    public static void startAllWorker() {
        startPublishNewMsgWorker();
        startReceivedConfirmationWorker();
    }

    /**
     * 关闭所有Work
     */
    public static void cancelAllWork() {
        Context context = MainApplication.getInstance();
        WorkManager.getInstance(context).cancelUniqueWork(PUBLISH_NEW_MSG_WORK_NAME);
        WorkManager.getInstance(context).cancelUniqueWork(RECEIVED_CONFIRMATION_WORK_NAME);
    }
}