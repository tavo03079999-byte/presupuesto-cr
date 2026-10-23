package com.presupuesto.cr.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_reminders")
data class RecurringMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: CategoryType,
    val categoryId: Long,
    val accountId: Long,
    val frequency: RecurringFrequency = RecurringFrequency.FORTNIGHTLY_15_30,
    val dayOfMonth: Int = 15,
    val secondDayOfMonth: Int? = 30,
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val isPaidForCurrentPeriod: Boolean = false,
    val autoDeductOnDueDate: Boolean = false,
    val reminderOffsetHours: Int = 24, // 24 = 1 día antes, 0 = mismo día, 1 = 1 hora antes
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
