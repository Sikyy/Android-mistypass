package com.mistyislet.app.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.data.api.UserApi
import com.mistyislet.app.data.repository.CredentialRepository
import com.mistyislet.app.data.repository.MobileCredentialRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.Credential
import com.mistyislet.app.domain.model.MobileCredential
import com.mistyislet.app.domain.model.QRTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class CredentialsUiState(
    val credentials: List<Credential> = emptyList(),
    val mobileCredentials: List<MobileCredential> = emptyList(),
    val userId: String? = null,
    val organizationName: String = "Mistyislet",
    val placeName: String? = null,
    val qrDoors: List<AccessibleDoor> = emptyList(),
    val selectedQrDoorId: String? = null,
    val dynamicQrContent: String? = null,
    val qrExpiresAt: Instant? = null,
    val isQrLoading: Boolean = false,
    val qrErrorMessage: String? = null,
    val pinCode: String? = null,
    val pinExpiresAt: Instant? = null,
    val pinPeriodSecs: Int = 30,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val mobileCredentialRepository: MobileCredentialRepository,
    private val userApi: UserApi,
    private val accessApi: AccessApi,
    private val selectedPlaceRepository: SelectedPlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState

    private var qrRefreshJob: Job? = null
    private var pinRefreshJob: Job? = null

    init {
        observeSelectedScope()
        observeCached()
        refresh()
        loadUser()
        loadQrDoors()
        loadMobileCredentials()
        startQrRefreshLoop()
        startPinRefreshLoop()
    }

    private fun observeSelectedScope() {
        viewModelScope.launch {
            selectedPlaceRepository.scope.collect { scope ->
                _uiState.value = _uiState.value.copy(
                    organizationName = scope.orgName ?: _uiState.value.organizationName,
                    placeName = scope.placeName ?: _uiState.value.placeName,
                )
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            when (val result = safeApiCall { userApi.getCurrentUser() }) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(userId = result.data.id)
                }
                else -> {}
            }
        }
    }

    private fun startQrRefreshLoop() {
        qrRefreshJob?.cancel()
        qrRefreshJob = viewModelScope.launch {
            while (true) {
                refreshQrToken()
                val expiresAt = _uiState.value.qrExpiresAt
                val waitMs = if (expiresAt != null) {
                    val remaining = expiresAt.toEpochMilli() - System.currentTimeMillis() - 2000
                    remaining.coerceAtLeast(1000L)
                } else {
                    25_000L
                }
                delay(waitMs)
            }
        }
    }

    private fun startPinRefreshLoop() {
        pinRefreshJob?.cancel()
        pinRefreshJob = viewModelScope.launch {
            refreshPinCode()
            while (true) {
                val expiresAt = _uiState.value.pinExpiresAt
                val waitMs = if (expiresAt != null) {
                    val remaining = expiresAt.toEpochMilli() - System.currentTimeMillis()
                    remaining.coerceAtLeast(500L)
                } else {
                    (_uiState.value.pinPeriodSecs * 1000L)
                }
                delay(waitMs)
                refreshPinCode()
            }
        }
    }

    private fun loadQrDoors() {
        viewModelScope.launch {
            when (val result = safeApiCall { accessApi.getMyDoors().items }) {
                is ApiResult.Success -> {
                    val doors = result.data
                    val selectedDoorId = _uiState.value.selectedQrDoorId
                        ?.takeIf { id -> doors.any { it.id == id } }
                        ?: doors.firstOrNull()?.id
                    _uiState.value = _uiState.value.copy(
                        qrDoors = doors,
                        selectedQrDoorId = selectedDoorId,
                    )
                    refreshQrToken()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(qrErrorMessage = result.message)
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(qrErrorMessage = result.throwable.localizedMessage)
                }
            }
        }
    }

    private suspend fun refreshQrToken() {
        _uiState.value = _uiState.value.copy(isQrLoading = true, qrErrorMessage = null)
        when (val result = safeApiCall { accessApi.getQrToken(QRTokenRequest(doorId = _uiState.value.selectedQrDoorId)) }) {
            is ApiResult.Success -> {
                val token = result.data
                val expiresAt = try {
                    Instant.parse(token.expiresAt)
                } catch (_: Exception) {
                    Instant.now().plusSeconds(300)
                }
                _uiState.value = _uiState.value.copy(
                    dynamicQrContent = token.token,
                    qrExpiresAt = expiresAt,
                    isQrLoading = false,
                    qrErrorMessage = null,
                )
            }
            is ApiResult.Error -> refreshBleQrFallback(result.message)
            is ApiResult.Exception -> refreshBleQrFallback(result.throwable.localizedMessage)
        }
    }

    private suspend fun refreshBleQrFallback(errorMessage: String?) {
        when (val result = safeApiCall { accessApi.getBleToken() }) {
            is ApiResult.Success -> {
                val token = result.data
                _uiState.value = _uiState.value.copy(
                    dynamicQrContent = "mistyislet://ble/${token.bleToken}",
                    qrExpiresAt = Instant.now().plusSeconds(token.expiresIn.toLong()),
                    isQrLoading = false,
                    qrErrorMessage = null,
                )
            }
            else -> {
                val userId = _uiState.value.userId
                if (userId != null && _uiState.value.dynamicQrContent == null) {
                    _uiState.value = _uiState.value.copy(
                        dynamicQrContent = "mistyislet://access/$userId",
                        qrExpiresAt = Instant.now().plusSeconds(25),
                        isQrLoading = false,
                        qrErrorMessage = null,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isQrLoading = false,
                        qrErrorMessage = errorMessage,
                    )
                }
            }
        }
    }

    private suspend fun refreshPinCode() {
        when (val result = safeApiCall { accessApi.getPinCode() }) {
            is ApiResult.Success -> {
                val pin = result.data
                val expiresAt = try {
                    Instant.parse(pin.validUntil)
                } catch (_: Exception) {
                    Instant.now().plusSeconds(pin.periodSecs.toLong())
                }
                _uiState.value = _uiState.value.copy(
                    pinCode = pin.pin,
                    pinExpiresAt = expiresAt,
                    pinPeriodSecs = pin.periodSecs,
                )
            }
            else -> {
                if (_uiState.value.pinCode == null) {
                    val randomPin = String.format("%06d", (0..999999).random())
                    _uiState.value = _uiState.value.copy(
                        pinCode = randomPin,
                        pinExpiresAt = Instant.now().plusSeconds(30),
                        pinPeriodSecs = 30,
                    )
                }
            }
        }
    }

    fun manualRefreshQr() {
        viewModelScope.launch { refreshQrToken() }
    }

    fun selectQrDoor(doorId: String) {
        if (doorId == _uiState.value.selectedQrDoorId) return
        _uiState.value = _uiState.value.copy(
            selectedQrDoorId = doorId,
            dynamicQrContent = null,
            qrExpiresAt = null,
        )
        manualRefreshQr()
    }

    fun manualRefreshPin() {
        viewModelScope.launch { refreshPinCode() }
    }

    private fun loadMobileCredentials() {
        viewModelScope.launch {
            when (val result = mobileCredentialRepository.listCredentials()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(mobileCredentials = result.data)
                }
                else -> {}
            }
        }
    }

    private fun observeCached() {
        viewModelScope.launch {
            credentialRepository.getCachedCredentials().collect { credentials ->
                _uiState.value = _uiState.value.copy(credentials = credentials)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = credentialRepository.refreshCredentials()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                is ApiResult.Exception -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.throwable.localizedMessage)
            }
        }
        loadMobileCredentials()
    }

    override fun onCleared() {
        super.onCleared()
        qrRefreshJob?.cancel()
        pinRefreshJob?.cancel()
    }
}
