package com.presupuesto.cr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_movements")
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
    val secondDayOfMonth: Int? = 30, // Para quincenas en Costa Rica (15 y 30)
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val isPaidForCurrentPeriod: Boolean = false,
    val autoDeductOnDueDate: Boolean = false
)
