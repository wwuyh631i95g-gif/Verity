package com.hyperfold.axis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * HYPERFOLD — a small 4D maze game.
 *
 * The four W-slices are drawn as offset planes. Their linked corners form
 * a projected hypercube, while a rotating 16-vertex tesseract glyph provides
 * a second view of the same fourth-axis idea.
 */
public final class HyperfoldView extends View {
    private static final int GRID = 9;
    private static final String[] MAPS = {
            "#########",
            "#.......#",
            "#.###.#.#",
            "#...#...#",
            "###.#.###",
            "#...#...#",
            "#.#####.#",
            "#.......#",
            "#########"
    };
    private static final String[] MAP_1 = {
            "#########",
            "#.......#",
            "#.#####.#",
            "#.###.#.#",
            "#.#...#.#",
            "#.###.#.#",
            "#.......#",
            "#.......#",
            "#########"
    };
    private static final String[] MAP_2 = {
            "#########",
            "#.......#",
            "#.#.###.#",
            "#.#...#.#",
            "#...#...#",
            "#.###.#.#",
            "#.#.###.#",
            "#.......#",
            "#########"
    };
    private static final String[] MAP_3 = {
            "#########",
            "#.......#",
            "#.#.#.#.#",
            "#.#...#.#",
            "#...#...#",
            "#.#.#.#.#",
            "#...#...#",
            "#.......#",
            "#########"
    };
    private static final String[][] WORLD = { MAPS, MAP_1, MAP_2, MAP_3 };

    // x, y, W — each echo exists in exactly one fourth-axis slice.
    private static final int[][] SHARDS = {
            {7, 1, 0},
            {3, 7, 1},
            {7, 7, 2},
            {3, 1, 3}
    };
    private static final int EXIT_X = 7;
    private static final int EXIT_Y = 7;
    private static final int EXIT_W = 3;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random random = new Random(4104L);
    private final float[] starX = new float[82];
    private final float[] starY = new float[82];
    private final float[] starSize = new float[82];
    private final float[] starPhase = new float[82];
    private final int touchSlop;

    private int playerX = 1;
    private int playerY = 1;
    private int currentW = 0;
    private final boolean[] collected = new boolean[4];
    private boolean won = false;
    private boolean showHelp = false;
    private String message = "Tap a tile to move. Shift W to inspect another slice.";
    private long messageUntil = 0L;
    private long nextWalkStep = 0L;
    private final ArrayDeque<int[]> walkPath = new ArrayDeque<>();

    private float downX;
    private float downY;
    private long downAt;
    private float screenW;
    private float screenH;
    private float density;
    private float tile;
    private float centerY;

