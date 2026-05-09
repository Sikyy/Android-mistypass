package com.mistyislet.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

data class GatewayGroup(
    val id: String,
    val name: String,
    val status: String,
    val doorCount: Int,
    val doors: List<AccessibleDoor> = emptyList(),
)

@HiltViewModel
class AdminGatewaysViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<GatewayGroup>>(emptyList())
    val items: StateFlow<List<GatewayGroup>> = _items
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

    fun renameGateway(gatewayId: String, name: String) {
        viewModelScope.launch {
            adminRepository.renameGateway(gatewayId, name)
            loadData()
        }
    }

    private suspend fun loadData() {
        val pid = placeId ?: return
        when (val result = placeRepository.listPlaceDoors(pid)) {
            is ApiResult.Success -> {
                val grouped: Map<String, List<AccessibleDoor>> =
                    result.data.groupBy { door -> door.gatewayId ?: door.gatewayStatus }
                _items.value = grouped.map { (key, doors) ->
                    GatewayGroup(
                        id = key,
                        name = doors.first().gatewayName ?: "Gateway $key",
                        status = doors.first().gatewayStatus,
                        doorCount = doors.size,
                        doors = doors,
                    )
                }.sortedBy { it.name }
                _error.value = null
            }
            is ApiResult.Error -> _error.value = result.message
            is ApiResult.Exception -> _error.value = result.throwable.localizedMessage
        }
        _isLoading.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGatewaysScreen(
    onBack: () -> Unit,
    viewModel: AdminGatewaysViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedGateway by remember { mutableStateOf<GatewayGroup?>(null) }
    var renameTarget by remember { mutableStateOf<GatewayGroup?>(null) }
    var renameText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val online = items.count { it.status.lowercase() == "online" }
    val offline = items.count { it.status.lowercase() != "online" }

    AdminListScreen(
        title = stringResource(R.string.dashboard_gateways),
        items = items.map { gw ->
            AdminListItem(
                id = gw.id,
                title = gw.name,
                subtitle = stringResource(R.string.admin_doors_count, gw.doorCount),
                trailing = gw.status.replaceFirstChar { it.uppercase() },
                trailingColor = if (gw.status == "online") Color(0xFF35A853) else Color(0xFFD93025),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            selectedGateway = items.find { it.id == item.id }
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

    selectedGateway?.let { gw ->
        ModalBottomSheet(
            onDismissRequest = { selectedGateway = null },
            sheetState = sheetState,
        ) {
            GatewayDetailSheet(
                gateway = gw,
                onRename = {
                    renameText = gw.name
                    renameTarget = gw
                    selectedGateway = null
                },
            )
        }
    }

    renameTarget?.let { gw ->
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
                            viewModel.renameGateway(gw.id, renameText.trim())
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
private fun GatewayDetailSheet(gateway: GatewayGroup, onRename: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = gateway.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_rename), modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = gateway.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = if (gateway.status.lowercase() == "online") Color(0xFF35A853) else Color(0xFFD93025),
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.admin_doors_count, gateway.doorCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.admin_doors),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (gateway.doors.isEmpty()) {
            Text(
                text = stringResource(R.string.admin_no_doors),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(gateway.doors, key = { it.id }) { door ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.DoorFront,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = door.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                door.groupName?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = door.status.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (door.status.lowercase()) {
                                    "online" -> Color(0xFF35A853)
                                    "offline" -> Color(0xFFD93025)
                                    else -> Color(0xFF9E9E9E)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
