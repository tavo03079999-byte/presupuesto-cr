package com.presupuesto.cr.data.repository

import com.presupuesto.cr.data.local.dao.BudgetWithCategory
import com.presupuesto.cr.data.local.dao.CategoryTotal
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import com.presupuesto.cr.data.local.entity.BudgetEntity
import com.presupuesto.cr.data.local.entity.CategoryEntity
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import com.presupuesto.cr.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    // Cuentas y Balances
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    fun getNetWorthFlow(): Flow<Double?>
    fun getSafeToSpendBalanceFlow(): Flow<Double?>
    suspend fun getSafeToSpendBalance(): Double
    suspend fun updateAccountBalance(accountId: Long, amountDelta: Double)
    suspend fun syncAccountBalancesWithTransactions()
    suspend fun insertAccount(account: AccountEntity): Long

    // Categorías Personalizadas
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)

    // Transacciones
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>
    suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity>
    fun getTotalByTypeBetween(type: CategoryType, startTime: Long, endTime: Long): Flow<Double?>
    suspend fun getTotalByTypeBetweenSync(type: CategoryType, startTime: Long, endTime: Long): Double
    suspend fun getCategorySpentBetweenSync(categoryId: Long, startTime: Long, endTime: Long): Double
    fun getExpenseSummaryByCategory(startTime: Long, endTime: Long): Flow<List<CategoryTotal>>
    fun getSummaryByCategoryAndType(type: CategoryType, startTime: Long, endTime: Long): Flow<List<CategoryTotal>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Recurrentes y Recordatorios de Pago
    fun getActiveRecurringMovements(): Flow<List<RecurringMovementEntity>>
    fun getAllRecurringMovements(): Flow<List<RecurringMovementEntity>>
    suspend fun getPendingMovementsForCurrentPeriod(): List<RecurringMovementEntity>
    fun getPendingRecurringExpensesFlow(): Flow<Double?>
    suspend fun getPendingRecurringExpensesSync(): Double
    fun getPendingScheduledSavingsFlow(): Flow<Double?>
    suspend fun getPendingScheduledSavingsSync(): Double
    suspend fun insertRecurringMovement(movement: RecurringMovementEntity): Long
    suspend fun updateRecurringMovement(movement: RecurringMovementEntity)
    suspend fun deleteRecurringMovement(movement: RecurringMovementEntity)
    suspend fun setRecurringPaidStatus(id: Long, isPaid: Boolean)

    // Presupuestos por Categoría
    fun getBudgetsWithCategories(): Flow<List<BudgetWithCategory>>
    suspend fun getBudgetByCategory(categoryId: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun saveMonthlyBudgetForCategory(categoryId: Long, limitAmount: Double)
    suspend fun deleteBudgetById(budgetId: Long)

    // Bandeja BCR
    fun getPendingBcrTransactions(): Flow<List<BcrPendingEntity>>
    fun getPendingBcrCountFlow(): Flow<Int>
    suspend fun insertBcrPending(entity: BcrPendingEntity): Long
    suspend fun markBcrAsProcessed(id: Long)
    suspend fun deleteBcrPending(entity: BcrPendingEntity)
}
