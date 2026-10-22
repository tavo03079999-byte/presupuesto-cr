package com.presupuesto.cr.ui.reports.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.util.CurrencyFormatter

@Composable
fun IncomeExpenseBarChart(
    totalIncome: Double,
    totalExpense: Double,
    totalSavings: Double,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(totalIncome, totalExpense, totalSavings).coerceAtLeast(1000.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparativa: Ingresos vs. Gastos vs. Ahorro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                FinancialBar(
                    label = "Ingresos",
                    amount = totalIncome,
                    fraction = (totalIncome / maxVal).toFloat().coerceIn(0.05f, 1f),
                    color = EmeraldPrimary
                )
                FinancialBar(
                    label = "Gastos",
                    amount = totalExpense,
                    fraction = (totalExpense / maxVal).toFloat().coerceIn(0.05f, 1f),
                    color = Color(0xFFE53935)
                )
                FinancialBar(
                    label = "Ahorro/Inv.",
                    amount = totalSavings,
                    fraction = (totalSavings / maxVal).toFloat().coerceIn(0.05f, 1f),
                    color = Color(0xFFFBC02D)
                )
            }
        }
    }
}

@Composable
private fun FinancialBar(
    label: String,
    amount: Double,
    fraction: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            text = CurrencyFormatter.formatCrc(amount),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(fraction)
                .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
