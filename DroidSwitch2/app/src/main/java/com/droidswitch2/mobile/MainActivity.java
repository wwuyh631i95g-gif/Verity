package com.droidswitch2.mobile;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.StatFs;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String DSU_PACKAGE = "com.android.dynsystem";
    private static final String ACTION_START_DSU_LOADER =
            "android.settings.development.START_DSU_LOADER";
    private static final long RECOMMENDED_SPACE = 10L * 1024L * 1024L * 1024L;

    private final int bg = Color.rgb(8, 14, 26);
    private final int card = Color.rgb(20, 31, 50);
    private final int text = Color.rgb(241, 246, 255);
    private final int muted = Color.rgb(169, 185, 207);
    private final int blue = Color.rgb(92, 163, 255);
    private final int green = Color.rgb(119, 220, 164);
    private final int amber = Color.rgb(255, 199, 112);

    private LinearLayout content;
    private TextView deviceState;
    private TextView dsuState;
    private TextView storageState;
    private TextView overallState;

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

        addText("DROIDSWITCH 2.2  /  DSU COMPANION", 11, blue, true, 5);
        addText("Вторая Android-система", 28, text, true, 7);
        addText("Помощник для штатного Android DSU Loader на HONOR X8b (LLY-LX1). Он открывает настоящий системный экран установки, если HONOR оставил эту функцию доступной.",
                14, muted, false, 16);

        addCard("Что получится, если HONOR поддерживает DSU",
                "MagicOS останется основной системой. DSU может добавить гостевую Android-систему на отдельное динамическое хранилище. После установки появится системное уведомление с действиями для запуска гостевой системы, возврата к MagicOS или удаления DSU.");

        addText("ПРОВЕРКА ЭТОГО ТЕЛЕФОНА", 11, blue, true, 7);
        overallState = addText("Проверяю устройство…", 14, amber, true, 8);
        deviceState = addText("Модель: проверка…", 13, muted, false, 5);
        dsuState = addText("Компонент DSU: проверка…", 13, muted, false, 5);
        storageState = addText("Свободное место: проверка…", 13, muted, false, 14);
        addButton("Обновить проверку", v -> refreshStatus());

        addText("ЗАПУСК УСТАНОВКИ", 11, blue, true, 7);
        addButton("Открыть системный DSU Loader", v -> openDsuLoader());
        addButton("Открыть параметры разработчика", v -> openDeveloperOptions());

        addCard("Как добавить вторую систему",
                "1. Нажми «Открыть системный DSU Loader».\n"
                + "2. Если HONOR предоставляет DSU Loader, Android покажет список образов, которые предлагает прошивка именно этому устройству.\n"
                + "3. Устанавливай только образ, который отображается в системном DSU Loader. Прочитай условия и дождись штатного уведомления об окончании.\n"
                + "4. Когда появится уведомление DSU, нажми «Restart», чтобы загрузить гостевую систему.\n"
                + "5. Чтобы вернуться, используй действие возврата/перезагрузки в исходную систему в уведомлении DSU.\n\n"
                + "DroidSwitch не подменяет системный список образов и не скачивает случайные прошивки.");

        addCard("Сколько места нужно",
                "Для DSU Android рекомендует иметь около 10 ГБ свободного пространства: образ системы занимает примерно 1–1,5 ГБ, а гостевые пользовательские данные могут резервировать около 8 ГБ. Фактический размер зависит от пакета и прошивки. Перед установкой проверь свободное место.");

        addCard("Какие разрешения нужны",
                "DroidSwitch 2.2 не запрашивает разрешений: нет root, USB, ADB, файлового доступа, специальных возможностей или администратора устройства. Загрузку и установку выполняет системный DSU Loader в настройках Android. Системное окно само покажет нужные подтверждения, если функция доступна.");

        addCard("Почему DSU может не появиться",
                "DSU — функция производителя, а не обязательная возможность каждого телефона с Android 14. Наличие динамических разделов и компонента com.android.dynsystem не доказывает, что HONOR включил DSU Loader или опубликовал совместимые образы для LLY-LX1. Если системный экран не открывается или список пуст, обычное приложение не может безопасно включить эту функцию.");

        addCard("Как долго останется вторая система",
                "Это не таймер на несколько минут. DSU может оставаться установленной между несколькими перезагрузками, если на устройстве включён режим сохранения (sticky). В системном уведомлении есть управление гостевой системой. Гостевая система исчезнет, если выбрать «Discard/Удалить», очистить DSU средствами системы или если прошивка производителя сбросит временное окружение. При режиме одного запуска следующий запуск может вернуться в MagicOS. Сам по себе обычный возврат в MagicOS не обязательно удаляет гостевую систему.");

        addCard("Ограничения именно для HONOR X8b",
                "Твой отчёт подтверждает Android 14, ARM64, Treble и динамические разделы, но не подтверждает наличие DSU Loader. HONOR может скрыть или отключить его в этой региональной прошивке. Поэтому DroidSwitch сначала открывает штатный интерфейс и ничего не меняет без системного подтверждения. Постоянная прошивка MagicOS на другую ОС этой программой не выполняется.");

        addButton("Официальная инструкция Google по DSU", v ->
                openUrl("https://developer.android.com/topic/dsu"));
        addButton("Требования и место для DSU (Android)", v ->
                openUrl("https://source.android.com/docs/core/ota/dynamic-system-updates"));

        addText("Версия 2.2.0 · режим безопасного запуска штатного DSU · без root и без специальных разрешений",
                12, muted, false, 10);
    }

    private void refreshStatus() {
        String model = safe(Build.MODEL);
        String device = safe(Build.DEVICE);
        boolean targetModel = "LLY-LX1".equalsIgnoreCase(model)
                || "HNLLY-Q".equalsIgnoreCase(device);

        deviceState.setText("Устройство: " + safe(Build.MANUFACTURER) + " " + model
                + " · " + safe(Build.VERSION.RELEASE) + " · " + safe(Build.DEVICE));
        deviceState.setTextColor(targetModel ? green : amber);

        boolean componentFound = false;
        boolean componentEnabled = false;
        String version = "";

        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = getPackageManager().getPackageInfo(
                        DSU_PACKAGE, PackageManager.PackageInfoFlags.of(0));
            } else {
                info = getPackageManager().getPackageInfo(DSU_PACKAGE, 0);
            }
            componentFound = true;
            int enabledState = getPackageManager().getApplicationEnabledSetting(DSU_PACKAGE);
            componentEnabled = enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    && enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
            if (info.versionName != null) version = " · " + info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            componentFound = false;
        } catch (Exception ignored) {
            componentFound = false;
        }

        if (componentFound && componentEnabled) {
            dsuState.setText("com.android.dynsystem найден и не отключён" + version
                    + " · это не подтверждает доступность DSU Loader");
            dsuState.setTextColor(green);
        } else if (componentFound) {
            dsuState.setText("Компонент DSU найден, но отключён или недоступен");
            dsuState.setTextColor(amber);
        } else {
            dsuState.setText("Системный компонент DSU не обнаружен обычным приложением");
            dsuState.setTextColor(amber);
        }

        long free = getFreeBytes();
        storageState.setText("Свободно во внутреннем хранилище: " + formatBytes(free)
                + " · рекомендация для DSU: около 10 ГБ");
        storageState.setTextColor(free >= RECOMMENDED_SPACE ? green : amber);

        if (targetModel && componentFound && componentEnabled && free >= RECOMMENDED_SPACE) {
            overallState.setText("Модель совпадает; предварительные условия выглядят лучше. Наличие DSU Loader проверяется только открытием системного экрана.");
            overallState.setTextColor(green);
        } else if (targetModel) {
            overallState.setText("Обнаружен целевой HONOR X8b. Поддержка DSU на этой прошивке пока не доказана — проверь системный загрузчик.");
            overallState.setTextColor(amber);
        } else {
            overallState.setText("Эта сборка рассчитана на HONOR X8b; обнаружена другая модель. Не используй неподходящие образы.");
            overallState.setTextColor(amber);
        }
    }

    private long getFreeBytes() {
        try {
            File data = getFilesDir();
            StatFs stat = new StatFs(data.getAbsolutePath());
            return stat.getAvailableBytes();
        } catch (Exception error) {
            return -1L;
        }
    }

    private String formatBytes(long value) {
        if (value < 0) return "не удалось определить";
        double gib = value / (1024.0 * 1024.0 * 1024.0);
        return String.format(Locale.US, "%.1f ГБ", gib);
    }

    private void openDsuLoader() {
        Intent intent = new Intent(ACTION_START_DSU_LOADER);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        try {
            startActivity(intent);
            Toast.makeText(this,
                    "Открыт запрос системному DSU Loader. Дальнейшие действия и совместимость определяет HONOR/Android.",
                    Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException | SecurityException error) {
            showLoaderUnavailable();
        } catch (Exception error) {
            showLoaderUnavailable();
        }
    }

    private void showLoaderUnavailable() {
        new AlertDialog.Builder(this)
                .setTitle("DSU Loader не открыт")
                .setMessage("На этой прошивке HONOR прямой экран DSU Loader не предоставлен приложению. Проверь «Для разработчиков». Если пункта DSU Loader там тоже нет, DroidSwitch не может включить его самостоятельно.")
                .setPositiveButton("Открыть параметры разработчика",
                        (dialog, which) -> openDeveloperOptions())
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void showDeveloperOptionsHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Как найти DSU Loader")
                .setMessage("1. Открой Настройки → О телефоне.\n"
                        + "2. Нажми «Номер сборки» семь раз, чтобы включить параметры разработчика (если они ещё не включены).\n"
                        + "3. Открой Система и обновления → Для разработчиков.\n"
                        + "4. Найди DSU Loader.\n\n"
                        + "Не включай OEM-разблокировку и USB-отладку ради DSU. Если DSU Loader отсутствует, не пытайся принудительно включать скрытые системные флаги.")
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

    private String safe(String value) {
        return value == null || value.isEmpty() ? "неизвестно" : value;
    }
}
