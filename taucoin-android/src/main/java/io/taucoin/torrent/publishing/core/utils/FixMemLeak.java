package io.taucoin.torrent.publishing.core.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.fragment.app.Fragment;

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

    /**
     * 修复三星手机SemClipboardManager中变量内存泄漏
     */
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
     * 修复三星手机紧急模式内存泄漏
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
     * 修复三星分屏中相关内存泄漏
     * 1、低版本
     * 修复问题，PhoneWindow中多个成员存在内存泄漏，无法一一通过反射清除
     * 解决思路：直接设置PhoneWindow为null, 会产生Crash, 所以采用创建一个新的PhoneWindow对象替换原对象的方案
     * 2、高版本
     * 修复三星Android9及更高版本分屏中的内存泄漏
     * 由于Android9及更高版本, 对@hide注释的类和方法做了反射限制，
     * @see HookUtils 所以在这里通过两次反射，代理Class.getDeclaredMethod方法来绕过此限制
     */
    private static void fixSamSungMultiWindowLeak(Activity activity) {
        try {
            if (Build.MANUFACTURER.equals("samsung")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    View view = activity.getWindow().getDecorView();
                    Field field = view.getClass().getDeclaredField("mMultiWindowDecorSupportBridge");
                    field.setAccessible(true);
                    Object mMultiWindowDecorSupportBridge = field.get(view);

                    Field IBridge = mMultiWindowDecorSupportBridge.getClass().getDeclaredField("IBridge");
                    IBridge.setAccessible(true);
                    Object IBridgeObj = IBridge.get(mMultiWindowDecorSupportBridge);

                    Field mWindow = IBridgeObj.getClass().getDeclaredField("mWindow");
                    mWindow.setAccessible(true);

                    // 创建一个新的PhoneWindow对象替换原对象
                    Class phoneWindow = Class.forName("com.android.internal.policy.PhoneWindow");
                    Constructor constructor = phoneWindow.getDeclaredConstructor(Context.class);
                    mWindow.set(IBridgeObj, constructor.newInstance(activity.getApplicationContext()));
                } else {
                    Object viewRootImpl = activity.getWindow().getDecorView().getParent();
                    // 两次反射，绕过@hide反射限制
                    Field mInsetsController = HookUtils.getDeclaredField(viewRootImpl.getClass(), "mInsetsController");
                    Object mInsetsControllerObj = HookUtils.fieldGetValue(mInsetsController, viewRootImpl);

                    if (mInsetsControllerObj != null) {
                        Field mSourceConsumers = HookUtils.getDeclaredField(mInsetsControllerObj.getClass(), "mSourceConsumers");
                        Object mSourceConsumersObj = HookUtils.fieldGetValue(mSourceConsumers, mInsetsControllerObj);
                        if (mSourceConsumersObj instanceof SparseArray) {
                            SparseArray<?> sparseArray = (SparseArray<?>) mSourceConsumersObj;
                            for (int i = sparseArray.size() - 1; i >= 0; i--) {
                                int key = sparseArray.keyAt(i);
                                Object object = sparseArray.get(key);
                                // 删除三星的com.samsung.android.view.ClipBoardInsetsSourceConsumer对象
                                String samsungConsumer = "com.samsung.android.view.ClipBoardInsetsSourceConsumer";
                                if (object != null && StringUtil.isEquals(samsungConsumer, object.getClass().getName())) {
                                    sparseArray.delete(key);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 修复OPPO手机中Fragment中AppCompatEditText内存泄漏
     */
    public static void fixOPPOLeak(Fragment fragment) {
        try {
            if (Build.MANUFACTURER.equals("OPPO") && Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                if (fragment != null && fragment.getActivity() != null) {
                    Window window = fragment.getActivity().getWindow();

                    Field mDecor = HookUtils.getDeclaredField(window.getClass(), "mDecor");
                    Object mDecorObj = HookUtils.fieldGetValue(mDecor, window);

                    Class clz = Class.forName("android.view.View");
                    Field mAttachInfo = HookUtils.getDeclaredField(clz, "mAttachInfo");
                    Object mAttachInfoObj = HookUtils.fieldGetValue(mAttachInfo, mDecorObj);

                    Field mTreeObserver = mAttachInfoObj.getClass().getDeclaredField("mTreeObserver");
                    mTreeObserver.setAccessible(true);
                    Object mTreeObserverObj = mTreeObserver.get(mAttachInfoObj);

                    Field mOnTouchModeChangeListeners = mTreeObserverObj.getClass().getDeclaredField("mOnTouchModeChangeListeners");
                    mOnTouchModeChangeListeners.setAccessible(true);
                    Object mOnTouchModeChangeListenerObj = mOnTouchModeChangeListeners.get(mTreeObserverObj);

                    if (mOnTouchModeChangeListenerObj != null) {
                        CopyOnWriteArrayList<?> list = (CopyOnWriteArrayList<?>) mOnTouchModeChangeListenerObj;

                        for (int i = 0; i < list.size(); i++) {
                            Object object = list.get(i);
                            // 删除Oppo的android.widget.OppoCursorController对象
                            String oppoController = "android.widget.OppoCursorController$InsertionPointCursorController";
                            if (object != null && StringUtil.isEquals(oppoController, object.getClass().getName())) {
                                list.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }
}