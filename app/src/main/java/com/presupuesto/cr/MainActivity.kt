package com.presupuesto.cr

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.presupuesto.cr.domain.usecase.CalculateSafeToSpendUseCase
import com.presupuesto.cr.domain.usecase.ExportPdfReportUseCase
import com.presupuesto.cr.domain.usecase.ForecastEngineUseCase
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.domain.usecase.ProcessGeminiVoiceUseCase
import com.presupuesto.cr.ui.auth.BiometricAuthHelper
import com.presupuesto.cr.ui.home.HomeViewModel
import com.presupuesto.cr.ui.navigation.MainAppNavigation
import com.presupuesto.cr.ui.ocr.ReceiptScannerHelper
import com.presupuesto.cr.ui.reports.ReportsViewModel
import com.presupuesto.cr.ui.settings.SettingsViewModel
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.ui.theme.PresupuestoCRTheme
import com.presupuesto.cr.ui.transactions.TransactionsViewModel

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthHelper: BiometricAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PresupuestoApp
        val repository = app.repository

        // Inicialización de Use Cases
        val calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(repository)
        val forecastEngineUseCase = ForecastEngineUseCase(repository)
        val processGeminiVoiceUseCase = ProcessGeminiVoiceUseCase(repository)
        val exportPdfReportUseCase = ExportPdfReportUseCase(this, repository, calculateSafeToSpendUseCase)
        val googleDriveBackupUseCase = GoogleDriveBackupUseCase(this)
        val receiptScannerHelper = ReceiptScannerHelper(this)

        // ViewModels
        val homeViewModel = HomeViewModel(
            repository = repository,
            calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
            forecastEngineUseCase = forecastEngineUseCase,
            processGeminiVoiceUseCase = processGeminiVoiceUseCase
        )

        val transactionsViewModel = TransactionsViewModel(
            repository = repository,
            receiptScannerHelper = receiptScannerHelper
        )

        val reportsViewModel = ReportsViewModel(
            repository = repository,
            forecastEngineUseCase = forecastEngineUseCase,
            exportPdfReportUseCase = exportPdfReportUseCase
        )

        val settingsViewModel = SettingsViewModel(
            context = this,
            repository = repository,
            googleDriveBackupUseCase = googleDriveBackupUseCase
        )

        biometricAuthHelper = BiometricAuthHelper(this)

        val prefs = getSharedPreferences("settings_prefs_cr", Context.MODE_PRIVATE)
        val isBiometricConfigured = prefs.getBoolean("biometric_enabled", true)

        setContent {
            PresupuestoCRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(!isBiometricConfigured) }
                    var authError by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        if (isBiometricConfigured && biometricAuthHelper.canAuthenticate()) {
                            biometricAuthHelper.showBiometricPrompt(
                                title = "Presupuesto Inteligente CR",
                                subtitle = "Desbloquea con tu huella dactilar o rostro",
                                onSuccess = {
                                    isAuthenticated = true
                                    authError = null
                                },
                                onError = { error ->
                                    authError = error
                                }
                            )
                        } else {
                            isAuthenticated = true
                        }
                    }

                    if (isAuthenticated) {
                        MainAppNavigation(
                            homeViewModel = homeViewModel,
                            transactionsViewModel = transactionsViewModel,
                            reportsViewModel = reportsViewModel,
                            settingsViewModel = settingsViewModel,
                            googleDriveBackupUseCase = googleDriveBackupUseCase
                        )
                    } else {
                        // Pantalla de Bloqueo de Seguridad
                        LockScreen(
                            errorMessage = authError,
                            onRetryAuth = {
                                biometricAuthHelper.showBiometricPrompt(
                                    title = "Presupuesto Inteligente CR",
                                    subtitle = "Desbloquea con tu huella dactilar o rostro",
                                    onSuccess = {
                                        isAuthenticated = true
                                        authError = null
                                    },
                                    onError = { error ->
                                        authError = error
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(
    errorMessage: String?,
    onRetryAuth: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BcrNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Presupuesto Inteligente CR",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sesión protegida para tavo03079999@gmail.com",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetryAuth,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = CircleShape,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desbloquear con Biometría", fontWeight = FontWeight.Bold)
            }
        }
    }
}
