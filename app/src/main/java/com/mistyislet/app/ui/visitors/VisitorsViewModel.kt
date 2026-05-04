package com.mistyislet.app.ui.visitors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.DoorRepository
import com.mistyislet.app.domain.model.CreateVisitorPassRequest
import com.mistyislet.app.domain.model.VisitorPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class VisitorsUiState(
    val passes: List<VisitorPass> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val showCreateSheet: Boolean = false,
)

@HiltViewModel
class VisitorsViewModel @Inject constructor(
    private val doorRepository: DoorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitorsUiState())
    val uiState: StateFlow<VisitorsUiState> = _uiState

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    init {
        loadPasses()
    }

    fun loadPasses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = doorRepository.getVisitorPasses()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        passes = result.data,
                        isLoading = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _toastMessage.emit(result.message)
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _toastMessage.emit(result.throwable.localizedMessage ?: "Error")
                }
            }
        }
    }

    fun showCreateSheet() {
        _uiState.value = _uiState.value.copy(showCreateSheet = true)
    }

    fun hideCreateSheet() {
        _uiState.value = _uiState.value.copy(showCreateSheet = false)
    }

    fun createPass(visitor: String, method: String, hours: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            val expiresAt = Instant.now().plus(hours.toLong(), ChronoUnit.HOURS).toString()
            val request = CreateVisitorPassRequest(
                visitor = visitor,
                deliveryMethod = method,
                expiresAt = expiresAt,
            )
            when (val result = doorRepository.createVisitorPass(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isCreating = false, showCreateSheet = false)
                    _toastMessage.emit("Visitor pass created")
                    loadPasses()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isCreating = false)
                    _toastMessage.emit(result.message)
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isCreating = false)
                    _toastMessage.emit(result.throwable.localizedMessage ?: "Error")
                }
            }
        }
    }
}
