package app.verity.touchduo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS = "touchduo_settings";
    static final String ACTION_REFRESH = "app.verity.touchduo.REFRESH_OVERLAY";

    private EditText[] xs = new EditText[4];
    private EditText[] ys = new EditText[4];
    private EditText durationField;
    private CheckBox thirdPoint;
    private CheckBox fourthPoint;
    private Switch overlaySwitch;
    private TextView serviceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadFields();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeText(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private void addGap(LinearLayout root, int height) {
        View gap = new View(this);
        root.addView(gap, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private EditText numberField(String hint) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setHint(hint);
        edit.setTextSize(15);
        edit.setEms(3);
        edit.setGravity(Gravity.CENTER);
        return edit;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(20));
        root.setBackgroundColor(Color.rgb(246, 248, 252));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = makeText("TouchDuo", 27, Color.rgb(24, 37, 62));
        title.setTypeface(null, 1);
        root.addView(title);
        root.addView(makeText("Виртуальная комбинация из нескольких касаний", 14, Color.rgb(76, 91, 116)));
        addGap(root, 10);

        serviceStatus = makeText("", 14, Color.rgb(31, 112, 71));
        root.addView(serviceStatus);
        addGap(root, 10);

        TextView note = makeText(
                "TouchDuo не меняет сенсорный контроллер часов. Он отправляет в Android одну виртуальную комбинацию из 2–4 точек, когда ты нажимаешь плавающую кнопку DUO. Точки задаются в процентах от размера экрана.",
                14, Color.rgb(42, 54, 75));
        root.addView(note);
        addGap(root, 14);

        root.addView(makeText("Координаты касаний", 18, Color.rgb(24, 37, 62)));
        root.addView(makeText("X — слева направо, Y — сверху вниз. 0–100%.", 12, Color.DKGRAY));
        addGap(root, 6);

        int[] defaultsX = {0, 18, 82, 50, 50};
        int[] defaultsY = {0, 82, 82, 48, 68};
        for (int i = 0; i < 4; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = makeText("Точка " + (i + 1), 14, Color.rgb(42, 54, 75));
            row.addView(label, new LinearLayout.LayoutParams(0, dp(48), 1));
            row.addView(makeText("X", 13, Color.DKGRAY));
            xs[i] = numberField("18");
            xs[i].setText(String.valueOf(defaultsX[i + 1]));
            row.addView(xs[i], new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView yLabel = makeText("Y", 13, Color.DKGRAY);
            yLabel.setPadding(dp(6), 0, 0, 0);
            row.addView(yLabel);
            ys[i] = numberField("82");
            ys[i].setText(String.valueOf(defaultsY[i + 1]));
            row.addView(ys[i], new LinearLayout.LayoutParams(dp(48), dp(48)));
            root.addView(row);
        }

        thirdPoint = new CheckBox(this);
        thirdPoint.setText("Использовать третью точку");
        root.addView(thirdPoint);
        fourthPoint = new CheckBox(this);
        fourthPoint.setText("Использовать четвёртую точку");
        root.addView(fourthPoint);

        LinearLayout durationRow = new LinearLayout(this);
        durationRow.setGravity(Gravity.CENTER_VERTICAL);
        durationRow.addView(makeText("Длительность касания (мс)", 14, Color.rgb(42, 54, 75)),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        durationField = numberField("180");
        durationField.setText("180");
        durationRow.addView(durationField, new LinearLayout.LayoutParams(dp(66), dp(48)));
        root.addView(durationRow);
        addGap(root, 10);

        Button save = new Button(this);
        save.setText("Сохранить настройки");
        root.addView(save, new LinearLayout.LayoutParams(-1, dp(48)));
        save.setOnClickListener(v -> saveSettings());

        Button accessibility = new Button(this);
        accessibility.setText("Включить TouchDuo в специальных возможностях");
        root.addView(accessibility, new LinearLayout.LayoutParams(-1, dp(52)));
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button test = new Button(this);
        test.setText("Открыть экран проверки");
        root.addView(test, new LinearLayout.LayoutParams(-1, dp(48)));
        test.setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));

        overlaySwitch = new Switch(this);
        overlaySwitch.setText("Показывать плавающую кнопку DUO");
        root.addView(overlaySwitch);
        overlaySwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("overlay_enabled", checked).apply();
            refreshServiceOverlay();
        });

        addGap(root, 12);
        TextView limitation = makeText(
                "Важно: Android отменяет уже идущие касания, когда AccessibilityService отправляет виртуальный жест. Поэтому это помощник для заранее настроенных одновременных комбинаций, а не полноценное превращение одноточечного экрана в настоящий мультитач. Нельзя одновременно удерживать палец на экране и добавить к нему виртуальный палец.",
                13, Color.rgb(135, 68, 42));
        root.addView(limitation);
    }

    private void loadFields() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        int[] dx = {18, 82, 50, 50};
        int[] dy = {82, 82, 48, 68};
        for (int i = 0; i < 4; i++) {
            xs[i].setText(String.valueOf(p.getInt("x" + (i + 1), dx[i])));
            ys[i].setText(String.valueOf(p.getInt("y" + (i + 1), dy[i])));
        }
        thirdPoint.setChecked(p.getInt("point_count", 2) >= 3);
        fourthPoint.setChecked(p.getInt("point_count", 2) >= 4);
        durationField.setText(String.valueOf(p.getInt("duration_ms", 180)));
        overlaySwitch.setChecked(p.getBoolean("overlay_enabled", true));
    }

    private int read(EditText field, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(field.getText().toString().trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void saveSettings() {
        int count = fourthPoint.isChecked() ? 4 : (thirdPoint.isChecked() ? 3 : 2);
        int[] dx = {18, 82, 50, 50};
        int[] dy = {82, 82, 48, 68};
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (int i = 0; i < 4; i++) {
            editor.putInt("x" + (i + 1), read(xs[i], dx[i], 0, 100));
            editor.putInt("y" + (i + 1), read(ys[i], dy[i], 0, 100));
        }
        editor.putInt("point_count", count);
        editor.putInt("duration_ms", read(durationField, 180, 80, 700));
        editor.apply();
        refreshServiceOverlay();
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
    }

    private void refreshServiceOverlay() {
        sendBroadcast(new Intent(ACTION_REFRESH).setPackage(getPackageName()));
    }

    private void updateServiceStatus() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String component = new ComponentName(this, TouchAccessibilityService.class).flattenToString();
        boolean active = enabled != null && enabled.toLowerCase().contains(component.toLowerCase());
        serviceStatus.setText(active
                ? "Служба включена. Плавающая кнопка должна появиться поверх приложений."
                : "Служба пока выключена. Нажми кнопку ниже и включи TouchDuo в списке.");
    }
}
