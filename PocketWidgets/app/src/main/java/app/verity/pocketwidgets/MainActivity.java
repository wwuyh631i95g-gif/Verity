package app.verity.pocketwidgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import java.util.Random;

public final class MainActivity extends Activity {
    private int tapCount, dinosaurJumps, snakeSteps, luckyCount;
    private TextView tapLabel, timerLabel, robotLabel, dinoLabel, bottleLabel, snakeLabel, luckyLabel;
    private CountDownTimer countdown;
    private long stopwatchAccumulated;
    private boolean stopwatchRunning;
    private Chronometer chronometer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(28, 27, 38));
        getWindow().setNavigationBarColor(Color.rgb(28, 27, 38));
        tapCount = getSharedPreferences("pocket_widgets", MODE_PRIVATE).getInt("tap_count", 0);
        boolean compact = getResources().getConfiguration().screenWidthDp <= 360
                || getResources().getConfiguration().screenHeightDp <= 500;
        setContentView(buildScreen(compact));
    }

    private View buildScreen(boolean compact) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = compact ? 10 : 18;
        root.setPadding(dp(pad), dp(14), dp(pad), dp(24));
        root.setBackgroundColor(Color.rgb(17, 16, 26));
        scroll.addView(root);

        TextView title = text("POCKET WIDGETS", compact ? 20 : 26, Color.rgb(255, 215, 75), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());
        TextView subtitle = text(compact ? "МИНИ-ПАНЕЛЬ" : "Виджеты и мини-инструменты", 12, Color.LTGRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        addGap(root, 8);
        root.addView(subtitle, matchWrap());

        addCounterCard(root);
        addTimerCard(root);
        addStopwatchCard(root);
        addRobotCard(root);
        addDinoCard(root);
        addBottleCard(root);
        addSnakeCard(root);
        addLuckyCard(root);
        addMinesCard(root);

        TextView hint = text("Для размещения виджета: удерживай пустое место на рабочем столе → Виджеты → Pocket Widgets. На часах приложение автоматически использует компактную раскладку.", 11, Color.rgb(190, 183, 214), false);
        hint.setPadding(dp(8), dp(16), dp(8), dp(8));
        root.addView(hint, matchWrap());
        return scroll;
    }

    private void addCounterCard(LinearLayout root) {
        LinearLayout card = card(root, "БЫСТРАЯ КНОПКА · АНТИСТРЕСС");
        tapLabel = text("Нажатий: " + tapCount, 17, Color.WHITE, true);
        card.addView(tapLabel, matchWrap());
        addButton(card, "НАЖАТЬ  +1", v -> {
            tapCount++;
            getSharedPreferences("pocket_widgets", MODE_PRIVATE).edit().putInt("tap_count", tapCount).apply();
            tapLabel.setText("Нажатий: " + tapCount);
            sendWidgetRefresh();
        });
    }

    private void addTimerCard(LinearLayout root) {
        LinearLayout card = card(root, "ТАЙМЕР");
        timerLabel = text("01:00", 24, Color.WHITE, true);
        card.addView(timerLabel, matchWrap());
        LinearLayout row = row(card);
        addButton(row, "1 МИН", v -> startTimer(60_000));
        addButton(row, "5 МИН", v -> startTimer(300_000));
        addButton(row, "СТОП", v -> {
            if (countdown != null) countdown.cancel();
            timerLabel.setText("ОСТАНОВЛЕН");
        });
    }

    private void startTimer(long duration) {
        if (countdown != null) countdown.cancel();
        countdown = new CountDownTimer(duration, 250) {
            @Override public void onTick(long left) {
                long sec = left / 1000;
                timerLabel.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
            }
            @Override public void onFinish() {
                timerLabel.setText("ГОТОВО!");
                Toast.makeText(MainActivity.this, "Таймер завершён", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void addStopwatchCard(LinearLayout root) {
        LinearLayout card = card(root, "СЕКУНДОМЕР");
        chronometer = new Chronometer(this);
        chronometer.setTextColor(Color.WHITE);
        chronometer.setTextSize(22);
        chronometer.setFormat("%s");
        card.addView(chronometer, matchWrap());
        LinearLayout row = row(card);
        addButton(row, "СТАРТ", v -> {
            if (!stopwatchRunning) {
                chronometer.setBase(android.os.SystemClock.elapsedRealtime() - stopwatchAccumulated);
                chronometer.start();
                stopwatchRunning = true;
            }
        });
        addButton(row, "ПАУЗА", v -> {
            if (stopwatchRunning) {
                chronometer.stop();
                stopwatchAccumulated = android.os.SystemClock.elapsedRealtime() - chronometer.getBase();
                stopwatchRunning = false;
            }
        });
        addButton(row, "СБРОС", v -> {
            chronometer.stop();
            stopwatchRunning = false;
            stopwatchAccumulated = 0;
            chronometer.setBase(android.os.SystemClock.elapsedRealtime());
            chronometer.setText("00:00");
        });
    }

    private void addRobotCard(LinearLayout root) {
        LinearLayout card = card(root, "РОБОТ-ПОМОЩНИК");
        robotLabel = text("Привет! Нажми кнопку — у меня будет новая реплика.", 14, Color.WHITE, false);
        card.addView(robotLabel, matchWrap());
        addButton(card, "ПОГОВОРИТЬ", v -> {
            String[] phrases = {
                "Проверка связи: всё работает.",
                "Хорошая идея — сохранить её в заметки.",
                "Сейчас режим спокойствия.",
                "Я на связи. Что будем собирать дальше?",
                "Маленький шаг — тоже прогресс."
            };
            robotLabel.setText(phrases[random.nextInt(phrases.length)]);
        });
    }

    private void addDinoCard(LinearLayout root) {
        LinearLayout card = card(root, "МИНИ-ИГРА · ДИНОЗАВР");
        dinoLabel = text("🦖  Прыжки: 0", 18, Color.WHITE, true);
        card.addView(dinoLabel, matchWrap());
        addButton(card, "ПРЫГНУТЬ", v -> {
            dinosaurJumps++;
            dinoLabel.setText("🦖  Прыжки: " + dinosaurJumps + "  ·  Очки: " + (dinosaurJumps * 5));
            dinoLabel.animate().translationY(-dp(18)).setDuration(110)
                    .withEndAction(() -> dinoLabel.animate().translationY(0).setDuration(130).start()).start();
        });
    }

    private void addBottleCard(LinearLayout root) {
        LinearLayout card = card(root, "ЦИФРОВОЙ FLIP-СИМУЛЯТОР");
        bottleLabel = text("◉", 42, Color.rgb(110, 205, 255), true);
        bottleLabel.setGravity(Gravity.CENTER);
        card.addView(bottleLabel, matchWrap());
        addButton(card, "ВИРТУАЛЬНО ПОДБРОСИТЬ", v -> {
            bottleLabel.setRotation(0f);
            ObjectAnimator spin = ObjectAnimator.ofFloat(bottleLabel, View.ROTATION, 0f, 720f);
            spin.setDuration(650);
            spin.setInterpolator(new AccelerateDecelerateInterpolator());
            spin.start();
            bottleLabel.setText(random.nextBoolean() ? "✓" : "↻");
        });
    }

    private void addSnakeCard(LinearLayout root) {
        LinearLayout card = card(root, "МИНИ-ЗМЕЙКА");
        snakeLabel = text("● · · · ·    Очки: 0", 18, Color.rgb(115, 235, 160), true);
        card.addView(snakeLabel, matchWrap());
        LinearLayout row = row(card);
        addButton(row, "←", v -> stepSnake(-1));
        addButton(row, "●", v -> stepSnake(1));
        addButton(row, "→", v -> stepSnake(2));
    }

    private void stepSnake(int direction) {
        snakeSteps += direction == -1 ? -1 : 1;
        if (snakeSteps < 0) snakeSteps = 0;
        StringBuilder trail = new StringBuilder();
        for (int i = 0; i < snakeSteps % 8; i++) trail.append("· ");
        snakeLabel.setText(trail + "●    Очки: " + snakeSteps);
    }

    private void addLuckyCard(LinearLayout root) {
        LinearLayout card = card(root, "СЧЁТЧИК 6–7");
        luckyLabel = text("6–7: 0", 18, Color.WHITE, true);
        card.addView(luckyLabel, matchWrap());
        addButton(card, "ДОБАВИТЬ", v -> {
            luckyCount++;
            luckyLabel.setText("6–7: " + luckyCount);
        });
    }

    private void addMinesCard(LinearLayout root) {
        LinearLayout card = card(root, "МИНИ-САПЁР · 4×4");
        TextView info = text("Открывай клетки и находи скрытые метки.", 12, Color.LTGRAY, false);
        card.addView(info, matchWrap());
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        boolean[][] mines = new boolean[4][4];
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) mines[y][x] = random.nextInt(5) == 0;
        for (int y = 0; y < 4; y++) {
            LinearLayout line = row(grid);
            for (int x = 0; x < 4; x++) {
                final boolean mine = mines[y][x];
                Button button = button("·");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(button, lp);
                button.setOnClickListener(v -> {
                    button.setText(mine ? "✦" : "○");
                    button.setEnabled(false);
                    button.setAlpha(0.85f);
                });
            }
        }
        card.addView(grid, matchWrap());
        addButton(card, "НОВОЕ ПОЛЕ", v -> {
            Toast.makeText(this, "Закрой и открой приложение, чтобы создать новую сетку.", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendWidgetRefresh() {
        android.content.Intent update = new android.content.Intent(this, WidgetActionReceiver.class);
        update.setAction(WidgetActionReceiver.ACTION_REFRESH);
        sendBroadcast(update);
    }

    private LinearLayout card(LinearLayout root, String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(35, 32, 51));
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(72, 65, 97));
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        root.addView(box, lp);
        TextView heading = text(title, 13, Color.rgb(255, 215, 75), true);
        box.addView(heading, matchWrap());
        return box;
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.HORIZONTAL);
        result.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(result, matchWrap());
        return result;
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button b = button(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
        lp.setMargins(dp(3), dp(6), dp(3), dp(2));
        parent.addView(b, lp);
        b.setOnClickListener(listener);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void addGap(LinearLayout parent, int size) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(size)));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (countdown != null) countdown.cancel();
        if (chronometer != null && stopwatchRunning) chronometer.stop();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
