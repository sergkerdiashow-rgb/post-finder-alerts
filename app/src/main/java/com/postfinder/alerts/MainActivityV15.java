package com.postfinder.alerts;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivityV15 extends Activity {
    private static final int BG = Color.rgb(7, 9, 14);
    private static final int TEXT = Color.rgb(248, 249, 253);
    private static final int MUTED = Color.rgb(164, 171, 188);
    private static final int MUTED2 = Color.rgb(118, 126, 145);
    private static final int BLUE = Color.rgb(77, 125, 255);
    private static final int GREEN = Color.rgb(82, 226, 145);
    private static final int RED = Color.rgb(255, 103, 116);
    private static final int GOLD = Color.rgb(255, 201, 73);
    private static final int GLASS_STROKE = Color.argb(72, 255, 255, 255);
    private static final int GLASS_STROKE_STRONG = Color.argb(150, 185, 150, 255);
    private static final int REQ_HISTORY_EXPORT = 4101;
    private static final int REQ_HISTORY_IMPORT = 4102;
    private static final int REQ_BACKUP_EXPORT = 4103;
    private static final int REQ_BACKUP_IMPORT = 4104;
    private static final String UI_PREFS = "post_finder_alerts_ui";
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/sergkerdiashow-rgb/retro-platformer-android/post-finder-alerts-build/latest.json";

    private LinearLayout root;
    private FrameLayout pageContainer;
    private LinearLayout bottomNav;
    private int page = 0;
    private int historyFilter = 0;
    private int sortMode = 0;
    private String searchQuery = "";
    private boolean autoUpdateCheckDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertReceiver.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            v.setPadding(dp(14), top + dp(8), dp(14), Math.max(dp(8), bottom + dp(6)));
            return insets;
        });

        root.addView(buildHeader(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        pageContainer = new FrameLayout(this);
        root.addView(pageContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        bottomNav = buildBottomNav();
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70));
        navLp.topMargin = dp(7);
        root.addView(bottomNav, navLp);

        setContentView(root);
        requestNotifications();
        int openPage = getIntent() == null ? 0 : getIntent().getIntExtra("open_page", 0);
        showPage(Math.max(0, Math.min(openPage, 3)));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("open_page")) showPage(Math.max(0, Math.min(intent.getIntExtra("open_page", 0), 3)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentPage();
        if (!autoUpdateCheckDone && prefs().getBoolean("auto_update_check", true)) {
            autoUpdateCheckDone = true;
            checkForUpdates(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshCurrentPage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_HISTORY_EXPORT) {
                writeUri(uri, HistoryStore.exportJson(this));
                toast("История экспортирована");
            } else if (requestCode == REQ_HISTORY_IMPORT) {
                int imported = HistoryStore.importJson(this, readUri(uri));
                toast("Импортировано: " + imported);
                refreshCurrentPage();
            } else if (requestCode == REQ_BACKUP_EXPORT) {
                writeUri(uri, exportFullBackup());
                toast("Резервная копия сохранена");
            } else if (requestCode == REQ_BACKUP_IMPORT) {
                importFullBackup(readUri(uri));
                toast("Резервная копия восстановлена");
                refreshCurrentPage();
            }
        } catch (Exception e) {
            toast("Не удалось обработать файл");
        }
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), 0, dp(2), 0);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_pf_logo);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        logoLp.setMarginEnd(dp(11));
        row.addView(logo, logoLp);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);
        titleBox.addView(text("Post Finder", 22, TEXT, true));
        TextView sub = text("Alerts • Liquid Glass  " + appVersion(), 11, MUTED, false);
        sub.setPadding(0, dp(2), 0, 0);
        titleBox.addView(sub);
        row.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView more = iconButton("•••");
        more.setOnClickListener(v -> showQuickSheet());
        row.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(5), dp(6), dp(5));
        nav.setBackground(glassBg(true, dp(30)));
        nav.addView(navItem("⌂", "Главная", 0), equalLp(1f, 2));
        nav.addView(navItem("≡", "История", 1), equalLp(1f, 2));

        TextView plus = text("+", 31, Color.WHITE, true);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(accentGlass(dp(28)));
        plus.setOnClickListener(v -> showQuickSheet());
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        plusLp.setMargins(dp(4), 0, dp(4), 0);
        nav.addView(plus, plusLp);

        nav.addView(navItem("☆", "Избранное", 2), equalLp(1f, 2));
        nav.addView(navItem("▦", "Ещё", 3), equalLp(1f, 2));
        return nav;
    }

    private View navItem(String icon, String label, int target) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(2), dp(4), dp(2), dp(4));
        TextView i = text(icon, 22, MUTED, false);
        i.setGravity(Gravity.CENTER);
        TextView l = text(label, 10, MUTED, false);
        l.setGravity(Gravity.CENTER);
        box.addView(i, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(29)));
        box.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        box.setTag(new TextView[]{i, l});
        box.setOnClickListener(v -> showPage(target));
        return box;
    }

    private void styleBottomNav() {
        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View child = bottomNav.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof TextView[])) continue;
            TextView[] pair = (TextView[]) tag;
            int target;
            if (i == 0) target = 0;
            else if (i == 1) target = 1;
            else if (i == 3) target = 2;
            else if (i == 4) target = 3;
            else continue;
            boolean selected = page == target;
            pair[0].setTextColor(selected ? Color.WHITE : MUTED);
            pair[1].setTextColor(selected ? Color.WHITE : MUTED);
            child.setBackground(selected ? accentSoftBg(dp(22)) : new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void showPage(int target) {
        page = target;
        pageContainer.removeAllViews();
        if (page == 0) pageContainer.addView(buildHomePage());
        else if (page == 1) pageContainer.addView(buildHistoryPage(false));
        else if (page == 2) pageContainer.addView(buildHistoryPage(true));
        else pageContainer.addView(buildMorePage());
        styleBottomNav();
    }

    private void refreshCurrentPage() {
        if (pageContainer != null) showPage(page);
    }

    private View buildHomePage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = verticalContent();
        content.addView(buildStatusCard());
        content.addView(space(dp(10)));
        content.addView(buildStatsRow());
        content.addView(space(dp(10)));
        content.addView(buildHomeActions());
        content.addView(space(dp(18)));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Последние находки", 20, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView all = glassSmallButton("Все →");
        all.setOnClickListener(v -> showPage(1));
        header.addView(all);
        content.addView(header);
        content.addView(space(dp(8)));

        List<HistoryStore.Item> items = HistoryStore.get(this);
        if (items.isEmpty()) content.addView(buildEmptyCard("Ничего не найдено", "Новые находки появятся здесь автоматически", dp(238)));
        else {
            int limit = Math.min(3, items.size());
            for (int i = 0; i < limit; i++) {
                content.addView(buildItemCard(items.get(i)));
                if (i < limit - 1) content.addView(space(dp(8)));
            }
        }
        scroll.addView(content);
        return scroll;
    }

    private LinearLayout verticalContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(6), 0, dp(28));
        return content;
    }

    private View buildStatusCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(11), dp(11), dp(11));
        card.setBackground(glassBg(true, dp(21)));

        boolean enabled = notificationsEnabled();
        long last = AppStateStore.lastSignalTs(this);
        String subText;
        if (!enabled) subText = "Нажми, чтобы разрешить системные уведомления";
        else if (last > 0) subText = "Последний сигнал Post Finder WB • " + relativeTime(last);
        else subText = "Ожидаем первую реальную находку от Post Finder WB";

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(enabled ? "●  Уведомления активны" : "●  Уведомления отключены", 15, enabled ? GREEN : RED, true));
        TextView sub = text(subText, 12, MUTED, false);
        sub.setPadding(0, dp(4), 0, 0);
        txt.addView(sub);
        card.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView dot = iconButton("●");
        dot.setTextColor(enabled ? GREEN : RED);
        card.addView(dot, new LinearLayout.LayoutParams(dp(44), dp(44)));
        card.setOnClickListener(v -> showDiagnosticsScreen());
        return card;
    }

    private View buildStatsRow() {
        List<HistoryStore.Item> items = HistoryStore.get(this);
        long todayStart = startOfToday();
        int today = 0, unread = 0, best = 0, procCount = 0;
        long procSum = 0;
        for (HistoryStore.Item item : items) {
            if (item.timestamp >= todayStart) today++;
            if (!item.read) unread++;
            best = Math.max(best, item.saving);
            if (item.processingMs > 0) { procSum += item.processingMs; procCount++; }
        }
        String avg = procCount > 0 ? formatMs((int)(procSum / procCount)) : "—";
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(statCard("▣", "Сегодня", String.valueOf(today), "находок"), weighted(1, 0, 4));
        row.addView(statCard("⚡", "Новые", String.valueOf(unread), "непрочитанных"), weighted(1, 4, 4));
        row.addView(statCard("☆", "Лучшая", best > 0 ? money(best) : "—", best > 0 ? "₽ выгоды" : "—"), weighted(1, 4, 4));
        row.addView(statCard("◷", "Ср. обработка", avg, procCount > 0 ? "" : "—"), weighted(1, 4, 0));
        return row;
    }

    private View statCard(String icon, String label, String value, String sub) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(9), dp(10), dp(8), dp(9));
        card.setBackground(glassBg(false, dp(18)));
        card.addView(text(icon, 16, label.equals("Лучшая") ? GOLD : BLUE, true));
        TextView lbl = text(label, 10, MUTED, false); lbl.setPadding(0, dp(7), 0, 0); card.addView(lbl);
        TextView val = text(value, 19, TEXT, true); val.setPadding(0, dp(4), 0, 0); card.addView(val);
        TextView small = text(sub, 9, MUTED2, false); small.setPadding(0, dp(2), 0, 0); card.addView(small);
        card.setMinimumHeight(dp(112));
        return card;
    }

    private View buildHomeActions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        View test = actionCard("⚡", "Тест", "Проверить оповещения", true);
        test.setOnClickListener(v -> AlertReceiver.showNotification(this, "Post Finder Alerts", "Тест уведомления • всё работает", "", 15001));
        View stats = actionCard("▥", "Статистика", "Подробные отчёты", false);
        stats.setOnClickListener(v -> showStatsSheet());
        View settings = actionCard("⚙", "Настройки", "Параметры приложения", false);
        settings.setOnClickListener(v -> showSettingsScreen());
        row.addView(test, weighted(1, 0, 5));
        row.addView(stats, weighted(1, 5, 5));
        row.addView(settings, weighted(1, 5, 0));
        return row;
    }

    private View actionCard(String icon, String title, String sub, boolean accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(10), dp(10));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(accent ? accentGlass(dp(20)) : glassBg(false, dp(20)));
        card.addView(text(icon, 21, accent ? GOLD : BLUE, true));
        TextView t = text(title, 14, TEXT, true); t.setPadding(0, dp(8), 0, 0); card.addView(t);
        TextView s = text(sub, 10, accent ? Color.rgb(224, 218, 255) : MUTED, false); s.setPadding(0, dp(4), 0, 0); card.addView(s);
        card.setMinimumHeight(dp(118));
        return card;
    }

    private View buildHistoryPage(boolean favoritesOnly) {
        LinearLayout pageBox = new LinearLayout(this);
        pageBox.setOrientation(LinearLayout.VERTICAL);
        pageBox.setPadding(0, dp(6), 0, dp(18));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text(favoritesOnly ? "Избранное" : "Находки", 24, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView sort = glassSmallButton(sortLabel() + " ▾");
        sort.setOnClickListener(v -> showSortSheet());
        titleRow.addView(sort);
        pageBox.addView(titleRow);
        pageBox.addView(space(dp(10)));

        if (!favoritesOnly) {
            pageBox.addView(buildFilterBar());
            pageBox.addView(space(dp(8)));
        }
        pageBox.addView(buildSearchBox());
        pageBox.addView(space(dp(9)));

        List<HistoryStore.Item> visible = filteredItems(favoritesOnly);
        pageBox.addView(text((favoritesOnly ? "Избранных: " : "Показано: ") + visible.size(), 11, MUTED, false));
        pageBox.addView(space(dp(7)));

        if (visible.isEmpty()) {
            LinearLayout wrap = new LinearLayout(this);
            wrap.setGravity(Gravity.TOP);
            wrap.setPadding(0, dp(8), 0, 0);
            wrap.addView(buildEmptyCard(favoritesOnly ? "Избранное пусто" : "Ничего не найдено",
                    favoritesOnly ? "Долгое нажатие на находку добавляет её сюда" : "Новые находки появятся здесь автоматически", dp(265)),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pageBox.addView(wrap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            ListView list = new ListView(this);
            list.setDivider(null);
            list.setDividerHeight(0);
            list.setSelector(android.R.color.transparent);
            list.setVerticalScrollBarEnabled(false);
            list.setClipToPadding(false);
            list.setPadding(0, 0, 0, dp(18));
            list.setAdapter(new DealAdapter(visible));
            list.setOnItemClickListener((parent, view, position, id) -> {
                HistoryStore.Item item = visible.get(position);
                HistoryStore.markRead(this, item.id, true);
                showDetailsSheet(HistoryStore.find(this, item.id));
                refreshCurrentPage();
            });
            list.setOnItemLongClickListener((parent, view, position, id) -> {
                HistoryStore.Item item = visible.get(position);
                boolean fav = HistoryStore.toggleFavorite(this, item.id);
                toast(fav ? "Добавлено в избранное" : "Удалено из избранного");
                refreshCurrentPage();
                return true;
            });
            pageBox.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
        return pageBox;
    }

    private View buildFilterBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, dp(18), 0);
        String[] names = {"Все", "Сегодня", "WB", "★ Избранное", "● Новые"};
        for (int i = 0; i < names.length; i++) {
            final int mode = i;
            TextView chip = text(names[i], 12, historyFilter == i ? Color.WHITE : MUTED, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setBackground(historyFilter == i ? accentGlass(dp(17)) : glassBg(false, dp(17)));
            chip.setOnClickListener(v -> { historyFilter = mode; refreshCurrentPage(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            if (i > 0) lp.setMarginStart(dp(7));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private View buildSearchBox() {
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setTextSize(13);
        search.setHint("⌕  Поиск по товару, источнику или тексту");
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(glassBg(false, dp(18)));
        search.setText(searchQuery);
        search.setSelection(search.getText().length());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        search.setOnEditorActionListener((v, actionId, event) -> { refreshCurrentPage(); return false; });
        return wrapFixed(search, dp(48));
    }

    private List<HistoryStore.Item> filteredItems(boolean favoritesOnly) {
        List<HistoryStore.Item> all = HistoryStore.get(this);
        ArrayList<HistoryStore.Item> out = new ArrayList<>();
        long today = startOfToday();
        for (HistoryStore.Item item : all) {
            if (favoritesOnly && !item.favorite) continue;
            if (!favoritesOnly) {
                if (historyFilter == 1 && item.timestamp < today) continue;
                if (historyFilter == 2 && !item.isWildberries()) continue;
                if (historyFilter == 3 && !item.favorite) continue;
                if (historyFilter == 4 && item.read) continue;
            }
            if (!searchQuery.isEmpty() && !item.searchableText().contains(searchQuery)) continue;
            out.add(item);
        }
        sortItems(out);
        return out;
    }

    private View buildMorePage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box = verticalContent();
        box.addView(text("Ещё", 24, TEXT, true));
        box.addView(space(dp(12)));

        box.addView(sectionLabel("УВЕДОМЛЕНИЯ И ОБНОВЛЕНИЯ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("◉", "Настройки уведомлений", notificationsEnabled() ? "Разрешены" : "Отключены", this::openNotificationSettings),
                new MenuSpec("↻", "Проверить обновление", "Post Finder Alerts " + appVersion(), () -> checkForUpdates(true)),
                new MenuSpec("⚡", "Тест уведомления", "Проверить звук, вибрацию и карточку", () -> AlertReceiver.showNotification(this, "Post Finder Alerts", "Тест уведомления • всё работает", "", 15002))
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("ПРИЛОЖЕНИЕ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("⚙", "Настройки приложения", "Внешний вид, поведение и сервис", this::showSettingsScreen),
                new MenuSpec("◷", "Связь с Post Finder WB", diagnosticSummary(), this::showDiagnosticsScreen),
                new MenuSpec("▥", "Статистика", "Выгода, WB и скорость обработки", this::showStatsSheet)
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("РЕЗЕРВНЫЕ КОПИИ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("⇩", "Резервная копия всего", "История + настройки приложения", this::exportFullBackupFile),
                new MenuSpec("⇧", "Восстановить копию", "Объединить историю и вернуть настройки", this::importFullBackupFile),
                new MenuSpec("⇣", "Экспорт только истории", "JSON • избранное и отметки сохраняются", this::exportHistory),
                new MenuSpec("⇡", "Импорт только истории", "Объединить с текущей историей", this::importHistory)
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("ИСТОРИЯ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("✓", "Отметить всё прочитанным", unreadCount() + " непрочитанных", () -> { HistoryStore.markAllRead(this); toast("Готово"); refreshCurrentPage(); }),
                new MenuSpec("⌫", "Очистить историю", HistoryStore.get(this).size() + " записей", this::confirmClearHistory)
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("ИНФОРМАЦИЯ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("i", "О приложении", "Версия, сборка и совместимость", this::showAboutScreen)
        }));
        box.addView(space(dp(18)));

        TextView note = text("Post Finder Alerts хранит историю локально. Мониторинг Telegram выполняет Post Finder WB внутри exteraGram.", 11, MUTED, false);
        note.setPadding(dp(6), 0, dp(6), dp(18));
        box.addView(note);

        scroll.addView(box);
        return scroll;
    }

    private View menuGroup(MenuSpec[] specs) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(dp(5), dp(3), dp(5), dp(3));
        group.setBackground(glassBg(false, dp(22)));
        for (int i = 0; i < specs.length; i++) {
            MenuSpec s = specs[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), dp(10), dp(8), dp(10));

            TextView icon = text(s.icon, 20, BLUE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(glassBg(false, dp(14)));
            row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

            LinearLayout textBox = new LinearLayout(this);
            textBox.setOrientation(LinearLayout.VERTICAL);
            textBox.setPadding(dp(12), 0, dp(4), 0);
            textBox.addView(text(s.title, 14, TEXT, true));
            TextView sub = text(s.sub, 11, MUTED, false); sub.setPadding(0, dp(3), 0, 0); textBox.addView(sub);
            row.addView(textBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(text("›", 25, MUTED, false), new LinearLayout.LayoutParams(dp(28), dp(44)));
            row.setOnClickListener(v -> s.action.run());
            group.addView(row);
            if (i < specs.length - 1) group.addView(divider());
        }
        return group;
    }

    private View buildItemCard(HistoryStore.Item item) {
        boolean compact = prefs().getBoolean("compact_cards", false);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(compact ? 9 : 12), dp(14), dp(compact ? 9 : 12));
        card.setBackground(item.read ? glassBg(false, dp(20)) : glassUnreadBg(dp(20)));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text((item.favorite ? "★  " : "") + item.displayTitle(), 15, TEXT, true);
        title.setMaxLines(2);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(text(formatTime(item.timestamp), 11, MUTED, false));
        card.addView(top);

        int effective = item.effectivePrice > 0 ? item.effectivePrice : item.price;
        StringBuilder priceLine = new StringBuilder();
        if (effective > 0) priceLine.append(money(effective)).append(" ₽");
        if (item.saving > 0) priceLine.append("  •  +").append(money(item.saving)).append(" ₽ выгоды");
        if (item.wbExtra > 0) priceLine.append("  •  WB +").append(money(item.wbExtra));
        if (priceLine.length() > 0) {
            TextView p = text(priceLine.toString(), 13, Color.rgb(205, 183, 255), true);
            p.setPadding(0, dp(7), 0, 0);
            card.addView(p);
        }

        String source = item.sourceTitle.isEmpty() ? item.text : item.sourceTitle;
        if (!source.isEmpty()) {
            TextView s = text(source, 11, MUTED, false);
            s.setMaxLines(compact ? 1 : 2);
            s.setPadding(0, dp(6), 0, 0);
            card.addView(s);
        }

        card.setOnClickListener(v -> {
            HistoryStore.markRead(this, item.id, true);
            showDetailsSheet(HistoryStore.find(this, item.id));
            refreshCurrentPage();
        });
        card.setOnLongClickListener(v -> {
            boolean fav = HistoryStore.toggleFavorite(this, item.id);
            toast(fav ? "Добавлено в избранное" : "Удалено из избранного");
            refreshCurrentPage();
            return true;
        });
        return card;
    }

    private final class DealAdapter extends BaseAdapter {
        private final List<HistoryStore.Item> items;
        DealAdapter(List<HistoryStore.Item> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public HistoryStore.Item getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout outer = new LinearLayout(MainActivityV15.this);
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.addView(buildItemCard(getItem(position)));
            outer.setPadding(0, 0, 0, dp(8));
            return outer;
        }
    }

    private void showDetailsSheet(HistoryStore.Item item) {
        if (item == null) return;
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(item.displayTitle(), 20, TEXT, true);
        title.setMaxLines(2);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = iconButton("×");
        close.setOnClickListener(v -> d.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        panel.addView(head);
        panel.addView(space(dp(10)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(infoPill(formatDate(item.timestamp) + (item.sourceTitle.isEmpty() ? "" : "  •  " + item.sourceTitle)));

        int effective = item.effectivePrice > 0 ? item.effectivePrice : item.price;
        if (effective > 0) body.addView(metricRow("Итоговая цена", money(effective) + " ₽", Color.rgb(205, 183, 255)));
        if (item.price > 0 && item.price != effective) body.addView(metricRow("Цена товара", money(item.price) + " ₽", TEXT));
        if (item.wbExtra > 0) body.addView(metricRow("Пошлина WB", "+" + money(item.wbExtra) + " ₽", GOLD));
        if (item.referencePrice > 0) body.addView(metricRow("Прайс", money(item.referencePrice) + " ₽", TEXT));
        if (item.limit > 0) body.addView(metricRow("Лимит", money(item.limit) + " ₽", TEXT));
        if (item.saving > 0) body.addView(metricRow("Выгода", "+" + money(item.saving) + " ₽" + (item.discountPercent > 0 ? String.format(Locale.getDefault(), " • %.1f%%", item.discountPercent) : ""), GREEN));
        if (item.processingMs > 0 && prefs().getBoolean("show_processing", true)) body.addView(metricRow("Обработка", formatMs(item.processingMs), BLUE));

        String fullText = !item.postText.isEmpty() ? item.postText : item.text;
        if (!fullText.isEmpty()) {
            TextView fullTitle = sectionLabel("ИСХОДНЫЙ ПОСТ ПОЛНОСТЬЮ");
            fullTitle.setPadding(0, dp(13), 0, dp(7));
            body.addView(fullTitle);
            TextView full = text(fullText, 12, Color.rgb(222, 225, 235), false);
            full.setTextIsSelectable(true);
            full.setLineSpacing(0, 1.08f);
            full.setPadding(dp(12), dp(11), dp(12), dp(11));
            full.setBackground(glassBg(false, dp(16)));
            body.addView(full);
        }

        scroll.addView(body);
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        panel.addView(space(dp(10)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView open = sheetAction("↗\nИсточник", !item.link.isEmpty());
        open.setOnClickListener(v -> { if (!item.link.isEmpty()) openLink(item.link); });
        TextView fav = sheetAction(item.favorite ? "★\nВ избранном" : "☆\nИзбранное", true);
        fav.setOnClickListener(v -> { HistoryStore.toggleFavorite(this, item.id); d.dismiss(); refreshCurrentPage(); });
        TextView copy = sheetAction("⧉\nКопировать", true);
        copy.setOnClickListener(v -> copyToClipboard(detailsText(item)));
        TextView share = sheetAction("↗\nПоделиться", true);
        share.setOnClickListener(v -> shareText(detailsText(item)));
        actions.addView(open, weighted(1, 0, 4));
        actions.addView(fav, weighted(1, 4, 4));
        actions.addView(copy, weighted(1, 4, 4));
        actions.addView(share, weighted(1, 4, 0));
        panel.addView(actions);

        TextView delete = text("Удалить эту запись", 12, RED, true);
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(dp(12), dp(10), dp(12), dp(10));
        delete.setOnClickListener(v -> {
            if (prefs().getBoolean("confirm_delete", true)) confirmDeleteItem(d, item);
            else { HistoryStore.delete(this, item.id); d.dismiss(); refreshCurrentPage(); }
        });
        panel.addView(delete);
        setSheet(d, panel, 0.86f);
        d.show();
    }

    private void showQuickSheet() {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Быстрые действия", 20, TEXT, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(4), 0, dp(9));
        panel.addView(title);
        panel.addView(sheetMenuRow("↻", "Проверить обновление", () -> { d.dismiss(); checkForUpdates(true); }));
        panel.addView(sheetMenuRow("◉", "Настройки уведомлений", () -> { d.dismiss(); openNotificationSettings(); }));
        panel.addView(sheetMenuRow("▥", "Статистика", () -> { d.dismiss(); showStatsSheet(); }));
        panel.addView(sheetMenuRow("✓", "Отметить всё прочитанным", () -> { HistoryStore.markAllRead(this); d.dismiss(); refreshCurrentPage(); }));
        panel.addView(sheetMenuRow("⇩", "Резервная копия", () -> { d.dismiss(); exportFullBackupFile(); }));
        TextView close = glassWideButton("Закрыть", false);
        close.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.topMargin = dp(5);
        panel.addView(close, lp);
        setSheet(d, panel, 0f);
        d.show();
    }

    private void showSortSheet() {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Сортировка", 19, TEXT, true);
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        panel.addView(space(dp(8)));
        String[] names = {"Сначала новые", "Максимальная выгода", "Максимальный %", "Минимальная цена", "Самая быстрая обработка"};
        for (int i = 0; i < names.length; i++) {
            final int mode = i;
            panel.addView(sheetMenuRow(sortMode == i ? "✓" : "•", names[i], () -> {
                sortMode = mode;
                d.dismiss();
                refreshCurrentPage();
            }));
        }
        setSheet(d, panel, 0f);
        d.show();
    }

    private void showStatsSheet() {
        List<HistoryStore.Item> items = HistoryStore.get(this);
        int wb = 0, fav = 0, unread = 0, bestSaving = 0, procCount = 0;
        long totalSaving = 0, procSum = 0;
        double bestPct = 0;
        for (HistoryStore.Item item : items) {
            if (item.isWildberries()) wb++;
            if (item.favorite) fav++;
            if (!item.read) unread++;
            if (item.saving > 0) { totalSaving += item.saving; bestSaving = Math.max(bestSaving, item.saving); }
            bestPct = Math.max(bestPct, item.discountPercent);
            if (item.processingMs > 0) { procSum += item.processingMs; procCount++; }
        }
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Статистика", 21, TEXT, true); title.setGravity(Gravity.CENTER); panel.addView(title);
        panel.addView(space(dp(10)));
        panel.addView(metricRow("Всего находок", String.valueOf(items.size()), TEXT));
        panel.addView(metricRow("Непрочитанные", String.valueOf(unread), unread > 0 ? GOLD : TEXT));
        panel.addView(metricRow("Избранные", String.valueOf(fav), TEXT));
        panel.addView(metricRow("Wildberries", String.valueOf(wb), TEXT));
        panel.addView(metricRow("Суммарная выгода", totalSaving > 0 ? money((int)Math.min(Integer.MAX_VALUE, totalSaving)) + " ₽" : "—", GREEN));
        panel.addView(metricRow("Лучшая выгода", bestSaving > 0 ? money(bestSaving) + " ₽" : "—", GREEN));
        panel.addView(metricRow("Лучший %", bestPct > 0 ? String.format(Locale.getDefault(), "%.1f%%", bestPct) : "—", GREEN));
        panel.addView(metricRow("Средняя обработка", procCount > 0 ? formatMs((int)(procSum / procCount)) : "—", BLUE));
        TextView close = glassWideButton("Готово", false); close.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)); lp.topMargin = dp(8); panel.addView(close, lp);
        setSheet(d, panel, 0f);
        d.show();
    }

    private void showSettingsScreen() {
        page = 3;
        pageContainer.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box = verticalContent();
        box.addView(subPageHeader("Настройки", () -> showPage(3)));
        box.addView(space(dp(12)));

        box.addView(sectionLabel("ИНТЕРФЕЙС"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("▤", "Компактные карточки", prefs().getBoolean("compact_cards", false) ? "Включены" : "Обычный размер", () -> togglePref("compact_cards")),
                new MenuSpec("◷", "Показывать время обработки", prefs().getBoolean("show_processing", true) ? "Показывается в деталях" : "Скрыто", () -> togglePref("show_processing"))
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("ПОВЕДЕНИЕ"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("↻", "Проверять обновления при запуске", prefs().getBoolean("auto_update_check", true) ? "Включено" : "Выключено", () -> togglePref("auto_update_check")),
                new MenuSpec("!", "Подтверждать удаление", prefs().getBoolean("confirm_delete", true) ? "Спрашивать перед удалением" : "Удалять сразу", () -> togglePref("confirm_delete"))
        }));
        box.addView(space(dp(14)));

        box.addView(sectionLabel("СИСТЕМА"));
        box.addView(menuGroup(new MenuSpec[]{
                new MenuSpec("◉", "Настройки уведомлений", notificationsEnabled() ? "Разрешены" : "Отключены", this::openNotificationSettings),
                new MenuSpec("⚙", "Настройки Android для приложения", "Батарея, разрешения и хранилище", this::openAppSettings)
        }));
        box.addView(space(dp(14)));

        TextView reset = glassWideButton("Сбросить настройки приложения", false);
        reset.setTextColor(RED);
        reset.setOnClickListener(v -> confirmResetSettings());
        box.addView(reset, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        box.addView(space(dp(18)));
        scroll.addView(box);
        pageContainer.addView(scroll);
        styleBottomNav();
    }

    private void showDiagnosticsScreen() {
        page = 3;
        pageContainer.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box = verticalContent();
        box.addView(subPageHeader("Связь с Post Finder WB", () -> showPage(3)));
        box.addView(space(dp(12)));

        long last = AppStateStore.lastSignalTs(this);
        boolean seen = last > 0;
        TextView status = infoPill(seen ? "●  Последний реальный сигнал получен " + relativeTime(last) : "○  Реальных сигналов от плагина ещё не было");
        status.setTextColor(seen ? GREEN : MUTED);
        box.addView(status);
        box.addView(space(dp(8)));

        box.addView(metricRow("Приёмник companion", "Готов", GREEN));
        box.addView(metricRow("Уведомления Android", notificationsEnabled() ? "Разрешены" : "Отключены", notificationsEnabled() ? GREEN : RED));
        box.addView(metricRow("Сигналов принято", String.valueOf(AppStateStore.signalCount(this)), TEXT));
        box.addView(metricRow("Последний сигнал", seen ? formatDate(last) : "—", TEXT));
        box.addView(metricRow("Последний товар", emptyDash(AppStateStore.lastProduct(this)), TEXT));
        box.addView(metricRow("Источник", emptyDash(AppStateStore.lastSource(this)), TEXT));
        box.addView(metricRow("Задержка обработки", AppStateStore.lastProcessingMs(this) > 0 ? formatMs(AppStateStore.lastProcessingMs(this)) : "—", BLUE));
        String pv = AppStateStore.lastPluginVersion(this);
        box.addView(metricRow("Версия Post Finder WB", pv.isEmpty() ? "не передаётся" : pv, TEXT));
        box.addView(space(dp(12)));

        TextView note = infoPill("Важно: companion не опрашивает Telegram сам. Этот экран показывает последний фактически полученный сигнал. Если подходящих находок давно не было, это не означает, что мониторинг сломан.");
        box.addView(note);
        box.addView(space(dp(12)));

        LinearLayout actions = new LinearLayout(this);
        TextView test = glassWideButton("Тест", true);
        test.setOnClickListener(v -> AlertReceiver.showNotification(this, "Post Finder Alerts", "Диагностический тест • companion работает", "", 15003));
        TextView copy = glassWideButton("Копировать отчёт", false);
        copy.setOnClickListener(v -> copyToClipboard(buildDiagnosticsText()));
        actions.addView(test, weighted(1, 0, 4));
        actions.addView(copy, weighted(1, 4, 0));
        box.addView(actions);
        box.addView(space(dp(18)));

        scroll.addView(box);
        pageContainer.addView(scroll);
        styleBottomNav();
    }

    private void showAboutScreen() {
        page = 3;
        pageContainer.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout box = verticalContent();
        box.addView(subPageHeader("О приложении", () -> showPage(3)));
        box.addView(space(dp(12)));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16), dp(20), dp(16), dp(20));
        hero.setBackground(glassBg(true, dp(24)));
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ic_pf_logo);
        hero.addView(logo, new LinearLayout.LayoutParams(dp(72), dp(72)));
        TextView name = text("Post Finder Alerts", 22, TEXT, true); name.setPadding(0, dp(10), 0, 0); hero.addView(name);
        TextView ver = text("Версия " + appVersion() + " • Liquid Glass", 12, MUTED, false); ver.setPadding(0, dp(4), 0, 0); hero.addView(ver);
        box.addView(hero);
        box.addView(space(dp(10)));

        box.addView(metricRow("Пакет", getPackageName(), TEXT));
        box.addView(metricRow("Android API", String.valueOf(Build.VERSION.SDK_INT), TEXT));
        box.addView(metricRow("История", HistoryStore.get(this).size() + " / 1000", TEXT));
        box.addView(metricRow("Уведомления", notificationsEnabled() ? "Разрешены" : "Отключены", notificationsEnabled() ? GREEN : RED));
        box.addView(space(dp(10)));

        TextView whatsNew = infoPill("Что нового в 1.5: чистовая геометрия, нижний safe-area, компактные empty-state, свайп вниз для закрытия меню, настройки приложения, диагностика связи с Post Finder WB и полная резервная копия.");
        box.addView(whatsNew);
        box.addView(space(dp(12)));

        TextView update = glassWideButton("Проверить обновление", true);
        update.setOnClickListener(v -> checkForUpdates(true));
        box.addView(update, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        box.addView(space(dp(18)));

        scroll.addView(box);
        pageContainer.addView(scroll);
        styleBottomNav();
    }

    private View subPageHeader(String title, Runnable backAction) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = glassSmallButton("‹ Назад");
        back.setOnClickListener(v -> backAction.run());
        row.addView(back);
        TextView t = text(title, 22, TEXT, true);
        t.setGravity(Gravity.END);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void togglePref(String key) {
        boolean current = prefs().getBoolean(key, defaultPref(key));
        prefs().edit().putBoolean(key, !current).apply();
        showSettingsScreen();
    }

    private boolean defaultPref(String key) {
        return !key.equals("compact_cards");
    }

    private void exportHistory() {
        createDocument(REQ_HISTORY_EXPORT, "post-finder-alerts-history-" + timeStampFile() + ".json");
    }

    private void importHistory() {
        openDocument(REQ_HISTORY_IMPORT);
    }

    private void exportFullBackupFile() {
        createDocument(REQ_BACKUP_EXPORT, "post-finder-alerts-backup-" + timeStampFile() + ".json");
    }

    private void importFullBackupFile() {
        openDocument(REQ_BACKUP_IMPORT);
    }

    private String exportFullBackup() throws Exception {
        JSONObject root = new JSONObject();
        root.put("schema", 2);
        root.put("app", "Post Finder Alerts");
        root.put("version", appVersion());
        root.put("exported_at", System.currentTimeMillis());

        JSONObject settings = new JSONObject();
        settings.put("compact_cards", prefs().getBoolean("compact_cards", false));
        settings.put("show_processing", prefs().getBoolean("show_processing", true));
        settings.put("auto_update_check", prefs().getBoolean("auto_update_check", true));
        settings.put("confirm_delete", prefs().getBoolean("confirm_delete", true));
        root.put("settings", settings);

        JSONObject history = new JSONObject(HistoryStore.exportJson(this));
        root.put("history", history);
        return root.toString(2);
    }

    private void importFullBackup(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            SharedPreferences.Editor e = prefs().edit();
            if (settings.has("compact_cards")) e.putBoolean("compact_cards", settings.optBoolean("compact_cards", false));
            if (settings.has("show_processing")) e.putBoolean("show_processing", settings.optBoolean("show_processing", true));
            if (settings.has("auto_update_check")) e.putBoolean("auto_update_check", settings.optBoolean("auto_update_check", true));
            if (settings.has("confirm_delete")) e.putBoolean("confirm_delete", settings.optBoolean("confirm_delete", true));
            e.apply();
        }
        JSONObject history = root.optJSONObject("history");
        if (history != null) HistoryStore.importJson(this, history.toString());
        else if (root.has("items")) HistoryStore.importJson(this, root.toString());
    }

    private void checkForUpdates(boolean interactive) {
        if (interactive) toast("Проверяю обновление…");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);
                conn.setRequestProperty("User-Agent", "PostFinderAlerts/" + appVersion());
                conn.connect();
                if (conn.getResponseCode() != 200) throw new IllegalStateException("http " + conn.getResponseCode());
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject o = new JSONObject(sb.toString());
                int latestCode = o.optInt("versionCode", 0);
                String latestName = o.optString("versionName", "");
                String apk = o.optString("apk", "");
                String changes = o.optString("changelog", "");
                int current = appVersionCode();
                runOnUiThread(() -> {
                    if (latestCode > current && !apk.isEmpty()) showUpdateSheet(latestName, apk, changes);
                    else if (interactive) showUpToDateSheet();
                });
            } catch (Exception e) {
                runOnUiThread(() -> { if (interactive) toast("Не удалось проверить обновление"); });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showUpdateSheet(String version, String apk, String changes) {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Доступна версия " + version, 21, TEXT, true); title.setGravity(Gravity.CENTER); panel.addView(title);
        TextView sub = text(changes.isEmpty() ? "Новая версия Post Finder Alerts готова к установке." : changes, 12, MUTED, false);
        sub.setPadding(dp(4), dp(10), dp(4), dp(12)); panel.addView(sub);
        TextView install = glassWideButton("Скачать обновление", true);
        install.setOnClickListener(v -> { d.dismiss(); openLink(apk); });
        panel.addView(install, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        TextView later = glassWideButton("Позже", false); later.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)); lp.topMargin = dp(8); panel.addView(later, lp);
        setSheet(d, panel, 0f); d.show();
    }

    private void showUpToDateSheet() {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView ok = text("✓", 34, GREEN, true); ok.setGravity(Gravity.CENTER); panel.addView(ok);
        TextView title = text("Установлена актуальная версия", 19, TEXT, true); title.setGravity(Gravity.CENTER); title.setPadding(0, dp(7), 0, 0); panel.addView(title);
        TextView sub = text("Post Finder Alerts " + appVersion(), 12, MUTED, false); sub.setGravity(Gravity.CENTER); sub.setPadding(0, dp(4), 0, dp(12)); panel.addView(sub);
        TextView close = glassWideButton("Готово", false); close.setOnClickListener(v -> d.dismiss()); panel.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        setSheet(d, panel, 0f); d.show();
    }

    private void confirmClearHistory() {
        if (!prefs().getBoolean("confirm_delete", true)) {
            HistoryStore.clear(this); refreshCurrentPage(); toast("История очищена"); return;
        }
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Очистить историю?", 20, TEXT, true); title.setGravity(Gravity.CENTER); panel.addView(title);
        TextView sub = text("Удалятся только локальные находки. Настройки приложения и Post Finder WB не затрагиваются.", 12, MUTED, false);
        sub.setPadding(dp(4), dp(9), dp(4), dp(12)); panel.addView(sub);
        TextView clear = glassWideButton("Очистить историю", false); clear.setTextColor(RED);
        clear.setOnClickListener(v -> { HistoryStore.clear(this); d.dismiss(); refreshCurrentPage(); toast("История очищена"); });
        panel.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        TextView cancel = glassWideButton("Отмена", false); cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)); lp.topMargin = dp(8); panel.addView(cancel, lp);
        setSheet(d, panel, 0f); d.show();
    }

    private void confirmDeleteItem(Dialog parent, HistoryStore.Item item) {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Удалить запись?", 19, TEXT, true); title.setGravity(Gravity.CENTER); panel.addView(title);
        TextView del = glassWideButton("Удалить", false); del.setTextColor(RED);
        del.setOnClickListener(v -> { HistoryStore.delete(this, item.id); d.dismiss(); parent.dismiss(); refreshCurrentPage(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)); lp.topMargin = dp(12); panel.addView(del, lp);
        TextView cancel = glassWideButton("Отмена", false); cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)); lp2.topMargin = dp(8); panel.addView(cancel, lp2);
        setSheet(d, panel, 0f); d.show();
    }

    private void confirmResetSettings() {
        Dialog d = createSheet();
        LinearLayout panel = sheetPanel();
        panel.addView(sheetHandle(d));
        TextView title = text("Сбросить настройки?", 19, TEXT, true); title.setGravity(Gravity.CENTER); panel.addView(title);
        TextView sub = text("История находок не удалится.", 12, MUTED, false); sub.setGravity(Gravity.CENTER); sub.setPadding(0, dp(8), 0, dp(12)); panel.addView(sub);
        TextView reset = glassWideButton("Сбросить", false); reset.setTextColor(RED);
        reset.setOnClickListener(v -> { prefs().edit().clear().apply(); d.dismiss(); showSettingsScreen(); toast("Настройки сброшены"); });
        panel.addView(reset, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        TextView cancel = glassWideButton("Отмена", false); cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)); lp.topMargin = dp(8); panel.addView(cancel, lp);
        setSheet(d, panel, 0f); d.show();
    }

    private Dialog createSheet() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCanceledOnTouchOutside(true);
        return d;
    }

    private LinearLayout sheetPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(14));
        panel.setBackground(sheetBg(dp(28)));
        return panel;
    }

    private View sheetHandle(Dialog d) {
        TextView v = text("━", 22, Color.argb(145, 205, 190, 255), true);
        v.setGravity(Gravity.CENTER);
        final float[] start = new float[1];
        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                start[0] = event.getRawY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float delta = event.getRawY() - start[0];
                if (delta > dp(55)) d.dismiss();
                return true;
            }
            return true;
        });
        return v;
    }

    private void setSheet(Dialog d, View panel, float heightRatio) {
        FrameLayout outer = new FrameLayout(this);
        outer.setPadding(dp(10), dp(10), dp(10), dp(10));
        int height = heightRatio > 0 ? (int)(getResources().getDisplayMetrics().heightPixels * heightRatio) : ViewGroup.LayoutParams.WRAP_CONTENT;
        outer.addView(panel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.BOTTOM));
        d.setContentView(outer);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams p = w.getAttributes();
            p.dimAmount = 0.60f;
            p.gravity = Gravity.BOTTOM;
            p.width = WindowManager.LayoutParams.MATCH_PARENT;
            p.height = WindowManager.LayoutParams.WRAP_CONTENT;
            w.setAttributes(p);
        }
    }

    private View sheetMenuRow(String iconText, String titleText, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(6), dp(8), dp(6));
        row.setBackground(glassBg(false, dp(16)));
        TextView icon = text(iconText, 19, Color.rgb(183, 130, 255), true);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView title = text(titleText, 14, TEXT, false); title.setPadding(dp(8), 0, 0, 0);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text("›", 25, MUTED, false), new LinearLayout.LayoutParams(dp(28), dp(38)));
        row.setOnClickListener(v -> action.run());
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.bottomMargin = dp(5);
        wrapper.setLayoutParams(wp);
        return wrapper;
    }

    private TextView sheetAction(String label, boolean enabled) {
        TextView v = text(label, 10, enabled ? TEXT : MUTED2, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(glassBg(false, dp(16)));
        v.setEnabled(enabled);
        return v;
    }

    private View metricRow(String key, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(9), dp(11), dp(9));
        TextView k = text(key, 12, MUTED, false);
        TextView v = text(value, 13, valueColor, true); v.setGravity(Gravity.END);
        row.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setBackground(glassBg(false, dp(14)));
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(5);
        wrapper.setLayoutParams(p);
        return wrapper;
    }

    private TextView infoPill(String value) {
        TextView v = text(value, 11, MUTED, false);
        v.setPadding(dp(11), dp(9), dp(11), dp(9));
        v.setBackground(glassBg(false, dp(14)));
        return v;
    }

    private View buildEmptyCard(String titleText, String subText, int minHeight) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(24), dp(18), dp(24));
        card.setBackground(glassBg(false, dp(22)));
        card.setMinimumHeight(minHeight);
        card.addView(text("▱", 34, MUTED2, false));
        TextView title = text(titleText, 18, TEXT, true); title.setPadding(0, dp(10), 0, 0); card.addView(title);
        TextView sub = text(subText, 13, MUTED, false); sub.setGravity(Gravity.CENTER); sub.setPadding(0, dp(7), 0, 0); card.addView(sub);
        return card;
    }

    private TextView sectionLabel(String s) {
        TextView v = text(s, 10, MUTED, true);
        v.setPadding(dp(5), 0, dp(5), dp(7));
        return v;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.argb(34, 255, 255, 255));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(66), 0, dp(10), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private TextView iconButton(String s) {
        TextView v = text(s, 19, TEXT, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(glassBg(false, dp(22)));
        return v;
    }

    private TextView glassSmallButton(String s) {
        TextView v = text(s, 12, Color.rgb(205, 184, 255), true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), dp(7), dp(12), dp(7));
        v.setBackground(glassBg(false, dp(16)));
        return v;
    }

    private TextView glassWideButton(String s, boolean accent) {
        TextView v = text(s, 14, Color.WHITE, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(accent ? accentGlass(dp(18)) : glassBg(false, dp(18)));
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private GradientDrawable glassBg(boolean stronger, float radius) {
        int[] colors = stronger
                ? new int[]{Color.argb(118, 32, 35, 51), Color.argb(84, 61, 43, 95), Color.argb(98, 20, 24, 38)}
                : new int[]{Color.argb(96, 30, 33, 47), Color.argb(68, 41, 37, 60), Color.argb(82, 18, 22, 34)};
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), stronger ? GLASS_STROKE_STRONG : GLASS_STROKE);
        return d;
    }

    private GradientDrawable glassUnreadBg(float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(125, 46, 39, 71), Color.argb(88, 69, 44, 111), Color.argb(90, 24, 27, 43)});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), GLASS_STROKE_STRONG);
        return d;
    }

    private GradientDrawable accentGlass(float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(139, 79, 242), Color.rgb(105, 82, 244), Color.rgb(63, 111, 243)});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), Color.argb(165, 230, 220, 255));
        return d;
    }

    private GradientDrawable accentSoftBg(float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(110, 127, 76, 226), Color.argb(62, 69, 91, 213)});
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable sheetBg(float radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(31, 26, 48), Color.rgb(20, 22, 34), Color.rgb(24, 19, 39)});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), Color.rgb(178, 115, 255));
        return d;
    }

    private LinearLayout.LayoutParams weighted(float weight, int left, int right) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(left), 0, dp(right), 0);
        return lp;
    }

    private LinearLayout.LayoutParams equalLp(float weight, int margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(margin), 0, dp(margin), 0);
        return lp;
    }

    private View wrapFixed(View v, int height) {
        LinearLayout box = new LinearLayout(this);
        box.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        return box;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return v;
    }

    private void sortItems(List<HistoryStore.Item> items) {
        Comparator<HistoryStore.Item> c;
        if (sortMode == 1) c = (a, b) -> Integer.compare(b.saving, a.saving);
        else if (sortMode == 2) c = (a, b) -> Double.compare(b.discountPercent, a.discountPercent);
        else if (sortMode == 3) c = (a, b) -> Integer.compare(safePrice(a), safePrice(b));
        else if (sortMode == 4) c = (a, b) -> Integer.compare(safeProcessing(a), safeProcessing(b));
        else c = (a, b) -> Long.compare(b.timestamp, a.timestamp);
        Collections.sort(items, c);
    }

    private int safePrice(HistoryStore.Item i) {
        int p = i.effectivePrice > 0 ? i.effectivePrice : i.price;
        return p > 0 ? p : Integer.MAX_VALUE;
    }

    private int safeProcessing(HistoryStore.Item i) {
        return i.processingMs > 0 ? i.processingMs : Integer.MAX_VALUE;
    }

    private String sortLabel() {
        String[] names = {"Сначала новые", "По выгоде", "По %", "Цена ↑", "Скорость ↑"};
        return names[Math.max(0, Math.min(sortMode, names.length - 1))];
    }

    private int unreadCount() {
        int n = 0;
        for (HistoryStore.Item i : HistoryStore.get(this)) if (!i.read) n++;
        return n;
    }

    private boolean notificationsEnabled() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return Build.VERSION.SDK_INT < 24 || nm.areNotificationsEnabled();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
    }

    private void openNotificationSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        } catch (Exception e) {
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            toast("Не удалось открыть настройки Android");
        }
    }

    private void createDocument(int requestCode, String title) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, title);
        startActivityForResult(i, requestCode);
    }

    private void openDocument(int requestCode) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, requestCode);
    }

    private void writeUri(Uri uri, String value) throws Exception {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("output stream");
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private String readUri(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("input stream");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    private void openLink(String link) {
        if (link == null || link.isEmpty()) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))); }
        catch (Exception e) { toast("Не удалось открыть ссылку"); }
    }

    private void copyToClipboard(String value) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Post Finder", value));
        toast("Скопировано");
    }

    private void shareText(String value) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, value);
        startActivity(Intent.createChooser(i, "Поделиться находкой"));
    }

    private String detailsText(HistoryStore.Item item) {
        StringBuilder b = new StringBuilder();
        b.append(item.displayTitle()).append('\n');
        if (item.sourceTitle != null && !item.sourceTitle.isEmpty()) b.append("Источник: ").append(item.sourceTitle).append('\n');
        int effective = item.effectivePrice > 0 ? item.effectivePrice : item.price;
        if (effective > 0) b.append("Итог: ").append(money(effective)).append(" ₽\n");
        if (item.wbExtra > 0) b.append("Пошлина WB: +").append(money(item.wbExtra)).append(" ₽\n");
        if (item.saving > 0) b.append("Выгода: +").append(money(item.saving)).append(" ₽\n");
        if (item.link != null && !item.link.isEmpty()) b.append(item.link).append('\n');
        String full = item.postText != null && !item.postText.isEmpty() ? item.postText : item.text;
        if (full != null && !full.isEmpty()) b.append("\n").append(full);
        return b.toString().trim();
    }

    private String buildDiagnosticsText() {
        StringBuilder b = new StringBuilder(AppStateStore.diagnostics(this));
        b.append("app_version=").append(appVersion()).append('\n');
        b.append("android_api=").append(Build.VERSION.SDK_INT).append('\n');
        b.append("notifications=").append(notificationsEnabled()).append('\n');
        b.append("history_count=").append(HistoryStore.get(this).size()).append('\n');
        b.append("unread_count=").append(unreadCount()).append('\n');
        return b.toString();
    }

    private String diagnosticSummary() {
        long last = AppStateStore.lastSignalTs(this);
        return last > 0 ? "Последний сигнал • " + relativeTime(last) : "Ожидаем первый реальный сигнал";
    }

    private String relativeTime(long ts) {
        long sec = Math.max(0, (System.currentTimeMillis() - ts) / 1000L);
        if (sec < 60) return sec < 5 ? "только что" : sec + " сек назад";
        long min = sec / 60;
        if (min < 60) return min + " мин назад";
        long h = min / 60;
        if (h < 24) return h + " ч назад";
        long d = h / 24;
        return d + " дн назад";
    }

    private String emptyDash(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s;
    }

    private long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String formatDate(long ts) {
        return ts > 0 ? new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(ts)) : "—";
    }

    private String formatTime(long ts) {
        return ts > 0 ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts)) : "";
    }

    private String formatMs(int ms) {
        if (ms <= 0) return "—";
        if (ms < 1000) return ms + " мс";
        return String.format(Locale.getDefault(), "%.1f с", ms / 1000.0);
    }

    private String money(int value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value).replace(',', ' ');
    }

    private String timeStampFile() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(new Date());
    }

    private String appVersion() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return p.versionName == null ? "1.5" : p.versionName;
        } catch (Exception e) {
            return "1.5";
        }
    }

    private int appVersionCode() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? (int)p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 6;
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(UI_PREFS, MODE_PRIVATE);
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private static final class MenuSpec {
        final String icon;
        final String title;
        final String sub;
        final Runnable action;
        MenuSpec(String icon, String title, String sub, Runnable action) {
            this.icon = icon;
            this.title = title;
            this.sub = sub;
            this.action = action;
        }
    }
}
