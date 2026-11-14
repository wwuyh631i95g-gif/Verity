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
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("island_buddy_settings", MODE_PRIVATE) }

    private var islandWidth = 178
    private var islandHeight = 48
    private var horizontalOffset = 0
    private var verticalOffset = 0
    private var faceStyle = 0
    private var eyeColor = Color.rgb(60, 207, 255)
    private var animationsEnabled = true
    private var animationMode = 0
    private var accessory = 0

    private var preview: PhonePreview? = null
    private var geometryLabel: TextView? = null
    private var assistantMode = false
    private var transcript: TextView? = null
    private var chatInput: EditText? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val history = mutableListOf<Pair<String, String>>()

    companion object {
        const val ACTION_ASSIST = "com.verity.islandbuddy.OPEN_ASSISTANT"
        private const val REQUEST_OVERLAY = 601
        private const val REQUEST_AUDIO = 602
        private const val REQUEST_SPEECH = 603
        private const val DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val DEFAULT_MODEL = "openrouter/auto"

        private val animationNames = arrayOf(
            "Спокойное моргание",
            "Частое моргание",
            "Сонные глаза",
            "Удивлённые глаза",
            "Смотрит по сторонам",
            "Подмигивание"
        )
        private val colors = listOf(
            "Голубые" to Color.rgb(60, 207, 255),
            "Зелёные" to Color.rgb(80, 245, 150),
            "Фиолетовые" to Color.rgb(184, 125, 255),
            "Золотые" to Color.rgb(255, 190, 75),
            "Розовые" to Color.rgb(255, 125, 190),
            "Красные" to Color.rgb(255, 90, 105),
            "Белые" to Color.rgb(240, 248, 255)
        )
        private val accessories = listOf(
            "Нет" to 0,
            "Очки" to 1,
            "Корона" to 2,
            "Антенна" to 3,
            "Румянец" to 4
        )
    }

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
        horizontalOffset = prefs.getInt("x", 0)
        verticalOffset = prefs.getInt("offset", 0)
        faceStyle = prefs.getInt("style", 0)
        eyeColor = prefs.getInt("eye_color", Color.rgb(60, 207, 255))
        animationsEnabled = prefs.getBoolean("animations", true)
        animationMode = prefs.getInt("animation_mode", 0).coerceIn(0, animationNames.lastIndex)
        accessory = prefs.getInt("accessory", 0).coerceIn(0, accessories.lastIndex)
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt("width", islandWidth)
            .putInt("height", islandHeight)
            .putInt("x", horizontalOffset)
            .putInt("offset", verticalOffset)
            .putInt("style", faceStyle)
            .putInt("eye_color", eyeColor)
            .putBoolean("animations", animationsEnabled)
            .putInt("animation_mode", animationMode)
            .putInt("accessory", accessory)
            .apply()
    }

    private fun showSettings() {
        assistantMode = false
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            background = rounded(Color.rgb(10, 14, 22), 0)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(label("ISLAND BUDDY  2.2", 25f, Color.WHITE, true))
        page.addView(label("Камера-островок с живыми глазами и голосовым ИИ", 13f, 0xFF9CAFC6.toInt()))

        val previewCard = card()
        previewCard.addView(label("НАСТРОЙКА ПАЛЬЦЕМ", 13f, Color.WHITE, true))
        previewCard.addView(label(
            "Перетаскивай сам островок, чтобы менять положение. Потяни за круглые точки вокруг него, чтобы растягивать или сжимать.",
            12f, 0xFFB7C6D8.toInt()
        ))
        preview = PhonePreview(this).apply {
            islandWidthDp = this@MainActivity.islandWidth
            islandHeightDp = this@MainActivity.islandHeight
            horizontalOffsetDp = this@MainActivity.horizontalOffset
            verticalOffsetDp = this@MainActivity.verticalOffset
            faceStyle = this@MainActivity.faceStyle
            eyeColor = this@MainActivity.eyeColor
            animationsEnabled = this@MainActivity.animationsEnabled
            animationMode = this@MainActivity.animationMode
            accessory = this@MainActivity.accessory
            onGeometryChanged = { w, h, x, y ->
                islandWidth = w
                islandHeight = h
                horizontalOffset = x
                verticalOffset = y
                saveSettings()
                updateGeometryLabel()
                updateIslandIfRunning()
            }
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(dp(184), dp(407)).apply {
            gravity = Gravity.CENTER
            topMargin = dp(9)
            bottomMargin = dp(4)
        })
        geometryLabel = label("", 12f, 0xFFD6E0EC.toInt())
        previewCard.addView(geometryLabel)
        page.addView(previewCard)
        updateGeometryLabel()

        val appearanceCard = card()
        appearanceCard.addView(label("ВНЕШНИЙ ВИД И ГЛАЗА", 13f, Color.WHITE, true))
        val faceRow = optionRow()
        faceRow.addView(smallButton("Робот") { faceStyle = 0; refreshAppearance() }, optionParams())
        faceRow.addView(smallButton("Кот") { faceStyle = 1; refreshAppearance() }, optionParams())
        faceRow.addView(smallButton("Пришелец") { faceStyle = 2; refreshAppearance() }, optionParams())
        appearanceCard.addView(faceRow)

        val animationSwitch = Switch(this).apply {
            text = "Анимации глаз"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = animationsEnabled
            setOnCheckedChangeListener { _, checked ->
                animationsEnabled = checked
                refreshAppearance()
            }
        }
        appearanceCard.addView(animationSwitch)
        appearanceCard.addView(button("Эффект: ${animationNames[animationMode]}  ↻") {
            animationMode = (animationMode + 1) % animationNames.size
            refreshAppearance()
            toast("Анимация: ${animationNames[animationMode]}")
        })

        appearanceCard.addView(label("ЦВЕТ ГЛАЗ", 12f, 0xFFB7C6D8.toInt(), true))
        val colorRow1 = optionRow()
        colors.take(4).forEach { item ->
            colorRow1.addView(smallButton("● ${item.first}") {
                eyeColor = item.second
                refreshAppearance()
            }, optionParams())
        }
        appearanceCard.addView(colorRow1)
        val colorRow2 = optionRow()
        colors.drop(4).forEach { item ->
            colorRow2.addView(smallButton("● ${item.first}") {
                eyeColor = item.second
                refreshAppearance()
            }, optionParams())
        }
        appearanceCard.addView(colorRow2)

        appearanceCard.addView(label("АКСЕССУАРЫ", 12f, 0xFFB7C6D8.toInt(), true))
        val accessoryRow1 = optionRow()
        accessories.take(3).forEach { item ->
            accessoryRow1.addView(smallButton(item.first) {
                accessory = item.second
                refreshAppearance()
            }, optionParams())
        }
        appearanceCard.addView(accessoryRow1)
        val accessoryRow2 = optionRow()
        accessories.drop(3).forEach { item ->
            accessoryRow2.addView(smallButton(item.first) {
                accessory = item.second
                refreshAppearance()
            }, optionParams())
        }
        appearanceCard.addView(accessoryRow2)
        page.addView(appearanceCard)

        val aiCard = card()
        aiCard.addView(label("ГОЛОСОВОЙ ИИ", 13f, Color.WHITE, true))
        aiCard.addView(label(
            "Островок открывает чат. Скажи вопрос голосом или напиши его: ответ появится белым сообщением снизу экрана и будет озвучен. Для настоящих ответов нейросети добавь свой API-ключ.",
            12f, 0xFFB7C6D8.toInt()
        ))
        val endpoint = editField("HTTPS-адрес API", prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT)
        val model = editField("Модель", prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
        val key = editField("Личный API-ключ", SecretBox.load(this)).apply {
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        aiCard.addView(endpoint)
        aiCard.addView(model)
        aiCard.addView(key)
        aiCard.addView(label(
            "Ключ хранится на телефоне в зашифрованном виде. Не вставляй его в чат и не публикуй.",
            11f, 0xFF899CB5.toInt()
        ))
        aiCard.addView(button("Сохранить настройки ИИ") {
            val endpointValue = endpoint.text?.toString()?.trim().orEmpty()
            val modelValue = model.text?.toString()?.trim().orEmpty()
            val keyValue = key.text?.toString()?.trim().orEmpty()
            if (!endpointValue.startsWith("https://", true)) {
                toast("Адрес API должен начинаться с https://")
            } else if (modelValue.isBlank()) {
                toast("Укажи название модели")
            } else {
                prefs.edit().putString("endpoint", endpointValue).putString("model", modelValue).apply()
                try {
                    SecretBox.save(this, keyValue)
                    toast("Настройки ИИ сохранены")
                } catch (e: Exception) {
                    toast("Не удалось сохранить ключ: ${e.message}")
                }
            }
        })
        aiCard.addView(button("Проверить подключение к нейросети") {
            val endpointValue = endpoint.text?.toString()?.trim().orEmpty()
            val modelValue = model.text?.toString()?.trim().orEmpty()
            val keyValue = key.text?.toString()?.trim().orEmpty()
            if (!endpointValue.startsWith("https://", true)) {
                toast("Сначала укажи HTTPS-адрес API")
            } else if (keyValue.isBlank()) {
                toast("Сначала добавь свой API-ключ")
            } else {
                toast("Проверяю подключение…")
                AiClient.ask(endpointValue, modelValue, keyValue, listOf("user" to "Ответь одним коротким словом: подключено.")) { answer, error ->
                    runOnUiThread {
                        if (answer != null) toast("Нейросеть ответила: ${answer.take(100)}")
                        else toast("Проверка не прошла: ${error ?: "ошибка"}")
                    }
                }
            }
        })
        page.addView(aiCard)

        val actions = card()
        actions.addView(label("ЗАПУСК", 13f, Color.WHITE, true))
        actions.addView(button("Включить островок поверх приложений") { enableIsland() })
        actions.addView(button("Открыть голосового помощника") { startActivity(Intent(this, AssistantActivity::class.java)) })
        actions.addView(button("Выключить островок") { stopIsland() })
        page.addView(actions)
        page.addView(label("Island Buddy 2.2 • Android 8–14 • Honor X8b", 11f, 0xFF71839A.toInt()))
        setContentView(scroll)
    }

    private fun updateGeometryLabel() {
        geometryLabel?.text = "Размер: ${islandWidth} × ${islandHeight} dp   •   Смещение: X ${horizontalOffset}, Y ${verticalOffset}"
    }

    private fun refreshAppearance() {
        preview?.apply {
            faceStyle = this@MainActivity.faceStyle
            eyeColor = this@MainActivity.eyeColor
            animationsEnabled = this@MainActivity.animationsEnabled
            animationMode = this@MainActivity.animationMode
            accessory = this@MainActivity.accessory
            invalidate()
        }
        saveSettings()
        updateIslandIfRunning()
    }

    private fun updateIslandIfRunning() {
        if (!prefs.getBoolean("overlay_enabled", false) || !Settings.canDrawOverlays(this)) return
        try {
            startService(islandIntent(IslandOverlayService.ACTION_START))
        } catch (_: Exception) {
            toast("Не удалось обновить островок. Открой его заново.")
        }
    }

    private fun islandIntent(action: String): Intent =
        Intent(this, IslandOverlayService::class.java).apply {
            this.action = action
            putExtra(IslandOverlayService.EXTRA_WIDTH, islandWidth)
            putExtra(IslandOverlayService.EXTRA_HEIGHT, islandHeight)
            putExtra(IslandOverlayService.EXTRA_X, horizontalOffset)
            putExtra(IslandOverlayService.EXTRA_Y, verticalOffset)
            putExtra(IslandOverlayService.EXTRA_STYLE, faceStyle)
            putExtra(IslandOverlayService.EXTRA_EYE_COLOR, eyeColor)
            putExtra(IslandOverlayService.EXTRA_ANIMATIONS, animationsEnabled)
            putExtra(IslandOverlayService.EXTRA_ANIMATION_MODE, animationMode)
            putExtra(IslandOverlayService.EXTRA_ACCESSORY, accessory)
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
                toast("Открой настройки Android и разреши показ поверх других приложений для Island Buddy")
            }
            return
        }
        startIslandService()
    }

    private fun startIslandService() {
        prefs.edit().putBoolean("overlay_enabled", true).apply()
        try {
            val intent = islandIntent(IslandOverlayService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            toast("Островок включён. Его можно перетаскивать прямо на экране.")
        } catch (e: Exception) {
            prefs.edit().putBoolean("overlay_enabled", false).apply()
            toast("Не удалось включить островок: ${e.message}")
        }
    }

    private fun stopIsland() {
        prefs.edit().putBoolean("overlay_enabled", false).apply()
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
            setPadding(dp(16), dp(16), dp(16), dp(14))
            background = rounded(Color.rgb(10, 14, 22), 0)
        }
        page.addView(label("ISLAND BUDDY", 24f, Color.WHITE, true))
        page.addView(label("Нажми на микрофон и задай вопрос", 13f, 0xFF9CAFC6.toInt()))

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
            showOverlayReply(answer)
            speak(answer)
            return
        }

        addMessage("Island Buddy", "Думаю…")
        val endpoint = prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
        val model = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        AiClient.ask(endpoint, model, key, history.toList()) { answer, error ->
            runOnUiThread {
                val result = answer ?: "Не получилось получить ответ от нейросети: ${error ?: "ошибка соединения"}"
                if (answer != null) history.add("assistant" to answer)
                replaceThinking(result)
                showOverlayReply(result)
                speak(result)
            }
        }
    }

    private fun showOverlayReply(message: String) {
        if (!prefs.getBoolean("overlay_enabled", false) || !Settings.canDrawOverlays(this)) return
        try {
            val intent = Intent(this, IslandOverlayService::class.java).apply {
                action = IslandOverlayService.ACTION_SHOW_REPLY
                putExtra(IslandOverlayService.EXTRA_REPLY, message)
            }
            startService(intent)
        } catch (_: Exception) {
            // The response remains visible in the app's chat even if Android blocks the overlay bubble.
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
                val languageResult = tts?.setLanguage(Locale("ru"))
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.setLanguage(Locale.getDefault())
                }
                val waiting = pendingSpeech
                pendingSpeech = null
                if (!waiting.isNullOrBlank()) tts?.speak(waiting, TextToSpeech.QUEUE_FLUSH, null, "island_buddy_answer")
            }
        }
    }

    private fun speak(text: String) {
        if (tts == null) initTts()
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "island_buddy_answer")
        } else {
            pendingSpeech = text
        }
    }

    private fun optionRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun optionParams() = LinearLayout.LayoutParams(0, dp(43), 1f).apply {
        marginEnd = dp(4)
        topMargin = dp(3)
        bottomMargin = dp(3)
    }

    private fun smallButton(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 10f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(38, 75, 115), 11)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            setOnClickListener { action() }
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
                topMargin = dp(7)
                bottomMargin = dp(7)
            }
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
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(Color.rgb(19, 27, 40), 18)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(7)
                bottomMargin = dp(7)
            }
        }

    private fun gap(height: Int): View =
        View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

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
}
