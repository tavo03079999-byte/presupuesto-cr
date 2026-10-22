package com.presupuesto.cr.data.local.entity

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
    val isSpecialBalanceAllocation: Boolean = false
) {
    companion object {
        fun getDefaultCategories(): List<CategoryEntity> {
            return listOf(
                // 1. INGRESOS
                CategoryEntity(
                    id = 1,
                    name = "Salario Quincenal",
                    type = CategoryType.INCOME,
                    iconName = "Payments",
                    colorHex = "#00C853",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 2,
                    name = "Cuentas por Cobrar",
                    type = CategoryType.INCOME,
                    iconName = "AccountBalanceWallet",
                    colorHex = "#26A69A",
                    isFixedExpense = false
                ),

                // 2. GASTOS
                CategoryEntity(
                    id = 3,
                    name = "Diezmo",
                    type = CategoryType.EXPENSE,
                    iconName = "Favorite",
                    colorHex = "#7E57C2",
                    isFixedExpense = true
                ),
                CategoryEntity(
                    id = 4,
                    name = "Comida (Abarrotes/Súper)",
                    type = CategoryType.EXPENSE,
                    iconName = "ShoppingCart",
                    colorHex = "#FF7043",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 5,
                    name = "Carnes",
                    type = CategoryType.EXPENSE,
                    iconName = "Restaurant",
                    colorHex = "#E53935",
                    isFixedExpense = false
                ),
                CategoryEntity(
                    id = 6,
                    name = "Servicios (Agua, Luz, Internet)",
                    type = CategoryType.EXPENSE,
                    iconName = "Bolt",
                    colorHex = "#1E88E5",
                    isFixedExpense = true
                ),
                CategoryEntity(
                    id = 7,
                    name = "Estilo de Vida (Gym)",
                    type = CategoryType.EXPENSE,
                    iconName = "FitnessCenter",
                    colorHex = "#00ACC1",
                    isFixedExpense = true
                ),

                // 3. ASIGNACIONES ESPECIALES DE BALANCE
                CategoryEntity(
                    id = 8,
                    name = "Ahorro e Inversión",
                    type = CategoryType.SPECIAL_ALLOCATION,
                    iconName = "Savings",
                    colorHex = "#FBC02D",
                    isSpecialBalanceAllocation = true
                )
            )
        }
    }
}
