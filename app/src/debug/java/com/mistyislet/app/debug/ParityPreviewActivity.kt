package com.mistyislet.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.MobileCredential
import com.mistyislet.app.domain.model.UserInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import com.mistyislet.app.ui.admin.AdminDemoData
import com.mistyislet.app.ui.admin.AdminListItem
import com.mistyislet.app.ui.admin.AdminListScreen
import com.mistyislet.app.ui.admin.AdminUserDetailDataState
import com.mistyislet.app.ui.admin.UserDetailPageContent
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.credentials.CredentialsScreenContent
import com.mistyislet.app.ui.credentials.CredentialsUiState
import com.mistyislet.app.ui.dashboard.DashboardScreenContent
import com.mistyislet.app.ui.doors.DoorDetailsContent
import com.mistyislet.app.ui.doors.DoorsScreenContent
import com.mistyislet.app.ui.history.EventDetailContent
import com.mistyislet.app.ui.history.HistoryScreenContent
import com.mistyislet.app.ui.doors.DoorsTab
import com.mistyislet.app.ui.doors.DoorsUiState
import com.mistyislet.app.ui.profile.ChangePasswordContent
import com.mistyislet.app.ui.profile.GeofenceSettingsContent
import com.mistyislet.app.ui.profile.LanguageSettingsContent
import com.mistyislet.app.ui.profile.ProfileMainView
import com.mistyislet.app.ui.profile.ProfileUiState
import com.mistyislet.app.ui.navigation.MistyFloatingBottomNav
import com.mistyislet.app.ui.navigation.Routes
import com.mistyislet.app.ui.theme.MistyisletTheme

/**
 * DEBUG-only harness for iOS-parity screenshot comparison. Renders a chosen screen with
 * deterministic mock data — no Hilt, no backend, no auth — so on-device captures can be
 * diffed against the iOS app. Pick the screen with `-e screen <name>`, e.g.:
 *
 *   adb shell am start -n com.mistyislet.app/com.mistyislet.app.debug.ParityPreviewActivity -e screen doors
 *
 * Includes the real floating bottom nav so the comparison matches the actual app shell
 * (which iOS shows as a tab bar). Mock content mirrors the iOS app's "Sudirman Hub".
 */
class ParityPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent?.getStringExtra("screen") ?: "doors"
        setContent {
            MistyisletTheme {
                when (screen) {
                    "pass" -> PassPreview()
                    "profile" -> ProfilePreview()
                    "dashboard" -> DashboardPreview()
                    "history" -> HistoryPreview()
                    "eventdetail" -> EventDetailPreview()
                    "changepassword" -> ChangePasswordPreview()
                    "language" -> LanguagePreview()
                    "geofence" -> GeofencePreview()
                    "doordetail" -> DoorDetailPreview()
                    "adminusers" -> AdminUsersPreview()
                    "admincontrollers" -> AdminControllersPreview()
                    "adminuserdetail" -> AdminUserDetailPreview()
                    else -> DoorsPreview()
                }
            }
        }
    }
}

private fun mockDoor(
    id: String,
    name: String,
    building: String = "b1",
    status: String = "online",
    gateway: String = "online",
    canUnlock: Boolean = true,
    favorite: Boolean = false,
    kind: String? = "door",
    group: String? = null,
): AccessibleDoor = AccessibleDoor(
    id = id,
    name = name,
    buildingId = building,
    status = status,
    gatewayStatus = gateway,
    canUnlock = canUnlock,
    isFavorite = favorite,
    kind = kind,
    groupName = group,
)

