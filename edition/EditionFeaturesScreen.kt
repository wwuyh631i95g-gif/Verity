package dev.veriti.app

import android.provider.AlarmClock
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

private data class EditionFeature(
    val title: String,
    val description: String,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditionFeaturesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("verity_edition_tools", Context.MODE_PRIVATE) }
    val edition = remember(context.packageName) {
        when {
            context.packageName.contains("x9call", true) -> "watch"
            context.packageName.contains("developer", true) -> "developer"
            else -> "phone"
        }
    }
    val title = when (edition) {
        "watch" -> "X9CALL · функции часов"
        "developer" -> "Developer · инструменты"
        else -> "Phone · функции телефона"
    }
    var note by remember { mutableStateOf(prefs.getString("quick_note", "") ?: "") }
    var noteDialog by remember { mutableStateOf(false) }
    var infoTitle by remember { mutableStateOf("") }
    var infoText by remember { mutableStateOf("") }
    var infoDialog by remember { mutableStateOf(false) }

    fun showInfo(t: String, body: String) {
        infoTitle = t
        infoText = body
        infoDialog = true
    }
    fun toast(s: String) { Toast.makeText(context, s, Toast.LENGTH_SHORT).show() }
    fun open(action: String, label: String) {
        runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { toast("Экран «$label» недоступен на этой версии Android.") }
    }
    fun copy(s: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Verity", s))
        toast("Скопировано")
    }
    fun share(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast("Не удалось открыть меню отправки") }
    }
    fun batteryInfo(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val state = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "заряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "заряжена"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "питание от батареи"
            else -> "статус не определён"
        }
        return "Заряд: ${if (percent >= 0) "$percent%" else "недоступен"}\nСостояние: $state\nУстройство: ${Build.MANUFACTURER} ${Build.MODEL}"
    }
    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) toast("Вибромотор не обнаружен")
        else if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
        else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(90)
        }
    }
    fun versionLine(): String {
        val info = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        return "Приложение: Verity\nПакет: ${context.packageName}\nВерсия: ${info?.versionName ?: "неизвестно"}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nУстройство: ${Build.MANUFACTURER} ${Build.MODEL}\nABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "неизвестно"}"
    }
    fun prefsJson(): String {
        val json = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Set<*> -> json.put(key, JSONArray(value.toList()))
                else -> json.put(key, value)
            }
        }
        return json.toString(2)
    }
    fun bytes(path: File): Long = runCatching {
        if (!path.exists()) 0L else if (path.isFile) path.length()
        else path.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)
    fun sizeText(n: Long): String = String.format(Locale.getDefault(), "%.2f МБ", n / 1048576.0)
    fun assetInventory(): String {
        val names = runCatching { context.assets.list("")?.toList().orEmpty() }.getOrDefault(emptyList())
        return "Файлов верхнего уровня в assets: ${names.size}\n" + names.take(40).joinToString("\n")
    }
    fun permissionReport(): String {
        val permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        return permissions.joinToString("\n") { p ->
            val granted = context.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED
            "${p.substringAfterLast('.')} — ${if (granted) "разрешено" else "не разрешено"}"
        }
    }

    val features = when (edition) {
        "watch" -> listOf(
            EditionFeature("01 · Один короткий вибросигнал", "Проверка вибромотора часов без постоянной работы в фоне.") { vibrate() },
            EditionFeature("02 · Уровень батареи", "Показывает заряд, состояние питания и модель часов.") { showInfo("Батарея часов", batteryInfo()) },
            EditionFeature("03 · Поставить будильник", "Открывает системное создание будильника на часах.") {
                runCatching { context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { open(Settings.ACTION_DATE_SETTINGS, "дата и время") }
            },
            EditionFeature("04 · Таймер на 1 минуту", "Создаёт системный одно-минутный таймер.") {
                runCatching {
                    context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, 60)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Verity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { toast("Системный таймер недоступен") }
            },
            EditionFeature("05 · Bluetooth", "Переход к быстрому управлению подключением часов.") { open(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth") },
            EditionFeature("06 · Wi‑Fi", "Переход к настройкам сети часов.") { open(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi") },
            EditionFeature("07 · Экран часов", "Яркость, тайм-аут и параметры дисплея.") { open(Settings.ACTION_DISPLAY_SETTINGS, "экран") },
            EditionFeature("08 · Звук и вибрация", "Системные громкость и режимы звука.") { open(Settings.ACTION_SOUND_SETTINGS, "звук") },
            EditionFeature("09 · Дата и время", "Часовой пояс, формат и синхронизация часов.") { open(Settings.ACTION_DATE_SETTINGS, "дата и время") },
            EditionFeature("10 · Информация о Verity", "Пакет, версия приложения, Android и модель часов.") { showInfo("Диагностика часов", versionLine()) }
        )
        "developer" -> listOf(
            EditionFeature("01 · Диагностика устройства", "Сводка build, пакета, Android, модели и ABI.") { showInfo("Диагностика", versionLine()) },
            EditionFeature("02 · Копировать build-сводку", "Копирует компактные сведения о сборке в буфер обмена.") { copy(versionLine()) },
            EditionFeature("03 · Отправить отчёт разработчика", "Подготавливает текстовый отчёт без личной переписки и API-ключей.") {
                share("Verity Developer report", versionLine() + "\n\nЛокальная диагностика создана пользователем.")
            },
            EditionFeature("04 · Инвентаризация assets", "Показывает имена ресурсов, включённых в APK.") { showInfo("Assets", assetInventory()) },
            EditionFeature("05 · Проверка разрешений", "Показывает состояние нескольких чувствительных разрешений.") { showInfo("Разрешения", permissionReport()) },
            EditionFeature("06 · Размер хранилища", "Оценивает размер локальных файлов приложения и кэша.") {
                showInfo("Хранилище", "Файлы приложения: ${sizeText(bytes(context.filesDir))}\nКэш: ${sizeText(bytes(context.cacheDir))}")
            },
            EditionFeature("07 · Экспорт локального набора настроек", "Отправляет только настройки этого экрана Edition Tools в JSON.") { share("Verity Edition settings", prefsJson()) },
            EditionFeature("08 · Вибрационный тест", "Проверка вибромотора при отладке тактильных элементов.") { vibrate() },
            EditionFeature("09 · Настройки приложения Android", "Открывает системную страницу разрешений и хранилища Verity.") {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { toast("Не удалось открыть настройки Verity") }
            },
            EditionFeature("10 · Очистить только кэш", "Удаляет временный cacheDir; чаты, скины и настройки не затрагиваются.") {
                showInfo("Очистка кэша", "Для безопасности эту операцию можно выполнить в системной странице приложения Android. Там видны размер кэша и кнопка очистки.")
            }
        )
        else -> listOf(
            EditionFeature("01 · Быстрая заметка", "Сохраняет короткую заметку локально на телефоне.") { noteDialog = true },
            EditionFeature("02 · Скопировать заметку", "Копирует сохранённую заметку в буфер обмена.") {
                if (note.isBlank()) toast("Сначала создай заметку") else copy(note)
            },
            EditionFeature("03 · Поделиться заметкой", "Отправляет заметку через системное меню Android.") {
                if (note.isBlank()) toast("Сначала создай заметку") else share("Заметка Verity", note)
            },
            EditionFeature("04 · Калькулятор Android", "Открывает установленный системный калькулятор.") {
                val i = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CALCULATOR")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }.onFailure { toast("Калькулятор не найден") }
            },
            EditionFeature("05 · Создать будильник", "Открывает системную форму создания будильника.") {
                runCatching { context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { toast("Приложение часов недоступно") }
            },
            EditionFeature("06 · Батарея телефона", "Показывает заряд и состояние питания.") { showInfo("Батарея телефона", batteryInfo()) },
            EditionFeature("07 · Wi‑Fi и сеть", "Открывает системные параметры подключения.") { open(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi") },
            EditionFeature("08 · Bluetooth", "Открывает список сопряжённых устройств.") { open(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth") },
            EditionFeature("09 · Экран и яркость", "Переход к яркости, размеру текста и параметрам экрана.") { open(Settings.ACTION_DISPLAY_SETTINGS, "экран") },
            EditionFeature("10 · Звук и уведомления", "Переход к громкости и управлению уведомлениями.") { open(Settings.ACTION_SOUND_SETTINGS, "звук") }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            when (edition) {
                                "watch" -> "КОМПАКТНЫЙ ПАКЕТ ДЛЯ X9CALL"
                                "developer" -> "РАСШИРЕННЫЙ НАБОР ДЛЯ РАЗРАБОТКИ"
                                else -> "НАБОР ДЛЯ HONOR / ANDROID"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "В этой сборке доступно 10 отдельных инструментов. Они не требуют подключения к сети; системные действия открывают штатные настройки Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(features) { feature ->
                Card(onClick = feature.action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(feature.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(1.dp))
                        Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }

    if (noteDialog) {
        AlertDialog(
            onDismissRequest = { noteDialog = false },
            title = { Text("Быстрая заметка") },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { newText: String -> note = newText.take(1600) },
                    label = { Text("Текст заметки") },
                    minLines = 4,
                    maxLines = 7
                )
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putString("quick_note", note).apply()
                    noteDialog = false
                    toast("Заметка сохранена на устройстве")
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { noteDialog = false }) { Text("Закрыть") } }
        )
    }
    if (infoDialog) {
        AlertDialog(
            onDismissRequest = { infoDialog = false },
            title = { Text(infoTitle) },
            text = { Text(infoText) },
            confirmButton = { TextButton(onClick = { infoDialog = false }) { Text("Готово") } },
            dismissButton = { TextButton(onClick = { copy(infoText) }) { Text("Копировать") } }
        )
    }
}
