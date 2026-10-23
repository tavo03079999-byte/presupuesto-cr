package com.presupuesto.cr.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {

    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Restaurant" to Icons.Default.Restaurant,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalHospital" to Icons.Default.LocalHospital,
        "Movie" to Icons.Default.Movie,
        "Bolt" to Icons.Default.Bolt,
        "Home" to Icons.Default.Home,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Savings" to Icons.Default.Savings,
        "Payments" to Icons.Default.Payments,
        "School" to Icons.Default.School,
        "Flight" to Icons.Default.Flight,
        "Pets" to Icons.Default.Pets,
        "Favorite" to Icons.Default.Favorite,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Category" to Icons.Default.Category
    )

    val availableColorsHex: List<String> = listOf(
        "#4CAF50",
        "#2196F3",
        "#F44336",
        "#FF9800",
        "#9C27B0",
        "#00BCD4",
        "#E91E63",
        "#FFC107",
        "#009688",
        "#607D8B"
    )

    fun getIcon(iconName: String): ImageVector {
        return availableIcons.find { it.first.equals(iconName, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }

    fun parseColor(colorHex: String, fallback: Color = Color(0xFF2196F3)): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            fallback
        }
    }
}
