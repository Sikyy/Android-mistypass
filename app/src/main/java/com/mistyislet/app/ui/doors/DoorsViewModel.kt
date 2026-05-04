package com.mistyislet.app.ui.doors

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.MobileCredentialRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.usecase.BLEUnlockUseCase
import com.mistyislet.app.domain.usecase.GetMyDoorsUseCase
import com.mistyislet.app.domain.usecase.UnlockDoorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoorsUiState(
    val doors: List<AccessibleDoor> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val unlockingDoorId: String? = null,
    val unlockResult: UnlockFeedback? = null,
    val buildings: List<String> = emptyList(),
    val selectedBuilding: String? = null, // null = 全部
)

data class UnlockFeedback(
    val doorId: String,
    val success: Boolean,
    val message: String,
    val method: String = "cloud", // "cloud" or "ble"
)

@HiltViewModel
class DoorsViewModel @Inject constructor(
    private val getMyDoorsUseCase: GetMyDoorsUseCase,
    private val unlockDoorUseCase: UnlockDoorUseCase,
    private val bleUnlockUseCase: BLEUnlockUseCase,
    private val mobileCredentialRepo: MobileCredentialRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        private val KEY_SELECTED_BUILDING = stringPreferencesKey("selected_building")
    }

    private val _uiState = MutableStateFlow(DoorsUiState())
    val uiState: StateFlow<DoorsUiState> = _uiState

    init {
        loadSavedBuilding()
        observeCachedDoors()
        refresh()
        startAutoRefresh()
        ensureBLECredential()
    }

    private fun loadSavedBuilding() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val saved = prefs[KEY_SELECTED_BUILDING]
            if (saved != null) {
                _uiState.value = _uiState.value.copy(selectedBuilding = saved)
            }
        }
    }

    /** Auto-register BLE credential if not yet present on this device. */
    private fun ensureBLECredential() {
        if (mobileCredentialRepo.hasLocalKeyPair()) {
            Log.i("DoorsVM", "BLE credential already exists in Keystore")
            return
        }
        viewModelScope.launch {
            // Wait for auth token to be stored after login
            delay(2000)
            Log.i("DoorsVM", "No BLE credential found, registering...")
            when (val result = mobileCredentialRepo.registerCredential()) {
                is ApiResult.Success -> Log.i("DoorsVM", "BLE credential registered: ${result.data.id}")
                is ApiResult.Error -> Log.e("DoorsVM", "BLE credential registration failed: ${result.message}")
                is ApiResult.Exception -> Log.e("DoorsVM", "BLE credential registration error", result.throwable)
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                silentRefresh()
            }
        }
    }

    private suspend fun silentRefresh() {
        getMyDoorsUseCase.refresh()
    }

    private fun observeCachedDoors() {
        viewModelScope.launch {
            getMyDoorsUseCase.getCached().collect { doors ->
                val buildings = doors.map { it.groupName ?: it.buildingId }.distinct()
                _uiState.value = _uiState.value.copy(doors = doors, buildings = buildings)
            }
        }
    }

    fun selectBuilding(building: String?) {
        _uiState.value = _uiState.value.copy(selectedBuilding = building)
        viewModelScope.launch {
            dataStore.edit {
                if (building == null) it.remove(KEY_SELECTED_BUILDING)
                else it[KEY_SELECTED_BUILDING] = building
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            when (val result = getMyDoorsUseCase.refresh()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = result.message,
                    )
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = result.throwable.localizedMessage,
                    )
                }
            }
        }
    }

    fun dismissUnlockResult() {
        _uiState.value = _uiState.value.copy(unlockResult = null, unlockingDoorId = null)
    }

    /** Cloud-based unlock (tap). App → Cloud API → Gateway → Unlock */
    fun unlock(door: AccessibleDoor) {
        if (!door.canUnlock) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(unlockingDoorId = door.id)

            when (val result = unlockDoorUseCase(door.id)) {
                is ApiResult.Success -> {
                    val response = result.data
                    _uiState.value = _uiState.value.copy(
                        unlockingDoorId = null,
                        unlockResult = UnlockFeedback(
                            doorId = door.id,
                            success = response.decision == "allow",
                            message = response.reason,
                            method = "cloud",
                        ),
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        unlockingDoorId = null,
                        unlockResult = UnlockFeedback(
                            doorId = door.id,
                            success = false,
                            message = result.message,
                            method = "cloud",
                        ),
                    )
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        unlockingDoorId = null,
                        unlockResult = UnlockFeedback(
                            doorId = door.id,
                            success = false,
                            message = result.throwable.localizedMessage ?: "Unknown error",
                            method = "cloud",
                        ),
                    )
                }
            }

            delay(3000)
            _uiState.value = _uiState.value.copy(unlockResult = null)
        }
    }

    /** BLE-based unlock (long press). App → BLE/TCP → Gateway → Unlock */
    fun bleUnlock(door: AccessibleDoor) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(unlockingDoorId = door.id)

            val result = bleUnlockUseCase.unlock()
            val feedback = when (result) {
                is BLEUnlockUseCase.UnlockResult.Success -> UnlockFeedback(
                    doorId = door.id,
                    success = true,
                    message = "BLE unlock success",
                    method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.Denied -> UnlockFeedback(
                    doorId = door.id,
                    success = false,
                    message = "BLE denied: ${result.reason}",
                    method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.Failed -> UnlockFeedback(
                    doorId = door.id,
                    success = false,
                    message = "BLE error: ${result.message}",
                    method = "ble",
                )
                is BLEUnlockUseCase.UnlockResult.NoCredential -> UnlockFeedback(
                    doorId = door.id,
                    success = false,
                    message = "No BLE credential. Register first.",
                    method = "ble",
                )
            }

            _uiState.value = _uiState.value.copy(
                unlockingDoorId = null,
                unlockResult = feedback,
            )

            delay(3000)
            _uiState.value = _uiState.value.copy(unlockResult = null)
        }
    }
}
