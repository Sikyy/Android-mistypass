package com.mistyislet.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.login(state.email, state.password)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _loginSuccess.emit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                    )
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.throwable.localizedMessage,
                    )
                }
            }
        }
    }
}
