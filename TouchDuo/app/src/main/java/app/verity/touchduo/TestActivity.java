package app.verity.touchduo;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class TestActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(new TouchTestView());
    }

    private class TouchTestView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int current = 0;
        private int maximum = 0;
        private String message = "Нажми плавающую кнопку A+B";
        private final float[] px = new float[10];
        private final float[] py = new float[10];

        TouchTestView() {
            super(TestActivity.this);
            setBackgroundColor(Color.rgb(15, 25, 43));
            setClickable(true);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(21);
            canvas.drawText("ПРОВЕРКА MULTITOUCH", w / 2, h * 0.18f, paint);

            paint.setColor(Color.rgb(185, 201, 222));
            paint.setTextSize(15);
            canvas.drawText(message, w / 2, h * 0.27f, paint);

            paint.setColor(Color.rgb(45, 125, 220));
            paint.setTextSize(46);
            canvas.drawText(String.valueOf(current), w / 2, h * 0.48f, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(16);
            canvas.drawText("Одновременно сейчас", w / 2, h * 0.54f, paint);
            canvas.drawText("Максимум: " + maximum, w / 2, h * 0.60f, paint);

            for (int i = 0; i < current && i < px.length; i++) {
                paint.setColor(i == 0 ? Color.rgb(48, 210, 150) : Color.rgb(255, 190, 60));
                canvas.drawCircle(px[i], py[i], 18, paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(13);
                canvas.drawText(i == 0 ? "A" : "B", px[i], py[i] + 5, paint);
            }

            paint.setColor(Color.rgb(175, 190, 212));
            paint.setTextSize(13);
            canvas.drawText("Если максимум равен 2, Android доставил", w / 2, h * 0.84f, paint);
            canvas.drawText("две точки в одном жесте.", w / 2, h * 0.88f, paint);
            canvas.drawText("Назад: системная кнопка / жест назад", w / 2, h * 0.94f, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                current = event.getPointerCount();
                maximum = Math.max(maximum, current);
                for (int i = 0; i < current && i < px.length; i++) {
                    px[i] = event.getX(i);
                    py[i] = event.getY(i);
                }
                message = "Получен жест Android";
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP) {
                current = Math.max(0, event.getPointerCount() - 1);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                current = 0;
                message = maximum >= 2
                        ? "Успешно: две точки пришли вместе"
                        : "Касание завершено";
                invalidate();
                return true;
            }
            return true;
        }
    }
}
