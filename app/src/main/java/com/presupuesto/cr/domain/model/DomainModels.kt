package com.presupuesto.cr.domain.model

enum class FinancialHealthStatus {
    EXCELLENT,   // Safe-to-spend holgado (> ₡15.000 / día)
    STABLE,      // Safe-to-spend moderado (₡5.000 - ₡15.000 / día)
    WARNING,     // Safe-to-spend ajustado (₡1.000 - ₡5.000 / día)
    CRITICAL     // En riesgo de déficit (< ₡1.000 o negativo)
}

data class SafeToSpendResult(
    val totalAvailableInAccounts: Double,
    val pendingFixedExpenses: Double,
    val pendingScheduledSavings: Double,
    val upcomingCreditCardDebt: Double,
    val totalSafeToSpendFortnight: Double,
    val safeToSpendDailyToday: Double,
    val daysRemainingInPeriod: Int,
    val healthStatus: FinancialHealthStatus,
    val healthMessage: String
)

data class ForecastPoint(
    val dayIndex: Int,
    val dateTimestamp: Long,
    val formattedDate: String,
    val projectedBalance: Double,
    val isCriticalValley: Boolean = false,
    val eventDescription: String? = null
)

data class ForecastResult(
    val periodDays: Int, // 90 días (3 meses) o 180 días (6 meses)
    val points: List<ForecastPoint>,
    val minProjectedBalance: Double,
    val maxProjectedBalance: Double,
    val finalBalance: Double,
    val criticalAlerts: List<String>
)

data class CategoryExpenseSummary(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val spentAmount: Double,
    val percentageOfTotal: Double
)
