package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;

import static io.taucoin.torrent.publishing.service.Scheduler.SCHEDULER_WORK_SWITCH_WIFI_ONLY;
import static io.taucoin.torrent.publishing.service.Scheduler.SCHEDULER_WORK_WAKE_UP_APP;

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
            case SCHEDULER_WORK_WAKE_UP_APP:
                TauDaemon.getInstance(appContext).start();
                break;
        }
    }

    private void switchWifiOnly(Context appContext, SettingsRepository settingsRepo) {
        if (settingsRepo.wifiOnly())
            return;
        settingsRepo.wifiOnly(true);
        ((WifiManager)appContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
    }
}
