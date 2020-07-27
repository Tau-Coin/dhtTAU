package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/*
 * The receiver for power monitoring.
 */

public class PowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null){
            return;
        }
        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                settingsRepo.chargingState(true);
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                settingsRepo.chargingState(false);
                break;
        }
    }

    public static IntentFilter getCustomFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        return filter;
    }
}
