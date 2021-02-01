package io.taucoin.torrent.publishing.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
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
}
