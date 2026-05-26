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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.ui.components.MistyListCard
import com.mistyislet.app.ui.theme.Ash
import com.mistyislet.app.ui.theme.Brass
import com.mistyislet.app.ui.theme.Copper
import com.mistyislet.app.ui.theme.Moss
import com.mistyislet.app.ui.theme.Success
import com.mistyislet.app.ui.theme.Teal
import com.mistyislet.app.ui.theme.Warning

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.placeId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_dashboard),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Activity
            item { SectionHeader(stringResource(R.string.dashboard_activity)) }
            item {
                DashboardRow(
                    icon = Icons.Default.History,
                    iconTint = Success,
                    title = stringResource(R.string.dashboard_event_history),
                    onClick = { onNavigate("history") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_events),
                    onClick = { onNavigate("admin_events") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Warning,
                    iconTint = Warning,
                    title = stringResource(R.string.dashboard_incidents),
                    onClick = { onNavigate("admin_incidents") },
                )
            }

            // Management
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_management)) }
            item {
                DashboardRow(
                    icon = Icons.Default.People,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_users),
                    onClick = { onNavigate("admin_users") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.PeopleAlt,
                    iconTint = Moss,
                    title = stringResource(R.string.dashboard_groups),
                    onClick = { onNavigate("admin_groups") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Groups,
                    iconTint = Brass,
                    title = stringResource(R.string.dashboard_teams),
                    onClick = { onNavigate("admin_teams") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Schedule,
                    iconTint = Copper,
                    title = stringResource(R.string.dashboard_schedules),
                    onClick = { onNavigate("admin_schedules") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Map,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_zones),
                    onClick = { onNavigate("admin_zones") },
                )
            }

            // Security
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_security)) }
            item {
                DashboardRow(
                    icon = Icons.Default.Notifications,
                    iconTint = Warning,
                    title = stringResource(R.string.dashboard_alarms),
                    onClick = { onNavigate("admin_alarms") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.PersonSearch,
                    iconTint = Success,
                    title = stringResource(R.string.dashboard_live_activity),
                    onClick = { onNavigate("admin_live_activity") },
                )
            }

            // Visitors
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_visitors_section)) }
            item {
                DashboardRow(
                    icon = Icons.Default.PeopleAlt,
                    iconTint = Brass,
                    title = stringResource(R.string.dashboard_guest_management),
                    onClick = { onNavigate("admin_guest_management") },
                )
            }

            // Bookings
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_bookings_section)) }
            item {
                DashboardRow(
                    icon = Icons.Default.CalendarMonth,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_bookings),
                    onClick = { onNavigate("admin_bookings") },
                )
            }

            // Credentials
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_credentials_section)) }
            item {
                DashboardRow(
                    icon = Icons.Default.CreditCard,
                    iconTint = Brass,
                    title = stringResource(R.string.dashboard_cards),
                    onClick = { onNavigate("admin_cards") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Key,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_digital_credentials),
                    onClick = { onNavigate("admin_credentials") },
                )
            }

            // Reports
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_reports)) }
            item {
                DashboardRow(
                    icon = Icons.Default.BarChart,
                    iconTint = Moss,
                    title = stringResource(R.string.dashboard_analytics),
                    onClick = { onNavigate("admin_analytics") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.PersonSearch,
                    iconTint = Brass,
                    title = stringResource(R.string.dashboard_user_presence),
                    onClick = { onNavigate("admin_user_presence") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Upload,
                    iconTint = Copper,
                    title = stringResource(R.string.dashboard_export_events),
                    onClick = { onNavigate("admin_export") },
                )
            }

            // Access Control
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_access_control)) }
            item {
                DashboardRow(
                    icon = Icons.Default.Shield,
                    iconTint = Warning,
                    title = stringResource(R.string.dashboard_access_rights),
                    onClick = { onNavigate("admin_access_rights") },
                )
            }

            // My Device
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SectionHeader(stringResource(R.string.dashboard_my_device)) }
            item {
                DashboardRow(
                    icon = Icons.Default.MeetingRoom,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.dashboard_door_controllers),
                    onClick = { onNavigate("admin_controllers") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.Router,
                    iconTint = Teal,
                    title = stringResource(R.string.dashboard_gateways),
                    onClick = { onNavigate("admin_gateways") },
                )
            }
            item {
                DashboardRow(
                    icon = Icons.Default.CameraAlt,
                    iconTint = Moss,
                    title = stringResource(R.string.dashboard_cameras),
                    onClick = { onNavigate("admin_cameras") },
                )
            }

            // Org Settings (conditional)
            if (uiState.orgId != null) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader(stringResource(R.string.dashboard_org_settings_section)) }
                item {
                    DashboardRow(
                    icon = Icons.Default.Settings,
                    iconTint = Ash,
                    title = stringResource(R.string.dashboard_org_settings),
                        onClick = { onNavigate("admin_org_settings") },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun DashboardRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
) {
    MistyListCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
            )
        }
    }
}
