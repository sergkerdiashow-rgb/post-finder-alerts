package com.postfinder.alerts;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 12, 17);
    private static final int CARD = Color.rgb(23, 26, 34);
    private static final int CARD_2 = Color.rgb(29, 33, 43);
    private static final int TEXT = Color.rgb(246, 247, 251);
    private static final int MUTED = Color.rgb(160, 168, 184);
    private static final int ACCENT = Color.rgb(139, 92, 246);
    private static final int GREEN = Color.rgb(77, 222, 128);
    private static final int RED = Color.rgb(255, 107, 107);

    private TextView statusText;
    private TextView statusSub;
    private TextView totalValue;
    private TextView lastValue;
    private TextView emptyView;
    private TextView allChip;
    private TextView todayChip;
    private ListView historyList;
    private HistoryAdapter adapter;
    private boolean todayOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertReceiver.ensureChannel(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), dp(18), dp(20), dp(12));

        root.addView(buildHeader());
        root.addView(space(16));
        root.addView(buildStatusCard());
        root.addView(space(12));
        root.addView(buildStatsRow());
        root.addView(space(14));
        root.addView(buildActionsRow());
        root.addView(space(20));
        root.addView(buildHistoryHeader());
        root.addView(space(10));
        root.addView(buildFilterRow());
        root.addView(space(10));

        historyList = new ListView(this);
        historyList.setDivider(null);
        historyList.setDividerHeight(0);
        historyList.setSelector(android.R.color.transparent);
        historyList.setVerticalScrollBarEnabled(false);
        historyList.setClipToPadding(false);
        historyList.setPadding(0, 0, 0, dp(12));
        adapter = new HistoryAdapter(this);
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener((parent, view, position, id) -> {
            HistoryStore.Item item = adapter.getItem(position);
            if (item == null || item.link.isEmpty()) {
                Toast.makeText(this, "Для этой находки нет ссылки", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(item.link));
                startActivity(open);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
            }
        });

        emptyView = label("Пока ничего не приходило\nНовые находки появятся здесь автоматически", 15, MUTED, Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(52), dp(20), dp(20));

        LinearLayout historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        historyBox.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        historyBox.addView(historyList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(historyBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        requestNotifications();
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_pf_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(54), dp(54));
        lpLogo.setMarginEnd(dp(14));
        row.addView(logo, lpLogo);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("Post Finder Alerts", 25, TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView sub = label("Мгновенные находки • отдельные уведомления", 13, MUTED, Gravity.START);
        sub.setPadding(0, dp(3), 0, 0);
        text.addView(title);
        text.addView(sub);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View buildStatusCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(strokeBg(CARD, dp(16), ACCENT, 1));

        statusText = label("Проверяем уведомления…", 17, TEXT, Gravity.START);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusSub = label("", 13, MUTED, Gravity.START);
        statusSub.setPadding(0, dp(5), 0, 0);
        card.addView(statusText);
        card.addView(statusSub);
        return card;
    }

    private View buildStatsRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout total = statCard("НАХОДОК", "0");
        totalValue = (TextView) total.getChildAt(1);
        LinearLayout last = statCard("ПОСЛЕДНЯЯ", "—");
        lastValue = (TextView) last.getChildAt(1);

        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        a.setMarginEnd(dp(6));
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b.setMarginStart(dp(6));
        row.addView(total, a);
        row.addView(last, b);
        return row;
    }

    private LinearLayout statCard(String name, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(13), dp(15), dp(13));
        box.setBackground(bg(CARD, dp(15)));
        TextView nameView = label(name, 11, MUTED, Gravity.START);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        TextView valueView = label(value, 20, TEXT, Gravity.START);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setPadding(0, dp(4), 0, 0);
        box.addView(nameView);
        box.addView(valueView);
        return box;
    }

    private View buildActionsRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView test = actionButton("⚡  Тест", true);
        test.setOnClickListener(v -> AlertReceiver.showNotification(
                this,
                "🔥 Post Finder Alerts работает",
                "Тест отдельного уведомления. exteraGram может быть заблокирован.",
                "",
                1001));
        TextView settings = actionButton("⚙  Настройки", false);
        settings.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });

        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(48), 1f);
        a.setMarginEnd(dp(6));
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(48), 1f);
        b.setMarginStart(dp(6));
        row.addView(test, a);
        row.addView(settings, b);
        return row;
    }

    private View buildHistoryHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("История находок", 19, TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = label("Очистить", 13, MUTED, Gravity.CENTER);
        clear.setPadding(dp(12), dp(8), dp(12), dp(8));
        clear.setBackground(bg(CARD, dp(12)));
        clear.setOnClickListener(v -> {
            HistoryStore.clear(this);
            refreshHistory();
            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
        });
        row.addView(clear);
        return row;
    }

    private View buildFilterRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        allChip = chip("Все");
        todayChip = chip("Сегодня");
        allChip.setOnClickListener(v -> {
            todayOnly = false;
            refreshHistory();
        });
        todayChip.setOnClickListener(v -> {
            todayOnly = true;
            refreshHistory();
        });
        row.addView(allChip, new LinearLayout.LayoutParams(dp(88), dp(38)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(100), dp(38));
        lp.setMarginStart(dp(8));
        row.addView(todayChip, lp);
        return row;
    }

    private TextView chip(String text) {
        TextView v = label(text, 13, TEXT, Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private TextView actionButton(String text, boolean accent) {
        TextView v = label(text, 14, Color.WHITE, Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setBackground(bg(accent ? ACCENT : CARD_2, dp(14)));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void refreshAll() {
        refreshStatus();
        refreshHistory();
    }

    private void refreshStatus() {
        if (statusText == null) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean enabled = Build.VERSION.SDK_INT < 24 || nm.areNotificationsEnabled();
        statusText.setText(enabled ? "✅ Уведомления активны" : "⛔ Уведомления отключены");
        statusText.setTextColor(enabled ? GREEN : RED);
        statusSub.setText(enabled
                ? "exteraGram может оставаться полностью заблокированным"
                : "Разреши уведомления этому приложению в настройках Android");
    }

    private void refreshHistory() {
        if (adapter == null) return;
        List<HistoryStore.Item> all = HistoryStore.get(this);
        totalValue.setText(String.valueOf(all.size()));
        if (all.isEmpty()) {
            lastValue.setText("—");
        } else {
            lastValue.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(all.get(0).timestamp)));
        }

        ArrayList<HistoryStore.Item> visible = new ArrayList<>();
        if (todayOnly) {
            long start = startOfToday();
            for (HistoryStore.Item it : all) if (it.timestamp >= start) visible.add(it);
        } else {
            visible.addAll(all);
        }
        adapter.setItems(visible);
        emptyView.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        historyList.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        updateChips();
    }

    private void updateChips() {
        allChip.setTextColor(todayOnly ? MUTED : Color.WHITE);
        todayChip.setTextColor(todayOnly ? Color.WHITE : MUTED);
        allChip.setBackground(bg(todayOnly ? CARD : ACCENT, dp(12)));
        todayChip.setBackground(bg(todayOnly ? ACCENT : CARD, dp(12)));
    }

    private long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private TextView label(String text, int sp, int color, int gravity) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(gravity);
        v.setIncludeFontPadding(false);
        return v;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable strokeBg(int color, float radius, int strokeColor, int strokeDp) {
        GradientDrawable d = bg(color, radius);
        d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class HistoryAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<HistoryStore.Item> items = new ArrayList<>();
        private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

        HistoryAdapter(Context context) {
            this.context = context;
        }

        void setItems(List<HistoryStore.Item> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public HistoryStore.Item getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView == null) {
                LinearLayout outer = new LinearLayout(context);
                outer.setOrientation(LinearLayout.VERTICAL);
                outer.setPadding(0, 0, 0, dp(10));

                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(15), dp(14), dp(15), dp(13));
                card.setBackground(bg(CARD, dp(16)));

                LinearLayout top = new LinearLayout(context);
                top.setGravity(Gravity.CENTER_VERTICAL);
                TextView title = label("", 16, TEXT, Gravity.START);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                title.setMaxLines(2);
                TextView time = label("", 12, MUTED, Gravity.END);
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                top.addView(title, titleLp);
                LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                timeLp.setMarginStart(dp(10));
                top.addView(time, timeLp);

                TextView body = label("", 14, Color.rgb(210, 214, 224), Gravity.START);
                body.setPadding(0, dp(9), 0, 0);
                body.setMaxLines(5);

                TextView open = label("Открыть источник  →", 13, Color.rgb(177, 149, 255), Gravity.START);
                open.setTypeface(Typeface.DEFAULT_BOLD);
                open.setPadding(0, dp(10), 0, 0);

                card.addView(top);
                card.addView(body);
                card.addView(open);
                outer.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                h = new Holder(title, time, body, open);
                outer.setTag(h);
                convertView = outer;
            } else {
                h = (Holder) convertView.getTag();
            }

            HistoryStore.Item item = getItem(position);
            h.title.setText(item.title.isEmpty() ? "Новая находка" : item.title);
            h.time.setText(isToday(item.timestamp) ? timeFmt.format(new Date(item.timestamp)) : dateFmt.format(new Date(item.timestamp)));
            h.body.setText(item.text);
            h.open.setVisibility(item.link.isEmpty() ? View.GONE : View.VISIBLE);
            return convertView;
        }

        private boolean isToday(long ts) {
            return ts >= startOfToday();
        }
    }

    private static final class Holder {
        final TextView title;
        final TextView time;
        final TextView body;
        final TextView open;
        Holder(TextView title, TextView time, TextView body, TextView open) {
            this.title = title;
            this.time = time;
            this.body = body;
            this.open = open;
        }
    }
}
