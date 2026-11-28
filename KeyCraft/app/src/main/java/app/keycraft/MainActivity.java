package app.keycraft;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    public static final String EXTRA_REQUEST_CAMERA = "request_camera";
    private static final String PREFS = "keycraft_settings";
    private static final int REQUEST_CAMERA = 4102;

    private final String[] prefKeys = {
        "up_click", "up_double", "up_long",
        "down_click", "down_double", "down_long"
    };
    private final Spinner[] actionSpinners = new Spinner[6];
    private final List<AppChoice> appChoices = new ArrayList<>();
    private Spinner appSpinner;
    private TextView accessibilityStatus;
    private TextView cameraStatus;
    private LinearLayout page;

    private static final int BG = Color.rgb(10, 15, 30);
    private static final int PANEL = Color.rgb(22, 31, 51);
    private static final int PANEL_ALT = Color.rgb(28, 40, 65);
    private static final int TEXT = Color.rgb(243, 247, 255);
    private static final int MUTED = Color.rgb(161, 177, 205);
    private static final int TEAL = Color.rgb(83, 226, 194);
    private static final int PURPLE = Color.rgb(153, 135, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().setNavigationBarDividerColor(BG);
        }

        ensureDefaults();
        buildScreen();
        handleCameraRequest(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCameraRequest(intent);
    }

    private void handleCameraRequest(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_REQUEST_CAMERA, false)) {
            intent.removeExtra(EXTRA_REQUEST_CAMERA);
            page.postDelayed(this::requestCameraPermission, 450);
        }
    }

    private void ensureDefaults() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        android.content.SharedPreferences.Editor e = p.edit();
        boolean changed = false;
        for (String key : prefKeys) {
            if (!p.contains(key)) {
                e.putString(key, key.endsWith("_click") ? Actions.VOLUME : Actions.NONE);
                changed = true;
            }
        }
        if (!p.contains("target_package")) {
            e.putString("target_package", "");
            changed = true;
        }
        if (changed) e.apply();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("PERSONAL BUTTON SHORTCUTS", 11, TEAL, true);
        eyebrow.setLetterSpacing(0.13f);
        page.addView(eyebrow);

        TextView title = text("KeyCraft", 34, TEXT, true);
        title.setPadding(0, dp(4), 0, 0);
        page.addView(title);

        TextView subtitle = text("Преврати кнопки громкости в быстрые действия.", 15, MUTED, false);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        page.addView(subtitle);

        LinearLayout statusCard = card(page);
        TextView statusTitle = text("Подключение", 18, TEXT, true);
        statusCard.addView(statusTitle);

        accessibilityStatus = text("", 14, MUTED, false);
        accessibilityStatus.setPadding(0, dp(8), 0, dp(12));
        statusCard.addView(accessibilityStatus);

        Button accessButton = button("Включить доступ к кнопкам", PURPLE);
        accessButton.setOnClickListener(v -> openAccessibilitySettings());
        statusCard.addView(accessButton);

        cameraStatus = text("", 13, MUTED, false);
        cameraStatus.setPadding(0, dp(12), 0, dp(7));
        statusCard.addView(cameraStatus);

        Button cameraButton = button("Разрешить фонарик", PANEL_ALT);
        cameraButton.setOnClickListener(v -> requestCameraPermission());
        statusCard.addView(cameraButton);

        addSectionTitle("Назначения кнопок");
        addText("Выбери действие для обычного, двойного и долгого нажатия. По умолчанию одиночное нажатие остаётся обычной регулировкой громкости.");

        addKeyCard("ГРОМКОСТЬ +", "Верхняя кнопка", 0);
        addKeyCard("ГРОМКОСТЬ −", "Нижняя кнопка", 3);

        addSectionTitle("Приложение для быстрого запуска");
        LinearLayout appCard = card(page);
        appCard.addView(text("Выбранное приложение", 16, TEXT, true));
        TextView appHint = text("Его можно назначить на любое нажатие через действие «Открыть выбранное приложение».", 13, MUTED, false);
        appHint.setPadding(0, dp(5), 0, dp(9));
        appCard.addView(appHint);
        buildAppPicker(appCard);

        Button saveButton = button("СОХРАНИТЬ НАЗНАЧЕНИЯ", TEAL);
        saveButton.setTextColor(Color.rgb(8, 24, 30));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        saveParams.topMargin = dp(18);
        page.addView(saveButton, saveParams);
        saveButton.setOnClickListener(v -> saveMappings());

        addSectionTitle("Важно знать");
        LinearLayout infoCard = card(page);
        addInfo(infoCard,
            "Кнопка питания",
            "Android не разрешает обычным приложениям переназначать кнопку питания. KeyCraft настраивает две кнопки громкости; сама кнопка питания остаётся системной.");
        addInfo(infoCard,
            "Как работает доступ",
            "Нужно вручную включить службу KeyCraft в настройках специальных возможностей. Она запрашивает только фильтрацию клавиш громкости и не получает доступ к тексту или содержимому экрана. Данные не отправляются в интернет.");
        addInfo(infoCard,
            "Обычная громкость",
            "Если для кнопки назначено двойное или долгое нажатие, распознавание короткого нажатия занимает примерно треть секунды. Вариант «Обычная громкость» сохраняет регулировку звука.");
        addInfo(infoCard,
            "Совместимость",
            "Проект рассчитан на Android 8 и новее, включая Android 14 на Honor X8b. Одновременно фильтровать клавиши может только одна служба специальных возможностей, поэтому другое приложение для переназначения кнопок иногда придётся отключить.");
        setContentView(scroll);
        updatePermissionStatus();
    }

    private void addKeyCard(String heading, String keyLabel, int offset) {
        LinearLayout box = card(page);
        TextView title = text(heading, 17, TEXT, true);
        box.addView(title);
        TextView sub = text(keyLabel, 12, MUTED, false);
        sub.setPadding(0, dp(2), 0, dp(8));
        box.addView(sub);

        String[] triggerNames = {"Одно нажатие", "Двойное нажатие", "Удержание"};
        for (int i = 0; i < 3; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));
            TextView label = text(triggerNames[i], 13, MUTED, true);
            row.addView(label);

            Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, Actions.LABELS);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            String selected = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(prefKeys[offset + i], i == 0 ? Actions.VOLUME : Actions.NONE);
            spinner.setSelection(indexOfAction(selected));
            row.addView(spinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
            box.addView(row);
            actionSpinners[offset + i] = spinner;
        }
    }

    private void buildAppPicker(LinearLayout appCard) {
        appChoices.clear();
        appChoices.add(new AppChoice("— Не выбрано —", ""));
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolved = pm.queryIntentActivities(launchIntent, 0);
        List<AppChoice> found = new ArrayList<>();
        for (ResolveInfo ri : resolved) {
            String packageName = ri.activityInfo.packageName;
            if (getPackageName().equals(packageName)) continue;
            String label;
            try {
                label = ri.loadLabel(pm).toString();
            } catch (Exception ex) {
                label = packageName;
            }
            found.add(new AppChoice(label, packageName));
        }
        Collections.sort(found, Comparator.comparing(a -> a.label.toLowerCase()));
        appChoices.addAll(found);

        appSpinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (AppChoice choice : appChoices) labels.add(choice.label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appSpinner.setAdapter(adapter);

        String selected = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("target_package", "");
        int selectedIndex = 0;
        for (int i = 0; i < appChoices.size(); i++) {
            if (appChoices.get(i).packageName.equals(selected)) {
                selectedIndex = i;
                break;
            }
        }
        appSpinner.setSelection(selectedIndex);
        appCard.addView(appSpinner, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
    }

    private void addInfo(LinearLayout parent, String title, String body) {
        TextView heading = text(title, 14, TEAL, true);
        heading.setPadding(0, dp(4), 0, dp(3));
        parent.addView(heading);
        TextView detail = text(body, 13, MUTED, false);
        detail.setLineSpacing(dp(2), 1.0f);
        detail.setPadding(0, 0, 0, dp(12));
        parent.addView(detail);
    }

    private void addSectionTitle(String title) {
        TextView t = text(title, 19, TEXT, true);
        t.setPadding(0, dp(20), 0, dp(7));
        page.addView(t);
    }

    private void addText(String value) {
        TextView t = text(value, 13, MUTED, false);
        t.setPadding(0, 0, 0, dp(7));
        page.addView(t);
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL);
        bg.setCornerRadius(dp(20));
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        parent.addView(box, lp);
        return box;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(TEXT);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(15));
        b.setBackground(bg);
        b.setAllCaps(false);
        b.setPadding(dp(12), dp(3), dp(12), dp(3));
        return b;
    }

    private int indexOfAction(String action) {
        for (int i = 0; i < Actions.IDS.length; i++) {
            if (Actions.IDS[i].equals(action)) return i;
        }
        return 1;
    }

    private void saveMappings() {
        android.content.SharedPreferences.Editor edit =
            getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (int i = 0; i < actionSpinners.length; i++) {
            int pos = actionSpinners[i].getSelectedItemPosition();
            edit.putString(prefKeys[i], Actions.IDS[Math.max(0, Math.min(pos, Actions.IDS.length - 1))]);
        }
        if (appSpinner != null && appSpinner.getSelectedItemPosition() < appChoices.size()) {
            edit.putString("target_package",
                appChoices.get(appSpinner.getSelectedItemPosition()).packageName);
        }
        edit.apply();

        sendBroadcast(new Intent(VolumeKeyService.ACTION_REFRESH).setPackage(getPackageName()));
        Toast.makeText(this, "Назначения сохранены", Toast.LENGTH_SHORT).show();
        updatePermissionStatus();
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть настройки специальных возможностей", Toast.LENGTH_LONG).show();
        }
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Разрешение камеры уже выдано. Фонарик готов.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < 23 ||
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName target = new ComponentName(this, VolumeKeyService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String item = splitter.next();
            ComponentName parsed = ComponentName.unflattenFromString(item);
            if (parsed != null && parsed.equals(target)) return true;
        }
        return false;
    }

    private void updatePermissionStatus() {
        if (accessibilityStatus != null) {
            if (isServiceEnabled()) {
                accessibilityStatus.setText("● KeyCraft включён. Назначенные действия готовы.");
                accessibilityStatus.setTextColor(TEAL);
            } else {
                accessibilityStatus.setText("○ Служба ещё не включена. Нажми кнопку ниже и активируй KeyCraft в списке.");
                accessibilityStatus.setTextColor(MUTED);
            }
        }
        if (cameraStatus != null) {
            cameraStatus.setText(hasCameraPermission()
                ? "Фонарик: разрешение выдано."
                : "Фонарик: для этого действия понадобится разрешение камеры.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accessibilityStatus != null) updatePermissionStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            updatePermissionStatus();
            Toast.makeText(this,
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ? "Фонарик разрешён"
                    : "Без разрешения камеры переключение фонарика работать не будет",
                Toast.LENGTH_LONG).show();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppChoice {
        final String label;
        final String packageName;
        AppChoice(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }
}
