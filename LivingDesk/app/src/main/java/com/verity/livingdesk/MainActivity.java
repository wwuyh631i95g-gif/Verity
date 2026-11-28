package com.verity.livingdesk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.PointerIcon;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_IMAGE = 7101;
    private static final int REQUEST_OVERLAY = 7102;
    private static final String PREFS = "living_desk_settings";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private SeasonPreviewView seasonPreview;
    private PetView petPreview;
    private TextView clockText;
    private TextView performanceText;
    private TextView batteryText;
    private TextView pauseText;
    private TextView cursorText;
    private ProgressBar memoryProgress;
    private ImageView studioImage;
    private Bitmap studioBitmap;
    private String selectedSeason = "auto";
    private int petType = 0;
    private int cursorShape = 0;
    private int cursorColor = 0;
    private boolean glassEnabled = true;
    private boolean pendingOverlay = false;
    private boolean pauseRunning = false;
    private long pauseUntil = 0L;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateClock();
            updatePerformance();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 11, 24));
        getWindow().setNavigationBarColor(Color.rgb(7, 11, 24));
        if (Build.VERSION.SDK_INT >= 28) getWindow().setNavigationBarDividerColor(0xFF202A43);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedSeason = prefs.getString("season", "auto");
        petType = prefs.getInt("pet_type", 0);
        glassEnabled = prefs.getBoolean("glass", true);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF080D19);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = text("LIVING DESK   /   ANDROID 14", 10, 0xFF9AA9D8, true);
        eyebrow.setLetterSpacing(0.16f);
        root.addView(eyebrow);

        TextView title = text("Живой стол", 31, Color.WHITE, true);
        title.setPadding(0, dp(5), 0, dp(3));
        root.addView(title);
        root.addView(text("Четыре сезона, стеклянный док, питомец и живые обои — в одном приложении.", 13, 0xFFB6C2DE, false));

        LinearLayout hero = panel();
        addText(hero, "ТВОЙ ЭКРАН — ЖИВОЙ", 11, 0xFFB6C7FF, true);
        addText(hero, "Выбери настроение рабочего стола", 18, Color.WHITE, true);
        seasonPreview = new SeasonPreviewView(this);
        LinearLayout.LayoutParams sceneLp = new LinearLayout.LayoutParams(-1, dp(230));
        sceneLp.topMargin = dp(12);
        hero.addView(seasonPreview, sceneLp);
        addText(hero, "Анимированные частицы, мягкое освещение и пейзаж меняются вместе с сезоном.", 12, 0xFFD0D8EF, false);
        root.addView(hero, spaceTop(16));

        LinearLayout seasons = panel();
        addText(seasons, "01  /  ЧЕТЫРЕ СЕЗОНА", 11, 0xFFB6C7FF, true);
        addText(seasons, "Выбери зиму, весну, лето, осень или автоматическую смену по календарю.", 13, 0xFFC1CBE2, false);
        HorizontalScrollView seasonScroll = new HorizontalScrollView(this);
        seasonScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout seasonRow = new LinearLayout(this);
        seasonRow.setOrientation(LinearLayout.HORIZONTAL);
        String[][] seasonButtons = {
                {"❄️  Зима", "winter"}, {"🌸  Весна", "spring"},
                {"☀️  Лето", "summer"}, {"🍂  Осень", "autumn"},
                {"✦  Авто", "auto"}
        };
        for (String[] item : seasonButtons) {
            Button b = button(item[0], null);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, dp(44));
            bp.rightMargin = dp(7);
            seasonRow.addView(b, bp);
            b.setOnClickListener(v -> chooseSeason(item[1]));
        }
        seasonScroll.addView(seasonRow);
        seasons.addView(seasonScroll, spaceTop(10));
        Button liveWallpaper = button("Установить живые обои", null);
        liveWallpaper.setOnClickListener(v -> openLiveWallpaperPicker());
        seasons.addView(liveWallpaper, spaceTop(10));
        addText(seasons, "После выбора Android откроет системный экран предпросмотра обоев. Там нажми «Установить».", 11, 0xFF98A7CB, false);
        root.addView(seasons, spaceTop(12));

        LinearLayout dock = panel();
        addText(dock, "02  /  ПАРЯЩИЙ ДОК + СТЕКЛО", 11, 0xFFB6C7FF, true);
        addText(dock, "Небольшая полупрозрачная панель поверх других приложений. Она не перекрывает весь экран: нажимаются только сами значки.", 13, 0xFFC1CBE2, false);
        addText(dock, "В доке будут кнопки быстрого перехода к Живому столу, сезонам, питомцу и игровому меню.", 12, 0xFF9EADCF, false);
        Button enableDock = button("Включить плавающий док", null);
        enableDock.setOnClickListener(v -> requestOverlayAndStart());
        dock.addView(enableDock, spaceTop(10));
        Button disableDock = button("Выключить док и питомца", null);
        disableDock.setOnClickListener(v -> stopOverlay());
        dock.addView(disableDock, spaceTop(7));
        Button glass = button(glassEnabled ? "Стекло: включено  ✓" : "Стекло: выключено", null);
        glass.setOnClickListener(v -> {
            glassEnabled = !glassEnabled;
            prefs.edit().putBoolean("glass", glassEnabled).apply();
            glass.setText(glassEnabled ? "Стекло: включено  ✓" : "Стекло: выключено");
            Intent i = new Intent(this, DeskOverlayService.class);
            i.setAction(DeskOverlayService.ACTION_REFRESH);
            if (prefs.getBoolean("overlay_enabled", false)) startOverlayService(i);
            Toast.makeText(this, glassEnabled ? "Стеклянный стиль включён" : "Стеклянный стиль выключен", Toast.LENGTH_SHORT).show();
        });
        dock.addView(glass, spaceTop(7));
        root.addView(dock, spaceTop(12));

        LinearLayout pet = panel();
        addText(pet, "03  /  ПИТОМЕЦ", 11, 0xFFB6C7FF, true);
        addText(pet, "Маленький компаньон моргает, двигает хвостом и реагирует на касания. Его можно оставить рядом с плавающим доком.", 13, 0xFFC1CBE2, false);
        petPreview = new PetView(this);
        petPreview.setPetType(petType);
        LinearLayout.LayoutParams petLp = new LinearLayout.LayoutParams(-1, dp(138));
        petLp.topMargin = dp(8);
        pet.addView(petPreview, petLp);
        Button changePet = button("Сменить питомца", null);
        changePet.setOnClickListener(v -> {
            petType = (petType + 1) % 3;
            prefs.edit().putInt("pet_type", petType).apply();
            petPreview.setPetType(petType);
            Intent i = new Intent(this, DeskOverlayService.class);
            i.setAction(DeskOverlayService.ACTION_PET);
            i.putExtra(DeskOverlayService.EXTRA_PET_TYPE, petType);
            if (prefs.getBoolean("overlay_enabled", false)) startOverlayService(i);
        });
        pet.addView(changePet, spaceTop(5));
        Button showPet = button("Показать питомца поверх приложений", null);
        showPet.setOnClickListener(v -> requestOverlayAndShowPet());
        pet.addView(showPet, spaceTop(7));
        root.addView(pet, spaceTop(12));

        LinearLayout newYear = panel();
        addText(newYear, "04  /  НОВЫЙ ГОД", 11, 0xFFB6C7FF, true);
        clockText = text("До Нового года: считаем…", 24, Color.WHITE, true);
        clockText.setGravity(Gravity.CENTER);
        clockText.setPadding(0, dp(8), 0, dp(4));
        newYear.addView(clockText);
        addText(newYear, "Добавь виджет «Новый год — обратный отсчёт» на главный экран: удерживай пустое место → «Виджеты» → «Живой стол».", 12, 0xFFB9C6E5, false);
        root.addView(newYear, spaceTop(12));

        LinearLayout studio = panel();
        addText(studio, "05  /  СТУДИЯ ОБОЕВ", 11, 0xFFB6C7FF, true);
        addText(studio, "Выбери своё фото или картинку и установи её на главный экран, экран блокировки или сразу на оба.", 13, 0xFFC1CBE2, false);
        Button pickImage = button("Выбрать изображение", null);
        pickImage.setOnClickListener(v -> pickWallpaperImage());
        studio.addView(pickImage, spaceTop(8));
        studioImage = new ImageView(this);
        studioImage.setVisibility(View.GONE);
        studioImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        studioImage.setBackground(round(0xFF151F36, 0xFF4A5D91));
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, dp(170));
        imageLp.topMargin = dp(9);
        studio.addView(studioImage, imageLp);
        LinearLayout applyRow = new LinearLayout(this);
        applyRow.setOrientation(LinearLayout.HORIZONTAL);
        Button homeWallpaper = button("Главный экран", null);
        homeWallpaper.setOnClickListener(v -> applyStudioWallpaper(WallpaperManager.FLAG_SYSTEM));
        Button lockWallpaper = button("Блокировка", null);
        lockWallpaper.setOnClickListener(v -> applyStudioWallpaper(WallpaperManager.FLAG_LOCK));
        Button bothWallpaper = button("Оба", null);
        bothWallpaper.setOnClickListener(v -> applyStudioWallpaper(WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK));
        for (Button b : new Button[]{homeWallpaper, lockWallpaper, bothWallpaper}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
            p.rightMargin = dp(5);
            applyRow.addView(b, p);
        }
        studio.addView(applyRow, spaceTop(8));
        root.addView(studio, spaceTop(12));

        LinearLayout cursors = panel();
        addText(cursors, "06  /  КУРСОРЫ — 11 520 ВАРИАНТОВ", 11, 0xFFB6C7FF, true);
        addText(cursors, "Комбинируемые формы, цвета, размеры и эффекты. Выбери стиль и проверь его на кнопках приложения.", 13, 0xFFC1CBE2, false);
        cursorText = text("➤   Классическая стрелка  •  стиль 1 / 11 520", 16, Color.WHITE, true);
        cursorText.setPadding(dp(4), dp(12), dp(4), dp(12));
        cursors.addView(cursorText);
        LinearLayout cursorRow = new LinearLayout(this);
        cursorRow.setOrientation(LinearLayout.HORIZONTAL);
        Button shape = button("Форма", null);
        shape.setOnClickListener(v -> {
            cursorShape = (cursorShape + 1) % 8;
            updateCursorPreview();
        });
        Button color = button("Цвет", null);
        color.setOnClickListener(v -> {
            cursorColor = (cursorColor + 1) % 24;
            updateCursorPreview();
        });
        Button randomCursor = button("Случайный", null);
        randomCursor.setOnClickListener(v -> {
            cursorShape = (cursorShape + 3) % 8;
            cursorColor = (cursorColor + 7) % 24;
            updateCursorPreview();
        });
        for (Button b : new Button[]{shape, color, randomCursor}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
            p.rightMargin = dp(5);
            cursorRow.addView(b, p);
        }
        cursors.addView(cursorRow, spaceTop(5));
        addText(cursors, "Ограничение Android: обычное приложение не может заменить системный курсор во всех приложениях. Выбранный стиль действует в интерфейсе Живого стола и его доке.", 11, 0xFF98A7CB, false);
        root.addView(cursors, spaceTop(12));
        updateCursorPreview();

        LinearLayout load = panel();
        addText(load, "07  /  НАГРУЗКА", 11, 0xFFB6C7FF, true);
        addText(load, "Лёгкая панель контроля памяти и батареи. Показания обновляются автоматически; отдельное разрешение не требуется.", 13, 0xFFC1CBE2, false);
        memoryProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        memoryProgress.setMax(100);
        load.addView(memoryProgress, spaceTop(10));
        performanceText = text("Память: измеряем…", 13, Color.WHITE, true);
        performanceText.setPadding(0, dp(7), 0, dp(3));
        load.addView(performanceText);
        batteryText = text("Батарея: измеряем…", 12, 0xFFB9C6E5, false);
        load.addView(batteryText);
        root.addView(load, spaceTop(12));

        LinearLayout games = panel();
        addText(games, "08  /  ИГРЫ — ПАУЗА", 11, 0xFFB6C7FF, true);
        addText(games, "Открой список установленных игр и запускай их из одного места. Таймер паузы помогает сделать перерыв во время игры.", 13, 0xFFC1CBE2, false);
        Button gameList = button("Мои игры и приложения", null);
        gameList.setOnClickListener(v -> showLaunchableApps());
        games.addView(gameList, spaceTop(8));
        Button pause = button("Пауза на 5 минут", null);
        pause.setOnClickListener(v -> togglePause());
        games.addView(pause, spaceTop(7));
        pauseText = text("Пауза не активна", 12, 0xFFB9C6E5, false);
        pauseText.setPadding(0, dp(7), 0, 0);
        games.addView(pauseText);
        addText(games, "Android не даёт стороннему приложению нажимать «паузу» внутри чужой игры. Здесь есть собственный таймер и быстрый запуск игр.", 11, 0xFF98A7CB, false);
        root.addView(games, spaceTop(12));

        TextView footer = text("ЖИВОЙ СТОЛ 1.0  •  Honor X8b / Android 14\nРаботает локально. Интернет и регистрация не нужны.", 10, 0xFF66769A, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(22), 0, dp(5));
        root.addView(footer);
        updateClock();
        updatePerformance();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (pendingOverlay && (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this))) {
            pendingOverlay = false;
            startOverlay(false);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                studioBitmap = decodeScaled(data.getData());
                studioImage.setImageBitmap(studioBitmap);
                studioImage.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Изображение загружено в студию", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть это изображение", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
                pendingOverlay = false;
                startOverlay(false);
            } else {
                pendingOverlay = false;
                Toast.makeText(this, "Разрешение не выдано — док пока выключен", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void chooseSeason(String value) {
        selectedSeason = value;
        prefs.edit().putString("season", value).apply();
        if (seasonPreview != null) seasonPreview.setSeason(value);
        String label = value.equals("auto") ? "Автоматическая смена сезонов включена" : "Выбран сезон: " + value;
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
    }

    private void openLiveWallpaperPicker() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, SeasonWallpaperService.class));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Системный экран живых обоев недоступен на этой прошивке", Toast.LENGTH_LONG).show();
        }
    }

    private void requestOverlayAndStart() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            pendingOverlay = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQUEST_OVERLAY);
            return;
        }
        startOverlay(false);
    }

    private void requestOverlayAndShowPet() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            pendingOverlay = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQUEST_OVERLAY);
            return;
        }
        startOverlay(true);
    }

    private void startOverlay(boolean showPet) {
        Intent i = new Intent(this, DeskOverlayService.class);
        i.setAction(showPet ? DeskOverlayService.ACTION_SHOW_PET : DeskOverlayService.ACTION_SHOW_DOCK);
        i.putExtra(DeskOverlayService.EXTRA_PET_TYPE, petType);
        startOverlayService(i);
        Toast.makeText(this, showPet ? "Питомец появился поверх приложений" : "Плавающий док включён", Toast.LENGTH_SHORT).show();
    }

    private void startOverlayService(Intent i) {
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void stopOverlay() {
        stopService(new Intent(this, DeskOverlayService.class));
        Toast.makeText(this, "Плавающий док и питомец выключены", Toast.LENGTH_SHORT).show();
    }

    private void pickWallpaperImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Выбери обои"), REQUEST_IMAGE);
    }

    private Bitmap decodeScaled(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int max = 1800;
        int sample = 1;
        while (bounds.outWidth / sample > max || bounds.outHeight / sample > max) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
            if (bitmap == null) throw new IOException("Bitmap decode returned null");
            return bitmap;
        }
    }

    private void applyStudioWallpaper(int which) {
        if (studioBitmap == null) {
            Toast.makeText(this, "Сначала выбери изображение", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            WallpaperManager.getInstance(this).setBitmap(studioBitmap, null, true, which);
            Toast.makeText(this, "Обои установлены", Toast.LENGTH_LONG).show();
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Не удалось установить эти обои", Toast.LENGTH_LONG).show();
        }
    }

    private void updateClock() {
        if (clockText != null) {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime next = LocalDate.of(now.getYear() + 1, 1, 1).atStartOfDay(now.getZone());
            long seconds = Math.max(0L, Duration.between(now, next).getSeconds());
            long days = seconds / 86400L;
            long hours = (seconds % 86400L) / 3600L;
            long minutes = (seconds % 3600L) / 60L;
            long secs = seconds % 60L;
            clockText.setText(String.format(Locale.getDefault(), "%d дн.  %02d:%02d:%02d", days, hours, minutes, secs));
        }
        if (pauseText != null) {
            if (pauseRunning) {
                long left = Math.max(0L, (pauseUntil - System.currentTimeMillis()) / 1000L);
                if (left == 0L) {
                    pauseRunning = false;
                    pauseText.setText("Пауза завершена");
                } else {
                    pauseText.setText(String.format(Locale.getDefault(), "Перерыв: %02d:%02d осталось", left / 60L, left % 60L));
                }
            }
        }
    }

    private void updatePerformance() {
        if (performanceText == null) return;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(info);
        long total = Math.max(1L, info.totalMem);
        long used = Math.max(0L, total - info.availMem);
        int percent = (int) Math.min(100L, used * 100L / total);
        if (memoryProgress != null) memoryProgress.setProgress(percent);
        Debug.MemoryInfo appMem = new Debug.MemoryInfo();
        Debug.getMemoryInfo(appMem);
        performanceText.setText(String.format(Locale.getDefault(),
                "ОЗУ системы занято: %d%%  •  свободно %.0f МБ\nПамять приложения: %d МБ",
                percent, info.availMem / 1048576.0, appMem.getTotalPss() / 1024));
        Intent battery = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null && batteryText != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int pct = scale > 0 ? Math.round(level * 100f / scale) : level;
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            batteryText.setText("Батарея: " + pct + "%" + (charging ? "  •  зарядка" : "  •  работает от аккумулятора"));
        }
    }

    private void updateCursorPreview() {
        if (cursorText == null) return;
        String[] shapes = {"➤ Стрелка", "☝ Рука", "✛ Прицел", "I Текст", "✦ Звезда", "◉ Круг", "◆ Ромб", "✧ Искра"};
        int styleNumber = cursorShape * 24 * 6 * 10 + cursorColor * 6 * 10 + 1;
        cursorText.setText(shapes[cursorShape] + "  •  стиль " + styleNumber + " / 11 520");
        int[] colors = {0xFFFFFFFF, 0xFF80D8FF, 0xFFFF9DCB, 0xFFFFD36E, 0xFF9CFFB4, 0xFFB7A2FF,
                0xFFFF8B72, 0xFF7CFFF1, 0xFFE7F08A, 0xFFFFA3F0, 0xFF8DAAFF, 0xFFFFE2B8,
                0xFFB8F7FF, 0xFFDBB8FF, 0xFFFFB8B8, 0xFFB8FFD4, 0xFFFFD1F2, 0xFFC7D5FF,
                0xFFFFF0A8, 0xFF9EE7FF, 0xFFE2C6FF, 0xFFC9FF9B, 0xFFFFC49E, 0xFFD5E0F5};
        cursorText.setTextColor(colors[cursorColor]);
        try {
            int[] types = {PointerIcon.TYPE_ARROW, PointerIcon.TYPE_HAND, PointerIcon.TYPE_CROSSHAIR,
                    PointerIcon.TYPE_TEXT, PointerIcon.TYPE_HELP, PointerIcon.TYPE_WAIT,
                    PointerIcon.TYPE_ZOOM_IN, PointerIcon.TYPE_CELL};
            View root = getWindow().getDecorView();
            root.setPointerIcon(PointerIcon.getSystemIcon(this, types[cursorShape]));
        } catch (Exception ignored) { }
    }

    private void togglePause() {
        if (pauseRunning) {
            pauseRunning = false;
            pauseText.setText("Перерыв остановлен");
        } else {
            pauseRunning = true;
            pauseUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
            pauseText.setText("Перерыв: 05:00 осталось");
        }
    }

    private void showLaunchableApps() {
        Intent launcher = new Intent(Intent.ACTION_MAIN, null);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> all = pm.queryIntentActivities(launcher, 0);
        if (all == null || all.isEmpty()) {
            Toast.makeText(this, "Не удалось получить список запускаемых приложений", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<ResolveInfo> items = new ArrayList<>();
        for (ResolveInfo r : all) {
            ApplicationInfo ai = r.activityInfo.applicationInfo;
            if (ai.category == ApplicationInfo.CATEGORY_GAME) items.add(r);
        }
        if (items.isEmpty()) items.addAll(all);
        Collections.sort(items, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return pm.getApplicationLabel(a.activityInfo.applicationInfo).toString()
                        .compareToIgnoreCase(pm.getApplicationLabel(b.activityInfo.applicationInfo).toString());
            }
        });
        int limit = Math.min(80, items.size());
        String[] labels = new String[limit];
        for (int n = 0; n < limit; n++) labels[n] = pm.getApplicationLabel(items.get(n).activityInfo.applicationInfo).toString();
        new AlertDialog.Builder(this)
                .setTitle("Игры и приложения")
                .setItems(labels, (dialog, which) -> {
                    String pkg = items.get(which).activityInfo.packageName;
                    Intent launch = pm.getLaunchIntentForPackage(pkg);
                    if (launch != null) {
                        try { startActivity(launch); }
                        catch (Exception e) { Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show(); }
                    }
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(13), dp(14), dp(14));
        box.setBackground(round(0xCC17233D, 0x665F75B6));
        return box;
    }

    private GradientDrawable round(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{fill, (fill & 0x00FFFFFF) | 0xDD000000});
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView t = text(value, size, color, bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(5);
        parent.addView(t, p);
    }

    private Button button(String value, Runnable action) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setMinHeight(dp(44));
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(round(0xFF344A80, 0x887D9BFF));
        if (action != null) b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams spaceTop(int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(margin);
        return p;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
