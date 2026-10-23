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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.domain.model.CategoryExpenseSummary
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import kotlin.math.atan2

@Composable
fun CategoryPieChart(
    categoriesSummary: List<CategoryExpenseSummary>,
    onNavigateToCategory: (CategoryExpenseSummary) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (categoriesSummary.isEmpty()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin gastos por categoría en el rango seleccionado.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var selectedCategory by remember(categoriesSummary) {
        mutableStateOf(categoriesSummary.firstOrNull())
    }
    val totalSpent = categoriesSummary.sumOf { it.spentAmount }

    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución de gastos por categoría",
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

            Spacer(modifier = Modifier.height(16.dp))

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
                            .pointerInput(categoriesSummary) {
                                detectTapGestures { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = offset.x - cx
                                    val dy = offset.y - cy
                                    var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    touchAngle = (touchAngle + 90f + 360f) % 360f

                                    var cumulative = 0f
                                    for (item in categoriesSummary) {
                                        val sweep = (item.percentageOfTotal * 360.0 / 100.0).toFloat()
                                        if (touchAngle >= cumulative && touchAngle <= cumulative + sweep) {
                                            if (selectedCategory?.categoryId == item.categoryId) {
                                                onNavigateToCategory(item)
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

                        categoriesSummary.forEach { item ->
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
                            text = CurrencyFormatter.formatCrc(totalSpent),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total gastos",
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
                    categoriesSummary.take(6).forEach { item ->
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
                        .clickable { onNavigateToCategory(sel) }
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
