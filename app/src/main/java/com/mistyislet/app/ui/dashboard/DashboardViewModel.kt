package com.mistyislet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val orgId: String? = null,
    val placeId: String? = null,
    val placeName: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        viewModelScope.launch {
            selectedPlaceRepository.scope.collect { scope ->
                _uiState.value = DashboardUiState(
                    orgId = scope.orgId,
                    placeId = scope.placeId,
                    placeName = scope.placeName,
                )
            }
        }
    }
}
