package com.presupuesto.cr.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.domain.model.CategoryExpenseSummary
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import kotlin.math.atan2

@Composable
fun CategoryPieChart(
    categoriesSummary: List<CategoryExpenseSummary>,
    incomeCategoriesSummary: List<CategoryExpenseSummary> = emptyList(),
    onNavigateToCategory: (CategoryType, CategoryExpenseSummary) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    val activeList = if (selectedType == CategoryType.EXPENSE) {
        categoriesSummary
    } else {
        incomeCategoriesSummary
    }

    var selectedCategory by remember(activeList, selectedType) {
        mutableStateOf(activeList.firstOrNull())
    }
    val totalAmount = activeList.sumOf { it.spentAmount }
    val isExpense = selectedType == CategoryType.EXPENSE

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isExpense) {
                    "Distribución de gastos por categoría"
                } else {
                    "Distribución de ingresos por categoría"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Toca una sección para ver su tooltip y navegar al detalle",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector Gastos vs Ingresos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { selectedType = CategoryType.EXPENSE },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isExpense) ExpenseRed.copy(alpha = 0.22f) else Color.Transparent,
                    border = if (isExpense) BorderStroke(1.dp, ExpenseRed) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gastos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = { selectedType = CategoryType.INCOME },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isExpense) IncomeGreen.copy(alpha = 0.22f) else Color.Transparent,
                    border = if (!isExpense) BorderStroke(1.dp, IncomeGreen) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingresos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpense) {
                            "Sin gastos registrados en el rango seleccionado."
                        } else {
                            "Sin ingresos registrados en el rango seleccionado."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Box(
                        modifier = Modifier.size(144.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(128.dp)
                                .pointerInput(activeList, selectedType) {
                                    detectTapGestures { offset ->
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val dx = offset.x - cx
                                        val dy = offset.y - cy
                                        var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        touchAngle = (touchAngle + 90f + 360f) % 360f

                                        var cumulative = 0f
                                        for (item in activeList) {
                                            val sweep = (item.percentageOfTotal * 360.0 / 100.0).toFloat()
                                            if (touchAngle >= cumulative && touchAngle <= cumulative + sweep) {
                                                if (selectedCategory?.categoryId == item.categoryId) {
                                                    onNavigateToCategory(selectedType, item)
                                                } else {
                                                    selectedCategory = item
                                                }
                                                break
                                            }
                                            cumulative += sweep
                                        }
                                    }
                                }
                        ) {
                            var currentStartAngle = -90f
                            val strokeWidth = 22.dp.toPx()

                            activeList.forEach { item ->
                                val sweepAngle = (item.percentageOfTotal * 360f / 100f).toFloat()
                                val sliceColor = CategoryIconMapper.parseColor(item.colorHex)
                                val isSelected = selectedCategory?.categoryId == item.categoryId

                                drawArc(
                                    color = if (isSelected) sliceColor else sliceColor.copy(alpha = 0.78f),
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(
                                        width = if (isSelected) strokeWidth * 1.25f else strokeWidth,
                                        cap = StrokeCap.Butt
                                    )
                                )
                                currentStartAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = CurrencyFormatter.formatCrc(totalAmount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isExpense) "Total gastos" else "Total ingresos",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        activeList.take(6).forEach { item ->
                            val sliceColor = CategoryIconMapper.parseColor(item.colorHex)
                            val isSelected = selectedCategory?.categoryId == item.categoryId
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) sliceColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCategory = item }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(sliceColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.categoryName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${String.format("%.0f", item.percentageOfTotal)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = sliceColor
                                )
                            }
                        }
                    }
                }

                selectedCategory?.let { sel ->
                    val selColor = CategoryIconMapper.parseColor(sel.colorHex)
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = selColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, selColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCategory(selectedType, sel) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tooltip • ${sel.categoryName}: ${CurrencyFormatter.formatCrc(sel.spentAmount)} (${String.format("%.1f", sel.percentageOfTotal)}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Toca aquí para ver todas las transacciones de ${sel.categoryName} →",
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
        }
    }
}
