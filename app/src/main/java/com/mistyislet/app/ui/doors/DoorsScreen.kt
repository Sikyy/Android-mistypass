package com.mistyislet.app.ui.doors

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.DoorDisplayStatus
import com.mistyislet.app.domain.model.displayStatus
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success
import com.mistyislet.app.ui.theme.Warning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SortMode { NAME, STATUS, BUILDING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoorsScreen(
    viewModel: DoorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedDoor by remember { mutableStateOf<AccessibleDoor?>(null) }

    val filteredDoors = remember(uiState.doors, searchQuery, uiState.selectedBuilding, sortMode) {
        uiState.doors.filter { door ->
            val matchesSearch = searchQuery.isBlank() ||
                door.name.contains(searchQuery, ignoreCase = true) ||
                door.groupName?.contains(searchQuery, ignoreCase = true) == true
            val matchesBuilding = uiState.selectedBuilding == null ||
                (door.groupName ?: door.buildingId) == uiState.selectedBuilding
            matchesSearch && matchesBuilding
        }.let { doors ->
            when (sortMode) {
                SortMode.NAME -> doors.sortedBy { it.name }
                SortMode.STATUS -> doors.sortedBy { it.displayStatus().ordinal }
                SortMode.BUILDING -> doors.sortedBy { it.groupName ?: it.buildingId }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title row with sort button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(R.string.doors_sort),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doors_sort_name)) },
                            onClick = { sortMode = SortMode.NAME; showSortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doors_sort_status)) },
                            onClick = { sortMode = SortMode.STATUS; showSortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.doors_sort_building)) },
                            onClick = { sortMode = SortMode.BUILDING; showSortMenu = false },
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
            )

            // Building filter chips
            if (uiState.buildings.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedBuilding == null,
                            onClick = { viewModel.selectBuilding(null) },
                            label = { Text("All") },
                        )
                    }
                    items(uiState.buildings) { building ->
                        FilterChip(
                            selected = uiState.selectedBuilding == building,
                            onClick = { viewModel.selectBuilding(building) },
                            label = { Text(building) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Door list
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (filteredDoors.isEmpty() && !uiState.isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DoorFront,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.doors_empty),
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
                        items(filteredDoors, key = { it.id }) { door ->
                            DoorListCard(
                                door = door,
                                isUnlocking = uiState.unlockingDoorId == door.id,
                                onUnlock = { viewModel.unlock(door) },
                                onTap = { selectedDoor = door },
                            )
                        }
                    }
                }
            }
        }

        // Full-screen unlock dialog overlay
        val unlockResult = uiState.unlockResult
        val unlockingDoorId = uiState.unlockingDoorId
        if (unlockResult != null || unlockingDoorId != null) {
            val dialogState = when {
                unlockResult?.success == true -> UnlockDialogState.Granted(
                    doorName = uiState.doors.find { it.id == unlockResult.doorId }?.name ?: ""
                )
                unlockResult?.success == false -> UnlockDialogState.Denied(
                    reason = unlockResult.message
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
            onDismiss = { selectedDoor = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoorDetailsSheet(
    door: AccessibleDoor,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val displayStatus = door.displayStatus()

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
            // Door name + status
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

            // Info rows
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

            ListItem(
                headlineContent = { Text(stringResource(R.string.doors_type)) },
                supportingContent = { Text(stringResource(R.string.doors_type_door)) },
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
        }
    }
}

@Composable
private fun DoorListCard(
    door: AccessibleDoor,
    isUnlocking: Boolean,
    onUnlock: () -> Unit,
    onTap: () -> Unit,
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
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header row: door name + status dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = door.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
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
                    )
                }
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = statusColor,
                )
            }

            // Offline status text
            if (displayStatus == DoorDisplayStatus.OFFLINE || displayStatus == DoorDisplayStatus.DISCONNECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.door_controller_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Danger,
                )
            }

            // Hold to Unlock button
            if (isUnlockable) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
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
                                            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
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
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.door_hold_to_unlock),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
