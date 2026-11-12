package com.cyborgos.mobile

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.min

/**
 * Original, lightweight vector wallpapers inspired by fluid iPhone gradients
 * and Android Material You colour fields. No external image files required.
 */
class DualWallpaperDrawable(private val iphoneSide: Boolean) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var drawableAlpha = 255
    private var filter: ColorFilter? = null

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        if (w <= 0f || h <= 0f) return

        val save = canvas.save()
        val radius = min(w, h) * 0.055f
        val clip = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(clip)

        paint.style = Paint.Style.FILL
        paint.alpha = drawableAlpha
        paint.colorFilter = filter
        paint.shader = if (iphoneSide) {
            LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(25, 53, 116), Color.rgb(90, 112, 190),
                    Color.rgb(179, 126, 174), Color.rgb(42, 61, 123)),
                null, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(7, 45, 58), Color.rgb(20, 91, 99),
                    Color.rgb(47, 77, 126), Color.rgb(13, 31, 58)),
                null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        if (iphoneSide) drawIphoneLayers(canvas, w, h)
        else drawAndroidLayers(canvas, w, h)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = min(w, h) * 0.004f
        paint.color = if (iphoneSide) Color.argb(80, 244, 247, 255)
            else Color.argb(75, 224, 250, 245)
        paint.shader = null
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)
        canvas.restoreToCount(save)
        paint.colorFilter = null
    }

    private fun drawIphoneLayers(canvas: Canvas, w: Float, h: Float) {
        val first = Path().apply {
            moveTo(-w * .12f, h * .84f)
            cubicTo(w * .08f, h * .52f, w * .28f, h * .31f, w * .53f, h * .12f)
            cubicTo(w * .76f, -h * .04f, w * .98f, h * .02f, w * 1.12f, h * .19f)
            lineTo(w * 1.12f, h * .54f)
            cubicTo(w * .88f, h * .39f, w * .69f, h * .52f, w * .47f, h * .73f)
            cubicTo(w * .23f, h * .96f, w * .05f, h * 1.02f, -w * .12f, h * .98f)
            close()
        }
        fillPath(canvas, first, w, h, intArrayOf(
            Color.rgb(188, 222, 255), Color.rgb(106, 165, 246), Color.rgb(79, 107, 194)
        ))

        val second = Path().apply {
            moveTo(-w * .1f, h * .15f)
            cubicTo(w * .19f, h * .28f, w * .26f, h * .53f, w * .53f, h * .63f)
            cubicTo(w * .79f, h * .73f, w * .98f, h * .64f, w * 1.1f, h * .48f)
            lineTo(w * 1.1f, h * 1.12f)
            lineTo(-w * .1f, h * 1.12f)
            close()
        }
        fillPath(canvas, second, w, h, intArrayOf(
            Color.rgb(245, 190, 202), Color.rgb(158, 132, 194), Color.rgb(58, 78, 151)
        ))

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(w * .18f, h * .18f, min(w, h) * .48f,
            intArrayOf(Color.argb(85, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun drawAndroidLayers(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(w * .84f, h * .20f, min(w, h) * .62f,
            intArrayOf(Color.argb(150, 90, 218, 202), Color.argb(0, 90, 218, 202)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val upper = Path().apply {
            moveTo(w * .34f, -h * .08f)
            cubicTo(w * .64f, h * .05f, w * .89f, h * .16f, w * 1.12f, h * .40f)
            lineTo(w * 1.12f, h * .70f)
            cubicTo(w * .83f, h * .47f, w * .55f, h * .45f, w * .33f, h * .28f)
            cubicTo(w * .15f, h * .14f, w * .18f, h * .02f, w * .34f, -h * .08f)
            close()
        }
        fillPath(canvas, upper, w, h, intArrayOf(
            Color.rgb(105, 220, 205), Color.rgb(63, 151, 180), Color.rgb(77, 108, 177)
        ))

        val lower = Path().apply {
            moveTo(-w * .12f, h * .62f)
            cubicTo(w * .12f, h * .43f, w * .31f, h * .49f, w * .49f, h * .68f)
            cubicTo(w * .69f, h * .89f, w * .91f, h * .86f, w * 1.12f, h * .69f)
            lineTo(w * 1.12f, h * 1.12f)
            lineTo(-w * .12f, h * 1.12f)
            close()
        }
        fillPath(canvas, lower, w, h, intArrayOf(
            Color.rgb(28, 93, 105), Color.rgb(40, 68, 119), Color.rgb(16, 37, 72)
        ))
    }

    private fun fillPath(canvas: Canvas, path: Path, w: Float, h: Float, colors: IntArray) {
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, h * .08f, w, h * .92f,
            colors, null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        filter = colorFilter
        invalidateSelf()
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
