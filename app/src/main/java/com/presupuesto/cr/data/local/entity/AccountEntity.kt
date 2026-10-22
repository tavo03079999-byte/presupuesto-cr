package com.presupuesto.cr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: AccountType,
    val currentBalance: Double,
    val creditLimit: Double = 0.0,
    val currency: String = "CRC",
    val accountNumberMasked: String = "",
    val colorHex: String = "#00875A",
    val isIncludedInNetWorth: Boolean = true,
    val isIncludedInSafeToSpend: Boolean = true
)
