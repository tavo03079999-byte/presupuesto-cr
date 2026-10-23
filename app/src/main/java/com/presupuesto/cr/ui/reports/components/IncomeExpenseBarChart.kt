package com.presupuesto.cr.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CurrencyFormatter

private data class BarEntry(
    val label: String,
    val amount: Double,
    val color: Color,
    val categoryType: CategoryType?
)

@Composable
fun IncomeExpenseBarChart(
    totalIncome: Double,
    totalExpense: Double,
    totalSavings: Double,
    onNavigateToType: (CategoryType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(totalIncome, totalExpense, totalSavings).coerceAtLeast(1000.0)
    val grandTotal = (totalIncome + totalExpense + totalSavings).coerceAtLeast(1.0)

    val entries = listOf(
        BarEntry("Ingresos", totalIncome, IncomeGreen, CategoryType.INCOME),
        BarEntry("Gastos", totalExpense, ExpenseRed, CategoryType.EXPENSE),
        BarEntry("Ahorro", totalSavings, Color(0xFFFFC107), CategoryType.SPECIAL_ALLOCATION)
    )

    var selectedBarLabel by remember { mutableStateOf("Gastos") }
    val selectedEntry = entries.find { it.label == selectedBarLabel } ?: entries.first()
    val selectedPct = if ((totalIncome + totalExpense + totalSavings) > 0) {
        (selectedEntry.amount / grandTotal) * 100.0
    } else {
        0.0
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparativa interactiva: Ingresos vs. Gastos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Toca una barra para inspeccionar su monto y abrir movimientos",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { bar ->
                    val isSelected = bar.label == selectedBarLabel
                    FinancialBarItem(
                        label = bar.label,
                        amount = bar.amount,
                        fraction = (bar.amount / maxVal).toFloat().coerceIn(0.08f, 1f),
                        color = bar.color,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected && bar.categoryType != null) {
                                onNavigateToType(bar.categoryType)
                            } else {
                                selectedBarLabel = bar.label
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = selectedEntry.color.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, selectedEntry.color.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedEntry.categoryType?.let { onNavigateToType(it) }
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Tooltip • ${selectedEntry.label}: ${CurrencyFormatter.formatCrc(selectedEntry.amount)} (${String.format("%.1f", selectedPct)}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Toca aquí para abrir todos los movimientos de ${selectedEntry.label} →",
                        fontSize = 11.sp,
                        color = ThemeBlue,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialBarItem(
    label: String,
    amount: Double,
    fraction: Float,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
    ) {
        Text(
            text = CurrencyFormatter.formatCrc(amount),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (isSelected) 52.dp else 42.dp)
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(
                        color = if (isSelected) color else color.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
