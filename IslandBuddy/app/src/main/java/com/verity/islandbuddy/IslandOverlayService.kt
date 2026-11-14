package com.verity.islandbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class IslandOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var islandView: IslandFaceView? = null
    private var islandParams: WindowManager.LayoutParams? = null
    private var replyView: View? = null
    private var replyParams: WindowManager.LayoutParams? = null

    private var savedWidth = 178
    private var savedHeight = 48
    private var savedX = 0
    private var savedY = 0
    private var savedStyle = 0
    private var savedEyeColor = Color.rgb(60, 207, 255)
    private var savedAnimations = true
    private var savedAnimationMode = 0
    private var savedAccessory = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startAsForeground()
    }

    private fun startAsForeground() {
        val open = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ASSIST
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Island Buddy включён")
            .setContentText("Нажми на островок, чтобы поговорить с помощником")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW_REPLY -> {
                val message = intent.getStringExtra(EXTRA_REPLY).orEmpty()
                if (message.isNotBlank()) showReplyBubble(message)
                return START_STICKY
            }
            ACTION_DISMISS_REPLY -> {
                removeReplyBubble()
                return START_STICKY
            }
        }

        savedWidth = intent?.getIntExtra(EXTRA_WIDTH, savedWidth) ?: savedWidth
        savedHeight = intent?.getIntExtra(EXTRA_HEIGHT, savedHeight) ?: savedHeight
        savedX = intent?.getIntExtra(EXTRA_X, savedX) ?: savedX
        savedY = intent?.getIntExtra(EXTRA_Y, savedY) ?: savedY
        savedStyle = intent?.getIntExtra(EXTRA_STYLE, savedStyle) ?: savedStyle
        savedEyeColor = intent?.getIntExtra(EXTRA_EYE_COLOR, savedEyeColor) ?: savedEyeColor
        savedAnimations = intent?.getBooleanExtra(EXTRA_ANIMATIONS, savedAnimations) ?: savedAnimations
        savedAnimationMode = intent?.getIntExtra(EXTRA_ANIMATION_MODE, savedAnimationMode) ?: savedAnimationMode
        savedAccessory = intent?.getIntExtra(EXTRA_ACCESSORY, savedAccessory) ?: savedAccessory
        showOrUpdateIsland()
        return START_STICKY
    }

    private fun showOrUpdateIsland() {
        val density = resources.displayMetrics.density
        val widthPx = (savedWidth * density).toInt()
        val heightPx = (savedHeight * density).toInt()

        if (islandView == null) {
            val view = IslandFaceView(this).apply {
                style = savedStyle
                eyeColor = savedEyeColor
                animationsEnabled = savedAnimations
                animationMode = savedAnimationMode
                accessory = savedAccessory
            }
            val lp = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = (savedX * density).toInt()
                y = (savedY * density).toInt()
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
                setTitle("Island Buddy")
            }
            view.onOpenAssistant = { openAssistant() }
            view.onMoved = { dx, dy ->
                lp.x += dx
                lp.y += dy
                savedX = (lp.x / density).toInt()
                savedY = (lp.y / density).toInt().coerceAtLeast(0)
                try { windowManager.updateViewLayout(view, lp) } catch (_: Exception) {}
            }
            islandView = view
            islandParams = lp
            try {
                windowManager.addView(view, lp)
            } catch (_: Exception) {
                islandView = null
                islandParams = null
                stopSelf()
            }
        } else {
            val view = islandView ?: return
            val lp = islandParams ?: return
            lp.width = widthPx
            lp.height = heightPx
            lp.x = (savedX * density).toInt()
            lp.y = (savedY * density).toInt()
            view.style = savedStyle
            view.eyeColor = savedEyeColor
            view.animationsEnabled = savedAnimations
            view.animationMode = savedAnimationMode
            view.accessory = savedAccessory
            view.invalidate()
            try { windowManager.updateViewLayout(view, lp) } catch (_: Exception) {}
        }
    }

    private fun openAssistant() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ASSIST
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun showReplyBubble(message: String) {
        removeReplyBubble()
        val text = TextView(this).apply {
            this.text = message.take(520)
            setTextColor(Color.rgb(25, 31, 41))
            textSize = 14f
            maxLines = 6
            setPadding(dp(15), dp(12), dp(7), dp(12))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val close = Button(this).apply {
            text = "×"
            textSize = 22f
            isAllCaps = false
            setTextColor(Color.rgb(40, 50, 65))
            background = transparentRounded(Color.rgb(235, 240, 247), dp(18))
            setOnClickListener { removeReplyBubble() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(5), dp(6), dp(5))
            background = rounded(Color.WHITE, dp(20))
            elevation = dp(12).toFloat()
            addView(text, LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f))
            addView(close, LinearLayout.LayoutParams(dp(43), dp(43)))
        }
        val screenWidth = resources.displayMetrics.widthPixels
        val lp = WindowManager.LayoutParams(
            (screenWidth - dp(28)).coerceAtLeast(dp(220)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(92)
            setTitle("Island Buddy — ответ")
        }
        try {
            windowManager.addView(row, lp)
            replyView = row
            replyParams = lp
        } catch (_: Exception) {
            replyView = null
            replyParams = null
        }
    }

    private fun removeReplyBubble() {
        replyView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        replyView = null
        replyParams = null
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun transparentRounded(color: Int, radius: Int): GradientDrawable =
        rounded(color, radius)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeReplyBubble()
        islandView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        islandView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Island Buddy", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private class IslandFaceView(context: android.content.Context) : View(context) {
        var style = 0
        var eyeColor = Color.rgb(60, 207, 255)
        var animationsEnabled = true
        var animationMode = 0
        var accessory = 0
        var onOpenAssistant: (() -> Unit)? = null
        var onMoved: ((Int, Int) -> Unit)? = null
        private var downRawX = 0f
        private var downRawY = 0f
        private var moved = false
        private var lastRawX = 0f
        private var lastRawY = 0f

        init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            FaceRenderer.draw(
                canvas, 0f, 0f, width.toFloat(), height.toFloat(),
                style, eyeColor, animationsEnabled, animationMode, accessory
            )
            if (animationsEnabled) postInvalidateDelayed(95)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - downRawX) > 7f || abs(event.rawY - downRawY) > 7f) moved = true
                    if (moved) {
                        val dx = (event.rawX - lastRawX).toInt()
                        val dy = (event.rawY - lastRawY).toInt()
                        onMoved?.invoke(dx, dy)
                        lastRawX = event.rawX
                        lastRawY = event.rawY
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) performClick()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            onOpenAssistant?.invoke()
            return true
        }
    }

    companion object {
        const val ACTION_START = "com.verity.islandbuddy.START"
        const val ACTION_STOP = "com.verity.islandbuddy.STOP"
        const val ACTION_SHOW_REPLY = "com.verity.islandbuddy.SHOW_REPLY"
        const val ACTION_DISMISS_REPLY = "com.verity.islandbuddy.DISMISS_REPLY"
        const val EXTRA_REPLY = "reply"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
        const val EXTRA_STYLE = "style"
        const val EXTRA_EYE_COLOR = "eye_color"
        const val EXTRA_ANIMATIONS = "animations"
        const val EXTRA_ANIMATION_MODE = "animation_mode"
        const val EXTRA_ACCESSORY = "accessory"
        private const val CHANNEL = "island_buddy_overlay"
        private const val NOTIFICATION_ID = 4107
    }
}
