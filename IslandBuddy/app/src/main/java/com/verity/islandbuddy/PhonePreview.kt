package com.verity.islandbuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min

class PhonePreview(context: Context) : View(context) {
    var islandWidthDp = 178
    var islandHeightDp = 48
    var verticalOffsetDp = 0
    var faceStyle = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        canvas.save()
        canvas.scale(d, d)
        val w = width / d
        val h = height / d
        p.style = Paint.Style.FILL
        p.color = Color.rgb(5, 8, 14)
        canvas.drawRoundRect(RectF(0f, 0f, w, h), 28f, 28f, p)
        val sw = w - 14f
        val sh = h - 14f
        p.shader = LinearGradient(0f, 7f, sw, sh, Color.rgb(29, 48, 74), Color.rgb(10, 19, 32), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(7f, 7f, w - 7f, h - 7f), 23f, 23f, p)
        p.shader = null
        p.color = Color.argb(150, 255, 255, 255)
        p.textSize = 9f
        canvas.drawText("HONOR X8b  •  ПРЕДПРОСМОТР", 19f, 24f, p)

        val pillW = min(islandWidthDp.toFloat(), sw - 34f)
        val pillH = islandHeightDp.toFloat()
        FaceRenderer.draw(canvas, (w - pillW) / 2f, 30f + verticalOffsetDp, pillW, pillH, faceStyle, false)

        p.color = Color.argb(90, 255, 255, 255)
        p.textSize = 9f
        canvas.drawText("Рабочий стол", 20f, 112f, p)
        val colors = intArrayOf(
            Color.rgb(75, 153, 255), Color.rgb(71, 194, 143),
            Color.rgb(247, 173, 70), Color.rgb(178, 126, 255),
            Color.rgb(245, 102, 126), Color.rgb(70, 188, 205)
        )
        val startY = h * 0.59f
        for (row in 0..2) for (col in 0..3) {
            val cx = 37f + col * ((w - 74f) / 3f)
            val cy = startY + row * 57f
            p.color = colors[(row * 4 + col) % colors.size]
            canvas.drawRoundRect(RectF(cx - 14f, cy - 14f, cx + 14f, cy + 14f), 9f, 9f, p)
        }
        p.color = Color.argb(55, 255, 255, 255)
        canvas.drawRoundRect(RectF(20f, h - 45f, w - 20f, h - 17f), 16f, 16f, p)
        p.color = Color.WHITE
        p.textSize = 9f
        canvas.drawText("Островок поверх приложений", 27f, h - 29f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = Color.rgb(78, 91, 110)
        canvas.drawRoundRect(RectF(0.7f, 0.7f, w - 0.7f, h - 0.7f), 28f, 28f, p)
        p.style = Paint.Style.FILL
        canvas.restore()
    }
}
