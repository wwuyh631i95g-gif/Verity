package dev.ios18.simulator

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private data class IOS18App(
    val id: String,
    val title: String,
    val icon: String,
    val color: Color,
    val category: String = "Утилиты"
)

private data class IOSMiniApp(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val screenText: String,
    val buttonText: String
)

private val ios18Apps = listOf(
    IOS18App("messages", "Сообщения", "💬", Color(0xFF35C96F), "Общение"),
    IOS18App("calendar", "Календарь", "📅", Color(0xFFF5F5F5), "Продуктивность"),
    IOS18App("photos", "Фото", "🌈", Color(0xFFFF8A98), "Медиа"),
    IOS18App("camera", "Камера", "📷", Color(0xFF66717D), "Медиа"),
    IOS18App("weather", "Погода", "☀️", Color(0xFF55A7E8), "Информация"),
    IOS18App("clock", "Часы", "⏱️", Color(0xFF46566A), "Утилиты"),
    IOS18App("maps", "Карты", "🗺️", Color(0xFF65B991), "Информация"),
    IOS18App("home", "Дом", "🏠", Color(0xFFDB9B66), "Утилиты"),
    IOS18App("wallet", "Wallet", "💳", Color(0xFF536A9B), "Финансы"),
    IOS18App("notes", "Заметки", "📝", Color(0xFFFFD45D), "Продуктивность"),
    IOS18App("reminders", "Напоминания", "✅", Color(0xFF5BBF9D), "Продуктивность"),
    IOS18App("stocks", "Акции", "📈", Color(0xFF263C4A), "Информация"),
    IOS18App("books", "Книги", "📚", Color(0xFFB56C47), "Медиа"),
    IOS18App("appstore", "App Store", "🅰️", Color(0xFF4D9DE8), "Магазин"),
    IOS18App("podcasts", "Подкасты", "🎙️", Color(0xFF9C70D9), "Медиа"),
    IOS18App("tv", "TV", "📺", Color(0xFF25252E), "Медиа"),
    IOS18App("health", "Здоровье", "❤️", Color(0xFFE76E86), "Информация"),
    IOS18App("journal", "Дневник", "📔", Color(0xFFB77B55), "Продуктивность"),
    IOS18App("settings", "Настройки", "⚙️", Color(0xFF8B98A8), "Утилиты"),
    IOS18App("files", "Файлы", "📁", Color(0xFF65A9E2), "Утилиты"),
    IOS18App("safari", "Safari", "🧭", Color(0xFF4CA6E8), "Интернет"),
    IOS18App("music", "Музыка", "🎵", Color(0xFFE56E9C), "Медиа"),
    IOS18App("mail", "Почта", "✉️", Color(0xFF5799E7), "Общение"),
    IOS18App("phone", "Телефон", "📞", Color(0xFF37BC71), "Общение"),
    IOS18App("contacts", "Контакты", "👤", Color(0xFFAD91DA), "Общение"),
    IOS18App("calculator", "Калькулятор", "🧮", Color(0xFF687482), "Утилиты"),
    IOS18App("passwords", "Пароли", "🔑", Color(0xFF607A9A), "Утилиты"),
    IOS18App("gamecenter", "Game Center", "🎮", Color(0xFF7E6BCC), "Игры"),
    IOS18App("shareplay", "SharePlay", "▶️", Color(0xFF4B9DCA), "Общение"),
    IOS18App("shortcuts", "Команды", "🧩", Color(0xFF7771B8), "Утилиты"),
    IOS18App("translate", "Перевод", "🌐", Color(0xFF559BC7), "Утилиты"),
    IOS18App("appgallery", "Библиотека", "🗂️", Color(0xFF637A9A), "Утилиты"),
    IOS18App("siri", "Siri", "✨", Color(0xFF7668B9), "Утилиты"),
    IOS18App("focus", "Фокусирование", "🌙", Color(0xFF6677A6), "Утилиты")
)

private fun miniAppsToJson(apps: List<IOSMiniApp>): String {
    val array = JSONArray()
    apps.forEach { app ->
        array.put(JSONObject().put("id", app.id).put("title", app.title).put("icon", app.icon)
            .put("description", app.description).put("screenText", app.screenText).put("buttonText", app.buttonText))
    }
    return array.toString()
}

private fun parseMiniAppPackage(raw: String): IOSMiniApp {
    val root = JSONObject(raw)
    require(root.optString("format") == "ios18-miniapp") { "Это не пакет приложения iOS 18." }
    val app = root.getJSONObject("app")
    val title = app.getString("title").take(32)
    val icon = app.optString("icon", "📦").take(4)
    val id = app.optString("id", "mini_" + title.hashCode().toString()).take(60)
    return IOSMiniApp(
        id = id,
        title = title,
        icon = icon,
        description = app.optString("description", "Приложение сообщества").take(100),
        screenText = app.optString("screenText", "Добро пожаловать!").take(800),
        buttonText = app.optString("buttonText", "Нажать").take(24)
    )
}

private fun miniAppPackage(app: IOSMiniApp): String {
    val item = JSONObject()
        .put("id", app.id)
        .put("title", app.title)
        .put("icon", app.icon)
        .put("description", app.description)
        .put("screenText", app.screenText)
        .put("buttonText", app.buttonText)
    return JSONObject().put("format", "ios18-miniapp").put("version", 1).put("app", item).toString(2)
}

