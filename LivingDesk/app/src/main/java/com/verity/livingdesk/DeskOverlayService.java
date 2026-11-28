package com.verity.livingdesk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class DeskOverlayService extends Service {
    public static final String ACTION_SHOW_DOCK = "com.verity.livingdesk.SHOW_DOCK";
    public static final String ACTION_SHOW_PET = "com.verity.livingdesk.SHOW_PET";
    public static final String ACTION_PET = "com.verity.livingdesk.CHANGE_PET";
    public static final String ACTION_REFRESH = "com.verity.livingdesk.REFRESH";
    public static final String ACTION_STOP = "com.verity.livingdesk.STOP";
    public static final String EXTRA_PET_TYPE = "pet_type";
    private static final String CHANNEL = "living_desk_overlay";

    private WindowManager windowManager;
    private LinearLayout dockView;
    private PetView petView;
    private WindowManager.LayoutParams dockParams;
    private WindowManager.LayoutParams petParams;
    private boolean showDock = false;
    private boolean showPet = false;
    private int petType = 0;
    private boolean glass = true;

    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        glass = getSharedPreferences("living_desk_settings", MODE_PRIVATE).getBoolean("glass", true);
        petType = getSharedPreferences("living_desk_settings", MODE_PRIVATE).getInt("pet_type", 0);
        getSharedPreferences("living_desk_settings", MODE_PRIVATE).edit().putBoolean("overlay_enabled", true).apply();
        createChannel();
        startAsForeground();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Живой стол", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Плавающий док и питомец");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void startAsForeground() {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Живой стол работает")
                .setContentText("Нажми, чтобы открыть настройки плавающего дока")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(61, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(61, notification);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (intent.hasExtra(EXTRA_PET_TYPE)) petType = intent.getIntExtra(EXTRA_PET_TYPE, petType);
            if (ACTION_REFRESH.equals(action)) {
                glass = getSharedPreferences("living_desk_settings", MODE_PRIVATE).getBoolean("glass", true);
                removeDock();
                removePet();
                if (showDock) addDock();
                if (showPet) addPet();
            } else if (ACTION_SHOW_PET.equals(action) || ACTION_PET.equals(action)) {
                showPet = true;
                addPet();
            } else if (ACTION_SHOW_DOCK.equals(action)) {
                showDock = true;
                showPet = true;
                addDock();
                addPet();
            }
        }
        return START_NOT_STICKY;
    }

    private void addDock() {
        if (dockView != null || !canDraw()) return;
        dockView = new LinearLayout(this);
        dockView.setOrientation(LinearLayout.HORIZONTAL);
        dockView.setGravity(Gravity.CENTER);
        dockView.setPadding(dp(9), dp(7), dp(9), dp(7));
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                glass ? new int[]{0xE52B3E66, 0xD9111B30} : new int[]{0xFF24304A, 0xFF111827});
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), glass ? 0xAA9CB7FF : 0xFF53658F);
        dockView.setBackground(bg);

        addDockItem("⌂", "Домой", () -> openApp());
        addDockItem("❄", "Сезон", () -> openApp());
        addDockItem("🐾", "Питомец", () -> {
            if (petView != null) petView.performClick();
            Toast.makeText(this, "Питомец ждёт касания", Toast.LENGTH_SHORT).show();
        });
        addDockItem("🎮", "Игры", () -> openApp());
        addDockItem("×", "Скрыть", () -> {
            showDock = false;
            removeDock();
            if (!showPet) stopSelf();
        });

        dockParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT);
        dockParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        dockParams.y = dp(26);
        try {
            windowManager.addView(dockView, dockParams);
        } catch (Exception e) {
            dockView = null;
            Toast.makeText(this, "Не удалось показать док. Проверь разрешение поверх окон.", Toast.LENGTH_LONG).show();
        }
    }

    private void addDockItem(String icon, String label, Runnable action) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(8), dp(1), dp(8), dp(1));
        TextView glyph = new TextView(this);
        glyph.setText(icon);
        glyph.setTextSize(20);
        glyph.setTextColor(Color.WHITE);
        glyph.setGravity(Gravity.CENTER);
        cell.addView(glyph);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextSize(8);
        caption.setTextColor(0xFFDCE5FF);
        caption.setGravity(Gravity.CENTER);
        cell.addView(caption);
        cell.setOnClickListener(v -> action.run());
        dockView.addView(cell, new LinearLayout.LayoutParams(-2, -2));
    }

    private void addPet() {
        if (petView != null || !canDraw()) return;
        petView = new PetView(this);
        petView.setPetType(petType);
        petView.setOnClickListener(v -> {
            petView.cycleMood();
            Toast.makeText(this, "Мяу! Питомец рад тебя видеть.", Toast.LENGTH_SHORT).show();
        });
        petParams = new WindowManager.LayoutParams(
                dp(78), dp(78),
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT);
        petParams.gravity = Gravity.TOP | Gravity.RIGHT;
        petParams.x = dp(18);
        petParams.y = dp(170);
        attachPetDrag();
        try {
            windowManager.addView(petView, petParams);
        } catch (Exception e) {
            petView = null;
            Toast.makeText(this, "Не удалось показать питомца", Toast.LENGTH_SHORT).show();
        }
    }

    private void attachPetDrag() {
        petView.setOnTouchListener(new View.OnTouchListener() {
            float downRawX, downRawY;
            int startX, startY;
            long downAt;
            boolean moved;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = petParams.x;
                        startY = petParams.y;
                        downAt = System.currentTimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.abs(dx) + Math.abs(dy) > dp(5)) moved = true;
                        petParams.x = Math.max(0, startX - (int) dx);
                        petParams.y = Math.max(0, startY + (int) dy);
                        try { windowManager.updateViewLayout(petView, petParams); } catch (Exception ignored) { }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - downAt < 500L) {
                            Toast.makeText(DeskOverlayService.this, "Мяу! Погладь меня ещё раз.", Toast.LENGTH_SHORT).show();
                            petView.performClick();
                        }
                        return true;
                }
                return true;
            }
        });
    }

    private void openApp() {
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } catch (Exception ignored) { }
    }

    private boolean canDraw() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void removeDock() {
        if (dockView != null) {
            try { windowManager.removeView(dockView); } catch (Exception ignored) { }
            dockView = null;
        }
    }

    private void removePet() {
        if (petView != null) {
            try { windowManager.removeView(petView); } catch (Exception ignored) { }
            petView = null;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroy() {
        removeDock();
        removePet();
        getSharedPreferences("living_desk_settings", MODE_PRIVATE).edit().putBoolean("overlay_enabled", false).apply();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
