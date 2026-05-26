package com.example.smartbudget_asistent_financiar.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartbudget_asistent_financiar.ui.screens.detail.ReceiptDetailScreen
import com.example.smartbudget_asistent_financiar.ui.screens.home.HomeScreen
import com.example.smartbudget_asistent_financiar.ui.screens.receipts.ReceiptListScreen
import com.example.smartbudget_asistent_financiar.ui.screens.scan.ScanScreen

private data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Scan, "Scan", Icons.Default.PhotoCamera),
    BottomNavItem(Screen.ReceiptList, "Receipts", Icons.AutoMirrored.Filled.List),
)

@Composable
fun SmartBudgetNavGraph(onSignOut: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
                    onReceiptClick = { id ->
                        navController.navigate(Screen.ReceiptDetail.createRoute(id))
                    },
                    onSignOut = onSignOut
                )
            }

            composable(Screen.ReceiptList.route) {
                ReceiptListScreen(
                    onReceiptClick = { id ->
                        navController.navigate(Screen.ReceiptDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.Scan.route) {
                ScanScreen(
                    onReceiptSaved = { id ->
                        navController.navigate(Screen.ReceiptDetail.createRoute(id)) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.ReceiptDetail.route,
                arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: return@composable
                ReceiptDetailScreen(
                    receiptId = receiptId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
