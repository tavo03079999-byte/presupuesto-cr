package com.presupuesto.cr.ui.reports

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.ui.reports.components.CategoryPieChart
import com.presupuesto.cr.ui.reports.components.ForecastChart
import com.presupuesto.cr.ui.reports.components.IncomeExpenseBarChart
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToFilteredTransactions: (CategoryType?, Long?, Long?, Long?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    val selectedFilter by viewModel.selectedTimeFilter.collectAsState()
    val customStart by viewModel.customStartDate.collectAsState()
    val customEnd by viewModel.customEndDate.collectAsState()
    val comparisonMode by viewModel.comparisonMode.collectAsState()
    val compAStart by viewModel.compRangeAStart.collectAsState()
    val compAEnd by viewModel.compRangeAEnd.collectAsState()
    val compBStart by viewModel.compRangeBStart.collectAsState()
    val compBEnd by viewModel.compRangeBEnd.collectAsState()
    val comparisonStats by viewModel.comparisonStats.collectAsState()

    val forecastMonths by viewModel.forecastMonths.collectAsState()
    val forecastResult by viewModel.forecastResult.collectAsState()
    val categoriesSummary by viewModel.categoriesSummary.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportedShare by viewModel.exportedShareFile.collectAsState()

    // Selector de fechas: 0 = ninguno, 1 = customStart, 2 = customEnd, 3 = A_Start, 4 = A_End, 5 = B_Start, 6 = B_End
    var activeDatePickerSlot by remember { mutableIntStateOf(0) }

    val netBalance = totalIncome - totalExpense

    LaunchedEffect(exportedShare) {
        exportedShare?.let { exported ->
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exported.file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = exported.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte Financiero PresupuestoCR")
                    putExtra(Intent.EXTRA_TEXT, "Adjunto datos exportados de PresupuestoCR.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir archivo exportado"))
            } catch (e: Exception) {
                // Manejo seguro al compartir
            }
            viewModel.clearExportedFile()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Reportes e Inteligencia",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Intervalos personalizados, gráficos interactivos y exportación",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TimeFilter.values().toList()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.setTimeFilter(filter) },
                                label = { Text(filter.label, fontSize = 12.sp) }
                            )
                        }
                    }

                    if (selectedFilter == TimeFilter.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { activeDatePickerSlot = 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Inicio: ${DateUtils.formatDate(customStart)}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { activeDatePickerSlot = 2 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Fin: ${DateUtils.formatDate(customEnd)}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        title = "Ingresos",
                        amount = totalIncome,
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Gastos",
                        amount = totalExpense,
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Balance Neto",
                        amount = netBalance,
                        color = if (netBalance >= 0) ThemeBlue else ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val (activeStart, activeEnd) = viewModel.getActiveRangeBounds()
                IncomeExpenseBarChart(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    totalSavings = totalSavings,
                    onNavigateToType = { type ->
                        onNavigateToFilteredTransactions(type, null, activeStart, activeEnd)
                    }
                )
            }

            item {
                val (activeStart, activeEnd) = viewModel.getActiveRangeBounds()
                CategoryPieChart(
                    categoriesSummary = categoriesSummary,
                    onNavigateToCategory = { catSummary ->
                        onNavigateToFilteredTransactions(
                            CategoryType.EXPENSE,
                            catSummary.categoryId,
                            activeStart,
                            activeEnd
                        )
                    }
                )
            }

            item {
                PeriodComparisonCard(
                    mode = comparisonMode,
                    stats = comparisonStats,
                    compAStart = compAStart,
                    compAEnd = compAEnd,
                    compBStart = compBStart,
                    compBEnd = compBEnd,
                    onModeSelected = { viewModel.setComparisonMode(it) },
                    onPickSlot = { slot -> activeDatePickerSlot = slot }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Proyección de liquidez",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = forecastMonths == 3,
                            onClick = { viewModel.setForecastHorizon(3) },
                            label = { Text("3 Meses", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = forecastMonths == 6,
                            onClick = { viewModel.setForecastHorizon(6) },
                            label = { Text("6 Meses", fontSize = 11.sp) }
                        )
                    }
                }
            }

            item {
                ForecastChart(forecastResult = forecastResult)
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exportar datos del rango seleccionado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val (s, e) = viewModel.getActiveRangeBounds()
                        Text(
                            text = "Rango: ${DateUtils.formatDate(s)} al ${DateUtils.formatDate(e)} (compartir por WhatsApp, correo, etc.)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.exportCsvReport() },
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CSV", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.exportPdfReport() },
                                enabled = !isExporting,
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (activeDatePickerSlot != 0) {
        val initialMillis = when (activeDatePickerSlot) {
            1 -> customStart
            2 -> customEnd
            3 -> compAStart
            4 -> compAEnd
            5 -> compBStart
            else -> compBEnd
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { activeDatePickerSlot = 0 },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { chosen ->
                            when (activeDatePickerSlot) {
                                1 -> viewModel.setCustomDateRange(chosen, customEnd)
                                2 -> viewModel.setCustomDateRange(customStart, chosen)
                                3 -> viewModel.setCustomComparisonRangeA(chosen, compAEnd)
                                4 -> viewModel.setCustomComparisonRangeA(compAStart, chosen)
                                5 -> viewModel.setCustomComparisonRangeB(chosen, compBEnd)
                                6 -> viewModel.setCustomComparisonRangeB(compBStart, chosen)
                            }
                        }
                        activeDatePickerSlot = 0
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerSlot = 0 }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.formatCrc(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PeriodComparisonCard(
    mode: PeriodComparisonMode,
    stats: ComparisonStats,
    compAStart: Long,
    compAEnd: Long,
    compBStart: Long,
    compBEnd: Long,
    onModeSelected: (PeriodComparisonMode) -> Unit,
    onPickSlot: (Int) -> Unit
) {
    val deltaExpensePct = if (stats.periodAExpense > 0) {
        ((stats.periodBExpense - stats.periodAExpense) / stats.periodAExpense) * 100.0
    } else {
        0.0
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparación entre periodos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PeriodComparisonMode.values().toList()) { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { onModeSelected(item) },
                        label = { Text(item.label, fontSize = 11.sp) }
                    )
                }
            }

            if (mode == PeriodComparisonMode.TWO_CUSTOM_RANGES) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Intervalo A:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { onPickSlot(3) }, modifier = Modifier.weight(1f)) {
                        Text(DateUtils.formatDate(compAStart), fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onPickSlot(4) }, modifier = Modifier.weight(1f)) {
                        Text(DateUtils.formatDate(compAEnd), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Intervalo B:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { onPickSlot(5) }, modifier = Modifier.weight(1f)) {
                        Text(DateUtils.formatDate(compBStart), fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onPickSlot(6) }, modifier = Modifier.weight(1f)) {
                        Text(DateUtils.formatDate(compBEnd), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.periodALabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ing: ${CurrencyFormatter.formatCrc(stats.periodAIncome)}",
                        fontSize = 12.sp,
                        color = IncomeGreen
                    )
                    Text(
                        text = "Gas: ${CurrencyFormatter.formatCrc(stats.periodAExpense)}",
                        fontSize = 12.sp,
                        color = ExpenseRed
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stats.periodBLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ThemeBlue
                    )
                    Text(
                        text = "Ing: ${CurrencyFormatter.formatCrc(stats.periodBIncome)}",
                        fontSize = 12.sp,
                        color = IncomeGreen
                    )
                    Text(
                        text = "Gas: ${CurrencyFormatter.formatCrc(stats.periodBExpense)}",
                        fontSize = 12.sp,
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Variación de gastos: ${if (deltaExpensePct >= 0) "+" else ""}${String.format("%.1f", deltaExpensePct)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (deltaExpensePct <= 0) IncomeGreen else ExpenseRed
            )
        }
    }
}
