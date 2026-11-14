package com.verity.islandbuddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("island_buddy_settings", MODE_PRIVATE) }
    private var islandWidth = 178
    private var islandHeight = 48
    private var verticalOffset = 0
    private var faceStyle = 0
    private var preview: PhonePreview? = null
    private var assistantMode = false
    private var transcript: TextView? = null
    private var chatInput: EditText? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val history = mutableListOf<Pair<String, String>>()
    private var endpointField: EditText? = null
    private var modelField: EditText? = null
    private var keyField: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()
        window.statusBarColor = Color.rgb(10, 14, 22)
        window.navigationBarColor = Color.rgb(10, 14, 22)
        assistantMode = intent?.action == ACTION_ASSIST
        if (assistantMode) showAssistant() else showSettings()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == ACTION_ASSIST) {
            assistantMode = true
            showAssistant()
        }
    }

    private fun loadSettings() {
        islandWidth = prefs.getInt("width", 178)
        islandHeight = prefs.getInt("height", 48)
        verticalOffset = prefs.getInt("offset", 0)
        faceStyle = prefs.getInt("style", 0)
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt("width", islandWidth)
            .putInt("height", islandHeight)
            .putInt("offset", verticalOffset)
            .putInt("style", faceStyle)
            .apply()
    }

    private fun showSettings() {
        assistantMode = false
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            background = rounded(Color.rgb(10, 14, 22), 0)
        }
        val scroll = ScrollView(this).apply { addView(page) }
        page.addView(label("ISLAND BUDDY", 25f, Color.WHITE, true))
        page.addView(label("Живой островок вокруг фронтальной камеры", 14f, 0xFF9CAFC6.toInt()))
        page.addView(gap(14))

        val previewCard = card()
        previewCard.addView(label("ПРЕДПРОСМОТР HONOR X8b", 12f, 0xFF8FA6C2.toInt(), true))
        preview = PhonePreview(this).apply {
            islandWidthDp = islandWidth
            islandHeightDp = islandHeight
            verticalOffsetDp = verticalOffset
            faceStyle = this@MainActivity.faceStyle
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(dp(220), dp(410)).apply {
            gravity = android.view.Gravity.CENTER
            topMargin = dp(12)
        })
        page.addView(previewCard)

        val sizeCard = card()
        sizeCard.addView(label("РАЗМЕР И ПОЛОЖЕНИЕ", 13f, Color.WHITE, true))
        sizeCard.addView(label("Ширина островка: $islandWidth dp", 13f, 0xFFD6E0EC.toInt()))
        val widthLabel = sizeCard.getChildAt(sizeCard.childCount - 1) as TextView
        sizeCard.addView(slider(islandWidth - 110, 140) { value ->
            islandWidth = value + 110
            widthLabel.text = "Ширина островка: $islandWidth dp"
            refreshPreview()
        })
        sizeCard.addView(label("Высота островка: $islandHeight dp", 13f, 0xFFD6E0EC.toInt()))
        val heightLabel = sizeCard.getChildAt(sizeCard.childCount - 1) as TextView
        sizeCard.addView(slider(islandHeight - 34, 46) { value ->
            islandHeight = value + 34
            heightLabel.text = "Высота островка: $islandHeight dp"
            refreshPreview()
        })
        sizeCard.addView(label("Отступ сверху: $verticalOffset dp", 13f, 0xFFD6E0EC.toInt()))
        val offsetLabel = sizeCard.getChildAt(sizeCard.childCount - 1) as TextView
        sizeCard.addView(slider(verticalOffset, 48) { value ->
            verticalOffset = value
            offsetLabel.text = "Отступ сверху: $verticalOffset dp"
            refreshPreview()
        })
        page.addView(sizeCard)

        val faceCard = card()
        faceCard.addView(label("ВНЕШНИЙ ВИД", 13f, Color.WHITE, true))
        val faces = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        faces.addView(faceButton("Робот", 0), weightParams())
        faces.addView(faceButton("Кот", 1), weightParams())
        faces.addView(faceButton("Пришелец", 2), weightParams())
        faceCard.addView(faces)
        page.addView(faceCard)

        val aiCard = card()
        aiCard.addView(label("ГОЛОСОВОЙ ИИ", 13f, Color.WHITE, true))
        aiCard.addView(label(
            "Речь распознаёт Android, ответ озвучивает системный голос. Для ответов нейросети подключи совместимый API. Текст сообщений будет отправляться выбранному API-провайдеру.",
            13f, 0xFFB7C6D8.toInt()
        ))
        val endpoint = editField("HTTPS-адрес API", prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT)
        val model = editField("Название модели", prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini")
        val key = editField("API-ключ (не отправляй его в чат)", SecretBox.load(this)).apply {
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        endpointField = endpoint
        modelField = model
        keyField = key
        aiCard.addView(endpoint)
        aiCard.addView(model)
        aiCard.addView(key)
        aiCard.addView(label(
            "Ключ шифруется Android Keystore на телефоне. При пустом ключе остаётся локальный демонстрационный режим.",
            11f, 0xFF899CB5.toInt()
        ))
        aiCard.addView(button("Сохранить настройки ИИ") {
            val endpointValue = endpoint.text?.toString()?.trim().orEmpty()
            val modelValue = model.text?.toString()?.trim().orEmpty()
            val keyValue = key.text?.toString()?.trim().orEmpty()
            if (!endpointValue.startsWith("https://", true)) {
                toast("Адрес API должен начинаться с https://")
            } else {
                prefs.edit().putString("endpoint", endpointValue).putString("model", modelValue).apply()
                try {
                    SecretBox.save(this, keyValue)
                    toast("Настройки ИИ сохранены")
                } catch (e: Exception) {
                    toast("Не удалось защитить ключ: ${e.message}")
                }
            }
        })
        page.addView(aiCard)

        val actions = card()
        actions.addView(label("ЗАПУСК", 13f, Color.WHITE, true))
        actions.addView(button("Включить островок поверх приложений") { enableIsland() })
        actions.addView(button("Открыть голосового помощника") { showAssistant() })
        actions.addView(button("Выключить островок") { stopIsland() })
        actions.addView(label(
            "Android попросит разрешить показ поверх других приложений. Островок можно перетаскивать пальцем. Микрофон включается только после нажатия кнопки голосового ввода.",
            11f, 0xFF899CB5.toInt()
        ))
        page.addView(actions)
        page.addView(label("Версия 1.0 • Android 8–14 • портретный режим", 11f, 0xFF71839A.toInt()))
        setContentView(scroll)
    }

    private fun refreshPreview() {
        preview?.islandWidthDp = islandWidth
        preview?.islandHeightDp = islandHeight
        preview?.verticalOffsetDp = verticalOffset
        preview?.faceStyle = faceStyle
        preview?.invalidate()
    }

    private fun faceButton(title: String, style: Int): Button =
        button(title) {
            faceStyle = style
            refreshPreview()
            saveSettings()
        }.apply {
            textSize = 11f
            isAllCaps = false
        }

    private fun enableIsland() {
        saveSettings()
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    REQUEST_OVERLAY
                )
            } catch (_: Exception) {
                toast("Открой настройки Android и разреши показ поверх приложений для Island Buddy")
            }
            return
        }
        startIslandService()
    }

    private fun startIslandService() {
        val intent = Intent(this, IslandOverlayService::class.java).apply {
            action = IslandOverlayService.ACTION_START
            putExtra(IslandOverlayService.EXTRA_WIDTH, islandWidth)
            putExtra(IslandOverlayService.EXTRA_HEIGHT, islandHeight)
            putExtra(IslandOverlayService.EXTRA_OFFSET, verticalOffset)
            putExtra(IslandOverlayService.EXTRA_STYLE, faceStyle)
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            toast("Островок включён. Перетащи его пальцем, чтобы подвинуть.")
        } catch (e: Exception) {
            toast("Не удалось включить островок: ${e.message}")
        }
    }

    private fun stopIsland() {
        val intent = Intent(this, IslandOverlayService::class.java).apply {
            action = IslandOverlayService.ACTION_STOP
        }
        try { startService(intent) } catch (_: Exception) { stopService(intent) }
        toast("Островок выключен")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) startIslandService()
            else toast("Без этого разрешения островок не сможет отображаться поверх приложений")
        } else if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                chatInput?.setText(spoken)
                sendQuestion(spoken)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchSpeechRecognizer()
            } else toast("Для голосового ввода нужен доступ к микрофону")
        }
    }

    private fun showAssistant() {
        assistantMode = true
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = rounded(Color.rgb(10, 14, 22), 0)
        }
        page.addView(label("ISLAND BUDDY", 24f, Color.WHITE, true))
        page.addView(label("Нажми на микрофон и задай вопрос", 13f, 0xFF9CAFC6.toInt()))
        page.addView(gap(12))

        val historyScroll = ScrollView(this).apply {
            background = rounded(Color.rgb(20, 28, 41), 18)
        }
        transcript = TextView(this).apply {
            setTextColor(0xFFE6EEF8.toInt())
            textSize = 15f
            setPadding(dp(14), dp(14), dp(14), dp(14))
            text = "Island Buddy: Привет! Я готов слушать, когда ты нажмёшь кнопку."
        }
        historyScroll.addView(transcript)
        page.addView(historyScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val input = editField("Напиши сообщение…", "")
        chatInput = input
        input.imeOptions = EditorInfo.IME_ACTION_SEND
        input.setSingleLine(false)
        page.addView(input)

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(button("🎙 Голос") { beginVoiceInput() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        controls.addView(button("Отправить") {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) sendQuestion(text)
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        page.addView(controls)
        page.addView(button("← Настройки островка") { showSettings() })
        setContentView(page)
        initTts()
    }

    private fun beginVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("На телефоне не найден системный сервис распознавания речи. Можно написать сообщение вручную.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else launchSpeechRecognizer()
    }

    private fun launchSpeechRecognizer() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи, что нужно помощнику")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            startActivityForResult(intent, REQUEST_SPEECH)
        } catch (_: Exception) {
            toast("Системный голосовой ввод сейчас недоступен")
        }
    }

    private fun sendQuestion(question: String) {
        chatInput?.setText("")
        addMessage("Ты", question)
        history.add("user" to question)
        val key = SecretBox.load(this)
        if (key.isBlank()) {
            val answer = AssistantLogic.localReply(question)
            history.add("assistant" to answer)
            addMessage("Island Buddy", answer)
            speak(answer)
            return
        }
        addMessage("Island Buddy", "Думаю…")
        val endpoint = prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
        val model = prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
        AiClient.ask(endpoint, model, key, history.toList()) { answer, error ->
            runOnUiThread {
                val result = answer ?: "Не получилось получить ответ от нейросети: ${error ?: "ошибка соединения"}"
                if (answer != null) history.add("assistant" to answer)
                replaceThinking(result)
                speak(result)
            }
        }
    }

    private fun addMessage(who: String, message: String) {
        val view = transcript ?: return
        view.append("\n\n$who: $message")
        view.post { (view.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun replaceThinking(answer: String) {
        val view = transcript ?: return
        val current = view.text.toString()
        val index = current.lastIndexOf("\n\nIsland Buddy: Думаю…")
        if (index >= 0) view.text = current.substring(0, index) + "\n\nIsland Buddy: $answer"
        else addMessage("Island Buddy", answer)
    }

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                val result = tts?.setLanguage(Locale("ru"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    private fun speak(text: String) {
        if (tts == null) initTts()
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "island_buddy_answer")
    }

    private fun editField(hintText: String, value: String): EditText =
        EditText(this).apply {
            hint = hintText
            setText(value)
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF71839A.toInt())
            textSize = 14f
            setPadding(dp(13), dp(8), dp(13), dp(8))
            background = rounded(Color.rgb(25, 35, 51), 12)
            layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        }

    private fun slider(progress: Int, maxValue: Int, changed: (Int) -> Unit): SeekBar =
        SeekBar(this).apply {
            max = maxValue
            this.progress = progress.coerceIn(0, maxValue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    changed(value)
                    saveSettings()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

    private fun button(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(38, 91, 153), 14)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(5)
                bottomMargin = dp(5)
            }
            setOnClickListener { action() }
        }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(Color.rgb(19, 27, 40), 18)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        }

    private fun gap(height: Int): View =
        View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

    private fun weightParams() = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
        marginEnd = dp(4)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onBackPressed() {
        if (assistantMode) showSettings() else super.onBackPressed()
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_ASSIST = "com.verity.islandbuddy.OPEN_ASSISTANT"
        private const val REQUEST_OVERLAY = 601
        private const val REQUEST_AUDIO = 602
        private const val REQUEST_SPEECH = 603
        private const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    }
}
