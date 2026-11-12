package com.excp.podroid.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SetupScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val complete = viewModel.setupComplete.collectAsStateWithLifecycle().value
    LaunchedEffect(complete) {
        if (complete) onSetupComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF101831), Color(0xFF282C5A), Color(0xFF075A69)))
        ).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xDD25365D))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("◈", color = Color.White, fontSize = 54.sp)
                Text("Добро пожаловать в Aether OS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    "Сейчас будет подготовлена отдельная гостевая система Linux ARM64 с собственным ядром и виртуальным диском. Она запускается внутри Android и не заменяет прошивку телефона.",
                    color = Color(0xFFD6E0F7), fontSize = 14.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(3.dp))
                Text("Начальные параметры", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Память: 1 ГБ   •   Процессоры: 2   •   Диск: 8 ГБ", color = Color(0xFFD6E0F7), fontSize = 13.sp, textAlign = TextAlign.Center)
                Text("Дополнительные разрешения Android не запрашиваются. Данные виртуальной системы сохраняются в закрытом хранилище приложения.", color = Color(0xFFBFEAE4), fontSize = 12.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        viewModel.completeSetup(
                            storageSizeGb = 8,
                            vmRamMb = 1024,
                            vmCpus = 2,
                            sshEnabled = false,
                            storageAccessEnabled = false,
                            usbPassthroughEnabled = false,
                            loadBalanceEnabled = false,
                            bandwidthMbps = 0,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0ECFF), contentColor = Color(0xFF132343))
                ) {
                    if (complete) CircularProgressIndicator(color = Color(0xFF132343))
                    else Text("ПОДГОТОВИТЬ AETHER OS", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
