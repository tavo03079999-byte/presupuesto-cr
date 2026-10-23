package com.presupuesto.cr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.presupuesto.cr.data.local.entity.BcrPendingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BcrPendingDao {
    @Query("SELECT * FROM bcr_pending_transactions WHERE isProcessed = 0 ORDER BY timestamp DESC")
    fun getPendingTransactions(): Flow<List<BcrPendingEntity>>

    @Query("SELECT * FROM bcr_pending_transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<BcrPendingEntity>

    @Query("SELECT * FROM bcr_pending_transactions WHERE id = :id")
    suspend fun getById(id: Long): BcrPendingEntity?

    @Query("SELECT COUNT(*) FROM bcr_pending_transactions WHERE isProcessed = 0")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(entity: BcrPendingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BcrPendingEntity): Long

    @Update
    suspend fun updatePendingTransaction(entity: BcrPendingEntity)

    @Update
    suspend fun update(entity: BcrPendingEntity)

    @Query("UPDATE bcr_pending_transactions SET isProcessed = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markAsProcessed(id: Long, updatedAt: Long)

    @Delete
    suspend fun deletePendingTransaction(entity: BcrPendingEntity)

    @Delete
    suspend fun delete(entity: BcrPendingEntity)
}
