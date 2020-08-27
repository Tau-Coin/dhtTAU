package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.receiver.BootReceiver;

import java.io.File;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
 * General utils.
 */

public class Utils {
    public static final String HASH_PATTERN = "\\b[0-9a-fA-F]{5,40}\\b";

    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
            "\u00A0-\uD7FF" +
            "\uF900-\uFDCF" +
            "\uFDF0-\uFFEF" +
            "\uD800\uDC00-\uD83F\uDFFD" +
            "\uD840\uDC00-\uD87F\uDFFD" +
            "\uD880\uDC00-\uD8BF\uDFFD" +
            "\uD8C0\uDC00-\uD8FF\uDFFD" +
            "\uD900\uDC00-\uD93F\uDFFD" +
            "\uD940\uDC00-\uD97F\uDFFD" +
            "\uD980\uDC00-\uD9BF\uDFFD" +
            "\uD9C0\uDC00-\uD9FF\uDFFD" +
            "\uDA00\uDC00-\uDA3F\uDFFD" +
            "\uDA40\uDC00-\uDA7F\uDFFD" +
            "\uDA80\uDC00-\uDABF\uDFFD" +
            "\uDAC0\uDC00-\uDAFF\uDFFD" +
            "\uDB00\uDC00-\uDB3F\uDFFD" +
            "\uDB44\uDC00-\uDB7F\uDFFD" +
            "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    public static final Pattern MAGNET_URL = Pattern.compile("("
            + "(?i:magnet):\\?"
            + "("
            + "(?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*"
            + ")?"
            + WORD_BOUNDARY
            + ")");

    public static boolean isHash(@NonNull String hash) {
        if (TextUtils.isEmpty(hash))
            return false;

        Pattern pattern = Pattern.compile(HASH_PATTERN);
        Matcher matcher = pattern.matcher(hash.trim());

        return matcher.matches();
    }

