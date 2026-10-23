package com.presupuesto.cr.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringFrequency
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import com.presupuesto.cr.data.local.entity.TransactionSource
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.util.DateUtils
import com.presupuesto.cr.worker.PaymentReminderWorker
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val context: Context,
    private val repository: FinanceRepository
) : ViewModel() {

    private val initialCal = Calendar.getInstance()

    private val _displayedYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    val displayedYear: StateFlow<Int> = _displayedYear.asStateFlow()

    private val _displayedMonth = MutableStateFlow(initialCal.get(Calendar.MONTH))
    val displayedMonth: StateFlow<Int> = _displayedMonth.asStateFlow()

    private val _selectedDayOfMonth = MutableStateFlow<Int?>(null)
    val selectedDayOfMonth: StateFlow<Int?> = _selectedDayOfMonth.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val reminders: StateFlow<List<RecurringMovementEntity>> = repository.getAllRecurringMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPendingMonthAmount: StateFlow<Double> = reminders
        .map { list ->
            list.filter { it.isActive && !it.isPaidForCurrentPeriod && it.type == CategoryType.EXPENSE }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingRemindersCount: StateFlow<Int> = reminders
        .map { list ->
            list.count { it.isActive && !it.isPaidForCurrentPeriod }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _displayedYear.value)
            set(Calendar.MONTH, _displayedMonth.value)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        _displayedYear.value = cal.get(Calendar.YEAR)
        _displayedMonth.value = cal.get(Calendar.MONTH)
        _selectedDayOfMonth.value = null
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _displayedYear.value)
            set(Calendar.MONTH, _displayedMonth.value)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }
        _displayedYear.value = cal.get(Calendar.YEAR)
        _displayedMonth.value = cal.get(Calendar.MONTH)
        _selectedDayOfMonth.value = null
    }

    fun selectDay(day: Int?) {
        _selectedDayOfMonth.value = if (_selectedDayOfMonth.value == day) null else day
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun addPaymentReminder(
        name: String,
        amount: Double,
        type: CategoryType,
        categoryId: Long,
        accountId: Long,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        repeatMonthly: Boolean,
        reminderOffsetHours: Int
    ) {
        viewModelScope.launch {
            try {
                val dueCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, _displayedYear.value)
                    set(Calendar.MONTH, _displayedMonth.value)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
                    set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
                    set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis < System.currentTimeMillis() && repeatMonthly) {
                        add(Calendar.MONTH, 1)
                    }
                }

                val entity = RecurringMovementEntity(
                    name = name.trim(),
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    frequency = if (repeatMonthly) RecurringFrequency.MONTHLY else RecurringFrequency.ANNUAL,
                    dayOfMonth = dayOfMonth.coerceIn(1, 31),
                    secondDayOfMonth = null,
                    nextDueDate = dueCal.timeInMillis,
                    isActive = true,
                    isPaidForCurrentPeriod = false,
                    reminderOffsetHours = reminderOffsetHours
                )
                val insertedId = repository.insertRecurringMovement(entity)
                if (insertedId > 0) {
                    PaymentReminderWorker.scheduleReminder(
                        context = context,
                        reminderId = insertedId,
                        name = entity.name,
                        amount = entity.amount,
                        dueDateMillis = dueCal.timeInMillis,
                        offsetHours = reminderOffsetHours
                    )
                    _statusMessage.value = "Recordatorio \"${entity.name}\" programado correctamente."
                }
            } catch (e: Exception) {
                _statusMessage.value = "No se pudo guardar el recordatorio. Intenta nuevamente."
            }
        }
    }

    fun updateReminderOffset(movement: RecurringMovementEntity, offsetHours: Int) {
        viewModelScope.launch {
            try {
                val updated = movement.copy(
                    reminderOffsetHours = offsetHours,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateRecurringMovement(updated)
                PaymentReminderWorker.scheduleReminder(
                    context = context,
                    reminderId = updated.id,
                    name = updated.name,
                    amount = updated.amount,
                    dueDateMillis = updated.nextDueDate,
                    offsetHours = offsetHours
                )
            } catch (e: Exception) {
                _statusMessage.value = "No fue posible actualizar la alerta del recordatorio."
            }
        }
    }

    fun markReminderAsPaid(
        movement: RecurringMovementEntity,
        createLinkedTransaction: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.setRecurringPaidStatus(movement.id, true)
                PaymentReminderWorker.cancelReminder(context, movement.id)

                if (createLinkedTransaction) {
                    val fallbackAccountId = accounts.value.firstOrNull()?.id ?: movement.accountId
                    val tx = TransactionEntity(
                        accountId = fallbackAccountId,
                        categoryId = movement.categoryId,
                        amount = movement.amount,
                        type = movement.type,
                        timestamp = System.currentTimeMillis(),
                        note = "Pago programado: ${movement.name}",
                        merchant = movement.name,
                        source = TransactionSource.MANUAL
                    )
                    repository.insertTransaction(tx)

                    if (movement.type == CategoryType.EXPENSE) {
                        checkBudgetExceededAfterExpense(movement.categoryId)
                    }
                    _statusMessage.value = "Marcado como pagado y registrado en tus transacciones."
                } else {
                    _statusMessage.value = "Recordatorio marcado como pagado."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Ocurrió un inconveniente al actualizar el estado de pago."
            }
        }
    }

    fun markReminderAsPending(movement: RecurringMovementEntity) {
        viewModelScope.launch {
            try {
                repository.setRecurringPaidStatus(movement.id, false)
                PaymentReminderWorker.scheduleReminder(
                    context = context,
                    reminderId = movement.id,
                    name = movement.name,
                    amount = movement.amount,
                    dueDateMillis = movement.nextDueDate,
                    offsetHours = movement.reminderOffsetHours
                )
            } catch (e: Exception) {
                _statusMessage.value = "No se pudo cambiar el estado a pendiente."
            }
        }
    }

    fun deleteReminder(movement: RecurringMovementEntity) {
        viewModelScope.launch {
            try {
                PaymentReminderWorker.cancelReminder(context, movement.id)
                repository.deleteRecurringMovement(movement)
                _statusMessage.value = "Recordatorio eliminado."
            } catch (e: Exception) {
                _statusMessage.value = "No se pudo eliminar el recordatorio."
            }
        }
    }

    private suspend fun checkBudgetExceededAfterExpense(categoryId: Long) {
        try {
            val budget = repository.getBudgetByCategory(categoryId) ?: return
            if (budget.limitAmount <= 0.0) return
            val (monthStart, monthEnd) = DateUtils.getCurrentMonthBounds()
            val spent = repository.getCategorySpentBetweenSync(categoryId, monthStart, monthEnd)
            if (spent >= budget.limitAmount) {
                val categoryName = repository.getCategoryById(categoryId)?.name ?: "Categoría"
                PaymentReminderWorker.triggerBudgetExceededNotification(
                    context = context,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    spentAmount = spent,
                    limitAmount = budget.limitAmount
                )
            }
        } catch (_: Exception) {
        }
    }
}
