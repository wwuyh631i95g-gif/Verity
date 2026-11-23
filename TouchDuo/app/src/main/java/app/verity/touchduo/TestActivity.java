package app.verity.touchduo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class TestActivity extends Activity {
    private TextView status;
    private int maximumPointers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18, 25, 40));

        status = new TextView(this);
        status.setText("Экран проверки\n\nНажми плавающую кнопку DUO. Здесь отобразится, сколько касаний Android получил одновременно.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(24, 24, 24, 24);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(status, lp);

        TextView back = new TextView(this);
        back.setText("НАЗАД");
        back.setTextColor(Color.WHITE);
        back.setTextSize(14);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.rgb(46, 62, 90));
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 64, Gravity.BOTTOM);
        root.addView(back, backLp);
        back.setOnClickListener(v -> finish());

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_POINTER_DOWN
                        || action == MotionEvent.ACTION_MOVE) {
                    maximumPointers = Math.max(maximumPointers, event.getPointerCount());
                    status.setText("СЕЙЧАС ОДНОВРЕМЕННО: " + event.getPointerCount()
                            + "\n\nМаксимум за проверку: " + maximumPointers
                            + "\n\nНажми DUO ещё раз, чтобы повторить.");
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    status.setText("Максимум одновременных точек: " + maximumPointers
                            + "\n\nЕсли показано 2 или больше — виртуальная комбинация проходит.");
                }
                return true;
            }
        });
        setContentView(root);
    }
}
