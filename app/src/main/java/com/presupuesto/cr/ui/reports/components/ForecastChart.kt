package com.presupuesto.cr.ui.reports.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.ui.theme.DangerRed
import com.presupuesto.cr.ui.theme.EmeraldLight
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.ui.theme.GoldAmber
import com.presupuesto.cr.util.CurrencyFormatter

@Composable
fun ForecastChart(
    forecastResult: ForecastResult?,
    modifier: Modifier = Modifier
) {
    if (forecastResult == null || forecastResult.points.isEmpty()) return

    val points = forecastResult.points
    val minVal = (forecastResult.minProjectedBalance * 0.9).coerceAtLeast(0.0)
    val maxVal = (forecastResult.maxProjectedBalance * 1.1).coerceAtLeast(100000.0)
    val valRange = (maxVal - minVal).coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Curva Predictiva de Liquidez",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Horizonte a ${forecastResult.periodDays} días (${forecastResult.periodDays / 30} meses)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Final: ${CurrencyFormatter.formatCrc(forecastResult.finalBalance)}",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lienzo Canvas de la Gráfica Interactiva
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height

                val path = Path()
                val fillPath = Path()

                val stepX = width / (points.size - 1).coerceAtLeast(1)

                points.forEachIndexed { index, pt ->
                    val x = index * stepX
                    val normalizedY = ((pt.projectedBalance - minVal) / valRange).toFloat()
                    val y = height - (normalizedY * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    // Puntos críticos de alerta (Valles de liquidez)
                    if (pt.isCriticalValley) {
                        drawCircle(
                            color = DangerRed,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Relleno degradado bajo la curva
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(EmeraldLight.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Trazo principal de la curva
                drawPath(
                    path = path,
                    color = EmeraldPrimary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Alertas Preventivas de Caídas Futuras de Saldo
            if (forecastResult.criticalAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    forecastResult.criticalAlerts.forEach { alert ->
                        Surface(
                            color = DangerRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = alert,
                                    color = DangerRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
