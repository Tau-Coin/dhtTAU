package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.service.Scheduler;

import static io.taucoin.torrent.publishing.service.Scheduler.SCHEDULER_WORK_SWITCH_WIFI_ONLY;
import static io.taucoin.torrent.publishing.service.Scheduler.SCHEDULER_WORK_WAKE_UP_APP_SERVICE;

/**
 * The receiver for AlarmManager scheduling
 */
public class SchedulerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }

        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);

        switch (intent.getAction()) {
            case SCHEDULER_WORK_SWITCH_WIFI_ONLY:
                switchWifiOnly(appContext, settingsRepo);
                break;
            case SCHEDULER_WORK_WAKE_UP_APP_SERVICE:
                wakeUpAppService(appContext);
                break;
        }
    }

    /**
     * 唤醒App Service
     */
    private void wakeUpAppService(Context appContext) {
        TauDaemon.getInstance(appContext).start();
        Scheduler.setWakeUpAppAlarm(appContext);
    }

    /**
     * 切换到Wifi Only设置
     */
    private void switchWifiOnly(Context appContext, SettingsRepository settingsRepo) {
        if (settingsRepo.wifiOnly())
            return;
        settingsRepo.wifiOnly(true);
        ((WifiManager)appContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
    }
}
