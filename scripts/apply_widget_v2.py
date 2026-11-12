from pathlib import Path
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path('Verity-main')
JAVA = ROOT / 'app/src/main/java/dev/veriti/app'
SERVICE = JAVA / 'service/FloatingAssistantService.kt'
MODELS = JAVA / 'data/Models.kt'
STORE = JAVA / 'data/ChatStore.kt'
MAIN = JAVA / 'MainActivity.kt'
RES = ROOT / 'app/src/main/res/drawable-nodpi'

def make_original_emotion_cutout(source_name: str, target_name: str):
    source = ROOT / 'app/src/main/res/drawable' / source_name
    target = RES / target_name
    if target.exists():
        return
    image = Image.open(source).convert('RGB')
    pixels = np.asarray(image, dtype=np.int16)
    red, green, blue = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
    yellow = (red > 145) & (green > 125) & (blue < 175) & ((red - blue) > 48) & ((green - blue) > 35)
    ys, xs = np.where(yellow)
    if len(xs) < 1000:
        raise SystemExit(f'Could not isolate original yellow face in {source_name}')
    center_x = (int(xs.min()) + int(xs.max())) / 2.0
    center_y = (int(ys.min()) + int(ys.max())) / 2.0
    diameter = max(int(xs.max()) - int(xs.min()) + 1, int(ys.max()) - int(ys.min()) + 1) + 14
    left = int(round(center_x - diameter / 2))
    top = int(round(center_y - diameter / 2))
    crop = Image.new('RGB', (diameter, diameter), (0, 0, 0))
    sx0, sy0 = max(0, left), max(0, top)
    sx1, sy1 = min(image.width, left + diameter), min(image.height, top + diameter)
    crop.paste(image.crop((sx0, sy0, sx1, sy1)), (sx0 - left, sy0 - top))
    side = 1536
    crop = crop.resize((side, side), Image.Resampling.LANCZOS)
    crop = crop.filter(ImageFilter.UnsharpMask(radius=1.0, percent=90, threshold=3))
    hi = side * 3
    mask = Image.new('L', (hi, hi), 0)
    ImageDraw.Draw(mask).ellipse((7, 7, hi - 7, hi - 7), fill=255)
    mask = mask.resize((side, side), Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(0.35))
    crop.putalpha(mask)
    RES.mkdir(parents=True, exist_ok=True)
    crop.save(target, format='PNG', optimize=True)

make_original_emotion_cutout('verity_sad.jpg', 'verity_sad_cutout.png')
make_original_emotion_cutout('verity_very_sad.jpg', 'verity_very_sad_cutout.png')
make_original_emotion_cutout('verity_very_sad_talking.jpg', 'verity_very_sad_talking_cutout.png')

def replace_once(path: Path, old: str, new: str, label: str):
    content = path.read_text(encoding='utf-8')
    count = content.count(old)
    if count == 0 and new in content:
        return
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match; found {count}')
    path.write_text(content.replace(old, new, 1), encoding='utf-8')

replace_once(MODELS, 'val voiceStyle: String = "off"', 'val voiceStyle: String = "natural"', 'default voice style')
replace_once(MODELS, 'val overlaySizeDp: Int = 96', 'val overlaySizeDp: Int = 76', 'default widget size')
replace_once(MODELS, 'val overlayControlsVisible: Boolean = true', 'val overlayControlsVisible: Boolean = false', 'default widget controls')
replace_once(MODELS, 'val overlayChatButtonVisible: Boolean = true', 'val overlayChatButtonVisible: Boolean = false', 'default widget chat button')
replace_once(MODELS, 'val overlayHideButtonVisible: Boolean = true', 'val overlayHideButtonVisible: Boolean = false', 'default widget hide button')
replace_once(STORE, 'voiceStyle = prefs.getString("voiceStyle", "off") ?: "off"', 'voiceStyle = prefs.getString("voiceStyle", "natural") ?: "natural"', 'stored default voice')
replace_once(STORE, 'overlaySizeDp = prefs.getInt("overlaySizeDp", 96)', 'overlaySizeDp = prefs.getInt("overlaySizeDp", 76)', 'stored default widget size')
replace_once(STORE, 'overlayControlsVisible = prefs.getBoolean("overlayControlsVisible", true)', 'overlayControlsVisible = prefs.getBoolean("overlayControlsVisible", false)', 'stored default controls')
replace_once(STORE, 'overlayChatButtonVisible = prefs.getBoolean("overlayChatButtonVisible", true)', 'overlayChatButtonVisible = prefs.getBoolean("overlayChatButtonVisible", false)', 'stored default chat button')
replace_once(STORE, 'overlayHideButtonVisible = prefs.getBoolean("overlayHideButtonVisible", true)', 'overlayHideButtonVisible = prefs.getBoolean("overlayHideButtonVisible", false)', 'stored default hide button')
replace_once(MAIN,
    'listOf("off" to "Текст", "bright" to "Высокий", "deep" to "Низкий", "fast" to "Быстрый")',
    'listOf("natural" to "Естественный", "bright" to "Высокий", "deep" to "Низкий", "fast" to "Быстрый", "off" to "Без голоса")',
    'voice style choices')

