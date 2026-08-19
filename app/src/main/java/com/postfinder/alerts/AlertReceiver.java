package com.postfinder.alerts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

public class AlertReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.postfinder.alerts.SHOW_ALERT";
    public static final String CHANNEL_ID = "post_finder_alerts_live_v1";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        String link = intent.getStringExtra("link");
        int id = intent.getIntExtra("id", (int)(System.currentTimeMillis() & 0x7fffffff));
        showNotification(context,
                title == null || title.isEmpty() ? "🔥 Post Finder WB" : title,
                text == null ? "Новая находка" : text,
                link == null ? "" : link,
                id);
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Post Finder — новые находки", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Только новые находки Post Finder WB");
        ch.enableVibration(true);
        ch.setLightColor(Color.MAGENTA);
        ch.enableLights(true);
        nm.createNotificationChannel(ch);
    }

    public static void showNotification(Context context, String title, String text, String link, int id) {
        ensureChannel(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent pi = null;
        if (link != null && !link.isEmpty()) {
            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            pi = PendingIntent.getActivity(context, id, open, flags);
        }

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        if (pi != null) b.setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH);
            b.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        }
        nm.notify(id, b.build());
    }
}
