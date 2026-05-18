package com.mistyislet.app.ui.doors

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.auth.BiometricHelper
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.MobileCredentialRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.domain.model.DoorRestriction
import com.mistyislet.app.domain.model.DoorSchedule
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.usecase.BLEUnlockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class DoorsTab { ALL, FAVORITES }
enum class DoorSort { NAME, STATUS, BUILDING }

data class DoorsUiState(
    val placeId: String? = null,
    val placeName: String? = null,
    val doors: List<AccessibleDoor> = emptyList(),
    val tab: DoorsTab = DoorsTab.ALL,
    val searchQuery: String = "",
    val sort: DoorSort = DoorSort.NAME,
    val isLockdown: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val lastSyncedAt: Instant? = null,
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
    val biometricHelper: BiometricHelper,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    private val _uiState = MutableStateFlow(DoorsUiState())
    val uiState: StateFlow<DoorsUiState> = _uiState

    val biometricEnabled = dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    private val _doorRestrictions = MutableStateFlow<List<DoorRestriction>>(emptyList())
    val doorRestrictions: StateFlow<List<DoorRestriction>> = _doorRestrictions
    private val _doorSchedules = MutableStateFlow<List<DoorSchedule>>(emptyList())
    val doorSchedules: StateFlow<List<DoorSchedule>> = _doorSchedules

    fun loadDoorExtras(doorId: String) {
        val pid = _uiState.value.placeId ?: return
        viewModelScope.launch {
            _doorRestrictions.value = emptyList()
            _doorSchedules.value = emptyList()
            when (val r = placeRepository.getDoorRestrictions(pid, doorId)) {
                is ApiResult.Success -> _doorRestrictions.value = r.data
                else -> {}
            }
            when (val r = placeRepository.getDoorSchedules(pid, doorId)) {
                is ApiResult.Success -> _doorSchedules.value = r.data
                else -> {}
            }
        }
    }

    init {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _uiState.value = _uiState.value.copy(isOffline = false)
            }
            override fun onLost(network: Network) {
                _uiState.value = _uiState.value.copy(isOffline = true)
            }
        })
        val activeNet = cm.activeNetwork
        val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
        _uiState.value = _uiState.value.copy(
            isOffline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true,
        )
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
            // Ensure NFC HCE has the userId (may be missing if registered before HCE was added)
            ensureHCEUserId()
            return
        }
        viewModelScope.launch {
            delay(2000)
            Log.i("DoorsVM", "No BLE credential found, registering...")
            when (val result = mobileCredentialRepo.registerCredential()) {
                is ApiResult.Success -> {
                    Log.i("DoorsVM", "BLE credential registered: ${result.data.id}")
                    result.data.userId?.let { uid ->
                        com.mistyislet.app.core.nfc.HceService.saveUserId(appContext, uid)
                        Log.i("DoorsVM", "NFC HCE userId saved: $uid")
                    }
                }
                is ApiResult.Error -> Log.e("DoorsVM", "BLE credential registration failed: ${result.message}")
                is ApiResult.Exception -> Log.e("DoorsVM", "BLE credential registration error", result.throwable)
            }
        }
    }

    /** Backfill userId into NFC HCE prefs for devices that registered before HCE was added. */
    private fun ensureHCEUserId() {
        // Migrate: clear any leftover plaintext prefs from before encrypted storage
        val legacyPrefs = appContext.getSharedPreferences("mistyislet_credential", android.content.Context.MODE_PRIVATE)
        val legacyUserId = legacyPrefs.getString("credential_user_id", null)
        if (legacyUserId != null) {
            com.mistyislet.app.core.nfc.HceService.saveUserId(appContext, legacyUserId)
            legacyPrefs.edit().clear().apply()
            Log.i("DoorsVM", "Migrated HCE userId from plaintext to encrypted storage")
            return
        }

        // Check encrypted prefs — if userId already present, nothing to do
        try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(appContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                appContext,
                "mistyislet_credential_enc",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            if (encPrefs.getString("credential_user_id", null) != null) return
        } catch (e: Exception) {
            Log.w("DoorsVM", "Could not read encrypted HCE prefs for backfill check", e)
        }
        viewModelScope.launch {
            when (val result = mobileCredentialRepo.listCredentials()) {
                is ApiResult.Success -> {
                    val active = result.data.firstOrNull { it.status == "active" }
                    active?.userId?.let { uid ->
                        com.mistyislet.app.core.nfc.HceService.saveUserId(appContext, uid)
                        Log.i("DoorsVM", "NFC HCE userId backfilled: $uid")
                    }
                }
                else -> Log.d("DoorsVM", "Could not fetch credentials for HCE backfill")
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
                    lastSyncedAt = Instant.now(),
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

    fun setSort(sort: DoorSort) {
        _uiState.value = _uiState.value.copy(sort = sort)
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
