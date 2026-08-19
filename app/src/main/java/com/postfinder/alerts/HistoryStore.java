package com.postfinder.alerts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        public final String product;
        public final String sourceTitle;
        public final String postText;
        public final String productType;
        public final int price;
        public final int effectivePrice;
        public final int referencePrice;
        public final int limit;
        public final int saving;
        public final int wbExtra;
        public final int processingMs;
        public final double discountPercent;

        Item(long timestamp, String title, String text, String link,
             String product, String sourceTitle, String postText, String productType,
             int price, int effectivePrice, int referencePrice, int limit,
             int saving, int wbExtra, int processingMs, double discountPercent) {
            this.timestamp = timestamp;
            this.title = safe(title);
            this.text = safe(text);
            this.link = safe(link);
            this.product = safe(product);
            this.sourceTitle = safe(sourceTitle);
            this.postText = safe(postText);
            this.productType = safe(productType);
            this.price = price;
            this.effectivePrice = effectivePrice;
            this.referencePrice = referencePrice;
            this.limit = limit;
            this.saving = saving;
            this.wbExtra = wbExtra;
            this.processingMs = processingMs;
            this.discountPercent = discountPercent;
        }

        public String displayTitle() {
            return product.isEmpty() ? (title.isEmpty() ? "Новая находка" : title) : product;
        }

        public boolean isWildberries() {
            return wbExtra > 0 || postText.toLowerCase(Locale.ROOT).contains("wildberries.ru/catalog/");
        }

        public String searchableText() {
            return (displayTitle() + " " + sourceTitle + " " + text + " " + postText).toLowerCase(Locale.ROOT);
        }
    }

    public static synchronized void add(
            Context context, String title, String text, String link,
            String product, String sourceTitle, String postText, String productType,
            int price, int effectivePrice, int referencePrice, int limit,
            int saving, int wbExtra, int processingMs, double discountPercent) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray oldItems;
        try {
            oldItems = new JSONArray(sp.getString(KEY, "[]"));
        } catch (Exception ignored) {
            oldItems = new JSONArray();
        }

        JSONArray next = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.put("ts", System.currentTimeMillis());
            obj.put("title", safe(title));
            obj.put("text", safe(text));
            obj.put("link", safe(link));
            obj.put("product", safe(product));
            obj.put("source_title", safe(sourceTitle));
            obj.put("post_text", safe(postText));
            obj.put("product_type", safe(productType));
            obj.put("price", price);
            obj.put("effective_price", effectivePrice);
            obj.put("reference_price", referencePrice);
            obj.put("limit", limit);
            obj.put("saving", saving);
            obj.put("wb_extra", wbExtra);
            obj.put("processing_ms", processingMs);
            obj.put("discount_percent", discountPercent);
            next.put(obj);
            for (int i = 0; i < oldItems.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject it = oldItems.optJSONObject(i);
                if (it != null) next.put(it);
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized void add(Context context, String title, String text, String link) {
        add(context, title, text, link, "", "", "", "", 0, 0, 0, 0, 0, 0, 0, 0.0);
    }

    public static synchronized List<Item> get(Context context) {
        ArrayList<Item> result = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                int price = o.optInt("price", 0);
                int effective = o.optInt("effective_price", price);
                result.add(new Item(
                        o.optLong("ts", 0L),
                        o.optString("title", ""),
                        o.optString("text", ""),
                        o.optString("link", ""),
                        o.optString("product", ""),
                        o.optString("source_title", ""),
                        o.optString("post_text", ""),
                        o.optString("product_type", ""),
                        price,
                        effective,
                        o.optInt("reference_price", 0),
                        o.optInt("limit", 0),
                        o.optInt("saving", 0),
                        o.optInt("wb_extra", 0),
                        o.optInt("processing_ms", 0),
                        o.optDouble("discount_percent", 0.0)
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
