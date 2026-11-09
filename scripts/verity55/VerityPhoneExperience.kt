package dev.veriti.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Phone18App(
    val id: String,
    val title: String,
    val icon: String,
    val tint: Color
)

private val phone18Apps = listOf(
    Phone18App("settings", "Настройки", "⚙️", Color(0xFF8EA2B7)),
    Phone18App("photos", "Фото", "🌈", Color(0xFFFFA6A6)),
    Phone18App("camera", "Камера", "📷", Color(0xFF9FA8B2)),
    Phone18App("phone", "Телефон", "📞", Color(0xFF55C982)),
    Phone18App("messages", "Сообщения", "💬", Color(0xFF55C982)),
    Phone18App("safari", "Safari", "🧭", Color(0xFF6CB7F5)),
    Phone18App("music", "Музыка", "🎵", Color(0xFFED78A8)),
    Phone18App("calendar", "Календарь", "📅", Color(0xFFEB6868)),
    Phone18App("clock", "Часы", "⏱️", Color(0xFF9BA9BA)),
    Phone18App("notes", "Заметки", "📝", Color(0xFFFFD56B)),
    Phone18App("reminders", "Напоминания", "✅", Color(0xFF6FC5AD)),
    Phone18App("maps", "Карты", "🗺️", Color(0xFF83C69B)),
    Phone18App("files", "Файлы", "📁", Color(0xFF75B9E9)),
    Phone18App("appstore", "App Store", "🔷", Color(0xFF6EABF4)),
    Phone18App("wallet", "Wallet", "💳", Color(0xFF8D8D9D)),
    Phone18App("health", "Здоровье", "❤️", Color(0xFFE87A8D)),
    Phone18App("weather", "Погода", "☀️", Color(0xFF78B9EA)),
    Phone18App("mail", "Почта", "✉️", Color(0xFF70A9E8)),
    Phone18App("contacts", "Контакты", "👤", Color(0xFFB8A0E8)),
    Phone18App("calculator", "Калькулятор", "🧮", Color(0xFF9097A4))
)

