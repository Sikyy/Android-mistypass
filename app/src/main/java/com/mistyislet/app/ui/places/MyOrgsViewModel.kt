package com.mistyislet.app.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.Organization
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyOrgsUiState(
    val orgs: List<Organization> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class MyOrgsViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyOrgsUiState())
    val uiState: StateFlow<MyOrgsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = placeRepository.listOrgs()) {
                is ApiResult.Success -> {
                    val orgs = result.data
                    _uiState.value = _uiState.value.copy(orgs = orgs, isLoading = false)
                    if (orgs.size == 1) {
                        selectedPlaceRepository.selectOrg(orgs.first().id, orgs.first().name)
                    }
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message,
                )
                is ApiResult.Exception -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.throwable.localizedMessage,
                )
            }
        }
    }

    fun select(org: Organization) {
        viewModelScope.launch {
            selectedPlaceRepository.selectOrg(org.id, org.name)
        }
    }
}
