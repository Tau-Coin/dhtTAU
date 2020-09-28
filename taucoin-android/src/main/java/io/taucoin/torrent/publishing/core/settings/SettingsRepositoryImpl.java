package io.taucoin.torrent.publishing.core.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.R;

/**
 * SettingsRepository: 用户设置的接口的实现
 */
public class SettingsRepositoryImpl implements SettingsRepository {
    private static class Default {
        static final boolean bootStart = true;
        static final boolean serverMode = false;
        static final boolean wifiOnly = true;
        static final boolean chargingState = false;
        static final boolean internetState = false;
        static final boolean wakeLock = false;
        static final boolean isShowBanDialog = false;
    }

    private Context appContext;
    private SharedPreferences pref;

    public SettingsRepositoryImpl(@NonNull Context appContext) {
        this.appContext = appContext;
        pref = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    /**
     * 观察设置改变的工作流
     * @return  Flowable
     */
    @Override
    public Flowable<String> observeSettingsChanged() {
        return Flowable.create((emitter) -> {
            SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
                if (emitter.isCancelled()) {
                    return;
                }
                emitter.onNext(key);
            };
            if (!emitter.isCancelled()) {
                pref.registerOnSharedPreferenceChangeListener(listener);
                emitter.setDisposable(Disposables.fromAction(() ->
                        pref.unregisterOnSharedPreferenceChangeListener(listener)));
            }
        }, BackpressureStrategy.LATEST);
    }

    @Override
    public boolean bootStart() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_boot_start),
                Default.bootStart);
    }

    @Override
    public void bootStart(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_boot_start),val)
                .apply();
    }

    @Override
    public boolean serverMode() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_server_mode),
                Default.serverMode);
    }

    @Override
    public void serverMode(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_server_mode),val)
                .apply();
    }

    @Override
    public boolean wifiOnly() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_wifi_only),
                Default.wifiOnly);
    }

    @Override
    public void telecomDataEndTime(long time) {
        pref.edit().putLong(appContext.getString(R.string.pref_key_telecom_data_end_time), time)
                .apply();
    }

    @Override
    public long telecomDataEndTime() {
        return pref.getLong(appContext.getString(R.string.pref_key_telecom_data_end_time),
                0);
    }

    @Override
    public void wifiOnly(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_wifi_only),val)
                .apply();
    }

    @Override
    public void chargingState(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_charging_state),val)
                .apply();
    }

    @Override
    public boolean chargingState() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_charging_state),
                Default.chargingState);
    }

    @Override
    public boolean internetState() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_internet_state),
                Default.internetState);
    }

    @Override
    public void internetState(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_internet_state),val)
                .apply();
    }

    @Override
    public boolean wakeLock() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_wake_lock),
                Default.wakeLock);
    }

    @Override
    public void wakeLock(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_wake_lock),val)
                .apply();
    }

    @Override
    public String lastTxFee(String chainID){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        return pref.getString(key, "");
    }

    @Override
    public void lastTxFee(String chainID, String fee){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        pref.edit().putString(key, fee)
                .apply();
    }

    @Override
    public void doNotShowBanDialog(boolean isShow) {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        pref.edit().putBoolean(key, isShow)
                .apply();
    }

    @Override
    public boolean doNotShowBanDialog() {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        return pref.getBoolean(key, Default.isShowBanDialog);
    }

    @Override
    public void setApkDownloadID(long downloadID) {
        String key = appContext.getString(R.string.pref_key_apk_download_id);
        pref.edit().putLong(key, downloadID)
                .apply();
    }

    @Override
    public long getApkDownloadID() {
        String key = appContext.getString(R.string.pref_key_apk_download_id);
        return pref.getLong(key, -1);
    }


    @Override
    public boolean isNeedPromptUser() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_need_prompt_user), true);
    }

    @Override
    public void setNeedPromptUser(boolean isNeed) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_need_prompt_user), isNeed)
                .apply();
    }

    @Override
    public long getValue(String key) {
        return pref.getLong(key, 0);
    }

    @Override
    public void setValue(String key, long value) {
        pref.edit().putLong(key, value)
                .apply();
    }
}