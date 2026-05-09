package com.mistyislet.app.ui.doors

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.MobileCredentialRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.usecase.BLEUnlockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DoorsTab { ALL, FAVORITES }

data class DoorsUiState(
    val placeId: String? = null,
    val placeName: String? = null,
    val doors: List<AccessibleDoor> = emptyList(),
    val tab: DoorsTab = DoorsTab.ALL,
    val searchQuery: String = "",
    val isLockdown: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val unlockingDoorId: String? = null,
    val unlockResult: UnlockFeedback? = null,
)

data class UnlockFeedback(
    val doorId: String,
    val success: Boolean,
    val message: String,
    val method: String = "cloud",
)

@HiltViewModel
class DoorsViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val selectedPlaceRepository: SelectedPlaceRepository,
    private val bleUnlockUseCase: BLEUnlockUseCase,
    private val mobileCredentialRepo: MobileCredentialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoorsUiState())
    val uiState: StateFlow<DoorsUiState> = _uiState

    init {
        viewModelScope.launch {
            selectedPlaceRepository.scope
                .map { it.placeId to it.placeName }
                .distinctUntilChanged()
                .collect { (placeId, placeName) ->
                    _uiState.value = _uiState.value.copy(
                        placeId = placeId,
                        placeName = placeName,
                        doors = if (placeId == null) emptyList() else _uiState.value.doors,
                    )
                    if (placeId != null) {
                        refreshFor(placeId)
                    }
                }
        }
        ensureBLECredential()
    }

    private fun ensureBLECredential() {
        if (mobileCredentialRepo.hasLocalKeyPair()) {
            Log.i("DoorsVM", "BLE credential already exists in Keystore")
            return
        }
        viewModelScope.launch {
            delay(2000)
            Log.i("DoorsVM", "No BLE credential found, registering...")
            when (val result = mobileCredentialRepo.registerCredential()) {
                is ApiResult.Success -> Log.i("DoorsVM", "BLE credential registered: ${result.data.id}")
                is ApiResult.Error -> Log.e("DoorsVM", "BLE credential registration failed: ${result.message}")
                is ApiResult.Exception -> Log.e("DoorsVM", "BLE credential registration error", result.throwable)
            }
        }
    }

    fun refresh() {
        val placeId = _uiState.value.placeId ?: return
        viewModelScope.launch { refreshFor(placeId) }
    }

    private suspend fun refreshFor(placeId: String) {
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        when (val result = placeRepository.listPlaceDoors(placeId)) {
            is ApiResult.Success -> {
                val doors = result.data
                val anyLockedDown = doors.any { it.status == "locked_down" }
                _uiState.value = _uiState.value.copy(
                    doors = doors,
                    isLockdown = anyLockedDown,
                    isRefreshing = false,
                )
            }
            is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                isRefreshing = false, errorMessage = result.message,
            )
            is ApiResult.Exception -> _uiState.value = _uiState.value.copy(
                isRefreshing = false, errorMessage = result.throwable.localizedMessage,
            )
        }
    }

    fun setTab(tab: DoorsTab) {
        _uiState.value = _uiState.value.copy(tab = tab)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun back() {
        viewModelScope.launch {
            selectedPlaceRepository.clearPlace()
        }
    }

    fun toggleFavorite(door: AccessibleDoor) {
        val placeId = _uiState.value.placeId ?: return
        viewModelScope.launch {
            val result = if (door.isFavorite) {
                placeRepository.unfavoriteDoor(placeId, door.id)
            } else {
                placeRepository.favoriteDoor(placeId, door.id)
            }
            if (result is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(
                    doors = _uiState.value.doors.map {
                        if (it.id == door.id) it.copy(isFavorite = !door.isFavorite) else it
                    },
                )
            }
        }
    }

    fun toggleLockdown() {
        val placeId = _uiState.value.placeId ?: return
        val current = _uiState.value.isLockdown
        viewModelScope.launch {
            val result = if (current) {
                placeRepository.disableLockdown(placeId)
            } else {
                placeRepository.enableLockdown(placeId)
            }
            if (result is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(isLockdown = !current)
                refreshFor(placeId)
            }
        }
    }

    fun dismissUnlockResult() {
        _uiState.value = _uiState.value.copy(unlockResult = null, unlockingDoorId = null)
    }

    fun unlock(door: AccessibleDoor) {
        if (!door.canUnlock) return
        val placeId = _uiState.value.placeId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(unlockingDoorId = door.id)
            val feedback = when (val result = placeRepository.unlockPlaceDoor(placeId, door.id)) {
                is ApiResult.Success -> UnlockFeedback(
                    doorId = door.id,
                    success = result.data.decision == "allow",
                    message = result.data.reason,
                    method = "cloud",
                )
                is ApiResult.Error -> UnlockFeedback(
                    doorId = door.id, success = false, message = result.message, method = "cloud",
                )
                is ApiResult.Exception -> UnlockFeedback(
                    doorId = door.id,
                    success = false,
                    message = result.throwable.localizedMessage ?: "Unknown error",
                    method = "cloud",
                )
            }
            _uiState.value = _uiState.value.copy(unlockingDoorId = null, unlockResult = feedback)
            delay(3000)
            _uiState.value = _uiState.value.copy(unlockResult = null)
        }
    }

    fun bleUnlock(door: AccessibleDoor) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(unlockingDoorId = door.id)
            val result = bleUnlockUseCase.unlock()
            val feedback = when (result) {
                is BLEUnlockUseCase.UnlockResult.Success -> UnlockFeedback(
                    doorId = door.id, success = true, message = "BLE unlock success", method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.Denied -> UnlockFeedback(
                    doorId = door.id, success = false, message = "BLE denied: ${result.reason}", method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.Failed -> UnlockFeedback(
                    doorId = door.id, success = false, message = "BLE error: ${result.message}", method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.NoCredential -> UnlockFeedback(
                    doorId = door.id, success = false, message = "No BLE credential. Register first.", method = "ble",
                )
            }
            _uiState.value = _uiState.value.copy(unlockingDoorId = null, unlockResult = feedback)
            delay(3000)
            _uiState.value = _uiState.value.copy(unlockResult = null)
        }
    }
}