halo_file = JAVA / 'service/WidgetHaloView.kt'
halo_file.write_text(r'''package dev.veriti.app.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.min

internal class WidgetHaloView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        color = Color.rgb(255, 211, 45)
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * density
        color = 0x99FFF2A6.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height).toFloat()
        if (side <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = side * 0.49f
        glowPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00FFD43B, 0x44FFD43B, 0x55F4B91D, 0x00D99000),
            floatArrayOf(0f, 0.58f, 0.80f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, glowPaint)
        glowPaint.shader = null
        ringPaint.color = 0xE6FFD442.toInt()
        canvas.drawCircle(cx, cy, radius * 0.78f, ringPaint)
        innerRingPaint.alpha = 165
        canvas.drawCircle(cx, cy, radius * 0.70f, innerRingPaint)
    }
}
''', encoding='utf-8')

service = SERVICE.read_text(encoding='utf-8')
for import_line, after_line in [
    ('import android.speech.tts.UtteranceProgressListener\n', 'import android.speech.RecognizerIntent\n'),
    ('import android.os.SystemClock\n', 'import android.os.Looper\n'),
    ('import android.widget.FrameLayout\n', 'import android.widget.Button\n'),
]:
    if import_line.strip() not in service:
        if after_line not in service:
            raise SystemExit(f'Import anchor missing: {after_line.strip()}')
        service = service.replace(after_line, after_line + import_line, 1)

field_anchor = '    private var textShakeAnimator: ObjectAnimator? = null\n'
field_add = '''    private var textShakeAnimator: ObjectAnimator? = null
    private lateinit var widgetStatusChip: TextView
    private lateinit var characterFrame: FrameLayout
    private var haloView: WidgetHaloView? = null
    private var haloPulseAnimator: ObjectAnimator? = null
    private var idleFloatAnimator: ObjectAnimator? = null
    private var currentFaceMood: String = "happy"
    private var currentFaceFallback: Int = R.drawable.verity_happy_cutout
    private var mouthOpen: Boolean = false
    private var ttsSpeaking: Boolean = false
    private var spokenText: String = ""
    private var lastMouthRangeAt: Long = 0L
    private var mouthCloseRunnable: Runnable? = null

    private val mouthFallbackRunnable = object : Runnable {
        override fun run() {
            if (!ttsSpeaking) return
            if (SystemClock.uptimeMillis() - lastMouthRangeAt > 650L) {
                setMouthOpen(!mouthOpen)
            }
            handler.postDelayed(this, 125L)
        }
    }

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (::characterFrame.isInitialized && !listening && !ttsSpeaking) {
                characterFrame.animate().cancel()
                characterFrame.animate().scaleY(0.12f).setDuration(65L).withEndAction {
                    characterFrame.animate().scaleY(1f).setDuration(90L).start()
                }.start()
            }
            handler.postDelayed(this, Random.nextLong(3_200L, 6_200L))
        }
    }
'''
if field_anchor in service:
    service = service.replace(field_anchor, field_add, 1)
elif 'private lateinit var widgetStatusChip: TextView' not in service:
    raise SystemExit('Service field anchor not found')

