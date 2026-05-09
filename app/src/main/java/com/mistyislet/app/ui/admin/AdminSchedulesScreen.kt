package com.mistyislet.app.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.R
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AdminSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSchedulesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminSchedule>>(emptyList())
    val items: StateFlow<List<AdminSchedule>> = _items
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            val placeId = selectedPlaceRepository.scope.first().placeId ?: return@launch
            when (val result = adminRepository.getSchedules(placeId)) {
                is ApiResult.Success -> _items.value = result.data
                else -> {}
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun AdminSchedulesScreen(
    onBack: () -> Unit,
    viewModel: AdminSchedulesViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AdminListScreen(
        title = stringResource(R.string.dashboard_schedules),
        items = items.map { schedule ->
            val timeRange = listOfNotNull(schedule.startTime, schedule.endTime).joinToString(" – ")
            AdminListItem(
                id = schedule.id,
                title = schedule.name,
                subtitle = if (schedule.days.isNotEmpty()) schedule.days.joinToString(", ") else timeRange.ifBlank { null },
            )
        },
        isLoading = isLoading,
        emptyMessage = stringResource(R.string.dashboard_no_data),
        onBack = onBack,
    )
}
