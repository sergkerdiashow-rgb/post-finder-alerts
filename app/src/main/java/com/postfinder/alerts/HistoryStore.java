package com.postfinder.alerts;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HistoryStore {
    private static final String PREFS = "post_finder_alert_history";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 1000;

    private HistoryStore() {}

    public static final class Item {
        public final String id;
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
        public final boolean favorite;
        public final boolean read;

        Item(String id, long timestamp, String title, String text, String link,
             String product, String sourceTitle, String postText, String productType,
             int price, int effectivePrice, int referencePrice, int limit,
             int saving, int wbExtra, int processingMs, double discountPercent,
             boolean favorite, boolean read) {
            this.id = safe(id); this.timestamp = timestamp; this.title = safe(title); this.text = safe(text); this.link = safe(link);
            this.product = safe(product); this.sourceTitle = safe(sourceTitle); this.postText = safe(postText); this.productType = safe(productType);
            this.price = price; this.effectivePrice = effectivePrice; this.referencePrice = referencePrice; this.limit = limit;
            this.saving = saving; this.wbExtra = wbExtra; this.processingMs = processingMs; this.discountPercent = discountPercent;
            this.favorite = favorite; this.read = read;
        }

        public String displayTitle() { return product.isEmpty() ? (title.isEmpty() ? "Новая находка" : title) : product; }
        public boolean isWildberries() { String hay = (postText + " " + text + " " + link).toLowerCase(Locale.ROOT); return wbExtra > 0 || hay.contains("wildberries.ru/catalog/"); }
        public String searchableText() { return (displayTitle() + " " + sourceTitle + " " + text + " " + postText + " " + productType).toLowerCase(Locale.ROOT); }
    }

    public static synchronized void add(Context context, String title, String text, String link, String product, String sourceTitle, String postText, String productType, int price, int effectivePrice, int referencePrice, int limit, int saving, int wbExtra, int processingMs, double discountPercent) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray oldItems = readArray(sp); JSONArray next = new JSONArray(); long now = System.currentTimeMillis();
        try {
            JSONObject obj = new JSONObject(); obj.put("id", makeId(now, product, sourceTitle, link, text)); obj.put("ts", now); obj.put("title", safe(title)); obj.put("text", safe(text)); obj.put("link", safe(link)); obj.put("product", safe(product)); obj.put("source_title", safe(sourceTitle)); obj.put("post_text", safe(postText)); obj.put("product_type", safe(productType)); obj.put("price", price); obj.put("effective_price", effectivePrice); obj.put("reference_price", referencePrice); obj.put("limit", limit); obj.put("saving", saving); obj.put("wb_extra", wbExtra); obj.put("processing_ms", processingMs); obj.put("discount_percent", discountPercent); obj.put("favorite", false); obj.put("read", false); next.put(obj);
            for (int i = 0; i < oldItems.length() && next.length() < MAX_ITEMS; i++) { JSONObject it = oldItems.optJSONObject(i); if (it != null) next.put(it); }
        } catch (Exception ignored) {}
        sp.edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized void add(Context context, String title, String text, String link) { add(context, title, text, link, "", "", "", "", 0, 0, 0, 0, 0, 0, 0, 0.0); }

    public static synchronized List<Item> get(Context context) {
        ArrayList<Item> result = new ArrayList<>(); SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray arr = readArray(sp); boolean migrated = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i); if (o == null) continue; long ts = o.optLong("ts", 0L); String id = o.optString("id", "");
            if (id.isEmpty()) { id = makeId(ts, o.optString("product", ""), o.optString("source_title", ""), o.optString("link", ""), o.optString("text", "")); try { o.put("id", id); migrated = true; } catch (Exception ignored) {} }
            int price = o.optInt("price", 0); int effective = o.optInt("effective_price", price);
            result.add(new Item(id, ts, o.optString("title", ""), o.optString("text", ""), o.optString("link", ""), o.optString("product", ""), o.optString("source_title", ""), o.optString("post_text", ""), o.optString("product_type", ""), price, effective, o.optInt("reference_price", 0), o.optInt("limit", 0), o.optInt("saving", 0), o.optInt("wb_extra", 0), o.optInt("processing_ms", 0), o.optDouble("discount_percent", 0.0), o.optBoolean("favorite", false), o.optBoolean("read", false)));
        }
        if (migrated) sp.edit().putString(KEY, arr.toString()).apply(); return result;
    }

    public static synchronized Item find(Context context, String id) { if (id == null || id.isEmpty()) return null; for (Item item : get(context)) if (id.equals(item.id)) return item; return null; }
    public static synchronized boolean toggleFavorite(Context context, String id) { return updateBoolean(context, id, "favorite", null); }
    public static synchronized boolean markRead(Context context, String id, boolean value) { return updateBoolean(context, id, "read", value); }

    public static synchronized void markAllRead(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray arr = readArray(sp);
        for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); if (o == null) continue; try { o.put("read", true); } catch (Exception ignored) {} }
        sp.edit().putString(KEY, arr.toString()).apply();
    }

    public static synchronized void delete(Context context, String id) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray arr = readArray(sp); JSONArray next = new JSONArray();
        for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); if (o != null && !safe(id).equals(o.optString("id", ""))) next.put(o); }
        sp.edit().putString(KEY, next.toString()).apply();
    }

    public static synchronized void clear(Context context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply(); }

    public static synchronized String exportJson(Context context) {
        try { JSONObject root = new JSONObject(); root.put("schema", 1); root.put("app", "Post Finder Alerts"); root.put("exported_at", System.currentTimeMillis()); root.put("items", readArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))); return root.toString(2); }
        catch (Exception e) { return "{\"schema\":1,\"items\":[]}"; }
    }

    public static synchronized int importJson(Context context, String raw) throws Exception {
        if (raw == null) throw new IllegalArgumentException("empty"); String trimmed = raw.trim(); JSONArray incoming = trimmed.startsWith("[") ? new JSONArray(trimmed) : new JSONObject(trimmed).optJSONArray("items"); if (incoming == null) throw new IllegalArgumentException("items");
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray current = readArray(sp); Set<String> ids = new HashSet<>();
        for (int i = 0; i < current.length(); i++) { JSONObject o = current.optJSONObject(i); if (o != null) ids.add(normalizedId(o)); }
        JSONArray merged = new JSONArray(); int imported = 0;
        for (int i = 0; i < incoming.length() && merged.length() < MAX_ITEMS; i++) { JSONObject o = incoming.optJSONObject(i); if (o == null) continue; String id = normalizedId(o); if (ids.contains(id)) continue; ids.add(id); merged.put(o); imported++; }
        for (int i = 0; i < current.length() && merged.length() < MAX_ITEMS; i++) { JSONObject o = current.optJSONObject(i); if (o != null) merged.put(o); }
        sp.edit().putString(KEY, merged.toString()).apply(); return imported;
    }

    private static String normalizedId(JSONObject o) { String id = o.optString("id", ""); if (!id.isEmpty()) return id; long ts = o.optLong("ts", 0L); id = makeId(ts, o.optString("product", ""), o.optString("source_title", ""), o.optString("link", ""), o.optString("text", "")); try { o.put("id", id); } catch (Exception ignored) {} return id; }
    private static boolean updateBoolean(Context context, String id, String key, Boolean forced) { SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); JSONArray arr = readArray(sp); boolean result = false; for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); if (o == null || !safe(id).equals(o.optString("id", ""))) continue; boolean value = forced != null ? forced : !o.optBoolean(key, false); try { o.put(key, value); } catch (Exception ignored) {} result = value; break; } sp.edit().putString(KEY, arr.toString()).apply(); return result; }
    private static JSONArray readArray(SharedPreferences sp) { try { return new JSONArray(sp.getString(KEY, "[]")); } catch (Exception ignored) { return new JSONArray(); } }
    private static String makeId(long ts, String product, String source, String link, String text) { String raw = ts + "|" + safe(product) + "|" + safe(source) + "|" + safe(link) + "|" + safe(text); return Long.toHexString(ts) + "-" + Integer.toHexString(raw.hashCode()); }
    private static String safe(String value) { return value == null ? "" : value; }
}
