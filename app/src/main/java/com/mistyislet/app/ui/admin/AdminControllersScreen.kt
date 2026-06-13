package com.mistyislet.app.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.DoorDisplayStatus
import com.mistyislet.app.domain.model.displayStatus
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.theme.IosGray
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminControllersViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val demoFallback: AdminDemoFallback,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AccessibleDoor>>(emptyList())
    val items: StateFlow<List<AccessibleDoor>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var placeId: String? = null

    init {
        viewModelScope.launch {
            placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            loadData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun renameDoor(doorId: String, name: String) {
        val pid = placeId ?: return
        viewModelScope.launch {
            adminRepository.renameDoor(pid, doorId, name)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        val state = demoFallback.resolveList(placeRepository.listPlaceDoors(pid)) { AdminDemoData.accessibleDoors }
        _items.value = state.items
        _error.value = state.error
        _isLoading.value = false
    }
}

@Composable
fun AdminControllersScreen(
    onBack: () -> Unit,
    viewModel: AdminControllersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var renameTarget by remember { mutableStateOf<AccessibleDoor?>(null) }
    var renameText by remember { mutableStateOf("") }

    val online = items.count { it.status.lowercase() != "offline" && it.gatewayStatus.lowercase() == "online" }
    val offline = items.size - online

    AdminListScreen(
        title = stringResource(R.string.dashboard_door_controllers),
        items = items.map { door ->
            AdminListItem(
                id = door.id,
                title = door.name,
                subtitle = listOfNotNull(door.groupName, door.gatewayName).joinToString(" · ").ifBlank { null },
                trailing = controllerStatusLabel(door),
                trailingColor = controllerStatusColor(door),
                trailingChip = true,
                leadingDotColor = controllerStatusColor(door),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.hardware_no_controllers),
        emptyDescription = stringResource(R.string.hardware_no_controllers_description),
        emptyIcon = Icons.Default.DoorFront,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        listSectionTitle = stringResource(R.string.hardware_all_controllers),
        onItemLongClick = { item ->
            val door = items.find { it.id == item.id }
            if (door != null) {
                renameText = door.name
                renameTarget = door
            }
        },
        headerContent = {
            StatusSummaryRow(
                items = listOf(
                    KpiItem(online.toString(), stringResource(R.string.admin_online), IosGreen),
                    KpiItem(offline.toString(), stringResource(R.string.admin_offline), if (offline > 0) IosRed else IosGray),
                    KpiItem(items.size.toString(), stringResource(R.string.admin_total), MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    )

    renameTarget?.let { door ->
        MistyAlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.admin_rename)) },
            text = {
                MistyFormTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = stringResource(R.string.admin_enter_new_name),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameDoor(door.id, renameText.trim())
                            renameTarget = null
                        }
                    },
                    enabled = renameText.isNotBlank(),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun controllerStatusLabel(door: AccessibleDoor): String = when (door.displayStatus()) {
    DoorDisplayStatus.ONLINE_UNLOCKABLE -> stringResource(R.string.doors_online)
    DoorDisplayStatus.ONLINE_LOCKED_DOWN -> stringResource(R.string.doors_lockdown)
    DoorDisplayStatus.OFFLINE -> stringResource(R.string.door_offline)
    DoorDisplayStatus.DISCONNECTED -> stringResource(R.string.door_disconnected)
}

private fun controllerStatusColor(door: AccessibleDoor): Color = when (door.displayStatus()) {
    DoorDisplayStatus.ONLINE_UNLOCKABLE -> IosGreen
    DoorDisplayStatus.ONLINE_LOCKED_DOWN -> IosOrange
    DoorDisplayStatus.OFFLINE -> IosRed
    DoorDisplayStatus.DISCONNECTED -> IosGray
}
