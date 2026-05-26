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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.DoorDisplayStatus
import com.mistyislet.app.domain.model.displayStatus
import androidx.compose.material.icons.filled.Check
import com.mistyislet.app.ui.theme.Danger
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
    var showSortMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle(false)
    val segmentColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        activeContentColor = MaterialTheme.colorScheme.onSurface,
        activeBorderColor = MaterialTheme.colorScheme.outline,
        inactiveContainerColor = MaterialTheme.colorScheme.surface,
        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
        inactiveBorderColor = MaterialTheme.colorScheme.outline,
    )

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title row with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::back) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.places_back),
                    )
                }
                Text(
                    text = uiState.placeName ?: stringResource(R.string.nav_doors),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.SortByAlpha, contentDescription = stringResource(R.string.doors_sort))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DoorSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (sort) {
                                            DoorSort.NAME -> stringResource(R.string.doors_sort_name)
                                            DoorSort.STATUS -> stringResource(R.string.doors_sort_status)
                                            DoorSort.BUILDING -> stringResource(R.string.doors_sort_building)
                                        },
                                    )
                                },
                                onClick = { viewModel.setSort(sort); showSortMenu = false },
                                trailingIcon = {
                                    if (uiState.sort == sort) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                            )
                        }
                    }
                }
            }

            // All / Favorites segmented control
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                SegmentedButton(
                    selected = uiState.tab == DoorsTab.ALL,
                    onClick = { viewModel.setTab(DoorsTab.ALL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentColors,
                ) { Text(stringResource(R.string.doors_tab_all)) }
                SegmentedButton(
                    selected = uiState.tab == DoorsTab.FAVORITES,
                    onClick = { viewModel.setTab(DoorsTab.FAVORITES) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentColors,
                ) { Text(stringResource(R.string.doors_tab_favorites)) }
            }

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_doors),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (visibleDoors.isEmpty() && !uiState.isRefreshing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (uiState.tab == DoorsTab.FAVORITES) {
                                        Icons.Default.StarBorder
                                    } else {
                                        Icons.Default.DoorFront
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(
                                        if (uiState.tab == DoorsTab.FAVORITES) {
                                            R.string.doors_no_favorites
                                        } else {
                                            R.string.doors_empty
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(visibleDoors, key = { it.id }) { door ->
                                DoorListCard(
                                    door = door,
                                    isUnlocking = uiState.unlockingDoorId == door.id,
                                    onUnlock = {
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
                                    onTap = { selectedDoor = door },
                                    onToggleFavorite = { viewModel.toggleFavorite(door) },
                                )
                            }
                        }
                    }
                }
            }

            // Lockdown banner (bottom)
            if (uiState.isLockdown) {
                LockdownBanner(onDisable = viewModel::toggleLockdown)
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
                onDismiss = viewModel::dismissUnlockResult,
            )
        }
    }

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
        Button(
            onClick = onDisable,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Danger,
            ),
        ) {
            Text(stringResource(R.string.doors_disable_lockdown))
        }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayStatus = door.displayStatus()
    val restrictions by viewModel.doorRestrictions.collectAsStateWithLifecycle()
    val schedules by viewModel.doorSchedules.collectAsStateWithLifecycle()
    val isLockedDown = door.status == "locked_down"

    androidx.compose.runtime.LaunchedEffect(door.id) {
        viewModel.loadDoorExtras(door.id)
    }

    val statusColor = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> Success
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> Danger
        DoorDisplayStatus.OFFLINE -> Warning
        DoorDisplayStatus.DISCONNECTED -> MaterialTheme.colorScheme.outlineVariant
    }

    val statusLabel = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> stringResource(R.string.doors_online)
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> stringResource(R.string.door_locked_down)
        DoorDisplayStatus.OFFLINE -> stringResource(R.string.door_offline)
        DoorDisplayStatus.DISCONNECTED -> stringResource(R.string.door_disconnected)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = door.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            door.groupName?.let {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.doors_location)) },
                    supportingContent = { Text(it) },
                    leadingContent = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.doors_gateway)) },
                supportingContent = {
                    Text(
                        text = if (door.gatewayStatus == "online") stringResource(R.string.doors_online) else stringResource(R.string.door_offline),
                        color = if (door.gatewayStatus == "online") Success else Warning,
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            door.lastUnlockAt?.let { timestamp ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.doors_last_unlocked)) },
                    supportingContent = { Text(timestamp.replace("T", " ").take(16)) },
                    leadingContent = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.doors_type)) },
                supportingContent = { Text(door.kind?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.doors_type_door)) },
                leadingContent = {
                    Icon(Icons.Default.DoorFront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.doors_access)) },
                supportingContent = {
                    Text(
                        text = if (door.canUnlock) stringResource(R.string.doors_access_allowed) else stringResource(R.string.doors_access_denied),
                        color = if (door.canUnlock) Success else Danger,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (door.canUnlock) Success else Danger,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            // Lockdown toggle
            if (placeId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.doors_security),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.doors_lockdown)) },
                    supportingContent = {
                        Text(
                            if (isLockedDown) stringResource(R.string.doors_lockdown_active)
                            else stringResource(R.string.doors_lockdown_inactive),
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isLockedDown) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = isLockedDown,
                            onCheckedChange = { viewModel.toggleLockdown() },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            // Restrictions section
            if (restrictions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.doors_restrictions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                restrictions.forEach { restriction ->
                    ListItem(
                        headlineContent = {
                            Text(restriction.type.replace("_", " ").replaceFirstChar { it.uppercase() })
                        },
                        supportingContent = {
                            restriction.radiusMeters?.let { Text("${it}m radius") }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (restriction.isEnabled) Warning else MaterialTheme.colorScheme.outlineVariant,
                            )
                        },
                        trailingContent = {
                            Text(
                                text = if (restriction.isEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (restriction.isEnabled) Success else MaterialTheme.colorScheme.outlineVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            // Schedules section
            if (schedules.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.doors_schedules),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                schedules.forEach { schedule ->
                    ListItem(
                        headlineContent = { Text(schedule.name) },
                        supportingContent = {
                            Column {
                                Text("${schedule.startTime} – ${schedule.endTime}")
                                Text(
                                    schedule.daysOfWeek.mapNotNull { dayNames.getOrNull(it) }.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
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

    val statusColor = when (displayStatus) {
        DoorDisplayStatus.ONLINE_UNLOCKABLE -> Success
        DoorDisplayStatus.ONLINE_LOCKED_DOWN -> Danger
        DoorDisplayStatus.OFFLINE -> Warning
        DoorDisplayStatus.DISCONNECTED -> MaterialTheme.colorScheme.outlineVariant
    }

    val isUnlockable = displayStatus == DoorDisplayStatus.ONLINE_UNLOCKABLE && !isUnlocking

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = door.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
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
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (door.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(
                            if (door.isFavorite) R.string.doors_unfavorite else R.string.doors_favorite,
                        ),
                        tint = if (door.isFavorite) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = buildString {
                    door.groupName?.let { append(it) }
                    door.buildingId.takeIf { it.isNotBlank() }?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                }.ifEmpty { " " },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp),
            )

            if (displayStatus == DoorDisplayStatus.OFFLINE || displayStatus == DoorDisplayStatus.DISCONNECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.door_controller_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Danger,
                )
            }

            if (isUnlockable) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
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
                                        onUnlock()
                                    }
                                    tryAwaitRelease()
                                    if (holdProgress.value < 1f) {
                                        holdJob?.cancel()
                                        holdProgress.snapTo(0f)
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (holdProgress.value > 0f && holdProgress.value < 1f) {
                            CircularProgressIndicator(
                                progress = { holdProgress.value },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.door_hold_to_unlock),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
