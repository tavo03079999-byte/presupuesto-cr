package com.presupuesto.cr.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transacciones", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Reportes", Icons.Default.Assessment)
    object Calendar : Screen("calendar", "Calendario", Icons.Default.CalendarMonth)
    object Settings : Screen("settings", "Configuración", Icons.Default.Settings)
}
