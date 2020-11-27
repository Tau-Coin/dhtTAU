package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;

public class FixMemLeak {
    private static Field field;
    private static boolean hasField = true;

    public static void fixLeak(Context context) {
        fixHuaWeiLeak(context);
        fixSamSungLeak(context);
    }

    private static void fixHuaWeiLeak(Context context) {
        if (!hasField) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        String[] arr = new String[]{"mLastSrvView"};
        for (String param : arr) {
            try {
                if (field == null) {
                    field = imm.getClass().getDeclaredField(param);
                }
                if (field == null) {
                    hasField = false;
                }
                if (field != null) {
                    field.setAccessible(true);
                    field.set(imm, null);
                }
            } catch (Throwable ignore) {
            }
        }
    }
    public static void fixSamSungLeak(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.MANUFACTURER.equals("samsung")) {
                Object systemService = context.getSystemService(Class.forName("com.samsung.android.content.clipboard.SemClipboardManager"));
                Field mContext = systemService.getClass().getDeclaredField("mContext");
                mContext.setAccessible(true);
                mContext.set(systemService, null);
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NullPointerException ignored) {
        }
    }
}