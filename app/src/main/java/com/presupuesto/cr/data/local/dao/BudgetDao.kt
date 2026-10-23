package com.presupuesto.cr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.presupuesto.cr.data.local.entity.BudgetEntity
import com.presupuesto.cr.data.local.entity.BudgetPeriod
import kotlinx.coroutines.flow.Flow

data class BudgetWithCategory(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val iconName: String,
    val period: BudgetPeriod,
    val limitAmount: Double,
    val alertThresholdPercent: Double
)

@Dao
interface BudgetDao {
    @Query("""
        SELECT b.id, b.categoryId, c.name AS categoryName, c.colorHex AS categoryColor,
               c.iconName AS iconName, b.period, b.limitAmount, b.alertThresholdPercent
        FROM budgets b
        INNER JOIN categories c ON b.categoryId = c.id
        ORDER BY b.id ASC
    """)
    fun getBudgetsWithCategories(): Flow<List<BudgetWithCategory>>

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetByCategory(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}
