package com.presupuesto.cr.domain.usecase

import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringFrequency
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.ForecastPoint
import com.presupuesto.cr.domain.model.ForecastResult
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ForecastEngineUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(forecastMonths: Int = 3): ForecastResult {
        val totalDays = if (forecastMonths == 6) 180 else 90

        // 1. Obtener saldo inicial consolidado
        val currentAccounts = repository.getAllAccounts().first()
        var runningBalance = currentAccounts.sumOf { it.currentBalance }

        // 2. Calcular velocidad histórica de gasto diario variable (últimos 30 días excluyendo fijos)
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val recentTransactions = repository.getTransactionsBetweenSync(thirtyDaysAgo, now)
        val recentVariableExpenses = recentTransactions
            .filter { it.type == CategoryType.EXPENSE && !it.isRecurring }
            .sumOf { it.amount }

        // Gasto variable diario promedio histórico (fallback prudencial de ₡7.500 si aún no hay historial)
        val averageDailyVariableExpense = if (recentVariableExpenses > 0) {
            recentVariableExpenses / 30.0
        } else {
            7500.0
        }

        // 3. Obtener movimientos recurrentes activos (Salarios, Alquiler, Diezmo, Servicios, etc.)
        val recurringMovements = repository.getActiveRecurringMovements().first()

        val forecastPoints = mutableListOf<ForecastPoint>()
        val criticalAlerts = mutableListOf<String>()

        var minBalance = runningBalance
        var maxBalance = runningBalance

        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
        }

        for (day in 1..totalDays) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val timestamp = calendar.timeInMillis
            val formattedDate = DateUtils.formatShort(timestamp)

            var dailyEventDescription: String? = null

            // A. Aplicar gastos variables proyectados
            runningBalance -= averageDailyVariableExpense

            // B. Aplicar ingresos y gastos recurrentes según fecha del calendario
            for (recurring in recurringMovements) {
                var appliesToday = false
                when (recurring.frequency) {
                    RecurringFrequency.WEEKLY -> {
                        // Aplica cada 7 días
                        if (day % 7 == 0) appliesToday = true
                    }
                    RecurringFrequency.FORTNIGHTLY_15_30 -> {
                        // Típico pago quincenal costarricense: días 15 y último día del mes (30 o 31)
                        if (currentDayOfMonth == 15 || currentDayOfMonth == maxDayOfMonth || (recurring.secondDayOfMonth != null && currentDayOfMonth == recurring.secondDayOfMonth)) {
                            appliesToday = true
                        }
                    }
                    RecurringFrequency.MONTHLY -> {
                        if (currentDayOfMonth == recurring.dayOfMonth || (recurring.dayOfMonth > maxDayOfMonth && currentDayOfMonth == maxDayOfMonth)) {
                            appliesToday = true
                        }
                    }
                    RecurringFrequency.ANNUAL -> {
                        // No impacta periodo corto salvo aguinaldo en diciembre
                        if (calendar.get(Calendar.MONTH) == Calendar.DECEMBER && currentDayOfMonth == 15 && recurring.type == CategoryType.INCOME) {
                            appliesToday = true
                        }
                    }
                }

                if (appliesToday) {
                    when (recurring.type) {
                        CategoryType.INCOME -> {
                            runningBalance += recurring.amount
                            dailyEventDescription = "+ ${recurring.name} (${CurrencyFormatter.formatCrc(recurring.amount)})"
                        }
                        CategoryType.EXPENSE, CategoryType.SPECIAL_ALLOCATION -> {
                            runningBalance -= recurring.amount
                            dailyEventDescription = "- ${recurring.name} (${CurrencyFormatter.formatCrc(recurring.amount)})"
                        }
                    }
                }
            }

            if (runningBalance < minBalance) minBalance = runningBalance
            if (runningBalance > maxBalance) maxBalance = runningBalance

            // C. Detección de Valle Crítico (saldo < ₡25.000)
            val isCritical = runningBalance < 25000.0
            if (isCritical && criticalAlerts.size < 4) {
                val alertMsg = "Alerta de Liquidez el $formattedDate: Saldo estimado cae a ${CurrencyFormatter.formatCrc(runningBalance)}. Se recomienda ajustar compras no esenciales previas."
                if (!criticalAlerts.contains(alertMsg)) {
                    criticalAlerts.add(alertMsg)
                }
            }

            forecastPoints.add(
                ForecastPoint(
                    dayIndex = day,
                    dateTimestamp = timestamp,
                    formattedDate = formattedDate,
                    projectedBalance = runningBalance,
                    isCriticalValley = isCritical,
                    eventDescription = dailyEventDescription
                )
            )
        }

        return ForecastResult(
            periodDays = totalDays,
            points = forecastPoints,
            minProjectedBalance = minBalance,
            maxProjectedBalance = maxBalance,
            finalBalance = runningBalance,
            criticalAlerts = criticalAlerts
        )
    }
}
