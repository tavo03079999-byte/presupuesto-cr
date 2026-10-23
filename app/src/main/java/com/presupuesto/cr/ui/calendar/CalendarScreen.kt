package com.presupuesto.cr.ui.calendar

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringFrequency
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.ui.theme.CyanNeon
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.IncomeGreen
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.ui.theme.WarningOrange
import com.presupuesto.cr.util.CategoryIconMapper
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils
import java.util.Calendar

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    val reminders by viewModel.reminders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val displayedYear by viewModel.displayedYear.collectAsState()
    val displayedMonth by viewModel.displayedMonth.collectAsState()
    val selectedDay by viewModel.selectedDayOfMonth.collectAsState()
    val totalPendingAmount by viewModel.totalPendingMonthAmount.collectAsState()
    val pendingCount by viewModel.pendingRemindersCount.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var showAddReminderDialog by remember { mutableStateOf(false) }
    var reminderToMarkPaid by remember { mutableStateOf<RecurringMovementEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<RecurringMovementEntity?>(null) }

    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    val displayedReminders = remember(reminders, selectedDay) {
        if (selectedDay == null) {
            reminders.sortedBy { it.dayOfMonth }
        } else {
            reminders.filter {
                it.dayOfMonth == selectedDay || it.secondDayOfMonth == selectedDay
            }.sortedBy { it.dayOfMonth }
        }
    }

    val paidCount = remember(reminders) { reminders.count { it.isActive && it.isPaidForCurrentPeriod } }
    val totalActiveCount = (paidCount + pendingCount).coerceAtLeast(1)
    val paidRatio = if (reminders.isEmpty()) 0f else (paidCount.toFloat() / totalActiveCount.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReminderDialog = true },
                containerColor = ThemeBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Recordatorio")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Calendario de Pagos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Recordatorios mensuales, vencimientos y alertas WorkManager",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (statusMessage != null) {
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
                                text = statusMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                Text("OK", fontWeight = FontWeight.Bold, color = ThemeBlue)
                            }
                        }
                    }
                }
            }

            // Resumen rápido del total de pagos pendientes del mes
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total pendiente del mes",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatCrc(totalPendingAmount),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalPendingAmount > 0.0) ExpenseRed else IncomeGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = if (pendingCount > 0) WarningOrange.copy(alpha = 0.16f) else IncomeGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (pendingCount > 0) "$pendingCount pendientes" else "Al día",
                                    color = if (pendingCount > 0) WarningOrange else IncomeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { paidRatio },
                            color = IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pagados: $paidCount",
                                fontSize = 11.sp,
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Por cubrir: $pendingCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cuadrícula mensual interactiva
            item {
                MonthlyCalendarGridCard(
                    year = displayedYear,
                    monthZeroBased = displayedMonth,
                    selectedDay = selectedDay,
                    reminders = reminders,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onSelectDay = { day -> viewModel.selectDay(day) }
                )
            }

            // Encabezado y filtro de lista de recordatorios
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDay != null) {
                            "Pagos del día $selectedDay"
                        } else {
                            "Pagos programados del mes"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedDay != null) {
                        TextButton(onClick = { viewModel.selectDay(null) }) {
                            Text("Ver todos", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = { showAddReminderDialog = true }) {
                            Text("+ Nuevo pago", color = ThemeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (displayedReminders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = ThemeBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedDay != null) {
                                    "No hay pagos programados para el día $selectedDay."
                                } else {
                                    "Aún no tienes recordatorios de pago registrados."
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        category = categoriesMap[reminder.categoryId],
                        onTogglePaidClick = {
                            if (reminder.isPaidForCurrentPeriod) {
                                viewModel.markReminderAsPending(reminder)
                            } else {
                                reminderToMarkPaid = reminder
                            }
                        },
                        onChangeOffset = { hours -> viewModel.updateReminderOffset(reminder, hours) },
                        onDeleteClick = { reminderToDelete = reminder }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        if (showAddReminderDialog) {
            val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            AddReminderDialog(
                defaultDay = selectedDay ?: todayDay,
                categories = categories,
                onConfirm = { name, amount, type, categoryId, day, hour, minute, repeatMonthly, offsetHours ->
                    viewModel.addPaymentReminder(
                        name = name,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accounts.firstOrNull()?.id ?: 1L,
                        dayOfMonth = day,
                        hourOfDay = hour,
                        minute = minute,
                        repeatMonthly = repeatMonthly,
                        reminderOffsetHours = offsetHours
                    )
                    showAddReminderDialog = false
                },
                onDismiss = { showAddReminderDialog = false }
            )
        }

        reminderToMarkPaid?.let { target ->
            MarkReminderPaidDialog(
                reminder = target,
                onConfirm = { createLinkedTx ->
                    viewModel.markReminderAsPaid(target, createLinkedTx)
                    reminderToMarkPaid = null
                },
                onDismiss = { reminderToMarkPaid = null }
            )
        }

        reminderToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { reminderToDelete = null },
                title = {
                    Text("Eliminar recordatorio", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("¿Estás seguro de que deseas eliminar el recordatorio \"${target.name}\"?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteReminder(target)
                            reminderToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { reminderToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun MonthlyCalendarGridCard(
    year: Int,
    monthZeroBased: Int,
    selectedDay: Int?,
    reminders: List<RecurringMovementEntity>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val isCurrentMonth = todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == monthZeroBased
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val calInfo = remember(year, monthZeroBased) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Lunes = 0 .. Domingo = 6
        val dayOfWeekJava = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmptyCells = (dayOfWeekJava + 5) % 7
        leadingEmptyCells to daysInMonth
    }

    val leadingEmpty = calInfo.first
    val daysInMonth = calInfo.second
    val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7
    val weekHeaders = listOf("L", "M", "K", "J", "V", "S", "D")

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                }

                Text(
                    text = DateUtils.formatMonthYear(year, monthZeroBased),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val rows = totalCells / 7
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingEmpty + 1
                        if (dayNumber in 1..daysInMonth) {
                            val dayReminders = reminders.filter {
                                it.isActive && (it.dayOfMonth == dayNumber || it.secondDayOfMonth == dayNumber)
                            }
                            val hasPending = dayReminders.any { !it.isPaidForCurrentPeriod }
                            val hasPaid = dayReminders.any { it.isPaidForCurrentPeriod }
                            val isSelected = selectedDay == dayNumber
                            val isToday = isCurrentMonth && todayDay == dayNumber

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> ThemeBlue
                                            hasPending -> ExpenseRed.copy(alpha = 0.12f)
                                            hasPaid -> IncomeGreen.copy(alpha = 0.12f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = if (isToday && !isSelected) CyanNeon else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectDay(dayNumber) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday || dayReminders.isNotEmpty()) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (dayReminders.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            if (hasPending) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(
                                                            if (isSelected) Color.White else ExpenseRed,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                            if (hasPaid) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(
                                                            if (isSelected) Color.White else IncomeGreen,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDotItem(color = ExpenseRed, label = "Pago pendiente")
                LegendDotItem(color = IncomeGreen, label = "Pagado")
                LegendDotItem(color = CyanNeon, label = "Hoy")
            }
        }
    }
}

@Composable
private fun LegendDotItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderItemCard(
    reminder: RecurringMovementEntity,
    category: CategoryEntity?,
    onTogglePaidClick: () -> Unit,
    onChangeOffset: (Int) -> Unit,
    onDeleteClick: () -> Unit
) {
    val amountColor = if (reminder.type == CategoryType.INCOME) IncomeGreen else ExpenseRed
    val catColor = CategoryIconMapper.parseColor(category?.colorHex ?: "#2196F3", ThemeBlue)
    val catIcon = CategoryIconMapper.getIcon(category?.iconName ?: "Category")
    val freqText = when (reminder.frequency) {
        RecurringFrequency.MONTHLY -> "Mensual"
        RecurringFrequency.FORTNIGHTLY_15_30 -> "Quincenal"
        RecurringFrequency.WEEKLY -> "Semanal"
        RecurringFrequency.ANNUAL -> "Único / Anual"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                    IconButton(onClick = onTogglePaidClick) {
                        Icon(
                            imageVector = if (reminder.isPaidForCurrentPeriod) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = "Marcar estado de pago",
                            tint = if (reminder.isPaidForCurrentPeriod) IncomeGreen else WarningOrange
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(catColor.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Día ${reminder.dayOfMonth}" +
                                (if (reminder.secondDayOfMonth != null) " y ${reminder.secondDayOfMonth}" else "") +
                                " • ${DateUtils.formatTime(reminder.nextDueDate)} • $freqText",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatCrc(reminder.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = amountColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (reminder.isPaidForCurrentPeriod) {
                            IncomeGreen.copy(alpha = 0.15f)
                        } else {
                            WarningOrange.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onTogglePaidClick() }
                    ) {
                        Text(
                            text = if (reminder.isPaidForCurrentPeriod) "Pagado" else "Pendiente",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isPaidForCurrentPeriod) IncomeGreen else WarningOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = ThemeBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    FilterChip(
                        selected = reminder.reminderOffsetHours == 24,
                        onClick = { onChangeOffset(24) },
                        label = { Text("1 día antes", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = reminder.reminderOffsetHours == 0,
                        onClick = { onChangeOffset(0) },
                        label = { Text("Mismo día", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = reminder.reminderOffsetHours == 1,
                        onClick = { onChangeOffset(1) },
                        label = { Text("1h antes", fontSize = 10.sp) }
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar recordatorio",
                        tint = ExpenseRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkReminderPaidDialog(
    reminder: RecurringMovementEntity,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var createLinkedTransaction by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Marcar pago como realizado",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${reminder.name} — ${CurrencyFormatter.formatCrc(reminder.amount)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Crear transacción vinculada",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Registra automáticamente el gasto en tus movimientos",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = createLinkedTransaction,
                            onCheckedChange = { createLinkedTransaction = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = IncomeGreen)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(createLinkedTransaction) },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
            ) {
                Text("Confirmar pago", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun AddReminderDialog(
    defaultDay: Int,
    categories: List<CategoryEntity>,
    onConfirm: (
        name: String,
        amount: Double,
        type: CategoryType,
        categoryId: Long,
        day: Int,
        hour: Int,
        minute: Int,
        repeatMonthly: Boolean,
        offsetHours: Int
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf(defaultDay.toString()) }
    var hourText by remember { mutableStateOf("09") }
    var minuteText by remember { mutableStateOf("00") }
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    val filteredCats = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }.ifEmpty { categories }
    }
    var selectedCategoryId by remember(filteredCats) {
        mutableLongStateOf(filteredCats.firstOrNull()?.id ?: 1L)
    }
    var repeatMonthly by remember { mutableStateOf(true) }
    var offsetHours by remember { mutableIntStateOf(24) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nuevo Recordatorio de Pago",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del pago (ej. Luz CNFL, Alquiler)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto (₡ CRC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it },
                        label = { Text("Día (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("Hora (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it },
                        label = { Text("Min (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (filteredCats.isNotEmpty()) {
                    Text(
                        text = "Categoría:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredCats, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = {
                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = ThemeBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Repetir cada mes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = repeatMonthly,
                        onCheckedChange = { repeatMonthly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ThemeBlue)
                    )
                }

                Text(
                    text = "Notificación (WorkManager):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = offsetHours == 24,
                        onClick = { offsetHours = 24 },
                        label = { Text("1 día antes", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = offsetHours == 0,
                        onClick = { offsetHours = 0 },
                        label = { Text("Mismo día", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = offsetHours == 1,
                        onClick = { offsetHours = 1 },
                        label = { Text("1h antes", fontSize = 10.5.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val day = dayText.toIntOrNull()?.coerceIn(1, 31) ?: defaultDay
                    val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
                    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    if (name.isNotBlank() && amount > 0.0) {
                        onConfirm(
                            name,
                            amount,
                            selectedType,
                            selectedCategoryId,
                            day,
                            hour,
                            minute,
                            repeatMonthly,
                            offsetHours
                        )
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
