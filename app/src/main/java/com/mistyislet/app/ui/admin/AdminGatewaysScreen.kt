package com.mistyislet.app.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
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
)

@HiltViewModel
class AdminGatewaysViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<GatewayGroup>>(emptyList())
    val items: StateFlow<List<GatewayGroup>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            when (val result = placeRepository.listPlaceDoors(placeId)) {
                is ApiResult.Success -> {
                    val grouped: Map<String, List<AccessibleDoor>> =
                        result.data.groupBy { door -> door.gatewayId ?: door.gatewayStatus }
                    _items.value = grouped.map { (key, doors) ->
                        GatewayGroup(
                            id = key,
                            name = doors.first().gatewayName ?: "Gateway $key",
                            status = doors.first().gatewayStatus,
                            doorCount = doors.size,
                        )
                    }.sortedBy { it.name }
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminGatewaysScreen(
    onBack: () -> Unit,
    viewModel: AdminGatewaysViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_gateways),
        items = items.map { gw ->
            AdminListItem(
                id = gw.id,
                title = gw.name,
                subtitle = "${gw.doorCount} doors",
                trailing = gw.status.replaceFirstChar { it.uppercase() },
                trailingColor = if (gw.status == "online") Color(0xFF35A853) else Color(0xFFD93025),
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
    )
}
