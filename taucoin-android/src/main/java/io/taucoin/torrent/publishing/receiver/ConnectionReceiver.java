package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;

/*
 * The receiver for Network connection state changes state.
 */
public class ConnectionReceiver extends BroadcastReceiver {
    private Logger logger = LoggerFactory.getLogger("ConnectionReceiver");
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null){
            return;
        }
        Context appContext = context.getApplicationContext();
        SettingsRepository settingsRepo = RepositoryHelper.getSettingsRepository(appContext);
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            logger.debug("NetworkType::{}, TypeName::{}, isActiveNetworkMetered::{}",
                    info != null ? info.getType() : null,
                    info != null ? info.getTypeName() : null,
                    connectivityManager.isActiveNetworkMetered());
            NetworkSetting.setMeteredNetwork(info != null && connectivityManager.isActiveNetworkMetered());
            if (info != null && (info.getType() == ConnectivityManager.TYPE_WIFI ||
                    info.getType() == ConnectivityManager.TYPE_MOBILE)
                    && info.isConnected()) {
                WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                logger.debug("SSID::{}, BSSID::{}, LinkSpeed::{}", wifiInfo.getSSID(),
                        wifiInfo.getBSSID(), wifiInfo.getLinkSpeed());
                settingsRepo.internetState(true);
            }else{
                settingsRepo.internetState(false);
            }
        }
    }

    public static IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }
}