service = service.replace('''        showBubble()
        scheduleMysteryFace()''', '''        showBubble()
        startWidgetIdleAnimations()
        scheduleMysteryFace()''', 1)

service = service.replace('''        speechText = TextView(this).apply {
            setTextColor(Color.rgb(33, 28, 38))
            textSize = 15f
            maxLines = 6
            setLineSpacing(0f, 1.08f)
            setOnClickListener {
                AssistantBridge.focusComposerRequested = true
                openApp()
            }
        }''', '''        speechText = TextView(this).apply {
            setTextColor(Color.rgb(248, 241, 214))
            textSize = 15f
            maxLines = 7
            setLineSpacing(dp(2).toFloat(), 1.08f)
            setOnClickListener {
                AssistantBridge.focusComposerRequested = true
                openApp()
            }
        }
        val widgetHeader = TextView(this).apply {
            text = "VERITY  //  WIDGET  //  V2"
            setTextColor(Color.rgb(255, 211, 62))
            textSize = 9f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER_VERTICAL
        }
        widgetStatusChip = TextView(this).apply {
            text = "ГОТОВ"
            setTextColor(Color.rgb(255, 218, 77))
            textSize = 9f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(11).toFloat()
                setColor(0x221D1605)
                setStroke(dp(1), Color.rgb(166, 126, 23))
            }
        }
        val widgetHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(widgetHeader, LinearLayout.LayoutParams(0, dp(28), 1f))
            addView(widgetStatusChip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(28)))
        }''', 1)

service = service.replace('''        moreButton = Button(this).apply {
            text = "Ещё"
            isAllCaps = false
            setOnClickListener { showNextSpeechPage() }
        }
        val closeButton = Button(this).apply {
            text = "Закрыть"
            isAllCaps = false
            setOnClickListener {
                closeSpeech()
                AssistantBridge.pendingNewChat = true
                startNewOverlayChat = true
                overlayChatId = null
            }
        }''', '''        moreButton = Button(this).apply {
            text = "Ещё"
            isAllCaps = false
            setTextColor(Color.rgb(255, 220, 91))
            background = GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setColor(0xFF17130A.toInt())
                setStroke(dp(1), Color.rgb(160, 119, 20))
            }
            setOnClickListener { showNextSpeechPage() }
        }
        val closeButton = Button(this).apply {
            text = "Закрыть"
            isAllCaps = false
            setTextColor(Color.rgb(255, 220, 91))
            background = GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setColor(0xFF17130A.toInt())
                setStroke(dp(1), Color.rgb(160, 119, 20))
            }
            setOnClickListener {
                closeSpeech()
                AssistantBridge.pendingNewChat = true
                startNewOverlayChat = true
                overlayChatId = null
            }
        }''', 1)

service = service.replace('''            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(Color.rgb(250, 246, 255))
                setStroke(dp(1), Color.rgb(211, 199, 226))
            }
            elevation = dp(8).toFloat()
            visibility = View.GONE
            addView(speechText, LinearLayout.LayoutParams(dp(218), LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(actions, LinearLayout.LayoutParams(dp(218), LinearLayout.LayoutParams.WRAP_CONTENT))''', '''            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(
                Color.rgb(8, 8, 9), Color.rgb(18, 15, 8), Color.rgb(7, 7, 8)
            )).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(179, 137, 28))
            }
            elevation = dp(10).toFloat()
            visibility = View.GONE
            addView(widgetHeaderRow, LinearLayout.LayoutParams(dp(218), dp(30)))
            addView(speechText, LinearLayout.LayoutParams(dp(218), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            })
            addView(actions, LinearLayout.LayoutParams(dp(218), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(7)
            })''', 1)

service = service.replace('''        val characterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(character, LinearLayout.LayoutParams(dp(actualCharacterSizeDp), dp(actualCharacterSizeDp)))
        }''', '''        val characterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        characterFrame = FrameLayout(this).apply {
            haloView = WidgetHaloView(this@FloatingAssistantService)
            addView(haloView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            addView(character, FrameLayout.LayoutParams(
                dp((actualCharacterSizeDp - 12).coerceAtLeast(44)),
                dp((actualCharacterSizeDp - 12).coerceAtLeast(44)),
                Gravity.CENTER
            ))
            elevation = dp(14).toFloat()
        }
        characterRow.addView(characterFrame, LinearLayout.LayoutParams(
            dp(actualCharacterSizeDp), dp(actualCharacterSizeDp)
        ))''', 1)
