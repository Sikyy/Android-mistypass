package com.mistyislet.app.ui.doors

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.DoorDisplayStatus
import com.mistyislet.app.domain.model.displayStatus
import com.mistyislet.app.ui.components.MistyBottomSheet
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyDoorIcon
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyPillActionButton
import com.mistyislet.app.ui.components.MistySearchField
import com.mistyislet.app.ui.components.MistySegmentedControl
import com.mistyislet.app.ui.components.MistyUnlockButtonHeight
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosYellow
import com.mistyislet.app.ui.theme.Success
import com.mistyislet.app.ui.theme.Warning
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoorsScreen(
    viewModel: DoorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDoor by remember { mutableStateOf<AccessibleDoor?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle(false)

    DoorsScreenContent(
        uiState = uiState,
        onBack = viewModel::back,
        onSearchChange = viewModel::setSearchQuery,
        onTabChange = viewModel::setTab,
        onRefresh = viewModel::refresh,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onToggleLockdown = viewModel::toggleLockdown,
        onDismissUnlockResult = viewModel::dismissUnlockResult,
        onTapDoor = { selectedDoor = it },
        onUnlock = { door ->
            if (biometricEnabled) {
                scope.launch {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val ok = viewModel.biometricHelper.authenticate(
                            activity,
                            title = context.getString(R.string.biometric_unlock_title),
                            subtitle = context.getString(R.string.biometric_unlock_subtitle, door.name),
                        )
                        if (ok) viewModel.unlock(door)
                    } else {
                        viewModel.unlock(door)
                    }
                }
            } else {
                viewModel.unlock(door)
            }
        },
    )

    // Door details bottom sheet
    selectedDoor?.let { door ->
        DoorDetailsSheet(
            door = door,
            placeId = uiState.placeId,
            viewModel = viewModel,
            onDismiss = { selectedDoor = null },
        )
    }
}

