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

@HiltViewModel
class AdminControllersViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AccessibleDoor>>(emptyList())
    val items: StateFlow<List<AccessibleDoor>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            when (val result = placeRepository.listPlaceDoors(placeId)) {
                is ApiResult.Success -> _items.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminControllersScreen(
    onBack: () -> Unit,
    viewModel: AdminControllersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

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
    )
}
