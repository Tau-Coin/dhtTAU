package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.service.WorkloadManager;

/**
 * 设备启动接收器(部分手机如：小米手机需要开启自启动之后，才能收到广播)
 * 第一次安装启动后, 再次重启可接收
 * 设置里点击“强行停止”，那么重启手机后，就收不到BOOT_COMPLETED广播了。
 * 如果该应用被有些三方安全软件强制杀掉进程后，重启手机也会收不到BOOT_COMPLETED广播。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }
        // 设备启动完成
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) ||
                intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            startDaemonAndWakeUpWorker(context);
        }
    }

    /**
     * 启动Daemon和WakeUpWorker
     * @param context
     */
    private void startDaemonAndWakeUpWorker(Context context) {
        Context appContext = context.getApplicationContext();
        TauDaemon.getInstance(appContext).start();
        WorkloadManager.startWakeUpWorker(appContext);
    }
}