@Composable
fun VerityPhoneExperience() {
    var page by rememberSaveable { mutableStateOf("lock") }
    var currentApp by rememberSaveable { mutableStateOf("settings") }
    var isDark by rememberSaveable { mutableStateOf(false) }
    var wallpaper by rememberSaveable { mutableStateOf(0) }
    var wifi by rememberSaveable { mutableStateOf(true) }
    var bluetooth by rememberSaveable { mutableStateOf(true) }
    var airplane by rememberSaveable { mutableStateOf(false) }
    var focus by rememberSaveable { mutableStateOf(false) }
    var brightness by rememberSaveable { mutableStateOf(0.82f) }
    var islandNotice by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("Добро пожаловать в Phone OS 18!\n\nЭто локальная виртуальная система Verity.") }
    var reminderText by rememberSaveable { mutableStateOf("") }
    val reminders = remember { mutableStateListOf("Познакомиться с Phone OS 18", "Настроить главный экран") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var callStatus by rememberSaveable { mutableStateOf("Набор номера работает в демонстрационном режиме.") }
    var messageDraft by rememberSaveable { mutableStateOf("") }
    val messages = remember { mutableStateListOf("Привет! Это локальный чат-демо.", "Сообщения сохраняются только внутри симулятора.") }
    var browserText by rememberSaveable { mutableStateOf("https://verity.local") }
    var browserPage by rememberSaveable { mutableStateOf("Стартовая страница Verity") }
    var musicPlaying by rememberSaveable { mutableStateOf(false) }
    var photoCount by rememberSaveable { mutableStateOf(4) }
    var timerMinutes by rememberSaveable { mutableStateOf("5") }
    var timerStatus by rememberSaveable { mutableStateOf("Таймер готов к запуску.") }
    var calculatorText by rememberSaveable { mutableStateOf("0") }
    var calendarEvent by rememberSaveable { mutableStateOf("Встреча с друзьями — 18:00") }
    var installed by remember { mutableStateOf(setOf("Погода", "Заметки", "Калькулятор")) }

    val backgroundColors = when (wallpaper % 3) {
        0 -> listOf(Color(0xFF182B55), Color(0xFF7160A8), Color(0xFFF0A5A4))
        1 -> listOf(Color(0xFF102E42), Color(0xFF288F9C), Color(0xFFB2D9C8))
        else -> listOf(Color(0xFF28203D), Color(0xFF9A577C), Color(0xFFF2BD83))
    }
    val surfaceColor = if (isDark) Color(0xE8202430) else Color(0xDDF8FAFF)
    val cardText = if (isDark) Color.White else Color(0xFF18243A)
    val mutedText = if (isDark) Color(0xFFB9C4D5) else Color(0xFF53647A)

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(backgroundColors))
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
            Box(Modifier.fillMaxWidth().height(42.dp)) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("9:41", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 5.dp)
                        .width(112.dp).height(27.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.Black).clickable { islandNotice = !islandNotice },
                    contentAlignment = Alignment.Center
                ) {
                    Text("●  ●", color = Color(0xFF6E7584), fontSize = 9.sp)
                }
                Text(
                    "▮▮  Wi-Fi  ▰",
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedVisibility(visible = islandNotice) {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE10141F))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("◉", color = Color(0xFF8CC8FF), fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Phone OS", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Система работает в демонстрационном режиме", color = Color(0xFFCBD4E2), fontSize = 11.sp)
                        }
                        TextButton(onClick = { islandNotice = false }) { Text("OK") }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = page) { screen ->
                    when (screen) {
                        "lock" -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 35.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ПОНЕДЕЛЬНИК · 9 НОЯБРЯ", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("9:41", color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.Light)
                            Text("Добро пожаловать", color = Color.White, fontSize = 17.sp)
                            Spacer(Modifier.height(30.dp))
                            PhoneGlassCard(surfaceColor) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("ПОГОДА", color = mutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("☀️  21°", color = cardText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                        Text("Ясно · демонстрационные данные", color = mutedText, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("АКТИВНОСТЬ", color = mutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("🚶  4 820", color = cardText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("шагов · пример", color = mutedText, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            PhoneGlassCard(surfaceColor) {
                                Text("Уведомления", color = cardText, fontWeight = FontWeight.Bold)
                                Text("Новых уведомлений нет. Здесь будут отображаться локальные события симулятора.", color = mutedText, fontSize = 12.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { page = "home" },
                                shape = RoundedCornerShape(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF25385B))
                            ) {
                                Text("Разблокировать  →", fontWeight = FontWeight.Bold)
                            }
                            Text("Face ID · демонстрация", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
                        }

                        "home" -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("ПОНЕДЕЛЬНИК, 9 НОЯБРЯ", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text("Привет!", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { page = "search" }) { Text("⌕ Поиск", color = Color.White) }
                            }
                            PhoneGlassCard(surfaceColor) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Виджет погоды", color = mutedText, fontSize = 11.sp)
                                        Text("21°  ·  Ясно", color = cardText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Text("Данные для демонстрации", color = mutedText, fontSize = 10.sp)
                                    }
                                    Text("☀️", fontSize = 38.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            phone18Apps.chunked(4).forEach { rowApps ->
                                Row(Modifier.fillMaxWidth()) {
                                    rowApps.forEach { item ->
                                        Phone18Tile(item, Modifier.weight(1f)) {
                                            currentApp = item.id
                                            page = "app"
                                        }
                                    }
                                    repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            PhoneGlassCard(surfaceColor) {
                                Text("Предложения", color = cardText, fontWeight = FontWeight.Bold)
                                Text("Продолжить знакомство с системой · Настроить экран блокировки · Открыть Центр управления", color = mutedText, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xAA111827)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("phone", "messages", "safari", "music").forEach { id ->
                                    val item = phone18Apps.first { it.id == id }
                                    Phone18Tile(item, Modifier.weight(1f), compact = true) {
                                        currentApp = id
                                        page = "app"
                                    }
                                }
                            }
                        }

                        "control" -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 10.dp)
                        ) {
                            Text("Центр управления", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Быстрые настройки виртуального устройства", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(Modifier.height(15.dp))
                            PhoneGlassCard(surfaceColor) {
                                Row {
                                    PhoneToggle("Wi-Fi", "◉", wifi && !airplane, { wifi = !wifi }, Modifier.weight(1f))
                                    PhoneToggle("Bluetooth", "ᛒ", bluetooth, { bluetooth = !bluetooth }, Modifier.weight(1f))
                                }
                                Row {
                                    PhoneToggle("Авиарежим", "✈", airplane, { airplane = !airplane }, Modifier.weight(1f))
                                    PhoneToggle("Фокус", "☾", focus, { focus = !focus }, Modifier.weight(1f))
                                }
                                Row {
                                    PhoneToggle("Ориентация", "▣", true, { islandNotice = true }, Modifier.weight(1f))
                                    PhoneToggle("Фонарик", "🔦", false, { islandNotice = true }, Modifier.weight(1f))
                                }
                            }
                            PhoneGlassCard(surfaceColor) {
                                Text("Яркость", color = cardText, fontWeight = FontWeight.Bold)
                                Slider(value = brightness, onValueChange = { brightness = it })
                                Text("Громкость", color = cardText, fontWeight = FontWeight.Bold)
                                Slider(value = 0.6f, onValueChange = { })
                            }
                            PhoneGlassCard(surfaceColor) {
                                Text("Медиаплеер", color = cardText, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("♫  Verity Radio", color = mutedText, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { musicPlaying = !musicPlaying }) { Text(if (musicPlaying) "Ⅱ" else "▶") }
                                }
                            }
                        }

                        "search" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 14.dp)) {
                            Text("Поиск", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                label = { Text("Приложения и функции") },
                                singleLine = true
                            )
                            phone18Apps.filter { it.title.contains(searchText, ignoreCase = true) || it.id.contains(searchText, ignoreCase = true) }.forEach { item ->
                                PhoneGlassCard(surfaceColor) {
                                    Row(Modifier.fillMaxWidth().clickable { currentApp = item.id; page = "app" }, verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.icon, fontSize = 27.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(item.title, color = cardText, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.weight(1f))
                                        Text("›", color = mutedText, fontSize = 24.sp)
                                    }
                                }
                            }
                        }

                        "switcher" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 15.dp)) {
                            Text("Многозадачность", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Недавно открытые приложения", color = Color.White.copy(alpha = 0.8f))
                            phone18Apps.take(8).forEach { item ->
                                PhoneGlassCard(surfaceColor) {
                                    Row(Modifier.fillMaxWidth().clickable { currentApp = item.id; page = "app" }, verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.icon, fontSize = 26.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(item.title, color = cardText, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text("Открыть  ›", color = mutedText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        "app" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 7.dp, bottom = 12.dp)) {
                            val selected = phone18Apps.firstOrNull { it.id == currentApp }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { page = "home" }) { Text("‹ Домой", color = Color.White) }
                                Spacer(Modifier.weight(1f))
                                Text(selected?.title ?: "Приложение", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { page = "control" }) { Text("•••", color = Color.White) }
                            }
                            when (currentApp) {
                                "settings" -> {
                                    Text("Настройки", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Verity Phone 18", color = cardText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text("Версия виртуальной системы 18.0 · локальный режим", color = mutedText, fontSize = 11.sp)
                                    }
                                    PhoneGlassCard(surfaceColor) {
                                        PhoneSettingRow("Тёмное оформление", isDark) { isDark = !isDark }
                                        PhoneSettingRow("Wi-Fi", wifi) { wifi = !wifi }
                                        PhoneSettingRow("Bluetooth", bluetooth) { bluetooth = !bluetooth }
                                        PhoneSettingRow("Авиарежим", airplane) { airplane = !airplane }
                                        PhoneSettingRow("Фокусирование", focus) { focus = !focus }
                                    }
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Обои", color = cardText, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            repeat(3) { index ->
                                                Box(Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(14.dp))
                                                    .background(Brush.verticalGradient(when (index) {
                                                        0 -> listOf(Color(0xFF203D7C), Color(0xFFD58DA5))
                                                        1 -> listOf(Color(0xFF1D6C78), Color(0xFFB2D9C8))
                                                        else -> listOf(Color(0xFF543A77), Color(0xFFF2BD83))
                                                    })).clickable { wallpaper = index })
                                            }
                                        }
                                    }
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Приватность", color = cardText, fontWeight = FontWeight.Bold)
                                        Text("Симулятор не запрашивает доступ к камере, микрофону, контактам, SMS, геолокации или Bluetooth. Все переключатели здесь виртуальные.", color = mutedText, fontSize = 12.sp)
                                    }
                                }
                                "notes" -> {
                                    Text("Заметки", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp), label = { Text("Новая заметка") })
                                        Text("Автосохранение в памяти экрана симулятора", color = mutedText, fontSize = 10.sp)
                                    }
                                }
                                "messages" -> {
                                    Text("Сообщения", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        messages.forEach { Text("●  " + it, color = cardText, modifier = Modifier.padding(vertical = 5.dp), fontSize = 13.sp) }
                                        OutlinedTextField(value = messageDraft, onValueChange = { messageDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сообщение") })
                                        Button(onClick = { if (messageDraft.isNotBlank()) { messages.add(messageDraft); messageDraft = "" } }) { Text("Отправить локально") }
                                    }
                                }
                                "phone" -> {
                                    Text("Телефон", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text(phoneNumber.ifBlank { "Введите номер" }, color = cardText, fontSize = 25.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        (1..9).toList().chunked(3).forEach { row ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                row.forEach { digit -> TextButton(onClick = { phoneNumber += digit.toString() }) { Text(digit.toString(), color = cardText, fontSize = 22.sp) } }
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                            TextButton(onClick = { phoneNumber += "*" }) { Text("*", color = cardText) }
                                            TextButton(onClick = { phoneNumber += "0" }) { Text("0", color = cardText) }
                                            TextButton(onClick = { phoneNumber += "#" }) { Text("#", color = cardText) }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                            Button(onClick = { callStatus = "Демо-звонок: реальный вызов не выполняется." }) { Text("Позвонить") }
                                            TextButton(onClick = { phoneNumber = ""; callStatus = "Номер очищен." }) { Text("Очистить") }
                                        }
                                        Text(callStatus, color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "camera" -> {
                                    Text("Камера", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(Color(0xFF10141F)) {
                                        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)).background(Brush.radialGradient(listOf(Color(0xFF526478), Color(0xFF10141F)))), contentAlignment = Alignment.Center) {
                                            Text("◉", color = Color.White, fontSize = 65.sp)
                                        }
                                        Text("Демонстрационная камера · без доступа к камере телефона", color = Color(0xFFCBD4E2), fontSize = 11.sp)
                                        Button(onClick = { photoCount += 1 }) { Text("Сделать виртуальный снимок") }
                                    }
                                }
                                "photos" -> {
                                    Text("Фото", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    Text("$photoCount виртуальных снимков", color = Color.White.copy(alpha = 0.8f))
                                    (0 until photoCount.coerceAtMost(24)).toList().chunked(3).forEach { row ->
                                        Row(Modifier.fillMaxWidth()) {
                                            row.forEach { index ->
                                                Box(Modifier.weight(1f).padding(4.dp).height(88.dp).clip(RoundedCornerShape(14.dp))
                                                    .background(Brush.linearGradient(listOf(Color(0xFF4E82A5 + (index * 0x00010101)), Color(0xFFCE9B9A)))),
                                                    contentAlignment = Alignment.Center
                                                ) { Text(listOf("🌅", "🌊", "🌿", "🌌")[index % 4], fontSize = 28.sp) }
                                            }
                                        }
                                    }
                                }
                                "safari" -> {
                                    Text("Safari", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        OutlinedTextField(value = browserText, onValueChange = { browserText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Адрес или поиск") }, singleLine = true)
                                        Button(onClick = { browserPage = "Демо-страница для: " + browserText }) { Text("Открыть") }
                                        Text(browserPage, color = cardText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        Text("Локальный браузерный макет. Интернет и внешние сайты не открываются.", color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "music" -> {
                                    Text("Музыка", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("♫", color = cardText, fontSize = 60.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("Verity Radio", color = cardText, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("Демонстрационный трек", color = mutedText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Button(onClick = { musicPlaying = !musicPlaying }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (musicPlaying) "Ⅱ Пауза" else "▶ Воспроизвести") }
                                    }
                                }
                                "clock" -> {
                                    Text("Часы", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("9:41", color = cardText, fontSize = 54.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("Будильник · Мировые часы · Секундомер · Таймер", color = mutedText, fontSize = 12.sp)
                                        OutlinedTextField(value = timerMinutes, onValueChange = { timerMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Минуты таймера") })
                                        Button(onClick = { timerStatus = "Таймер на " + timerMinutes.ifBlank { "0" } + " мин. запущен в демо-режиме." }) { Text("Запустить таймер") }
                                        Text(timerStatus, color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "calendar" -> {
                                    Text("Календарь", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Ноябрь 2026", color = cardText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                        Text("ПН   ВТ   СР   ЧТ   ПТ   СБ   ВС", color = mutedText, fontSize = 11.sp)
                                        (1..30).toList().chunked(7).forEach { row ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                row.forEach { day -> Text(day.toString(), color = if (day == 9) Color(0xFF4B9CF0) else cardText, modifier = Modifier.padding(5.dp), fontWeight = if (day == 9) FontWeight.Bold else FontWeight.Normal) }
                                            }
                                        }
                                        OutlinedTextField(value = calendarEvent, onValueChange = { calendarEvent = it }, label = { Text("Событие") }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                                "reminders" -> {
                                    Text("Напоминания", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        OutlinedTextField(value = reminderText, onValueChange = { reminderText = it }, label = { Text("Новое напоминание") }, modifier = Modifier.fillMaxWidth())
                                        Button(onClick = { if (reminderText.isNotBlank()) { reminders.add(reminderText); reminderText = "" } }) { Text("Добавить") }
                                        reminders.forEachIndexed { index, value ->
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text("○  " + value, color = cardText, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                                TextButton(onClick = { reminders.removeAt(index) }) { Text("✓") }
                                            }
                                        }
                                    }
                                }
                                "maps" -> {
                                    Text("Карты", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFDCE8D6)), contentAlignment = Alignment.Center) {
                                            Text("╱╲  ───  ╱╲\n  ●  ─────  ●\n╲  │  ╱  │", color = Color(0xFF5D9077), fontSize = 23.sp, textAlign = TextAlign.Center)
                                        }
                                        Text("Карта-макет без GPS и геолокации.", color = mutedText, fontSize = 11.sp)
                                        OutlinedTextField(value = searchText, onValueChange = { searchText = it }, label = { Text("Поиск места (демо)") })
                                    }
                                }
                                "files" -> {
                                    Text("Файлы", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        listOf("На iPhone", "Загрузки", "Недавние", "Заметки", "Виртуальные снимки").forEach {
                                            Text("📁  " + it, color = cardText, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                                        }
                                        Text("Это виртуальная файловая система. Файлы телефона не читаются.", color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "appstore" -> {
                                    Text("App Store", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    listOf("Мини-игры", "Темы оформления", "Пакет виджетов", "Звуки интерфейса").forEach { title ->
                                        PhoneGlassCard(surfaceColor) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(title, color = cardText, fontWeight = FontWeight.Bold)
                                                    Text("Виртуальное дополнение Verity", color = mutedText, fontSize = 11.sp)
                                                }
                                                TextButton(onClick = { installed = installed + title }) { Text(if (installed.contains(title)) "Добавлено" else "Получить") }
                                            }
                                        }
                                    }
                                    Text("Установлено в каталоге: " + installed.size, color = Color.White, fontSize = 12.sp)
                                }
                                "wallet" -> {
                                    Text("Wallet", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(Color(0xFF243C66)) {
                                        Text("VERITY PASS", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 2.sp)
                                        Text("Виртуальный пропуск", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                        Text("••••  ••••  ••••  1800", color = Color.White, fontSize = 16.sp)
                                    }
                                    Text("Макет Wallet. Реальные банковские карты и платежи не подключаются.", color = Color.White, fontSize = 11.sp)
                                }
                                "health" -> {
                                    Text("Здоровье", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Сводка · пример данных", color = cardText, fontWeight = FontWeight.Bold)
                                        Text("❤️  Пульс: 72 уд/мин", color = cardText, fontSize = 17.sp)
                                        Text("🚶  Шаги: 4 820", color = cardText, fontSize = 17.sp)
                                        Text("😴  Сон: 7 ч 35 мин", color = cardText, fontSize = 17.sp)
                                        Text("Показатели вымышлены и не считываются с датчиков.", color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "weather" -> {
                                    Text("Погода", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("☀️", color = cardText, fontSize = 55.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("21°", color = cardText, fontSize = 54.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("Ясно · демонстрационная погода", color = mutedText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                        Text("ПН  21°   ВТ  19°   СР  20°   ЧТ  18°", color = cardText, modifier = Modifier.padding(top = 15.dp), fontSize = 12.sp)
                                    }
                                }
                                "mail" -> {
                                    Text("Почта", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text("Входящие", color = cardText, fontWeight = FontWeight.Bold)
                                        Text("Добро пожаловать в Verity", color = cardText, fontWeight = FontWeight.SemiBold)
                                        Text("Это демонстрационное письмо. Реальная почта не подключена.", color = mutedText, fontSize = 12.sp)
                                    }
                                }
                                "contacts" -> {
                                    Text("Контакты", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        listOf("Александр", "Мария", "Даниил", "Verity Support").forEach { name ->
                                            Text("👤  " + name, color = cardText, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp))
                                        }
                                        Text("Локальные примеры. Контакты Android не используются.", color = mutedText, fontSize = 11.sp)
                                    }
                                }
                                "calculator" -> {
                                    Text("Калькулятор", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    PhoneGlassCard(surfaceColor) {
                                        Text(calculatorText, color = cardText, fontSize = 34.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                                        listOf("7","8","9","4","5","6","1","2","3","C","0","+").chunked(3).forEach { row ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                row.forEach { key ->
                                                    Button(onClick = { calculatorText = if (key == "C") "0" else if (calculatorText == "0") key else calculatorText + key }) { Text(key) }
                                                }
                                            }
                                        }
                                        Text("Демо-калькулятор интерфейса", color = mutedText, fontSize = 10.sp)
                                    }
                                }
                                else -> Text("Приложение", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (page != "lock") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 9.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { page = "home" }) { Text("⌂  Домой", color = Color.White) }
                    TextButton(onClick = { page = "control" }) { Text("☷  Центр", color = Color.White) }
                    TextButton(onClick = { page = "search" }) { Text("⌕  Поиск", color = Color.White) }
                    TextButton(onClick = { page = "switcher" }) { Text("▢  Окна", color = Color.White) }
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(108.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.8f)))
                }
            }
        }
    }
}

@Composable
private fun Phone18Tile(
    app: Phone18App,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(if (compact) 3.dp else 4.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 5.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(if (compact) 43.dp else 51.dp).clip(RoundedCornerShape(15.dp))
                .background(app.tint),
            contentAlignment = Alignment.Center
        ) {
            Text(app.icon, fontSize = if (compact) 22.sp else 26.sp)
        }
        if (!compact) {
            Text(app.title, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun PhoneGlassCard(
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), content = content)
    }
}

@Composable
private fun PhoneToggle(
    title: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(5.dp).clip(RoundedCornerShape(17.dp))
            .background(if (enabled) Color(0xFF3979C9) else Color(0xFF394150))
            .clickable(onClick = onClick).padding(11.dp)
    ) {
        Text(icon, color = Color.White, fontSize = 22.sp)
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(if (enabled) "Вкл." else "Выкл.", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
    }
}

@Composable
private fun PhoneSettingRow(title: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onChange).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color(0xFF243247), modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = { onChange() })
    }
}