service = service.replace('''        character.setOnTouchListener(DragTouchListener(params, overlayRoot))''', '''        characterFrame.setOnTouchListener(DragTouchListener(params, overlayRoot))''', 1)

service = service.replace('''        speechPages = paginate(text)
        speechPage = 0
        speechText.text = speechPages.firstOrNull().orEmpty()''', '''        speechPages = paginate(text)
        speechPage = 0
        speechText.text = speechPages.firstOrNull().orEmpty()
        updateWidgetStatus(if (requestInFlight) "ДУМАЕТ" else "ГОТОВ")''', 1)
service = service.replace('''                "sad", "sleepy" -> R.drawable.verity_sad_talking_cutout''', '''                "sad", "sleepy" -> R.drawable.verity_sad_cutout''', 1)
service = service.replace('''    private fun closeSpeech() {
        speechCard.visibility = View.GONE
        speechPages = emptyList()
        speechPage = 0
        textToSpeech?.stop()
        textShakeAnimator?.cancel()
        speechText.translationX = 0f
        setCharacterImage(R.drawable.verity_happy_cutout)
    }''', '''    private fun closeSpeech() {
        speechCard.visibility = View.GONE
        speechPages = emptyList()
        speechPage = 0
        textToSpeech?.stop()
        stopSpeakingAnimation()
        textShakeAnimator?.cancel()
        speechText.translationX = 0f
        setCharacterImage(R.drawable.verity_happy_cutout, "happy")
    }''', 1)

service = service.replace('''                override fun onRmsChanged(rmsdB: Float) {
                    val scale = (1f + rmsdB.coerceAtLeast(0f) / 60f).coerceAtMost(1.18f)
                    character.scaleX = scale; character.scaleY = scale
                }''', '''                override fun onRmsChanged(rmsdB: Float) {
                    val level = rmsdB.coerceAtLeast(0f)
                    val scale = (1f + level / 70f).coerceAtMost(1.15f)
                    character.scaleX = scale; character.scaleY = scale
                    setMouthOpen(level > 3.2f)
                }''', 1)
service = service.replace('''    private fun setListeningVisual(value: Boolean) {
        listening = value
        character.alpha = if (value) .78f else 1f
        setCharacterImage(if (value) R.drawable.verity_sad_talking_cutout else R.drawable.verity_happy_cutout)
        if (!value) { character.scaleX = 1f; character.scaleY = 1f }
    }''', '''    private fun setListeningVisual(value: Boolean) {
        listening = value
        character.alpha = 1f
        if (value) {
            speechText.text = "Я слушаю тебя…"
            speechCard.visibility = View.VISIBLE
            moreButton.visibility = View.INVISIBLE
            updateWidgetStatus("СЛУШАЕТ")
            setCharacterImage(R.drawable.verity_happy_cutout, "happy")
        } else {
            setMouthOpen(false)
            if (!requestInFlight) updateWidgetStatus("ГОТОВ")
            character.scaleX = 1f
            character.scaleY = 1f
        }
    }''', 1)

