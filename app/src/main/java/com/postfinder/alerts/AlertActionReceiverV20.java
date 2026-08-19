package com.postfinder.alerts;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlertActionReceiverV20 extends BroadcastReceiver {
    public static final String ACTION = "com.postfinder.alerts.LOCAL_ACTION_V20";
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String id = intent.getStringExtra("history_id");
        String op = intent.getStringExtra("op");
        int notificationId = intent.getIntExtra("notification_id", -1);
        if (id == null || id.isEmpty() || op == null) return;
        if ("favorite".equals(op)) HistoryStoreV20.toggleFavorite(context, id);
        else if ("bought".equals(op)) { HistoryStoreV20.setStatus(context, id, "bought"); HistoryStoreV20.markRead(context, id, true); }
        else if ("ignored".equals(op)) { HistoryStoreV20.setStatus(context, id, "ignored"); HistoryStoreV20.markRead(context, id, true); }
        else if ("read".equals(op)) HistoryStoreV20.markRead(context, id, true);
        if (notificationId >= 0) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);
        }
    }
}
