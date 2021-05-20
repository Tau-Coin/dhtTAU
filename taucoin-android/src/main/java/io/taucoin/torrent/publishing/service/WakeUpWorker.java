package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * APP睡眠时唤醒
 */
public class WakeUpWorker extends Worker {

    private static final Logger logger = LoggerFactory.getLogger("WakeUpWorker");
    public WakeUpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        logger.debug("constructor");
        logger.debug("constructor isStopped::{}", isStopped());
    }

    @NonNull
    @Override
    public Result doWork() {
        logger.warn("Process.myPid::{}", android.os.Process.myPid());
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        logger.debug("Worker onStopped");
    }
}
