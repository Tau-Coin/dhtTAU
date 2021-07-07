package io.taucoin.torrent.publishing.core.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.encrypt.DigestUtils;

/**
 * 设备信息工具类
 */
public class DeviceUtils {

    /**
     * 获取自定义deviceID
     * @param context 上下文
     * @return 字符串
     */
    public static String getCustomDeviceID(@NonNull Context context) {

        StringBuilder stringBuilder = new StringBuilder();

        // 使用设备信息的字符串，拼接一个类似imei的15位串
        stringBuilder.append("35");
        stringBuilder.append(Build.BOARD.length()%10);
        stringBuilder.append(Build.BRAND.length()%10);
        stringBuilder.append(Build.CPU_ABI.length()%10);
        stringBuilder.append(Build.DEVICE.length()%10);
        stringBuilder.append(Build.DISPLAY.length()%10);
        stringBuilder.append(Build.HOST.length()%10);
        stringBuilder.append(Build.ID.length()%10);
        stringBuilder.append(Build.MANUFACTURER.length()%10);
        stringBuilder.append(Build.MODEL.length()%10);
        stringBuilder.append(Build.PRODUCT.length()%10);
        stringBuilder.append(Build.TAGS.length()%10);
        stringBuilder.append(Build.TYPE.length()%10);
        stringBuilder.append(Build.USER.length()%10);

        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        stringBuilder.append(androidId);

//        stringBuilder.append(getMac(context));

        return DigestUtils.md5(stringBuilder.toString());
    }

    /**
     * 获取系统运行内存(RAM)大小：
     * @return
     */
    public static long getRAMTotalMemorySize(){
        Context context = MainApplication.getInstance();
        //获得ActivityManager服务的对象
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo) ;
        return memoryInfo.totalMem;
    }

    /**
     * 获取系统存储空间大小:
     */
    public static long getFreeSpace() {
        return Environment.getExternalStorageDirectory().getFreeSpace();
    }

    /**
     * 是否触发存储空间不足警告
     * Android8.0 根据内存的5%和500M取最小值，作为一个阀值
     */
    public static boolean isSpaceInsufficient() {
        try {
            long freeSpace = Environment.getExternalStorageDirectory().getFreeSpace();
            long totalMemorySize = getRAMTotalMemorySize();
            long insufficient = Math.min(totalMemorySize * 5 / 100, 500 * 1024 * 1024);
            return freeSpace <= insufficient;
        } catch (Exception ignore) {}
        return false;
    }
}
