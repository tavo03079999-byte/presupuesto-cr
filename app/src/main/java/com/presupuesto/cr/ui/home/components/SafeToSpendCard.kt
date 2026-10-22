package com.presupuesto.cr.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.domain.model.FinancialHealthStatus
import com.presupuesto.cr.domain.model.SafeToSpendResult
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.DangerRed
import com.presupuesto.cr.ui.theme.EmeraldDark
import com.presupuesto.cr.ui.theme.EmeraldLight
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.ui.theme.GoldAmber
import com.presupuesto.cr.util.CurrencyFormatter

@Composable
fun SafeToSpendCard(
    safeToSpend: SafeToSpendResult?,
    modifier: Modifier = Modifier
) {
    if (safeToSpend == null) return

    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (safeToSpend.healthStatus) {
        FinancialHealthStatus.EXCELLENT -> EmeraldLight
        FinancialHealthStatus.STABLE -> Color(0xFF29B6F6)
        FinancialHealthStatus.WARNING -> GoldAmber
        FinancialHealthStatus.CRITICAL -> DangerRed
    }

    val statusLabel = when (safeToSpend.healthStatus) {
        FinancialHealthStatus.EXCELLENT -> "Salud Excelente"
        FinancialHealthStatus.STABLE -> "Presupuesto Estable"
        FinancialHealthStatus.WARNING -> "Precaución"
        FinancialHealthStatus.CRITICAL -> "Riesgo de Déficit"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BcrNavy, Color(0xFF0D1B2A))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Fila Superior: Título + Badge de Salud
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DINERO LIBRE HOY",
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Disponible sin desajustarte",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monto Principal Destacado (Diario)
                Text(
                    text = CurrencyFormatter.formatCrc(safeToSpend.safeToSpendDailyToday),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Resumen del Periodo Quincenal
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total quincena: ${CurrencyFormatter.formatCrc(safeToSpend.totalSafeToSpendFortnight)}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${safeToSpend.daysRemainingInPeriod} días restantes",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mensaje Contextual de IA / Algoritmo
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (safeToSpend.healthStatus == FinancialHealthStatus.CRITICAL || safeToSpend.healthStatus == FinancialHealthStatus.WARNING)
                                Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = safeToSpend.healthMessage,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Botón expandible para ver el desglose matemático
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar desglose" else "Ver fórmula detallada",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Desglose de la Fórmula en Tiempo Real
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        FormulaRow("Saldo Total Líquido:", "+ ${CurrencyFormatter.formatCrc(safeToSpend.totalAvailableInAccounts)}", Color(0xFF81C784))
                        FormulaRow("Gastos Fijos Pendientes:", "- ${CurrencyFormatter.formatCrc(safeToSpend.pendingFixedExpenses)}", Color(0xFFE57373))
                        FormulaRow("Metas de Ahorro / Inversión:", "- ${CurrencyFormatter.formatCrc(safeToSpend.pendingScheduledSavings)}", Color(0xFFFFD54F))
                        FormulaRow("Deuda Tarjetas BCR Próximas:", "- ${CurrencyFormatter.formatCrc(safeToSpend.upcomingCreditCardDebt)}", Color(0xFFEF5350))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
