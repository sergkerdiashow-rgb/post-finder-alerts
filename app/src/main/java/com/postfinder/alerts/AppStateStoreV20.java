package com.postfinder.alerts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class AppStateStoreV20 {
    private static final String PREFS = "post_finder_alerts_state";
    private AppStateStoreV20() {}

    public static void recordSignal(Context context, Intent intent) {
        if (context == null || intent == null) return;
        SharedPreferences sp = prefs(context);
        sp.edit()
                .putLong("last_signal_ts", System.currentTimeMillis())
                .putLong("signal_count", sp.getLong("signal_count", 0L) + 1L)
                .putString("last_product", value(intent,"product"))
                .putString("last_source", value(intent,"source_title"))
                .putString("last_plugin_version", value(intent,"plugin_version"))
                .putInt("last_processing_ms", intent.getIntExtra("processing_ms",0))
                .putInt("last_price", intent.getIntExtra("price",0))
                .putInt("last_effective_price", intent.getIntExtra("effective_price", intent.getIntExtra("price",0)))
                .putInt("last_deal_score", intent.getIntExtra("deal_score",0))
                .putString("last_score_label", value(intent,"score_label"))
                .putString("last_watchlist", value(intent,"watchlist_match"))
                .putInt("last_expected_profit", intent.getIntExtra("expected_profit",0))
                .apply();
    }

    public static long lastSignalTs(Context c){return prefs(c).getLong("last_signal_ts",0L);} public static long signalCount(Context c){return prefs(c).getLong("signal_count",0L);}
    public static String lastProduct(Context c){return prefs(c).getString("last_product","");} public static String lastSource(Context c){return prefs(c).getString("last_source","");}
    public static String lastPluginVersion(Context c){return prefs(c).getString("last_plugin_version","");} public static int lastProcessingMs(Context c){return prefs(c).getInt("last_processing_ms",0);}
    public static int lastPrice(Context c){return prefs(c).getInt("last_price",0);} public static int lastEffectivePrice(Context c){return prefs(c).getInt("last_effective_price",0);}
    public static int lastDealScore(Context c){return prefs(c).getInt("last_deal_score",0);} public static String lastScoreLabel(Context c){return prefs(c).getString("last_score_label","");}
    public static String lastWatchlist(Context c){return prefs(c).getString("last_watchlist","");} public static int lastExpectedProfit(Context c){return prefs(c).getInt("last_expected_profit",0);}

    public static String diagnostics(Context c){return "Post Finder Alerts 2.0 diagnostics\n"+
            "last_signal_ts="+lastSignalTs(c)+"\n"+"signal_count="+signalCount(c)+"\n"+"last_product="+lastProduct(c)+"\n"+
            "last_source="+lastSource(c)+"\n"+"last_plugin_version="+lastPluginVersion(c)+"\n"+"last_processing_ms="+lastProcessingMs(c)+"\n"+
            "last_price="+lastPrice(c)+"\n"+"last_effective_price="+lastEffectivePrice(c)+"\n"+"last_deal_score="+lastDealScore(c)+"\n"+
            "last_score_label="+lastScoreLabel(c)+"\n"+"last_watchlist="+lastWatchlist(c)+"\n"+"last_expected_profit="+lastExpectedProfit(c)+"\n";}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);} private static String value(Intent i,String k){String v=i.getStringExtra(k);return v==null?"":v;}
}
