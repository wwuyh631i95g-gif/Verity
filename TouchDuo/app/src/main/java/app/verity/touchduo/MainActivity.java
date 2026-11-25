package app.verity.touchduo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS = "touchduo_settings";
    static final String ACTION_REFRESH = "app.verity.touchduo.REFRESH_OVERLAY";

    private EditText xA, yA, xB, yB;
    private SeekBar durationBar;
    private TextView durationText, statusText;
    private Switch overlaySwitch;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        loadSettings();
    }

    @Override protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private EditText number(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setTextSize(16);
        e.setHint(hint);
        e.setEms(3);
        e.setGravity(Gravity.CENTER);
        return e;
    }

    private void gap(LinearLayout box, int h) {
        View v = new View(this);
        box.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(rounded(Color.WHITE, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        parent.addView(c, lp);
        return c;
    }

    private void coordinateRow(LinearLayout box, String title, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label(title, 15, Color.rgb(35, 48, 68), true),
                new LinearLayout.LayoutParams(0, dp(54), 1));
        row.addView(label("X", 13, Color.DKGRAY, true));
        EditText x = number("50");
        row.addView(x, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView yLabel = label("Y", 13, Color.DKGRAY, true);
        yLabel.setPadding(dp(8), 0, 0, 0);
        row.addView(yLabel);
        EditText y = number("50");
        row.addView(y, new LinearLayout.LayoutParams(dp(50), dp(50)));
        box.addView(row);
        if (first) { xA = x; yA = y; } else { xB = x; yB = y; }
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(16), dp(14), dp(20));
        root.setBackgroundColor(Color.rgb(239, 243, 250));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(label("TouchDuo", 28, Color.rgb(22, 40, 67), true));
        root.addView(label("ОДНО НАЖАТИЕ → ДВА КАСАНИЯ ОДНОВРЕМЕННО",
                11, Color.rgb(35, 105, 195), true));
        gap(root, 12);

        LinearLayout statusCard = card(root);
        statusText = label("Проверка службы…", 14, Color.rgb(43, 58, 80), true);
        statusCard.addView(statusText);

        LinearLayout intro = card(root);
        intro.addView(label("Это не автокликер", 18, Color.rgb(22, 40, 67), true));
        gap(intro, 5);
        intro.addView(label(
                "Плавающая кнопка A+B отправляет в игру две виртуальные точки одним общим жестом. Обе точки нажимаются в один момент и удерживаются заданное время. Повторных нажатий и автоматического цикла нет.",
                14, Color.rgb(62, 75, 96), false));

        LinearLayout coords = card(root);
        coords.addView(label("1. Укажи две кнопки игры", 18, Color.rgb(22, 40, 67), true));
        gap(coords, 3);
        coords.addView(label(
                "Введи положение центров игровых кнопок в процентах от всего экрана: X — слева направо, Y — сверху вниз.",
                13, Color.rgb(82, 94, 112), false));
        gap(coords, 6);
        coordinateRow(coords, "Точка A", true);
        coordinateRow(coords, "Точка B", false);

        LinearLayout hold = card(root);
        hold.addView(label("2. Время одновременного удержания", 17, Color.rgb(22, 40, 67), true));
        durationText = label("300 мс", 15, Color.rgb(35, 105, 195), true);
        hold.addView(durationText);
        durationBar = new SeekBar(this);
        durationBar.setMax(1900);
        durationBar.setProgress(200);
        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                durationText.setText((progress + 100) + " мс");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        hold.addView(durationBar);

        LinearLayout options = card(root);
        overlaySwitch = new Switch(this);
        overlaySwitch.setText("Показывать кнопку A+B поверх игры");
        overlaySwitch.setTextSize(14);
        options.addView(overlaySwitch);
        overlaySwitch.setOnCheckedChangeListener((view, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("overlay_enabled", checked).apply();
            refreshOverlay();
        });
        gap(options, 5);
        options.addView(label(
                "Кнопку A+B можно перетаскивать по экрану. Один короткий тап по ней отправляет пару точек A и B одновременно.",
                13, Color.rgb(82, 94, 112), false));

        Button save = button("СОХРАНИТЬ КОМБИНАЦИЮ");
        root.addView(save, new LinearLayout.LayoutParams(-1, dp(50)));
        save.setOnClickListener(v -> saveSettings());

        Button enable = button("ВКЛЮЧИТЬ СЛУЖБУ TOUCHDUO");
        root.addView(enable, new LinearLayout.LayoutParams(-1, dp(50)));
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button test = button("ПРОВЕРИТЬ ДВЕ ТОЧКИ");
        root.addView(test, new LinearLayout.LayoutParams(-1, dp(50)));
        test.setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));

        gap(root, 10);
        root.addView(label(
                "Важно: служба Android отправляет пару касаний программно. Она отменяет касания, которые уже выполняются в этот момент. Поэтому нажимать нужно именно плавающую кнопку A+B — не держать один физический палец в игре и пытаться добавить второй. Работоспособность зависит от того, принимает ли конкретная игра виртуальные мультитач-события.",
                12, Color.rgb(143, 72, 42), false));
    }

    private void loadSettings() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        xA.setText(String.valueOf(p.getInt("x1", 18)));
        yA.setText(String.valueOf(p.getInt("y1", 82)));
        xB.setText(String.valueOf(p.getInt("x2", 82)));
        yB.setText(String.valueOf(p.getInt("y2", 82)));
        durationBar.setProgress(Math.max(0, Math.min(1900, p.getInt("duration_ms", 300) - 100)));
        overlaySwitch.setChecked(p.getBoolean("overlay_enabled", true));
    }

    private int numberValue(EditText field, int fallback) {
        try {
            return Math.max(0, Math.min(100,
                    Integer.parseInt(field.getText().toString().trim())));
        } catch (Exception ignored) { return fallback; }
    }

    private void saveSettings() {
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        e.putInt("x1", numberValue(xA, 18));
        e.putInt("y1", numberValue(yA, 82));
        e.putInt("x2", numberValue(xB, 82));
        e.putInt("y2", numberValue(yB, 82));
        e.putInt("duration_ms", durationBar.getProgress() + 100);
        e.apply();
        refreshOverlay();
        Toast.makeText(this, "Пара A+B сохранена", Toast.LENGTH_SHORT).show();
    }

    private void refreshOverlay() {
        sendBroadcast(new Intent(ACTION_REFRESH).setPackage(getPackageName()));
    }

    private void updateServiceStatus() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String component = new ComponentName(this, TouchAccessibilityService.class)
                .flattenToString();
        boolean active = enabled != null &&
                enabled.toLowerCase().contains(component.toLowerCase());
        statusText.setText(active
                ? "Служба включена — кнопку A+B можно использовать поверх игры."
                : "Служба выключена — включи TouchDuo в специальных возможностях Android.");
    }
}
