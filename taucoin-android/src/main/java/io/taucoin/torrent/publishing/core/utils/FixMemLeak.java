package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    private static void fixSamSungLeak(Context context) {
        try {
            if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                Object clipboardManager = context.getSystemService(Class.forName("com.samsung.android.content.clipboard.SemClipboardManager"));
                Field mContext = clipboardManager.getClass().getDeclaredField("mContext");
                mContext.setAccessible(true);
                mContext.set(clipboardManager, null);

                Field mCocktailBarManager = clipboardManager.getClass().getDeclaredField("mCocktailBarManager");
                mCocktailBarManager.setAccessible(true);
                mCocktailBarManager.set(clipboardManager, null);

                Field mPasteListener = clipboardManager.getClass().getDeclaredField("mPasteListener");
                mPasteListener.setAccessible(true);
                mPasteListener.set(clipboardManager, null);

                Field mPersonaManager = clipboardManager.getClass().getDeclaredField("mPersonaManager");
                mPersonaManager.setAccessible(true);
                mPersonaManager.set(clipboardManager, null);
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 修复三星手机紧急模式内训泄漏
     * 修复思路：SemEmergencyManager为单例，在Application.onCreate()中用ApplicationContext先初始化，
     * 防止挟持Activity对象不能释放
     * @param context ApplicationContext
     */
    public static void fixSamSungEmergencyModeLeak(Context context) {
        try {
            if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                Class<?> semEmergencyManager = Class.forName("com.samsung.android.emergencymode.SemEmergencyManager");
                Method method = semEmergencyManager.getMethod("getInstance", Context.class);
                method.invoke(null, context.getApplicationContext());
            }
        } catch (Exception ignore) {
        }
    }
}