package com.postfinder.alerts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class HistoryStore {
    private static final String PREFS = "post_finder_alert_history";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 500;

    private HistoryStore() {}

    public static final class Item {
        public final long timestamp;
        public final String title;
        public final String text;
        public final String link;

        Item(long timestamp, String title, String text, String link) {
            this.timestamp = timestamp;
            this.title = title == null ? "" : title;
            this.text = text == null ? "" : text;
            this.link = link == null ? "" : link;
        }
    }

    public static synchronized void add(Context context, String title, String text, String link) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray oldItems;
        try {
            oldItems = new JSONArray(sp.getString(KEY, "[]"));
        } catch (Exception ignored) {
            oldItems = new JSONArray();
        }

        JSONArray next = new JSONArray();
        JSONObject obj = new JSONObject();
        try {
            obj.put("ts", System.currentTimeMillis());
            obj.put("title", title == null ? "" : title);
            obj.put("text", text == null ? "" : text);
            obj.put("link", link == null ? "" : link);
            next.put(obj);
            for (int i = 0; i < oldItems.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject it = oldItems.optJSONObject(i);
                if (it != null) next.put(it);
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized List<Item> get(Context context) {
        ArrayList<Item> result = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                result.add(new Item(
                        o.optLong("ts", 0L),
                        o.optString("title", ""),
                        o.optString("text", ""),
                        o.optString("link", "")
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static synchronized void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
    }
}
