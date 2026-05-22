package com.mistyislet.app.ui.visitors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.DoorRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.CreateVisitorPassRequest
import com.mistyislet.app.domain.model.VisitorGroup
import com.mistyislet.app.domain.model.VisitorGroupMember
import com.mistyislet.app.domain.model.VisitorPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class VisitorsUiState(
    val passes: List<VisitorPass> = emptyList(),
    val visitorGroup: VisitorGroup? = null,
    val groupMembers: List<VisitorGroupMember> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val showCreateSheet: Boolean = false,
)

@HiltViewModel
class VisitorsViewModel @Inject constructor(
    private val doorRepository: DoorRepository,
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitorsUiState())
    val uiState: StateFlow<VisitorsUiState> = _uiState

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    private var currentPlaceId: String? = null

    init {
        observePlace()
        loadPasses()
    }

    private fun observePlace() {
        viewModelScope.launch {
            selectedPlaceRepository.scope.collect { scope ->
                val placeId = scope.placeId
                if (placeId != currentPlaceId) {
                    currentPlaceId = placeId
                    if (placeId != null) fetchVisitorGroup(placeId)
                }
            }
        }
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

    private suspend fun fetchVisitorGroup(placeId: String) {
        when (val result = placeRepository.listVisitorGroups(placeId)) {
            is ApiResult.Success -> {
                val group = result.data.firstOrNull()
                _uiState.value = _uiState.value.copy(visitorGroup = group)
                if (group != null) fetchGroupMembers(placeId, group.id)
            }
            else -> {}
        }
    }

    private suspend fun fetchGroupMembers(placeId: String, groupId: String) {
        when (val result = placeRepository.listGroupMembers(placeId, groupId)) {
            is ApiResult.Success -> {
                _uiState.value = _uiState.value.copy(groupMembers = result.data)
            }
            else -> {}
        }
    }

    fun cleanupExpired() {
        val placeId = currentPlaceId ?: return
        val group = _uiState.value.visitorGroup ?: return
        viewModelScope.launch {
            when (placeRepository.cleanupExpiredMembers(placeId, group.id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        groupMembers = _uiState.value.groupMembers.filter { it.isActive && !isMemberExpired(it) },
                    )
                }
                else -> {}
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
            val request = CreateVisitorPassRequest(
                visitor = visitor,
                deliveryMethod = method,
                ttlHours = hours.toDouble(),
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

fun isMemberExpired(member: VisitorGroupMember): Boolean {
    val expiresAt = member.expiresAt ?: return false
    return try {
        Instant.parse(expiresAt).isBefore(Instant.now())
    } catch (_: Exception) {
        false
    }
}
