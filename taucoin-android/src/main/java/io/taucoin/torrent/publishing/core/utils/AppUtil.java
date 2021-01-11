package io.taucoin.torrent.publishing.core.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.util.List;

import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;

public class AppUtil {

    public static String getSysVersion(){
        return android.os.Build.VERSION.RELEASE;
    }

    // Version name
    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    // Version code
    public static int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    /**
     * Does the application run in the foreground
     */
    public static boolean isOnForeground(Context context) {
        android.app.ActivityManager activityManager = (android.app.ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (null == appProcesses) {
            return false;
        }
        for (android.app.ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                return appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }

    /**
     * 判断某个界面是否在前台,返回true，为显示,否则不是
     */
    public static boolean isForeground(Context context, Class<?> zClass) {
        return isForeground(context, zClass.getName());
    }

    /**
     * 判断某个界面是否在前台,返回true，为显示,否则不是
     */
    public static boolean isForeground(Context context, String className) {
        if (context == null || StringUtil.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            String topClassName = cpn.getClassName();
            return className.equals(topClassName);
        }
        return false;
    }

    public static boolean getUnInstallApkInfo(Context context, String filePath) {
        boolean result = false;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                result = true;
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    /**
     * Install the downloaded APK file
     */
    public static void installApk(Context context, String apkPath) {
        File file = new File(apkPath);
        Uri apkUri = FileUtil.getUriForFile(file);
        installApk(context, apkUri);
    }

    public static void installApk(Context context, Uri apkUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //over 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean hasInstallPermission = context.getPackageManager().canRequestPackageInstalls();
            if (!hasInstallPermission) {
                startInstallPermissionSettingActivity(context);
                return;
            }
        }
        // over 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * 启动安装权限设置页面
     * @param context
     */
    private static void startInstallPermissionSettingActivity(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ToastUtils.showShortToast(R.string.permission_install_unknown_apk_note);
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * APP安全退出
     */
    public static void appSafeExit() {
        Context context = MainApplication.getInstance();
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP);
        intent.setComponent(new ComponentName(context.getPackageName(),
                NotificationReceiver.class.getName()));
        context.sendBroadcast(intent);
    }


    /**
     * Check whether the file exists and is valid
     */
    public static boolean isApkFileExists(Context context, String apkPath) {
        File file = new File(apkPath);
        boolean isFileExists = file.exists();
        if(isFileExists){
            isFileExists = AppUtil.getUnInstallApkInfo(context, apkPath);
        }
        return isFileExists;
    }

    public static void killProcess() {
        int myPid = android.os.Process.myPid();
        android.os.Process.killProcess(myPid);
        System.exit(0);
    }
}