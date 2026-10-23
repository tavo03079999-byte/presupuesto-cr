package com.presupuesto.cr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.presupuesto.cr.data.local.entity.RecurringMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringMovementDao {
    @Query("SELECT * FROM payment_reminders WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveMovements(): Flow<List<RecurringMovementEntity>>

    @Query("SELECT * FROM payment_reminders ORDER BY id ASC")
    fun getAllMovements(): Flow<List<RecurringMovementEntity>>

    @Query("SELECT * FROM payment_reminders ORDER BY id ASC")
    suspend fun getAll(): List<RecurringMovementEntity>

    @Query("SELECT * FROM payment_reminders WHERE id = :id")
    suspend fun getMovementById(id: Long): RecurringMovementEntity?

    @Query("SELECT * FROM payment_reminders WHERE id = :id")
    suspend fun getById(id: Long): RecurringMovementEntity?

    @Query("SELECT * FROM payment_reminders WHERE isActive = 1 AND isPaidForCurrentPeriod = 0")
    suspend fun getPendingMovementsForCurrentPeriod(): List<RecurringMovementEntity>

    @Query("SELECT * FROM payment_reminders WHERE isActive = 1 AND isPaidForCurrentPeriod = 0")
    fun getPendingMovementsFlow(): Flow<List<RecurringMovementEntity>>

    @Query("""
        SELECT SUM(amount) FROM payment_reminders 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'EXPENSE'
    """)
    fun getPendingRecurringExpensesFlow(): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM payment_reminders 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'EXPENSE'
    """)
    suspend fun getPendingRecurringExpensesSync(): Double?

    @Query("""
        SELECT SUM(amount) FROM payment_reminders 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'SPECIAL_ALLOCATION'
    """)
    fun getPendingScheduledSavingsFlow(): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM payment_reminders 
        WHERE isActive = 1 AND isPaidForCurrentPeriod = 0 AND type = 'SPECIAL_ALLOCATION'
    """)
    suspend fun getPendingScheduledSavingsSync(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: RecurringMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: RecurringMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<RecurringMovementEntity>)

    @Update
    suspend fun updateMovement(movement: RecurringMovementEntity)

    @Update
    suspend fun update(movement: RecurringMovementEntity)

    @Query("UPDATE payment_reminders SET isPaidForCurrentPeriod = :isPaid, updated_at = :updatedAt WHERE id = :id")
    suspend fun setPaymentStatus(id: Long, isPaid: Boolean, updatedAt: Long)

    @Query("UPDATE payment_reminders SET isPaidForCurrentPeriod = 0")
    suspend fun resetAllPeriodPayments()

    @Delete
    suspend fun deleteMovement(movement: RecurringMovementEntity)

    @Delete
    suspend fun delete(movement: RecurringMovementEntity)
}
