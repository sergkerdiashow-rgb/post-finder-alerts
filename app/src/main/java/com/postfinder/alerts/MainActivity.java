package com.postfinder.alerts;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 10, 15);
    private static final int TEXT = Color.rgb(248, 249, 253);
    private static final int MUTED = Color.rgb(160, 168, 184);
    private static final int ACCENT = Color.rgb(139, 92, 246);
    private static final int GREEN = Color.rgb(86, 226, 143);
    private static final int RED = Color.rgb(255, 112, 120);
    private static final int GOLD = Color.rgb(255, 205, 92);
    private static final int GLASS_STROKE = Color.argb(80, 255, 255, 255);
    private static final int GLASS_STROKE_STRONG = Color.argb(135, 190, 165, 255);
    private static final int PURPLE_TEXT = Color.rgb(199, 180, 255);

    private TextView statusText;
    private TextView todayValue;
    private TextView unreadValue;
    private TextView bestValue;
    private TextView historyTitle;
    private TextView emptyView;
    private TextView sortButton;
    private final ArrayList<TextView> filterChips = new ArrayList<>();
    private ListView historyList;
    private HistoryAdapter adapter;
    private EditText searchInput;

    private int filterMode = 0;
    private int sortMode = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertReceiver.ensureChannel(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(8), dp(16), dp(8));

        root.addView(buildCompactHeader());
        root.addView(space(8));
        root.addView(buildStatusPill());
        root.addView(space(8));
        root.addView(buildQuickStats());
        root.addView(space(8));
        root.addView(buildActionBar());
        root.addView(space(14));
        root.addView(buildHistoryHeader());
        root.addView(space(8));
        root.addView(buildFilters());
        root.addView(space(8));
        root.addView(buildSearch());
        root.addView(space(8));

        historyList = new ListView(this);
        historyList.setDivider(null);
        historyList.setDividerHeight(0);
        historyList.setSelector(android.R.color.transparent);
        historyList.setVerticalScrollBarEnabled(false);
        historyList.setClipToPadding(false);
        historyList.setPadding(0, 0, 0, dp(10));
        adapter = new HistoryAdapter(this);
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener((parent, view, position, id) -> {
            HistoryStore.Item item = adapter.getItem(position);
            if (item == null) return;
            HistoryStore.markRead(this, item.id, true);
            HistoryStore.Item fresh = HistoryStore.find(this, item.id);
            refreshHistory();
            showDetails(fresh == null ? item : fresh);
        });
        historyList.setOnItemLongClickListener((parent, view, position, id) -> {
            HistoryStore.Item item = adapter.getItem(position);
            if (item == null) return true;
            boolean favorite = HistoryStore.toggleFavorite(this, item.id);
            Toast.makeText(this, favorite ? "Добавлено в избранное" : "Удалено из избранного", Toast.LENGTH_SHORT).show();
            refreshHistory();
            return true;
        });

        emptyView = label("Пока ничего нет по этому фильтру", 14, MUTED, Gravity.CENTER);
        emptyView.setPadding(dp(12), dp(28), dp(12), dp(12));

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

    private View buildCompactHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(46));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_pf_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpLogo.setMarginEnd(dp(11));
        row.addView(logo, lpLogo);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("Post Finder", 21, TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView sub = label("Alerts • Liquid Glass", 11, MUTED, Gravity.START);
        sub.setPadding(0, dp(2), 0, 0);
        text.addView(title);
        text.addView(sub);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView version = label("1.3", 12, PURPLE_TEXT, Gravity.CENTER);
        version.setTypeface(Typeface.DEFAULT_BOLD);
        version.setPadding(dp(10), dp(6), dp(10), dp(6));
        version.setBackground(glassBg(false, dp(14)));
        row.addView(version);
        return row;
    }

    private View buildStatusPill() {
        LinearLayout pill = new LinearLayout(this);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(12), 0, dp(12), 0);
        pill.setBackground(glassBg(true, dp(16)));
        statusText = label("● Проверяем уведомления", 13, TEXT, Gravity.START);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        pill.addView(statusText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        return pill;
    }

    private View buildQuickStats() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout today = miniStat("СЕГОДНЯ", "0");
        todayValue = (TextView) today.getChildAt(1);
        LinearLayout unread = miniStat("НОВЫЕ", "0");
        unreadValue = (TextView) unread.getChildAt(1);
        LinearLayout best = miniStat("ЛУЧШАЯ", "—");
        bestValue = (TextView) best.getChildAt(1);

        addEqual(row, today, 0, 5);
        addEqual(row, unread, 5, 5);
        addEqual(row, best, 5, 0);
        return row;
    }

    private LinearLayout miniStat(String name, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), dp(8), dp(10), dp(8));
        box.setBackground(glassBg(false, dp(17)));
        TextView n = label(name, 10, MUTED, Gravity.START);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        TextView v = label(value, 17, TEXT, Gravity.START);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(2), 0, 0);
        box.addView(n);
        box.addView(v);
        box.setMinimumHeight(dp(58));
        return box;
    }

    private View buildActionBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView test = glassButton("⚡ Тест", true);
        test.setOnClickListener(v -> AlertReceiver.showNotification(
                this, "Post Finder Alerts", "Тест уведомления • всё работает", "", 13001));

        TextView stats = glassButton("◫ Статистика", false);
        stats.setOnClickListener(v -> showStats());

        TextView settingsBtn = glassButton("⚙ Ещё", false);
        settingsBtn.setOnClickListener(v -> showMoreMenu());

        addEqual(row, test, 0, 5);
        addEqual(row, stats, 5, 5);
        addEqual(row, settingsBtn, 5, 0);
        return row;
    }

    private View buildHistoryHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        historyTitle = label("Находки", 19, TEXT, Gravity.START);
        historyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(historyTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        sortButton = label("Сначала новые ▾", 12, PURPLE_TEXT, Gravity.CENTER);
        sortButton.setTypeface(Typeface.DEFAULT_BOLD);
        sortButton.setPadding(dp(10), dp(7), dp(10), dp(7));
        sortButton.setBackground(glassBg(false, dp(13)));
        sortButton.setOnClickListener(v -> showSortDialog());
        row.addView(sortButton);
        return row;
    }

    private View buildFilters() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Все", "Сегодня", "WB", "★ Избранное", "● Новые"};
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            TextView chip = label(labels[i], 12, MUTED, Gravity.CENTER);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setMinHeight(dp(36));
            chip.setOnClickListener(v -> {
                filterMode = mode;
                refreshHistory();
            });
            filterChips.add(chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            if (i > 0) lp.setMarginStart(dp(7));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private View buildSearch() {
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("⌕  Поиск товара, источника, текста");
        searchInput.setTextSize(13);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setBackground(glassBg(false, dp(16)));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                refreshHistory();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        return searchInput;
    }

    private void refreshAll() {
        refreshStatus();
        refreshHistory();
    }

    private void refreshStatus() {
        if (statusText == null) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean enabled = Build.VERSION.SDK_INT < 24 || nm.areNotificationsEnabled();
        statusText.setText(enabled ? "● Уведомления активны" : "● Уведомления отключены");
        statusText.setTextColor(enabled ? GREEN : RED);
    }

    private void refreshHistory() {
        if (adapter == null) return;
        List<HistoryStore.Item> all = HistoryStore.get(this);
        long start = startOfToday();

        int todayCount = 0;
        int unreadCount = 0;
        int bestSaving = 0;
        for (HistoryStore.Item item : all) {
            if (item.timestamp >= start) todayCount++;
            if (!item.read) unreadCount++;
            if (item.saving > bestSaving) bestSaving = item.saving;
        }
        if (todayValue != null) todayValue.setText(String.valueOf(todayCount));
        if (unreadValue != null) unreadValue.setText(String.valueOf(unreadCount));
        if (bestValue != null) bestValue.setText(bestSaving > 0 ? money(bestSaving) + " ₽" : "—");
        if (historyTitle != null) historyTitle.setText("Находки  " + all.size());

        ArrayList<HistoryStore.Item> visible = new ArrayList<>();
        for (HistoryStore.Item item : all) {
            if (filterMode == 1 && item.timestamp < start) continue;
            if (filterMode == 2 && !item.isWildberries()) continue;
            if (filterMode == 3 && !item.favorite) continue;
            if (filterMode == 4 && item.read) continue;
            if (!searchQuery.isEmpty() && !item.searchableText().contains(searchQuery)) continue;
            visible.add(item);
        }
        sortItems(visible);
        adapter.setItems(visible);
        emptyView.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        historyList.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        styleFilters();
    }

    private void sortItems(ArrayList<HistoryStore.Item> items) {
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

    private void styleFilters() {
        for (int i = 0; i < filterChips.size(); i++) {
            TextView chip = filterChips.get(i);
            boolean selected = i == filterMode;
            chip.setTextColor(selected ? Color.WHITE : MUTED);
            chip.setBackground(selected ? accentGlass(dp(14)) : glassBg(false, dp(14)));
        }
    }

    private void showSortDialog() {
        String[] names = {"Сначала новые", "Максимальная выгода", "Максимальный %", "Минимальная цена", "Самая быстрая обработка"};
        new AlertDialog.Builder(this)
                .setTitle("Сортировка")
                .setSingleChoiceItems(names, sortMode, (dialog, which) -> {
                    sortMode = which;
                    if (sortButton != null) sortButton.setText(names[which] + " ▾");
                    dialog.dismiss();
                    refreshHistory();
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showStats() {
        List<HistoryStore.Item> all = HistoryStore.get(this);
        long start = startOfToday();
        int today = 0, wb = 0, fav = 0, unread = 0, withSaving = 0;
        long totalSaving = 0, totalProcessing = 0;
        int best = 0, procCount = 0, fastest = Integer.MAX_VALUE;
        double bestPct = 0.0;

        for (HistoryStore.Item i : all) {
            if (i.timestamp >= start) today++;
            if (i.isWildberries()) wb++;
            if (i.favorite) fav++;
            if (!i.read) unread++;
            if (i.saving > 0) {
                withSaving++;
                totalSaving += i.saving;
                best = Math.max(best, i.saving);
            }
            bestPct = Math.max(bestPct, i.discountPercent);
            if (i.processingMs > 0) {
                procCount++;
                totalProcessing += i.processingMs;
                fastest = Math.min(fastest, i.processingMs);
            }
        }

        String avgSaving = withSaving > 0 ? money((int)(totalSaving / withSaving)) + " ₽" : "—";
        String avgProc = procCount > 0 ? (totalProcessing / procCount) + " мс" : "—";
        String fastestText = fastest != Integer.MAX_VALUE ? fastest + " мс" : "—";

        String body =
                "Всего находок: " + all.size() +
                "\nСегодня: " + today +
                "\nНепрочитано: " + unread +
                "\nИзбранное: " + fav +
                "\nWildberries: " + wb +
                "\n\nСуммарная выгода: " + moneyLong(totalSaving) + " ₽" +
                "\nСредняя выгода: " + avgSaving +
                "\nЛучшая выгода: " + (best > 0 ? money(best) + " ₽" : "—") +
                "\nЛучший процент: " + (bestPct > 0 ? String.format(Locale.getDefault(), "%.1f%%", bestPct) : "—") +
                "\n\nСредняя обработка: " + avgProc +
                "\nСамая быстрая: " + fastestText;

        new AlertDialog.Builder(this)
                .setTitle("Статистика Post Finder")
                .setMessage(body)
                .setNeutralButton("Поделиться", (d, w) -> shareText("Статистика Post Finder\n\n" + body))
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showMoreMenu() {
        String[] actions = {
                "Настройки уведомлений",
                "Отметить всё прочитанным",
                "Очистить историю",
                "О приложении"
        };
        new AlertDialog.Builder(this)
                .setTitle("Post Finder Alerts")
                .setItems(actions, (d, which) -> {
                    if (which == 0) openNotificationSettings();
                    else if (which == 1) {
                        HistoryStore.markAllRead(this);
                        refreshHistory();
                        Toast.makeText(this, "Все отмечено прочитанным", Toast.LENGTH_SHORT).show();
                    } else if (which == 2) confirmClear();
                    else showAbout();
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Post Finder Alerts 1.3")
                .setMessage("Локальная история находок Post Finder WB.\n\n" +
                        "• до 1000 записей\n" +
                        "• поиск, фильтры и сортировка\n" +
                        "• избранное и непрочитанные\n" +
                        "• статистика и WB-метрики\n" +
                        "• полный исходный пост\n\n" +
                        "Данные истории хранятся только на устройстве.")
                .setPositiveButton("Готово", null)
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Очистить историю?")
                .setMessage("Удалятся локальные записи Post Finder Alerts. Настройки уведомлений не изменятся.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Очистить", (d, w) -> {
                    HistoryStore.clear(this);
                    refreshHistory();
                    Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showDetails(HistoryStore.Item item) {
        if (item == null) return;
        String details = buildDetails(item);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle((item.favorite ? "★ " : "") + item.displayTitle())
                .setMessage(details)
                .setNegativeButton("Закрыть", null)
                .setNeutralButton("Действия", (d, w) -> showItemActions(item.id))
                .setPositiveButton(item.link.isEmpty() ? "Готово" : "Открыть", (d, w) -> {
                    if (!item.link.isEmpty()) openLink(item.link);
                })
                .create();
        dialog.setOnShowListener(d -> {
            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) {
                message.setTextColor(Color.rgb(224, 227, 236));
                message.setTextSize(14);
                message.setTextIsSelectable(true);
            }
        });
        dialog.show();
    }

    private String buildDetails(HistoryStore.Item item) {
        StringBuilder b = new StringBuilder();
        b.append(formatDate(item.timestamp));
        if (!item.sourceTitle.isEmpty()) b.append("\nИсточник: ").append(item.sourceTitle);
        if (!item.productType.isEmpty()) b.append("\nТип: ").append(item.productType);
        if (item.effectivePrice > 0) b.append("\n\nИтоговая цена: ").append(money(item.effectivePrice)).append(" ₽");
        if (item.price > 0 && item.price != item.effectivePrice) b.append("\nЦена товара: ").append(money(item.price)).append(" ₽");
        if (item.wbExtra > 0) b.append("\nПошлина WB: +").append(money(item.wbExtra)).append(" ₽");
        if (item.referencePrice > 0) b.append("\nПрайс: ").append(money(item.referencePrice)).append(" ₽");
        if (item.limit > 0) b.append("\nЛимит: ").append(money(item.limit)).append(" ₽");
        if (item.saving > 0) b.append("\nВыгода: ").append(money(item.saving)).append(" ₽");
        if (item.discountPercent > 0) b.append(String.format(Locale.getDefault(), " (%.1f%%)", item.discountPercent));
        if (item.processingMs > 0) b.append("\nОбработка: ").append(item.processingMs).append(" мс");
        if (!item.text.isEmpty()) b.append("\n\nУведомление:\n").append(item.text);
        if (!item.postText.isEmpty()) b.append("\n\nИсходный пост полностью:\n").append(item.postText);
        if (!item.link.isEmpty()) b.append("\n\nСсылка:\n").append(item.link);
        return b.toString();
    }

    private void showItemActions(String itemId) {
        HistoryStore.Item item = HistoryStore.find(this, itemId);
        if (item == null) return;
        String fav = item.favorite ? "☆ Убрать из избранного" : "★ В избранное";
        String read = item.read ? "● Сделать непрочитанным" : "○ Отметить прочитанным";
        String[] actions = {fav, read, "Копировать всё", "Поделиться", "Открыть источник", "Удалить запись"};
        new AlertDialog.Builder(this)
                .setTitle(item.displayTitle())
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        HistoryStore.toggleFavorite(this, itemId);
                        refreshHistory();
                    } else if (which == 1) {
                        HistoryStore.markRead(this, itemId, !item.read);
                        refreshHistory();
                    } else if (which == 2) {
                        copy(buildDetails(item));
                    } else if (which == 3) {
                        shareText(item.displayTitle() + "\n\n" + buildDetails(item));
                    } else if (which == 4) {
                        if (item.link.isEmpty()) Toast.makeText(this, "Ссылки нет", Toast.LENGTH_SHORT).show();
                        else openLink(item.link);
                    } else {
                        confirmDelete(itemId, item.displayTitle());
                    }
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void confirmDelete(String itemId, String title) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить запись?")
                .setMessage(title)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> {
                    HistoryStore.delete(this, itemId);
                    refreshHistory();
                })
                .show();
    }

    private void openNotificationSettings() {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }

    private void openLink(String link) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))); }
        catch (Exception e) { Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show(); }
    }

    private void shareText(String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Поделиться"));
    }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Post Finder", text));
        Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void addEqual(LinearLayout row, View view, int left, int right) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(left), 0, dp(right), 0);
        row.addView(view, lp);
    }

    private TextView glassButton(String text, boolean accent) {
        TextView v = label(text, 12, Color.WHITE, Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setBackground(accent ? accentGlass(dp(15)) : glassBg(false, dp(15)));
        v.setMinHeight(dp(42));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private GradientDrawable glassBg(boolean strong, float radius) {
        int top = strong ? Color.argb(175, 46, 41, 66) : Color.argb(150, 32, 35, 47);
        int bottom = strong ? Color.argb(145, 26, 30, 43) : Color.argb(105, 22, 25, 35);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), strong ? GLASS_STROKE_STRONG : GLASS_STROKE);
        d.setDither(true);
        return d;
    }

    private GradientDrawable accentGlass(float radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(235, 151, 93, 255), Color.argb(210, 96, 126, 255)});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), Color.argb(175, 255, 255, 255));
        d.setDither(true);
        return d;
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

    private View space(int value) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(value)));
        return v;
    }

    private String formatDate(long ts) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(ts));
    }

    private String money(int value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    private String moneyLong(long value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    private long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class HistoryAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<HistoryStore.Item> items = new ArrayList<>();
        private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

        HistoryAdapter(Context context) { this.context = context; }
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
                outer.setPadding(0, 0, 0, dp(8));

                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(11));
                card.setBackground(glassBg(false, dp(18)));

                LinearLayout top = new LinearLayout(context);
                top.setGravity(Gravity.CENTER_VERTICAL);

                TextView dot = label("●", 10, ACCENT, Gravity.CENTER);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(16), ViewGroup.LayoutParams.WRAP_CONTENT);
                top.addView(dot, dotLp);

                TextView title = label("", 15, TEXT, Gravity.START);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                title.setMaxLines(2);
                top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView star = label("", 16, GOLD, Gravity.CENTER);
                star.setPadding(dp(6), 0, dp(3), 0);
                top.addView(star);

                TextView time = label("", 11, MUTED, Gravity.END);
                LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                timeLp.setMarginStart(dp(6));
                top.addView(time, timeLp);

                LinearLayout badges = new LinearLayout(context);
                badges.setOrientation(LinearLayout.HORIZONTAL);
                badges.setPadding(0, dp(7), 0, 0);
                TextView price = badge("");
                TextView saving = badge("");
                TextView wb = badge("WB");
                badges.addView(price);
                badges.addView(saving);
                badges.addView(wb);

                TextView body = label("", 12, Color.rgb(213, 217, 227), Gravity.START);
                body.setPadding(0, dp(7), 0, 0);
                body.setMaxLines(2);

                card.addView(top);
                card.addView(badges);
                card.addView(body);
                outer.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                h = new Holder(title, time, dot, star, price, saving, wb, body);
                outer.setTag(h);
                convertView = outer;
            } else {
                h = (Holder) convertView.getTag();
            }

            HistoryStore.Item item = getItem(position);
            h.title.setText(item.displayTitle());
            h.time.setText(isToday(item.timestamp) ? timeFmt.format(new Date(item.timestamp)) : dateFmt.format(new Date(item.timestamp)));
            h.dot.setVisibility(item.read ? View.INVISIBLE : View.VISIBLE);
            h.star.setText(item.favorite ? "★" : "");

            int p = item.effectivePrice > 0 ? item.effectivePrice : item.price;
            setBadge(h.price, p > 0 ? money(p) + " ₽" : "", true);
            setBadge(h.saving, item.saving > 0 ? "−" + money(item.saving) + " ₽" : "", false);
            setBadge(h.wb, item.isWildberries() ? "WB" : "", false);

            String body = item.sourceTitle;
            if (!item.text.isEmpty()) body += (body.isEmpty() ? "" : "\n") + item.text;
            h.body.setText(body);
            h.body.setVisibility(body.isEmpty() ? View.GONE : View.VISIBLE);
            return convertView;
        }

        private TextView badge(String text) {
            TextView v = label(text, 11, PURPLE_TEXT, Gravity.CENTER);
            v.setTypeface(Typeface.DEFAULT_BOLD);
            v.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            v.setLayoutParams(lp);
            v.setBackground(glassBg(false, dp(10)));
            return v;
        }

        private void setBadge(TextView v, String text, boolean primary) {
            v.setText(text);
            v.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            v.setTextColor(primary ? Color.WHITE : PURPLE_TEXT);
        }

        private boolean isToday(long ts) { return ts >= startOfToday(); }
    }

    private static final class Holder {
        final TextView title, time, dot, star, price, saving, wb, body;
        Holder(TextView title, TextView time, TextView dot, TextView star,
               TextView price, TextView saving, TextView wb, TextView body) {
            this.title = title;
            this.time = time;
            this.dot = dot;
            this.star = star;
            this.price = price;
            this.saving = saving;
            this.wb = wb;
            this.body = body;
        }
    }
}
