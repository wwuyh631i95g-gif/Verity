package dev.veriti.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private suspend fun seedVerityWorkspace(context: Context, root: File) = withContext(Dispatchers.IO) {
    root.deleteRecursively()
    root.mkdirs()
    val canonicalRoot = root.canonicalFile
    context.assets.open("verity-dev-source.zip").use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val path = entry.name.replace('\\', '/').removePrefix("Verity-main/")
                if (path.isNotBlank()) {
                    val output = File(root, path).canonicalFile
                    require(output.path.startsWith(canonicalRoot.path + File.separator)) { "Недопустимый путь в архиве" }
                    if (entry.isDirectory) output.mkdirs() else {
                        output.parentFile?.mkdirs()
                        output.outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    File(root, ".source_ready").writeText("Verity 5.0.1")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeveloperWorkbenchScreen(
    onResetApp: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = remember { File(context.filesDir, "verity_dev_workspace") }
    var ready by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var editing by remember { mutableStateOf<File?>(null) }
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Загружаю исходники…") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    fun refresh() {
        val current = File(root, folder).canonicalFile
        if (!current.path.startsWith(root.canonicalPath)) return
        files = current.listFiles()?.filterNot { it.name == ".source_ready" }
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })).orEmpty()
    }

    LaunchedEffect(Unit) {
        runCatching {
            if (!File(root, ".source_ready").exists()) seedVerityWorkspace(context, root)
        }.onSuccess {
            ready = true
            message = "Чистая копия исходников Verity 5.0.1 готова."
            refresh()
        }.onFailure {
            error = it.message ?: "Не удалось распаковать исходники"
            message = "Рабочая папка не готова."
        }
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        ZipOutputStream(output).use { zip ->
                            root.walkTopDown().filter { it.isFile && it.name != ".source_ready" }.forEach { file ->
                                zip.putNextEntry(ZipEntry("Verity-main/" + file.relativeTo(root).invariantSeparatorsPath))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    } ?: error("Не удалось открыть файл для сохранения")
                }
            }.onSuccess { message = "ZIP рабочей копии сохранён."; error = null }
                .onFailure { error = it.message ?: "Ошибка экспорта ZIP" }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Verity Developer", fontWeight = FontWeight.Bold) },
                navigationIcon = { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.padding(start = 16.dp)) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Тестовая среда", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Verity+ открыт только в этой отдельной Developer-сборке. Исходники хранятся в отдельной рабочей копии приложения.", style = MaterialTheme.typography.bodySmall)
                    Text("Изменения сохраняются в файлы сразу. Но Kotlin/Java-код уже установленного APK нельзя перекомпилировать на лету: чтобы изменения кода заработали, экспортируй ZIP, собери новый APK и установи его.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Функции Verity") }
                OutlinedButton(onClick = { if (ready) export.launch("Verity-5.0.1-Developer-Source.zip") }, enabled = ready, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(5.dp)); Text("Экспорт ZIP")
                }
            }
            if (folder.isNotBlank()) {
                TextButton(onClick = { folder = folder.substringBeforeLast('/', ""); refresh() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null); Spacer(Modifier.width(5.dp)); Text("Назад")
                }
            }
            Text(if (folder.isBlank()) "Исходники" else "Исходники / " + folder, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (!ready) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(files, key = { it.relativeTo(root).invariantSeparatorsPath }) { file ->
                    val editable = file.isFile && file.extension.lowercase() in setOf("kt", "java", "xml", "kts", "gradle", "properties", "json", "md", "txt", "js", "html", "css", "yml", "yaml")
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = file.isDirectory || editable) {
                            if (file.isDirectory) {
                                folder = listOf(folder, file.name).filter { it.isNotBlank() }.joinToString("/")
                                refresh()
                            } else if (editable) scope.launch {
                                runCatching { withContext(Dispatchers.IO) { file.readText() } }
                                    .onSuccess { text = it; editing = file }
                                    .onFailure { error = it.message ?: "Не удалось открыть файл" }
                            }
                        }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (file.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, maxLines = 1)
                            Text(if (file.isDirectory) "Папка" else if (editable) "${file.length()} байт · открыть редактор" else "Бинарный файл", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.RestartAlt, null); Spacer(Modifier.width(7.dp)); Text("Сбросить до стандартного Verity")
            }
            Spacer(Modifier.height(5.dp))
        }
    }

    editing?.let { file ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(file.relativeTo(root).invariantSeparatorsPath, maxLines = 2) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 520.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    minLines = 12,
                    maxLines = 24
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { file.writeText(text) } }
                            .onSuccess { message = "Сохранено: ${file.name}"; error = null }
                            .onFailure { error = it.message ?: "Не удалось сохранить" }
                        editing = null
                        refresh()
                    }
                }) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(5.dp)); Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Закрыть") } }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить рабочую среду?") },
            text = { Text("Рабочие исходники восстановятся из чистого архива 5.0.1, а настройки приложения вернутся к значениям по умолчанию. История чатов не удалится. Тестовый Verity+ останется включённым в Developer-сборке.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        runCatching { seedVerityWorkspace(context, root) }
                            .onSuccess {
                                folder = ""
                                refresh()
                                onResetApp()
                                message = "Исходники и настройки восстановлены."
                                error = null
                            }
                            .onFailure { error = it.message ?: "Не удалось выполнить сброс" }
                    }
                }) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } }
        )
    }
}
