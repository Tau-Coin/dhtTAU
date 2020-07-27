package io.taucoin.torrent.publishing.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

/*
 * Helper of showing notifications.
 */
public class TauNotifier {
    private static final String TAG = TauNotifier.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    public static final String FOREGROUND_NOTIFY_CHAN_ID = "io.taucoin.torrent.publishing.FOREGROUND_NOTIFY_CHAN";
    public static final String DEFAULT_NOTIFY_CHAN_ID = "io.taucoin.torrent.publishing.DEFAULT_NOTIFY_CHAN_ID";

    // 服务启动的通知ID
    private static final int SERVICE_STARTED_NOTIFICATION_ID = -1;

    private static volatile TauNotifier INSTANCE;

    private Context appContext;
    private NotificationManager notifyManager;

    public static TauNotifier getInstance(@NonNull Context appContext) {
        if (INSTANCE == null) {
            synchronized (TauNotifier.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauNotifier(appContext);
            }
        }
        return INSTANCE;
    }

    private TauNotifier(Context appContext) {
        this.appContext = appContext;
        notifyManager = (NotificationManager)appContext.getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 创建通知渠道
     */
    public void makeNotifyChans() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            return;
        }
        ArrayList<NotificationChannel> channels = new ArrayList<>();

        channels.add(new NotificationChannel(DEFAULT_NOTIFY_CHAN_ID,
                appContext.getString(R.string.def),
                NotificationManager.IMPORTANCE_DEFAULT));
        NotificationChannel foregroundChan = new NotificationChannel(FOREGROUND_NOTIFY_CHAN_ID,
                appContext.getString(R.string.foreground_notification),
                NotificationManager.IMPORTANCE_LOW);
        foregroundChan.setShowBadge(false);
        channels.add(foregroundChan);

        notifyManager.createNotificationChannels(channels);
    }

    /**
     * 创建前台通知
     * @param service 对应的Service
     */
    public static void makeForegroundNotify(Service service) {
        /* For starting main activity after click */
        Context context = service.getApplicationContext();
        Intent startupIntent = new Intent(context, MainActivity.class);
        startupIntent.setAction(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent startupPendingIntent = PendingIntent.getActivity(context, 0, startupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder foregroundNotify = new NotificationCompat.Builder(context, FOREGROUND_NOTIFY_CHAN_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(startupPendingIntent)
                .setContentTitle(context.getString(R.string.app_running_in_the_background))
                .setTicker(context.getString(R.string.app_running_in_the_background))
                .setWhen(System.currentTimeMillis());

        foregroundNotify.addAction(makeShutdownAction(service));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            foregroundNotify.setCategory(Notification.CATEGORY_SERVICE);
        }

        /* Disallow killing the service process by system */
        service.startForeground(SERVICE_STARTED_NOTIFICATION_ID, foregroundNotify.build());
    }

    /**
     * 创建关闭动作
     * @param service 对应的Service
     */
    private static NotificationCompat.Action makeShutdownAction(Service service) {
        Context context = service.getApplicationContext();
        Intent shutdownIntent = new Intent(context, NotificationReceiver.class);
        shutdownIntent.setAction(NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP);
        PendingIntent shutdownPendingIntent = PendingIntent.getBroadcast(context, 0,
                shutdownIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action.Builder(
                R.mipmap.ic_power_settings_new_white_24dp,
                context.getString(R.string.app_shutdown),
                shutdownPendingIntent)
                .build();
    }
}
