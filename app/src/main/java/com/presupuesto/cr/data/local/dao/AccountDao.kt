package com.presupuesto.cr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.presupuesto.cr.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE isIncludedInNetWorth = 1")
    fun getNetWorthFlow(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE isIncludedInSafeToSpend = 1")
    fun getSafeToSpendBalanceFlow(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM accounts WHERE isIncludedInSafeToSpend = 1")
    suspend fun getSafeToSpendBalance(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalance = currentBalance + :amountDelta, updated_at = :updatedAt WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amountDelta: Double, updatedAt: Long)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}