    @Nullable
    public static ClipData getClipData(@NonNull Context context) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip())
            return null;

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0)
            return null;

        return clip;
    }

    public static List<CharSequence> getClipboardText(@NonNull Context context)
    {
        ArrayList<CharSequence> clipboardText = new ArrayList<>();

        ClipData clip = Utils.getClipData(context);
        if (clip == null)
            return clipboardText;

        for (int i = 0; i < clip.getItemCount(); i++) {
            CharSequence item = clip.getItemAt(i).getText();
            if (item == null)
                continue;
            clipboardText.add(item);
        }

        return clipboardText;
    }

    public static int getAppTheme(@NonNull Context context) {
        return R.style.AppTheme;
    }
    /*
     * Migrate from Tray settings database to shared preferences.
     * TODO: delete after some releases
     */
    @Deprecated
    public static void migrateTray2SharedPreferences(@NonNull Context appContext)
    {
        final String TAG = "tray2shared";
        final String migrate_key = "tray2shared_migrated";
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(appContext);

        if (pref.getBoolean(migrate_key, false))
            return;

        File dbFile = appContext.getDatabasePath("tray.db");
        if (dbFile == null || !dbFile.exists()) {
            Log.w(TAG, "Database not found");
            pref.edit().putBoolean(migrate_key, true).apply();

            return;
        }
        SQLiteDatabase db;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't open database: " + Log.getStackTraceString(e));
            appContext.deleteDatabase("tray");
            pref.edit().putBoolean(migrate_key, true).apply();

            return;
        }
        Cursor c = db.query("TrayPreferences",
                new String[]{"KEY", "VALUE"},
                null,
                null,
                null,
                null,
                null);
        SharedPreferences.Editor edit = pref.edit();
        Log.i(TAG, "Start migrate");
        try {
            int key_i = c.getColumnIndex("KEY");
            int value_i = c.getColumnIndex("VALUE");
            while (c.moveToNext()) {
                String key = c.getString(key_i);
                String value = c.getString(value_i);

                if (value.equalsIgnoreCase("true")) {
                    edit.putBoolean(key, true);
                } else if (value.equalsIgnoreCase("false")) {
                    edit.putBoolean(key, false);
                } else {
                    try {
                        int number = Integer.parseInt(value);
                        edit.putInt(key, number);
                    } catch (NumberFormatException e) {
                        edit.putString(key, value);
                    }
                }
            }
            Log.i(TAG, "Migrate completed");

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            c.close();
            db.close();
            appContext.deleteDatabase("tray.db");
            edit.putBoolean(migrate_key, true);
            edit.apply();
        }
    }

    /*
     * Workaround for start service in Android 8+ if app no started.
     * We have a window of time to get around to calling startForeground() before we get ANR,
     * if work is longer than a millisecond but less than a few seconds.
     */

    public static void startServiceBackground(@NonNull Context context, @NonNull Intent i)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(i);
        else
            context.startService(i);
    }

    /**
     * 启动/禁止设备启动广播接收器
     * @param context 上下文
     * @param enable 是否启动
     */
    public static void enableBootReceiver(@NonNull Context context, boolean enable) {
        int flag = !enable ?
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ComponentName bootReceiver = new ComponentName(context, BootReceiver.class);
        context.getPackageManager()
                .setComponentEnabledSetting(bootReceiver, flag, PackageManager.DONT_KILL_APP);
    }

    /**
     * 启动/禁止组件（四大组件）
     * @param context 上下文
     * @param cls 组件
     * @param enable 是否启动
     */
    public static void enableComponent(@NonNull Context context,  @NonNull Class<?> cls, boolean enable){
        int flag = !enable ?
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        ComponentName componentName = new ComponentName(context, cls);
        context.getPackageManager()
                .setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
    }


    /*
     * Without additional information (e.g -DEBUG)
     */

    public static String getAppVersionNumber(@NonNull String versionName)
    {
        int index = versionName.indexOf("-");
        if (index >= 0)
            versionName = versionName.substring(0, index);

        return versionName;
    }

    /*
     * Return version components in these format: [major, minor, revision]
     */

    public static int[] getVersionComponents(@NonNull String versionName)
    {
        int[] version = new int[3];

        /* Discard additional information */
        versionName = getAppVersionNumber(versionName);

        String[] components = versionName.split("\\.");
        if (components.length < 2)
            return version;

        try {
            version[0] = Integer.parseInt(components[0]);
            version[1] = Integer.parseInt(components[1]);
            if (components.length >= 3)
                version[2] = Integer.parseInt(components[2]);

        } catch (NumberFormatException e) {
            /* Ignore */
        }

        return version;
    }

    public static String makeSha1Hash(@NonNull String s)
    {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        messageDigest.update(s.getBytes(Charset.forName("UTF-8")));
        StringBuilder sha1 = new StringBuilder();
        for (byte b : messageDigest.digest()) {
            if ((0xff & b) < 0x10)
                sha1.append("0");
            sha1.append(Integer.toHexString(0xff & b));
        }

        return sha1.toString();
    }

    public static SSLContext getSSLContext() throws GeneralSecurityException
    {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore)null);

        TrustManager[] trustManagers = tmf.getTrustManagers();
        final X509TrustManager origTrustManager = (X509TrustManager)trustManagers[0];

        TrustManager[] wrappedTrustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return origTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException
                    {
                        origTrustManager.checkClientTrusted(certs, authType);
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException
                    {
                        origTrustManager.checkServerTrusted(certs, authType);
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, wrappedTrustManagers, null);

        return sslContext;
    }

    public static void showActionModeStatusBar(@NonNull Activity activity, boolean mode)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;

        int attr = (mode ? R.attr.actionModeBackground : R.attr.statusBarColor);
        activity.getWindow().setStatusBarColor(getAttributeColor(activity, attr));
    }

    public static int getAttributeColor(@NonNull Context context, int attributeId)
    {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = context.getResources().getColor(colorRes);

        } catch (Resources.NotFoundException e) {
            return color;
        }

        return color;
    }

    /**
     * 根据groupName获取相应展示的颜色
     * @param firstLetters 对应社区首字母
     * @return 色值
     */
    public static int getGroupColor(String firstLetters) {
        Context context = MainApplication.getInstance();
        Resources res = context.getResources();
        int[] colors = res.getIntArray(R.array.group_color);
        int charCount = 0;
        if(StringUtil.isNotEmpty(firstLetters)){
            char[] chars = firstLetters.toCharArray();
            for (char aChar : chars) {
                charCount += aChar;
            }
        }
        return colors[charCount % colors.length];
    }

    /**
     * 验证URL是否合法
     * @param url URL
     * @return 是否合法
     */
    public static boolean validateUrl(String url) {
        String strRegex = "^"
                + "(([0-9]{1,3}.){3}[0-9]{1,3}" // IP形式的URL- 199.194.52.184
                + "|" // 允许IP和DOMAIN（域名）
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]." // 二级域名
                + "[a-z]{2,6})" // first level domain- .com or .museum
                + "(:[0-9]{1,4})" // 端口- :80
                + "$";
        Pattern pattern = Pattern.compile(strRegex);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    /**
     * 获取中位数数据
     */
    public static long getMedianData(List<Long> total) {
        if(total.size() > 0){
            int size = total.size();
            if(size % 2 == 1){
                return total.get((size -1 ) / 2);
            }else {
                return (long) ((total.get(size / 2 - 1) + total.get(size / 2) + 0.0) / 2);
            }
        }
        return Constants.MIN_FEE.longValue();
    }
    /**
     * 解析字符串中的link，加下划线和改成蓝色
     */
    public static SpannableStringBuilder getSpannableStringUrl(String msg) {
        SpanUtils spanUtils = new SpanUtils();
        getSpannableStringUrl(spanUtils, msg);
        return spanUtils.create();
    }

    private static void getSpannableStringUrl(SpanUtils spanUtils, String msg) {
        if(StringUtil.isNotEmpty(msg)){
            MatcherResult result = parseMatcherFormStr(msg);
            if(result != null){
                String link = result.link;
                int linkStart = result.start;
                int linkEnd = result.end;
                spanUtils.append(msg.substring(0, linkStart));
                spanUtils.append(link);
                spanUtils.setUnderline();
                int blueColor = MainApplication.getInstance().getResources().getColor(R.color.primary_dark);
                spanUtils.setForegroundColor(blueColor);
                spanUtils.append(msg.substring(linkEnd));
                return;
            }
        }
        spanUtils.append(msg);
    }

    public static String parseUrlFormStr(String msg){
        MatcherResult result = parseMatcherFormStr(msg);
        if(result != null){
            return result.link;
        }
        return null;
    }

    private static MatcherResult parseMatcherFormStr(String msg){
        Matcher magnet = MAGNET_URL.matcher(msg);
        if(magnet.find()){
            String magnetUrl = magnet.group();
            int magnetStart = magnet.start();
            int magnetEnd = magnet.end();
            if(magnetStart < magnetEnd){
                return new MatcherResult(magnetUrl, magnetStart, magnetEnd);
            }
        }
        Matcher web = Patterns.WEB_URL.matcher(msg);
        if(web.find()){
            String webLink = web.group();
            int webStart = web.start();
            int webEnd = web.end();

            if(webStart < webEnd){
                return new MatcherResult(webLink, webStart, webEnd);
            }
        }
        return null;
    }

    public static String getCommunityName(String chainID) {
        String[] splits = chainID.split(ChainParam.ChainidDelimeter);
        if(splits.length > 0){
            return splits[0];
        }
        return "";
    }

    static class MatcherResult{
        String link;
        int start;
        int end;
        MatcherResult(String link, int start, int end){
            this.link = link;
            this.start = start;
            this.end = end;
        }
    }
}