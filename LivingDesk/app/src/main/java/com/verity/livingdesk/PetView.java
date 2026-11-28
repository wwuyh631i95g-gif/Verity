package com.verity.livingdesk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public final class PetView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int petType = 0;
    private int mood = 0;
    private long started = System.currentTimeMillis();

    public PetView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setOnClickListener(v -> {
            mood = (mood + 1) % 4;
            invalidate();
        });
    }

    public void setPetType(int type) {
        petType = Math.max(0, type) % 3;
        invalidate();
    }

    public void cycleMood() {
        mood = (mood + 1) % 4;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float scale = Math.min(w, h) / 110f;
        canvas.save();
        canvas.translate((w - 110f * scale) / 2f, (h - 110f * scale) / 2f);
        canvas.scale(scale, scale);

        long t = System.currentTimeMillis() - started;
        float bob = (float) Math.sin(t * 0.004) * 2.4f;
        float tail = (float) Math.sin(t * 0.006) * 9f;
        canvas.translate(0, bob);

        // Soft floor shadow.
        p.setColor(0x44313B5B);
        canvas.drawOval(20, 92, 91, 105, p);

        int body = petType == 1 ? 0xFFFFB46A : petType == 2 ? 0xFFE7E5F8 : 0xFF8DAAFF;
        int ear = petType == 1 ? 0xFFE88753 : petType == 2 ? 0xFFC9C6EA : 0xFF617BCC;
        int innerEar = petType == 1 ? 0xFFFFD1AA : petType == 2 ? 0xFFF4AFCB : 0xFFFFB9D2;

        // Animated tail.
        p.setColor(body);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(12);
        p.setStrokeCap(Paint.Cap.ROUND);
        path.reset();
        path.moveTo(77, 75);
        path.cubicTo(105, 78 + tail * 0.25f, 104 + tail, 42, 84, 48 + tail * 0.32f);
        canvas.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);

        // Ears.
        if (petType == 2) {
            path.reset();
            path.moveTo(37, 39); path.quadTo(23, -8, 39, 3); path.quadTo(49, 18, 49, 39); path.close();
            p.setColor(ear); canvas.drawPath(path, p);
            path.reset();
            path.moveTo(64, 39); path.quadTo(61, 8, 77, 3); path.quadTo(92, -2, 75, 39); path.close();
            canvas.drawPath(path, p);
            p.setColor(innerEar);
            canvas.drawOval(35, 10, 43, 31, p);
            canvas.drawOval(69, 10, 78, 31, p);
        } else {
            path.reset();
            path.moveTo(30, 48); path.lineTo(30, 10); path.quadTo(34, 3, 42, 15); path.lineTo(51, 42); path.close();
            p.setColor(ear); canvas.drawPath(path, p);
            path.reset();
            path.moveTo(60, 42); path.lineTo(70, 14); path.quadTo(81, 3, 83, 12); path.lineTo(80, 48); path.close();
            canvas.drawPath(path, p);
            p.setColor(innerEar);
            path.reset();
            path.moveTo(35, 34); path.lineTo(35, 18); path.lineTo(43, 37); path.close();
            canvas.drawPath(path, p);
            path.reset();
            path.moveTo(69, 37); path.lineTo(78, 18); path.lineTo(77, 35); path.close();
            canvas.drawPath(path, p);
        }

        // Body and head.
        p.setColor(body);
        canvas.drawOval(23, 54, 91, 96, p);
        canvas.drawCircle(56, 49, 35, p);
        p.setColor(0x22FFFFFF);
        canvas.drawOval(32, 63, 70, 84, p);

        // Cheeks and muzzle.
        p.setColor(0x66FF91B7);
        canvas.drawCircle(35, 58, 6, p);
        canvas.drawCircle(77, 58, 6, p);
        p.setColor(0xFFFFF3F4);
        canvas.drawOval(43, 55, 68, 72, p);

        // Blinking eyes.
        boolean blink = (t % 4100L) > 3900L;
        p.setColor(0xFF24304C);
        if (blink) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.6f);
            canvas.drawLine(41, 46, 49, 46, p);
            canvas.drawLine(64, 46, 72, 46, p);
            p.setStyle(Paint.Style.FILL);
        } else {
            canvas.drawCircle(46, 46, 4.2f, p);
            canvas.drawCircle(67, 46, 4.2f, p);
            p.setColor(Color.WHITE);
            canvas.drawCircle(47.2f, 44.8f, 1.4f, p);
            canvas.drawCircle(68.2f, 44.8f, 1.4f, p);
        }

        // Nose and changing smile.
        p.setColor(0xFFE7839F);
        canvas.drawCircle(56.5f, 57, 2.5f, p);
        p.setColor(0xFF5C4057);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.7f);
        path.reset();
        path.moveTo(56, 60);
        if (mood == 1) path.quadTo(56, 68, 64, 64);
        else if (mood == 2) path.quadTo(56, 67, 48, 64);
        else path.quadTo(56, 65, 56, 62);
        canvas.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);

        // Small collar and charm.
        p.setColor(0xFFFFD36E);
        canvas.drawRoundRect(42, 79, 70, 84, 3, 3, p);
        canvas.drawCircle(56, 85, 4, p);
        p.setColor(0xFF35436B);
        canvas.drawCircle(56, 85, 1.8f, p);

        // Mood sparkles on taps.
        if (mood == 1 || mood == 3) {
            p.setColor(0xFFFFE38C);
            canvas.drawCircle(18, 34, 3, p);
            canvas.drawCircle(94, 31, 2.5f, p);
        }
        canvas.restore();
        postInvalidateDelayed(38L);
    }
}
