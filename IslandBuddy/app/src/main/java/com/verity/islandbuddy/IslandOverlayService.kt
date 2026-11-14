package com.verity.islandbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class IslandOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var islandView: IslandFaceView? = null
    private var params: WindowManager.LayoutParams? = null
    private var savedWidth = 178
    private var savedHeight = 48
    private var savedOffset = 0
    private var savedStyle = 0

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
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        savedWidth = intent?.getIntExtra(EXTRA_WIDTH, savedWidth) ?: savedWidth
        savedHeight = intent?.getIntExtra(EXTRA_HEIGHT, savedHeight) ?: savedHeight
        savedOffset = intent?.getIntExtra(EXTRA_OFFSET, savedOffset) ?: savedOffset
        savedStyle = intent?.getIntExtra(EXTRA_STYLE, savedStyle) ?: savedStyle
        showOrUpdateIsland()
        return START_STICKY
    }

    private fun showOrUpdateIsland() {
        val density = resources.displayMetrics.density
        val widthPx = (savedWidth * density).toInt()
        val heightPx = (savedHeight * density).toInt()
        if (islandView == null) {
            val view = IslandFaceView(this).apply { style = savedStyle }
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
                y = (savedOffset * density).toInt()
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
                setTitle("Island Buddy")
            }
            view.onOpenAssistant = { openAssistant() }
            view.onMoved = { dx, dy ->
                lp.x += dx
                lp.y += dy
                try { windowManager.updateViewLayout(view, lp) } catch (_: Exception) {}
            }
            islandView = view
            params = lp
            try {
                windowManager.addView(view, lp)
            } catch (_: Exception) {
                islandView = null
                params = null
                stopSelf()
            }
        } else {
            val view = islandView ?: return
            val lp = params ?: return
            lp.width = widthPx
            lp.height = heightPx
            view.style = savedStyle
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

    override fun onDestroy() {
        islandView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
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
            FaceRenderer.draw(canvas, 0f, 0f, width.toFloat(), height.toFloat(), style, true)
            postInvalidateDelayed(130)
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
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_OFFSET = "offset"
        const val EXTRA_STYLE = "style"
        private const val CHANNEL = "island_buddy_overlay"
        private const val NOTIFICATION_ID = 4107
    }
}
