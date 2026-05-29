package com.mistyislet.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.ui.doors.DoorsScreenContent
import com.mistyislet.app.ui.doors.DoorsTab
import com.mistyislet.app.ui.doors.DoorsUiState
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
