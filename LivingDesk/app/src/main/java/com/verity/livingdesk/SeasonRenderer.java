package com.verity.livingdesk;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Random;

public final class SeasonRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] particleX = new float[64];
    private final float[] particleY = new float[64];
    private final float[] particleSpeed = new float[64];
    private final float[] particleSize = new float[64];
    private int season = 0;

    public SeasonRenderer() {
        Random random = new Random(6312026L);
        for (int i = 0; i < particleX.length; i++) {
            particleX[i] = random.nextFloat();
            particleY[i] = random.nextFloat();
            particleSpeed[i] = 0.0007f + random.nextFloat() * 0.0024f;
            particleSize[i] = 1.2f + random.nextFloat() * 3.8f;
        }
    }

    public void setSeason(String id) {
        season = resolveSeason(id);
    }

    public static int resolveSeason(String id) {
        if ("winter".equals(id)) return 0;
        if ("spring".equals(id)) return 1;
        if ("summer".equals(id)) return 2;
        if ("autumn".equals(id)) return 3;
        Month month = ZonedDateTime.now().getMonth();
        int n = month.getValue();
        if (n == 12 || n <= 2) return 0;
        if (n <= 5) return 1;
        if (n <= 8) return 2;
        return 3;
    }

    public void draw(Canvas canvas, int width, int height, long timeMs) {
        if (width <= 0 || height <= 0) return;
        float w = width, h = height;
        int top, bottom, hillA, hillB, ground, light;
        switch (season) {
            case 1:
                top = 0xFF273C69; bottom = 0xFFB88C9E; hillA = 0xFF375E66; hillB = 0xFF64866D; ground = 0xFF426A56; light = 0xFFFFC5D8; break;
            case 2:
                top = 0xFF14335C; bottom = 0xFFF0B56D; hillA = 0xFF275C53; hillB = 0xFF3B8060; ground = 0xFF28664E; light = 0xFFFFE6A3; break;
            case 3:
                top = 0xFF182B4B; bottom = 0xFFCE8059; hillA = 0xFF435046; hillB = 0xFF80543D; ground = 0xFF624532; light = 0xFFFFC27A; break;
            default:
                top = 0xFF0B1732; bottom = 0xFF758AAE; hillA = 0xFF35465D; hillB = 0xFF53677D; ground = 0xFF667E9B; light = 0xFFE0EEFF; break;
        }

        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        float sunX = w * 0.77f;
        float sunY = h * 0.23f;
        paint.setColor(withAlpha(light, 35));
        canvas.drawCircle(sunX, sunY, h * 0.18f, paint);
        paint.setColor(withAlpha(light, 80));
        canvas.drawCircle(sunX, sunY, h * 0.105f, paint);
        paint.setColor(light);
        canvas.drawCircle(sunX, sunY, h * 0.065f, paint);

        // Distant mountain silhouettes.
        path.reset();
        path.moveTo(0, h * 0.62f);
        path.lineTo(w * 0.16f, h * 0.39f);
        path.lineTo(w * 0.30f, h * 0.57f);
        path.lineTo(w * 0.48f, h * 0.34f);
        path.lineTo(w * 0.69f, h * 0.59f);
        path.lineTo(w * 0.87f, h * 0.40f);
        path.lineTo(w, h * 0.57f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(hillA);
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(0, h * 0.72f);
        path.quadTo(w * 0.25f, h * 0.52f, w * 0.50f, h * 0.73f);
        path.quadTo(w * 0.77f, h * 0.51f, w, h * 0.70f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(hillB);
        canvas.drawPath(path, paint);

        paint.setColor(ground);
        canvas.drawRect(0, h * 0.82f, w, h, paint);

        // A little cabin on the right.
        float houseX = w * 0.67f, houseY = h * 0.67f, houseW = w * 0.17f, houseH = h * 0.16f;
        paint.setColor(0xFF25334B);
        canvas.drawRect(houseX, houseY, houseX + houseW, houseY + houseH, paint);
        path.reset();
        path.moveTo(houseX - houseW * 0.10f, houseY + 2);
        path.lineTo(houseX + houseW * 0.50f, houseY - houseH * 0.48f);
        path.lineTo(houseX + houseW * 1.10f, houseY + 2);
        path.close();
        paint.setColor(0xFF18243A);
        canvas.drawPath(path, paint);
        paint.setColor(0xFFFFD98A);
        canvas.drawRect(houseX + houseW * 0.15f, houseY + houseH * 0.24f, houseX + houseW * 0.34f, houseY + houseH * 0.48f, paint);
        canvas.drawRect(houseX + houseW * 0.62f, houseY + houseH * 0.24f, houseX + houseW * 0.81f, houseY + houseH * 0.48f, paint);
        paint.setColor(0xFF111C30);
        canvas.drawRect(houseX + houseW * 0.42f, houseY + houseH * 0.55f, houseX + houseW * 0.58f, houseY + houseH, paint);

        // Two stylized trees frame the scene.
        drawTree(canvas, w * 0.16f, h * 0.85f, h * 0.36f, season);
        drawTree(canvas, w * 0.90f, h * 0.87f, h * 0.28f, season);

        // Animated seasonal particles.
        for (int i = 0; i < particleX.length; i++) {
            particleY[i] += particleSpeed[i] * (season == 2 ? 0.65f : 1f);
            if (particleY[i] > 1.08f) {
                particleY[i] = -0.06f;
                particleX[i] = (particleX[i] + 0.173f) % 1f;
            }
            float x = particleX[i] * w;
            float y = particleY[i] * h;
            float s = particleSize[i];
            float sway = (float) Math.sin(timeMs * 0.0014 + i * 1.7) * (season == 0 ? 2f : 6f);
            if (season == 0) {
                paint.setColor(0xDDEFFFFF);
                canvas.drawCircle(x + sway, y, s, paint);
            } else if (season == 1) {
                paint.setColor(i % 2 == 0 ? 0xDDF8B8D5 : 0xDDF8E0A7);
                canvas.save();
                canvas.rotate((float) (timeMs * 0.025 + i * 31), x + sway, y);
                canvas.drawOval(x + sway - s, y - s * 0.55f, x + sway + s, y + s * 0.55f, paint);
                canvas.restore();
            } else if (season == 2) {
                paint.setColor(i % 3 == 0 ? 0xFFFFE59A : 0x99D7F5A5);
                canvas.drawCircle(x + sway, y, s * 0.75f, paint);
            } else {
                paint.setColor(i % 2 == 0 ? 0xFFE9A35F : 0xFFD66E43);
                canvas.save();
                canvas.rotate((float) (timeMs * 0.035 + i * 39), x + sway, y);
                canvas.drawOval(x + sway - s * 1.15f, y - s * 0.55f, x + sway + s * 1.15f, y + s * 0.55f, paint);
                canvas.restore();
            }
        }

        // Soft horizon glow and a subtle vignette.
        paint.setShader(new LinearGradient(0, h * 0.58f, 0, h, withAlpha(light, 0), withAlpha(light, 30), Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.58f, w, h, paint);
        paint.setShader(null);
    }

    private void drawTree(Canvas canvas, float x, float baseY, float size, int seasonIndex) {
        paint.setColor(0xFF273448);
        canvas.drawRoundRect(x - size * 0.045f, baseY - size * 0.62f, x + size * 0.045f, baseY, size * 0.03f, size * 0.03f, paint);
        if (seasonIndex == 0) {
            paint.setColor(0xFFB7C7D8);
            for (int k = 0; k < 3; k++) {
                float yy = baseY - size * (0.32f + k * 0.16f);
                canvas.drawLine(x, yy, x - size * (0.19f - k * 0.025f), yy - size * 0.13f, paint);
                canvas.drawLine(x, yy, x + size * (0.19f - k * 0.025f), yy - size * 0.13f, paint);
            }
        } else {
            int leaf = seasonIndex == 1 ? 0xFF76B58E : seasonIndex == 2 ? 0xFF4A9A66 : 0xFFC17A43;
            paint.setColor(leaf);
            for (int k = 0; k < 5; k++) {
                float ox = (k - 2) * size * 0.085f;
                float oy = (k % 2) * size * 0.06f;
                canvas.drawCircle(x + ox, baseY - size * 0.62f + oy, size * (0.13f + (k % 2) * 0.02f), paint);
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
