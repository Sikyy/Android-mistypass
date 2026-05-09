package com.mistyislet.app.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPlacesUiState(
    val orgId: String? = null,
    val orgName: String? = null,
    val places: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class MyPlacesViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPlacesUiState())
    val uiState: StateFlow<MyPlacesUiState> = _uiState

    init {
        viewModelScope.launch {
            selectedPlaceRepository.scope
                .map { it.orgId to it.orgName }
                .distinctUntilChanged()
                .collect { (orgId, orgName) ->
                    _uiState.value = _uiState.value.copy(orgId = orgId, orgName = orgName)
                    if (orgId != null) {
                        refreshFor(orgId)
                    }
                }
        }
    }

    fun refresh() {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch { refreshFor(orgId) }
    }

    private suspend fun refreshFor(orgId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        when (val result = placeRepository.listPlaces(orgId)) {
            is ApiResult.Success -> {
                val places = result.data
                _uiState.value = _uiState.value.copy(places = places, isLoading = false)
                if (places.size == 1) {
                    selectedPlaceRepository.selectPlace(places.first().id, places.first().name)
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

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun select(place: Place) {
        viewModelScope.launch {
            selectedPlaceRepository.selectPlace(place.id, place.name)
        }
    }

    fun back() {
        viewModelScope.launch {
            selectedPlaceRepository.clearOrg()
        }
    }
}
