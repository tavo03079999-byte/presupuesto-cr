package com.presupuesto.cr.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.ui.home.components.HoldToTalkGeminiButton
import com.presupuesto.cr.ui.home.components.QuickWalletRow
import com.presupuesto.cr.ui.home.components.SafeToSpendCard
import com.presupuesto.cr.ui.home.components.VoiceConfirmSheet
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.EmeraldLight
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.util.CurrencyFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToBcrInbox: () -> Unit = {},
    onNavigateToReports: () -> Unit = {}
) {
    val safeToSpend by viewModel.safeToSpend.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val pendingBcrCount by viewModel.pendingBcrCount.collectAsState()
    val quickForecast by viewModel.quickForecast.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isProcessingVoice by viewModel.isProcessingVoice.collectAsState()
    val parsedTransactionToConfirm by viewModel.parsedTransactionToConfirm.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 100.dp)
            ) {
                // Barra Superior de Perfil y Notificaciones BCR
                HomeTopBar(
                    email = "tavo03079999@gmail.com",
                    pendingBcrCount = pendingBcrCount,
                    onBcrClick = onNavigateToBcrInbox
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Widget Principal: Dinero Libre (Safe-to-Spend)
                SafeToSpendCard(
                    safeToSpend = safeToSpend,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Múltiples Cuentas y Carteras
                QuickWalletRow(accounts = accounts)

                Spacer(modifier = Modifier.height(20.dp))

                // Proyección Rápida a 30 Días
                QuickForecastCard(
                    forecast = quickForecast,
                    onClick = onNavigateToReports,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sección Asistente Inteligente Gemini Hold-to-Talk
                VoiceAssistantHelperBanner(
                    isListening = isListening,
                    isProcessing = isProcessingVoice
                )
            }

            // Botón Flotante Central de Micrófono Gemini Hold-to-Talk
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                HoldToTalkGeminiButton(
                    isListening = isListening,
                    onSpeechStart = { viewModel.startListening() },
                    onSpeechComplete = { text -> viewModel.onSpeechComplete(text) }
                )
            }

            // Modal de confirmación en 1 toque tras hablar
            parsedTransactionToConfirm?.let { parsed ->
                VoiceConfirmSheet(
                    parsed = parsed,
                    onConfirm = { viewModel.confirmTransaction(it) },
                    onDismiss = { viewModel.dismissConfirmDialog() }
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    email: String,
    pendingBcrCount: Int,
    onBcrClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(BcrNavy, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Costa Rica 🇨🇷 (₡ CRC)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = email,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Campanita / Bandeja BCR con Badge
        BadgedBox(
            badge = {
                if (pendingBcrCount > 0) {
                    Badge(containerColor = Color(0xFFE53935)) {
                        Text(text = "$pendingBcrCount", color = Color.White)
                    }
                }
            }
        ) {
            IconButton(onClick = onBcrClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Bandeja BCR",
                    tint = if (pendingBcrCount > 0) Color(0xFF002F6C) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickForecastCard(
    forecast: com.presupuesto.cr.domain.model.ForecastResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (forecast == null) return

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Proyección a 30 días",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Saldo final estimado: ${CurrencyFormatter.formatCrc(forecast.finalBalance)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Ver Forecast",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceAssistantHelperBanner(
    isListening: Boolean,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EmeraldPrimary)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isListening) "Escuchando... habla libremente"
                    else if (isProcessing) "Gemini AI analizando transacción..."
                    else "Mantén presionado el micrófono para registrar por voz",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ej: \"Gasté 15 mil colones en carnes en el súper con la tarjeta BCR\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