    public HyperfoldView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextFloat();
            starY[i] = random.nextFloat();
            starSize[i] = 0.45f + random.nextFloat() * 1.4f;
            starPhase[i] = random.nextFloat() * 6.28f;
        }
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        screenW = getWidth();
        screenH = getHeight();
        tile = Math.min(screenW * 0.046f, screenH * 0.024f);
        centerY = screenH * 0.465f;

        long now = System.currentTimeMillis();
        advanceWalk(now);
        drawBackground(canvas, now);
        drawHeader(canvas, now);
        drawWorld(canvas, now);
        drawHint(canvas, now);
        drawControls(canvas);

        if (showHelp) drawHelp(canvas);
        if (won) drawWin(canvas);

        postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas c, long now) {
        p.setShader(new LinearGradient(0, 0, screenW, screenH,
                new int[]{Color.rgb(4, 8, 24), Color.rgb(10, 13, 37), Color.rgb(4, 7, 20)},
                null, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, screenW, screenH, p);
        p.setShader(null);

        // A faint central glow makes the projected volume feel suspended in space.
        p.setShader(new RadialGradient(screenW * 0.5f, screenH * 0.46f,
                Math.max(screenW, screenH) * 0.58f,
                new int[]{0x30275D9E, 0x10213A6B, 0x00040A18},
                null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, screenW, screenH, p);
        p.setShader(null);

        for (int i = 0; i < starX.length; i++) {
            float twinkle = 0.55f + 0.45f * (float) Math.sin(now * 0.0012 + starPhase[i]);
            p.setColor(Color.argb((int) (75 + 125 * twinkle), 155, 204, 255));
            c.drawCircle(starX[i] * screenW, starY[i] * screenH,
                    dp(starSize[i] * 0.65f), p);
        }

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(0x1236A9E8);
        c.drawCircle(screenW * 0.5f, screenH * 0.465f, screenW * 0.39f, p);
        p.setColor(0x0C8C70ED);
        c.drawCircle(screenW * 0.5f, screenH * 0.465f, screenW * 0.48f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHeader(Canvas c, long now) {
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(dp(28));
        text.setColor(Color.WHITE);
        c.drawText("HYPERFOLD", screenW * 0.065f, screenH * 0.057f, text);

        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextSize(dp(9));
        text.setColor(0xFF8DA8D7);
        c.drawText("THE FOURTH AXIS", screenW * 0.07f, screenH * 0.082f, text);

        drawHypercubeGlyph(c, screenW * 0.82f, screenH * 0.057f, dp(22), now);

        // Slice and shard status panel.
        RectF panel = new RectF(screenW * 0.055f, screenH * 0.112f,
                screenW * 0.945f, screenH * 0.177f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xB9142542);
        c.drawRoundRect(panel, dp(13), dp(13), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(0x664A8DCD);
        c.drawRoundRect(panel, dp(13), dp(13), p);
        p.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.LEFT);
        text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.setTextSize(dp(9));
        text.setColor(0xFF9AB2D8);
        c.drawText("ECHO SHARDS", panel.left + dp(12), panel.top + dp(17), text);

        float pipX = panel.left + dp(15);
        float pipY = panel.top + dp(39);
        for (int i = 0; i < 4; i++) {
            p.setColor(collected[i] ? 0xFFFFCE72 : 0xFF324765);
            c.drawCircle(pipX + i * dp(21), pipY, dp(5.2f), p);
            if (collected[i]) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(1.5f));
                p.setColor(0xFFFFF0B9);
                c.drawCircle(pipX + i * dp(21), pipY, dp(8), p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        text.setTextAlign(Paint.Align.RIGHT);
        text.setTextSize(dp(10));
        text.setColor(0xFF8FEAFF);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        c.drawText("W  " + (currentW + 1) + " / 4", panel.right - dp(13),
                panel.top + dp(27), text);
        text.setTextSize(dp(8));
        text.setColor(0xFF7C8FB8);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        c.drawText("SLICE", panel.right - dp(13), panel.top + dp(43), text);

        // Tiny help button.
        float hx = screenW * 0.94f;
        float hy = screenH * 0.052f;
        p.setColor(showHelp ? 0xFF5A8DCE : 0x553A5F91);
        c.drawCircle(hx, hy, dp(11), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(0xFF8EBBEA);
        c.drawCircle(hx, hy, dp(11), p);
        p.setStyle(Paint.Style.FILL);
        text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(12));
        text.setColor(Color.WHITE);
        c.drawText("?", hx, hy + dp(4), text);
    }

    /** Project one (x,y,W) cell centre into the 2D screen plane. */
    private float[] project(int x, int y, int w) {
        float ax = tile * 0.86f;
        float ay = tile * 0.44f;
        float cx = screenW * 0.5f + (x - 4) * ax - (y - 4) * ax
                + (w - 1.5f) * tile * 1.24f;
        float cy = centerY + (x + y - 8) * ay
                - (w - 1.5f) * tile * 0.78f;
        return new float[]{cx, cy};
    }

    private void diamond(Canvas c, float cx, float cy, int fill, int stroke, float width) {
        float ax = tile * 0.86f;
        float ay = tile * 0.44f;
        path.reset();
        path.moveTo(cx, cy - ay);
        path.lineTo(cx + ax, cy);
        path.lineTo(cx, cy + ay);
        path.lineTo(cx - ax, cy);
        path.close();

        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawPath(path, p);
        if (stroke != 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
            p.setColor(stroke);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private boolean isWall(int x, int y, int w) {
        if (x < 0 || y < 0 || x >= GRID || y >= GRID || w < 0 || w > 3) return true;
        return WORLD[w][y].charAt(x) == '#';
    }

    private void drawLayer(Canvas c, int w, boolean active, long now) {
        int baseAlpha = active ? 1 : Math.max(12, 34 - Math.abs(w - currentW) * 7);
        int[] sliceColors = {0xFF45D9E8, 0xFFAD8CFF, 0xFFFF83B7, 0xFFFFC56E};
        int color = sliceColors[w];

        for (int y = 1; y <= 7; y++) {
            for (int x = 1; x <= 7; x++) {
                float[] pt = project(x, y, w);
                boolean wall = isWall(x, y, w);
                if (active) {
                    if (wall) {
                        drawWallBlock(c, pt[0], pt[1], color);
                    } else {
                        int fill = 0x26366A8E;
                        int edge = 0x704D9CC2;
                        if ((x + y) % 2 == 0) fill = 0x303C78A0;
                        diamond(c, pt[0], pt[1], fill, edge, dp(0.75f));
                    }
                } else {
                    if (!wall) {
                        int fill = Color.argb(baseAlpha, Color.red(color), Color.green(color), Color.blue(color));
                        int edge = Color.argb(baseAlpha + 8, Color.red(color), Color.green(color), Color.blue(color));
                        diamond(c, pt[0], pt[1], fill, edge, dp(0.7f));
                    } else {
                        diamond(c, pt[0], pt[1], 0x081E2C49,
                                Color.argb(baseAlpha + 4, Color.red(color), Color.green(color), Color.blue(color)),
                                dp(0.6f));
                    }
                }
            }
        }

        // A highlighted outline makes each parallel W-slice readable as a separate plane.
        int a = active ? 0xAA : baseAlpha + 12;
        float[][] corners = {
                project(1, 1, w), project(7, 1, w),
                project(7, 7, w), project(1, 7, w)
        };
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(active ? dp(1.7f) : dp(0.9f));
        p.setColor(Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)));
        for (int i = 0; i < 4; i++) {
            float[] a1 = corners[i];
            float[] a2 = corners[(i + 1) % 4];
            c.drawLine(a1[0], a1[1], a2[0], a2[1], p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawWallBlock(Canvas c, float cx, float cy, int color) {
        float ax = tile * 0.86f;
        float ay = tile * 0.44f;
        float lift = tile * 0.74f;
        float topY = cy - lift;

        // Two shaded sides connect the raised top face to the floor below.
        path.reset();
        path.moveTo(cx, topY + ay);
        path.lineTo(cx + ax, topY);
        path.lineTo(cx + ax, cy);
        path.lineTo(cx, cy + ay);
        path.close();
        p.setColor(0xCC172D4A);
        c.drawPath(path, p);

        path.reset();
        path.moveTo(cx, topY + ay);
        path.lineTo(cx - ax, topY);
        path.lineTo(cx - ax, cy);
        path.lineTo(cx, cy + ay);
        path.close();
        p.setColor(0xCC10213B);
        c.drawPath(path, p);

        path.reset();
        path.moveTo(cx, topY - ay);
        path.lineTo(cx + ax, topY);
        path.lineTo(cx, topY + ay);
        path.lineTo(cx - ax, topY);
        path.close();
        p.setColor(0xFF244765);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(0.9f));
        p.setColor(Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)));
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawWorld(Canvas c, long now) {
        // Draw distant slices first, then the active slice.
        for (int w = 3; w >= 0; w--) {
            if (w != currentW) drawLayer(c, w, false, now);
        }
        drawLayer(c, currentW, true, now);

        // W-axis connectors join matching corners of the four planes.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.15f));
        p.setColor(0x805CA9D8);
        int[] edge = {1, 7};
        for (int x : edge) {
            for (int y : edge) {
                for (int w = 0; w < 3; w++) {
                    float[] a = project(x, y, w);
                    float[] b = project(x, y, w + 1);
                    p.setColor(w == currentW || w + 1 == currentW ? 0xB06EEBFF : 0x405A87B8);
                    c.drawLine(a[0], a[1], b[0], b[1], p);
                }
            }
        }
        p.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 4; i++) {
            if (!collected[i]) drawShard(c, i, now);
        }
        drawExit(c, now);
        drawPlayer(c, now);
    }

    private void drawShard(Canvas c, int index, long now) {
        int[] shard = SHARDS[index];
        float[] pt = project(shard[0], shard[1], shard[2]);
        float pulse = 0.82f + 0.18f * (float) Math.sin(now * 0.004 + index * 1.7);
        float r = tile * 0.31f * pulse;
        float layerAlpha = 1.0f / (1.0f + Math.abs(shard[2] - currentW) * 0.72f);
        int[] colors = {0xFF64F4FF, 0xFFC1A1FF, 0xFFFF9DC8, 0xFFFFD27B};
        int color = colors[index];
        int alpha = (int) (230 * layerAlpha);

        p.setColor(Color.argb(alpha / 5, Color.red(color), Color.green(color), Color.blue(color)));
        c.drawCircle(pt[0], pt[1], r * 3.2f, p);
        p.setColor(Color.argb(alpha / 3, Color.red(color), Color.green(color), Color.blue(color)));
        c.drawCircle(pt[0], pt[1], r * 2.0f, p);

        path.reset();
        path.moveTo(pt[0], pt[1] - r * 1.55f);
        path.lineTo(pt[0] + r, pt[1]);
        path.lineTo(pt[0], pt[1] + r * 1.55f);
        path.lineTo(pt[0] - r, pt[1]);
        path.close();
        p.setShader(new LinearGradient(pt[0] - r, pt[1] - r, pt[0] + r, pt[1] + r,
                Color.argb(alpha, 255, 255, 255), Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP));
        c.drawPath(path, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(Color.argb(alpha, 240, 250, 255));
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawExit(Canvas c, long now) {
        float[] pt = project(EXIT_X, EXIT_Y, EXIT_W);
        boolean unlocked = collectedCount() == 4;
        float pulse = 1.0f + 0.12f * (float) Math.sin(now * 0.003);
        float r = tile * 0.62f * pulse;
        int alpha = currentW == EXIT_W ? 225 : 65;

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2.2f));
        p.setColor(unlocked ? Color.argb(alpha, 255, 211, 121) : Color.argb(alpha / 2, 130, 151, 188));
        c.drawCircle(pt[0], pt[1], r, p);
        p.setStrokeWidth(dp(1));
        p.setColor(unlocked ? Color.argb(alpha, 255, 235, 177) : Color.argb(alpha / 2, 92, 111, 145));
        c.drawCircle(pt[0], pt[1], r * 0.57f, p);
        p.setStyle(Paint.Style.FILL);
        if (currentW == EXIT_W) {
            text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(dp(7));
            text.setColor(unlocked ? 0xFFFFE6A4 : 0xFF8191B3);
            c.drawText(unlocked ? "EXIT" : "LOCKED", pt[0], pt[1] + r + dp(10), text);
        }
    }

    private void drawPlayer(Canvas c, long now) {
        float[] pt = project(playerX, playerY, currentW);
        float bob = (float) Math.sin(now * 0.005) * tile * 0.08f;
        float r = tile * 0.28f;

        p.setColor(0x553BEAFF);
        c.drawCircle(pt[0], pt[1] + bob, r * 3.2f, p);
        p.setColor(0x663BEAFF);
        c.drawCircle(pt[0], pt[1] + bob, r * 2.0f, p);

        p.setShader(new RadialGradient(pt[0] - r * 0.28f, pt[1] - r * 0.34f + bob,
                r * 1.7f, new int[]{Color.WHITE, 0xFF9AF7FF, 0xFF20B8E9, 0xFF12608D},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(pt[0], pt[1] + bob, r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.2f));
        p.setColor(0xFFE1FFFF);
        c.drawCircle(pt[0], pt[1] + bob, r, p);
        p.setStyle(Paint.Style.FILL);

        // A tiny orbit dot makes the player's position in the slice easier to spot.
        p.setColor(0xFFFFE19A);
        c.drawCircle(pt[0] + r * 1.5f, pt[1] - r * 1.2f + bob, dp(2), p);
    }

    private void drawHypercubeGlyph(Canvas c, float cx, float cy, float radius, long now) {
        double a = now * 0.00042;
        double b = now * 0.00031 + 0.65;
        double ca = Math.cos(a), sa = Math.sin(a);
        double cb = Math.cos(b), sb = Math.sin(b);
        float[][] points = new float[16][2];

        for (int i = 0; i < 16; i++) {
            double x = ((i & 1) == 0) ? -1 : 1;
            double y = ((i & 2) == 0) ? -1 : 1;
            double z = ((i & 4) == 0) ? -1 : 1;
            double w = ((i & 8) == 0) ? -1 : 1;

            double nx = x * ca - w * sa;
            double nw = x * sa + w * ca;
            x = nx; w = nw;
            double ny = y * cb - w * sb;
            nw = y * sb + w * cb;
            y = ny; w = nw;

            double f4 = 2.8 / (3.4 - w * 0.48);
            x *= f4; y *= f4; z *= f4;
            double f3 = 3.2 / (3.8 - z * 0.42);
            points[i][0] = cx + (float) ((x + z * 0.18) * radius * 0.68 * f3);
            points[i][1] = cy + (float) ((y - z * 0.14) * radius * 0.68 * f3);
        }

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.05f));
        for (int i = 0; i < 16; i++) {
            for (int j = i + 1; j < 16; j++) {
                int bits = Integer.bitCount(i ^ j);
                if (bits == 1) {
                    boolean wEdge = ((i ^ j) & 8) != 0;
                    p.setColor(wEdge ? 0xFFD49DFF : 0xFF64E9F5);
                    c.drawLine(points[i][0], points[i][1], points[j][0], points[j][1], p);
                }
            }
        }
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 16; i++) {
            p.setColor((i & 8) == 0 ? 0xFF76F1F7 : 0xFFE1B5FF);
            c.drawCircle(points[i][0], points[i][1], dp(1.5f), p);
        }
    }

    private void drawHint(Canvas c, long now) {
        float top = screenH * 0.724f;
        RectF box = new RectF(screenW * 0.055f, top, screenW * 0.945f, top + dp(43));
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xB8172945);
        c.drawRoundRect(box, dp(11), dp(11), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(0x555B9BCD);
        c.drawRoundRect(box, dp(11), dp(11), p);
        p.setStyle(Paint.Style.FILL);

        String line = hintText(now);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(10));
        text.setColor(0xFFD5E7FF);
        c.drawText(line, box.centerX(), box.centerY() + dp(3.5f), text);
    }

    private String hintText(long now) {
        if (messageUntil > now && message != null) return message;
        int count = collectedCount();
        if (count < 4) {
            for (int i = 0; i < 4; i++) {
                if (!collected[i]) {
                    int[] s = SHARDS[i];
                    if (s[2] == currentW) return "This slice contains an echo. Find the glowing crystal.";
                    return "Echo " + count + "/4  •  Shift W to slice " + (s[2] + 1) + " to find the next crystal.";
                }
            }
        }
        return "All echoes aligned. Reach the gold EXIT ring in slice W 4.";
    }

    private void drawControls(Canvas c) {
        float cx = screenW * 0.245f;
        float cy = screenH * 0.88f;
        float gap = dp(48);
        float r = dp(21.5f);

        text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(8));
        text.setColor(0xFF7893B7);
        c.drawText("MOVE IN THE SLICE", screenW * 0.245f, screenH * 0.807f, text);
        c.drawText("FOURTH AXIS", screenW * 0.79f, screenH * 0.807f, text);

        drawButton(c, cx, cy - gap, r, "↗", 0xFF8DDDEB);
        drawButton(c, cx - gap, cy, r, "↖", 0xFF8DDDEB);
        drawButton(c, cx + gap, cy, r, "↘", 0xFF8DDDEB);
        drawButton(c, cx, cy + gap, r, "↙", 0xFF8DDDEB);

        drawButton(c, screenW * 0.70f, cy, dp(25), "W−", 0xFFC9A7FF);
        drawButton(c, screenW * 0.875f, cy, dp(25), "W+", 0xFFFFD48A);

        text.setTextSize(dp(8));
        text.setColor(0xFF657D9F);
        c.drawText("TAP A TILE  •  SWIPE TO STEP", screenW * 0.5f,
                screenH * 0.968f, text);
    }

    private void drawButton(Canvas c, float x, float y, float radius, String label, int tint) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAA172A46);
        c.drawCircle(x, y + dp(2), radius + dp(2), p);
        p.setColor(0xFF203A5A);
        c.drawCircle(x, y, radius, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.15f));
        p.setColor(tint);
        c.drawCircle(x, y, radius, p);
        p.setStyle(Paint.Style.FILL);
        text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(label.length() > 1 ? dp(12) : dp(19));
        text.setColor(Color.WHITE);
        c.drawText(label, x, y + dp(label.length() > 1 ? 4 : 6), text);
    }

    private void drawHelp(Canvas c) {
        p.setColor(0xE9060B1C);
        c.drawRect(0, 0, screenW, screenH, p);
        RectF card = new RectF(screenW * 0.08f, screenH * 0.25f,
                screenW * 0.92f, screenH * 0.72f);
        p.setColor(0xFF142642);
        c.drawRoundRect(card, dp(20), dp(20), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.2f));
        p.setColor(0xFF65DDED);
        c.drawRoundRect(card, dp(20), dp(20), p);
        p.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextSize(dp(23));
        text.setColor(Color.WHITE);
        c.drawText("YOU ARE OUTSIDE THE SHAPE", card.centerX(), card.top + dp(42), text);

        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextSize(dp(12));
        text.setColor(0xFFD0E4FF);
        String[] lines = {
                "This maze has a fourth coordinate: W.",
                "The four tilted planes are different W-slices.",
                "Tap a tile or use the arrows to move.",
                "W− and W+ fold you between slices.",
                "Collect all 4 echo shards, then reach the gold exit.",
                "",
                "The little rotating wireframe is a tesseract:",
                "a 4D cube shown as a 2D projection."
        };
        float y = card.top + dp(91);
        for (String line : lines) {
            if (!line.isEmpty()) c.drawText(line, card.centerX(), y, text);
            y += dp(27);
        }
        text.setTextSize(dp(10));
        text.setColor(0xFF81EAF5);
        c.drawText("TAP ANYWHERE TO RETURN", card.centerX(), card.bottom - dp(25), text);
    }

    private void drawWin(Canvas c) {
        p.setColor(0xD9060B1B);
        c.drawRect(0, 0, screenW, screenH, p);
        RectF card = new RectF(screenW * 0.09f, screenH * 0.31f,
                screenW * 0.91f, screenH * 0.66f);
        p.setColor(0xFF152A49);
        c.drawRoundRect(card, dp(22), dp(22), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.4f));
        p.setColor(0xFFFFD58B);
        c.drawRoundRect(card, dp(22), dp(22), p);
        p.setStyle(Paint.Style.FILL);

        text.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(29));
        text.setColor(0xFFFFE1A3);
        c.drawText("REALITY RECOMPOSED", card.centerX(), card.top + dp(58), text);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setTextSize(dp(13));
        text.setColor(0xFFD7E9FF);
        c.drawText("You found all four echoes across W.", card.centerX(), card.top + dp(93), text);
        c.drawText("The whole shape is visible now.", card.centerX(), card.top + dp(116), text);

        RectF button = new RectF(card.centerX() - dp(76), card.bottom - dp(67),
                card.centerX() + dp(76), card.bottom - dp(25));
        p.setColor(0xFF315D80);
        c.drawRoundRect(button, dp(14), dp(14), p);
        text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.setTextSize(dp(12));
        text.setColor(Color.WHITE);
        c.drawText("PLAY AGAIN", button.centerX(), button.centerY() + dp(4), text);
    }

    private int collectedCount() {
        int count = 0;
        for (boolean value : collected) if (value) count++;
        return count;
    }

    private void setMessage(String value) {
        message = value;
        messageUntil = System.currentTimeMillis() + 2600L;
    }

    private void collectAtPlayer() {
        for (int i = 0; i < SHARDS.length; i++) {
            int[] s = SHARDS[i];
            if (!collected[i] && playerX == s[0] && playerY == s[1] && currentW == s[2]) {
                collected[i] = true;
                setMessage("ECHO STABILIZED  •  " + collectedCount() + " / 4");
            }
        }
        if (currentW == EXIT_W && playerX == EXIT_X && playerY == EXIT_Y) {
            if (collectedCount() == 4) {
                won = true;
                walkPath.clear();
            } else {
                setMessage("EXIT LOCKED  •  " + (4 - collectedCount()) + " ECHOES REMAIN");
            }
        }
    }

    private void move(int dx, int dy) {
        if (won || showHelp) return;
        walkPath.clear();
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (isWall(nx, ny, currentW)) {
            setMessage("SOLID IN THIS SLICE  •  TRY ANOTHER ROUTE");
            return;
        }
        playerX = nx;
        playerY = ny;
        collectAtPlayer();
    }

    private void shiftW(int delta) {
        if (won || showHelp) return;
        walkPath.clear();
        int target = Math.max(0, Math.min(3, currentW + delta));
        if (target == currentW) {
            setMessage("EDGE OF THE FOURTH AXIS");
            return;
        }
        changeSlice(target);
    }

    private void changeSlice(int target) {
        if (target < 0 || target > 3) return;
        currentW = target;
        if (isWall(playerX, playerY, currentW)) {
            int[] nearest = nearestOpen(playerX, playerY, currentW);
            if (nearest != null) {
                playerX = nearest[0];
                playerY = nearest[1];
                setMessage("THE FOLD MOVED YOU TO THE NEAREST OPEN TILE");
            }
        } else {
            setMessage("FOLDED INTO W SLICE " + (currentW + 1));
        }
        collectAtPlayer();
    }

    private int[] nearestOpen(int x, int y, int w) {
        for (int radius = 1; radius <= 5; radius++) {
            for (int yy = 1; yy <= 7; yy++) {
                for (int xx = 1; xx <= 7; xx++) {
                    if (Math.abs(xx - x) + Math.abs(yy - y) == radius && !isWall(xx, yy, w)) {
                        return new int[]{xx, yy};
                    }
                }
            }
        }
        return null;
    }

    private void navigateTo(int targetX, int targetY) {
        if (targetX < 1 || targetX > 7 || targetY < 1 || targetY > 7) return;
        if (isWall(targetX, targetY, currentW)) {
            setMessage("THAT TILE IS SOLID IN W " + (currentW + 1));
            return;
        }
        if (targetX == playerX && targetY == playerY) {
            collectAtPlayer();
            return;
        }

        int[][] parent = new int[GRID * GRID][2];
        for (int[] row : parent) Arrays.fill(row, -1);
        boolean[] seen = new boolean[GRID * GRID];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = playerY * GRID + playerX;
        int goal = targetY * GRID + targetX;
        queue.add(start);
        seen[start] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!queue.isEmpty() && !seen[goal]) {
            int at = queue.removeFirst();
            int x = at % GRID;
            int y = at / GRID;
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (isWall(nx, ny, currentW)) continue;
                int id = ny * GRID + nx;
                if (seen[id]) continue;
                seen[id] = true;
                parent[id][0] = x;
                parent[id][1] = y;
                queue.addLast(id);
            }
        }

        if (!seen[goal]) {
            setMessage("NO OPEN PATH TO THAT TILE");
            return;
        }

        List<int[]> reverse = new ArrayList<>();
        int cx = targetX;
        int cy = targetY;
        while (cx != playerX || cy != playerY) {
            reverse.add(new int[]{cx, cy});
            int id = cy * GRID + cx;
            int px = parent[id][0];
            int py = parent[id][1];
            if (px < 0 || py < 0) break;
            cx = px;
            cy = py;
        }
        walkPath.clear();
        for (int i = reverse.size() - 1; i >= 0; i--) walkPath.addLast(reverse.get(i));
        nextWalkStep = System.currentTimeMillis() + 70L;
    }

    private void advanceWalk(long now) {
        if (walkPath.isEmpty() || won || showHelp || now < nextWalkStep) return;
        int[] next = walkPath.removeFirst();
        playerX = next[0];
        playerY = next[1];
        collectAtPlayer();
        nextWalkStep = now + 105L;
    }

    private void tapWorld(float x, float y) {
        float best = Float.MAX_VALUE;
        int bestX = -1, bestY = -1, bestW = currentW;
        // A tap on one of the visible planes selects that plane as well as the tile.
        for (int w = 0; w < 4; w++) {
            for (int yy = 1; yy <= 7; yy++) {
                for (int xx = 1; xx <= 7; xx++) {
                    float[] pt = project(xx, yy, w);
                    float dx = pt[0] - x;
                    float dy = pt[1] - y;
                    float d = dx * dx + dy * dy;
                    if (d < best) {
                        best = d;
                        bestX = xx;
                        bestY = yy;
                        bestW = w;
                    }
                }
            }
        }
        float limit = tile * tile * 1.35f;
        if (bestX >= 0 && best < limit) {
            if (bestW != currentW) changeSlice(bestW);
            navigateTo(bestX, bestY);
        }
    }

    private void swipeWorld(float dx, float dy) {
        // The two in-plane axes are drawn diagonally, so resolve the swipe against them.
        float bx = tile * 0.86f;
        float by = tile * 0.44f;
        float len = (float) Math.sqrt(bx * bx + by * by);
        float ux = bx / len;
        float uy = by / len;
        float vx = -ux;
        float vy = uy;
        float dotX = dx * ux + dy * uy;
        float dotY = dx * vx + dy * vy;
        if (Math.abs(dotX) > Math.abs(dotY)) {
            move(dotX >= 0 ? 1 : -1, 0);
        } else {
            move(0, dotY >= 0 ? 1 : -1);
        }
    }

    private void restart() {
        playerX = 1;
        playerY = 1;
        currentW = 0;
        Arrays.fill(collected, false);
        walkPath.clear();
        won = false;
        showHelp = false;
        setMessage("THE FOURTH AXIS IS OPEN  •  FIND THE FOUR ECHOES");
    }

    private boolean inCircle(float x, float y, float cx, float cy, float radius) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private boolean handleButton(float x, float y) {
        float cx = screenW * 0.245f;
        float cy = screenH * 0.88f;
        float gap = dp(48);
        float r = dp(27);

        if (inCircle(x, y, cx, cy - gap, r)) { move(0, -1); return true; }  // ↗
        if (inCircle(x, y, cx - gap, cy, r)) { move(-1, 0); return true; }  // ↖
        if (inCircle(x, y, cx + gap, cy, r)) { move(1, 0); return true; }   // ↘
        if (inCircle(x, y, cx, cy + gap, r)) { move(0, 1); return true; }   // ↙
        if (inCircle(x, y, screenW * 0.70f, cy, dp(30))) { shiftW(-1); return true; }
        if (inCircle(x, y, screenW * 0.875f, cy, dp(30))) { shiftW(1); return true; }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                downAt = System.currentTimeMillis();
                return true;

            case MotionEvent.ACTION_UP:
                if (showHelp) {
                    showHelp = false;
                    invalidate();
                    return true;
                }
                if (won) {
                    restart();
                    invalidate();
                    return true;
                }
                if (inCircle(x, y, screenW * 0.94f, screenH * 0.052f, dp(18))) {
                    showHelp = true;
                    walkPath.clear();
                    invalidate();
                    return true;
                }
                if (handleButton(x, y)) {
                    invalidate();
                    return true;
                }
                if (y > screenH * 0.19f && y < screenH * 0.71f) {
                    float dx = x - downX;
                    float dy = y - downY;
                    if (Math.abs(dx) + Math.abs(dy) > Math.max(dp(22), touchSlop * 1.4f)) {
                        swipeWorld(dx, dy);
                    } else {
                        tapWorld(x, y);
                    }
                    invalidate();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return true;
        }
    }

    public boolean handleBack() {
        if (showHelp) {
            showHelp = false;
            invalidate();
            return true;
        }
        if (!won) {
            showHelp = true;
            invalidate();
            return true;
        }
        return false;
    }
}
