package io.taucoin.torrent.publishing.core.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FixMemLeak {
    private static Field field;
    private static boolean hasField = true;

    public static void fixLeak(Activity activity) {
        fixHuaWeiLeak(activity);
        fixSamSungLeak(activity);
        fixSamSungMultiWindowLeak(activity);
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

    /**
     * 修复三星输入法相关内存泄漏
     * 修复问题，PhoneWindow中多个成员存在内存泄漏，无法一一通过反射清除
     * 解决思路：直接设置PhoneWindow为null, 会产生Crash, 所以采用创建一个新的PhoneWindow对象替换原对象的方案
     */
    private static void fixSamSungMultiWindowLeak(Activity activity) {
        try {
            if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                View view = activity.getWindow().getDecorView();
                Field field = view.getClass().getDeclaredField("mMultiWindowDecorSupportBridge");
                field.setAccessible(true);
                Object mMultiWindowDecorSupportBridge = field.get(view);

                Field IBridge = mMultiWindowDecorSupportBridge.getClass().getDeclaredField("IBridge");
                IBridge.setAccessible(true);
                Object IBridgeObj = IBridge.get(mMultiWindowDecorSupportBridge);

                Field mWindow = IBridgeObj.getClass().getDeclaredField("mWindow");
                mWindow.setAccessible(true);

                Class phoneWindow = Class.forName("com.android.internal.policy.PhoneWindow");
                Constructor constructor = phoneWindow.getDeclaredConstructor(Context.class);
                mWindow.set(IBridgeObj, constructor.newInstance(activity.getApplicationContext()));
            }
        } catch (Exception ignore) {
        }
    }
}