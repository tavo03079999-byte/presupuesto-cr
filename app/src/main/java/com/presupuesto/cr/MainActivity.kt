package com.presupuesto.cr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.presupuesto.cr.domain.usecase.CalculateSafeToSpendUseCase
import com.presupuesto.cr.domain.usecase.ExportPdfReportUseCase
import com.presupuesto.cr.domain.usecase.ForecastEngineUseCase
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.ui.auth.BiometricAuthHelper
import com.presupuesto.cr.ui.calendar.CalendarViewModel
import com.presupuesto.cr.ui.home.HomeViewModel
import com.presupuesto.cr.ui.navigation.MainAppNavigation
import com.presupuesto.cr.ui.ocr.ReceiptScannerHelper
import com.presupuesto.cr.ui.reports.ReportsViewModel
import com.presupuesto.cr.ui.settings.SettingsViewModel
import com.presupuesto.cr.ui.theme.BackgroundDark
import com.presupuesto.cr.ui.theme.CyanNeon
import com.presupuesto.cr.ui.theme.PresupuestoCRTheme
import com.presupuesto.cr.ui.theme.SurfaceDark
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.ui.transactions.TransactionsViewModel

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthHelper: BiometricAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PresupuestoApp
        val repository = app.repository

        val calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(repository)
        val forecastEngineUseCase = ForecastEngineUseCase(repository)
        val exportPdfReportUseCase = ExportPdfReportUseCase(this, repository, calculateSafeToSpendUseCase)
        val googleDriveBackupUseCase = GoogleDriveBackupUseCase(this)
        val receiptScannerHelper = ReceiptScannerHelper(this)

        val homeViewModel = HomeViewModel(
            repository = repository,
            calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
            forecastEngineUseCase = forecastEngineUseCase
        )

        val transactionsViewModel = TransactionsViewModel(
            context = this,
            repository = repository,
            receiptScannerHelper = receiptScannerHelper
        )

        val reportsViewModel = ReportsViewModel(
            repository = repository,
            forecastEngineUseCase = forecastEngineUseCase,
            exportPdfReportUseCase = exportPdfReportUseCase
        )

        val calendarViewModel = CalendarViewModel(
            context = this,
            repository = repository
        )

        val settingsViewModel = SettingsViewModel(
            context = this,
            repository = repository,
            googleDriveBackupUseCase = googleDriveBackupUseCase,
            exportPdfReportUseCase = exportPdfReportUseCase
        )

        biometricAuthHelper = BiometricAuthHelper(this)

        val prefs = getSharedPreferences("settings_prefs_cr", Context.MODE_PRIVATE)
        val isBiometricConfigured = prefs.getBoolean("biometric_enabled", false)

        setContent {
            val isDarkMode by settingsViewModel.isDarkThemeEnabled.collectAsState()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            PresupuestoCRTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(!isBiometricConfigured) }
                    var authError by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        if (isBiometricConfigured && biometricAuthHelper.canAuthenticate()) {
                            biometricAuthHelper.showBiometricPrompt(
                                title = "SaldoX",
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
                            calendarViewModel = calendarViewModel,
                            settingsViewModel = settingsViewModel,
                            googleDriveBackupUseCase = googleDriveBackupUseCase
                        )
                    } else {
                        LockScreen(
                            errorMessage = authError,
                            onRetryAuth = {
                                biometricAuthHelper.showBiometricPrompt(
                                    title = "SaldoX",
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.saldox_logo),
                contentDescription = "Logo SaldoX",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, CyanNeon.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SaldoX",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sesión protegida para tavo03079999@gmail.com",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                shape = CircleShape,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Desbloquear con Biometría",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
