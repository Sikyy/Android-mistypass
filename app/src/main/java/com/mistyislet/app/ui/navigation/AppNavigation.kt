package com.mistyislet.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.ui.credentials.BindCardScreen
import com.mistyislet.app.ui.credentials.CredentialsScreen
import com.mistyislet.app.ui.dashboard.DashboardScreen
import com.mistyislet.app.ui.doors.DoorsScreen
import com.mistyislet.app.ui.history.HistoryScreen
import com.mistyislet.app.ui.login.LoginScreen
import com.mistyislet.app.ui.profile.ProfileScreen
import com.mistyislet.app.ui.visitors.VisitorsScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val DOORS = "doors"
    const val PASS = "pass"
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val VISITORS = "visitors"
    const val PROFILE = "profile"
    const val BIND_CARD = "bind_card"
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DOORS, Icons.Default.DoorFront, R.string.nav_doors),
    BottomNavItem(Routes.PASS, Icons.Default.Wallet, R.string.nav_pass),
    BottomNavItem(Routes.DASHBOARD, Icons.Default.Dashboard, R.string.nav_dashboard),
    BottomNavItem(Routes.PROFILE, Icons.Default.Person, R.string.nav_profile),
)

@Composable
fun AppNavigation(authRepository: AuthRepository) {
    val rootNavController = rememberNavController()
    val startDestination = if (authRepository.isLoggedIn()) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    rootNavController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onLogout = {
                    rootNavController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DOORS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.DOORS) { DoorsScreen() }
            composable(Routes.PASS) {
                CredentialsScreen(
                    onNavigateToBindCard = { navController.navigate(Routes.BIND_CARD) },
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                    onNavigateToVisitors = { navController.navigate(Routes.VISITORS) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.VISITORS) { VisitorsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(onLogout = onLogout)
            }
            composable(Routes.BIND_CARD) {
                BindCardScreen(onBindSuccess = { navController.popBackStack() })
            }
        }
    }
}
