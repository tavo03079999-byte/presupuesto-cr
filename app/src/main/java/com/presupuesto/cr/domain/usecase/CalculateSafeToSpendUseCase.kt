package com.presupuesto.cr.domain.usecase

import com.presupuesto.cr.data.local.entity.AccountType
import com.presupuesto.cr.data.repository.FinanceRepository
import com.presupuesto.cr.domain.model.FinancialHealthStatus
import com.presupuesto.cr.domain.model.SafeToSpendResult
import com.presupuesto.cr.util.CurrencyFormatter
import com.presupuesto.cr.util.DateUtils
import kotlinx.coroutines.flow.first

class CalculateSafeToSpendUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(): SafeToSpendResult {
        // 1. Saldo Total Líquido en Cuentas y Efectivo habilitadas para gasto diario
        val accounts = repository.getAllAccounts().first()
        val liquidAccountsBalance = accounts
            .filter { it.isIncludedInSafeToSpend && it.accountType != AccountType.CREDIT_CARD_BCR }
            .sumOf { it.currentBalance }

        // 2. Gastos Fijos Recurrentes del Periodo Pendientes de Pago (Diezmo, Servicios, Gym, etc.)
        val pendingFixedExpenses = repository.getPendingRecurringExpensesSync()

        // 3. Aportes Programados a Metas de Ahorro / Inversión del Periodo
        val pendingScheduledSavings = repository.getPendingScheduledSavingsSync()

        // 4. Cuotas / Saldo adeudado de Tarjetas de Crédito BCR Próximas a Vencer
        val upcomingCreditCardDebt = accounts
            .filter { it.accountType == AccountType.CREDIT_CARD_BCR }
            .sumOf { if (it.currentBalance < 0) kotlin.math.abs(it.currentBalance) else 0.0 }

        // 5. Algoritmo de "Dinero Libre Real"
        // [ Dinero Libre hoy ] = (Saldo Total) - (Gastos Fijos Pendientes) - (Metas Ahorro) - (Cuotas/Tarjetas BCR)
        val totalSafeToSpendFortnight = (liquidAccountsBalance - pendingFixedExpenses - pendingScheduledSavings - upcomingCreditCardDebt)
            .coerceAtLeast(0.0)

        // Días restantes en la quincena actual (o mínimo 1 para evitar división entre cero)
        val daysRemaining = DateUtils.getRemainingDaysInCurrentFortnight().coerceAtLeast(1)

        val dailySafeToSpend = (totalSafeToSpendFortnight / daysRemaining)

        // Evaluación de Salud Financiera
        val (healthStatus, healthMessage) = when {
            dailySafeToSpend >= 18000.0 -> {
                FinancialHealthStatus.EXCELLENT to "¡Excelente salud financiera! Tienes holgura de ${CurrencyFormatter.formatCrc(dailySafeToSpend)} diarios sin comprometer tus obligaciones quincenales ni pagos BCR."
            }
            dailySafeToSpend in 7000.0..17999.99 -> {
                FinancialHealthStatus.STABLE to "Presupuesto estable. Tu disponible seguro es de ${CurrencyFormatter.formatCrc(dailySafeToSpend)} diarios. Mantén este ritmo para cumplir tus metas de ahorro."
            }
            dailySafeToSpend in 2000.0..6999.99 -> {
                FinancialHealthStatus.WARNING to "Atención: Tienes ${CurrencyFormatter.formatCrc(dailySafeToSpend)} por día. Modera gastos hormiga para no rozar tus compromisos fijos."
            }
            else -> {
                FinancialHealthStatus.CRITICAL to "Alerta crítica de liquidez: Solo dispones de ${CurrencyFormatter.formatCrc(dailySafeToSpend)} diarios. Prioriza cubrir tus gastos fijos y tarjetas pendientes."
            }
        }

        return SafeToSpendResult(
            totalAvailableInAccounts = liquidAccountsBalance,
            pendingFixedExpenses = pendingFixedExpenses,
            pendingScheduledSavings = pendingScheduledSavings,
            upcomingCreditCardDebt = upcomingCreditCardDebt,
            totalSafeToSpendFortnight = totalSafeToSpendFortnight,
            safeToSpendDailyToday = dailySafeToSpend,
            daysRemainingInPeriod = daysRemaining,
            healthStatus = healthStatus,
            healthMessage = healthMessage
        )
    }
}
