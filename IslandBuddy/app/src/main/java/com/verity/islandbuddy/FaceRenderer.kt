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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, style: Int, animated: Boolean) {
        val radius = h * 0.5f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(2, 4, 8)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, h * 0.025f)
        paint.color = Color.rgb(34, 42, 53)
        canvas.drawRoundRect(RectF(x + 1f, y + 1f, x + w - 1f, y + h - 1f), radius, radius, paint)
        paint.style = Paint.Style.FILL

        if (style == 1) {
            val leftEar = Path().apply {
                moveTo(x + w * 0.20f, y + h * 0.30f)
                lineTo(x + w * 0.24f, y - h * 0.02f)
                lineTo(x + w * 0.34f, y + h * 0.28f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(x + w * 0.66f, y + h * 0.28f)
                lineTo(x + w * 0.76f, y - h * 0.02f)
                lineTo(x + w * 0.80f, y + h * 0.30f)
                close()
            }
            paint.color = Color.rgb(12, 15, 22)
            canvas.drawPath(leftEar, paint)
            canvas.drawPath(rightEar, paint)
        }

        val cycle = (SystemClock.uptimeMillis() % 3600L).toFloat() / 3600f
        val blink = if (animated && cycle > 0.90f) max(0.10f, 1f - (cycle - 0.90f) / 0.10f) else 1f
        val look = if (animated) sin(SystemClock.uptimeMillis().toDouble() / 850.0).toFloat() * h * 0.035f else 0f
        val eyeY = y + h * 0.53f
        val eyeW = h * if (style == 2) 0.34f else 0.31f
        val eyeH = h * (if (style == 2) 0.48f else 0.40f) * blink
        val white = if (style == 1) Color.rgb(244, 235, 226) else Color.rgb(236, 247, 255)
        val iris = when (style) {
            1 -> Color.rgb(255, 150, 190)
            2 -> Color.rgb(104, 255, 145)
            else -> Color.rgb(60, 207, 255)
        }
        drawEye(canvas, x + w * 0.31f, eyeY, eyeW, eyeH, white, iris, look, style)
        drawEye(canvas, x + w * 0.69f, eyeY, eyeW, eyeH, white, iris, look, style)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(1, 2, 4)
        canvas.drawCircle(x + w * 0.50f, eyeY, h * 0.085f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, h * 0.018f)
        paint.color = Color.rgb(36, 45, 58)
        canvas.drawCircle(x + w * 0.50f, eyeY, h * 0.085f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawEye(
        canvas: Canvas, cx: Float, cy: Float, ew: Float, eh: Float,
        white: Int, iris: Int, look: Float, style: Int
    ) {
        paint.style = Paint.Style.FILL
        paint.color = white
        canvas.drawOval(RectF(cx - ew / 2f, cy - eh / 2f, cx + ew / 2f, cy + eh / 2f), paint)
        val pupil = min(ew, eh) * if (style == 2) 0.38f else 0.34f
        paint.color = iris
        canvas.drawCircle(cx + look, cy, pupil, paint)
        paint.color = Color.rgb(2, 8, 14)
        canvas.drawCircle(cx + look, cy, pupil * 0.53f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx + look - pupil * 0.25f, cy - pupil * 0.28f, max(1f, pupil * 0.18f), paint)
    }
}
