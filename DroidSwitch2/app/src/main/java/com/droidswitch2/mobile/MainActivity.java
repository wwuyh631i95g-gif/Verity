package com.droidswitch2.mobile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DSU_PACKAGE = "com.android.dynsystem";
    private static final String ACTION_START_DSU_LOADER =
            "android.settings.development.START_DSU_LOADER";

    private final int bg = Color.rgb(8, 14, 26);
    private final int card = Color.rgb(20, 31, 50);
    private final int text = Color.rgb(241, 246, 255);
    private final int muted = Color.rgb(169, 185, 207);
    private final int blue = Color.rgb(92, 163, 255);
    private final int green = Color.rgb(119, 220, 164);
    private final int amber = Color.rgb(255, 199, 112);
    private LinearLayout content;
    private TextView dsuState;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildScreen();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dsuState != null) refreshStatus();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(22), dp(18), dp(34));
        content.setBackgroundColor(bg);
        scroll.addView(content);
        setContentView(scroll);

        addText("DROIDSWITCH 2  /  ANDROID SYSTEM TOOL", 11, blue, true, 5);
        addText("Попробовать другую систему", 27, text, true, 7);
        addText("Проверка телефона и запуск штатного Android DSU Loader — без компьютера.",
                14, muted, false, 17);

        addCard("Что программа действительно делает",
                "DroidSwitch 2 проверяет доступность системного компонента DSU и пытается открыть настоящий загрузчик DSU в настройках телефона. Если производитель оставил эту функцию включённой, дальше установку выполняет сама система Android, а не имитация внутри приложения.");

        addText("СОСТОЯНИЕ ТЕЛЕФОНА", 11, blue, true, 7);
        dsuState = addText("Проверяю системный компонент…", 14, amber, false, 12);
        addButton("Обновить проверку", v -> refreshStatus());
        addButton("Показать данные устройства", v -> showDeviceInfo());

        addText("УСТАНОВКА БЕЗ ПК", 11, blue, true, 7);
        addButton("Открыть настоящий DSU Loader", v -> openDsuLoader());
        addButton("Открыть параметры разработчика", v -> openDeveloperOptions());

        addCard("Как это будет работать",
                "1. Нажми «Открыть настоящий DSU Loader».
"
                + "2. Если HONOR предоставляет эту функцию, откроется системный список образов.
"
                + "3. Выбирай только образ, который предлагает сам системный загрузчик для этого устройства.
"
                + "4. Подтверди условия в системном окне. Android покажет уведомление об установке и предложит перезагрузку.

"
                + "Если DSU Loader отсутствует или пишет, что образов нет, это означает, что штатный путь на текущей прошивке недоступен.");

        addCard("Честное ограничение",
                "DSU запускает гостевую Android-систему временно и позволяет вернуться к исходной системе. Это не постоянная замена MagicOS. Обычное приложение не может получить привилегию INSTALL_DYNAMIC_SYSTEM или разблокировать загрузчик самостоятельно. Для постоянной установки другой прошивки нужны поддержка устройства и разрешённая производителем процедура.");

        addCard("Для HONOR X8b",
                "Поддержка DSU на HONOR X8b пока не подтверждена. Наличие Android 14 или компонента com.android.dynsystem само по себе не доказывает, что HONOR разрешает установку GSI. Если кнопка не открывает загрузчик, DroidSwitch покажет системные настройки вместо выдуманного прогресса.");

        addButton("Официальная документация Android DSU", v ->
                openUrl("https://developer.android.com/topic/dsu"));
        addButton("Требования к Android GSI", v ->
                openUrl("https://developer.android.com/topic/generic-system-image"));

        addText("Разрешения приложения: нет. Не используются root, ADB, USB, Accessibility, Device Admin или доступ к личным файлам.",
                12, muted, false, 12);
    }

    private void refreshStatus() {
        boolean found = false;
        boolean enabled = false;
        String version = "";
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = getPackageManager().getPackageInfo(
                        DSU_PACKAGE, PackageManager.PackageInfoFlags.of(0));
            } else {
                info = getPackageManager().getPackageInfo(DSU_PACKAGE, 0);
            }
            found = true;
            enabled = getPackageManager().getApplicationEnabledSetting(DSU_PACKAGE)
                    != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    && getPackageManager().getApplicationEnabledSetting(DSU_PACKAGE)
                    != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
            if (info.versionName != null) version = " · версия " + info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            found = false;
        } catch (Exception ignored) {
            found = false;
        }

        if (found && enabled) {
            dsuState.setText("Компонент com.android.dynsystem найден" + version
                    + ". Это предварительный признак; наличие DSU Loader ещё не подтверждено.");
            dsuState.setTextColor(green);
        } else if (found) {
            dsuState.setText("Компонент DSU найден, но отключён или недоступен. Его наличие не означает, что установка разрешена.");
            dsuState.setTextColor(amber);
        } else {
            dsuState.setText("Компонент com.android.dynsystem не обнаружен. На этой сборке DSU может отсутствовать.");
            dsuState.setTextColor(amber);
        }
    }

    private void openDsuLoader() {
        try {
            Intent intent = new Intent(ACTION_START_DSU_LOADER);
            startActivity(intent);
            Toast.makeText(this,
                    "Открываю системный DSU Loader. Если появится список, используй только предложенные системой образы.",
                    Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException | SecurityException error) {
            fallbackToDeveloperOptions();
        } catch (Exception error) {
            fallbackToDeveloperOptions();
        }
    }

    private void fallbackToDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            Toast.makeText(this,
                    "Прямой запуск DSU Loader недоступен. Проверь, есть ли пункт DSU Loader в параметрах разработчика.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(this, "Не удалось открыть настройки Android.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void showDeviceInfo() {
        StringBuilder value = new StringBuilder();
        value.append("Производитель: ").append(safe(Build.MANUFACTURER));
        value.append("\nМодель: ").append(safe(Build.MODEL));
        value.append("\nКод устройства: ").append(safe(Build.DEVICE));
        value.append("\nПродукт: ").append(safe(Build.PRODUCT));
        value.append("\nПлата: ").append(safe(Build.BOARD));
        value.append("\nAndroid: ").append(safe(Build.VERSION.RELEASE));
        value.append("\nAPI: ").append(Build.VERSION.SDK_INT);
        value.append("\nСборка: ").append(safe(Build.DISPLAY));
        value.append("\nПатч безопасности: ").append(safe(Build.VERSION.SECURITY_PATCH));
        value.append("\nАрхитектуры: ");
        String[] abis = Build.SUPPORTED_ABIS;
        for (int i = 0; i < abis.length; i++) {
            if (i > 0) value.append(", ");
            value.append(abis[i]);
        }
        value.append("\n\nЭти свойства не раскрывают IMEI или серийный номер.");
        new android.app.AlertDialog.Builder(this)
                .setTitle("Данные устройства")
                .setMessage(value.toString())
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть страницу.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCard(String title, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(card);
        shape.setCornerRadius(dp(18));
        box.setBackground(shape);

        TextView heading = makeText(title, 16, text, true);
        box.addView(heading);
        TextView message = makeText(body, 13, muted, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8);
        box.addView(message, p);
        addBlock(box, 0, 12);
    }

    private TextView addText(String value, int size, int color, boolean bold, int bottom) {
        TextView view = makeText(value, size, color, bold);
        addBlock(view, 0, bottom);
        return view;
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        view.setLineSpacing(dp(3), 1.0f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(text);
        button.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(36, 76, 130));
        shape.setCornerRadius(dp(14));
        button.setBackground(shape);
        addBlock(button, 0, 9);
    }

    private void addBlock(View view, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        content.addView(view, p);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "неизвестно" : value;
    }
}
