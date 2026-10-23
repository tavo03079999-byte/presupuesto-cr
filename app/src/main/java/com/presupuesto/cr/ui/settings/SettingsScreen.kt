package com.presupuesto.cr.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.domain.model.BudgetAlertLevel
import com.presupuesto.cr.domain.model.CategoryBudgetProgress
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.ui.theme.CyanNeon
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.ui.theme.WarningOrange
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    googleDriveBackupUseCase: GoogleDriveBackupUseCase
) {
    val context = LocalContext.current

    val isDarkThemeEnabled by viewModel.isDarkThemeEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgetProgressList by viewModel.budgetProgressList.collectAsState()

    // Estados para Categorías Personalizadas
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    // Estados para Presupuestos por Categoría
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetCategoryPreselectId by remember { mutableStateOf<Long?>(null) }
    var budgetInitialAmount by remember { mutableStateOf("") }
    var budgetToDelete by remember { mutableStateOf<CategoryBudgetProgress?>(null) }

    // Estados para Exportación CSV / PDF
    // 0 = Mes actual, 1 = Todo el historial, 2 = Rango personalizado
    var exportRangeMode by remember { mutableIntStateOf(0) }
    val (defaultMonthStart, defaultMonthEnd) = remember { DateUtils.getCurrentMonthBounds() }
    var exportStartMillis by remember { mutableLongStateOf(defaultMonthStart) }
    var exportEndMillis by remember { mutableLongStateOf(defaultMonthEnd) }
    // 0 = cerrado, 1 = fecha inicio, 2 = fecha fin
    var exportDatePickerSlot by remember { mutableIntStateOf(0) }

    LaunchedEffect(exportedFile) {
        exportedFile?.let { target ->
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    target.file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = target.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, target.chooserTitle))
            } catch (_: Exception) {
            } finally {
                viewModel.clearExportedFile()
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            viewModel.performGoogleDriveBackup(account)
        } catch (e: Exception) {
            viewModel.performGoogleDriveBackup(null)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Categorías, presupuestos mensuales, exportación y respaldos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (uiMessage != null) {
                item {
                    Surface(
                        color = ThemeBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ThemeBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { viewModel.clearUiMessage() }) {
                                Text("OK", fontWeight = FontWeight.Bold, color = ThemeBlue)
                            }
                        }
                    }
                }
            }

            // 1. Apariencia y Seguridad
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBlue.copy(alpha = 0.16f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo Oscuro Finora",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Alternar entre tema oscuro neón y tema claro",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Switch(
                                checked = isDarkThemeEnabled,
                                onCheckedChange = { viewModel.toggleDarkTheme(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(IncomeGreen.copy(alpha = 0.16f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloqueo Biométrico",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Huella dactilar o reconocimiento facial al abrir",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = IncomeGreen)
                            )
                        }
                    }
                }
            }

            // 2. Presupuesto Mensual por Categoría (Prompt 5)
            item {
                BudgetsSummarySectionCard(
                    budgets = budgetProgressList,
                    onAddBudget = {
                        budgetCategoryPreselectId = null
                        budgetInitialAmount = ""
                        showBudgetDialog = true
                    },
                    onEditBudget = { item ->
                        budgetCategoryPreselectId = item.categoryId
                        budgetInitialAmount = item.limitAmount.toLong().toString()
                        showBudgetDialog = true
                    },
                    onDeleteBudget = { item ->
                        budgetToDelete = item
                    }
                )
            }

            // 3. Exportar Datos CSV / PDF (Prompt 11)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Exportar y Compartir Datos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exporta tus movimientos en CSV o PDF y compártelos por WhatsApp o correo.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = exportRangeMode == 0,
                                onClick = { exportRangeMode = 0 },
                                label = { Text("Mes actual", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = exportRangeMode == 1,
                                onClick = { exportRangeMode = 1 },
                                label = { Text("Todo", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = exportRangeMode == 2,
                                onClick = { exportRangeMode = 2 },
                                label = { Text("Rango libre", fontSize = 11.sp) }
                            )
                        }

                        if (exportRangeMode == 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { exportDatePickerSlot = 1 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Desde: ${DateUtils.formatDate(exportStartMillis)}",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { exportDatePickerSlot = 2 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Hasta: ${DateUtils.formatDate(exportEndMillis)}",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val effectiveStart = when (exportRangeMode) {
                            0 -> defaultMonthStart
                            2 -> DateUtils.getStartOfDay(exportStartMillis)
                            else -> null
                        }
                        val effectiveEnd = when (exportRangeMode) {
                            0 -> defaultMonthEnd
                            2 -> DateUtils.getEndOfDay(exportEndMillis)
                            else -> null
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.exportTransactions(
                                        asCsv = true,
                                        startMillis = effectiveStart,
                                        endMillis = effectiveEnd
                                    )
                                },
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableView,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.exportTransactions(
                                        asCsv = false,
                                        startMillis = effectiveStart,
                                        endMillis = effectiveEnd
                                    )
                                },
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. Respaldo en Google Drive & Lector BCR
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ThemeBlue.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = ThemeBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Respaldo en Google Drive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Cuenta: ${GoogleDriveBackupUseCase.TARGET_ACCOUNT_EMAIL}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (backupStatus != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = IncomeGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = backupStatus ?: "",
                                    fontSize = 11.5.sp,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val account = googleDriveBackupUseCase.getLastSignedInAccount()
                                if (account != null) {
                                    viewModel.performGoogleDriveBackup(account)
                                } else {
                                    val signInIntent = googleDriveBackupUseCase.getGoogleSignInClient().signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                }
                            },
                            enabled = !isBackingUp,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Respaldar Ahora en Google Drive", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lector automático de notificaciones BCR / SINPE",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Configurar Permiso de Notificaciones")
                        }
                    }
                }
            }

            // 5. Categorías Personalizadas (Prompt 4)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Categorías Personalizadas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Crea, edita o elimina categorías con tu propio icono y color",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            categoryToEdit = null
                            showCategoryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            items(categories, key = { it.id }) { cat ->
                val catColor = CategoryIconMapper.parseColor(cat.colorHex, ThemeBlue)
                val catIcon = CategoryIconMapper.getIcon(cat.iconName)
                val typeLabel = when (cat.type) {
                    CategoryType.INCOME -> "Ingreso"
                    CategoryType.EXPENSE -> "Gasto"
                    CategoryType.SPECIAL_ALLOCATION -> "Ahorro"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(catColor.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = typeLabel,
                                    fontSize = 11.sp,
                                    color = if (cat.type == CategoryType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    categoryToEdit = cat
                                    showCategoryDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar categoría",
                                    tint = ThemeBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { categoryToDelete = cat }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar categoría",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Diálogo Crear / Editar Categoría
        if (showCategoryDialog) {
            CategoryEditorDialog(
                existingCategory = categoryToEdit,
                onConfirm = { name, type, iconName, colorHex ->
                    viewModel.saveCategory(
                        existing = categoryToEdit,
                        name = name,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                    showCategoryDialog = false
                    categoryToEdit = null
                },
                onDismiss = {
                    showCategoryDialog = false
                    categoryToEdit = null
                }
            )
        }

        // Confirmación Eliminar Categoría
        categoryToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = {
                    Text("Eliminar categoría", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("¿Estás seguro de que deseas eliminar la categoría \"${target.name}\"? Las transacciones asociadas pasarán a \"Otros\".")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(target)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { categoryToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo Definir / Editar Presupuesto Mensual
        if (showBudgetDialog) {
            val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }
            BudgetEditorDialog(
                expenseCategories = expenseCats,
                preselectedCategoryId = budgetCategoryPreselectId,
                initialLimitText = budgetInitialAmount,
                onConfirm = { categoryId, limitAmount ->
                    viewModel.saveCategoryBudget(categoryId, limitAmount)
                    showBudgetDialog = false
                },
                onDismiss = { showBudgetDialog = false }
            )
        }

        // Confirmación Eliminar Presupuesto
        budgetToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { budgetToDelete = null },
                title = {
                    Text("Eliminar presupuesto", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("¿Deseas eliminar el límite mensual de \"${target.categoryName}\"?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategoryBudget(target.budgetId)
                            budgetToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { budgetToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Selector de Fecha para Exportación
        if (exportDatePickerSlot != 0) {
            val initialMillis = if (exportDatePickerSlot == 1) exportStartMillis else exportEndMillis
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { exportDatePickerSlot = 0 },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val picked = datePickerState.selectedDateMillis ?: initialMillis
                            if (exportDatePickerSlot == 1) {
                                exportStartMillis = picked
                            } else {
                                exportEndMillis = picked
                            }
                            exportDatePickerSlot = 0
                        }
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exportDatePickerSlot = 0 }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun BudgetsSummarySectionCard(
    budgets: List<CategoryBudgetProgress>,
    onAddBudget: () -> Unit,
    onEditBudget: (CategoryBudgetProgress) -> Unit,
    onDeleteBudget: (CategoryBudgetProgress) -> Unit
) {
    val totalLimit = budgets.sumOf { it.limitAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val overallRatio = if (totalLimit > 0.0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Presupuestos por Categoría",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Alertas al 80% y notificación al superar el 100%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(onClick = onAddBudget) {
                    Text("+ Límite", fontWeight = FontWeight.Bold, color = ThemeBlue)
                }
            }

            if (budgets.isEmpty()) {
                Text(
                    text = "Aún no has definido límites mensuales. Presiona \"+ Límite\" para controlar el gasto por categoría.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total gastado: ${CurrencyFormatter.formatCrc(totalSpent)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Meta: ${CurrencyFormatter.formatCrc(totalLimit)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { overallRatio },
                            color = when {
                                overallRatio >= 1f -> ExpenseRed
                                overallRatio >= 0.8f -> WarningOrange
                                else -> IncomeGreen
                            },
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                budgets.forEach { b ->
                    val barColor = when (b.alertLevel) {
                        BudgetAlertLevel.EXCEEDED_100 -> ExpenseRed
                        BudgetAlertLevel.WARNING_80 -> WarningOrange
                        BudgetAlertLevel.NORMAL -> IncomeGreen
                    }
                    val progressClamped = (b.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(b.iconName),
                                    contentDescription = null,
                                    tint = CategoryIconMapper.parseColor(b.categoryColorHex, ThemeBlue),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = b.categoryName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (b.alertLevel != BudgetAlertLevel.NORMAL) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = barColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${b.percentageUsed.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                                IconButton(
                                    onClick = { onEditBudget(b) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar límite",
                                        tint = ThemeBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteBudget(b) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar límite",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressClamped },
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gastado: ${CurrencyFormatter.formatCrc(b.spentAmount)} / ${CurrencyFormatter.formatCrc(b.limitAmount)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (b.remainingAmount >= 0.0) {
                                    "Restan ${CurrencyFormatter.formatCrc(b.remainingAmount)}"
                                } else {
                                    "Excedido ${CurrencyFormatter.formatCrc(-b.remainingAmount)}"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = barColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    existingCategory: CategoryEntity?,
    onConfirm: (name: String, type: CategoryType, iconName: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existingCategory?.name ?: "") }
    var selectedType by remember { mutableStateOf(existingCategory?.type ?: CategoryType.EXPENSE) }
    var selectedIconName by remember {
        mutableStateOf(existingCategory?.iconName ?: CategoryIconMapper.availableIcons.first().first)
    }
    var selectedColorHex by remember {
        mutableStateOf(existingCategory?.colorHex ?: CategoryIconMapper.availableColorsHex.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingCategory == null) "Nueva Categoría" else "Editar Categoría",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la categoría") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == CategoryType.EXPENSE,
                        onClick = { selectedType = CategoryType.EXPENSE },
                        label = { Text("Gasto") }
                    )
                    FilterChip(
                        selected = selectedType == CategoryType.INCOME,
                        onClick = { selectedType = CategoryType.INCOME },
                        label = { Text("Ingreso") }
                    )
                }

                Text(
                    text = "Selecciona un icono:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CategoryIconMapper.availableIcons, key = { it.first }) { (iconKey, iconVector) ->
                        val isSelected = selectedIconName.equals(iconKey, ignoreCase = true)
                        val activeColor = CategoryIconMapper.parseColor(selectedColorHex, ThemeBlue)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconKey,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "Selecciona un color:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CategoryIconMapper.availableColorsHex, key = { it }) { hex ->
                        val swatchColor = CategoryIconMapper.parseColor(hex, ThemeBlue)
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedType, selectedIconName, selectedColorHex)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun BudgetEditorDialog(
    expenseCategories: List<CategoryEntity>,
    preselectedCategoryId: Long?,
    initialLimitText: String,
    onConfirm: (categoryId: Long, limitAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember(expenseCategories, preselectedCategoryId) {
        mutableLongStateOf(preselectedCategoryId ?: expenseCategories.firstOrNull()?.id ?: 3L)
    }
    var limitText by remember(initialLimitText) { mutableStateOf(initialLimitText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Límite Mensual de Categoría",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Elige la categoría de gasto:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(expenseCategories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = {
                                Text(
                                    text = cat.name,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Presupuesto mensual máximo (₡ CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (parsed > 0.0) {
                        onConfirm(selectedCategoryId, parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue)
            ) {
                Text("Guardar límite", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