@Composable
private fun DoorsPreview() {
    // Mirrors the iOS "Sudirman Hub" doors list (all online + unlockable, no favorites).
    val state = DoorsUiState(
        placeId = "building_demo_001",
        placeName = "Sudirman Hub",
        tab = DoorsTab.ALL,
        doors = listOf(
            mockDoor("d1", "A-23 East Wing"),
            mockDoor("d2", "B-12 West Lobby"),
            mockDoor("d3", "C-5 Server Room", building = "b2"),
            mockDoor("d4", "Parking Gate P1", kind = "gate"),
        ),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        DoorsScreenContent(
            uiState = state,
            onBack = {},
            onSearchChange = {},
            onTabChange = {},
            onRefresh = {},
            onToggleFavorite = {},
            onToggleLockdown = {},
            onDismissUnlockResult = {},
            onTapDoor = {},
            onUnlock = {},
        )
        MistyFloatingBottomNav(
            currentRoute = Routes.DOORS,
            onSelected = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PassPreview() {
    // Mirrors the iOS "Pass" tab for "MistyPass Jakarta Demo" (Access + PIN cards).
    val state = CredentialsUiState(
        organizationName = "MistyPass Jakarta Demo",
        placeName = "Sudirman Hub",
        mobileCredentials = listOf(
            MobileCredential(
                id = "dev1",
                platform = "android",
                deviceModel = "iPhone",
                status = "active",
                expiresAt = "2026-08-23T00:00:00Z",
            ),
        ),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        CredentialsScreenContent(uiState = state)
        MistyFloatingBottomNav(
            currentRoute = Routes.PASS,
            onSelected = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ProfilePreview() {
    val state = ProfileUiState(
        user = UserInfo(
            id = "u1",
            email = "siky@mistyislet.com",
            name = "Siky",
            tenantId = "t1",
            organizationName = "MistyPass Jakarta Demo",
            role = "building_admin",
            roleDisplayLabel = "Building Admin",
        ),
        biometricAvailable = true,
        biometricEnabled = true,
        biometricTypeName = "Face ID",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        ProfileMainView(uiState = state)
        MistyFloatingBottomNav(
            currentRoute = Routes.PROFILE,
            onSelected = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DashboardPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardScreenContent(
            placeId = "building_demo_001",
            orgId = "tenant_demo_jakarta",
            onNavigate = {},
        )
        MistyFloatingBottomNav(
            currentRoute = Routes.DASHBOARD,
            onSelected = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun EventDetailPreview() {
    // Mirrors the iOS Event Detail reached by tapping the first History row (Main Entrance, granted, BLE).
    val log = AccessLog(
        id = "1",
        doorName = "Main Entrance",
        result = "allow",
        method = "ble",
        at = "2026-05-30T08:34:00Z",
    )
    EventDetailContent(
        log = log,
        media = emptyList(),
        isLoadingMedia = false,
        onBack = {},
    )
}

@Composable
private fun HistoryPreview() {
    val logs = listOf(
        AccessLog(id = "1", doorName = "A-23 East Wing", result = "allow", method = "ble", at = "2026-05-30T09:41:00Z"),
        AccessLog(id = "2", doorName = "C-5 Server Room", result = "deny", method = "nfc", reason = "No permission for this door", at = "2026-05-30T08:15:00Z"),
        AccessLog(id = "3", doorName = "B-12 West Lobby", result = "allow", method = "app", at = "2026-05-30T07:50:00Z"),
        AccessLog(id = "4", doorName = "Parking Gate P1", result = "allow", method = "ble", at = "2026-05-29T18:30:00Z"),
        AccessLog(id = "5", doorName = "A-23 East Wing", result = "allow", method = "card", at = "2026-05-29T09:05:00Z"),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        HistoryScreenContent(logs = logs)
        MistyFloatingBottomNav(
            currentRoute = Routes.DASHBOARD,
            onSelected = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// --- Profile subpages: pushed screens with their own top bar, no tab bar
// (matches the real Android app shell; iOS keeps its tab bar = intentional paradigm diff).

@Composable
private fun ChangePasswordPreview() {
    // Empty form, no error — matches iOS ChangePasswordView pushed from Profile settings.
    ChangePasswordContent(
        uiState = ProfileUiState(),
        onBack = {},
        onSubmit = { _, _ -> },
    )
}

@Composable
private fun LanguagePreview() {
    LanguageSettingsContent(onBack = {}, onSelect = {})
}

@Composable
private fun GeofencePreview() {
    GeofenceSettingsContent(onBack = {})
}

@Composable
private fun DoorDetailPreview() {
    // Mirrors tapping a door card on the iOS Doors tab -> "Door Details" sheet.
    val door = AccessibleDoor(
        id = "d1",
        name = "A-23 East Wing",
        buildingId = "b1",
        status = "online",
        gatewayStatus = "online",
        canUnlock = true,
        kind = "office",
    )
    DoorDetailsContent(
        door = door,
        placeId = "building_demo_001",
        restrictions = emptyList(),
        schedules = emptyList(),
        onToggleLockdown = {},
        onDismiss = {},
    )
}

@Composable
private fun AdminUsersPreview() {
    // Mirrors the iOS Dashboard -> Users admin list (shared AdminListScreen).
    AdminListScreen(
        title = "Users",
        items = listOf(
            AdminListItem(
                id = "1",
                title = "Sudirman Building Admin",
                subtitle = "building.admin.sudirman@mistypass.local",
                trailing = "Building Admin",
                leadingInitial = "S",
            ),
            AdminListItem(
                id = "2",
                title = "Andri Pratama",
                subtitle = "andri.pratama@mistypass.local",
                trailing = "Employee",
                leadingInitial = "A",
            ),
            AdminListItem(
                id = "3",
                title = "Jakarta Resident",
                subtitle = "resident.jakarta@mistypass.local",
                trailing = "Employee",
                leadingInitial = "J",
            ),
            AdminListItem(
                id = "4",
                title = "Siky",
                subtitle = "siky@mistyislet.com",
                trailing = "Tenant Admin",
                leadingInitial = "S",
            ),
        ),
        isLoading = false,
        emptyMessage = "No users",
        onBack = {},
        onItemClick = {},
    )
}

@Composable
private fun AdminControllersPreview() {
    // Icon-based admin list — regression check for the shared AdminListScreen.
    AdminListScreen(
        title = "Controllers",
        items = listOf(
            AdminListItem(
                id = "1",
                title = "A-23 East Wing Main Entrance Controller",
                subtitle = "Gateway online",
                trailing = "Online",
                leadingIcon = Icons.Outlined.MeetingRoom,
            ),
            AdminListItem(
                id = "2",
                title = "B-12 West Lobby",
                subtitle = "Gateway online",
                trailing = "Online",
                leadingIcon = Icons.Outlined.MeetingRoom,
            ),
            AdminListItem(
                id = "3",
                title = "C-5 Server Room",
                subtitle = "Gateway offline",
                trailing = "Offline",
                leadingIcon = Icons.Outlined.MeetingRoom,
            ),
        ),
        isLoading = false,
        emptyMessage = "No controllers",
        onBack = {},
        onItemClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUserDetailPreview() {
    // iOS Dashboard > Users > (tap a user) detail (AdminCRUDViews): profile card + info + actions.
    val user = AdminDemoData.placeUsers[0]
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = { MistyNavigationTopBar(title = user.name, onBack = {}) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            UserDetailPageContent(
                user = user,
                detailState = AdminUserDetailDataState(
                    user = user,
                    logins = AdminDemoData.userLogins,
                    accessRights = AdminDemoData.userAccessRights,
                ),
                onSignOut = {},
                onRemove = {},
                onRoleChange = {},
                onShareAccess = {},
            )
        }
    }
}
