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
import com.mistyislet.app.ui.admin.AdminAccessRightsScreen
import com.mistyislet.app.ui.admin.AdminAlarmsScreen
import com.mistyislet.app.ui.admin.AdminAnalyticsScreen
import com.mistyislet.app.ui.admin.AdminBookingsScreen
import com.mistyislet.app.ui.admin.AdminCamerasScreen
import com.mistyislet.app.ui.admin.AdminCardsScreen
import com.mistyislet.app.ui.admin.AdminControllersScreen
import com.mistyislet.app.ui.admin.AdminDigitalCredentialsScreen
import com.mistyislet.app.ui.admin.AdminEventsScreen
import com.mistyislet.app.ui.admin.AdminExportScreen
import com.mistyislet.app.ui.admin.AdminGatewaysScreen
import com.mistyislet.app.ui.admin.AdminGuestManagementScreen
import com.mistyislet.app.ui.admin.AdminGroupsScreen
import com.mistyislet.app.ui.admin.AdminIncidentsScreen
import com.mistyislet.app.ui.admin.AdminLiveActivityScreen
import com.mistyislet.app.ui.admin.AdminOrgSettingsScreen
import com.mistyislet.app.ui.admin.AdminSchedulesScreen
import com.mistyislet.app.ui.admin.AdminTeamsScreen
import com.mistyislet.app.ui.admin.AdminUserPresenceScreen
import com.mistyislet.app.ui.admin.AdminUsersScreen
import com.mistyislet.app.ui.admin.AdminZonesScreen
import com.mistyislet.app.ui.credentials.BindCardScreen
import com.mistyislet.app.ui.credentials.CredentialsScreen
import com.mistyislet.app.ui.dashboard.DashboardScreen
import com.mistyislet.app.ui.doors.DoorsRootScreen
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

    const val ADMIN_EVENTS = "admin_events"
    const val ADMIN_INCIDENTS = "admin_incidents"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_GROUPS = "admin_groups"
    const val ADMIN_TEAMS = "admin_teams"
    const val ADMIN_SCHEDULES = "admin_schedules"
    const val ADMIN_ZONES = "admin_zones"
    const val ADMIN_ALARMS = "admin_alarms"
    const val ADMIN_LIVE_ACTIVITY = "admin_live_activity"
    const val ADMIN_BOOKINGS = "admin_bookings"
    const val ADMIN_CARDS = "admin_cards"
    const val ADMIN_CREDENTIALS = "admin_credentials"
    const val ADMIN_ANALYTICS = "admin_analytics"
    const val ADMIN_USER_PRESENCE = "admin_user_presence"
    const val ADMIN_EXPORT = "admin_export"
    const val ADMIN_ACCESS_RIGHTS = "admin_access_rights"
    const val ADMIN_CONTROLLERS = "admin_controllers"
    const val ADMIN_GATEWAYS = "admin_gateways"
    const val ADMIN_CAMERAS = "admin_cameras"
    const val ADMIN_GUEST_MANAGEMENT = "admin_guest_management"
    const val ADMIN_ORG_SETTINGS = "admin_org_settings"
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
            composable(Routes.DOORS) { DoorsRootScreen() }
            composable(Routes.PASS) {
                CredentialsScreen(
                    onNavigateToBindCard = { navController.navigate(Routes.BIND_CARD) },
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigate = { route -> navController.navigate(route) },
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

            // Admin screens
            composable(Routes.ADMIN_EVENTS) { AdminEventsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_INCIDENTS) { AdminIncidentsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_USERS) { AdminUsersScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_GROUPS) { AdminGroupsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_TEAMS) { AdminTeamsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_SCHEDULES) { AdminSchedulesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_ZONES) { AdminZonesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_ALARMS) { AdminAlarmsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_LIVE_ACTIVITY) { AdminLiveActivityScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_BOOKINGS) { AdminBookingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_CARDS) { AdminCardsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_CREDENTIALS) { AdminDigitalCredentialsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_ANALYTICS) { AdminAnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_USER_PRESENCE) { AdminUserPresenceScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_EXPORT) { AdminExportScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_ACCESS_RIGHTS) { AdminAccessRightsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_CONTROLLERS) { AdminControllersScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_GATEWAYS) { AdminGatewaysScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_CAMERAS) { AdminCamerasScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_GUEST_MANAGEMENT) { AdminGuestManagementScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_ORG_SETTINGS) { AdminOrgSettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
