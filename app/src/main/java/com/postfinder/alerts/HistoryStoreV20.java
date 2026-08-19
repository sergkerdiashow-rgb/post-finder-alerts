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

public final class HistoryStoreV20 {
    private static final String PREFS = "post_finder_alert_history";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 1500;

    private HistoryStoreV20() {}

    public static final class Item {
        public final String id;
        public final long timestamp;
        public final String title, text, link, product, sourceTitle, postText, productType;
        public final int price, effectivePrice, referencePrice, limit, saving, wbExtra, processingMs;
        public final double discountPercent;
        public final boolean favorite, read;
        public final int dealScore;
        public final String scoreLabel, watchlistMatch, ruleLines;
        public final int priceHistoryCount, priceHistoryMin, priceHistoryMedian;
        public final int sellPrice, businessCost, expectedProfit;
        public final String status, note;

        Item(String id, long timestamp, String title, String text, String link,
             String product, String sourceTitle, String postText, String productType,
             int price, int effectivePrice, int referencePrice, int limit,
             int saving, int wbExtra, int processingMs, double discountPercent,
             boolean favorite, boolean read, int dealScore, String scoreLabel,
             String watchlistMatch, int priceHistoryCount, int priceHistoryMin,
             int priceHistoryMedian, String ruleLines, int sellPrice, int businessCost,
             int expectedProfit, String status, String note) {
            this.id = safe(id); this.timestamp = timestamp; this.title = safe(title); this.text = safe(text); this.link = safe(link);
            this.product = safe(product); this.sourceTitle = safe(sourceTitle); this.postText = safe(postText); this.productType = safe(productType);
            this.price = price; this.effectivePrice = effectivePrice; this.referencePrice = referencePrice; this.limit = limit;
            this.saving = saving; this.wbExtra = wbExtra; this.processingMs = processingMs; this.discountPercent = discountPercent;
            this.favorite = favorite; this.read = read; this.dealScore = dealScore; this.scoreLabel = safe(scoreLabel);
            this.watchlistMatch = safe(watchlistMatch); this.priceHistoryCount = priceHistoryCount; this.priceHistoryMin = priceHistoryMin;
            this.priceHistoryMedian = priceHistoryMedian; this.ruleLines = safe(ruleLines); this.sellPrice = sellPrice;
            this.businessCost = businessCost; this.expectedProfit = expectedProfit; this.status = normalizeStatus(status); this.note = safe(note);
        }

        public String displayTitle() { return product.isEmpty() ? (title.isEmpty() ? "Новая находка" : title) : product; }
        public boolean isWildberries() {
            String hay = (postText + " " + text + " " + link).toLowerCase(Locale.ROOT);
            return wbExtra > 0 || hay.contains("wildberries.ru/catalog/");
        }
        public boolean isWatchlist() { return !watchlistMatch.isEmpty(); }
        public boolean isPriority() { return dealScore >= 90 || isWatchlist(); }
        public boolean isBought() { return "bought".equals(status); }
        public boolean isIgnored() { return "ignored".equals(status); }
        public String searchableText() {
            return (displayTitle() + " " + sourceTitle + " " + text + " " + postText + " " + productType + " " + note + " " + scoreLabel + " " + watchlistMatch)
                    .toLowerCase(Locale.ROOT);
        }
    }

