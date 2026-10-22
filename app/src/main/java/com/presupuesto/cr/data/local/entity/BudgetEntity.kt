package com.presupuesto.cr.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val limitAmount: Double,
    val alertThresholdPercent: Double = 75.0, // Verde <75%, Amarillo 75-90%, Rojo >90%
    val startDate: Long = System.currentTimeMillis()
)
