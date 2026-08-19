package com.postfinder.alerts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class AppStateStore {
    private static final String PREFS = "post_finder_alerts_state";

    private AppStateStore() {}

    public static void recordSignal(Context context, Intent intent) {
        if (context == null || intent == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putLong("last_signal_ts", System.currentTimeMillis())
                .putLong("signal_count", sp.getLong("signal_count", 0L) + 1L)
                .putString("last_product", value(intent, "product"))
                .putString("last_source", value(intent, "source_title"))
                .putString("last_plugin_version", value(intent, "plugin_version"))
                .putInt("last_processing_ms", intent.getIntExtra("processing_ms", 0))
                .putInt("last_price", intent.getIntExtra("price", 0))
                .putInt("last_effective_price", intent.getIntExtra("effective_price", intent.getIntExtra("price", 0)))
                .apply();
    }

    public static long lastSignalTs(Context context) { return prefs(context).getLong("last_signal_ts", 0L); }
    public static long signalCount(Context context) { return prefs(context).getLong("signal_count", 0L); }
    public static String lastProduct(Context context) { return prefs(context).getString("last_product", ""); }
    public static String lastSource(Context context) { return prefs(context).getString("last_source", ""); }
    public static String lastPluginVersion(Context context) { return prefs(context).getString("last_plugin_version", ""); }
    public static int lastProcessingMs(Context context) { return prefs(context).getInt("last_processing_ms", 0); }
    public static int lastPrice(Context context) { return prefs(context).getInt("last_price", 0); }
    public static int lastEffectivePrice(Context context) { return prefs(context).getInt("last_effective_price", 0); }

    public static String diagnostics(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Post Finder Alerts diagnostics\n");
        sb.append("last_signal_ts=").append(lastSignalTs(context)).append('\n');
        sb.append("signal_count=").append(signalCount(context)).append('\n');
        sb.append("last_product=").append(lastProduct(context)).append('\n');
        sb.append("last_source=").append(lastSource(context)).append('\n');
        sb.append("last_plugin_version=").append(lastPluginVersion(context)).append('\n');
        sb.append("last_processing_ms=").append(lastProcessingMs(context)).append('\n');
        sb.append("last_price=").append(lastPrice(context)).append('\n');
        sb.append("last_effective_price=").append(lastEffectivePrice(context)).append('\n');
        return sb.toString();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String value(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value == null ? "" : value;
    }
}
