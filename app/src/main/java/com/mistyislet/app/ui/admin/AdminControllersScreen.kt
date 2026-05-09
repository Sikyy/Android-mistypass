package com.mistyislet.app.ui.admin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
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
        when (val result = placeRepository.listPlaceDoors(pid)) {
            is ApiResult.Success -> { _items.value = result.data; _error.value = null }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
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

    val online = items.count { it.status.lowercase() == "online" }
    val offline = items.count { it.status.lowercase() == "offline" }

    AdminListScreen(
        title = stringResource(R.string.dashboard_door_controllers),
        items = items.map { door ->
            AdminListItem(
                id = door.id,
                title = door.name,
                subtitle = listOfNotNull(door.groupName, door.gatewayName).joinToString(" · ").ifBlank { null },
                trailing = door.status.replaceFirstChar { it.uppercase() },
                trailingColor = when (door.status.lowercase()) {
                    "online" -> Color(0xFF35A853)
                    "offline" -> Color(0xFFD93025)
                    "locked_down" -> Color(0xFFFF9800)
                    else -> Color(0xFF9E9E9E)
                },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            val door = items.find { it.id == item.id }
            if (door != null) {
                renameText = door.name
                renameTarget = door
            }
        },
        headerContent = {
            StatusSummaryRow(
                items = listOf(
                    KpiItem(online.toString(), stringResource(R.string.admin_online), Color(0xFF35A853)),
                    KpiItem(offline.toString(), stringResource(R.string.admin_offline), Color(0xFFD93025)),
                    KpiItem(items.size.toString(), stringResource(R.string.admin_total), Color(0xFF4285F4)),
                ),
            )
        },
    )

    renameTarget?.let { door ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.admin_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.admin_enter_new_name)) },
                    singleLine = true,
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
