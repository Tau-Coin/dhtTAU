/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.core.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

/**
 * Description: Activity tools
 * Author:yang
 * Date: 2019/01/02
 */
public class ActivityUtil {
    private static final Logger logger = LoggerFactory.getLogger("ActivityUtil");

    public static void startActivity(FragmentActivity context, Class<?> zClass){
        Intent intent = new Intent(context, zClass);
        context.startActivity(intent);
    }

    public static void startActivityForResult(FragmentActivity context, Class<?> zClass, int requestCode){
        Intent intent = new Intent(context, zClass);
        context.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(Fragment fragment, Class<?> zClass){
        Intent intent = new Intent(fragment.getActivity(), zClass);
        fragment.startActivity(intent);
    }

    public static void startActivity(Intent intent, FragmentActivity context, Class<?> zClass){
        intent.setClass(context, zClass);
        context.startActivity(intent);
    }

    public static void startActivityForResult(Intent intent, FragmentActivity context, Class<?> zClass, int requestCode){
        intent.setClass(context, zClass);
        context.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(Intent intent, Fragment fragment, Class<?> zClass){
        intent.setClass(fragment.getActivity(), zClass);
        fragment.startActivity(intent);
    }

    public static boolean moveTaskToFront(){
        boolean isSuccess = false;
        try {
            Context context = MainApplication.getInstance();
            ActivityManager mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
            for (ActivityManager.RunningTaskInfo rti : taskList) {
                if (rti.topActivity.getPackageName().equals(context.getPackageName())) {
                    mAm.moveTaskToFront(rti.id, 0);
                    isSuccess = true;
                    break;
                }
            }
        }catch (Exception e){
            logger.error("Task switch err!", e);
        }
        return isSuccess;
    }

    public static void restartAppTask(){
        Context context = MainApplication.getInstance();
        Intent intentSplash = new Intent(context, MainActivity.class);
        intentSplash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intentSplash);
    }

    public static void openUri(Context context, String uriStr) {
        boolean isExistBrowser = true;
        try{
            Uri uri = Uri.parse(uriStr);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            if(list != null && list.size() > 0){
                context.startActivity(intent);
            }else{
                isExistBrowser = false;
            }
        }catch (Exception e){
            logger.error("No browser installed", e);
            isExistBrowser = false;
        }
        if(!isExistBrowser){
            ToastUtils.showShortToast(R.string.common_install_browser);
        }
    }

    /**
     * Status bar immersion all phone
     * */
    public static void fullScreenAll(AppCompatActivity activity) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int systemUiVisibility = decorView.getSystemUiVisibility();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        systemUiVisibility |= flags;

        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
        fullScreen(activity);
    }

    /**
     * Status bar immersion
     * */
    private static void fullScreen(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                View decorView = window.getDecorView();
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
//                window.setNavigationBarColor(Color.TRANSPARENT);
            } else {
                Window window = activity.getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
//                int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
                attributes.flags |= flagTranslucentStatus;
//                attributes.flags |= flagTranslucentNavigation;
                window.setAttributes(attributes);
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void setRequestedOrientation(AppCompatActivity activity){
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Lock display to current orientation.
     */
    public static void lockOrientation(AppCompatActivity activity) {
        if(!isFullScreen(activity)){
            return;
        }
        // Only get the orientation if it's not locked to one yet.
        // Adapted from http://stackoverflow.com/a/14565436
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int baseOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = 0;
        if (baseOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            } else {
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else if (baseOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            } else {
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
        //noinspection ResourceType
        activity.setRequestedOrientation(orientation);
    }

    public static boolean isFullScreen(AppCompatActivity activity) {
        return (activity.getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN;
    }

    /**
     * 调起系统功能发短信
     * @param activity
     * @param message 消息内容
     */
    public static  void doSendSMSTo(AppCompatActivity activity, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        intent.putExtra("sms_body", message);
        activity.startActivity(intent);
    }

    /**
     * 打开SDCard文件夹
     * @param activity
     * @param path
     */
    public static void openSdcardFolder(AppCompatActivity activity, String path){
        File file = new File(path);
        if (!file.exists()){
            return;
        }
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:" +
                    "Android%2fdata%2fio.taucoin.torrent.publishing%2flogs");
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        }else{
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file);
                intent.setDataAndType(uri, "*/*");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "*/*");
            }
        }
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            logger.error("openAssignFolder error:: {}", e.getMessage());
        }
    }

    /**
     * 分享图片
     * @param activity 上下文
     * @param path 图片路径
     * @param shareTitle 分享标题
     */
    public static void sharePic(AppCompatActivity activity, String path, String shareTitle) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/jpg");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(Intent.EXTRA_STREAM, FileUtil.getUriForFile(new File(path)));
        try {
            activity.startActivity(Intent.createChooser(intent, shareTitle));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showShortToast(R.string.app_share_error);
        }
    }

    /**
     * 分享单个文件
     * @param activity 上下文
     * @param path 文件路径
     * @param shareTitle 分享标题
     */
    public static void shareFile(AppCompatActivity activity, String path, String shareTitle) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("application/octet-stream");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(Intent.EXTRA_STREAM, FileUtil.getUriForFile(new File(path)));
        try {
            activity.startActivity(Intent.createChooser(intent, shareTitle));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showShortToast(R.string.app_share_error);
        }
    }

    /**
     * 分享多个文件
     * @param activity 上下文
     * @param list 文件列表
     * @param shareTitle 分享标题
     */
    public static void shareFiles(AppCompatActivity activity, List<File> list, String shareTitle) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("application/octet-stream");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        ArrayList<Uri> fileUris = new ArrayList<>();
        for (File file: list) {
            Uri uri = FileUtil.getUriForFile(file);
            fileUris.add(uri);
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
        try {
            activity.startActivity(Intent.createChooser(intent, shareTitle));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showShortToast(R.string.app_share_error);
        }
    }

    /**
     * 分享文本
     * @param activity
     * @param title
     * @param text
     */
    public static void shareText(AppCompatActivity activity, String title, String text) {
        shareText(activity, title, text, null);
    }

    /**
     * 分享文本
     * @param activity
     * @param title
     * @param text
     * @param subject
     */
    private static void shareText(AppCompatActivity activity, String title,
                                 String text, String subject) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if(StringUtil.isNotEmpty(subject)){
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        }
        try {
            activity.startActivity(Intent.createChooser(intent, title));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showShortToast(R.string.app_share_error);
        }
    }
}