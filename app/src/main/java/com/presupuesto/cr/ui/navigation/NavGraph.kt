package com.presupuesto.cr.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.presupuesto.cr.domain.usecase.GoogleDriveBackupUseCase
import com.presupuesto.cr.ui.home.HomeScreen
import com.presupuesto.cr.ui.home.HomeViewModel
import com.presupuesto.cr.ui.reports.ReportsScreen
import com.presupuesto.cr.ui.reports.ReportsViewModel
import com.presupuesto.cr.ui.settings.SettingsScreen
import com.presupuesto.cr.ui.settings.SettingsViewModel
import com.presupuesto.cr.ui.theme.BcrNavy
import com.presupuesto.cr.ui.theme.EmeraldPrimary
import com.presupuesto.cr.ui.transactions.TransactionsScreen
import com.presupuesto.cr.ui.transactions.TransactionsViewModel

@Composable
fun MainAppNavigation(
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    reportsViewModel: ReportsViewModel,
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
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (screen == Screen.Transactions && pendingBcrCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Color(0xFFE53935)) {
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
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
                    onNavigateToBcrInbox = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = transactionsViewModel)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = reportsViewModel)
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
