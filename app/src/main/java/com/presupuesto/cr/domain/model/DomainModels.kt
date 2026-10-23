package com.presupuesto.cr.domain.model

enum class FinancialHealthStatus {
    EXCELLENT,
    STABLE,
    WARNING,
    CRITICAL
}

enum class BudgetAlertLevel {
    NORMAL,
    WARNING_80,
    EXCEEDED_100
}

data class CategoryBudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColorHex: String,
    val iconName: String,
    val spentAmount: Double,
    val limitAmount: Double,
    val remainingAmount: Double,
    val percentageUsed: Double,
    val alertLevel: BudgetAlertLevel
)

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
    val periodDays: Int,
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