    public static synchronized String add(Context context, String title, String text, String link,
            String product, String sourceTitle, String postText, String productType,
            int price, int effectivePrice, int referencePrice, int limit,
            int saving, int wbExtra, int processingMs, double discountPercent,
            int dealScore, String scoreLabel, String watchlistMatch,
            int priceHistoryCount, int priceHistoryMin, int priceHistoryMedian,
            String ruleLines, int sellPrice, int businessCost, int expectedProfit) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray oldItems = readArray(sp); JSONArray next = new JSONArray(); long now = System.currentTimeMillis();
        String id = makeId(now, product, sourceTitle, link, text);
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id); obj.put("ts", now); obj.put("title", safe(title)); obj.put("text", safe(text)); obj.put("link", safe(link));
            obj.put("product", safe(product)); obj.put("source_title", safe(sourceTitle)); obj.put("post_text", safe(postText)); obj.put("product_type", safe(productType));
            obj.put("price", price); obj.put("effective_price", effectivePrice); obj.put("reference_price", referencePrice); obj.put("limit", limit);
            obj.put("saving", saving); obj.put("wb_extra", wbExtra); obj.put("processing_ms", processingMs); obj.put("discount_percent", discountPercent);
            obj.put("deal_score", dealScore); obj.put("score_label", safe(scoreLabel)); obj.put("watchlist_match", safe(watchlistMatch));
            obj.put("price_history_count", priceHistoryCount); obj.put("price_history_min", priceHistoryMin); obj.put("price_history_median", priceHistoryMedian);
            obj.put("rule_lines", safe(ruleLines)); obj.put("sell_price", sellPrice); obj.put("business_cost", businessCost); obj.put("expected_profit", expectedProfit);
            obj.put("favorite", false); obj.put("read", false); obj.put("status", "new"); obj.put("note", "");
            next.put(obj);
            for (int i=0; i<oldItems.length() && next.length()<MAX_ITEMS; i++) { JSONObject it=oldItems.optJSONObject(i); if (it!=null) next.put(it); }
        } catch (Exception ignored) {}
        sp.edit().putString(KEY, next.toString()).apply();
        return id;
    }

    public static synchronized String add(Context context, String title, String text, String link) {
        return add(context,title,text,link,"","","","",0,0,0,0,0,0,0,0.0,0,"","",0,0,0,"",0,0,0);
    }

    public static synchronized List<Item> get(Context context) {
        ArrayList<Item> result = new ArrayList<>(); SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE); JSONArray arr=readArray(sp); boolean migrated=false;
        for (int i=0;i<arr.length();i++) {
            JSONObject o=arr.optJSONObject(i); if (o==null) continue; long ts=o.optLong("ts",0L); String id=o.optString("id","");
            if (id.isEmpty()) { id=makeId(ts,o.optString("product",""),o.optString("source_title",""),o.optString("link",""),o.optString("text","")); try{o.put("id",id);migrated=true;}catch(Exception ignored){} }
            int price=o.optInt("price",0), effective=o.optInt("effective_price",price);
            result.add(new Item(id,ts,o.optString("title",""),o.optString("text",""),o.optString("link",""),o.optString("product",""),o.optString("source_title",""),o.optString("post_text",""),o.optString("product_type",""),
                    price,effective,o.optInt("reference_price",0),o.optInt("limit",0),o.optInt("saving",0),o.optInt("wb_extra",0),o.optInt("processing_ms",0),o.optDouble("discount_percent",0.0),
                    o.optBoolean("favorite",false),o.optBoolean("read",false),o.optInt("deal_score",0),o.optString("score_label",""),o.optString("watchlist_match",""),
                    o.optInt("price_history_count",0),o.optInt("price_history_min",0),o.optInt("price_history_median",0),o.optString("rule_lines",""),o.optInt("sell_price",0),o.optInt("business_cost",0),o.optInt("expected_profit",0),o.optString("status","new"),o.optString("note","")));
        }
        if (migrated) sp.edit().putString(KEY,arr.toString()).apply();
        return result;
    }

    public static synchronized Item find(Context context,String id){ if(id==null||id.isEmpty())return null; for(Item item:get(context))if(id.equals(item.id))return item; return null; }
    public static synchronized boolean toggleFavorite(Context context,String id){ return updateBoolean(context,id,"favorite",null); }
    public static synchronized boolean markRead(Context context,String id,boolean value){ return updateBoolean(context,id,"read",value); }
    public static synchronized void setStatus(Context context,String id,String status){ updateString(context,id,"status",normalizeStatus(status)); }
    public static synchronized void setNote(Context context,String id,String note){ updateString(context,id,"note",safe(note)); }

    public static synchronized void markAllRead(Context context){
        SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE); JSONArray arr=readArray(sp);
        for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null)try{o.put("read",true);}catch(Exception ignored){}}
        sp.edit().putString(KEY,arr.toString()).apply();
    }
    public static synchronized void delete(Context context,String id){
        SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray arr=readArray(sp),next=new JSONArray();
        for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null&&!safe(id).equals(o.optString("id","")))next.put(o);}sp.edit().putString(KEY,next.toString()).apply();
    }
    public static synchronized void clear(Context context){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply();}

    public static synchronized String exportJson(Context context){
        try{JSONObject root=new JSONObject();root.put("schema",2);root.put("app","Post Finder Alerts");root.put("exported_at",System.currentTimeMillis());root.put("items",readArray(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)));return root.toString(2);}catch(Exception e){return "{\"schema\":2,\"items\":[]}";}
    }

    public static synchronized int importJson(Context context,String raw) throws Exception{
        if(raw==null)throw new IllegalArgumentException("empty");String trimmed=raw.trim();JSONArray incoming=trimmed.startsWith("[")?new JSONArray(trimmed):new JSONObject(trimmed).optJSONArray("items");if(incoming==null)throw new IllegalArgumentException("items");
        SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray current=readArray(sp);Set<String> ids=new HashSet<>();for(int i=0;i<current.length();i++){JSONObject o=current.optJSONObject(i);if(o!=null)ids.add(normalizedId(o));}
        JSONArray merged=new JSONArray();int imported=0;for(int i=0;i<incoming.length()&&merged.length()<MAX_ITEMS;i++){JSONObject o=incoming.optJSONObject(i);if(o==null)continue;String id=normalizedId(o);if(ids.contains(id))continue;ids.add(id);merged.put(o);imported++;}for(int i=0;i<current.length()&&merged.length()<MAX_ITEMS;i++){JSONObject o=current.optJSONObject(i);if(o!=null)merged.put(o);}sp.edit().putString(KEY,merged.toString()).apply();return imported;
    }

    private static String normalizedId(JSONObject o){String id=o.optString("id","");if(!id.isEmpty())return id;long ts=o.optLong("ts",0L);id=makeId(ts,o.optString("product",""),o.optString("source_title",""),o.optString("link",""),o.optString("text",""));try{o.put("id",id);}catch(Exception ignored){}return id;}
    private static boolean updateBoolean(Context context,String id,String key,Boolean forced){SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray arr=readArray(sp);boolean result=false;for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null||!safe(id).equals(o.optString("id","")))continue;boolean value=forced!=null?forced:!o.optBoolean(key,false);try{o.put(key,value);}catch(Exception ignored){}result=value;break;}sp.edit().putString(KEY,arr.toString()).apply();return result;}
    private static void updateString(Context context,String id,String key,String value){SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);JSONArray arr=readArray(sp);for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null||!safe(id).equals(o.optString("id","")))continue;try{o.put(key,safe(value));}catch(Exception ignored){}break;}sp.edit().putString(KEY,arr.toString()).apply();}
    private static JSONArray readArray(SharedPreferences sp){try{return new JSONArray(sp.getString(KEY,"[]"));}catch(Exception ignored){return new JSONArray();}}
    private static String makeId(long ts,String product,String source,String link,String text){String raw=ts+"|"+safe(product)+"|"+safe(source)+"|"+safe(link)+"|"+safe(text);return Long.toHexString(ts)+"-"+Integer.toHexString(raw.hashCode());}
    private static String normalizeStatus(String s){String v=safe(s).toLowerCase(Locale.ROOT);if(v.equals("bought")||v.equals("ignored"))return v;return "new";}
    private static String safe(String value){return value==null?"":value;}
}
