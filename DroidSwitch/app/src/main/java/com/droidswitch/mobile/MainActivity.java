package com.droidswitch.mobile;

import android.app.Activity;
import android.content.ComponentName;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String CATALOG_URL =
        "https://raw.githubusercontent.com/wwuyh631i95g-gif/Verity/main/DroidSwitch/catalog/systems.json";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final int background = Color.rgb(9, 15, 28);
    private final int panel = Color.rgb(20, 30, 48);
    private final int primary = Color.rgb(240, 245, 255);
    private final int secondary = Color.rgb(168, 184, 207);
    private final int blue = Color.rgb(83, 151, 255);
    private final int amber = Color.rgb(255, 198, 104);
    private LinearLayout root;
    private LinearLayout catalog;
    private TextView dsuStatus;
    private TextView catalogStatus;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
        root.setBackgroundColor(background);
        scroll.addView(root);
        setContentView(scroll);

        addText("DROIDSWITCH  /  SYSTEM LAB", 12, blue, true, 5);
        addText("Системы телефона", 29, primary, true, 2);
        addText("Проверка совместимости и безопасные способы попробовать другую Android-систему.",
                14, secondary, false, 16);

        addCard("Почему приложение не притворяется компьютером",
                "Эмуляция рабочего стола не превращает Android-приложение в настоящий ПК-прошивальщик. Когда сам телефон перезагружается в загрузчик, Android-приложение прекращает работу. Обычные разрешения не дают доступ к закрытым разделам памяти и не разблокируют загрузчик.");

        addCard("Твой телефон",
                "Цель: HONOR X8b. Приложение читает только общедоступные свойства Android. Никакие IMEI, серийные номера или личные файлы не запрашиваются.");
        addButton("Проверить модель и версию системы", v -> showDeviceInfo());

        dsuStatus = addText("Проверка DSU: выполняется…", 13, amber, false, 7);
        addButton("Проверить наличие системного DSU", v -> checkDsu(true));
        addButton("Открыть параметры разработчика", v -> openDeveloperOptions());
        addButton("Открыть обновление HONOR", v -> openSystemUpdate());

        addCard("Что такое DSU",
                "Dynamic System Updates — предусмотренный Android способ временно запустить гостевую системную сборку, не заменяя исходную систему. Если производитель включил DSU, можно попробовать совместимый доверенный образ, а затем вернуться к исходной системе через системное уведомление. Это не гарантировано на HONOR X8b.");

        addText("КАТАЛОГ СИСТЕМ", 12, blue, true, 3);
        catalogStatus = addText("Каталог ещё не загружен", 13, secondary, false, 5);
        addButton("Обновить каталог из интернета", v -> loadCatalog());
        catalog = new LinearLayout(this);
        catalog.setOrientation(LinearLayout.VERTICAL);
        root.addView(catalog);

        addCard("Важная проверка перед любым экспериментом",
                "Стандартные GSI обычно требуют полностью совместимого Project Treble-устройства и разблокированного загрузчика. Android отдельно предупреждает, что неподходящий образ может сделать устройство незагружаемым. Пока нет подтверждённого образа именно для твоего LLY-варианта, кнопки установки прошивки здесь не будет.");

        addText("Разрешения: только INTERNET для загрузки каталога. Нет Root, ADB, доступа к файлам, USB, Accessibility, Device Admin или Overlay.",
                12, secondary, false, 6);

        checkDsu(false);
        loadCatalog();
    }

    private void showDeviceInfo() {
        String manufacturer = safe(Build.MANUFACTURER);
        String model = safe(Build.MODEL);
        String device = safe(Build.DEVICE);
        String product = safe(Build.PRODUCT);
        String board = safe(Build.BOARD);
        String version = safe(Build.VERSION.RELEASE);
        String fingerprint = safe(Build.FINGERPRINT);

        String all = (model + " " + device + " " + product + " " + fingerprint).toUpperCase();
        String variant;
        if (all.contains("LLY-LX1")) {
            variant = "Обнаружено обозначение LLY-LX1. Сверь его с Настройки → О телефоне.";
        } else if (all.contains("LLY-LX2")) {
            variant = "Обнаружено обозначение LLY-LX2. Сверь его с Настройки → О телефоне.";
        } else if (all.contains("LLY-LX3")) {
            variant = "Обнаружено обозначение LLY-LX3. Сверь его с Настройки → О телефоне.";
        } else if (all.contains("HNLLY")) {
            variant = "Обнаружен идентификатор семейства HNLLY. Точный код LLY-LX необходимо сверить вручную в Настройки → О телефоне.";
        } else {
            variant = "Точный код LLY не найден в открытых свойствах. Проверь модель вручную в Настройки → О телефоне.";
        }

        String details = "Производитель: " + manufacturer
                + "\nМодель: " + model
                + "\nКод устройства: " + device
                + "\nПродукт: " + product
                + "\nПлата: " + board
                + "\nAndroid: " + version
                + "\nУровень API: " + Build.VERSION.SDK_INT
                + "\nАрхитектура: " + join(Build.SUPPORTED_ABIS)
                + "\n\n" + variant
                + "\n\nОтпечаток сборки: " + fingerprint;
        addCard("Результат проверки устройства", details);
    }

    private void checkDsu(boolean showMessage) {
        boolean installed = false;
        String version = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.android.dynsystem", 0);
            installed = true;
            if (info.versionName != null) version = " (версия " + info.versionName + ")";
        } catch (PackageManager.NameNotFoundException ignored) {
            installed = false;
        } catch (Exception ignored) {
            installed = false;
        }

        if (installed) {
            dsuStatus.setText("DSU-компонент найден" + version
                    + ". Это хороший признак, но не доказательство, что HONOR разрешает запуск DSU/GSI.");
            dsuStatus.setTextColor(Color.rgb(125, 220, 164));
            if (showMessage) {
                Toast.makeText(this,
                        "Компонент обнаружен. Теперь проверь, есть ли DSU Loader в параметрах разработчика.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            dsuStatus.setText("DSU-компонент не обнаружен через Android Package Manager. Возможно, HONOR не включает DSU на этой сборке.");
            dsuStatus.setTextColor(amber);
            if (showMessage) {
                Toast.makeText(this, "Компонент DSU не обнаружен.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openDeveloperOptions() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(this, "Не удалось открыть настройки.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openSystemUpdate() {
        String[] actions = {
            "android.settings.SYSTEM_UPDATE_SETTINGS",
            "android.settings.DEVICE_INFO_SETTINGS",
            Settings.ACTION_SETTINGS
        };
        for (String action : actions) {
            try {
                Intent intent = new Intent(action);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(this, "Открой Настройки → Система и обновления → Обновление ПО.",
                Toast.LENGTH_LONG).show();
    }

    private void loadCatalog() {
        catalogStatus.setText("Загружаю каталог…");
        worker.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "DroidSwitch/1.0");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);

                StringBuilder body = new StringBuilder();
                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }

                JSONObject json = new JSONObject(body.toString());
                runOnUiThread(() -> displayCatalog(json));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    catalogStatus.setText("Каталог не загрузился. Проверь соединение и попробуй ещё раз.");
                    catalog.removeAllViews();
                    addCatalogCard("Официальная поддержка HONOR X8b",
                            "Можно открыть официальный сайт поддержки вручную.",
                            "Открыть сайт",
                            "https://www.honor.com/eurasia/support/phones/honor-x8b/");
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void displayCatalog(JSONObject json) {
        catalog.removeAllViews();
        catalogStatus.setText("Каталог v" + json.optInt("catalog_version", 1)
                + " · обновлён " + json.optString("updated", "дата неизвестна"));

        JSONArray entries = json.optJSONArray("entries");
        if (entries == null || entries.length() == 0) {
            addCatalogCard("Каталог пуст", "В удалённом каталоге нет записей.", null, null);
            return;
        }
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null) continue;
            addCatalogCard(
                    item.optString("title", "Система"),
                    item.optString("kind", "") + "\n"
                            + item.optString("compatibility", "") + "\n\n"
                            + item.optString("details", ""),
                    "Открыть официальную информацию",
                    item.optString("url", "")
            );
        }

        String notice = json.optString("notice", "");
        if (!notice.isEmpty()) {
            addCatalogCard("Статус совместимости", notice, null, null);
        }
    }

    private void addCatalogCard(String title, String message, String buttonLabel, String url) {
        LinearLayout box = makePanel();
        addTextTo(box, title, 17, primary, true, 6);
        addTextTo(box, message, 13, secondary, false, 0);
        if (buttonLabel != null && url != null && url.startsWith("https://")) {
            Button button = makeButton(buttonLabel);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(10);
            box.addView(button, params);
            button.setOnClickListener(v -> openUrl(url));
        }
        LinearLayout.LayoutParams catalogParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        catalogParams.bottomMargin = dp(11);
        catalog.addView(box, catalogParams);
    }

    private void addCard(String title, String message) {
        LinearLayout box = makePanel();
        addTextTo(box, title, 17, primary, true, 7);
        addTextTo(box, message, 14, secondary, false, 0);
        addPanelToRoot(box);
    }

    private LinearLayout makePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(panel);
        drawable.setCornerRadius(dp(17));
        box.setBackground(drawable);
        return box;
    }

    private void addPanelToRoot(LinearLayout box) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(11);
        root.addView(box, params);
    }

    private TextView addText(String value, int size, int color, boolean bold, int bottom) {
        TextView view = makeText(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottom);
        root.addView(view, params);
        return view;
    }

    private void addTextTo(LinearLayout parent, String value, int size, int color,
                           boolean bold, int bottom) {
        TextView view = makeText(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottom);
        parent.addView(view, params);
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        view.setLineSpacing(dp(2), 1.0f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addButton(String title, View.OnClickListener listener) {
        Button button = makeButton(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(9);
        root.addView(button, params);
        button.setOnClickListener(listener);
    }

    private Button makeButton(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(39, 100, 190)));
        return button;
    }

    private void openUrl(String value) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку.", Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "неизвестно" : value;
    }

    private String join(String[] values) {
        if (values == null || values.length == 0) return "неизвестно";
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(", ");
            out.append(value);
        }
        return out.toString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
