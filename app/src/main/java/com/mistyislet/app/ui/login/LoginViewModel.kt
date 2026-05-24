package com.mistyislet.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.push.PushTokenRegistrar
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.OrgAuthConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthStep {
    EmailInput,
    OrgLookupLoading,
    PasswordInput,
    MfaInput,
    MagicLinkSent,
    SSORedirect,
}

data class LoginUiState(
    val authStep: AuthStep = AuthStep.EmailInput,
    val email: String = "",
    val password: String = "",
    val mfaCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orgAuthConfig: OrgAuthConfig? = null,
    val forgotPasswordSent: Boolean = false,
    val forgotPasswordError: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess: SharedFlow<Unit> = _loginSuccess

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onMfaCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(
            mfaCode = code.filter { it.isDigit() }.take(8),
            errorMessage = null,
        )
    }

    fun submitEmail() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) return

        val domain = email.substringAfter("@")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authStep = AuthStep.OrgLookupLoading, errorMessage = null)

            when (val result = authRepository.lookupOrg(domain)) {
                is ApiResult.Success -> {
                    val config = result.data
                    val nextStep = when (config.authType) {
                        "sso", "saml" -> AuthStep.SSORedirect
                        else -> AuthStep.PasswordInput
                    }
                    _uiState.value = _uiState.value.copy(authStep = nextStep, orgAuthConfig = config)
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(authStep = AuthStep.PasswordInput, orgAuthConfig = null)
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        val nextMfaCode = state.mfaCode.trim().takeIf { it.isNotBlank() }
        if (state.authStep == AuthStep.MfaInput && nextMfaCode == null) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.login(state.email, state.password, nextMfaCode)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, mfaCode = "")
                    pushTokenRegistrar.registerCurrentToken()
                    _loginSuccess.emit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = when {
                        result.isMFARequired() -> _uiState.value.copy(
                            authStep = AuthStep.MfaInput,
                            isLoading = false,
                            mfaCode = "",
                            errorMessage = null,
                        )
                        result.isInvalidMFACode() -> _uiState.value.copy(
                            authStep = AuthStep.MfaInput,
                            isLoading = false,
                            errorMessage = result.message,
                        )
                        else -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.throwable.localizedMessage)
                }
            }
        }
    }

    fun requestMagicLink() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (authRepository.requestMagicLink(email)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(authStep = AuthStep.MagicLinkSent, isLoading = false)
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to send login link")
                }
            }
        }
    }

    fun verifyMagicLink(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (authRepository.verifyMagicLink(token)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    pushTokenRegistrar.registerCurrentToken()
                    _loginSuccess.emit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Login link expired or invalid")
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Network error")
                }
            }
        }
    }

    fun goBack() {
        val state = _uiState.value
        if (state.authStep == AuthStep.MfaInput) {
            _uiState.value = state.copy(
                authStep = AuthStep.PasswordInput,
                mfaCode = "",
                errorMessage = null,
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            authStep = AuthStep.EmailInput,
            password = "",
            mfaCode = "",
            errorMessage = null,
            orgAuthConfig = null,
        )
    }

    fun restorePassword(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            when (authRepository.restorePassword(email)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(forgotPasswordSent = true, forgotPasswordError = null)
                is ApiResult.Error, is ApiResult.Exception -> _uiState.value = _uiState.value.copy(forgotPasswordError = "Failed to send reset email")
            }
        }
    }

    fun clearForgotPasswordState() {
        _uiState.value = _uiState.value.copy(forgotPasswordSent = false, forgotPasswordError = null)
    }

    private fun ApiResult.Error.isMFARequired(): Boolean =
        errorCode == "mfa_required" || message.contains("mfa code is required", ignoreCase = true)

    private fun ApiResult.Error.isInvalidMFACode(): Boolean =
        errorCode == "invalid_mfa_code" || message.contains("invalid admin mfa code", ignoreCase = true)
}
