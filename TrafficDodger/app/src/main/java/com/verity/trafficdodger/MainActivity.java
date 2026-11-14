package com.verity.trafficdodger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();
        gameView = new GameView(this);
        setContentView(gameView);
    }

    private void hideSystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    private static final class TrafficCar {
        int lane;
        int color;
        float y;
        boolean truck;

        TrafficCar(int lane, int color, float y, boolean truck) {
            this.lane = lane;
            this.color = color;
            this.y = y;
            this.truck = truck;
        }
    }

    private final class GameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<TrafficCar> traffic = new ArrayList<>();
        private final float[] textureX = new float[54];
        private final float[] textureY = new float[54];
        private final int[] trafficColors = {
                Color.rgb(239, 241, 237),
                Color.rgb(36, 48, 61),
                Color.rgb(48, 104, 157),
                Color.rgb(191, 45, 54),
                Color.rgb(220, 174, 54),
                Color.rgb(107, 111, 119),
                Color.rgb(235, 235, 225)
        };

        private float screenW;
        private float screenH;
        private float playerX;
        private final float playerYFactor = 0.735f;
        private int playerLane = 1;
        private float elapsed;
        private float score;
        private float roadScroll;
        private float spawnTimer;
        private float controlTimer;
        private float swipeStartX;
        private float swipeStartY;
        private boolean leftPressed;
        private boolean rightPressed;
        private boolean swiped;
        private boolean gameOver;
        private long lastFrameNanos;
        private int bestScore;
        private RectF restartBounds = new RectF();

        private final int playerColor = Color.rgb(199, 35, 48);

        GameView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            setFocusable(true);
            SharedPreferences prefs = getSharedPreferences("traffic_game", MODE_PRIVATE);
            bestScore = prefs.getInt("best", 0);
            for (int i = 0; i < textureX.length; i++) {
                textureX[i] = random.nextFloat();
                textureY[i] = random.nextFloat();
            }
            textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            lastFrameNanos = System.nanoTime();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            playerX = laneCenter(playerLane, screenH * playerYFactor);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (screenW <= 0 || screenH <= 0) {
                screenW = getWidth();
                screenH = getHeight();
            }

            long now = System.nanoTime();
            float dt = Math.min(0.05f, Math.max(0f, (now - lastFrameNanos) / 1_000_000_000f));
            lastFrameNanos = now;

            if (!gameOver) updateGame(dt);

            drawWorld(canvas, dt);
            if (gameOver) drawGameOver(canvas);
            else drawControls(canvas);

            postInvalidateDelayed(16);
        }

        private void updateGame(float dt) {
            elapsed += dt;
            score = elapsed * 12.0f;
            roadScroll += dt * screenH * (0.45f + Math.min(0.42f, elapsed * 0.006f));

            float targetX = laneCenter(playerLane, screenH * playerYFactor);
            playerX += (targetX - playerX) * Math.min(1f, dt * 13f);

            if (leftPressed || rightPressed) {
                controlTimer += dt;
                if (controlTimer >= 0.24f) {
                    controlTimer = 0f;
                    changeLane(leftPressed ? -1 : 1);
                }
            }

            float speedLevel = 1f + Math.min(1.25f, elapsed / 40f);
            spawnTimer += dt;
            float spawnInterval = Math.max(0.48f, 1.02f - elapsed * 0.008f);
            if (spawnTimer >= spawnInterval) {
                spawnTimer = 0f;
                spawnCar();
            }

            float playerW = roadWidth(screenH * playerYFactor) * 0.56f / 3f;
            float playerH = playerW * 1.62f;
            float playerTop = screenH * playerYFactor - playerH * 0.5f;
            float playerBottom = screenH * playerYFactor + playerH * 0.5f;

            Iterator<TrafficCar> iterator = traffic.iterator();
            while (iterator.hasNext()) {
                TrafficCar car = iterator.next();
                float depth = Math.max(0f, Math.min(1.3f, car.y / screenH));
                car.y += dt * screenH * (0.31f + 0.20f * depth) * speedLevel;

                float carW = roadWidth(car.y) * 0.56f / 3f;
                float carH = carW * (car.truck ? 1.52f : 1.62f);
                float carX = laneCenter(car.lane, car.y);
                float carLeft = carX - carW * 0.43f;
                float carRight = carX + carW * 0.43f;
                float carTop = car.y - carH * 0.46f;
                float carBottom = car.y + carH * 0.46f;

                boolean overlapX = Math.abs(carX - playerX) < (carW + playerW) * 0.39f;
                boolean overlapY = carBottom > playerTop + playerH * 0.12f
                        && carTop < playerBottom - playerH * 0.12f;
                if (overlapX && overlapY) {
                    finishGame();
                    break;
                }

                if (car.y > screenH + carH) iterator.remove();
            }
        }

        private void spawnCar() {
            int lane = random.nextInt(3);
            for (int attempt = 0; attempt < 8; attempt++) {
                int candidate = random.nextInt(3);
                boolean blocked = false;
                for (TrafficCar car : traffic) {
                    if (car.lane == candidate && car.y < screenH * 0.22f) {
                        blocked = true;
                        break;
                    }
                }
                lane = candidate;
                if (!blocked) break;
            }

            int color = trafficColors[random.nextInt(trafficColors.length)];
            boolean truck = elapsed > 8f && random.nextFloat() < 0.14f;
            traffic.add(new TrafficCar(lane, color, -screenH * 0.10f, truck));
        }

        private void finishGame() {
            gameOver = true;
            int finalScore = (int) score;
            if (finalScore > bestScore) {
                bestScore = finalScore;
                getSharedPreferences("traffic_game", MODE_PRIVATE)
                        .edit().putInt("best", bestScore).apply();
            }
            leftPressed = false;
            rightPressed = false;
        }

        private void restartGame() {
            traffic.clear();
            elapsed = 0f;
            score = 0f;
            roadScroll = 0f;
            spawnTimer = 0.55f;
            controlTimer = 0f;
            playerLane = 1;
            playerX = laneCenter(playerLane, screenH * playerYFactor);
            leftPressed = false;
            rightPressed = false;
            gameOver = false;
            lastFrameNanos = System.nanoTime();
            invalidate();
        }

        private void changeLane(int direction) {
            playerLane = Math.max(0, Math.min(2, playerLane + direction));
        }

        private float roadWidth(float y) {
            float t = Math.max(0f, Math.min(1f, y / Math.max(1f, screenH)));
            return screenW * (0.47f + 0.43f * t);
        }

        private float laneCenter(int lane, float y) {
            return screenW * 0.5f + (lane - 1) * roadWidth(y) / 3f;
        }

        private void drawWorld(Canvas canvas, float dt) {
            drawBackground(canvas);
            drawRoad(canvas);
            drawTraffic(canvas);

            float py = screenH * playerYFactor;
            float pw = roadWidth(py) * 0.56f / 3f;
            drawCar(canvas, playerX, py, pw, playerColor, false, true);

            drawHud(canvas);
        }

        private void drawBackground(Canvas canvas) {
            paint.setShader(new LinearGradient(0, 0, 0, screenH,
                    Color.rgb(82, 111, 91), Color.rgb(138, 133, 103), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, screenW, screenH, paint);
            paint.setShader(null);

            // Pale verges and roadside ground, echoing the real road in the reference clip.
            Path leftGround = new Path();
            leftGround.moveTo(0, 0);
            leftGround.lineTo(screenW * 0.265f, 0);
            leftGround.lineTo(screenW * 0.055f, screenH);
            leftGround.lineTo(0, screenH);
            leftGround.close();
            paint.setColor(Color.rgb(150, 153, 140));
            canvas.drawPath(leftGround, paint);

            Path rightGround = new Path();
            rightGround.moveTo(screenW * 0.735f, 0);
            rightGround.lineTo(screenW, 0);
            rightGround.lineTo(screenW, screenH);
            rightGround.lineTo(screenW * 0.945f, screenH);
            rightGround.close();
            paint.setColor(Color.rgb(164, 148, 112));
            canvas.drawPath(rightGround, paint);

            for (int i = 0; i < textureX.length; i++) {
                float y = textureY[i] * screenH;
                float xLeft = textureX[i] * screenW * 0.20f;
                float xRight = screenW * 0.80f + textureX[i] * screenW * 0.18f;
                paint.setColor(i % 3 == 0 ? Color.argb(65, 75, 91, 67) : Color.argb(50, 222, 211, 177));
                canvas.drawOval(xLeft, y, xLeft + screenW * 0.018f, y + screenH * 0.008f, paint);
                canvas.drawOval(xRight, y, xRight + screenW * 0.012f, y + screenH * 0.006f, paint);
            }
        }

        private void drawRoad(Canvas canvas) {
            float topLeft = screenW * 0.265f;
            float topRight = screenW * 0.735f;
            float bottomLeft = screenW * 0.055f;
            float bottomRight = screenW * 0.945f;

            Path road = new Path();
            road.moveTo(topLeft, 0);
            road.lineTo(topRight, 0);
            road.lineTo(bottomRight, screenH);
            road.lineTo(bottomLeft, screenH);
            road.close();

            paint.setShader(new LinearGradient(0, 0, 0, screenH,
                    Color.rgb(83, 89, 94), Color.rgb(43, 48, 53), Shader.TileMode.CLAMP));
            canvas.drawPath(road, paint);
            paint.setShader(null);

            // Roadside borders.
            paint.setColor(Color.rgb(220, 219, 204));
            paint.setStrokeWidth(Math.max(2f, screenW * 0.008f));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(topLeft, 0, bottomLeft, screenH, paint);
            canvas.drawLine(topRight, 0, bottomRight, screenH, paint);
            paint.setStyle(Paint.Style.FILL);

            // Subtle asphalt flecks.
            for (int i = 0; i < textureX.length; i++) {
                float y = textureY[i] * screenH;
                float x = screenW * 0.5f + (textureX[i] - 0.5f) * roadWidth(y) * 0.88f;
                paint.setColor(Color.argb(28, 230, 230, 230));
                float r = screenW * (0.0015f + 0.0015f * y / screenH);
                canvas.drawCircle(x, y, r, paint);
            }

            // Moving dashed lane markings create the endless forward motion.
            float dash = screenH * 0.060f;
            float gap = screenH * 0.055f;
            float period = dash + gap;
            float offset = roadScroll % period;
            for (int divider = 1; divider <= 2; divider++) {
                for (float y = -dash + offset; y < screenH; y += period) {
                    float center = screenW * 0.5f + (divider - 1.5f) * roadWidth(y) / 3f;
                    float localW = roadWidth(y);
                    paint.setColor(Color.argb(205, 236, 237, 224));
                    float lineW = Math.max(2f, localW * 0.012f);
                    canvas.drawRoundRect(center - lineW * 0.5f, y,
                            center + lineW * 0.5f, y + dash * (0.70f + 0.30f * Math.max(0f, y / screenH)),
                            lineW, lineW, paint);
                }
            }

            // A little bridge/overpass shadow crossing the road, moving out of view.
            float shadowY = (screenH * 0.50f + roadScroll * 0.40f) % (screenH * 1.20f);
            if (shadowY > screenH * 0.16f && shadowY < screenH * 0.88f) {
                float alpha = 26f;
                paint.setColor(Color.argb((int) alpha, 8, 11, 16));
                canvas.drawRect(screenW * 0.5f - roadWidth(shadowY) * 0.5f,
                        shadowY, screenW * 0.5f + roadWidth(shadowY) * 0.5f,
                        shadowY + screenH * 0.025f, paint);
                paint.setColor(Color.argb(48, 14, 18, 22));
                canvas.drawRect(screenW * 0.5f - roadWidth(shadowY) * 0.5f,
                        shadowY + screenH * 0.025f, screenW * 0.5f + roadWidth(shadowY) * 0.5f,
                        shadowY + screenH * 0.045f, paint);
            }
        }

        private void drawTraffic(Canvas canvas) {
            for (TrafficCar car : traffic) {
                float carW = roadWidth(car.y) * 0.56f / 3f;
                drawCar(canvas, laneCenter(car.lane, car.y), car.y, carW, car.color, car.truck, false);
            }
        }

        private void drawCar(Canvas canvas, float cx, float cy, float carW, int color,
                             boolean truck, boolean player) {
            float carH = carW * (truck ? 1.52f : 1.62f);
            float left = cx - carW * 0.47f;
            float right = cx + carW * 0.47f;
            float top = cy - carH * 0.50f;
            float bottom = cy + carH * 0.50f;
            float radius = carW * 0.17f;

            // Soft ground shadow.
            paint.setColor(Color.argb(85, 0, 0, 0));
            canvas.drawRoundRect(left + carW * 0.06f, top + carH * 0.025f,
                    right + carW * 0.07f, bottom + carH * 0.04f,
                    radius, radius, paint);

            // Wheels.
            paint.setColor(Color.rgb(19, 22, 25));
            float wheelW = carW * 0.13f;
            float wheelH = carH * 0.22f;
            canvas.drawRoundRect(left - wheelW * 0.12f, top + carH * 0.22f,
                    left + wheelW * 0.82f, top + carH * 0.22f + wheelH, wheelW * 0.25f, wheelW * 0.25f, paint);
            canvas.drawRoundRect(right - wheelW * 0.82f, top + carH * 0.22f,
                    right + wheelW * 0.12f, top + carH * 0.22f + wheelH, wheelW * 0.25f, wheelW * 0.25f, paint);
            canvas.drawRoundRect(left - wheelW * 0.12f, bottom - carH * 0.38f,
                    left + wheelW * 0.82f, bottom - carH * 0.16f, wheelW * 0.25f, wheelW * 0.25f, paint);
            canvas.drawRoundRect(right - wheelW * 0.82f, bottom - carH * 0.38f,
                    right + wheelW * 0.12f, bottom - carH * 0.16f, wheelW * 0.25f, wheelW * 0.25f, paint);

            // Main car body with a slight top-to-bottom highlight.
            RectF body = new RectF(left, top, right, bottom);
            paint.setShader(new LinearGradient(left, top, right, bottom,
                    lighten(color, 24), color, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(body, radius, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.2f, carW * 0.025f));
            paint.setColor(Color.argb(180, 15, 20, 24));
            canvas.drawRoundRect(body, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);

            if (truck) {
                paint.setColor(Color.argb(235, 38, 58, 72));
                canvas.drawRoundRect(left + carW * 0.13f, top + carH * 0.10f,
                        right - carW * 0.13f, top + carH * 0.43f,
                        carW * 0.07f, carW * 0.07f, paint);
                paint.setColor(Color.argb(75, 255, 255, 255));
                canvas.drawRect(left + carW * 0.15f, top + carH * 0.48f,
                        right - carW * 0.15f, bottom - carH * 0.12f, paint);
            } else {
                // Front windscreen.
                paint.setShader(new LinearGradient(0, top + carH * 0.20f, 0, top + carH * 0.49f,
                        Color.rgb(33, 54, 69), Color.rgb(91, 124, 143), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(left + carW * 0.17f, top + carH * 0.19f,
                        right - carW * 0.17f, top + carH * 0.48f,
                        carW * 0.12f, carW * 0.12f, paint);
                paint.setShader(null);

                // Rear window.
                paint.setColor(Color.rgb(43, 61, 74));
                canvas.drawRoundRect(left + carW * 0.20f, top + carH * 0.62f,
                        right - carW * 0.20f, top + carH * 0.82f,
                        carW * 0.09f, carW * 0.09f, paint);
                paint.setColor(Color.argb(55, 255, 255, 255));
                canvas.drawLine(cx, top + carH * 0.20f, cx, top + carH * 0.80f, paint);
            }

            // Mirrors and lamps.
            paint.setColor(Color.rgb(27, 34, 40));
            canvas.drawRoundRect(left - carW * 0.035f, top + carH * 0.31f,
                    left + carW * 0.12f, top + carH * 0.41f, carW * 0.035f, carW * 0.035f, paint);
            canvas.drawRoundRect(right - carW * 0.12f, top + carH * 0.31f,
                    right + carW * 0.035f, top + carH * 0.41f, carW * 0.035f, carW * 0.035f, paint);
            paint.setColor(Color.rgb(255, 241, 194));
            canvas.drawRoundRect(left + carW * 0.08f, top + carH * 0.035f,
                    left + carW * 0.29f, top + carH * 0.095f, carW * 0.025f, carW * 0.025f, paint);
            canvas.drawRoundRect(right - carW * 0.29f, top + carH * 0.035f,
                    right - carW * 0.08f, top + carH * 0.095f, carW * 0.025f, carW * 0.025f, paint);
            paint.setColor(Color.rgb(248, 70, 65));
            canvas.drawRoundRect(left + carW * 0.10f, bottom - carH * 0.085f,
                    left + carW * 0.29f, bottom - carH * 0.035f, carW * 0.025f, carW * 0.025f, paint);
            canvas.drawRoundRect(right - carW * 0.29f, bottom - carH * 0.085f,
                    right - carW * 0.10f, bottom - carH * 0.035f, carW * 0.025f, carW * 0.025f, paint);

            if (player) {
                paint.setColor(Color.argb(175, 255, 255, 255));
                canvas.drawRoundRect(cx - carW * 0.025f, top + carH * 0.54f,
                        cx + carW * 0.025f, top + carH * 0.58f, carW * 0.02f, carW * 0.02f, paint);
            }
        }

        private int lighten(int color, int amount) {
            int r = Math.min(255, Color.red(color) + amount);
            int g = Math.min(255, Color.green(color) + amount);
            int b = Math.min(255, Color.blue(color) + amount);
            return Color.rgb(r, g, b);
        }

        private void drawHud(Canvas canvas) {
            float unit = screenW / 420f;
            float margin = screenW * 0.045f;
            float top = screenH * 0.035f;

            paint.setColor(Color.argb(190, 18, 25, 31));
            canvas.drawRoundRect(margin, top, margin + screenW * 0.39f,
                    top + screenH * 0.105f, 18 * unit, 18 * unit, paint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(11 * unit);
            textPaint.setColor(Color.rgb(190, 207, 215));
            canvas.drawText("ПРОЕХАЛ", margin + 13 * unit, top + 23 * unit, textPaint);
            textPaint.setTextSize(25 * unit);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(((int) score) + " м", margin + 13 * unit, top + 52 * unit, textPaint);

            float bestW = screenW * 0.31f;
            float bx = screenW - margin - bestW;
            paint.setColor(Color.argb(175, 18, 25, 31));
            canvas.drawRoundRect(bx, top, bx + bestW, top + screenH * 0.072f,
                    16 * unit, 16 * unit, paint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(10 * unit);
            textPaint.setColor(Color.rgb(190, 207, 215));
            canvas.drawText("РЕКОРД", bx + bestW * 0.5f, top + 18 * unit, textPaint);
            textPaint.setTextSize(17 * unit);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(Math.max(bestScore, (int) score)),
                    bx + bestW * 0.5f, top + 40 * unit, textPaint);
        }

        private void drawControls(Canvas canvas) {
            float y = screenH * 0.91f;
            float size = Math.min(screenW * 0.145f, screenH * 0.075f);
            drawControl(canvas, screenW * 0.22f, y, size, true, leftPressed);
            drawControl(canvas, screenW * 0.78f, y, size, false, rightPressed);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(screenW * 0.027f);
            textPaint.setColor(Color.argb(220, 255, 255, 255));
            canvas.drawText("ДВИГАЙСЯ МЕЖДУ МАШИНАМИ", screenW * 0.5f,
                    screenH * 0.835f, textPaint);
        }

        private void drawControl(Canvas canvas, float cx, float cy, float size,
                                 boolean left, boolean pressed) {
            RectF b = new RectF(cx - size, cy - size, cx + size, cy + size);
            paint.setColor(pressed ? Color.rgb(194, 43, 54) : Color.argb(205, 18, 27, 35));
            canvas.drawRoundRect(b, size * 0.40f, size * 0.40f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.055f);
            paint.setColor(Color.argb(220, 255, 255, 255));
            canvas.drawRoundRect(b, size * 0.40f, size * 0.40f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(size * 0.13f);
            Path arrow = new Path();
            if (left) {
                arrow.moveTo(cx + size * 0.22f, cy - size * 0.40f);
                arrow.lineTo(cx - size * 0.20f, cy);
                arrow.lineTo(cx + size * 0.22f, cy + size * 0.40f);
            } else {
                arrow.moveTo(cx - size * 0.22f, cy - size * 0.40f);
                arrow.lineTo(cx + size * 0.20f, cy);
                arrow.lineTo(cx - size * 0.22f, cy + size * 0.40f);
            }
            canvas.drawPath(arrow, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawGameOver(Canvas canvas) {
            paint.setColor(Color.argb(175, 5, 9, 13));
            canvas.drawRect(0, 0, screenW, screenH, paint);

            float unit = screenW / 420f;
            float panelW = screenW * 0.82f;
            float panelH = screenH * 0.32f;
            float left = (screenW - panelW) * 0.5f;
            float top = screenH * 0.34f;
            paint.setColor(Color.rgb(24, 32, 40));
            canvas.drawRoundRect(left, top, left + panelW, top + panelH,
                    24 * unit, 24 * unit, paint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.rgb(255, 102, 102));
            textPaint.setTextSize(19 * unit);
            canvas.drawText("СТОЛКНОВЕНИЕ!", screenW * 0.5f, top + 47 * unit, textPaint);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(38 * unit);
            canvas.drawText(((int) score) + " м", screenW * 0.5f, top + 96 * unit, textPaint);

            textPaint.setColor(Color.rgb(184, 198, 207));
            textPaint.setTextSize(13 * unit);
            canvas.drawText("РЕКОРД: " + bestScore, screenW * 0.5f, top + 124 * unit, textPaint);

            float buttonW = panelW * 0.70f;
            float buttonH = screenH * 0.070f;
            float buttonTop = top + panelH - buttonH - 22 * unit;
            restartBounds.set(screenW * 0.5f - buttonW * 0.5f, buttonTop,
                    screenW * 0.5f + buttonW * 0.5f, buttonTop + buttonH);
            paint.setColor(Color.rgb(211, 47, 58));
            canvas.drawRoundRect(restartBounds, 16 * unit, 16 * unit, paint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(17 * unit);
            canvas.drawText("ЗАНОВО", screenW * 0.5f, buttonTop + buttonH * 0.66f, textPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (gameOver) {
                        if (restartBounds.contains(x, y)) restartGame();
                        return true;
                    }
                    swipeStartX = x;
                    swipeStartY = y;
                    swiped = false;
                    if (y >= screenH * 0.82f) {
                        if (x < screenW * 0.5f) {
                            leftPressed = true;
                            rightPressed = false;
                            changeLane(-1);
                        } else {
                            rightPressed = true;
                            leftPressed = false;
                            changeLane(1);
                        }
                        controlTimer = 0f;
                    }
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!gameOver && !swiped && swipeStartY < screenH * 0.80f
                            && Math.abs(x - swipeStartX) > screenW * 0.11f) {
                        changeLane(x < swipeStartX ? -1 : 1);
                        swiped = true;
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    leftPressed = false;
                    rightPressed = false;
                    invalidate();
                    return true;
            }
            return true;
        }
    }
}
