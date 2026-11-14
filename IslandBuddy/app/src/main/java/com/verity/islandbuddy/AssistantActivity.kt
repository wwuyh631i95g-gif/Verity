package com.verity.islandbuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class AssistantActivity : Activity() {
    private var statusView: TextView? = null
    private var input: EditText? = null
    private var sendButton: Button? = null
    private var listening = false
    private var waitingForSpeech = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val history = mutableListOf<Pair<String, String>>()

    companion object {
        private const val REQUEST_MIC = 711
        private const val REQUEST_RECOGNIZER = 712
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 13, 22)
        window.navigationBarColor = Color.rgb(9, 13, 22)
        buildScreen()
        initTts()
        status("Бадди проснулся. Слушаю тебя…")
        showBubble("Бадди: Я здесь! Говори, что хочешь узнать.")
        statusView?.postDelayed({ beginVoiceInput() }, 550)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!waitingForSpeech) {
            statusView?.postDelayed({ beginVoiceInput() }, 250)
        }
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(26), dp(20), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(9, 13, 22), Color.rgb(20, 31, 48))
            )
        }

        page.addView(TextView(this).apply {
            text = "●"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(78, 207, 255))
        })
        page.addView(TextView(this).apply {
            text = "БАДДИ"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Твой маленький спутник рядом с камерой"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF9CAFC6.toInt())
        })

        statusView = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(26), dp(8), dp(26))
            text = "Слушаю тебя…"
        }
        page.addView(statusView, LinearLayout.LayoutParams(-1, 0, 1f))

        input = EditText(this).apply {
            hint = "Или напиши сообщение…"
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF8092A8.toInt())
            textSize = 15f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Color.rgb(31, 43, 60), 14)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setSingleLine(false)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendQuestion(text?.toString().orEmpty())
                    true
                } else false
            }
        }
        page.addView(input, LinearLayout.LayoutParams(-1, dp(54)))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val voice = Button(this).apply {
            text = "🎙 Сказать ещё раз"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(33, 108, 169), 14)
            setOnClickListener { beginVoiceInput() }
        }
        sendButton = Button(this).apply {
            text = "Отправить"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(38, 91, 153), 14)
            setOnClickListener { sendQuestion(input?.text?.toString().orEmpty()) }
        }
        buttons.addView(voice, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(5) })
        buttons.addView(sendButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(5) })
        page.addView(buttons)

        page.addView(TextView(this).apply {
            text = "Ответ появится в маленьком окошке поверх приложений и прозвучит голосом."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFF8092A8.toInt())
            setPadding(0, dp(15), 0, 0)
        })
        setContentView(page)
    }

    private fun beginVoiceInput() {
        if (waitingForSpeech) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status("Голосовой сервис не найден. Напиши вопрос в поле ниже.")
            showBubble("Бадди: Не нашёл голосовой ввод. Напиши мне вопрос снизу.")
            return
        }
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
            return
        }
        launchSpeechRecognizer()
    }

    private fun launchSpeechRecognizer() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Бадди слушает. Скажи свой вопрос.")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            waitingForSpeech = true
            status("Говори. Я слушаю!")
            showBubble("Бадди: Слушаю тебя! Говори.")
            startActivityForResult(intent, REQUEST_RECOGNIZER)
        } catch (e: Exception) {
            waitingForSpeech = false
            status("Не удалось открыть голосовой ввод. Напиши вопрос ниже.")
            showBubble("Бадди: Микрофон сейчас не открылся. Можешь написать вопрос.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchSpeechRecognizer()
            } else {
                status("Доступ к микрофону не разрешён. Можно написать вопрос.")
                showBubble("Бадди: Разреши микрофон или напиши мне вопрос.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_RECOGNIZER) return
        waitingForSpeech = false
        val spoken = if (resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (!spoken.isNullOrBlank()) {
            input?.setText(spoken)
            sendQuestion(spoken)
        } else {
            status("Я не расслышал. Попробуй ещё раз или напиши вопрос.")
            showBubble("Бадди: Кажется, я не расслышал. Скажи ещё раз!")
        }
    }

    private fun sendQuestion(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isBlank() || listening) return
        input?.setText("")
        listening = true
        sendButton?.isEnabled = false
        status("Хм… дай мне секунду подумать.")
        showBubble("Бадди: Хм, сейчас разберусь…")
        history.add("user" to question)

        val key = SecretBox.load(this)
        if (key.isBlank()) {
            val answer = AssistantLogic.localReply(question)
            history.add("assistant" to answer)
            finishAnswer(answer)
            return
        }

        val prefs = getSharedPreferences("island_buddy_settings", MODE_PRIVATE)
        val endpoint = prefs.getString(
            "endpoint", "https://openrouter.ai/api/v1/chat/completions"
        ) ?: "https://openrouter.ai/api/v1/chat/completions"
        val model = prefs.getString("model", "openrouter/auto") ?: "openrouter/auto"
        AiClient.ask(endpoint, model, key, history.toList()) { answer, error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val result = answer ?: "Упс, не получилось связаться с нейросетью: ${error ?: "проверь интернет и настройки подключения"}"
                if (answer != null) history.add("assistant" to answer)
                finishAnswer(result)
            }
        }
    }

    private fun finishAnswer(answer: String) {
        listening = false
        sendButton?.isEnabled = true
        status("Бадди: $answer")
        showBubble("Бадди: $answer")
        speak(answer)
    }

    private fun showBubble(message: String) {
        try {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return
            val intent = Intent(this, IslandOverlayService::class.java).apply {
                action = IslandOverlayService.ACTION_SHOW_REPLY
                putExtra(IslandOverlayService.EXTRA_REPLY, message)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } catch (_: Exception) {
            // The assistant screen still displays the response if an overlay cannot be shown.
        }
    }

    private fun initTts() {
        if (tts != null) return
        try {
            tts = TextToSpeech(this) { result ->
                runOnUiThread {
                    if (result == TextToSpeech.SUCCESS) {
                        val language = tts?.setLanguage(Locale("ru", "RU")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                        ttsReady = language >= TextToSpeech.LANG_AVAILABLE
                        if (ttsReady) {
                            tts?.setSpeechRate(1.0f)
                            tts?.setPitch(1.03f)
                            val waiting = pendingSpeech
                            pendingSpeech = null
                            if (!waiting.isNullOrBlank()) speakNow(waiting)
                        } else {
                            status("Текст готов, но русский голос TTS не установлен на телефоне.")
                        }
                    } else {
                        ttsReady = false
                        status("Текст готов, но служба озвучки Android недоступна.")
                    }
                }
            }
        } catch (_: Exception) {
            ttsReady = false
            status("Озвучка Android сейчас недоступна. Ответ останется на экране.")
        }
    }

    private fun speak(text: String) {
        if (tts == null) initTts()
        if (ttsReady) speakNow(text) else pendingSpeech = text
    }

    private fun speakNow(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-answer-${System.currentTimeMillis()}")
        } catch (_: Exception) {
            status("Ответ показан текстом; голосовая озвучка не запустилась.")
        }
    }

    private fun status(message: String) {
        statusView?.text = message
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        super.onDestroy()
    }
}