package app.verity.touchduo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private void gap(LinearLayout root, int height) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout card(LinearLayout root) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(17));
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(13);
        root.addView(c, lp);
        return c;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(18), dp(15), dp(22));
        root.setBackgroundColor(Color.rgb(239, 243, 250));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(text("TouchDuo", 29, Color.rgb(22, 40, 67), true));
        root.addView(text("РУЧНАЯ ПРОВЕРКА ДВУХ ПАЛЬЦЕВ", 11,
                Color.rgb(35, 105, 195), true));
        gap(root, 13);

        LinearLayout mainCard = card(root);
        mainCard.addView(text("Теперь без автокасаний", 19,
                Color.rgb(22, 40, 67), true));
        gap(mainCard, 7);
        mainCard.addView(text(
                "В этой версии нет плавающей кнопки A+B и нет автоматических нажатий. Ты сам нажимаешь на экран двумя пальцами, а приложение показывает, сколько отдельных касаний часы действительно распознали одновременно.",
                15, Color.rgb(58, 72, 94), false));

        LinearLayout how = card(root);
        how.addView(text("Как проверить", 18, Color.rgb(22, 40, 67), true));
        gap(how, 7);
        how.addView(text(
                "1. Открой экран проверки.\n\n2. Поставь один палец на левую часть экрана.\n\n3. Не убирая первый палец, одновременно поставь второй палец на правую часть.\n\n4. Посмотри число активных касаний и максимальное число.",
                14, Color.rgb(58, 72, 94), false));

        Button test = new Button(this);
        test.setText("ПРОВЕРИТЬ ФИЗИЧЕСКИЙ MULTITOUCH");
        test.setAllCaps(false);
        root.addView(test, new LinearLayout.LayoutParams(-1, dp(58)));
        test.setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));

        gap(root, 13);
        LinearLayout note = card(root);
        note.addView(text("Важное ограничение", 18, Color.rgb(147, 73, 40), true));
        gap(note, 6);
        note.addView(text(
                "Приложение не может включить мультитач, если сам сенсор часов сообщает Android только одно касание. Если тест покажет 2, значит часы распознают два физических пальца — тогда пробуй нажимать кнопки игры напрямую. Если тест показывает только 1, обычное приложение не сможет исправить ограничение сенсорного контроллера.",
                14, Color.rgb(88, 68, 59), false));

        LinearLayout footer = card(root);
        footer.addView(text("Для W&O X9 Call · Android 8.1 · экран 320×385",
                12, Color.rgb(100, 112, 130), false));
    }
}
