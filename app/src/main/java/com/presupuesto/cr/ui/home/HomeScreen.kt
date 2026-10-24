package com.presupuesto.cr.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.R
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.domain.model.BudgetAlertLevel
import com.presupuesto.cr.domain.model.CategoryBudgetProgress
import com.presupuesto.cr.domain.model.CategoryExpenseSummary
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.domain.model.SafeToSpendResult
import com.presupuesto.cr.ui.home.components.QuickWalletRow
import com.presupuesto.cr.ui.theme.CyanNeon
import com.presupuesto.cr.ui.theme.EmeraldNeon
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.ui.theme.WarningOrange
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: (CategoryType?, Long?) -> Unit = { _, _ -> },
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val safeToSpend by viewModel.safeToSpend.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val pendingBcrCount by viewModel.pendingBcrCount.collectAsState()
    val quickForecast by viewModel.quickForecast.collectAsState()
    val monthIncome by viewModel.currentMonthIncome.collectAsState()
    val monthExpense by viewModel.currentMonthExpense.collectAsState()
    val monthCategories by viewModel.monthCategoriesSummary.collectAsState()
    val monthIncomeCategories by viewModel.monthIncomeCategoriesSummary.collectAsState()
    val budgetProgressList by viewModel.budgetProgressList.collectAsState()
    val dailyBars by viewModel.dailyBarsThisMonth.collectAsState()
    val totalBalance by viewModel.netBalance.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTransactions(null, null) },
                containerColor = ThemeBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva transacción")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FinoraTopHeader(
                    pendingBcrCount = pendingBcrCount,
                    onBcrClick = { onNavigateToTransactions(null, null) },
                    onSettingsClick = onNavigateToSettings
                )
            }

            item {
                FinoraHeroBalanceCard(
                    totalBalance = totalBalance,
                    safeToSpend = safeToSpend,
                    onClick = { onNavigateToTransactions(null, null) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                FinoraIncomeExpenseRow(
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    onIncomeClick = { onNavigateToTransactions(CategoryType.INCOME, null) },
                    onExpenseClick = { onNavigateToTransactions(CategoryType.EXPENSE, null) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                QuickWalletRow(
                    accounts = accounts,
                    onAccountClick = { onNavigateToTransactions(null, null) }
                )
            }

            item {
                FinoraMonthlyBarCard(
                    dailyBars = dailyBars,
                    onClick = onNavigateToReports,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (monthCategories.isNotEmpty() || monthIncomeCategories.isNotEmpty()) {
                item {
                    FinoraDonutSummaryCard(
                        expenseCategoriesSummary = monthCategories,
                        incomeCategoriesSummary = monthIncomeCategories,
                        totalExpense = monthExpense,
                        totalIncome = monthIncome,
                        onCategoryClick = { chosenType, catId -> onNavigateToTransactions(chosenType, catId) },
                        onHeaderClick = onNavigateToReports,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (budgetProgressList.isNotEmpty()) {
                item {
                    FinoraBudgetsCard(
                        budgets = budgetProgressList,
                        onAddOrManageClick = onNavigateToSettings,
                        onBudgetClick = { catId -> onNavigateToTransactions(CategoryType.EXPENSE, catId) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                FinoraRecentMovementsCard(
                    transactions = allTransactions.take(5),
                    categories = categories,
                    onSeeAllClick = { onNavigateToTransactions(null, null) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                FinoraSmartInsightsAndFutureCard(
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    budgets = budgetProgressList,
                    forecast = quickForecast,
                    onClick = onNavigateToReports,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FinoraTopHeader(
    pendingBcrCount: Int,
    onBcrClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.saldox_mark),
                contentDescription = "Logo SaldoX",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = CyanNeon.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Saldo")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF00E5B8),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("X")
                        }
                    },
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tu dinero, en perspectiva",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(
                badge = {
                    if (pendingBcrCount > 0) {
                        Badge(containerColor = ExpenseRed) {
                            Text(text = "$pendingBcrCount", color = Color.White)
                        }
                    }
                }
            ) {
                IconButton(onClick = onBcrClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Bandeja BCR",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = ThemeBlue,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun FinoraHeroBalanceCard(
    totalBalance: Double,
    safeToSpend: SafeToSpendResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ThemeBlue.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            ThemeBlue.copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saldo actual",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.formatCrc(totalBalance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (safeToSpend != null) {
                                "Libre hoy: ${CurrencyFormatter.formatCrc(safeToSpend.safeToSpendDailyToday)} / día"
                            } else {
                                "Balance consolidado activo"
                            },
                            fontSize = 12.sp,
                            color = IncomeGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Canvas(modifier = Modifier.size(92.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2.2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyanNeon.copy(alpha = 0.25f), Color.Transparent),
                            center = center,
                            radius = radius * 1.15f
                        ),
                        radius = radius * 1.15f,
                        center = center
                    )

                    drawCircle(
                        color = CyanNeon.copy(alpha = 0.45f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val barHeights = listOf(0.25f, 0.38f, 0.52f, 0.68f, 0.85f)
                    val barW = 6.dp.toPx()
                    val gap = 4.dp.toPx()
                    val startX = center.x - ((barHeights.size * (barW + gap)) / 2f)
                    val baseLineY = center.y + radius * 0.55f

                    barHeights.forEachIndexed { idx, h ->
                        val bh = radius * 1.05f * h
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(EmeraldNeon, ThemeBlue)),
                            topLeft = Offset(startX + idx * (barW + gap), baseLineY - bh),
                            size = Size(barW, bh),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    val trendPath = Path().apply {
                        moveTo(startX, baseLineY - radius * 0.28f)
                        quadraticBezierTo(
                            center.x,
                            baseLineY - radius * 0.45f,
                            startX + 4 * (barW + gap) + barW,
                            baseLineY - radius * 0.95f
                        )
                    }
                    drawPath(
                        path = trendPath,
                        color = EmeraldNeon,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinoraIncomeExpenseRow(
    monthIncome: Double,
    monthExpense: Double,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savingsRate = if (monthIncome > 0) {
        (((monthIncome - monthExpense) / monthIncome) * 100.0).coerceIn(-99.0, 100.0)
    } else {
        0.0
    }
    val expenseRatio = if (monthIncome > 0) {
        ((monthExpense / monthIncome) * 100.0).coerceIn(0.0, 999.0)
    } else {
        0.0
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            onClick = onIncomeClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(IncomeGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ingresos",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = CurrencyFormatter.formatCrc(monthIncome),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "↑ ${String.format("%.0f", savingsRate.coerceAtLeast(0.0))}% margen",
                    fontSize = 11.sp,
                    color = IncomeGreen,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Card(
            onClick = onExpenseClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ExpenseRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gastos",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = CurrencyFormatter.formatCrc(monthExpense),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "↓ ${String.format("%.0f", expenseRatio)}% de ingresos",
                    fontSize = 11.sp,
                    color = ExpenseRed,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FinoraMonthlyBarCard(
    dailyBars: List<Pair<Int, Double>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyBars.isEmpty()) return
    val maxVal = dailyBars.maxOf { it.second }.coerceAtLeast(1000.0)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen del mes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Este mes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyBars.forEachIndexed { index, (day, amount) ->
                    val fraction = if (amount > 0.0) {
                        (amount / maxVal).toFloat().coerceIn(0.18f, 1f)
                    } else {
                        0.06f
                    }
                    val brush = when {
                        amount <= 0.0 -> Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        index % 2 == 0 -> Brush.verticalGradient(listOf(CyanNeon, ThemeBlue))
                        else -> Brush.verticalGradient(listOf(EmeraldNeon, IncomeGreen))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(fraction)
                                    .background(brush, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (day in listOf(1, 5, 10, 15, 20, 25, 30)) {
                            Text(
                                text = "$day",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(13.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinoraDonutSummaryCard(
    expenseCategoriesSummary: List<CategoryExpenseSummary>,
    incomeCategoriesSummary: List<CategoryExpenseSummary>,
    totalExpense: Double,
    totalIncome: Double,
    onCategoryClick: (CategoryType, Long) -> Unit,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember {
        mutableStateOf(
            if (expenseCategoriesSummary.isNotEmpty() || incomeCategoriesSummary.isEmpty()) {
                CategoryType.EXPENSE
            } else {
                CategoryType.INCOME
            }
        )
    }
    val isExpense = selectedType == CategoryType.EXPENSE
    val activeList = if (isExpense) expenseCategoriesSummary else incomeCategoriesSummary
    val activeTotal = if (isExpense) totalExpense else totalIncome

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpense) "Categorías de gasto" else "Categorías de ingreso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Este mes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.padding(vertical = 7.dp),
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
                        modifier = Modifier.padding(vertical = 7.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpense) "Sin gastos este mes." else "Sin ingresos este mes.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(116.dp)) {
                            var startAngle = -90f
                            val stroke = 20.dp.toPx()
                            activeList.forEach { item ->
                                val sweep = (item.percentageOfTotal * 360.0 / 100.0).toFloat()
                                drawArc(
                                    color = CategoryIconMapper.parseColor(item.colorHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = CurrencyFormatter.formatCrc(activeTotal),
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeList.take(6).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategoryClick(selectedType, item.categoryId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(CategoryIconMapper.parseColor(item.colorHex), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.categoryName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${String.format("%.0f", item.percentageOfTotal)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinoraBudgetsCard(
    budgets: List<CategoryBudgetProgress>,
    onAddOrManageClick: () -> Unit,
    onBudgetClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Presupuestos por categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddOrManageClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Configurar presupuestos",
                        tint = ThemeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                budgets.forEach { b ->
                    val barColor = when (b.alertLevel) {
                        BudgetAlertLevel.EXCEEDED_100 -> ExpenseRed
                        BudgetAlertLevel.WARNING_80 -> WarningOrange
                        BudgetAlertLevel.NORMAL -> CategoryIconMapper.parseColor(b.categoryColorHex)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBudgetClick(b.categoryId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(barColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(b.iconName),
                                    contentDescription = null,
                                    tint = barColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = b.categoryName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${String.format("%.0f", b.percentageUsed)}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = barColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (b.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${CurrencyFormatter.formatCrc(b.spentAmount)} / ${CurrencyFormatter.formatCrc(b.limitAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (b.alertLevel == BudgetAlertLevel.EXCEEDED_100) "¡Límite superado!"
                                        else if (b.alertLevel == BudgetAlertLevel.WARNING_80) "Alerta 80%+"
                                        else "Quedan ${CurrencyFormatter.formatCrc(b.remainingAmount)}",
                                        fontSize = 10.sp,
                                        color = barColor,
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
}

@Composable
private fun FinoraRecentMovementsCard(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ver todos",
                    fontSize = 12.sp,
                    color = ThemeBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "Aún no hay movimientos registrados.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                transactions.forEachIndexed { idx, tx ->
                    val cat = categories.find { it.id == tx.categoryId }
                    val catColor = CategoryIconMapper.parseColor(cat?.colorHex ?: "#2196F3")
                    val amountColor = if (tx.type == CategoryType.INCOME) IncomeGreen else ExpenseRed
                    val prefix = if (tx.type == CategoryType.INCOME) "+" else "-"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeeAllClick() }
                            .padding(vertical = 8.dp),
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
                                    .background(catColor.copy(alpha = 0.18f), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(cat?.iconName ?: "Category"),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.note.ifBlank { cat?.name ?: "Movimiento" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = DateUtils.formatFriendly(tx.timestamp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "$prefix${CurrencyFormatter.formatCrc(tx.amount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = amountColor
                        )
                    }
                    if (idx < transactions.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinoraSmartInsightsAndFutureCard(
    monthIncome: Double,
    monthExpense: Double,
    budgets: List<CategoryBudgetProgress>,
    forecast: ForecastResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warningBudget = budgets.firstOrNull {
        it.alertLevel == BudgetAlertLevel.EXCEEDED_100 || it.alertLevel == BudgetAlertLevel.WARNING_80
    }
    val savingsPct = if (monthIncome > 0) {
        (((monthIncome - monthExpense) / monthIncome) * 100.0).coerceAtLeast(0.0)
    } else {
        0.0
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ThemeBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(ThemeBlue.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Análisis Inteligente y Futuro Financiero",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Últimos 30 días y proyección a 3 meses",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tu margen de ahorro actual es del ${String.format("%.0f", savingsPct)}% de tus ingresos.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (warningBudget != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atención en ${warningBudget.categoryName}: llevas el ${String.format("%.0f", warningBudget.percentageUsed)}% del presupuesto mensual.",
                        fontSize = 12.sp,
                        color = WarningOrange
                    )
                }
            }

            if (forecast != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Si mantienes este ritmo en 3 meses:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCrc(forecast.finalBalance),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Ver Reportes →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeBlue
                        )
                    }
                }
            }
        }
    }
}