start = service.index('    private fun setCharacterImage(')
end = service.index('    private fun updateTextShake()', start)
new_face = r'''    private fun setCharacterImage(fallback: Int, moodOverride: String? = null) {
        currentFaceFallback = fallback
        currentFaceMood = moodOverride ?: when (fallback) {
            R.drawable.verity_angry_cutout -> "angry"
            R.drawable.verity_sad_cutout, R.drawable.verity_sad_talking_cutout -> "sad"
            R.drawable.verity_normal_cutout -> "normal"
            R.drawable.verity_ouch_cutout -> "ouch"
            else -> "happy"
        }
        renderCharacterFaces()
    }

    private fun setMouthOpen(value: Boolean) {
        if (mouthOpen == value) return
        mouthOpen = value
        renderCharacterFaces()
    }

    private fun renderCharacterFaces() {
        if (!::character.isInitialized) return
        val settings = chatStore.loadSettings()
        val mode = settings.characterDisplayMode
        val avatar = if (mode == "avatar") SkinSheet.loadAvatar(this, settings.customAvatarUri) else null
        val displayMood = if (mouthOpen) "talking" else currentFaceMood
        val displayFallback = if (mouthOpen) R.drawable.verity_sad_talking_cutout else currentFaceFallback

        fun stockFace(mood: String, fallback: Int): Int {
            return when (mood) {
                "happy", "love", "laughing" -> R.drawable.verity_happy_cutout
                "angry" -> R.drawable.verity_angry_cutout
                "normal", "curious" -> R.drawable.verity_normal_cutout
                "talking" -> R.drawable.verity_sad_talking_cutout
                "scary" -> R.drawable.verity_very_sad_talking_cutout
                "ouch", "surprised" -> R.drawable.verity_ouch_cutout
                "sad" -> R.drawable.verity_sad_cutout
                "sleepy" -> R.drawable.verity_very_sad_cutout
                else -> fallback
            }
        }

        fun applyTo(target: ImageView, slot: Int, mood: String) {
            val asset = if (mode == "catalog") {
                dev.veriti.app.data.CharacterSlotAssets.get(settings, slot)
                    .ifBlank { settings.selectedCharacterAsset }
            } else ""
            val catalog = if (asset.isNotBlank()) SkinSheet.loadCatalogSprite(this, asset, mood) else null
            val alternate = slot > 0 && slot % 2 == 0 && settings.secondarySkinStyle != "none"
            val chosen = if (alternate) settings.copy(
                verityPlus = true,
                skinStyle = settings.secondarySkinStyle
            ) else settings
            when {
                slot == 0 && avatar != null -> {
                    val rounded = RoundedBitmapDrawableFactory.create(resources, avatar).apply {
                        isCircular = true
                        setAntiAlias(true)
                    }
                    target.setImageDrawable(rounded)
                }
                catalog != null -> target.setImageBitmap(catalog)
                mode == "skins" && chosen.skinStyle == "verity" -> target.setImageResource(
                    stockFace(mood, if (slot == 0) displayFallback else R.drawable.verity_happy_cutout)
                )
                else -> {
                    val skin = SkinSheet.faceForMood(this, chosen, mood)
                    if (skin != null) target.setImageBitmap(skin)
                    else target.setImageResource(if (slot == 0) displayFallback else R.drawable.verity_happy_cutout)
                }
            }
        }

        applyTo(character, 0, displayMood)
        extraCharacters.forEachIndexed { index, extra ->
            val moods = listOf("normal", "angry", "sad", "talking", "ouch", "scary", "happy", "curious", "sleepy", "laughing", "surprised", "love")
            applyTo(extra, index + 1, moods[index % moods.size])
        }
    }

'''
service = service[:start] + new_face + service[end:]

