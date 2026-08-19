package com.postfinder.alerts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

public class AlertReceiverV20 extends BroadcastReceiver {
    public static final String ACTION = "com.postfinder.alerts.SHOW_ALERT";
    private static final String CHANNEL_NORMAL = "post_finder_alerts_live_v2";
    private static final String CHANNEL_PRIORITY = "post_finder_alerts_priority_v2";
    private static final String GROUP_ID = "post_finder_alerts_deals";
    private static final String UI_PREFS = "post_finder_alerts_ui";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String title = value(intent,"title"); String text=value(intent,"text"); String link=value(intent,"link");
        int notificationId=intent.getIntExtra("id",(int)(System.currentTimeMillis()&0x7fffffff));
        if(title.isEmpty())title="Post Finder WB"; if(text.isEmpty())text="Новая находка";

        AppStateStoreV20.recordSignal(context,intent);
        int score=intent.getIntExtra("deal_score",0); String watch=value(intent,"watchlist_match"); int profit=intent.getIntExtra("expected_profit",0);
        String historyId=HistoryStoreV20.add(context,title,text,link,value(intent,"product"),value(intent,"source_title"),value(intent,"post_text"),value(intent,"product_type"),
                intent.getIntExtra("price",0),intent.getIntExtra("effective_price",intent.getIntExtra("price",0)),intent.getIntExtra("reference_price",0),intent.getIntExtra("limit",0),
                intent.getIntExtra("saving",0),intent.getIntExtra("wb_extra",0),intent.getIntExtra("processing_ms",0),doubleValue(intent,"discount_percent"),
                score,value(intent,"score_label"),watch,intent.getIntExtra("price_history_count",0),intent.getIntExtra("price_history_min",0),intent.getIntExtra("price_history_median",0),
                value(intent,"rule_lines"),intent.getIntExtra("sell_price",0),intent.getIntExtra("business_cost",0),profit);

        SharedPreferences prefs=context.getSharedPreferences(UI_PREFS,Context.MODE_PRIVATE);
        if(!watch.isEmpty() && prefs.getBoolean("auto_favorite_watchlist",true)) HistoryStoreV20.toggleFavorite(context,historyId);
        if(!shouldNotify(prefs,score,!watch.isEmpty())) return;
        showNotification(context,title,text,link,notificationId,historyId,score,watch,profit);
    }

    private static boolean shouldNotify(SharedPreferences prefs,int score,boolean watch){
        int min=Math.max(0,Math.min(100,prefs.getInt("min_notify_score",0)));
        boolean watchBypass=prefs.getBoolean("watchlist_bypass_score",true);
        if(min>0 && score<min && !(watch&&watchBypass)) return false;
        if(prefs.getBoolean("quiet_mode",false)) {
            boolean priority=watch||score>=90;
            if(!priority || !prefs.getBoolean("quiet_priority_only",true)) return false;
        }
        return true;
    }

    public static void ensureChannels(Context context){
        if(Build.VERSION.SDK_INT<26)return; NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel normal=new NotificationChannel(CHANNEL_NORMAL,"Post Finder — находки",NotificationManager.IMPORTANCE_HIGH); normal.setDescription("Подходящие находки Post Finder WB");normal.enableVibration(true);normal.setLightColor(Color.rgb(105,82,244));normal.enableLights(true);nm.createNotificationChannel(normal);
        NotificationChannel priority=new NotificationChannel(CHANNEL_PRIORITY,"Post Finder — приоритетные",NotificationManager.IMPORTANCE_HIGH);priority.setDescription("Watchlist и сделки с оценкой 90+");priority.enableVibration(true);priority.setLightColor(Color.rgb(255,201,73));priority.enableLights(true);nm.createNotificationChannel(priority);
    }
    public static void ensureChannel(Context context){ensureChannels(context);}

    public static void showNotification(Context context,String title,String text,String link,int id){showNotification(context,title,text,link,id,"",0,"",0);}

    public static void showNotification(Context context,String title,String text,String link,int id,String historyId,int score,String watch,int profit){
        ensureChannels(context); NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE); boolean priority=!watch.isEmpty()||score>=90; String channel=priority?CHANNEL_PRIORITY:CHANNEL_NORMAL;
        PendingIntent sourcePi=null; if(link!=null&&!link.isEmpty()){Intent open=new Intent(Intent.ACTION_VIEW, Uri.parse(link));open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);sourcePi=PendingIntent.getActivity(context,id,open,pendingFlags());}
        Intent historyIntent=new Intent(context,MainActivityV20.class);historyIntent.putExtra("open_page",1);historyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent historyPi=PendingIntent.getActivity(context,id^0x4f31,historyIntent,pendingFlags());
        String body=text==null?"":text; if(score>0)body+=" • "+score+"/100"; if(watch!=null&&!watch.isEmpty())body+=" • 🎯 Watchlist"; if(profit>0)body+=" • прибыль ~"+profit+" ₽";
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
        b.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setWhen(System.currentTimeMillis()).setShowWhen(true).setGroup(GROUP_ID).setCategory(Notification.CATEGORY_RECOMMENDATION).setContentIntent(sourcePi!=null?sourcePi:historyPi).addAction(R.drawable.ic_notification,"История",historyPi);
        if(historyId!=null&&!historyId.isEmpty()) {
            b.addAction(R.drawable.ic_notification,"★",actionPi(context,id^0x5511,id,historyId,"favorite"));
            b.addAction(R.drawable.ic_notification,"Куплено",actionPi(context,id^0x7711,id,historyId,"bought"));
        }
        if(Build.VERSION.SDK_INT<26){b.setPriority(Notification.PRIORITY_HIGH);b.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);}nm.notify(id,b.build());
    }

    private static PendingIntent actionPi(Context c,int request,int notificationId,String historyId,String op){Intent i=new Intent(c,AlertActionReceiverV20.class);i.setAction(AlertActionReceiverV20.ACTION);i.putExtra("history_id",historyId);i.putExtra("op",op);i.putExtra("notification_id",notificationId);return PendingIntent.getBroadcast(c,request^op.hashCode(),i,pendingFlags());}
    private static int pendingFlags(){int f=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)f|=PendingIntent.FLAG_IMMUTABLE;return f;}
    private static String value(Intent i,String k){String v=i.getStringExtra(k);return v==null?"":v;}
    private static double doubleValue(Intent i,String k){try{return Double.parseDouble(value(i,k).replace(',','.'));}catch(Exception ignored){return 0.0;}}
}
