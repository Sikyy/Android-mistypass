package com.mistyislet.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AccessLogRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.domain.model.EventMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val logs: List<AccessLog> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val accessLogRepository: AccessLogRepository,
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    private val _eventMedia = MutableStateFlow<List<EventMedia>>(emptyList())
    val eventMedia: StateFlow<List<EventMedia>> = _eventMedia
    private val _isLoadingMedia = MutableStateFlow(false)
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia

    fun loadEventMedia(eventId: String) {
        viewModelScope.launch {
            _eventMedia.value = emptyList()
            _isLoadingMedia.value = true
            val placeId = selectedPlaceRepository.scope.first().placeId
            if (placeId != null) {
                when (val r = placeRepository.getEventMedia(placeId, eventId)) {
                    is ApiResult.Success -> _eventMedia.value = r.data
                    else -> {}
                }
            }
            _isLoadingMedia.value = false
        }
    }

    private var currentPage = 1

    init {
        observeCached()
        refresh()
    }

    private fun observeCached() {
        viewModelScope.launch {
            accessLogRepository.getCachedLogs().collect { logs ->
                _uiState.value = _uiState.value.copy(logs = logs)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isLoading = true, hasMorePages = true)
            when (val result = accessLogRepository.refreshLogs(offset = 0)) {
                is ApiResult.Success -> {
                    val hasMore = result.data.size >= PAGE_SIZE
                    _uiState.value = _uiState.value.copy(isLoading = false, hasMorePages = hasMore)
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is ApiResult.Exception -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.throwable.localizedMessage)
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMorePages) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val offset = currentPage * PAGE_SIZE
            when (val result = accessLogRepository.refreshLogs(offset = offset, limit = PAGE_SIZE)) {
                is ApiResult.Success -> {
                    currentPage++
                    val hasMore = result.data.size >= PAGE_SIZE
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, hasMorePages = hasMore)
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
                is ApiResult.Exception -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
