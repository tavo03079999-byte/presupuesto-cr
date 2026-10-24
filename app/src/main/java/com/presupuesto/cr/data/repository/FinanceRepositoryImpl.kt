package com.presupuesto.cr.data.repository

import com.presupuesto.cr.data.local.AppDatabase
import com.presupuesto.cr.data.local.dao.BudgetWithCategory
import com.presupuesto.cr.data.local.dao.CategoryTotal
import com.presupuesto.cr.data.local.entity.AccountEntity
import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import com.presupuesto.cr.data.local.entity.BudgetEntity
import com.presupuesto.cr.data.local.entity.BudgetPeriod
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

    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override suspend fun getAccountById(id: Long): AccountEntity? = try {
        accountDao.getById(id)
    } catch (e: Exception) {
        null
    }

    override fun getNetWorthFlow(): Flow<Double?> = accountDao.getNetWorthFlow()
    override fun getSafeToSpendBalanceFlow(): Flow<Double?> = accountDao.getSafeToSpendBalanceFlow()

    override suspend fun getSafeToSpendBalance(): Double = try {
        accountDao.getSafeToSpendBalance() ?: 0.0
    } catch (e: Exception) {
        0.0
    }

    override suspend fun updateAccountBalance(accountId: Long, amountDelta: Double) {
        try {
            accountDao.updateBalance(accountId, amountDelta, System.currentTimeMillis())
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun syncAccountBalancesWithTransactions() {
        try {
            accountDao.recalculateAllAccountBalances(System.currentTimeMillis())
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun insertAccount(account: AccountEntity): Long = try {
        accountDao.insert(account)
    } catch (e: Exception) {
        -1L
    }

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun getCategoryById(id: Long): CategoryEntity? = try {
        categoryDao.getById(id)
    } catch (e: Exception) {
        null
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    override suspend fun insertCategory(category: CategoryEntity): Long = try {
        categoryDao.insert(category)
    } catch (e: Exception) {
        -1L
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        try {
            categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        try {
            val allCats = categoryDao.getAll()
            if (allCats.size <= 1) return
            val fallback = allCats.firstOrNull {
                it.id != category.id && it.name.contains("Otros", ignoreCase = true)
            } ?: allCats.firstOrNull { it.id != category.id }
            if (fallback != null) {
                transactionDao.reassignCategory(category.id, fallback.id)
            }
            categoryDao.delete(category)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startTime, endTime)

    override suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity> = try {
        transactionDao.getTransactionsBetweenSync(startTime, endTime)
    } catch (e: Exception) {
        emptyList()
    }

    override fun getTotalByTypeBetween(type: CategoryType, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalByTypeBetween(type, startTime, endTime)

    override suspend fun getTotalByTypeBetweenSync(type: CategoryType, startTime: Long, endTime: Long): Double = try {
        transactionDao.getTotalByTypeBetweenSync(type, startTime, endTime) ?: 0.0
    } catch (e: Exception) {
        0.0
    }

    override suspend fun getCategorySpentBetweenSync(categoryId: Long, startTime: Long, endTime: Long): Double = try {
        transactionDao.getCategorySpentBetweenSync(categoryId, startTime, endTime) ?: 0.0
    } catch (e: Exception) {
        0.0
    }

    override fun getExpenseSummaryByCategory(startTime: Long, endTime: Long): Flow<List<CategoryTotal>> =
        transactionDao.getExpenseSummaryByCategory(startTime, endTime)

    override fun getSummaryByCategoryAndType(
        type: CategoryType,
        startTime: Long,
        endTime: Long
    ): Flow<List<CategoryTotal>> =
        transactionDao.getSummaryByCategoryAndType(type, startTime, endTime)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = try {
        val now = System.currentTimeMillis()
        val id = transactionDao.insert(transaction)
        accountDao.recalculateAllAccountBalances(now)
        id
    } catch (e: Exception) {
        -1L
    }

    override suspend fun updateTransaction(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        try {
            val now = System.currentTimeMillis()
            transactionDao.update(newTransaction.copy(updatedAt = now))
            accountDao.recalculateAllAccountBalances(now)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        try {
            val now = System.currentTimeMillis()
            transactionDao.delete(transaction)
            accountDao.recalculateAllAccountBalances(now)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override fun getActiveRecurringMovements(): Flow<List<RecurringMovementEntity>> =
        recurringMovementDao.getActiveMovements()

    override fun getAllRecurringMovements(): Flow<List<RecurringMovementEntity>> =
        recurringMovementDao.getAllMovements()

    override suspend fun getPendingMovementsForCurrentPeriod(): List<RecurringMovementEntity> = try {
        recurringMovementDao.getPendingMovementsForCurrentPeriod()
    } catch (e: Exception) {
        emptyList()
    }

    override fun getPendingRecurringExpensesFlow(): Flow<Double?> =
        recurringMovementDao.getPendingRecurringExpensesFlow()

    override suspend fun getPendingRecurringExpensesSync(): Double = try {
        recurringMovementDao.getPendingRecurringExpensesSync() ?: 0.0
    } catch (e: Exception) {
        0.0
    }

    override fun getPendingScheduledSavingsFlow(): Flow<Double?> =
        recurringMovementDao.getPendingScheduledSavingsFlow()

    override suspend fun getPendingScheduledSavingsSync(): Double = try {
        recurringMovementDao.getPendingScheduledSavingsSync() ?: 0.0
    } catch (e: Exception) {
        0.0
    }

    override suspend fun insertRecurringMovement(movement: RecurringMovementEntity): Long = try {
        recurringMovementDao.insert(movement)
    } catch (e: Exception) {
        -1L
    }

    override suspend fun updateRecurringMovement(movement: RecurringMovementEntity) {
        try {
            recurringMovementDao.update(movement.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun deleteRecurringMovement(movement: RecurringMovementEntity) {
        try {
            recurringMovementDao.delete(movement)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun setRecurringPaidStatus(id: Long, isPaid: Boolean) {
        try {
            recurringMovementDao.setPaymentStatus(id, isPaid, System.currentTimeMillis())
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override fun getBudgetsWithCategories(): Flow<List<BudgetWithCategory>> =
        budgetDao.getBudgetsWithCategories()

    override suspend fun getBudgetByCategory(categoryId: Long): BudgetEntity? = try {
        budgetDao.getBudgetByCategory(categoryId)
    } catch (e: Exception) {
        null
    }

    override suspend fun insertBudget(budget: BudgetEntity): Long = try {
        budgetDao.insert(budget)
    } catch (e: Exception) {
        -1L
    }

    override suspend fun saveMonthlyBudgetForCategory(categoryId: Long, limitAmount: Double) {
        try {
            val now = System.currentTimeMillis()
            val existing = budgetDao.getBudgetByCategory(categoryId)
            if (existing != null) {
                budgetDao.update(
                    existing.copy(
                        limitAmount = limitAmount,
                        period = BudgetPeriod.MONTHLY,
                        alertThresholdPercent = 80.0,
                        updatedAt = now
                    )
                )
            } else {
                budgetDao.insert(
                    BudgetEntity(
                        categoryId = categoryId,
                        period = BudgetPeriod.MONTHLY,
                        limitAmount = limitAmount,
                        alertThresholdPercent = 80.0,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun deleteBudgetById(budgetId: Long) {
        try {
            budgetDao.deleteById(budgetId)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override fun getPendingBcrTransactions(): Flow<List<BcrPendingEntity>> =
        bcrPendingDao.getPendingTransactions()

    override fun getPendingBcrCountFlow(): Flow<Int> = bcrPendingDao.getPendingCountFlow()

    override suspend fun insertBcrPending(entity: BcrPendingEntity): Long = try {
        bcrPendingDao.insert(entity)
    } catch (e: Exception) {
        -1L
    }

    override suspend fun markBcrAsProcessed(id: Long) {
        try {
            bcrPendingDao.markAsProcessed(id, System.currentTimeMillis())
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }

    override suspend fun deleteBcrPending(entity: BcrPendingEntity) {
        try {
            bcrPendingDao.delete(entity)
        } catch (e: Exception) {
            // Manejo seguro de error de base de datos
        }
    }
}
