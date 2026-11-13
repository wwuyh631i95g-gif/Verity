package com.droidswitch2.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 14, 26);
    private final int card = Color.rgb(20, 31, 50);
    private final int text = Color.rgb(241, 246, 255);
    private final int muted = Color.rgb(169, 185, 207);
    private final int blue = Color.rgb(92, 163, 255);
    private final int green = Color.rgb(119, 220, 164);
    private final int amber = Color.rgb(255, 199, 112);
    private LinearLayout content;
    private TextView lockState;
    private TextView trebleState;
    private TextView summaryState;
    private Map<String, String> lastReport = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildScreen();
        refreshDiagnostics();
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

        addText("DROIDSWITCH 2.1  /  GSI READINESS", 11, blue, true, 5);
        addText("Проверка перед перепрошивкой", 27, text, true, 7);
        addText("Диагностика HONOR X8b и проверка условий для постоянной установки другой Android-системы.", 
                14, muted, false, 16);

        addCard("Важно перед началом",
                "Эта версия проверяет реальные системные признаки: состояние загрузчика, поддержку OEM-разблокировки, Treble, архитектуру и разметку разделов. Она не выдаёт предположение за успешную прошивку и не записывает системные разделы без подтверждённой совместимости.");

        addText("РЕЗУЛЬТАТ ПРОВЕРКИ", 11, blue, true, 7);
        summaryState = addText("Проверяю свойства Android…", 14, amber, false, 8);
        lockState = addText("Загрузчик: проверка…", 14, muted, true, 6);
        trebleState = addText("Project Treble: проверка…", 14, muted, true, 14);

        addButton("Проверить ещё раз", v -> refreshDiagnostics());
        addButton("Показать полный отчёт", v -> showReport());
        addButton("Поделиться отчётом", v -> shareReport());

        addText("ПРОВЕРИТЬ В НАСТРОЙКАХ", 11, blue, true, 7);
        addButton("Открыть параметры разработчика", v -> openDeveloperOptions());
        addButton("Как проверить, разблокирован ли загрузчик", v -> showBootloaderHelp());

        addCard("Что означает OEM-разблокировка",
                "Переключатель «Разблокировка OEM» в параметрах разработчика — это только разрешение на попытку разблокировки. Он НЕ означает, что загрузчик уже разблокирован. Фактическое состояние показывается отдельно загрузчиком или системным свойством, если производитель его публикует.");

        addCard("Почему приложение пока не прошивает",
                "Обычное Android-приложение не получает доступ к fastboot и защищённым разделам только потому, что установлено на телефоне. Для постоянной установки GSI нужны разблокированный загрузчик, полная совместимость Project Treble, подходящий образ именно для разметки и архитектуры устройства, а также рабочий способ восстановления заводской прошивки. Универсального безопасного способа записи разделов для всех телефонов нет.");

        addCard("Предупреждение о данных и совместимости",
                "Разблокировка загрузчика обычно стирает все пользовательские данные. Несовместимый GSI может привести к тому, что телефон перестанет загружаться. До подтверждения поддержки именно HONOR X8b не скачивай случайные образы и не запускай команды прошивки. Сначала сохрани резервную копию и найди официальный способ восстановления.");

        addButton("Официально: требования Android GSI", v ->
                openUrl("https://developer.android.com/topic/generic-system-image"));
        addButton("Официально: блокировка загрузчика Android", v ->
                openUrl("https://source.android.com/docs/core/architecture/bootloader/locking_unlocking"));

        addText("Нет root, ADB, USB, Accessibility или доступа к личным файлам. Все проверки только для чтения. Приложение не разблокирует и не перепрошивает телефон.", 
                12, muted, false, 10);
    }

    private void refreshDiagnostics() {
        summaryState.setText("Читаю системные свойства…");
        summaryState.setTextColor(amber);
        lockState.setText("Загрузчик: проверка…");
        trebleState.setText("Project Treble: проверка…");

        new Thread(() -> {
            Map<String, String> report = collectReport();
            runOnUiThread(() -> {
                lastReport = report;
                String lock = report.get("ro.boot.flash.locked");
                String deviceState = report.get("ro.boot.vbmeta.device_state");
                String treble = report.get("ro.treble.enabled");
                String oemSupport = report.get("ro.oem_unlock_supported");

                String resolved = resolveLockState(lock, deviceState);
                if ("Разблокирован".equals(resolved)) {
                    lockState.setText("Загрузчик: разблокирован (по системному свойству)");
                    lockState.setTextColor(green);
                } else if ("Заблокирован".equals(resolved)) {
                    lockState.setText("Загрузчик: заблокирован (по системному свойству)");
                    lockState.setTextColor(amber);
                } else {
                    lockState.setText("Загрузчик: Android не сообщил точное состояние");
                    lockState.setTextColor(amber);
                }

                if ("true".equalsIgnoreCase(treble)) {
                    trebleState.setText("Project Treble: включён — это только базовая проверка");
                    trebleState.setTextColor(green);
                } else if ("false".equalsIgnoreCase(treble)) {
                    trebleState.setText("Project Treble: не обнаружен");
                    trebleState.setTextColor(amber);
                } else {
                    trebleState.setText("Project Treble: неизвестно");
                    trebleState.setTextColor(amber);
                }

                boolean unlocked = "Разблокирован".equals(resolved);
                boolean trebleYes = "true".equalsIgnoreCase(treble);
                boolean oemYes = "1".equals(oemSupport) || "true".equalsIgnoreCase(oemSupport);

                if (unlocked && trebleYes) {
                    summaryState.setText("Найдены два важных признака. Это ещё НЕ подтверждение готовности к прошивке: нужно проверить разметку, VNDK и наличие подходящего образа.");
                    summaryState.setTextColor(green);
                } else if ("0".equals(oemSupport) && !unlocked) {
                    summaryState.setText("Система сообщает, что OEM-разблокировка не поддерживается. Постоянная установка сторонней системы может быть недоступна.");
                    summaryState.setTextColor(amber);
                } else if (unlocked) {
                    summaryState.setText("Загрузчик, похоже, разблокирован, но совместимость GSI пока не подтверждена.");
                    summaryState.setTextColor(amber);
                } else if (oemYes) {
                    summaryState.setText("Телефон сообщает о поддержке OEM-разблокировки, но это не означает, что загрузчик уже разблокирован.");
                    summaryState.setTextColor(amber);
                } else {
                    summaryState.setText("Пока нет подтверждения, что телефон готов к установке GSI. Открой полный отчёт.");
                    summaryState.setTextColor(amber);
                }
            });
        }).start();
    }

    private Map<String, String> collectReport() {
        Map<String, String> values = new LinkedHashMap<>();
        String[] keys = {
                "ro.boot.flash.locked",
                "ro.boot.vbmeta.device_state",
                "ro.boot.verifiedbootstate",
                "ro.oem_unlock_supported",
                "ro.treble.enabled",
                "ro.vndk.version",
                "ro.vndk.lite",
                "ro.product.cpu.abi",
                "ro.product.cpu.abilist",
                "ro.boot.slot_suffix",
                "ro.boot.dynamic_partitions",
                "ro.boot.super_partition",
                "ro.build.version.release",
                "ro.build.version.sdk",
                "ro.build.version.security_patch",
                "ro.product.manufacturer",
                "ro.product.model",
                "ro.product.device",
                "ro.build.display.id"
        };
        for (String key : keys) {
            values.put(key, readProperty(key));
        }

        values.put("OEM toggle (oem_unlock_enabled)",
                readGlobalSetting("oem_unlock_enabled"));
        values.put("Build.MANUFACTURER", safe(Build.MANUFACTURER));
        values.put("Build.MODEL", safe(Build.MODEL));
        values.put("Build.DEVICE", safe(Build.DEVICE));
        values.put("Build.PRODUCT", safe(Build.PRODUCT));
        values.put("Build.VERSION.RELEASE", safe(Build.VERSION.RELEASE));
        values.put("Build.VERSION.SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
        values.put("Build.SUPPORTED_ABIS", join(Build.SUPPORTED_ABIS));
        return values;
    }

    private String readProperty(String key) {
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/getprop", key)
                    .redirectErrorStream(true).start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
            String value = output.toString().trim();
            return value.isEmpty() ? "(не опубликовано)" : value;
        } catch (Exception error) {
            if (process != null) process.destroy();
            return "(недоступно для обычного приложения)";
        }
    }

    private String readGlobalSetting(String name) {
        try {
            int value = Settings.Global.getInt(getContentResolver(), name, -1);
            if (value == 1) return "включён (разрешение на попытку разблокировки)";
            if (value == 0) return "выключен";
            return "(не опубликовано / неизвестно)";
        } catch (Exception error) {
            return "(не удалось прочитать)";
        }
    }

    private String resolveLockState(String flashLocked, String deviceState) {
        if ("0".equals(flashLocked) || "unlocked".equalsIgnoreCase(deviceState)) {
            return "Разблокирован";
        }
        if ("1".equals(flashLocked) || "locked".equalsIgnoreCase(deviceState)) {
            return "Заблокирован";
        }
        return "Неизвестно";
    }

    private void showReport() {
        if (lastReport.isEmpty()) {
            Toast.makeText(this, "Сначала дождись завершения проверки.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder out = new StringBuilder();
        out.append("DroidSwitch 2.1 — отчёт диагностики\n");
        out.append("Устройство: ").append(safe(Build.MANUFACTURER)).append(" ")
                .append(safe(Build.MODEL)).append("\n\n");
        for (Map.Entry<String, String> entry : lastReport.entrySet()) {
            out.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        out.append("\nОтчёт не подтверждает возможность прошивки. Не содержит IMEI или серийного номера.");
        new AlertDialog.Builder(this)
                .setTitle("Диагностика")
                .setMessage(out.toString())
                .setPositiveButton("Закрыть", null)
                .setNeutralButton("Поделиться", (dialog, which) -> shareText(out.toString()))
                .show();
    }

    private void shareReport() {
        if (lastReport.isEmpty()) {
            Toast.makeText(this, "Сначала дождись завершения проверки.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder out = new StringBuilder("DroidSwitch 2.1 — отчёт диагностики\n");
        for (Map.Entry<String, String> entry : lastReport.entrySet()) {
            out.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        shareText(out.toString());
    }

    private void shareText(String value) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, value);
        try {
            startActivity(Intent.createChooser(send, "Отправить диагностический отчёт"));
        } catch (Exception error) {
            Toast.makeText(this, "Не удалось открыть меню отправки.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (ActivityNotFoundException error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void showBootloaderHelp() {
        String message =
                "1. Открой «Параметры разработчика» и найди «Разблокировка OEM».\n"
                + "2. Запиши, доступен ли переключатель и включён ли он. Само включение НЕ разблокирует загрузчик.\n"
                + "3. Посмотри строку «Загрузчик» в отчёте DroidSwitch. Если там «неизвестно», приложение не может достоверно прочитать состояние из Android.\n"
                + "4. Не выполняй команды разблокировки и не устанавливай образы, пока не подтверждена официальная процедура именно для HONOR X8b LLY-LX1.\n\n"
                + "Важно: реальная разблокировка обычно приводит к полному сбросу телефона и удалению данных. Не начинай её без резервной копии и подтверждённого способа восстановления.";
        new AlertDialog.Builder(this)
                .setTitle("Проверка загрузчика")
                .setMessage(message)
                .setPositiveButton("Открыть параметры", (dialog, which) -> openDeveloperOptions())
                .setNegativeButton("Закрыть", null)
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

        box.addView(makeText(title, 16, text, true));
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

    private String join(String[] values) {
        if (values == null || values.length == 0) return "неизвестно";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(", ");
            out.append(values[i]);
        }
        return out.toString();
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "неизвестно" : value;
    }
}
