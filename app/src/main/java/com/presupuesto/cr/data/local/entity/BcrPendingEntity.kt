package com.presupuesto.cr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bcr_pending_transactions")
data class BcrPendingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val parsedAmount: Double,
    val merchant: String? = null,
    val cardLast4: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedCategoryId: Long? = null,
    val suggestedAccountId: Long? = null,
    val isProcessed: Boolean = false
)
