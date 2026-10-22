package com.presupuesto.cr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.presupuesto.cr.data.local.entity.CategoryType
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringMovementDao {
    @Query("SELECT * FROM recurring_movements WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveMovements(): Flow<List<RecurringMovementEntity>>

    @Query("SELECT * FROM recurring_movements ORDER BY id ASC")
    fun getAllMovements(): Flow<List<RecurringMovementEntity>>

    @Query("SELECT * FROM recurring_movements WHERE id = :id")
    suspend fun getMovementById(id: Long): RecurringMovementEntity?

    @Query("SELECT * FROM recurring_movements WHERE isActive = 1 AND isPaidForCurrentPeriod = 0")
    suspend fun getPendingMovementsForCurrentPeriod(): List<RecurringMovementEntity>

    @Query("SELECT * FROM recurring_movements WHERE isActive = 1 AND isPaidForCurrentPeriod = 0")
    fun getPendingMovementsFlow(): Flow<List<RecurringMovementEntity>>

    @Query("""
        SELECT SUM(amount) FROM recurring_movements 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'EXPENSE'
    """)
    fun getPendingRecurringExpensesFlow(): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM recurring_movements 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'EXPENSE'
    """)
    suspend fun getPendingRecurringExpensesSync(): Double?

    @Query("""
        SELECT SUM(amount) FROM recurring_movements 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'SPECIAL_ALLOCATION'
    """)
    fun getPendingScheduledSavingsFlow(): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM recurring_movements 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'SPECIAL_ALLOCATION'
    """)
    suspend fun getPendingScheduledSavingsSync(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: RecurringMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<RecurringMovementEntity>)

    @Update
    suspend fun updateMovement(movement: RecurringMovementEntity)

    @Query("UPDATE recurring_movements SET isPaidForCurrentPeriod = :isPaid WHERE id = :id")
    suspend fun setPaymentStatus(id: Long, isPaid: Boolean)

    @Query("UPDATE recurring_movements SET isPaidForCurrentPeriod = 0")
    suspend fun resetAllPeriodPayments()

    @Delete
    suspend fun deleteMovement(movement: RecurringMovementEntity)
}
