package com.verity.islandbuddy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object FaceRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        style: Int,
        eyeColor: Int,
        animationsEnabled: Boolean,
        animationMode: Int,
        accessory: Int
    ) {
        val now = SystemClock.uptimeMillis()
        val r = h * 0.5f

        p.style = Paint.Style.FILL
        p.color = Color.rgb(2, 4, 8)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), r, r, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, h * 0.025f)
        p.color = Color.argb(210, Color.red(eyeColor), Color.green(eyeColor), Color.blue(eyeColor))
        canvas.drawRoundRect(RectF(x + 1f, y + 1f, x + w - 1f, y + h - 1f), r, r, p)
        p.style = Paint.Style.FILL

        if (style == 1) drawCatEars(canvas, x, y, w, h)
        if (style == 2) drawAlienBumps(canvas, x, y, w, h)

        val cycle = (now % 5000L).toFloat() / 5000f
        var blink = 1f
        var lookX = 0f
        var lookY = 0f
        if (animationsEnabled) {
            when (animationMode) {
                0 -> {
                    if (cycle > 0.88f) blink = max(0.08f, 1f - (cycle - 0.88f) / 0.12f)
                    lookX = sin(now.toDouble() / 920.0).toFloat() * h * 0.035f
                }
                1 -> {
                    val fast = (now % 1450L).toFloat() / 1450f
                    if (fast in 0.72f..0.93f) blink = max(0.08f, 1f - (fast - 0.72f) / 0.21f)
                    lookX = sin(now.toDouble() / 300.0).toFloat() * h * 0.025f
                }
                2 -> {
                    blink = 0.68f + sin(now.toDouble() / 2100.0).toFloat().coerceAtLeast(0f) * 0.12f
                    lookY = h * 0.025f
                }
                3 -> {
                    lookY = sin(now.toDouble() / 180.0).toFloat() * h * 0.07f
                    lookX = sin(now.toDouble() / 360.0).toFloat() * h * 0.04f
                    if (cycle > 0.94f) blink = 0.12f
                }
                4 -> {
                    lookX = sin(now.toDouble() / 190.0).toFloat() * h * 0.07f
                    lookY = sin(now.toDouble() / 480.0).toFloat() * h * 0.025f
                    if (cycle > 0.90f) blink = 0.10f
                }
                5 -> {
                    if (cycle > 0.84f) blink = max(0.08f, 1f - (cycle - 0.84f) / 0.16f)
                    lookX = sin(now.toDouble() / 730.0).toFloat() * h * 0.02f
                }
            }
        }

        val eyeY = y + h * 0.52f + lookY
        val eyeW = h * if (style == 2) 0.34f else 0.32f
        val eyeH = h * (if (style == 2) 0.47f else 0.43f)
        val white = if (style == 1) Color.rgb(255, 240, 241) else Color.rgb(238, 248, 255)

        val rightBlink = if (animationsEnabled && animationMode == 5 && cycle > 0.84f) {
            max(0.08f, 1f - (cycle - 0.84f) / 0.16f)
        } else blink

        drawEye(canvas, x + w * 0.31f, eyeY, eyeW, eyeH * blink, white, eyeColor, lookX, style)
        drawEye(canvas, x + w * 0.69f, eyeY, eyeW, eyeH * rightBlink, white, eyeColor, lookX, style)

        if (animationMode == 2 && animationsEnabled) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = max(1f, h * 0.035f)
            p.strokeCap = Paint.Cap.ROUND
            p.color = Color.rgb(15, 20, 29)
            canvas.drawLine(x + w * 0.18f, eyeY - h * 0.14f, x + w * 0.44f, eyeY - h * 0.14f, p)
            canvas.drawLine(x + w * 0.56f, eyeY - h * 0.14f, x + w * 0.82f, eyeY - h * 0.14f, p)
            p.style = Paint.Style.FILL
        }

        // The small central dark lens keeps the real camera cutout visually integrated.
        p.color = Color.rgb(1, 2, 4)
        canvas.drawCircle(x + w * 0.50f, y + h * 0.52f, h * 0.075f, p)

        // A tiny friendly mouth gives the island a little more personality.
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, h * 0.023f)
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.argb(180, Color.red(eyeColor), Color.green(eyeColor), Color.blue(eyeColor))
        val smile = Path().apply {
            moveTo(x + w * 0.45f, y + h * 0.82f)
            quadTo(x + w * 0.50f, y + h * 0.89f, x + w * 0.55f, y + h * 0.82f)
        }
        canvas.drawPath(smile, p)
        p.style = Paint.Style.FILL

        when (accessory) {
            1 -> drawGlasses(canvas, x, y, w, h, eyeColor)
            2 -> drawCrown(canvas, x, y, w, h, eyeColor)
            3 -> drawAntenna(canvas, x, y, w, h, eyeColor)
            4 -> drawCheeks(canvas, x, y, w, h, eyeColor)
        }
    }

    private fun drawEye(
        canvas: Canvas, cx: Float, cy: Float, ew: Float, eh: Float,
        white: Int, iris: Int, look: Float, style: Int
    ) {
        p.style = Paint.Style.FILL
        p.color = white
        canvas.drawOval(RectF(cx - ew / 2f, cy - eh / 2f, cx + ew / 2f, cy + eh / 2f), p)
        val pupil = min(ew, eh) * if (style == 2) 0.38f else 0.34f
        p.color = iris
        canvas.drawCircle(cx + look, cy, pupil, p)
        p.color = Color.rgb(2, 8, 14)
        canvas.drawCircle(cx + look, cy, pupil * 0.53f, p)
        p.color = Color.WHITE
        canvas.drawCircle(cx + look - pupil * 0.25f, cy - pupil * 0.28f, max(1f, pupil * 0.18f), p)
    }

    private fun drawCatEars(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val a = Path().apply {
            moveTo(x + w * 0.13f, y + h * 0.30f)
            lineTo(x + w * 0.20f, y - h * 0.10f)
            lineTo(x + w * 0.38f, y + h * 0.25f)
            close()
        }
        val b = Path().apply {
            moveTo(x + w * 0.62f, y + h * 0.25f)
            lineTo(x + w * 0.80f, y - h * 0.10f)
            lineTo(x + w * 0.87f, y + h * 0.30f)
            close()
        }
        p.color = Color.rgb(14, 18, 26)
        c.drawPath(a, p)
        c.drawPath(b, p)
    }

    private fun drawAlienBumps(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        p.color = Color.rgb(9, 17, 24)
        c.drawCircle(x + w * 0.22f, y + h * 0.08f, h * 0.12f, p)
        c.drawCircle(x + w * 0.78f, y + h * 0.08f, h * 0.12f, p)
    }

    private fun drawGlasses(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1.2f, h * 0.035f)
        p.color = color
        c.drawRoundRect(RectF(x + w * 0.13f, y + h * 0.28f, x + w * 0.45f, y + h * 0.73f), h * 0.09f, h * 0.09f, p)
        c.drawRoundRect(RectF(x + w * 0.55f, y + h * 0.28f, x + w * 0.87f, y + h * 0.73f), h * 0.09f, h * 0.09f, p)
        c.drawLine(x + w * 0.45f, y + h * 0.48f, x + w * 0.55f, y + h * 0.48f, p)
        p.style = Paint.Style.FILL
    }

    private fun drawCrown(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        val crown = Path().apply {
            moveTo(x + w * 0.34f, y + h * 0.13f)
            lineTo(x + w * 0.38f, y - h * 0.11f)
            lineTo(x + w * 0.49f, y + h * 0.07f)
            lineTo(x + w * 0.61f, y - h * 0.11f)
            lineTo(x + w * 0.66f, y + h * 0.13f)
            close()
        }
        p.color = color
        c.drawPath(crown, p)
    }

    private fun drawAntenna(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, h * 0.035f)
        p.color = color
        c.drawLine(x + w * 0.50f, y + h * 0.07f, x + w * 0.50f, y - h * 0.17f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(x + w * 0.50f, y - h * 0.19f, h * 0.065f, p)
    }

    private fun drawCheeks(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.color = Color.argb(175, Color.red(color), Color.green(color), Color.blue(color))
        c.drawOval(RectF(x + w * 0.08f, y + h * 0.66f, x + w * 0.22f, y + h * 0.81f), p)
        c.drawOval(RectF(x + w * 0.78f, y + h * 0.66f, x + w * 0.92f, y + h * 0.81f), p)
    }
}
