package com.presupuesto.cr.ui.reports

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.presupuesto.cr.ui.reports.components.CategoryPieChart
import com.presupuesto.cr.ui.reports.components.ForecastChart
import com.presupuesto.cr.ui.reports.components.IncomeExpenseBarChart
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.EmeraldPrimary

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val selectedFilter by viewModel.selectedTimeFilter.collectAsState()
    val forecastMonths by viewModel.forecastMonths.collectAsState()
    val forecastResult by viewModel.forecastResult.collectAsState()
    val categoriesSummary by viewModel.categoriesSummary.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val exportedFile by viewModel.exportedPdfFile.collectAsState()

    LaunchedEffect(exportedFile) {
        exportedFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Ejecutivo Presupuesto CR")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte financiero generado para tavo03079999@gmail.com")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PDF"))
            viewModel.clearExportedPdf()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Análisis Financiero y Reportes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Inteligencia multitemporal y predicción de flujo de caja",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Multitemporal: Día / Semana / Quincena / Mes / Año
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimeFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setTimeFilter(filter) },
                        label = { Text(filter.label, fontSize = 11.5.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Gráfico de Barras: Comparativa
            IncomeExpenseBarChart(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                totalSavings = totalSavings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Gráfico de Dona: Distribución por Categoría
            CategoryPieChart(categoriesSummary = categoriesSummary)

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Sección de Forecast Predictivo con selector de horizonte (3 meses / 6 meses)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proyecciones Predictivas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = forecastMonths == 3,
                        onClick = { viewModel.setForecastHorizon(3) },
                        label = { Text("3 Meses (90d)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = forecastMonths == 6,
                        onClick = { viewModel.setForecastHorizon(6) },
                        label = { Text("6 Meses (180d)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ForecastChart(forecastResult = forecastResult)

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Botón de Exportación de Reportes PDF
            Button(
                onClick = { viewModel.exportPdfReport() },
                enabled = !isExportingPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BcrNavy),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isExportingPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generando Reporte PDF...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Reporte Ejecutivo PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
