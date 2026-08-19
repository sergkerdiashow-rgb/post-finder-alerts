package com.postfinder.alerts;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Post Finder Alerts companion v1.0
public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertReceiver.ensureChannel(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 72, 48, 48);
        root.setGravity(Gravity.TOP);

        TextView title = new TextView(this);
        title.setText("Post Finder Alerts");
        title.setTextSize(28f);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Отдельные системные уведомления для находок Post Finder WB.\nУведомления exteraGram можно оставить полностью заблокированными.");
        desc.setTextSize(17f);
        desc.setTextColor(0xffcccccc);
        desc.setPadding(0, 28, 0, 36);
        root.addView(desc);

        status = new TextView(this);
        status.setTextSize(18f);
        status.setPadding(0, 0, 0, 30);
        root.addView(status);

        Button allow = new Button(this);
        allow.setText("Разрешить уведомления");
        allow.setOnClickListener(v -> requestNotifications());
        root.addView(allow);

        Button test = new Button(this);
        test.setText("Тест уведомления");
        test.setOnClickListener(v -> AlertReceiver.showNotification(
                this,
                "🔥 Post Finder Alerts работает",
                "Тест отдельного уведомления. exteraGram может быть заблокирован.",
                "",
                1001));
        root.addView(test);

        Button settings = new Button(this);
        settings.setText("Открыть настройки уведомлений");
        settings.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });
        root.addView(settings);

        root.setBackgroundColor(0xff101114);
        setContentView(root);
        requestNotifications();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void refreshStatus() {
        if (status == null) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean enabled = Build.VERSION.SDK_INT < 24 || nm.areNotificationsEnabled();
        status.setText(enabled ? "✅ Уведомления разрешены" : "⛔ Уведомления запрещены");
        status.setTextColor(enabled ? 0xff5ee27a : 0xffff6b6b);
    }
}
