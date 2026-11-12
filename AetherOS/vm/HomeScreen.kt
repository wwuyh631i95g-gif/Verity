package com.excp.podroid.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.WindowSizeClass
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.excp.podroid.engine.VmState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateToTerminal: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToContainerBackup: () -> Unit,
    onNavigateToX11: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val vmState by viewModel.vmState.collectAsStateWithLifecycle()
    val bootStage by viewModel.bootStage.collectAsStateWithLifecycle()
    val uptimeTick by viewModel.uptimeTicker.collectAsStateWithLifecycle()
    val stopping by viewModel.stopping.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(Date()) }
    var openDesktopAfterBoot by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    LaunchedEffect(vmState, openDesktopAfterBoot) {
        if (openDesktopAfterBoot && vmState is VmState.Running) {
            openDesktopAfterBoot = false
            onNavigateToX11()
        }
        if (openDesktopAfterBoot && vmState is VmState.Error) {
            openDesktopAfterBoot = false
        }
    }

    val running = vmState is VmState.Running
    val starting = vmState is VmState.Starting
    val status = when {
        stopping -> "Остановка виртуальной машины"
        running -> "Система запущена"
        starting -> "Загрузка Aether OS"
        vmState is VmState.Error -> "Не удалось запустить систему"
        else -> "Система выключена"
    }
    val clock = SimpleDateFormat("HH:mm", Locale("ru", "RU")).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", Locale("ru", "RU")).format(now)
    val uptime = viewModel.uptimeLabel(uptimeTick)

    fun openDesktop() {
        if (running) {
            onNavigateToX11()
        } else {
            openDesktopAfterBoot = true
            viewModel.startPodroid()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF101831),
                        Color(0xFF282C5A),
                        Color(0xFF075A69),
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("◈  AETHER OS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ВИРТУАЛЬНАЯ МОБИЛЬНАЯ СИСТЕМА", color = Color(0xFFBFCDF5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0x3327D6B0), shape = RoundedCornerShape(50)) {
                    Text("ARM64  •  VM", color = Color(0xFF9EF3DD), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(clock, color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Light)
                Text(date.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru", "RU")) else it.toString() }, color = Color(0xFFD6DDF7), fontSize = 15.sp)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xB82A3D66))
            ) {
                Column(
                    modifier = Modifier.padding(19.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aether OS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Отдельная гостевая Linux-система", color = Color(0xFFD4E0FA), fontSize = 12.sp)
                        }
                        if (starting) CircularProgressIndicator(color = Color(0xFF9DEBE5), modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        else Text(if (running) "●" else "○", color = if (running) Color(0xFF70E5C4) else Color(0xFFB4BBD0), fontSize = 26.sp)
                    }
                    Text(status, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (running && uptime != null) {
                        Text("Время работы: $uptime", color = Color(0xFFCDDAF4), fontSize = 12.sp)
                    } else if (starting) {
                        Text("Запускается ядро Linux и виртуальный рабочий стол…", color = Color(0xFFCDDAF4), fontSize = 12.sp)
                    } else {
                        Text("Собственное ядро Linux, виртуальный диск и приложения внутри гостевой системы.", color = Color(0xFFCDDAF4), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { openDesktop() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        enabled = !starting && !stopping,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE0ECFF),
                            contentColor = Color(0xFF132343),
                            disabledContainerColor = Color(0xFF71819D),
                            disabledContentColor = Color.White
                        )
                    ) {
                        Text(
                            when {
                                running -> "ОТКРЫТЬ РАБОЧИЙ СТОЛ"
                                starting -> "СИСТЕМА ЗАГРУЖАЕТСЯ"
                                else -> "ЗАПУСТИТЬ AETHER OS"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                    if (running) {
                        Text(
                            "Выключить виртуальную систему",
                            modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).clickable { viewModel.stopVm() }.padding(8.dp),
                            color = Color(0xFFFFD2D9),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text("ПРИЛОЖЕНИЯ", color = Color(0xFFD7E3FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AetherAppTile("⌂", "Рабочий стол", "Запустить систему", Modifier.weight(1f), ::openDesktop)
                AetherAppTile("⌨", "Терминал", "Команды Linux", Modifier.weight(1f), onNavigateToTerminal)
                AetherAppTile("▤", "Состояние", "Ресурсы и VM", Modifier.weight(1f), onNavigateToStatus)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AetherAppTile("🛍", "Магазин", "Программы", Modifier.weight(1f), ::openDesktop)
                AetherAppTile("🗂", "Файлы", "Виртуальный диск", Modifier.weight(1f), ::openDesktop)
                AetherAppTile("⚙", "Настройки", "Параметры", Modifier.weight(1f), onNavigateToSettings)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AetherAppTile("◷", "Резервные копии", "Сохранение данных", Modifier.weight(1f), onNavigateToContainerBackup)
                AetherAppTile("↻", "Перезапуск", "Загрузить заново", Modifier.weight(1f), { viewModel.restartVm() })
                AetherAppTile("⌘", "Информация", "О виртуальной ОС", Modifier.weight(1f), onNavigateToStatus)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x44212F55),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Aether OS работает в изолированной ARM64 Linux-виртуальной машине. Android остаётся хост-системой телефона.",
                    color = Color(0xFFC8D5F1),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(13.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AetherAppTile(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x552A3E68))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(color = Color(0x553F6B92), shape = RoundedCornerShape(17.dp), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
        Text(subtitle, color = Color(0xFFBFCBE7), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
    }
}
