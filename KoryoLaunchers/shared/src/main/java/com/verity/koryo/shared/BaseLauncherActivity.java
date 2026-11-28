package com.verity.koryo.shared;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public abstract class BaseLauncherActivity extends Activity {
    private static final String PREFS = "koryo_launcher_prefs";
    private static final String KEY_KOREAN = "korean";
    private static final int COLUMNS = 4;

    private final ArrayList<ResolveInfo> allApps = new ArrayList<>();
    private LinearLayout appGrid;
    private EditText searchBox;
    private WallpaperView wallpaper;
    private TextView clockText;
    private TextView dateText;
    private TextView batteryText;
    private boolean korean;
    private int wallpaperMode = 0;
    private float touchStartY = -1;
    private Dialog quickDialog;

    protected abstract boolean isClassic();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        korean = prefs.getBoolean(KEY_KOREAN, false);

        getWindow().setStatusBarColor(isClassic() ? Color.rgb(5, 18, 42) : Color.rgb(0, 40, 76));
        getWindow().setNavigationBarColor(isClassic() ? Color.rgb(4, 13, 30) : Color.rgb(0, 34, 68));
        getWindow().getDecorView().setSystemUiVisibility(0);

        buildLauncher();
        loadApps();
        renderApps("");
        updateClock();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return v;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable outlined(int color, int stroke, int radiusDp) {
        GradientDrawable d = rounded(color, radiusDp);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void buildLauncher() {
        wallpaper = new WallpaperView();
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.addView(wallpaper, new android.widget.FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(10), dp(18), dp(10));
        frame.addView(page, new android.widget.FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label(isClassic() ? "KORYO  ·  조선" : "KORYO  ·  푸른 화면", 15, Color.WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(36), 1));

        TextView language = label(korean ? "한" : "РУ", 12, Color.WHITE, true);
        language.setGravity(Gravity.CENTER);
        language.setBackground(rounded(0x553FFFFFF, 14));
        top.addView(language, new LinearLayout.LayoutParams(dp(42), dp(34)));
        language.setOnClickListener(v -> {
            korean = !korean;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_KOREAN, korean).apply();
            recreate();
        });

        TextView quick = label("☷", 21, Color.WHITE, true);
        quick.setGravity(Gravity.CENTER);
        quick.setBackground(rounded(0x553FFFFFF, 14));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(42), dp(34));
        qp.leftMargin = dp(8);
        top.addView(quick, qp);
        quick.setOnClickListener(v -> showQuickPanel());

        TextView menu = label("⋮", 22, Color.WHITE, true);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(rounded(0x553FFFFFF, 14));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(36), dp(34));
        mp.leftMargin = dp(7);
        top.addView(menu, mp);
        menu.setOnClickListener(v -> showMenu());
        page.addView(top);

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setPadding(dp(3), dp(12), dp(3), dp(8));
        clockText = label("12:00", isClassic() ? 48 : 46, Color.WHITE, false);
        clockText.setLetterSpacing(0.02f);
        dateText = label("", 13, 0xFFE0EEFF, false);
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateRow.addView(dateText, new LinearLayout.LayoutParams(0, dp(28), 1));
        batteryText = label(batteryPercent() + "%  ▰", 12, 0xFFE0EEFF, true);
        dateRow.addView(batteryText);
        clockBox.addView(clockText);
        clockBox.addView(dateRow);
        page.addView(clockBox);

        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(13), dp(11), dp(13), dp(11));
        hero.setBackground(rounded(isClassic() ? 0xCC0B2850 : 0xCC07517E, 20));
        FlagView flag = new FlagView();
        hero.addView(flag, new LinearLayout.LayoutParams(dp(65), dp(43)));
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.setPadding(dp(12), 0, 0, 0);
        heroText.addView(label(isClassic() ? (korean ? "환영합니다" : "Главный экран") :
                (korean ? "푸른 화면" : "Голубой экран"), 15, Color.WHITE, true));
        heroText.addView(label(isClassic() ? "조선풍 아이콘 · 클래식" :
                "파란 타일 · 빠른 메뉴", 11, 0xFFD5E9FF, false));
        hero.addView(heroText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView wallpaperButton = label("◈", 20, Color.WHITE, true);
        wallpaperButton.setGravity(Gravity.CENTER);
        wallpaperButton.setBackground(rounded(0x553FFFFFF, 14));
        hero.addView(wallpaperButton, new LinearLayout.LayoutParams(dp(42), dp(42)));
        wallpaperButton.setOnClickListener(v -> {
            wallpaperMode = (wallpaperMode + 1) % 3;
            wallpaper.invalidate();
            Toast.makeText(this, korean ? "배경 변경" : "Обои изменены", Toast.LENGTH_SHORT).show();
        });
        hero.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchStartY = event.getY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dy = event.getY() - touchStartY;
                if (dy > dp(24)) showQuickPanel();
                touchStartY = -1;
                return true;
            }
            return true;
        });
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(68));
        hp.topMargin = dp(5);
        page.addView(hero, hp);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(14);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setHintTextColor(0xBBD8E9FF);
        searchBox.setHint(korean ? "앱 검색" : "Поиск приложений");
        searchBox.setPadding(dp(15), 0, dp(15), 0);
        searchBox.setBackground(rounded(isClassic() ? 0x66234266 : 0x663B83A9, 15));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(43));
        sp.topMargin = dp(12);
        page.addView(searchBox, sp);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { renderApps(s.toString()); }
            @Override public void afterTextChanged(Editable e) {}
        });

        LinearLayout appsHeader = new LinearLayout(this);
        appsHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView appsTitle = label(korean ? "앱 목록" : "Приложения", 14, Color.WHITE, true);
        appsHeader.addView(appsTitle, new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView appCount = label(korean ? "전체 앱" : "ВСЕ ПРИЛОЖЕНИЯ", 9, 0xFFD9E9F7, true);
        appsHeader.addView(appCount);
        LinearLayout.LayoutParams ah = new LinearLayout.LayoutParams(-1, dp(38));
        ah.topMargin = dp(3);
        page.addView(appsHeader, ah);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appGrid, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(5), dp(6), dp(5), dp(6));
        dock.setBackground(rounded(isClassic() ? 0xAA071A35 : 0xAA03446F, 22));
        addDockItem(dock, "☎", korean ? "전화" : "Звонок", () -> launchAction(new Intent(Intent.ACTION_DIAL)));
        addDockItem(dock, "▣", korean ? "카메라" : "Камера", () -> launchAction(new Intent("android.media.action.IMAGE_CAPTURE")));
        addDockItem(dock, "▦", korean ? "달력" : "Календарь", () -> {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_APP_CALENDAR);
            launchAction(i);
        });
        addDockItem(dock, "⚙", korean ? "설정" : "Настройки", () -> launchAction(new Intent(Settings.ACTION_SETTINGS)));
        LinearLayout.LayoutParams dpDock = new LinearLayout.LayoutParams(-1, dp(61));
        dpDock.topMargin = dp(9);
        page.addView(dock, dpDock);

        TextView footer = label(korean ? "홈 화면  ·  안드로이드" : "Главный экран  ·  Android", 9, 0xAADDFFFFFF, false);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, new LinearLayout.LayoutParams(-1, dp(19)));

        setContentView(frame);
    }

    private void addDockItem(LinearLayout dock, String icon, String title, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER);
        item.setOrientation(LinearLayout.VERTICAL);
        TextView symbol = label(icon, 22, Color.WHITE, true);
        symbol.setGravity(Gravity.CENTER);
        TextView caption = label(title, 9, 0xFFEAF4FF, false);
        caption.setGravity(Gravity.CENTER);
        item.addView(symbol, new LinearLayout.LayoutParams(-1, dp(31)));
        item.addView(caption, new LinearLayout.LayoutParams(-1, dp(17)));
        item.setOnClickListener(v -> action.run());
        dock.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void updateClock() {
        Date now = new Date();
        clockText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        Locale locale = korean ? Locale.KOREAN : new Locale("ru", "RU");
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", locale).format(now));
        batteryText.setText(batteryPercent() + "%  ▰");
        clockText.postDelayed(this::updateClock, 30000);
    }

    private int batteryPercent() {
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent status = registerReceiver(null, filter);
            if (status != null) {
                int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (level >= 0 && scale > 0) return Math.round(level * 100f / scale);
            }
        } catch (Exception ignored) {}
        return 100;
    }

    private void loadApps() {
        allApps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(query, 0);
        for (ResolveInfo info : found) {
            if (info.activityInfo == null) continue;
            if (getPackageName().equals(info.activityInfo.packageName)) continue;
            allApps.add(info);
        }
        Collections.sort(allApps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(
                        b.loadLabel(getPackageManager()).toString());
            }
        });
    }

    private void renderApps(String filter) {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        ArrayList<ResolveInfo> visible = new ArrayList<>();
        for (ResolveInfo app : allApps) {
            String name = app.loadLabel(getPackageManager()).toString();
            if (needle.isEmpty() || name.toLowerCase(Locale.ROOT).contains(needle)
                    || app.activityInfo.packageName.toLowerCase(Locale.ROOT).contains(needle)) {
                visible.add(app);
            }
        }
        if (visible.isEmpty()) {
            TextView empty = label(korean ? "앱을 찾을 수 없습니다" : "Приложения не найдены", 13, Color.WHITE, false);
            empty.setGravity(Gravity.CENTER);
            appGrid.addView(empty, new LinearLayout.LayoutParams(-1, dp(88)));
            return;
        }
        for (int i = 0; i < visible.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            for (int j = i; j < Math.min(i + COLUMNS, visible.size()); j++) {
                row.addView(createAppTile(visible.get(j)), new LinearLayout.LayoutParams(0, dp(92), 1));
            }
            appGrid.addView(row, new LinearLayout.LayoutParams(-1, dp(96)));
        }
    }

    private View createAppTile(ResolveInfo info) {
        LinearLayout tile = new LinearLayout(this);
        tile.setGravity(Gravity.CENTER);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(2), dp(4), dp(2), dp(3));

        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(info.loadIcon(getPackageManager())); }
        catch (Exception ignored) {}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setBackground(rounded(isClassic() ? 0x443E79A9 : 0x44C9F0FF, 14));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(47), dp(47)));

        String appName = info.loadLabel(getPackageManager()).toString();
        TextView name = label(appName, 10, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(35));
        np.topMargin = dp(3);
        tile.addView(name, np);

        tile.setOnClickListener(v -> {
            try {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } catch (Exception e) {
                Toast.makeText(this, korean ? "앱을 열 수 없습니다" : "Не удалось открыть приложение", Toast.LENGTH_SHORT).show();
            }
        });
        tile.setOnLongClickListener(v -> {
            try {
                Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                details.setData(Uri.parse("package:" + info.activityInfo.packageName));
                startActivity(details);
            } catch (Exception ignored) {}
            return true;
        });
        return tile;
    }

    private void showQuickPanel() {
        if (quickDialog != null && quickDialog.isShowing()) return;
        Dialog dialog = new Dialog(this);
        quickDialog = dialog;

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(22), dp(20), dp(18));
        panel.setBackground(rounded(isClassic() ? 0xFF071A37 : 0xFF052E55, 0));
        TextView heading = label(korean ? "빠른 설정" : "Быстрая панель", 23, Color.WHITE, true);
        panel.addView(heading);
        TextView sub = label(korean ? "기기 설정 바로가기" : "Ярлыки настоящих настроек Android", 12, 0xFFD5E7F7, false);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-1, dp(28));
        panel.addView(sub, subp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        addQuickTile(grid, "◉", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        addQuickTile(grid, "ᛒ", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        addQuickTile(grid, "♫", korean ? "소리" : "Звук", Settings.ACTION_SOUND_SETTINGS);
        addQuickTile(grid, "☼", korean ? "화면" : "Экран", Settings.ACTION_DISPLAY_SETTINGS);
        addQuickTile(grid, "⌖", korean ? "위치" : "Геоданные", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        addQuickTile(grid, "⚙", korean ? "설정" : "Все настройки", Settings.ACTION_SETTINGS);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = dp(10);
        panel.addView(grid, gp);

        TextView note = label(korean ? "각 버튼은 실제 Android 설정을 엽니다." :
                "Кнопки открывают системные разделы Android. Визуальный стиль панели — макет.", 10, 0xFFB7D1E6, false);
        note.setPadding(0, dp(10), 0, dp(10));
        panel.addView(note);

        TextView close = label(korean ? "닫기" : "Закрыть", 14, Color.WHITE, true);
        close.setGravity(Gravity.CENTER);
        close.setBackground(rounded(0xFF1777B8, 15));
        panel.addView(close, new LinearLayout.LayoutParams(-1, dp(44)));
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.TOP);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.28f;
            w.setAttributes(lp);
        }
        dialog.setCancelable(true);
        dialog.show();
        if (w != null) w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void addQuickTile(GridLayout grid, String icon, String title, String action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setGravity(Gravity.CENTER);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(3), dp(9), dp(3), dp(8));
        tile.setBackground(rounded(isClassic() ? 0xFF15345B : 0xFF0D5885, 17));
        TextView symbol = label(icon, 24, Color.WHITE, true);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(rounded(isClassic() ? 0xFF146BC2 : 0xFF1497D4, 32));
        tile.addView(symbol, new LinearLayout.LayoutParams(dp(45), dp(45)));
        TextView caption = label(title, 10, Color.WHITE, true);
        caption.setGravity(Gravity.CENTER);
        tile.addView(caption, new LinearLayout.LayoutParams(-1, dp(27)));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(91);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(tile, lp);
        tile.setOnClickListener(v -> {
            if (quickDialog != null) quickDialog.dismiss();
            Intent intent = new Intent(action);
            launchAction(intent);
        });
    }

    private void showMenu() {
        String[] options = korean
                ? new String[]{"기본 홈 앱 선택", "Android 설정", "배경 변경", "정보"}
                : new String[]{"Выбрать главный экран", "Настройки Android", "Сменить обои", "О приложении"};
        new android.app.AlertDialog.Builder(this)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
                        catch (Exception e) { launchAction(new Intent(Settings.ACTION_SETTINGS)); }
                    } else if (which == 1) {
                        launchAction(new Intent(Settings.ACTION_SETTINGS));
                    } else if (which == 2) {
                        wallpaperMode = (wallpaperMode + 1) % 3;
                        wallpaper.invalidate();
                    } else {
                        new android.app.AlertDialog.Builder(this)
                                .setTitle(isClassic() ? "Koryo Classic" : "Koryo Blue")
                                .setMessage(korean
                                        ? "안드로이드용 홈 화면 테마입니다. 시스템의 실제 제한이나 네트워크 차단은 적용하지 않습니다."
                                        : "Самостоятельный Android-лаунчер, вдохновлённый интерфейсами из предоставленных роликов. Он меняет главный экран, но не подменяет прошивку и не вводит системные ограничения.")
                                .setPositiveButton("OK", null).show();
                    }
                }).show();
    }

    private void launchAction(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, korean ? "사용할 수 없습니다" : "Этот раздел недоступен на устройстве", Toast.LENGTH_SHORT).show();
        }
    }

    private class WallpaperView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        WallpaperView() { super(BaseLauncherActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            int[] colors;
            if (isClassic()) {
                colors = wallpaperMode == 0
                        ? new int[]{0xFF07172F, 0xFF0B3159, 0xFF102B4A}
                        : wallpaperMode == 1
                        ? new int[]{0xFF0C1C3B, 0xFF174B79, 0xFF07152B}
                        : new int[]{0xFF261C47, 0xFF0D3B69, 0xFF07172F};
            } else {
                colors = wallpaperMode == 0
                        ? new int[]{0xFF026B9B, 0xFF0D91B9, 0xFF06436F}
                        : wallpaperMode == 1
                        ? new int[]{0xFF007C99, 0xFF34B9CE, 0xFF07547F}
                        : new int[]{0xFF0B4E97, 0xFF2A81C1, 0xFF022D5C};
            }
            paint.setShader(new LinearGradient(0, 0, w, h, colors, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setColor(isClassic() ? 0x223EA8FF : 0x335CEBFF);
            canvas.drawCircle(w * 0.88f, h * 0.22f, w * 0.43f, paint);
            paint.setColor(isClassic() ? 0x1829A7D8 : 0x2244FFFF);
            canvas.drawCircle(w * 0.04f, h * 0.67f, w * 0.48f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0x22FFFFFF);
            for (int i = 0; i < 6; i++) {
                float y = h * 0.28f + i * dp(34);
                canvas.drawLine(0, y, w, y + dp(18), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private class FlagView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        FlagView() { super(BaseLauncherActivity.this); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float r = dp(5);
            p.setColor(Color.WHITE);
            c.drawRoundRect(0, 0, w, h, r, r, p);
            p.setColor(0xFF024B9B);
            c.drawRect(1, h * 0.08f, w - 1, h * 0.26f, p);
            c.drawRect(1, h * 0.74f, w - 1, h * 0.92f, p);
            p.setColor(0xFFED1C24);
            c.drawRect(1, h * 0.29f, w - 1, h * 0.71f, p);
            p.setColor(Color.WHITE);
            c.drawCircle(w * 0.27f, h * 0.5f, h * 0.14f, p);
            p.setColor(0xFFED1C24);
            Path star = new Path();
            float cx = w * 0.27f, cy = h * 0.5f, outer = h * 0.12f, inner = outer * 0.43f;
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float rr = (i % 2 == 0) ? outer : inner;
                float x = cx + (float)Math.cos(a) * rr;
                float y = cy + (float)Math.sin(a) * rr;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
            c.drawPath(star, p);
        }
    }
}