start = service.index('    private fun prepareTextToSpeech()')
end = service.index('    private fun refreshOverlaySettings()', start)
new_tts = r'''    private fun prepareTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                val selectedLanguage = chatStore.loadSettings().language
                val languageTag = when (selectedLanguage) {
                    "", "ru" -> "ru-RU"
                    "auto" -> Locale.getDefault().toLanguageTag()
                    else -> selectedLanguage
                }
                val locale = Locale.forLanguageTag(languageTag)
                val availability = textToSpeech?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val chosenLocale = if (availability >= TextToSpeech.LANG_AVAILABLE) locale else Locale.forLanguageTag("ru-RU")
                textToSpeech?.language = chosenLocale
                val installedVoice = textToSpeech?.voices
                    ?.filter { it.locale.language == chosenLocale.language && !it.isNetworkConnectionRequired }
                    ?.maxByOrNull { it.quality }
                if (installedVoice != null) textToSpeech?.voice = installedVoice
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId != "verity_reply") return
                        handler.post {
                            ttsSpeaking = true
                            lastMouthRangeAt = SystemClock.uptimeMillis()
                            updateWidgetStatus("ГОВОРИТ")
                            setMouthOpen(true)
                            handler.removeCallbacks(mouthFallbackRunnable)
                            handler.postDelayed(mouthFallbackRunnable, 150L)
                        }
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        if (utteranceId != "verity_reply") return
                        handler.post {
                            if (!ttsSpeaking) return@post
                            lastMouthRangeAt = SystemClock.uptimeMillis()
                            val safeStart = start.coerceIn(0, spokenText.length)
                            val safeEnd = end.coerceIn(safeStart, spokenText.length)
                            val segment = spokenText.substring(safeStart, safeEnd).lowercase(Locale.ROOT)
                            val hasVowel = segment.any { it in "аеёиоуыэюяaeiouy" }
                            setMouthOpen(hasVowel)
                            mouthCloseRunnable?.let(handler::removeCallbacks)
                            if (hasVowel) {
                                val openFor = (95L + segment.length * 22L).coerceIn(125L, 285L)
                                mouthCloseRunnable = Runnable { setMouthOpen(false) }
                                handler.postDelayed(mouthCloseRunnable!!, openFor)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "verity_reply") handler.post { stopSpeakingAnimation() }
                    }

                    @Deprecated("Deprecated by Android")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == "verity_reply") handler.post { stopSpeakingAnimation() }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId == "verity_reply") handler.post { stopSpeakingAnimation() }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        if (utteranceId == "verity_reply") handler.post { stopSpeakingAnimation() }
                    }
                })
            }
        }
    }

    private fun speakIfEnabled(text: String) {
        val settings = chatStore.loadSettings()
        if (!ttsReady || settings.voiceStyle == "off") return
        val (pitch, rate) = when (settings.voiceStyle) {
            "bright" -> 1.16f to 1.02f
            "deep" -> .90f to .94f
            "fast" -> 1.02f to 1.18f
            else -> 1.0f to .96f
        }
        spokenText = text
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(rate)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "verity_reply")
    }

    private fun stopSpeakingAnimation() {
        ttsSpeaking = false
        handler.removeCallbacks(mouthFallbackRunnable)
        mouthCloseRunnable?.let(handler::removeCallbacks)
        mouthCloseRunnable = null
        setMouthOpen(false)
        if (::widgetStatusChip.isInitialized && speechCard.visibility == View.VISIBLE && !listening) {
            updateWidgetStatus("ГОТОВ")
        }
    }

    private fun updateWidgetStatus(value: String) {
        if (!::widgetStatusChip.isInitialized) return
        widgetStatusChip.text = value
        val color = when (value) {
            "ГОВОРИТ" -> Color.rgb(255, 225, 93)
            "СЛУШАЕТ" -> Color.rgb(255, 190, 63)
            "ДУМАЕТ" -> Color.rgb(235, 201, 111)
            else -> Color.rgb(255, 218, 77)
        }
        widgetStatusChip.setTextColor(color)
    }

    private fun startWidgetIdleAnimations() {
        haloView?.let { halo ->
            haloPulseAnimator = ObjectAnimator.ofFloat(halo, View.ALPHA, 0.68f, 1f).apply {
                duration = 1_650L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
        if (::characterFrame.isInitialized) {
            idleFloatAnimator = ObjectAnimator.ofFloat(
                characterFrame, View.TRANSLATION_Y, -dp(2).toFloat(), dp(2).toFloat()
            ).apply {
                duration = 2_400L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
        handler.removeCallbacks(blinkRunnable)
        handler.postDelayed(blinkRunnable, Random.nextLong(2_800L, 5_000L))
    }

'''
service = service[:start] + new_tts + service[end:]

service = service.replace('''        handler.removeCallbacks(wanderRunnable)
        handler.removeCallbacks(resetFaceRunnable)''', '''        handler.removeCallbacks(wanderRunnable)
        handler.removeCallbacks(blinkRunnable)
        handler.removeCallbacks(mouthFallbackRunnable)
        mouthCloseRunnable?.let(handler::removeCallbacks)
        haloPulseAnimator?.cancel()
        idleFloatAnimator?.cancel()
        handler.removeCallbacks(resetFaceRunnable)''', 1)
service = service.replace('''        textToSpeech?.stop()
        textToSpeech?.shutdown()''', '''        textToSpeech?.stop()
        stopSpeakingAnimation()
        textToSpeech?.shutdown()''', 1)

SERVICE.write_text(service, encoding='utf-8')
print('Verity Widget V2 patch applied.')
