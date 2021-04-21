package io.taucoin.torrent.publishing.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.PowerManager;

import io.taucoin.torrent.publishing.MainApplication;

/**
 * 系统服务管理
 */
public class SystemServiceManager {
    private ConnectivityManager connectivityManager;
    private PowerManager powerManager;
    private Context appContext;
    private static volatile SystemServiceManager instance;
    private SystemServiceManager(){
        this.appContext = MainApplication.getInstance();;
        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
    }

    public static SystemServiceManager getInstance() {
        if (instance == null) {
            synchronized (SystemServiceManager.class) {
                if (instance == null)
                    instance = new SystemServiceManager();
            }
        }
        return instance;
    }

    /**
     * 是否有网络连接
     * @return boolean
     */
    public int getInternetType() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isConnected()) {
            return networkInfo.getType();
        } else {
            return -1;
        }
    }

    /**
     * 是否有网络连接
     * @return boolean
     */
    public boolean isHaveNetwork() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * 是否是Mobile连接
     * @return boolean
     */
    public boolean isNetworkMetered() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && connectivityManager.isActiveNetworkMetered();
    }

    /**
     * 是否在充电
     */
    public boolean isPlugged() {
        //创建过滤器拦截电量改变广播
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //通过过滤器来获取电量改变intent 电量改变是系统广播所以无需去设置所以receiver传null即可
        Intent intent = appContext.registerReceiver(null, intentFilter);
        if(intent != null){
            //获取电量信息
            int isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            //电源充电
            boolean acPlugged = BatteryManager.BATTERY_PLUGGED_AC == isPlugged;
            //usb充电
            boolean usbPlugged = BatteryManager.BATTERY_PLUGGED_USB == isPlugged;
            //无线充电
            boolean wirePlugged = BatteryManager.BATTERY_PLUGGED_WIRELESS == isPlugged;

            //满足充电即返回true
            return acPlugged || usbPlugged || wirePlugged;
        }
        return false;
    }
}
