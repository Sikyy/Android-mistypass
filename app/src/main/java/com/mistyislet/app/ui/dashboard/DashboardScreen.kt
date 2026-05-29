package com.mistyislet.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyDoorIcon
import com.mistyislet.app.ui.components.MistyIncidentIcon
import com.mistyislet.app.ui.components.MistyLargeTitle
import com.mistyislet.app.ui.components.MistyPage
import com.mistyislet.app.ui.components.MistySectionTitle
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosCyan
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosIndigo
import com.mistyislet.app.ui.theme.IosMint
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPurple
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosTeal

private data class DashboardAction(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val onClick: () -> Unit,
)

private data class DashboardSectionData(
    val title: String,
    val actions: List<DashboardAction>,
)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreenContent(placeId = uiState.placeId, orgId = uiState.orgId, onNavigate = onNavigate)
}

/** Stateless Dashboard content — driven by placeId/orgId so it renders in the DEBUG parity harness. */
@Composable
internal fun DashboardScreenContent(
    placeId: String?,
    orgId: String?,
    onNavigate: (String) -> Unit,
) {
    if (placeId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.dashboard_no_place),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dashboard_no_place_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val sections = buildList {
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_activity),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.History,
                        iconTint = IosGreen,
                        title = stringResource(R.string.dashboard_event_history),
                        onClick = { onNavigate("history") },
                    ),
                    DashboardAction(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        iconTint = IosBlue,
                        title = stringResource(R.string.dashboard_events),
                        onClick = { onNavigate("admin_events") },
                    ),
                    DashboardAction(
                        icon = MistyIncidentIcon,
                        iconTint = IosRed,
                        title = stringResource(R.string.dashboard_incidents),
                        onClick = { onNavigate("admin_incidents") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_management),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.People,
                        iconTint = IosBlue,
                        title = stringResource(R.string.dashboard_users),
                        onClick = { onNavigate("admin_users") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.SupervisedUserCircle,
                        iconTint = IosMint,
                        title = stringResource(R.string.dashboard_groups),
                        onClick = { onNavigate("admin_groups") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.Groups,
                        iconTint = IosIndigo,
                        title = stringResource(R.string.dashboard_teams),
                        onClick = { onNavigate("admin_teams") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.CalendarMonth,
                        iconTint = IosPurple,
                        title = stringResource(R.string.dashboard_schedules),
                        onClick = { onNavigate("admin_schedules") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.Map,
                        iconTint = IosTeal,
                        title = stringResource(R.string.dashboard_zones),
                        onClick = { onNavigate("admin_zones") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_security),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.Notifications,
                        iconTint = IosRed,
                        title = stringResource(R.string.dashboard_alarms),
                        onClick = { onNavigate("admin_alarms") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.RecordVoiceOver,
                        iconTint = IosGreen,
                        title = stringResource(R.string.dashboard_live_activity),
                        onClick = { onNavigate("admin_live_activity") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_visitors_section),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.PeopleAlt,
                        iconTint = IosOrange,
                        title = stringResource(R.string.dashboard_guest_management),
                        onClick = { onNavigate("admin_guest_management") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_bookings_section),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.CalendarMonth,
                        iconTint = IosCyan,
                        title = stringResource(R.string.dashboard_bookings),
                        onClick = { onNavigate("admin_bookings") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_credentials_section),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.CreditCard,
                        iconTint = IosOrange,
                        title = stringResource(R.string.dashboard_cards),
                        onClick = { onNavigate("admin_cards") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.Key,
                        iconTint = IosCyan,
                        title = stringResource(R.string.dashboard_digital_credentials),
                        onClick = { onNavigate("admin_credentials") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_reports),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.BarChart,
                        iconTint = IosPurple,
                        title = stringResource(R.string.dashboard_analytics),
                        onClick = { onNavigate("admin_analytics") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.PersonSearch,
                        iconTint = IosIndigo,
                        title = stringResource(R.string.dashboard_user_presence),
                        onClick = { onNavigate("admin_user_presence") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.IosShare,
                        iconTint = IosOrange,
                        title = stringResource(R.string.dashboard_export_events),
                        onClick = { onNavigate("admin_export") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_access_control),
                actions = listOf(
                    DashboardAction(
                        icon = Icons.Outlined.AdminPanelSettings,
                        iconTint = IosRed,
                        title = stringResource(R.string.dashboard_access_rights),
                        onClick = { onNavigate("admin_access_rights") },
                    ),
                ),
            ),
        )
        add(
            DashboardSectionData(
                title = stringResource(R.string.dashboard_my_device),
                actions = listOf(
                    DashboardAction(
                        icon = MistyDoorIcon,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.dashboard_door_controllers),
                        onClick = { onNavigate("admin_controllers") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.SettingsInputAntenna,
                        iconTint = IosTeal,
                        title = stringResource(R.string.dashboard_gateways),
                        onClick = { onNavigate("admin_gateways") },
                    ),
                    DashboardAction(
                        icon = Icons.Outlined.Videocam,
                        iconTint = IosBlue,
                        title = stringResource(R.string.dashboard_cameras),
                        onClick = { onNavigate("admin_cameras") },
                    ),
                ),
            ),
        )
        if (orgId != null) {
            add(
                DashboardSectionData(
                    title = stringResource(R.string.dashboard_org_settings_section),
                    actions = listOf(
                        DashboardAction(
                            icon = Icons.Outlined.Settings,
                            iconTint = IosGray,
                            title = stringResource(R.string.dashboard_org_settings),
                            onClick = { onNavigate("admin_org_settings") },
                        ),
                    ),
                ),
            )
        }
    }

    MistyPage {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = MistyBottomNavInset),
        ) {
            item { MistyLargeTitle(text = stringResource(R.string.nav_dashboard)) }
            sections.forEachIndexed { index, section ->
                item {
                    DashboardSection(
                        section = section,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = if (index == 0) 0.dp else 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    MistySectionTitle(text = text, modifier = modifier)
}

@Composable
private fun DashboardSection(
    section: DashboardSectionData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(
            text = section.title,
            modifier = Modifier.padding(start = 14.dp),
        )
        MistyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                section.actions.forEachIndexed { index, action ->
                    DashboardRow(action = action)
                    if (index < section.actions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp, end = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRow(action: DashboardAction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = action.onClick)
            .padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = action.iconTint,
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = action.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
