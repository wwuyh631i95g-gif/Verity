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
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(new PhysicalTouchView());
    }

    private class PhysicalTouchView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] x = new float[10];
        private final float[] y = new float[10];
        private int active = 0;
        private int maximum = 0;
        private boolean tested = false;
        private String message = "Коснись экрана двумя пальцами одновременно";

        PhysicalTouchView() {
            super(TestActivity.this);
            setBackgroundColor(Color.rgb(15, 25, 43));
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(19);
            canvas.drawText("ФИЗИЧЕСКИЙ MULTITOUCH", w / 2, h * 0.13f, paint);

            paint.setColor(Color.rgb(185, 201, 222));
            paint.setTextSize(14);
            canvas.drawText(message, w / 2, h * 0.23f, paint);

            paint.setColor(maximum >= 2 ? Color.rgb(48, 210, 150) : Color.rgb(255, 190, 60));
            paint.setTextSize(68);
            canvas.drawText(String.valueOf(active), w / 2, h * 0.47f, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(16);
            canvas.drawText("Пальцев сейчас на экране", w / 2, h * 0.53f, paint);
            canvas.drawText("Рекорд за проверку: " + maximum, w / 2, h * 0.61f, paint);

            for (int i = 0; i < active && i < x.length; i++) {
                paint.setColor(i == 0 ? Color.rgb(48, 210, 150) : Color.rgb(255, 190, 60));
                canvas.drawCircle(x[i], y[i], 22, paint);
                paint.setColor(Color.rgb(15, 25, 43));
                paint.setTextSize(14);
                canvas.drawText(String.valueOf(i + 1), x[i], y[i] + 5, paint);
            }

            paint.setColor(Color.rgb(185, 201, 222));
            paint.setTextSize(13);
            if (!tested) {
                canvas.drawText("Удерживай первый палец и добавь второй.", w / 2, h * 0.82f, paint);
                canvas.drawText("Не нажимай пальцы по очереди.", w / 2, h * 0.87f, paint);
            } else if (maximum >= 2) {
                paint.setColor(Color.rgb(48, 210, 150));
                paint.setTextSize(17);
                canvas.drawText("ОТЛИЧНО: часы увидели 2+ пальца", w / 2, h * 0.83f, paint);
                paint.setColor(Color.rgb(185, 201, 222));
                paint.setTextSize(12);
                canvas.drawText("Проверь те же два касания прямо в игре.", w / 2, h * 0.89f, paint);
            } else {
                paint.setColor(Color.rgb(255, 190, 60));
                paint.setTextSize(15);
                canvas.drawText("Пока зарегистрировано только 1 касание", w / 2, h * 0.83f, paint);
                paint.setColor(Color.rgb(185, 201, 222));
                paint.setTextSize(12);
                canvas.drawText("Повтори тест, добавив второй палец, не", w / 2, h * 0.88f, paint);
                canvas.drawText("отрывая первый.", w / 2, h * 0.92f, paint);
            }

            paint.setColor(Color.rgb(130, 150, 177));
            paint.setTextSize(12);
            canvas.drawText("Назад — закрыть проверку и начать заново", w / 2, h * 0.97f, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_MOVE) {
                active = event.getPointerCount();
                maximum = Math.max(maximum, active);
                tested = true;
                for (int i = 0; i < active && i < x.length; i++) {
                    x[i] = event.getX(i);
                    y[i] = event.getY(i);
                }
                message = active >= 2 ? "Android получил несколько физических точек"
                        : "Обнаружен один палец — добавь второй";
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP) {
                active = Math.max(0, event.getPointerCount() - 1);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                active = 0;
                message = maximum >= 2 ? "Проверка завершена: мультитач обнаружен"
                        : "Проверка завершена";
                invalidate();
                return true;
            }
            return true;
        }
    }
}
