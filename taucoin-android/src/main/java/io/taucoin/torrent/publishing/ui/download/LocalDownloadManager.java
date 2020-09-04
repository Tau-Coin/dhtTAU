/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.ui.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.MutableLiveData;
import io.taucoin.torrent.publishing.R;

/**
 * 本地下载管理
 */
class LocalDownloadManager {
    private Logger logger = LoggerFactory.getLogger("LocalDownloadManager");
    private static final int HANDLE_DOWNLOAD = 0x001;
    static final int DOWNLOAD_STATUS_FAILED = -1;
    static final int DOWNLOAD_STATUS_REMOVED = -2;
    private Context context;
    private DownloadManager downloadManager;
    private DownloadChangeObserver downloadObserver;
    private ScheduledExecutorService scheduledExecutorService;
    private MutableLiveData<Float> onProgress = new MutableLiveData<>();
    private long downloadID; // 系统服务下载的ID
    private int lastStatus;  // 上次的下载的状态

    LocalDownloadManager(Context context){
        this.context = context;
    }
    /**
     * 主线程的handler
     */
    @SuppressLint("HandlerLeak")
    private Handler downLoadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            logger.info("handleMessage, msg.what::{}, HANDLE_DOWNLOAD::{}, status::{}",
                    msg.what, HANDLE_DOWNLOAD, msg.obj);
            if (HANDLE_DOWNLOAD == msg.what) {
                float progress;
                int status = Integer.parseInt(msg.obj.toString());
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    int totalSize = msg.arg2;
                    int downloadSize = msg.arg1;
                    lastStatus = status;

                    progress = msg.arg1 / (float) msg.arg2 * 100;
                    logger.info("handleMessage, totalSize::{}, downloadSize::{}, progress::{}%",
                            totalSize, downloadSize, progress);
                    if(status == DownloadManager.STATUS_SUCCESSFUL
                            || status == DownloadManager.STATUS_FAILED){
                        closeQuerySchedule();
                    }
                    if(status == DownloadManager.STATUS_FAILED){
                        progress = DOWNLOAD_STATUS_FAILED;
                    }
                    onProgress.postValue(progress);
                }else{
                    if(status == 0 && lastStatus != 0){
                        progress = DOWNLOAD_STATUS_REMOVED;
                        lastStatus = 0;
                        onProgress.postValue(progress);
                    }
                }
            }
        }
    };

    private Runnable progressRunnable = this::updateProgress;

    long getDownloadID() {
        return downloadID;
    }

    MutableLiveData<Float> getOnProgress() {
        return onProgress;
    }

    /**
     * 利用系统DownloadManager实现下载升级APK
     * @param context
     * @param downloadUrl 下载URL
     * @param storagePath 存储路径
     */
    void downLoadUpgradeApk(Context context, String downloadUrl, String storagePath) {
        // 创建request对象
        Uri uri = Uri.parse(downloadUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置什么网络情况下可以下载
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                | DownloadManager.Request.NETWORK_WIFI);
        // 设置通知栏的标题
        request.setTitle(context.getString(R.string.app_name));
        // 设置通知栏的message
        request.setDescription(context.getString(R.string.app_upgrade_description));
        // 现在完成后在通知栏里显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 系统下载界面是否显示
        request.setVisibleInDownloadsUi(true);
        // 允许漫游时下载
        request.setAllowedOverRoaming(true);
        // 准许被系统扫描到
        request.allowScanningByMediaScanner();
        // 设置文件类型，以防止部分手机（例如模拟器等）无法正确打开文件
        request.setMimeType("application/vnd.android.package-archive");
        // 设置文件存放目录
        File file = new File(storagePath);
        Uri apkUri = Uri.fromFile(file);
        request.setDestinationUri(apkUri);
        // 获取系统服务
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        // 进入下载队列
        downloadID = downloadManager.enqueue(request);

        downloadObserver = new DownloadChangeObserver();
        registerContentObserver(context, downloadID);
    }

    /**
     * 发送Handler消息更新进度和状态
     * 将查询结果从子线程中发往主线程（handler方式），以防止ANR
     */
    private void updateProgress() {
        logger.info("updateProgress");
        int[] bytesAndStatus = getBytesAndStatus(downloadID);
        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(HANDLE_DOWNLOAD,
                bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]));
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId
     * @return
     */
    private int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{
                -1, -1, 0
        };
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                // 已经下载文件大小
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                // 下载文件的总大小
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                // 下载状态
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        }
        return bytesAndStatus;
    }

    /**
     * 监听下载进度
     */
    private class DownloadChangeObserver extends ContentObserver {

        DownloadChangeObserver() {
            super(downLoadHandler);
            logger.info("DownloadChangeObserver init");
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            // 在子线程中查询
            logger.info("DownloadChangeObserver onChange");
            scheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0,
                    1, TimeUnit.SECONDS);
        }
    }

    /**
     * 关闭定时器，线程等操作
     */
    void closeQuerySchedule() {
        unregisterContentObserver(context);
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }

        if (downLoadHandler != null) {
            downLoadHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver(Context context, long downloadID) {
        // observer download change
        if (downloadObserver != null) {
            Uri ContentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/all_downloads/"),
                    downloadID);
            context.getContentResolver().registerContentObserver(
                    ContentUri,
                    true, downloadObserver);
        }
    }

    /**
     * 注销ContentObserver
     */
    void unregisterContentObserver(Context context) {
        if (downloadObserver != null) {
            context.getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

    /**
     * 从下载中移除
     */
    void removeDownloadID(long id) {
        if (downloadManager != null) {
            downloadManager.remove(id);
        }
    }

    /**
     * 关闭下载
     */
    void closeDownloading() {
        closeQuerySchedule();
        if (downloadManager != null) {
            downloadManager.remove(downloadID);
        }
    }
}