/**
 * Stateless Doors screen content — driven entirely by [uiState] + callbacks so it can be
 * rendered in a DEBUG harness / preview with mock data (no Hilt, no backend). This is the
 * exact UI shown in production; tune iOS-parity fidelity here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DoorsScreenContent(
    uiState: DoorsUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTabChange: (DoorsTab) -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (AccessibleDoor) -> Unit,
    onToggleLockdown: () -> Unit,
    onDismissUnlockResult: () -> Unit,
    onTapDoor: (AccessibleDoor) -> Unit,
    onUnlock: (AccessibleDoor) -> Unit,
) {
    val visibleDoors = remember(uiState.doors, uiState.tab, uiState.searchQuery, uiState.sort) {
        uiState.doors
            .filter { door ->
                if (uiState.tab == DoorsTab.FAVORITES && !door.isFavorite) return@filter false
                val q = uiState.searchQuery
                if (q.isBlank()) return@filter true
                door.name.contains(q, ignoreCase = true) ||
                    door.groupName?.contains(q, ignoreCase = true) == true
            }
            .let { list ->
                when (uiState.sort) {
                    DoorSort.NAME -> list.sortedBy { it.name }
                    DoorSort.STATUS -> list.sortedBy { it.displayStatus().ordinal }
                    DoorSort.BUILDING -> list.sortedBy { it.buildingId.ifBlank { "zzz" } }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(), // proper status-bar inset (cross-device) instead of hardcoded 56dp
        ) {
            // iOS 26 nav bar: teal chevron inside a circular white "glass" button + soft shadow.
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            clip = false,
                            spotColor = Color.Black.copy(alpha = 0.18f),
                            ambientColor = Color.Black.copy(alpha = 0.10f),
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.places_back),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Large title — iOS leaves generous space above & below it.
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.placeName ?: stringResource(R.string.nav_doors),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 37.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MistySearchField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = stringResource(R.string.search_doors),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            MistySegmentedControl(
                labels = listOf(
                    stringResource(R.string.doors_tab_all),
                    stringResource(R.string.doors_tab_favorites),
                ),
                selectedIndex = if (uiState.tab == DoorsTab.ALL) 0 else 1,
                onSelected = { index ->
                    onTabChange(if (index == 0) DoorsTab.ALL else DoorsTab.FAVORITES)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Offline banner
            if (uiState.isOffline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Warning.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Warning,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append(stringResource(R.string.doors_offline_banner))
                            uiState.lastSyncedAt?.let { syncTime ->
                                val duration = java.time.Duration.between(syncTime, java.time.Instant.now())
                                val relative = when {
                                    duration.toMinutes() < 1 -> stringResource(R.string.doors_last_synced, "just now")
                                    duration.toMinutes() < 60 -> stringResource(R.string.doors_last_synced, "${duration.toMinutes()}m ago")
                                    duration.toHours() < 24 -> stringResource(R.string.doors_last_synced, "${duration.toHours()}h ago")
                                    else -> stringResource(R.string.doors_last_synced, "${duration.toDays()}d ago")
                                }
                                append(" · ")
                                append(relative)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Door list
            Box(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (visibleDoors.isEmpty() && !uiState.isRefreshing) {
                        MistyEmptyState(
                            icon = if (uiState.tab == DoorsTab.FAVORITES) Icons.Default.StarBorder else Icons.Outlined.DoorFront,
                            title = stringResource(
                                if (uiState.tab == DoorsTab.FAVORITES) R.string.doors_no_favorites else R.string.doors_empty,
                            ),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = MistyBottomNavInset),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(visibleDoors, key = { it.id }) { door ->
                                DoorListCard(
                                    door = door,
                                    isUnlocking = uiState.unlockingDoorId == door.id,
                                    onUnlock = { onUnlock(door) },
                                    onTap = { onTapDoor(door) },
                                    onToggleFavorite = { onToggleFavorite(door) },
                                )
                            }
                        }
                    }
                }
            }

            // Lockdown banner (bottom)
            if (uiState.isLockdown) {
                LockdownBanner(onDisable = onToggleLockdown)
            }
        }

        // Full-screen unlock dialog overlay
        val unlockResult = uiState.unlockResult
        val unlockingDoorId = uiState.unlockingDoorId
        if (unlockResult != null || unlockingDoorId != null) {
            val dialogState = when {
                unlockResult?.success == true -> UnlockDialogState.Granted(
                    doorName = uiState.doors.find { it.id == unlockResult.doorId }?.name ?: "",
                )
                unlockResult?.success == false -> UnlockDialogState.Denied(
                    reason = unlockResult.message,
                )
                else -> UnlockDialogState.Loading
            }
            UnlockResultDialog(
                state = dialogState,
                onDismiss = onDismissUnlockResult,
            )
        }
    }
}

@Composable
private fun LockdownBanner(onDisable: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Danger)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.doors_lockdown_banner),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        MistyPillActionButton(
            text = stringResource(R.string.doors_disable_lockdown),
            onClick = onDisable,
            tint = Danger,
            containerColor = Color.White,
            contentColor = Danger,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoorDetailsSheet(
    door: AccessibleDoor,
    placeId: String?,
    viewModel: DoorsViewModel,
    onDismiss: () -> Unit,
) {
    val displayStatus = door.displayStatus()
    val restrictions by viewModel.doorRestrictions.collectAsStateWithLifecycle()
    val schedules by viewModel.doorSchedules.collectAsStateWithLifecycle()
    val isLockedDown = door.status == "locked_down"

    androidx.compose.runtime.LaunchedEffect(door.id) {
        viewModel.loadDoorExtras(door.id)
    }

    val statusColor = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> IosGreen
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> IosRed
        DoorDisplayStatus.OFFLINE -> IosGray
        DoorDisplayStatus.DISCONNECTED -> IosGray
    }

    val statusLabel = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> stringResource(R.string.doors_online)
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> stringResource(R.string.door_locked_down)
        DoorDisplayStatus.OFFLINE -> stringResource(R.string.door_offline)
        DoorDisplayStatus.DISCONNECTED -> stringResource(R.string.door_disconnected)
    }

    MistyBottomSheet(
        title = stringResource(R.string.doors_details),
        doneLabel = stringResource(R.string.common_done),
        onDismiss = onDismiss,
        fillsHeight = true,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MistyGroupedSection {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = door.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                MistyGroupedSection(title = stringResource(R.string.doors_info)) {
                    door.groupName?.let {
                        DoorDetailRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.doors_location),
                            value = it,
                        )
                        DoorSheetDivider()
                    }
                    DoorDetailRow(
                        icon = Icons.Default.Router,
                        label = stringResource(R.string.doors_gateway),
                        value = if (door.gatewayStatus == "online") stringResource(R.string.doors_online) else stringResource(R.string.door_offline),
                        valueColor = if (door.gatewayStatus == "online") IosGreen else IosOrange,
                    )
                    if (door.lastUnlockAt != null || door.kind != null || door.canUnlock) {
                        DoorSheetDivider()
                    }
                    door.lastUnlockAt?.let { timestamp ->
                        DoorDetailRow(
                            icon = Icons.Default.Lock,
                            label = stringResource(R.string.doors_last_unlocked),
                            value = timestamp.replace("T", " ").take(16),
                        )
                        DoorSheetDivider()
                    }
                    DoorDetailRow(
                        icon = Icons.Outlined.DoorFront,
                        label = stringResource(R.string.doors_type),
                        value = door.kind?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.doors_type_door),
                    )
                    DoorSheetDivider()
                    DoorDetailRow(
                        icon = Icons.Default.Circle,
                        iconTint = if (door.canUnlock) IosGreen else IosRed,
                        label = stringResource(R.string.doors_access),
                        value = if (door.canUnlock) stringResource(R.string.doors_access_allowed) else stringResource(R.string.doors_access_denied),
                        valueColor = if (door.canUnlock) IosGreen else IosRed,
                    )
                }
            }

            if (placeId != null) {
                item {
                    MistyGroupedSection(title = stringResource(R.string.doors_security)) {
                        DoorDetailRow(
                            icon = Icons.Default.Lock,
                            iconTint = if (isLockedDown) IosRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = stringResource(R.string.doors_lockdown),
                            value = if (isLockedDown) stringResource(R.string.doors_lockdown_active) else stringResource(R.string.doors_lockdown_inactive),
                            trailing = {
                                Switch(
                                    checked = isLockedDown,
                                    onCheckedChange = { viewModel.toggleLockdown() },
                                )
                            },
                        )
                    }
                }
            }

            if (restrictions.isNotEmpty()) {
                item {
                    MistyGroupedSection(title = stringResource(R.string.doors_restrictions)) {
                        restrictions.forEachIndexed { index, restriction ->
                            DoorDetailRow(
                                icon = Icons.Default.LocationOn,
                                iconTint = if (restriction.isEnabled) IosOrange else IosGray,
                                label = restriction.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                                value = restriction.radiusMeters?.let { "${it}m radius" } ?: "",
                                trailing = {
                                    Text(
                                        text = if (restriction.isEnabled) "Enabled" else "Disabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (restriction.isEnabled) IosGreen else IosGray,
                                    )
                                },
                            )
                            if (index < restrictions.lastIndex) {
                                DoorSheetDivider()
                            }
                        }
                    }
                }
            }

            if (schedules.isNotEmpty()) {
                item {
                    MistyGroupedSection(title = stringResource(R.string.doors_schedules)) {
                        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        schedules.forEachIndexed { index, schedule ->
                            DoorDetailRow(
                                icon = Icons.Default.Star,
                                label = schedule.name,
                                value = "${schedule.startTime} – ${schedule.endTime}\n" +
                                    schedule.daysOfWeek.mapNotNull { dayNames.getOrNull(it) }.joinToString(", "),
                            )
                            if (index < schedules.lastIndex) {
                                DoorSheetDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoorSheetDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp))
}

@Composable
private fun DoorDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (value.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun DoorListCard(
    door: AccessibleDoor,
    isUnlocking: Boolean,
    isBleReady: Boolean = false,
    onUnlock: () -> Unit,
    onTap: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val displayStatus = door.displayStatus()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val holdProgress = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val iconColor = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> IosBlue
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> IosRed
        DoorDisplayStatus.OFFLINE -> IosGray
        DoorDisplayStatus.DISCONNECTED -> IosOrange
    }
    val primaryStatusLabel = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> stringResource(R.string.doors_online)
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> stringResource(R.string.doors_lockdown)
        DoorDisplayStatus.OFFLINE -> stringResource(R.string.door_offline)
        DoorDisplayStatus.DISCONNECTED -> stringResource(R.string.door_disconnected)
    }
    val primaryStatusColor = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> IosBlue
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> IosRed
        DoorDisplayStatus.OFFLINE -> IosYellow
        DoorDisplayStatus.DISCONNECTED -> IosGray
    }

    val isUnlockable = displayStatus == DoorDisplayStatus.ONLINE_UNLOCKABLE && !isUnlocking

    MistyCard(
        cornerRadius = 16.dp,
        // iOS Liquid-Glass cards read as a low-contrast, slightly translucent panel — not a
        // crisp pure-white block. Soften the fill so it sits closer to the grouped background.
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        borderColor = Color.Transparent,
        onClick = onTap,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = if (
                        door.kind?.contains("parking", ignoreCase = true) == true ||
                        door.name.contains("parking", ignoreCase = true) ||
                        door.name.contains("gate", ignoreCase = true)
                    ) {
                        Icons.Outlined.DirectionsCar
                    } else {
                        MistyDoorIcon
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = door.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (isBleReady) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ble")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse),
                        label = "bleAlpha",
                    )
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Row(
                    modifier = Modifier.clickable(onClick = onToggleFavorite),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (door.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (door.isFavorite) IosYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(if (door.isFavorite) R.string.doors_saved else R.string.doors_save),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DoorStatusPill(
                    text = primaryStatusLabel,
                    textColor = primaryStatusColor,
                    backgroundColor = primaryStatusColor.copy(alpha = 0.16f),
                )
                if (displayStatus == DoorDisplayStatus.ONLINE_UNLOCKABLE) {
                    DoorStatusPill(
                        text = stringResource(R.string.door_unlockable),
                        textColor = IosGreen,
                        backgroundColor = IosGreen.copy(alpha = 0.16f),
                    )
                }
            }

            door.groupName?.takeIf { it.isNotBlank() }?.let { group ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = group,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (displayStatus == DoorDisplayStatus.OFFLINE || displayStatus == DoorDisplayStatus.DISCONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.door_controller_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Danger,
                )
            }

            if (isUnlockable) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MistyUnlockButtonHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holdJob?.cancel()
                                    holdJob = scope.launch {
                                        holdProgress.snapTo(0f)
                                        holdProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(500, easing = LinearEasing),
                                        )
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val vibrator = context.getSystemService(Vibrator::class.java)
                                        vibrator?.vibrate(
                                            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE),
                                        )
                                    }
                                    tryAwaitRelease()
                                    val didComplete = holdProgress.value >= 0.995f
                                    holdJob?.cancel()
                                    if (didComplete) {
                                        onUnlock()
                                    }
                                    holdProgress.snapTo(0f)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val progress = holdProgress.value.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (progress >= 1f) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (progress > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (progress >= 1f) R.string.door_release_to_unlock else R.string.door_hold_to_unlock,
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (progress > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoorStatusPill(
    text: String,
    textColor: Color,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}
