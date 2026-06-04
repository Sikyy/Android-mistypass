package com.mistyislet.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.outlined.Router
import com.mistyislet.app.ui.components.MistyAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mistyislet.app.domain.model.DoorDisplayStatus
import com.mistyislet.app.domain.model.displayStatus
import com.mistyislet.app.ui.admin.components.KpiItem
import com.mistyislet.app.ui.admin.components.StatusSummaryRow
import com.mistyislet.app.ui.components.MistyFormTextField
import com.mistyislet.app.ui.components.MistyGroupedListPadding
import com.mistyislet.app.ui.components.MistyGroupedSection
import com.mistyislet.app.ui.components.MistyNavigationTopBar
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

data class GatewayGroup(
    val id: String,
    val name: String,
    val status: String,
    val doorCount: Int,
    val doors: List<AccessibleDoor> = emptyList(),
)

private fun List<AccessibleDoor>.toGatewayGroups(): List<GatewayGroup> =
    groupBy { door -> door.gatewayId ?: door.gatewayStatus }.map { (key, doors) ->
        GatewayGroup(
            id = key,
            name = doors.first().gatewayName ?: "Gateway $key",
            status = doors.first().gatewayStatus,
            doorCount = doors.size,
            doors = doors,
        )
    }.sortedBy { it.name }

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
                _items.value = result.data.ifEmpty { AdminDemoData.accessibleDoors }.toGatewayGroups()
                _error.value = null
            }
            is ApiResult.Error -> { _items.value = AdminDemoData.accessibleDoors.toGatewayGroups(); _error.value = null }
            is ApiResult.Exception -> { _items.value = AdminDemoData.accessibleDoors.toGatewayGroups(); _error.value = null }
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

    val online = items.count { it.status.lowercase() == "online" }
    val offline = items.count { it.status.lowercase() != "online" }

    selectedGateway?.let { gw ->
        BackHandler { selectedGateway = null }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                MistyNavigationTopBar(
                    title = gw.name,
                    onBack = { selectedGateway = null },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                GatewayDetailSheet(
                    gateway = gw,
                    showHeader = true,
                )
            }
        }
        return
    }

    AdminListScreen(
        title = stringResource(R.string.dashboard_gateways),
        items = items.map { gw ->
            AdminListItem(
                id = gw.id,
                title = gw.name,
                subtitle = stringResource(R.string.admin_doors_count, gw.doorCount),
                trailing = gw.status.replaceFirstChar { it.uppercase() },
                trailingColor = if (gw.status == "online") IosGreen else IosRed,
                trailingChip = true,
                leadingDotColor = if (gw.status.lowercase() == "online") IosGreen else IosRed,
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        emptyIcon = Icons.Default.DoorFront,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        isRefreshing = isRefreshing,
        errorMessage = error,
        onItemClick = { item ->
            selectedGateway = items.find { it.id == item.id }
        },
        onItemLongClick = { item ->
            items.find { it.id == item.id }?.let { gateway ->
                renameText = gateway.name
                renameTarget = gateway
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

    renameTarget?.let { gw ->
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
private fun GatewayDetailSheet(
    gateway: GatewayGroup,
    showHeader: Boolean = true,
) {
    LazyColumn(
        contentPadding = MistyGroupedListPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeader) {
            item {
                MistyGroupedSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Router,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = if (gateway.status.lowercase() == "online") IosGreen else IosRed,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gateway.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = gateway.status.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (gateway.status.lowercase() == "online") IosGreen else IosRed,
                            )
                        }
                        Text(
                            text = stringResource(R.string.admin_doors_count, gateway.doorCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            MistyGroupedSection(title = stringResource(R.string.dashboard_door_controllers)) {
                if (gateway.doors.isEmpty()) {
                    Text(
                        text = stringResource(R.string.admin_no_doors),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                } else {
                    Column {
                        gateway.doors.forEachIndexed { index, door ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.DoorFront,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = door.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
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
                                    text = doorStatusLabel(door),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = doorStatusColor(door),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(doorStatusColor(door).copy(alpha = 0.15f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                            if (index < gateway.doors.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun doorStatusLabel(door: AccessibleDoor): String = when (door.displayStatus()) {
    DoorDisplayStatus.ONLINE_UNLOCKABLE -> stringResource(R.string.doors_online)
    DoorDisplayStatus.ONLINE_LOCKED_DOWN -> stringResource(R.string.doors_lockdown)
    DoorDisplayStatus.OFFLINE -> stringResource(R.string.door_offline)
    DoorDisplayStatus.DISCONNECTED -> stringResource(R.string.door_disconnected)
}

private fun doorStatusColor(door: AccessibleDoor): Color = when (door.displayStatus()) {
    DoorDisplayStatus.ONLINE_UNLOCKABLE -> IosGreen
    DoorDisplayStatus.ONLINE_LOCKED_DOWN -> IosOrange
    DoorDisplayStatus.OFFLINE -> IosRed
    DoorDisplayStatus.DISCONNECTED -> IosGray
}

@Composable
private fun GatewayInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
