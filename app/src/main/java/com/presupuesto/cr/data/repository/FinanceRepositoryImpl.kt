package com.presupuesto.cr.data.repository

import com.presupuesto.cr.data.local.AppDatabase
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

class FinanceRepositoryImpl(
    private val database: AppDatabase
) : FinanceRepository {

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val recurringMovementDao = database.recurringMovementDao()
    private val budgetDao = database.budgetDao()
    private val bcrPendingDao = database.bcrPendingDao()

    // Cuentas y Balances
    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    override suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)
    override fun getNetWorthFlow(): Flow<Double?> = accountDao.getNetWorthFlow()
    override fun getSafeToSpendBalanceFlow(): Flow<Double?> = accountDao.getSafeToSpendBalanceFlow()
    override suspend fun getSafeToSpendBalance(): Double = accountDao.getSafeToSpendBalance() ?: 0.0
    override suspend fun updateAccountBalance(accountId: Long, amountDelta: Double) =
        accountDao.updateBalance(accountId, amountDelta)
    override suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    // Categorías
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    override suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
    override fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)
    override suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    // Transacciones
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    override fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startTime, endTime)
    override suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity> =
        transactionDao.getTransactionsBetweenSync(startTime, endTime)
    override fun getTotalByTypeBetween(type: CategoryType, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalByTypeBetween(type, startTime, endTime)
    override suspend fun getTotalByTypeBetweenSync(type: CategoryType, startTime: Long, endTime: Long): Double =
        transactionDao.getTotalByTypeBetweenSync(type, startTime, endTime) ?: 0.0
    override fun getExpenseSummaryByCategory(startTime: Long, endTime: Long): Flow<List<CategoryTotal>> =
        transactionDao.getExpenseSummaryByCategory(startTime, endTime)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // Impactar balance de la cuenta automáticamente
        val balanceDelta = when (transaction.type) {
            CategoryType.INCOME -> transaction.amount
            CategoryType.EXPENSE, CategoryType.SPECIAL_ALLOCATION -> -transaction.amount
        }
        accountDao.updateBalance(transaction.accountId, balanceDelta)
        return id
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        // Revertir balance
        val revertDelta = when (transaction.type) {
            CategoryType.INCOME -> -transaction.amount
            CategoryType.EXPENSE, CategoryType.SPECIAL_ALLOCATION -> transaction.amount
        }
        accountDao.updateBalance(transaction.accountId, revertDelta)
    }

    // Recurrentes
    override fun getActiveRecurringMovements(): Flow<List<RecurringMovementEntity>> =
        recurringMovementDao.getActiveMovements()
    override fun getAllRecurringMovements(): Flow<List<RecurringMovementEntity>> =
        recurringMovementDao.getAllMovements()
    override suspend fun getPendingMovementsForCurrentPeriod(): List<RecurringMovementEntity> =
        recurringMovementDao.getPendingMovementsForCurrentPeriod()
    override fun getPendingRecurringExpensesFlow(): Flow<Double?> =
        recurringMovementDao.getPendingRecurringExpensesFlow()
    override suspend fun getPendingRecurringExpensesSync(): Double =
        recurringMovementDao.getPendingRecurringExpensesSync() ?: 0.0
    override fun getPendingScheduledSavingsFlow(): Flow<Double?> =
        recurringMovementDao.getPendingScheduledSavingsFlow()
    override suspend fun getPendingScheduledSavingsSync(): Double =
        recurringMovementDao.getPendingScheduledSavingsSync() ?: 0.0
    override suspend fun insertRecurringMovement(movement: RecurringMovementEntity): Long =
        recurringMovementDao.insertMovement(movement)
    override suspend fun setRecurringPaidStatus(id: Long, isPaid: Boolean) =
        recurringMovementDao.setPaymentStatus(id, isPaid)

    // Presupuestos
    override fun getBudgetsWithCategories(): Flow<List<BudgetWithCategory>> =
        budgetDao.getBudgetsWithCategories()
    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)

    // Bandeja BCR
    override fun getPendingBcrTransactions(): Flow<List<BcrPendingEntity>> =
        bcrPendingDao.getPendingTransactions()
    override fun getPendingBcrCountFlow(): Flow<Int> = bcrPendingDao.getPendingCountFlow()
    override suspend fun insertBcrPending(entity: BcrPendingEntity): Long =
        bcrPendingDao.insertPendingTransaction(entity)
    override suspend fun markBcrAsProcessed(id: Long) = bcrPendingDao.markAsProcessed(id)
    override suspend fun deleteBcrPending(entity: BcrPendingEntity) =
        bcrPendingDao.deletePendingTransaction(entity)
}
