package io.taucoin.torrent.publishing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.taucoin.torrent.publishing.service.TauService;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

/*
 * The receiver for actions of foreground notification, added by service.
 */
public class NotificationReceiver extends BroadcastReceiver {
    public static final String NOTIFY_ACTION_SHUTDOWN_APP = "io.taucoin.torrent.publishing.receiver.NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;
        Intent mainIntent, serviceIntent;
        switch (action) {
            // Send action to the already running service
            case NOTIFY_ACTION_SHUTDOWN_APP:
                mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startActivity(mainIntent);

                serviceIntent = new Intent(context.getApplicationContext(), TauService.class);
                serviceIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startService(serviceIntent);
                break;
        }
    }
}
