package com.verity.islandbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

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

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isThinking = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val chatHistory = mutableListOf<Pair<String, String>>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var serviceDestroyed = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initTts()
        createNotificationChannel()
        startAsForeground()
    }

    private fun startAsForeground() {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Island Buddy включён")
            .setContentText("Нажми на персонажа, чтобы поговорить с Бадди")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            val types = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(NOTIFICATION_ID, notification, types)
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
        val widthPx = (savedWidth * density).toInt().coerceAtLeast(1)
        val heightPx = (savedHeight * density).toInt().coerceAtLeast(1)
        val display = resources.displayMetrics
        val maxX = ((display.widthPixels - widthPx) / 2).coerceAtLeast(0)
        val maxY = (display.heightPixels - heightPx).coerceAtLeast(0)
        // Старое смещение из альбомного режима может оказаться за краем
        // портретного экрана. Ограничиваем окно видимой областью.
        val safeX = (savedX * density).toInt().coerceIn(-maxX, maxX)
        val safeY = (savedY * density).toInt().coerceIn(0, maxY)

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
                x = safeX
                y = safeY
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
                setTitle("Island Buddy")
            }
            view.onOpenAssistant = { openAssistant() }
            view.onMoved = { dx, dy ->
                val currentDisplay = resources.displayMetrics
                val currentMaxX = ((currentDisplay.widthPixels - lp.width) / 2).coerceAtLeast(0)
                val currentMaxY = (currentDisplay.heightPixels - lp.height).coerceAtLeast(0)
                lp.x = (lp.x + dx).coerceIn(-currentMaxX, currentMaxX)
                lp.y = (lp.y + dy).coerceIn(0, currentMaxY)
                savedX = (lp.x / resources.displayMetrics.density).toInt()
                savedY = (lp.y / resources.displayMetrics.density).toInt().coerceAtLeast(0)
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
            lp.x = safeX
            lp.y = safeY
            view.style = savedStyle
            view.eyeColor = savedEyeColor
            view.animationsEnabled = savedAnimations
            view.animationMode = savedAnimationMode
            view.accessory = savedAccessory
            view.invalidate()
            try { windowManager.updateViewLayout(view, lp) } catch (_: Exception) {}
        }
    }

    // Голос распознаётся прямо внутри overlay-сервиса: пользователь остаётся в текущем приложении.
    private fun openAssistant() {
        if (isListening) {
            try { speechRecognizer?.stopListening() } catch (_: Exception) {}
            isListening = false
            showReplyBubble("Бадди: Остановил прослушивание.")
            return
        }
        if (isThinking) {
            showReplyBubble("Бадди: Я уже думаю над твоим вопросом. Секундочку!")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showReplyBubble("Бадди: Открой Island Buddy и разреши микрофон, тогда я смогу слушать тебя поверх приложений.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showReplyBubble("Бадди: Не нашёл голосовой сервис на телефоне. Проверь голосовой ввод Google.")
            return
        }
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { isListening = true; showReplyBubble("Бадди: Слушаю тебя! Говори…") }
                    override fun onBeginningOfSpeech() { showReplyBubble("Бадди: Ага, слышу тебя!") }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false; showReplyBubble("Бадди: Сейчас подумаю…") }
                    override fun onError(error: Int) {
                        isListening = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Бадди: Не расслышал. Нажми на меня и попробуй ещё раз."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Бадди: Мне нужен доступ к микрофону. Разреши его в настройках."
                            else -> "Бадди: Голосовой ввод прервался. Нажми на меня, чтобы повторить."
                        }
                        showReplyBubble(msg)
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val question = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                        if (question.isBlank()) showReplyBubble("Бадди: Не расслышал вопрос. Давай ещё раз!")
                        else answerQuestion(question)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            isListening = true
            showReplyBubble("Бадди: Слушаю тебя! Говори…")
            speechRecognizer?.startListening(speechIntent)
        } catch (_: Exception) {
            isListening = false
            showReplyBubble("Бадди: Не удалось включить микрофон. Проверь разрешения и голосовой сервис.")
        }
    }

    private fun answerQuestion(question: String) {
        isThinking = true
        chatHistory.add("user" to question)
        showReplyBubble("Бадди: Хм… дай-ка подумать.")
        val key = SecretBox.load(this)
        if (key.isBlank()) {
            val local = AssistantLogic.localReply(question)
            chatHistory.add("assistant" to local)
            finishAnswer(local)
            return
        }
        val prefs = getSharedPreferences("island_buddy_settings", MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "https://openrouter.ai/api/v1/chat/completions") ?: "https://openrouter.ai/api/v1/chat/completions"
        val model = prefs.getString("model", "openrouter/auto") ?: "openrouter/auto"
        AiClient.ask(endpoint, model, key, chatHistory.toList()) { answer, error ->
            mainHandler.post {
                if (serviceDestroyed) return@post
                if (answer != null) {
                    chatHistory.add("assistant" to answer)
                    finishAnswer(answer)
                } else finishAnswer("Упс, не получилось получить ответ. Проверь интернет и настройки ИИ. " + (error ?: ""))
            }
        }
    }

    private fun finishAnswer(answer: String) {
        isThinking = false
        showReplyBubble("Бадди: " + answer)
        speak(answer)
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(this) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    val lang = tts?.setLanguage(Locale("ru", "RU")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    ttsReady = lang >= TextToSpeech.LANG_AVAILABLE
                    tts?.setSpeechRate(1.02f)
                    tts?.setPitch(1.08f)
                    val pending = pendingSpeech
                    pendingSpeech = null
                    if (ttsReady && !pending.isNullOrBlank()) tts?.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "buddy-answer")
                }
            }
        } catch (_: Exception) { ttsReady = false }
    }

    private fun speak(text: String) {
        if (tts == null) initTts()
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-answer")
        else pendingSpeech = text
    }

    private fun showReplyBubble(message: String) {
        removeReplyBubble()
        val text = TextView(this).apply {
            this.text = message.take(520)
            setTextColor(Color.rgb(25, 31, 41))
            textSize = 14f
            maxLines = 5
            setPadding(dp(14), dp(11), dp(5), dp(11))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val close = Button(this).apply {
            this.text = "×"
            textSize = 21f
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
            addView(close, LinearLayout.LayoutParams(dp(39), dp(39)))
        }
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleWidth = (screenWidth - dp(24)).coerceAtMost(dp(324)).coerceAtLeast(dp(220))
        val lp = WindowManager.LayoutParams(
            bubbleWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(88)
            setTitle("Island Buddy — сообщение Бадди")
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
        serviceDestroyed = true
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        removeReplyBubble()
        islandView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        islandView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Пересчитываем окно островка при переходе между портретным и альбомным режимом.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mainHandler.postDelayed({
            if (!serviceDestroyed && islandView != null) showOrUpdateIsland()
        }, 120)
    }

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
        // 7 физических пикселей слишком мало: дрожание пальца превращало
        // обычный тап в перетаскивание. Минимум 12 dp плюс системный touch slop.
        private val touchSlop = maxOf(
            ViewConfiguration.get(context).scaledTouchSlop,
            (12f * context.resources.displayMetrics.density).toInt()
        )

        init {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isClickable = true
            contentDescription = "Island Buddy — нажми, чтобы поговорить"
        }

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
                    val currentX = event.rawX
                    val currentY = event.rawY
                    if (!moved) {
                        val totalX = currentX - downRawX
                        val totalY = currentY - downRawY
                        if (totalX * totalX + totalY * totalY > touchSlop * touchSlop) {
                            moved = true
                            onMoved?.invoke(totalX.toInt(), totalY.toInt())
                        }
                    } else {
                        onMoved?.invoke(
                            (currentX - lastRawX).toInt(),
                            (currentY - lastRawY).toInt()
                        )
                    }
                    lastRawX = currentX
                    lastRawY = currentY
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val totalX = event.rawX - downRawX
                    val totalY = event.rawY - downRawY
                    if (!moved && totalX * totalX + totalY * totalY <= touchSlop * touchSlop) {
                        performClick()
                    }
                    moved = false
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    moved = false
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