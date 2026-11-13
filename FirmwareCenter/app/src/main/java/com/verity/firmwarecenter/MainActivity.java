package com.verity.firmwarecenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
            "https://raw.githubusercontent.com/wwuyh631i95g-gif/Verity/main/FirmwareCenter/catalog/firmware-catalog.json";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout page;
    private LinearLayout catalogPanel;
    private TextView catalogStatus;

    private final int bg = Color.rgb(10, 16, 30);
    private final int card = Color.rgb(20, 30, 49);
    private final int text = Color.rgb(238, 243, 251);
    private final int muted = Color.rgb(164, 180, 202);
    private final int accent = Color.rgb(105, 166, 255);
    private final int warning = Color.rgb(255, 205, 112);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(30));
        page.setBackgroundColor(bg);
        scroll.addView(page);
        setContentView(scroll);

        addText("VERITY", 13, accent, true, 2);
        addText("Firmware Center", 30, text, true, 0);
        addText("Центр информации и официального обновления телефона", 14, muted, false, 14);

        addCard(
                "Важно",
                "Это не универсальный прошивальщик. Обычное приложение Android не может переписывать системные разделы, разблокировать закрытый загрузчик или обходить проверку подписи прошивки простыми разрешениями. Здесь доступны безопасная проверка устройства, каталог источников и переход к штатному обновлению HONOR."
        );

        addCard(
                "Целевое устройство",
                "HONOR X8b · семейство LLY-LX1 / LLY-LX2 / LLY-LX3. Варианты отличаются регионом и программной конфигурацией. Перед любым обновлением сравни точный код модели и номер сборки в разделе «Настройки → О телефоне»."
        );

        addActionButton("Проверить сведения об устройстве", v -> showDeviceInfo());
        addActionButton("Открыть штатное обновление ПО", v -> openSystemUpdater());
        addActionButton("Официальная поддержка HONOR X8b", v ->
                openUrl("https://www.honor.com/eurasia/support/phones/honor-x8b/"));

        catalogStatus = addText("Каталог: ещё не загружен", 13, muted, false, 6);
        addActionButton("Обновить каталог из GitHub", v -> loadCatalog());

        catalogPanel = new LinearLayout(this);
        catalogPanel.setOrientation(LinearLayout.VERTICAL);
        page.addView(catalogPanel);

        addCard(
                "Перед любым обновлением",
                "Сделай резервную копию фотографий и важных файлов, заряди телефон и используй только пакет, который штатный сервис предлагает именно для твоей модели, региона и текущей сборки. Не устанавливай файлы от LLY-LX2 или LLY-LX3 на LLY-LX1 и наоборот."
        );

        addText("Разрешения приложения: только INTERNET для загрузки текстового каталога. Root, ADB, USB debugging, доступ к файлам, камере, специальным возможностям и управлению устройством не запрашиваются.",
                12, muted, false, 8);

        loadCatalog();
    }

    private void showDeviceInfo() {
        String model = safe(Build.MODEL);
        String manufacturer = safe(Build.MANUFACTURER);
        String device = safe(Build.DEVICE);
        String product = safe(Build.PRODUCT);
        String fingerprint = safe(Build.FINGERPRINT);
        String all = (model + " " + device + " " + product + " " + fingerprint).toUpperCase();

        String match;
        if (all.contains("LLY-LX1")) {
            match = "Найдено обозначение LLY-LX1. Сверь его также с экраном «О телефоне».";
        } else if (all.contains("LLY-LX2")) {
            match = "Найдено обозначение LLY-LX2. Сверь его также с экраном «О телефоне».";
        } else if (all.contains("LLY-LX3")) {
            match = "Найдено обозначение LLY-LX3. Сверь его также с экраном «О телефоне».";
        } else {
            match = "Код LLY не обнаружен в доступных Android-свойствах. Это не означает несовместимость: проверь точный код вручную в «Настройки → О телефоне».";
        }

        String details = "Производитель: " + manufacturer
                + "\nМодель: " + model
                + "\nDEVICE: " + device
                + "\nPRODUCT: " + product
                + "\nAndroid: " + Build.VERSION.RELEASE
                + "\nAPI: " + Build.VERSION.SDK_INT
                + "\n\n" + match
                + "\n\nОтпечаток сборки: " + fingerprint;

        addCard("Сведения, доступные Android", details);
    }

    private void openSystemUpdater() {
        String[] actions = new String[] {
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
        Toast.makeText(this,
                "Открой Настройки → Система и обновления → Обновление ПО",
                Toast.LENGTH_LONG).show();
    }

    private void loadCatalog() {
        catalogStatus.setText("Каталог: загружаю с GitHub…");
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(CATALOG_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "Verity-Firmware-Center/1.0");

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("HTTP " + code);
                }

                StringBuilder body = new StringBuilder();
                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                JSONObject json = new JSONObject(body.toString());
                runOnUiThread(() -> renderCatalog(json));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    catalogStatus.setText("Каталог сейчас недоступен. Проверь интернет и нажми «Обновить каталог».");
                    catalogPanel.removeAllViews();
                    addCatalogFallback();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void renderCatalog(JSONObject json) {
        catalogPanel.removeAllViews();
        String updated = json.optString("updated", "дата не указана");
        catalogStatus.setText("Каталог v" + json.optInt("catalog_version", 1)
                + " · обновлён " + updated);

        String notice = json.optString("notice", "");
        if (!notice.isEmpty()) {
            addCatalogCard("Примечание каталога", notice, null, null);
        }

        JSONArray entries = json.optJSONArray("entries");
        if (entries == null || entries.length() == 0) {
            addCatalogCard("Нет записей", "Каталог не содержит записей.", null, null);
            return;
        }

        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null) continue;
            addCatalogCard(
                    item.optString("title", "Источник"),
                    item.optString("category", "") + " · " + item.optString("status", "")
                            + "\n\n" + item.optString("description", ""),
                    item.optString("button", "Открыть источник"),
                    item.optString("url", "")
            );
        }
    }

    private void addCatalogFallback() {
        addCatalogCard(
                "Официальная поддержка HONOR X8b",
                "Каталог не загрузился. Можно открыть официальный сайт вручную.",
                "Открыть поддержку",
                "https://www.honor.com/eurasia/support/phones/honor-x8b/"
        );
    }

    private void addCatalogCard(String title, String message, String buttonLabel, String url) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(roundRect(card, 16));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = dp(10);
        catalogPanel.addView(box, boxParams);

        TextView heading = makeText(title, 17, text, true);
        box.addView(heading);
        TextView body = makeText(message, 13, muted, false);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(7);
        box.addView(body, bodyParams);

        if (buttonLabel != null && url != null && !url.trim().isEmpty()) {
            Button button = makeButton(buttonLabel);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(10);
            box.addView(button, params);
            button.setOnClickListener(v -> openUrl(url));
        }
    }

    private void addCard(String title, String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(roundRect(card, 16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        page.addView(box, params);

        TextView heading = makeText(title, 17, text, true);
        box.addView(heading);
        TextView body = makeText(message, 14, muted, false);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(7);
        box.addView(body, bodyParams);
    }

    private TextView addText(String value, int size, int color, boolean bold, int bottom) {
        TextView view = makeText(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottom);
        page.addView(view, params);
        return view;
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

    private void addActionButton(String label, View.OnClickListener listener) {
        Button button = makeButton(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(9);
        page.addView(button, params);
        button.setOnClickListener(listener);
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(48, 105, 190)));
        return button;
    }

    private android.graphics.drawable.GradientDrawable roundRect(int color, int radius) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private void openUrl(String value) {
        try {
            if (!value.startsWith("https://")) {
                Toast.makeText(this, "Открываются только HTTPS-ссылки.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку.", Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "неизвестно" : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
