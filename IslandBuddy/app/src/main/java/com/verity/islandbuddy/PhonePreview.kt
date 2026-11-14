package com.verity.islandbuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PhonePreview(context: Context) : View(context) {
    var islandWidthDp = 178
    var islandHeightDp = 48
    var horizontalOffsetDp = 0
    var verticalOffsetDp = 0
    var faceStyle = 0
    var eyeColor = Color.rgb(60, 207, 255)
    var animationsEnabled = true
    var animationMode = 0
    var accessory = 0
    var onGeometryChanged: ((Int, Int, Int, Int) -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val phoneWidthDp = 393f
    private var scaleFactor = 1f
    private var dragMode = -1
    private var activeHandle = -1
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false

    private fun islandRect(): RectF {
        val viewWidthDp = width / resources.displayMetrics.density
        scaleFactor = viewWidthDp / phoneWidthDp
        val cx = phoneWidthDp * 0.5f + horizontalOffsetDp
        val top = 8f + verticalOffsetDp
        return RectF(
            (cx - islandWidthDp / 2f) * scaleFactor,
            top * scaleFactor,
            (cx + islandWidthDp / 2f) * scaleFactor,
            (top + islandHeightDp) * scaleFactor
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val vw = width / density
        val vh = height / density
        val screenScale = vw / phoneWidthDp
        scaleFactor = screenScale

        canvas.save()
        canvas.scale(density, density)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(4, 7, 12)
        canvas.drawRoundRect(RectF(0f, 0f, vw, vh), 25f, 25f, p)

        // A compact phone wallpaper and status row give a clearer scale reference.
        p.color = Color.rgb(16, 29, 48)
        canvas.drawRoundRect(RectF(5f, 5f, vw - 5f, vh - 5f), 21f, 21f, p)
        p.color = 0xFFDAE5F1.toInt()
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 10f
        canvas.drawText("9:41", 13f, 20f, p)
        p.textSize = 9f
        canvas.drawText("▮  Wi-Fi  ▰", max(13f, vw - 66f), 20f, p)

        canvas.save()
        canvas.scale(screenScale, screenScale)
        p.color = Color.rgb(80, 115, 157)
        canvas.drawRoundRect(RectF(8f, 45f, phoneWidthDp - 8f, 870f), 30f, 30f, p)
        p.color = Color.rgb(31, 48, 72)
        canvas.drawRoundRect(RectF(13f, 50f, phoneWidthDp - 13f, 865f), 26f, 26f, p)

        val iconColors = intArrayOf(
            Color.rgb(83, 157, 255), Color.rgb(72, 198, 150),
            Color.rgb(248, 177, 79), Color.rgb(181, 133, 255),
            Color.rgb(244, 109, 134), Color.rgb(72, 193, 211)
        )
        val y0 = 530f
        for (row in 0..3) for (col in 0..3) {
            val cx = 60f + col * 91f
            val cy = y0 + row * 76f
            p.color = iconColors[(row * 4 + col) % iconColors.size]
            canvas.drawRoundRect(RectF(cx - 18f, cy - 18f, cx + 18f, cy + 18f), 11f, 11f, p)
        }

        p.color = Color.argb(150, 255, 255, 255)
        p.textSize = 18f
        p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("HONOR X8b", 26f, 102f, p)
        p.textSize = 12f
        p.typeface = Typeface.DEFAULT
        canvas.drawText("Предпросмотр расположения островка", 26f, 126f, p)

        val actualRect = RectF(
            phoneWidthDp * 0.5f + horizontalOffsetDp - islandWidthDp / 2f,
            8f + verticalOffsetDp,
            phoneWidthDp * 0.5f + horizontalOffsetDp + islandWidthDp / 2f,
            8f + verticalOffsetDp + islandHeightDp
        )
        FaceRenderer.draw(
            canvas,
            actualRect.left, actualRect.top,
            actualRect.width(), actualRect.height(),
            faceStyle, eyeColor, animationsEnabled, animationMode, accessory
        )

        // Selection frame and eight round resize handles.
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = Color.rgb(105, 192, 255)
        canvas.drawRoundRect(
            RectF(actualRect.left - 8f, actualRect.top - 8f, actualRect.right + 8f, actualRect.bottom + 8f),
            12f, 12f, p
        )
        p.style = Paint.Style.FILL
        val handles = handlePoints(actualRect)
        handles.forEachIndexed { index, point ->
            p.color = if (index % 2 == 0) Color.WHITE else Color.rgb(105, 192, 255)
            canvas.drawCircle(point.first, point.second, 5.7f, p)
            p.color = Color.rgb(27, 105, 175)
            canvas.drawCircle(point.first, point.second, 2.4f, p)
        }
        p.color = Color.argb(220, 255, 255, 255)
        p.textSize = 15f
        p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Потяни за точки — изменить размер", 25f, 190f, p)
        p.textSize = 13f
        p.typeface = Typeface.DEFAULT
        canvas.drawText("Перетаскивай сам островок — изменить позицию", 25f, 214f, p)
        canvas.restore()

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.rgb(88, 105, 130)
        canvas.drawRoundRect(RectF(0.7f, 0.7f, vw - 0.7f, vh - 0.7f), 25f, 25f, p)
        p.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun handlePoints(r: RectF): List<Pair<Float, Float>> {
        val left = r.left
        val right = r.right
        val top = r.top
        val bottom = r.bottom
        return listOf(
            left to top,
            (left + right) / 2f to top,
            right to top,
            right to (top + bottom) / 2f,
            right to bottom,
            (left + right) / 2f to bottom,
            left to bottom,
            left to (top + bottom) / 2f
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val density = resources.displayMetrics.density
        val px = event.x / density
        val py = event.y / density
        val rect = islandRect()
        val points = handlePoints(rect)
        val hitRadius = 24f * scaleFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = px
                lastY = py
                moved = false
                activeHandle = points.indexOfFirst {
                    val dx = it.first - px
                    val dy = it.second - py
                    dx * dx + dy * dy <= hitRadius * hitRadius
                }
                dragMode = when {
                    activeHandle >= 0 -> 1
                    rect.contains(px, py) -> 0
                    else -> -1
                }
                if (dragMode < 0) return false
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragMode < 0) return false
                val dx = (px - lastX) / scaleFactor
                val dy = (py - lastY) / scaleFactor
                if (abs(dx) > 0.5f || abs(dy) > 0.5f) moved = true

                if (dragMode == 0) {
                    horizontalOffsetDp = (horizontalOffsetDp + dx).toInt()
                    verticalOffsetDp = (verticalOffsetDp + dy).toInt().coerceIn(0, 570)
                } else {
                    val leftHandle = activeHandle == 0 || activeHandle == 6 || activeHandle == 7
                    val rightHandle = activeHandle == 2 || activeHandle == 3 || activeHandle == 4
                    val topHandle = activeHandle == 0 || activeHandle == 1 || activeHandle == 2
                    val bottomHandle = activeHandle == 4 || activeHandle == 5 || activeHandle == 6

                    if (leftHandle) {
                        islandWidthDp = (islandWidthDp - dx).toInt().coerceIn(110, 270)
                        horizontalOffsetDp = (horizontalOffsetDp + dx / 2f).toInt()
                    } else if (rightHandle) {
                        islandWidthDp = (islandWidthDp + dx).toInt().coerceIn(110, 270)
                        if (islandWidthDp == 270) horizontalOffsetDp = horizontalOffsetDp.coerceIn(-60, 60)
                    }
                    if (topHandle) {
                        islandHeightDp = (islandHeightDp - dy).toInt().coerceIn(34, 90)
                        verticalOffsetDp = (verticalOffsetDp + dy).toInt().coerceIn(0, 570)
                    } else if (bottomHandle) {
                        islandHeightDp = (islandHeightDp + dy).toInt().coerceIn(34, 90)
                    }
                }
                lastX = px
                lastY = py
                onGeometryChanged?.invoke(islandWidthDp, islandHeightDp, horizontalOffsetDp, verticalOffsetDp)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!moved) performClick()
                dragMode = -1
                activeHandle = -1
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