@Composable
fun IOS18Experience() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ios18_simulator", 0) }

    var page by remember { mutableStateOf("lock") }
    var currentApp by remember { mutableStateOf("settings") }
    var isDark by remember { mutableStateOf(false) }
    var wallpaper by remember { mutableIntStateOf(0) }
    var iconTint by remember { mutableIntStateOf(0) }
    var largeIcons by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(true) }
    var homeEditing by remember { mutableStateOf(false) }
    var islandExpanded by remember { mutableStateOf(false) }
    var controlGroup by remember { mutableIntStateOf(0) }
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(true) }
    var airplane by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    var flashlight by remember { mutableStateOf(false) }
    var lowPower by remember { mutableStateOf(false) }
    var gameMode by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.62f) }
    var brightness by remember { mutableFloatStateOf(0.82f) }
    var siriText by remember { mutableStateOf("") }
    var siriAnswer by remember { mutableStateOf("Привет! Я помогу открыть приложения и переключить настройки симулятора.") }
    var searchText by remember { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("Добро пожаловать в iOS 18!\n\nПопробуй Math Notes: напиши 12*8+5= внизу.") }
    var messageDraft by remember { mutableStateOf("") }
    var scheduledDraft by remember { mutableStateOf("") }
    var messageEffect by remember { mutableStateOf("Обычный") }
    var messageReaction by remember { mutableStateOf("❤️") }
    var safariAddress by remember { mutableStateOf("https://apple.com") }
    var readerMode by remember { mutableStateOf(false) }
    var distractionControl by remember { mutableStateOf(false) }
    var mailCategory by remember { mutableStateOf("Входящие") }
    var journalText by remember { mutableStateOf("") }
    var journalEntries by remember { mutableIntStateOf(0) }
    var reminderDraft by remember { mutableStateOf("") }
    val reminders = remember { mutableStateListOf("Попробовать новый Центр управления", "Создать своё мини-приложение") }
    val deletedReminders = remember { mutableStateListOf<String>() }
    val messages = remember { mutableStateListOf("Привет! Это новый чат iMessage.", "Пакет приложения можно передать другу.") }
    val photos = remember { mutableStateListOf("🌅", "🌊", "🌿", "🌌", "🏙️", "🐈") }
    val homeApps = remember { mutableStateListOf<String>().apply { addAll(ios18Apps.take(24).map { it.id }) } }
    val hiddenApps = remember { mutableStateListOf<String>() }
    val miniApps = remember { mutableStateListOf<IOSMiniApp>() }
    var miniAppsLoaded by remember { mutableStateOf(false) }
    var miniCreateTitle by remember { mutableStateOf("") }
    var miniCreateIcon by remember { mutableStateOf("🚀") }
    var miniCreateDescription by remember { mutableStateOf("Моё первое мини-приложение") }
    var miniCreateText by remember { mutableStateOf("Добро пожаловать в моё приложение!") }
    var miniCreateButton by remember { mutableStateOf("Запустить") }
    var miniCounter by remember { mutableIntStateOf(0) }
    var miniPendingExport by remember { mutableStateOf("") }
    var calculatorExpression by remember { mutableStateOf("12*8+5") }
    var calculatorResult by remember { mutableStateOf("101") }
    var phoneNumber by remember { mutableStateOf("") }
    var callStatus by remember { mutableStateOf("Демо-вызов: реальные звонки не выполняются.") }
    var passwordSearch by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var appPin by remember { mutableStateOf("1800") }
    var pinInput by remember { mutableStateOf("") }
    var pendingLockedApp by remember { mutableStateOf("") }
    var appsLocked by remember { mutableStateOf(false) }
    var lockScreenWidget by remember { mutableStateOf("Погода · 21° · Ясно") }
    var accessibilityScale by remember { mutableFloatStateOf(1f) }
    var reduceMotion by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var sharePlayNote by remember { mutableStateOf("Общая доска SharePlay") }
    var sharePlayDraft by remember { mutableStateOf("") }
    var appInstallNotice by remember { mutableStateOf("") }
    var appSearch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val array = JSONArray(prefs.getString("mini_apps", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                miniApps.add(IOSMiniApp(o.getString("id"), o.getString("title"), o.optString("icon", "📦"),
                    o.optString("description", ""), o.optString("screenText", ""), o.optString("buttonText", "Открыть")))
            }
        } catch (_: Exception) { }
        isDark = prefs.getBoolean("dark", false)
        wallpaper = prefs.getInt("wallpaper", 0)
        iconTint = prefs.getInt("iconTint", 0)
        miniAppsLoaded = true
    }

    fun persistMiniApps() {
        prefs.edit().putString("mini_apps", miniAppsToJson(miniApps)).apply()
    }

    fun openApp(id: String) {
        if (id == "siri") { page = "siri"; return }
        if (id == "appgallery") { page = "appgallery"; return }
        if (appsLocked && id in listOf("passwords", "wallet", "photos")) {
            pendingLockedApp = id
            pinInput = ""
            page = "app_lock"
            return
        }
        currentApp = id
        page = "app"
        islandExpanded = false
    }

    val wallpapers = listOf(
        listOf(Color(0xFF172A57), Color(0xFF7662A8), Color(0xFFE79BA9)),
        listOf(Color(0xFF0E3C55), Color(0xFF238C9D), Color(0xFFB5D8CA)),
        listOf(Color(0xFF251D42), Color(0xFF965B85), Color(0xFFF1B782)),
        listOf(Color(0xFF102B38), Color(0xFF486F87), Color(0xFFB7CEE4))
    )
    val background = wallpapers[wallpaper.coerceIn(0, wallpapers.lastIndex)]
    val cardColor = if (isDark) Color(0xE9232938) else Color(0xEAF9FBFF)
    val mainText = if (isDark) Color.White else Color(0xFF1C2C43)
    val secondaryText = if (isDark) Color(0xFFB8C5D8) else Color(0xFF5E6F84)
    val iconOverlay = when (iconTint) {
        1 -> Color(0xFF202431)
        2 -> Color(0xFF8C83C8)
        3 -> Color(0xFFB5D8CA)
        else -> Color.Transparent
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && miniPendingExport.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(miniPendingExport.toByteArray(Charsets.UTF_8)) }
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Поделиться мини-приложением"))
                appInstallNotice = "Пакет сохранён. Передай его другу через меню «Поделиться»."
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось сохранить пакет", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val imported = parseMiniAppPackage(raw)
                miniApps.removeAll { it.id == imported.id }
                miniApps.add(imported)
                persistMiniApps()
                appInstallNotice = "«" + imported.title + "» установлено в симулятор!"
                page = "appstore"
            } catch (e: Exception) {
                Toast.makeText(context, "Пакет не распознан. Нужен файл .iosapp.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(background))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
            Row(
                Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("9:41", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("▮▮  Wi-Fi  ▰", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(31.dp), contentAlignment = Alignment.TopCenter) {
                Row(
                    Modifier.width(if (islandExpanded) 260.dp else 112.dp)
                        .height(if (islandExpanded) 54.dp else 27.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.Black)
                        .clickable { islandExpanded = !islandExpanded }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (gameMode) "🎮" else "●", color = Color(0xFF64D5A0), fontSize = 12.sp)
                    if (islandExpanded) {
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (gameMode) "Игровой режим" else "Активность", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(if (gameMode) "Снижены фоновые процессы · демо" else "iOS 18 Simulator · активно", color = Color.LightGray, fontSize = 9.sp)
                        }
                        Text("•••", color = Color.White)
                    } else {
                        Spacer(Modifier.weight(1f))
                        Text("●  ●", color = Color(0xFF6E7584), fontSize = 8.sp)
                    }
                }
            }

            AnimatedVisibility(visible = appInstallNotice.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xE72E805F)).clickable { appInstallNotice = "" }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓  " + appInstallNotice, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("×", color = Color.White, fontSize = 19.sp)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = page, label = "ios18-page") { screen ->
                    when (screen) {
                        "lock" -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("СРЕДА · 9:41", color = Color.White.copy(alpha = 0.84f), fontSize = 12.sp, letterSpacing = 2.sp)
                            Text("9:41", color = Color.White, fontSize = 78.sp, fontWeight = FontWeight.Light)
                            Text("Добро пожаловать", color = Color.White, fontSize = 18.sp)
                            Spacer(Modifier.height(23.dp))
                            IOSGlassCard(cardColor) {
                                Text("ВИДЖЕТЫ", color = secondaryText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(lockScreenWidget, color = mainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text("Настраиваемый экран блокировки · iOS 18", color = secondaryText, fontSize = 10.sp)
                            }
                            IOSGlassCard(cardColor) {
                                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                    IOSQuickButton("🔦", "Фонарик") { flashlight = !flashlight }
                                    IOSQuickButton("📷", "Камера") { openApp("camera") }
                                    IOSQuickButton("⌕", "Поиск") { page = "search" }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { page = "home" },
                                shape = RoundedCornerShape(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF25385B))
                            ) { Text("Разблокировать  →", fontWeight = FontWeight.Bold) }
                            Text("Face ID · визуальная демонстрация", color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp, bottom = 17.dp))
                        }

                        "home" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 5.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("СРЕДА, 9:41", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text("Привет!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { homeEditing = !homeEditing }) { Text(if (homeEditing) "Готово" else "Изменить", color = Color.White) }
                                TextButton(onClick = { page = "search" }) { Text("⌕", color = Color.White, fontSize = 22.sp) }
                            }
                            IOSGlassCard(cardColor) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("ПОГОДА · ВИДЖЕТ", color = secondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("21°  ·  Ясно", color = mainText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                        Text("Календарь: 2 события сегодня", color = secondaryText, fontSize = 10.sp)
                                    }
                                    Text("☀️", fontSize = 42.sp)
                                }
                            }
                            homeApps.toList().chunked(4).forEachIndexed { rowIndex, row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEachIndexed { columnIndex, id ->
                                        val app = ios18Apps.firstOrNull { it.id == id }
                                        if (app != null && id !in hiddenApps) {
                                            IOSAppIcon(
                                                app = app,
                                                modifier = Modifier.weight(1f),
                                                tint = iconOverlay,
                                                large = largeIcons,
                                                showLabel = showLabels,
                                                editing = homeEditing,
                                                onClick = {
                                                    if (homeEditing) {
                                                        val index = homeApps.indexOf(id)
                                                        if (index >= 0 && index < homeApps.lastIndex) {
                                                            val temp = homeApps[index + 1]
                                                            homeApps[index + 1] = homeApps[index]
                                                            homeApps[index] = temp
                                                        }
                                                    } else openApp(id)
                                                },
                                                onHide = { hiddenApps.add(id); homeApps.remove(id) }
                                            )
                                        }
                                    }
                                    repeat((4 - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            miniApps.forEach { mini ->
                                IOSMiniAppTile(mini, Modifier.fillMaxWidth().padding(vertical = 3.dp), onClick = { openApp(mini.id) })
                            }
                            IOSGlassCard(Color(0xAA19243A)) {
                                Text("Советы iOS 18", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Иконки можно тонировать, приложения расставлять свободно, а быстрые переключатели менять в Центре управления.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Color(0xAA101827)).padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("phone", "messages", "safari", "music").forEach { id ->
                                    val app = ios18Apps.first { it.id == id }
                                    IOSAppIcon(app, Modifier.weight(1f), compact = true, tint = iconOverlay, onClick = { openApp(id) })
                                }
                            }
                        }

                        "control" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                            Text("Центр управления", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Группы, быстрые действия и ползунки", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Избранное", "Связь", "Дом").forEachIndexed { index, title ->
                                    FilterChip(selected = controlGroup == index, onClick = { controlGroup = index }, label = { Text(title, fontSize = 10.sp) })
                                }
                            }
                            IOSGlassCard(cardColor) {
                                when (controlGroup) {
                                    0 -> {
                                        Row {
                                            IOSToggle("Wi-Fi", "◉", wifi && !airplane, { wifi = !wifi }, Modifier.weight(1f))
                                            IOSToggle("Bluetooth", "ᛒ", bluetooth, { bluetooth = !bluetooth }, Modifier.weight(1f))
                                        }
                                        Row {
                                            IOSToggle("Фокус", "☾", focus, { focus = !focus }, Modifier.weight(1f))
                                            IOSToggle("Режим полёта", "✈", airplane, { airplane = !airplane }, Modifier.weight(1f))
                                        }
                                        Row {
                                            IOSToggle("Энергия", "🔋", lowPower, { lowPower = !lowPower }, Modifier.weight(1f))
                                            IOSToggle("Игры", "🎮", gameMode, { gameMode = !gameMode }, Modifier.weight(1f))
                                        }
                                    }
                                    1 -> {
                                        Row {
                                            IOSToggle("Сотовая сеть", "▥", !airplane, { airplane = !airplane }, Modifier.weight(1f))
                                            IOSToggle("Точка доступа", "◉", false, { appInstallNotice = "Точка доступа показана как демо." }, Modifier.weight(1f))
                                        }
                                        Row {
                                            IOSToggle("Bluetooth", "ᛒ", bluetooth, { bluetooth = !bluetooth }, Modifier.weight(1f))
                                            IOSToggle("AirDrop", "⇧", false, { appInstallNotice = "AirDrop визуализирован локально." }, Modifier.weight(1f))
                                        }
                                    }
                                    else -> {
                                        Row {
                                            IOSToggle("Дом", "⌂", true, { openApp("home") }, Modifier.weight(1f))
                                            IOSToggle("Фонарик", "🔦", flashlight, { flashlight = !flashlight }, Modifier.weight(1f))
                                        }
                                        Row {
                                            IOSToggle("Поворот", "▣", true, { appInstallNotice = "Блокировка поворота включена в демо." }, Modifier.weight(1f))
                                            IOSToggle("Экран", "☀", true, { page = "settings" }, Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            IOSGlassCard(cardColor) {
                                Text("Яркость", color = mainText, fontWeight = FontWeight.Bold)
                                Slider(value = brightness, onValueChange = { brightness = it })
                                Text("Громкость", color = mainText, fontWeight = FontWeight.Bold)
                                Slider(value = volume, onValueChange = { volume = it })
                            }
                            IOSGlassCard(cardColor) {
                                Text("Медиаплеер", color = mainText, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("♫  iOS Music · демо-трек", color = secondaryText, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { openApp("music") }) { Text("Открыть") }
                                }
                            }
                            Text("Коснись вкладок сверху, чтобы переключить набор быстрых элементов.", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp)
                        }

                        "search" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                            Text("Поиск", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = searchText, onValueChange = { searchText = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("Приложения, настройки и функции") }, singleLine = true)
                            ios18Apps.filter { it.title.contains(searchText, true) || it.category.contains(searchText, true) }.forEach { app ->
                                IOSGlassCard(cardColor) {
                                    Row(Modifier.fillMaxWidth().clickable { openApp(app.id) }, verticalAlignment = Alignment.CenterVertically) {
                                        Text(app.icon, fontSize = 27.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(app.title, color = mainText, fontWeight = FontWeight.SemiBold)
                                            Text(app.category, color = secondaryText, fontSize = 10.sp)
                                        }
                                        Text("›", color = secondaryText, fontSize = 22.sp)
                                    }
                                }
                            }
                            miniApps.filter { it.title.contains(searchText, true) }.forEach { mini ->
                                IOSMiniAppTile(mini, Modifier.fillMaxWidth(), onClick = { openApp(mini.id) })
                            }
                        }

                        "appgallery" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                            Text("Библиотека приложений", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Категории и быстрый поиск", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            OutlinedTextField(value = appSearch, onValueChange = { appSearch = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("Найти приложение") }, singleLine = true)
                            ios18Apps.filter { it.title.contains(appSearch, true) }.groupBy { it.category }.forEach { (category, apps) ->
                                IOSGlassCard(cardColor) {
                                    Text(category, color = mainText, fontWeight = FontWeight.Bold)
                                    apps.chunked(4).forEach { row ->
                                        Row {
                                            row.forEach { app -> IOSAppIcon(app, Modifier.weight(1f), compact = true, tint = iconOverlay, onClick = { openApp(app.id) }) }
                                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                            if (miniApps.isNotEmpty()) {
                                IOSGlassCard(cardColor) {
                                    Text("Твои приложения", color = mainText, fontWeight = FontWeight.Bold)
                                    miniApps.forEach { mini -> IOSMiniAppTile(mini, Modifier.fillMaxWidth(), onClick = { openApp(mini.id) }) }
                                }
                            }
                            TextButton(onClick = { page = "appstore" }) { Text("Открыть App Store →", color = Color.White) }
                        }

                        "siri" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(108.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color(0xFF69D5F4), Color(0xFFA27BE7), Color(0xFFEA87C1), Color(0xFF69D5F4)))), contentAlignment = Alignment.Center) {
                                Text("✨", fontSize = 53.sp)
                            }
                            Text("Siri", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                            Text(siriAnswer, color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                            OutlinedTextField(value = siriText, onValueChange = { siriText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Спросить Siri") })
                            Button(onClick = {
                                val q = siriText.lowercase()
                                when {
                                    "тём" in q || "темн" in q -> { isDark = !isDark; siriAnswer = "Переключила оформление." }
                                    "замет" in q -> { openApp("notes"); siriAnswer = "Открываю Заметки." }
                                    "камер" in q -> { openApp("camera"); siriAnswer = "Открываю Камеру." }
                                    "сообщ" in q -> { openApp("messages"); siriAnswer = "Открываю Сообщения." }
                                    "магаз" in q || "прилож" in q -> { page = "appstore"; siriAnswer = "Открываю App Store." }
                                    "центр" in q -> { page = "control"; siriAnswer = "Открываю Центр управления." }
                                    else -> siriAnswer = "Я могу открыть Камеру, Заметки, Сообщения, App Store или переключить тёмное оформление."
                                }
                            }, modifier = Modifier.padding(top = 10.dp)) { Text("Спросить") }
                            Text("Голосовой ввод не требуется: можно печатать запросы.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        "app_lock" -> Column(Modifier.fillMaxSize().padding(top = 35.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 53.sp)
                            Text("Приложение защищено", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                            Text("Введите локальный код симулятора", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                            OutlinedTextField(value = pinInput, onValueChange = { pinInput = it.take(8) }, label = { Text("Код") }, singleLine = true)
                            Button(onClick = {
                                if (pinInput == appPin) { currentApp = pendingLockedApp; page = "app"; pinInput = "" }
                                else Toast.makeText(context, "Код не подходит", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.padding(top = 10.dp)) { Text("Разблокировать") }
                            Text("Это локальная имитация блокировки, не Face ID.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        "app" -> {
                            val selected = ios18Apps.firstOrNull { it.id == currentApp }
                            val mini = miniApps.firstOrNull { it.id == currentApp }
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 5.dp, bottom = 12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { page = "home" }) { Text("‹ Домой", color = Color.White) }
                                    Spacer(Modifier.weight(1f))
                                    Text(selected?.title ?: mini?.title ?: "Приложение", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { page = "control" }) { Text("•••", color = Color.White) }
                                }

                                when (currentApp) {
                                    "settings" -> {
                                        Text("Настройки", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("iPhone · iOS 18", color = mainText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                            Text("Версия симулятора 18.1 · без системных разрешений", color = secondaryText, fontSize = 10.sp)
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Внешний вид", color = mainText, fontWeight = FontWeight.Bold)
                                            IOSSettingRow("Тёмное оформление", isDark) { isDark = !isDark; prefs.edit().putBoolean("dark", isDark).apply() }
                                            IOSSettingRow("Крупные иконки", largeIcons) { largeIcons = !largeIcons }
                                            IOSSettingRow("Подписи под иконками", showLabels) { showLabels = !showLabels }
                                            IOSSettingRow("Высокая контрастность", highContrast) { highContrast = !highContrast }
                                            IOSSettingRow("Уменьшение движения", reduceMotion) { reduceMotion = !reduceMotion }
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Цвет значков", color = mainText, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                                listOf("Оригинал", "Тёмный", "Фиолетовый", "Мятный").forEachIndexed { index, name ->
                                                    FilterChip(selected = iconTint == index, onClick = { iconTint = index; prefs.edit().putInt("iconTint", index).apply() }, label = { Text(name, fontSize = 9.sp) })
                                                }
                                            }
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Обои", color = mainText, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                wallpapers.forEachIndexed { index, colors ->
                                                    Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(colors)).clickable { wallpaper = index; prefs.edit().putInt("wallpaper", index).apply() })
                                                }
                                            }
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Экран блокировки", color = mainText, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = lockScreenWidget, onValueChange = { lockScreenWidget = it.take(60) }, label = { Text("Виджет на экране блокировки") }, modifier = Modifier.fillMaxWidth())
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Приватность и защита", color = mainText, fontWeight = FontWeight.Bold)
                                            IOSSettingRow("Локальная блокировка Паролей / Wallet / Фото", appsLocked) { appsLocked = !appsLocked }
                                            OutlinedTextField(value = appPin, onValueChange = { appPin = it.take(8) }, label = { Text("Код блокировки") }, singleLine = true)
                                            Text("Face ID не используется. Код действует только внутри симулятора.", color = secondaryText, fontSize = 10.sp)
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Универсальный доступ", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("Размер текста", color = secondaryText, fontSize = 11.sp)
                                            Slider(value = accessibilityScale, onValueChange = { accessibilityScale = it }, valueRange = 0.85f..1.25f)
                                            Text("Масштаб интерфейса: " + (accessibilityScale * 100).toInt() + "%", color = mainText, fontSize = 11.sp)
                                        }
                                    }

                                    "messages" -> {
                                        Text("Сообщения", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("Обычный", "Взрыв", "Пульс", "Большой").forEach { effect ->
                                                    FilterChip(selected = messageEffect == effect, onClick = { messageEffect = effect }, label = { Text(effect, fontSize = 9.sp) })
                                                }
                                            }
                                            Text("Эффект отправки: " + messageEffect, color = secondaryText, fontSize = 10.sp)
                                        }
                                        IOSGlassCard(cardColor) {
                                            messages.forEach { line ->
                                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(line, color = mainText, modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (line.startsWith("Ты:")) Color(0xFF3479D4) else Color(0xFF68788C)).padding(10.dp), fontSize = 12.sp)
                                                    TextButton(onClick = { messageReaction = when (messageReaction) { "❤️" -> "😂"; "😂" -> "👍"; else -> "❤️" } }) { Text(messageReaction) }
                                                }
                                            }
                                            OutlinedTextField(value = messageDraft, onValueChange = { messageDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("iMessage") })
                                            Button(onClick = { if (messageDraft.isNotBlank()) { messages.add("Ты: " + messageDraft + if (messageEffect != "Обычный") "  ✨" else ""); messageDraft = "" } }) { Text("Отправить") }
                                            OutlinedTextField(value = scheduledDraft, onValueChange = { scheduledDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сообщение для отправки позже") })
                                            TextButton(onClick = { if (scheduledDraft.isNotBlank()) { messages.add("Запланировано: " + scheduledDraft); scheduledDraft = "" } }) { Text("Запланировать отправку") }
                                            Text("Tapback: нажми эмодзи рядом с сообщением. Всё хранится локально.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "safari" -> {
                                        Text("Safari", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            OutlinedTextField(value = safariAddress, onValueChange = { safariAddress = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Адрес сайта") }, singleLine = true)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { appInstallNotice = "Адрес подготовлен: " + safariAddress }) { Text("Перейти") }
                                                OutlinedButton(onClick = { readerMode = !readerMode }) { Text(if (readerMode) "Обычный вид" else "Reader") }
                                            }
                                            IOSSettingRow("Контроль отвлекающих элементов", distractionControl) { distractionControl = !distractionControl }
                                            Text(if (readerMode) "РЕЖИМ ЧТЕНИЯ\n\nСтраница показывает только основной текст, заголовки и краткое содержание. Это демонстрация Safari Reader." else "Стартовая страница Safari\nИзбранное · Часто посещаемые · Список чтения", color = mainText, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                                            Text(if (distractionControl) "Отвлекающие блоки скрыты в макете." else "HTTPS Priority: защищённые адреса предпочтительнее в этом демонстрационном макете.", color = secondaryText, fontSize = 10.sp)
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Highlights", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("Ключевые сведения и краткое содержание страницы отображаются здесь.", color = secondaryText, fontSize = 11.sp)
                                        }
                                    }

                                    "appstore" -> {
                                        Text("App Store", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                        Text("Открой каталог, создай мини-приложение или установи пакет друга.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                        IOSGlassCard(cardColor) {
                                            Text("Создать приложение", color = mainText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            OutlinedTextField(value = miniCreateTitle, onValueChange = { miniCreateTitle = it.take(32) }, modifier = Modifier.fillMaxWidth(), label = { Text("Название") }, singleLine = true)
                                            OutlinedTextField(value = miniCreateIcon, onValueChange = { miniCreateIcon = it.take(4) }, label = { Text("Эмодзи-иконка") }, singleLine = true)
                                            OutlinedTextField(value = miniCreateDescription, onValueChange = { miniCreateDescription = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("Описание") })
                                            OutlinedTextField(value = miniCreateText, onValueChange = { miniCreateText = it.take(800) }, modifier = Modifier.fillMaxWidth(), label = { Text("Что увидят внутри приложения") })
                                            OutlinedTextField(value = miniCreateButton, onValueChange = { miniCreateButton = it.take(24) }, label = { Text("Текст кнопки") }, singleLine = true)
                                            Button(onClick = {
                                                if (miniCreateTitle.isBlank()) {
                                                    Toast.makeText(context, "Введи название приложения", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val app = IOSMiniApp("mini_" + System.currentTimeMillis().toString(), miniCreateTitle.trim(), miniCreateIcon.ifBlank { "📦" }, miniCreateDescription, miniCreateText, miniCreateButton.ifBlank { "Открыть" })
                                                    miniApps.add(app)
                                                    persistMiniApps()
                                                    miniCreateTitle = ""
                                                    appInstallNotice = "Твоё приложение добавлено на главный экран."
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text("＋ Создать и установить") }
                                            Text("Конструктор создаёт безопасные мини-приложения из блоков интерфейса, без запуска чужого кода.", color = secondaryText, fontSize = 10.sp)
                                        }
                                        IOSGlassCard(cardColor) {
                                            Text("Приложения сообщества", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("Импорт пакета .iosapp", color = secondaryText, fontSize = 11.sp)
                                            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("↓ Установить пакет друга") }
                                            Text("Выбери полученный файл .iosapp в приложении «Файлы». Он появится на главном экране симулятора.", color = secondaryText, fontSize = 10.sp)
                                        }
                                        if (miniApps.isNotEmpty()) {
                                            IOSGlassCard(cardColor) {
                                                Text("Мои приложения · " + miniApps.size, color = mainText, fontWeight = FontWeight.Bold)
                                                miniApps.forEach { mini ->
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(mini.icon, fontSize = 26.sp)
                                                        Column(Modifier.weight(1f).padding(start = 9.dp)) {
                                                            Text(mini.title, color = mainText, fontWeight = FontWeight.SemiBold)
                                                            Text(mini.description, color = secondaryText, fontSize = 9.sp, maxLines = 2)
                                                        }
                                                        TextButton(onClick = { miniPendingExport = miniAppPackage(mini); exportLauncher.launch(mini.title.replace(" ", "_") + ".iosapp") }) { Text("Поделиться") }
                                                        TextButton(onClick = { openApp(mini.id) }) { Text("Открыть") }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "photos" -> {
                                        Text("Фото", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Коллекции", color = mainText, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Недавние", "Избранное", "Поездки").forEach { TextButton(onClick = { appInstallNotice = "Открыта коллекция: " + it }) { Text(it, fontSize = 10.sp) } }
                                            }
                                            Text("Все фотографии · " + photos.size, color = secondaryText, fontSize = 11.sp)
                                        }
                                        photos.chunked(3).forEach { row ->
                                            Row {
                                                row.forEach { item ->
                                                    Box(Modifier.weight(1f).padding(3.dp).height(95.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(0xFF3D7397), Color(0xFFD79A9E)))), contentAlignment = Alignment.Center) {
                                                        Text(item, fontSize = 33.sp)
                                                    }
                                                }
                                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                        }
                                        Button(onClick = { photos.add(listOf("🌠", "🌻", "🏖️", "🌃")[photos.size % 4]) }) { Text("＋ Создать виртуальный снимок") }
                                        Text("Clean Up и поиск по описанию представлены как локальные действия-демо.", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                                    }

                                    "notes" -> {
                                        Text("Заметки", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            OutlinedTextField(value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp), label = { Text("Заметка") })
                                            Text("Math Notes · быстрый расчёт выражения", color = mainText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                                            OutlinedTextField(value = calculatorExpression, onValueChange = { calculatorExpression = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Например: 12*8+5") })
                                            Button(onClick = { calculatorResult = safeMath(calculatorExpression) }) { Text("Вычислить") }
                                            Text("Результат: " + calculatorResult, color = mainText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Цветные выделения и сворачиваемые разделы доступны в меню заметки (демо).", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "calculator" -> {
                                        Text("Калькулятор", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text(calculatorExpression, color = mainText, fontSize = 28.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                            Text(calculatorResult, color = secondaryText, fontSize = 38.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                            listOf("7","8","9","÷","4","5","6","×","1","2","3","−","0",".","=","+").chunked(4).forEach { row ->
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    row.forEach { key -> Button(onClick = {
                                                        when (key) {
                                                            "=" -> calculatorResult = safeMath(calculatorExpression)
                                                            else -> calculatorExpression = if (key == "÷") calculatorExpression + "/" else if (key == "×") calculatorExpression + "*" else if (key == "−") calculatorExpression + "-" else if (calculatorExpression == "12*8+5") key else calculatorExpression + key
                                                        }
                                                    }, modifier = Modifier.weight(1f).padding(2.dp)) { Text(key) } }
                                                }
                                            }
                                            Text("Math Notes: выражения вычисляются локально.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "reminders" -> {
                                        Text("Напоминания", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            OutlinedTextField(value = reminderDraft, onValueChange = { reminderDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Новое напоминание") })
                                            Button(onClick = { if (reminderDraft.isNotBlank()) { reminders.add(reminderDraft); reminderDraft = "" } }) { Text("Добавить") }
                                            reminders.forEachIndexed { index, reminder ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("○  " + reminder, color = mainText, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                                    TextButton(onClick = { deletedReminders.add(reminders.removeAt(index)) }) { Text("✓") }
                                                }
                                            }
                                            Text("Недавно удалённые · " + deletedReminders.size, color = secondaryText, fontWeight = FontWeight.Bold)
                                            deletedReminders.forEachIndexed { index, deleted ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(deleted, color = secondaryText, modifier = Modifier.weight(1f), fontSize = 10.sp)
                                                    TextButton(onClick = { reminders.add(deleted); deletedReminders.removeAt(index) }) { Text("Вернуть") }
                                                }
                                            }
                                        }
                                    }

                                    "journal" -> {
                                        Text("Дневник", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Твоя серия записей", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("✍️  " + journalEntries + " записей", color = mainText, fontSize = 23.sp)
                                            OutlinedTextField(value = journalText, onValueChange = { journalText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp), label = { Text("О чём хочешь записать сегодня?") })
                                            Button(onClick = { if (journalText.isNotBlank()) { journalEntries++; journalText = ""; appInstallNotice = "Запись сохранена локально." } }) { Text("Сохранить запись") }
                                            Text("Календарь, поиск и статистика дневника показаны в мини-режиме.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "passwords" -> {
                                        Text("Пароли", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            OutlinedTextField(value = passwordSearch, onValueChange = { passwordSearch = it }, label = { Text("Поиск элементов") }, modifier = Modifier.fillMaxWidth())
                                            listOf("Демо-сайт · demo@example.test", "Wi-Fi · Home Network", "Тестовая учётная запись").filter { it.contains(passwordSearch, true) }.forEach {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🔑  " + it, color = mainText, modifier = Modifier.weight(1f), fontSize = 11.sp)
                                                    TextButton(onClick = { passwordVisible = !passwordVisible }) { Text(if (passwordVisible) "Скрыть" else "Показать") }
                                                }
                                                if (passwordVisible) Text("Пример: ••••••••  (данные не настоящие)", color = secondaryText, fontSize = 10.sp)
                                            }
                                            Text("Хранилище содержит только примеры, не вводи сюда настоящие пароли.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "calendar" -> {
                                        Text("Календарь", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Ноябрь 2026", color = mainText, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                                            Text("ПН   ВТ   СР   ЧТ   ПТ   СБ   ВС", color = secondaryText, fontSize = 11.sp)
                                            (1..30).toList().chunked(7).forEach { row ->
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    row.forEach { day -> Text(day.toString(), color = if (day == 11) Color(0xFF398CE5) else mainText, modifier = Modifier.padding(5.dp), fontWeight = if (day == 11) FontWeight.Bold else FontWeight.Normal) }
                                                }
                                            }
                                            Text("Сегодня", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("09:00  Познакомиться с iOS 18\n18:00  Создать новое приложение", color = secondaryText, fontSize = 12.sp)
                                            Text("Напоминания показываются вместе с событиями · демо.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "phone" -> {
                                        Text("Телефон", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text(phoneNumber.ifBlank { "Введите номер" }, color = mainText, fontSize = 28.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                            (1..9).toList().chunked(3).forEach { row ->
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    row.forEach { digit -> TextButton(onClick = { phoneNumber += digit.toString() }) { Text(digit.toString(), color = mainText, fontSize = 22.sp) } }
                                                }
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                TextButton(onClick = { phoneNumber += "*" }) { Text("*", color = mainText) }
                                                TextButton(onClick = { phoneNumber += "0" }) { Text("0", color = mainText) }
                                                TextButton(onClick = { phoneNumber += "#" }) { Text("#", color = mainText) }
                                            }
                                            Button(onClick = { callStatus = "Вызов " + phoneNumber + " показан в демонстрационном режиме." }) { Text("Позвонить") }
                                            Text(callStatus, color = secondaryText, fontSize = 10.sp)
                                            Text("Недавние · поиск по имени и номеру · Live Voicemail (пример)", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "mail" -> {
                                        Text("Почта", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                listOf("Входящие", "Покупки", "Обновления").forEach { cat -> FilterChip(selected = mailCategory == cat, onClick = { mailCategory = cat }, label = { Text(cat, fontSize = 9.sp) }) }
                                            }
                                            Text(mailCategory, color = mainText, fontWeight = FontWeight.Bold)
                                            val lines = when (mailCategory) {
                                                "Покупки" -> listOf("Квитанция · тестовый магазин", "Заказ подтверждён · пример")
                                                "Обновления" -> listOf("Новости iOS Simulator", "Обновление приложений · демо")
                                                else -> listOf("Добро пожаловать в iOS 18", "Новое сообщение от друга")
                                            }
                                            lines.forEach { Text("✉  " + it, color = mainText, modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp) }
                                            Text("Категоризация писем и группировка отправителей показаны локально.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "maps" -> {
                                        Text("Карты", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFFDDE9D8)), contentAlignment = Alignment.Center) {
                                                Text("╱╲  ─────  ╱╲\n  ● ──── ●\n╲  │   ╱  │", color = Color(0xFF5B8E75), fontSize = 23.sp, textAlign = TextAlign.Center)
                                            }
                                            Text("Пеший маршрут · обзор тропы", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("Парк → озеро → смотровая площадка · 2,4 км · пример.", color = secondaryText, fontSize = 11.sp)
                                            Button(onClick = { appInstallNotice = "Маршрут добавлен в сохранённые." }) { Text("Сохранить маршрут") }
                                            Text("GPS и настоящая геолокация не используются.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "wallet" -> {
                                        Text("Wallet", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(Color(0xFF233C68)) {
                                            Text("VERITY PASS", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, letterSpacing = 2.sp)
                                            Text("Виртуальный пропуск", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                            Text("••••  ••••  ••••  1800", color = Color.White, fontSize = 16.sp)
                                            Text("DEMO · НЕ ПЛАТЁЖНАЯ КАРТА", color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp)
                                        }
                                        Text("Wallet-пассы и Tap to Cash представлены без банковских платежей.", color = Color.White, fontSize = 10.sp)
                                    }

                                    "music" -> {
                                        Text("Музыка", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF8D4F9C), Color(0xFFE18A9D)))), contentAlignment = Alignment.Center) { Text("♫", fontSize = 75.sp, color = Color.White) }
                                            Text("iOS Radio", color = mainText, fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                                            Text("Демо-трек · пространственное звучание", color = secondaryText, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                                            Button(onClick = { islandExpanded = true; appInstallNotice = "Музыка отображается в Dynamic Island." }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("▶  Воспроизвести") }
                                            Text("Music Haptics: тактильный ритм показан визуальной анимацией, без вибрации.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "camera" -> {
                                        Text("Камера", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(Color(0xFF151923)) {
                                            Box(Modifier.fillMaxWidth().height(235.dp).clip(RoundedCornerShape(20.dp)).background(Brush.radialGradient(listOf(Color(0xFF52677E), Color(0xFF10141D)))), contentAlignment = Alignment.Center) { Text("◉", color = Color.White, fontSize = 75.sp) }
                                            Button(onClick = { photos.add("📸"); appInstallNotice = "Виртуальный снимок добавлен в Фото." }) { Text("● Снять") }
                                            Text("Двухступенчатый затвор · фокус и экспозиция (демо).", color = Color(0xFFCBD4E2), fontSize = 10.sp)
                                        }
                                    }

                                    "clock" -> {
                                        Text("Часы", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("9:41", color = mainText, fontSize = 58.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                            Text("Будильник · мировые часы · секундомер · таймер", color = secondaryText, fontSize = 11.sp)
                                            Button(onClick = { appInstallNotice = "Таймер запущен в демонстрационном режиме." }) { Text("Запустить таймер на 5 минут") }
                                        }
                                    }

                                    "gamecenter" -> {
                                        Text("Game Center", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Достижения", color = mainText, fontWeight = FontWeight.Bold)
                                            listOf("Первый запуск · получено", "Коллекционер · 3 приложения", "Создатель · создай мини-приложение").forEach { Text("🏆  " + it, color = mainText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp)) }
                                            Text("Таблица лидеров · только локальный пример.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "shareplay" -> {
                                        Text("SharePlay", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text(sharePlayNote, color = mainText, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(value = sharePlayDraft, onValueChange = { sharePlayDraft = it }, label = { Text("Добавить на общую доску") }, modifier = Modifier.fillMaxWidth())
                                            Button(onClick = { if (sharePlayDraft.isNotBlank()) { sharePlayNote += "\n• " + sharePlayDraft; sharePlayDraft = "" } }) { Text("Добавить") }
                                            Text("Это локальная доска для демонстрации совместной активности.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "files" -> {
                                        Text("Файлы", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            listOf("На iPhone", "Загрузки", "Недавние", "iCloud Drive · демо", "Пакеты .iosapp").forEach { Text("📁  " + it, color = mainText, modifier = Modifier.padding(vertical = 9.dp)) }
                                            Text("Для установки приложения друга нажми App Store → Установить пакет друга.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "weather" -> {
                                        Text("Погода", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("☀️", color = mainText, fontSize = 58.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                            Text("21°", color = mainText, fontSize = 54.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                            Text("Ясно · демонстрационные данные", color = secondaryText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                            Text("ПН 21°   ВТ 19°   СР 20°   ЧТ 18°", color = mainText, modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
                                        }
                                    }

                                    "health" -> {
                                        Text("Здоровье", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Сводка · пример", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("❤️ Пульс: 72 · пример\n🚶 Шаги: 4 820 · пример\n😴 Сон: 7 ч 35 мин · пример", color = mainText, fontSize = 15.sp)
                                            Text("Показатели вымышлены, не медицинские данные.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "contacts" -> {
                                        Text("Контакты", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            listOf("Александр", "Мария", "Даниил", "iOS Support").forEach { Text("👤  " + it, color = mainText, modifier = Modifier.padding(vertical = 8.dp)) }
                                            Text("Локальные демонстрационные контакты.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "home" -> {
                                        Text("Дом", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Мой дом", color = mainText, fontWeight = FontWeight.Bold)
                                            IOSSettingRow("Освещение гостиной", flashlight) { flashlight = !flashlight }
                                            IOSSettingRow("Сценарий «Кино»", gameMode) { gameMode = !gameMode }
                                            Text("Управление аксессуарами HomeKit показано локально.", color = secondaryText, fontSize = 10.sp)
                                        }
                                    }

                                    "shortcuts" -> {
                                        Text("Команды", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            listOf("Открыть App Store", "Включить тёмное оформление", "Открыть Камеру").forEach { command ->
                                                TextButton(onClick = {
                                                    when (command) {
                                                        "Открыть App Store" -> openApp("appstore")
                                                        "Включить тёмное оформление" -> isDark = true
                                                        else -> openApp("camera")
                                                    }
                                                }) { Text("▶  " + command) }
                                            }
                                        }
                                    }

                                    "translate" -> {
                                        Text("Перевод", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Переводчик · локальные примеры", color = mainText, fontWeight = FontWeight.Bold)
                                            Text("Hello → Привет\nThank you → Спасибо\nGood morning → Доброе утро", color = secondaryText, fontSize = 14.sp)
                                        }
                                    }

                                    "tv", "books", "podcasts", "stocks" -> {
                                        Text(selected?.title ?: "Приложение", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                        IOSGlassCard(cardColor) {
                                            Text("Добро пожаловать", color = mainText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                            Text("Это интерактивный макет приложения iOS 18. Здесь работают переходы, списки и локальные действия.", color = secondaryText, fontSize = 12.sp)
                                            Button(onClick = { appInstallNotice = "Раздел добавлен в избранное." }) { Text("Добавить в избранное") }
                                        }
                                    }

                                    else -> {
                                        if (mini != null) {
                                            Text(mini.title, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                            IOSGlassCard(cardColor) {
                                                Text(mini.icon, fontSize = 54.sp)
                                                Text(mini.description, color = secondaryText, fontSize = 11.sp)
                                                Text(mini.screenText, color = mainText, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
                                                Button(onClick = { miniCounter++; appInstallNotice = "Кнопка мини-приложения нажата " + miniCounter + " раз." }) { Text(mini.buttonText) }
                                                Text("Счётчик действий: " + miniCounter, color = secondaryText, fontSize = 10.sp)
                                                TextButton(onClick = { miniPendingExport = miniAppPackage(mini); exportLauncher.launch(mini.title.replace(" ", "_") + ".iosapp") }) { Text("Экспортировать / поделиться") }
                                            }
                                        } else {
                                            Text(selected?.title ?: "Приложение", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                            IOSGlassCard(cardColor) { Text("Макет приложения iOS 18", color = mainText) }
                                        }
                                    }
                                }
                            }
                        }

                        "appstore" -> {
                            // App Store is deliberately implemented as a local virtual marketplace, not as a real Apple service.
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 5.dp)) {
                                Text("App Store", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                                Text("Твоя мастерская приложений", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                                IOSGlassCard(cardColor) {
                                    Text("Создай своё приложение", color = mainText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                    OutlinedTextField(value = miniCreateTitle, onValueChange = { miniCreateTitle = it.take(32) }, modifier = Modifier.fillMaxWidth(), label = { Text("Название приложения") }, singleLine = true)
                                    OutlinedTextField(value = miniCreateIcon, onValueChange = { miniCreateIcon = it.take(4) }, label = { Text("Иконка-эмодзи") }, singleLine = true)
                                    OutlinedTextField(value = miniCreateDescription, onValueChange = { miniCreateDescription = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("Короткое описание") })
                                    OutlinedTextField(value = miniCreateText, onValueChange = { miniCreateText = it.take(800) }, modifier = Modifier.fillMaxWidth(), label = { Text("Содержимое экрана") })
                                    OutlinedTextField(value = miniCreateButton, onValueChange = { miniCreateButton = it.take(24) }, label = { Text("Текст кнопки") }, singleLine = true)
                                    Button(onClick = {
                                        if (miniCreateTitle.isBlank()) Toast.makeText(context, "Сначала введи название", Toast.LENGTH_SHORT).show()
                                        else {
                                            val created = IOSMiniApp("mini_" + System.currentTimeMillis(), miniCreateTitle.trim(), miniCreateIcon.ifBlank { "📦" }, miniCreateDescription, miniCreateText, miniCreateButton.ifBlank { "Открыть" })
                                            miniApps.add(created)
                                            persistMiniApps()
                                            miniCreateTitle = ""
                                            appInstallNotice = "Приложение создано и установлено!"
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("＋ Создать и установить") }
                                    Text("Конструктор создаёт небольшие приложения из безопасных блоков, а не запускает произвольный код.", color = secondaryText, fontSize = 10.sp)
                                }
                                IOSGlassCard(cardColor) {
                                    Text("Каталог сообщества", color = mainText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Установить пакет от другого пользователя", color = secondaryText, fontSize = 11.sp)
                                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("↓ Выбрать файл .iosapp") }
                                    Text("Выбери файл, полученный через мессенджер или меню «Поделиться». Приложение появится среди значков этого симулятора.", color = secondaryText, fontSize = 10.sp)
                                }
                                if (miniApps.isNotEmpty()) {
                                    Text("Установлено · " + miniApps.size, color = Color.White, fontWeight = FontWeight.Bold)
                                    miniApps.forEach { mini ->
                                        IOSGlassCard(cardColor) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(mini.icon, fontSize = 34.sp)
                                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                                    Text(mini.title, color = mainText, fontWeight = FontWeight.Bold)
                                                    Text(mini.description, color = secondaryText, fontSize = 10.sp)
                                                }
                                                TextButton(onClick = { miniPendingExport = miniAppPackage(mini); exportLauncher.launch(mini.title.replace(" ", "_") + ".iosapp") }) { Text("Поделиться") }
                                            }
                                            Row {
                                                TextButton(onClick = { openApp(mini.id) }) { Text("Открыть") }
                                                TextButton(onClick = { miniApps.remove(mini); homeApps.remove(mini.id); persistMiniApps() }) { Text("Удалить") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Text("Библиотека приложений", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Твои созданные приложения", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                            miniApps.forEach { mini -> IOSMiniAppTile(mini, Modifier.fillMaxWidth(), onClick = { openApp(mini.id) }) }
                            TextButton(onClick = { page = "appstore" }) { Text("Создать приложение / открыть App Store", color = Color.White) }
                        }
                    }
                }
            }

            if (page != "lock") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { page = "home" }) { Text("⌂  Домой", color = Color.White, fontSize = 10.sp) }
                    TextButton(onClick = { page = "control" }) { Text("☷  Центр", color = Color.White, fontSize = 10.sp) }
                    TextButton(onClick = { page = "search" }) { Text("⌕  Поиск", color = Color.White, fontSize = 10.sp) }
                    TextButton(onClick = { page = "appgallery" }) { Text("▦  Библиотека", color = Color.White, fontSize = 10.sp) }
                    TextButton(onClick = { openApp("appstore") }) { Text("A  Store", color = Color.White, fontSize = 10.sp) }
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(108.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.85f)))
                }
            }
        }
    }
}

@Composable
private fun IOSGlassCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun IOSAppIcon(
    app: IOS18App,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tint: Color = Color.Transparent,
    large: Boolean = false,
    showLabel: Boolean = true,
    editing: Boolean = false,
    onClick: () -> Unit,
    onHide: (() -> Unit)? = null
) {
    val size = if (compact) 42.dp else if (large) 64.dp else 53.dp
    Column(
        modifier = modifier.padding(3.dp).clickable(onClick = onClick).padding(vertical = if (compact) 4.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(if (compact) 13.dp else 16.dp))
                .background(if (tint != Color.Transparent) Brush.linearGradient(listOf(tint, app.color)) else Brush.linearGradient(listOf(app.color, app.color.copy(alpha = 0.72f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(app.icon, fontSize = if (compact) 23.sp else if (large) 32.sp else 27.sp)
            if (editing && onHide != null) {
                Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Color(0xFFE94D58)).clickable { onHide() }, contentAlignment = Alignment.Center) {
                    Text("−", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (!compact && showLabel && !large) {
            Text(app.title, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (editing && !compact) Text("↔", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
    }
}

@Composable
private fun IOSMiniAppTile(app: IOSMiniApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(17.dp)).background(Color(0xAA19243A)).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.icon, fontSize = 29.sp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(app.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(app.description, color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp, maxLines = 2)
        }
        Text("›", color = Color.White, fontSize = 22.sp)
    }
}

@Composable
private fun IOSQuickButton(icon: String, title: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF3D4960)), contentAlignment = Alignment.Center) { Text(icon, color = Color.White, fontSize = 21.sp) }
        Text(title, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
private fun IOSToggle(title: String, icon: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(4.dp).clip(RoundedCornerShape(17.dp))
            .background(if (enabled) Color(0xFF367CC7) else Color(0xFF3C475A))
            .clickable(onClick = onClick).padding(10.dp)
    ) {
        Text(icon, color = Color.White, fontSize = 22.sp)
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(if (enabled) "Вкл." else "Выкл.", color = Color.White.copy(alpha = 0.72f), fontSize = 9.sp)
    }
}

@Composable
private fun IOSSettingRow(title: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onChange).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color(0xFF25364C), modifier = Modifier.weight(1f), fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = { onChange() })
    }
}

@Composable
private fun IOSMiniAppTileSpacer() {}

private fun safeMath(expression: String): String {
    return try {
        val parser = IOSMathParser(expression.replace("×", "*").replace("÷", "/"))
        val result = parser.parse()
        if (!result.isFinite()) "Ошибка" else String.format(java.util.Locale.US, "%.8f", result).trimEnd('0').trimEnd('.')
    } catch (_: Exception) { "Ошибка в выражении" }
}

private class IOSMathParser(private val input: String) {
    private var pos = 0
    fun parse(): Double {
        val value = expression()
        spaces()
        require(pos == input.length)
        return value
    }
    private fun expression(): Double {
        var value = term()
        while (true) {
            spaces()
            value = when {
                take('+') -> value + term()
                take('-') -> value - term()
                else -> return value
            }
        }
    }
    private fun term(): Double {
        var value = factor()
        while (true) {
            spaces()
            value = when {
                take('*') -> value * factor()
                take('/') -> value / factor()
                else -> return value
            }
        }
    }
    private fun factor(): Double {
        spaces()
        if (take('+')) return factor()
        if (take('-')) return -factor()
        if (take('(')) {
            val result = expression()
            spaces()
            require(take(')'))
            return result
        }
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        require(pos > start)
        return input.substring(start, pos).toDouble()
    }
    private fun spaces() { while (pos < input.length && input[pos].isWhitespace()) pos++ }
    private fun take(char: Char): Boolean {
        if (pos < input.length && input[pos] == char) { pos++; return true }
        return false
    }
}
