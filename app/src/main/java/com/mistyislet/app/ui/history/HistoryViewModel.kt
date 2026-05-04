package com.mistyislet.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AccessLogRepository
import com.mistyislet.app.domain.model.AccessLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val logs: List<AccessLog> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val accessLogRepository: AccessLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

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
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = accessLogRepository.refreshLogs()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is ApiResult.Exception -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.throwable.localizedMessage)
            }
        }
    }
}
