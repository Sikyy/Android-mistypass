package com.mistyislet.app.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.data.api.UserApi
import com.mistyislet.app.data.repository.CredentialRepository
import com.mistyislet.app.domain.model.Credential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CredentialsUiState(
    val credentials: List<Credential> = emptyList(),
    val userId: String? = null,
    val dynamicQrContent: String? = null,
    val qrExpiresIn: Int = 300,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val userApi: UserApi,
    private val accessApi: AccessApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState

    init {
        observeCached()
        refresh()
        loadUser()
        startQrRefreshLoop()
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
        viewModelScope.launch {
            while (true) {
                refreshQrToken()
                // Refresh 30 seconds before expiry (token lasts 300s)
                delay(270_000L)
            }
        }
    }

    private suspend fun refreshQrToken() {
        when (val result = safeApiCall { accessApi.getBleToken() }) {
            is ApiResult.Success -> {
                val token = result.data
                _uiState.value = _uiState.value.copy(
                    dynamicQrContent = "mistyislet://ble/${token.bleToken}",
                    qrExpiresIn = token.expiresIn,
                )
            }
            else -> {
                // Fallback to static user-based QR
                val userId = _uiState.value.userId
                if (userId != null && _uiState.value.dynamicQrContent == null) {
                    _uiState.value = _uiState.value.copy(
                        dynamicQrContent = "mistyislet://access/$userId",
                    )
                }
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
    }
}
