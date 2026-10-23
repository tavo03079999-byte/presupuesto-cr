package com.presupuesto.cr.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconName: String,
    val colorHex: String,
    val isFixedExpense: Boolean = false,
    val isSpecialBalanceAllocation: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultCategories(): List<CategoryEntity> {
            return listOf(
                CategoryEntity(
                    id = 1,
                    name = "Salario",
                    type = CategoryType.INCOME,
                    iconName = "Payments",
                    colorHex = "#4CAF50",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 2,
                    name = "Otros Ingresos",
                    type = CategoryType.INCOME,
                    iconName = "AccountBalanceWallet",
                    colorHex = "#00BCD4",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 3,
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    iconName = "ShoppingCart",
                    colorHex = "#4CAF50",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 4,
                    name = "Transporte",
                    type = CategoryType.EXPENSE,
                    iconName = "DirectionsCar",
                    colorHex = "#FF9800",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 5,
                    name = "Salud",
                    type = CategoryType.EXPENSE,
                    iconName = "LocalHospital",
                    colorHex = "#F44336",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 6,
                    name = "Entretenimiento",
                    type = CategoryType.EXPENSE,
                    iconName = "Movie",
                    colorHex = "#9C27B0",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 7,
                    name = "Servicios",
                    type = CategoryType.EXPENSE,
                    iconName = "Bolt",
                    colorHex = "#2196F3",
                    isFixedExpense = true
                ),
                CategoryEntity(
                    id = 8,
                    name = "Vivienda",
                    type = CategoryType.EXPENSE,
                    iconName = "Home",
                    colorHex = "#00BCD4",
                    isFixedExpense = true
                ),
                CategoryEntity(
                    id = 9,
                    name = "Otros",
                    type = CategoryType.EXPENSE,
                    iconName = "Category",
                    colorHex = "#607D8B",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 10,
                    name = "Ahorro e Inversión",
                    type = CategoryType.SPECIAL_ALLOCATION,
                    iconName = "Savings",
                    colorHex = "#FFC107",
                    isSpecialBalanceAllocation = true
                )
            )
        }
    }
}
