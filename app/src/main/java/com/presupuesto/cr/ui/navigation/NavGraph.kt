package com.presupuesto.cr.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.ui.calendar.CalendarScreen
import com.presupuesto.cr.ui.calendar.CalendarViewModel
import com.presupuesto.cr.ui.home.HomeScreen
import com.presupuesto.cr.ui.home.HomeViewModel
import com.presupuesto.cr.ui.reports.ReportsScreen
import com.presupuesto.cr.ui.reports.ReportsViewModel
import com.presupuesto.cr.ui.settings.SettingsScreen
import com.presupuesto.cr.ui.settings.SettingsViewModel
import com.presupuesto.cr.ui.theme.ExpenseRed
import com.presupuesto.cr.ui.theme.ThemeBlue
import com.presupuesto.cr.ui.transactions.TransactionsScreen
import com.presupuesto.cr.ui.transactions.TransactionsViewModel

@Composable
fun MainAppNavigation(
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    reportsViewModel: ReportsViewModel,
    calendarViewModel: CalendarViewModel,
    settingsViewModel: SettingsViewModel,
    googleDriveBackupUseCase: GoogleDriveBackupUseCase
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingBcrCount by homeViewModel.pendingBcrCount.collectAsState()

    val screens = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Calendar,
        Screen.Settings
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    screens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navigateSingleTop(navController, screen.route)
                            },
                            icon = {
                                if (screen == Screen.Transactions && pendingBcrCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = ExpenseRed) {
                                                Text("$pendingBcrCount", color = Color.White)
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemeBlue,
                                selectedTextColor = ThemeBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTransactions = { type, categoryId ->
                        transactionsViewModel.applyExternalFilter(
                            type = type,
                            categoryId = categoryId
                        )
                        navigateSingleTop(navController, Screen.Transactions.route)
                    },
                    onNavigateToReports = {
                        navigateSingleTop(navController, Screen.Reports.route)
                    },
                    onNavigateToSettings = {
                        navigateSingleTop(navController, Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = transactionsViewModel)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateToFilteredTransactions = { type, categoryId, startMillis, endMillis ->
                        transactionsViewModel.applyExternalFilter(
                            type = type,
                            categoryId = categoryId,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                        navigateSingleTop(navController, Screen.Transactions.route)
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    googleDriveBackupUseCase = googleDriveBackupUseCase
                )
            }
        }
    }
}

private fun navigateSingleTop(
    navController: NavHostController,
    route: String
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
