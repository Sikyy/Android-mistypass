package com.mistyislet.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.data.repository.AccessLogRepository
import com.mistyislet.app.domain.model.AccessLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val recentLogs: List<AccessLog> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accessLogRepository: AccessLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        viewModelScope.launch {
            accessLogRepository.getCachedLogs().collect { logs ->
                _uiState.value = DashboardUiState(recentLogs = logs.take(5))
            }
        }
    }
}
