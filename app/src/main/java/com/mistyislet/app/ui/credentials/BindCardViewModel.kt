package com.mistyislet.app.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.CredentialRepository
import com.mistyislet.app.domain.model.NFCCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BindCardUiState(
    val isBinding: Boolean = false,
    val boundCard: NFCCard? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class BindCardViewModel @Inject constructor(
    private val credentialRepository: CredentialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BindCardUiState())
    val uiState: StateFlow<BindCardUiState> = _uiState

    fun bindCard(cardUid: String) {
        val nextUid = cardUid.trim()
        if (nextUid.isBlank() || _uiState.value.isBinding) return

        viewModelScope.launch {
            _uiState.value = BindCardUiState(isBinding = true)
            when (val result = credentialRepository.bindNFCCard(cardUid = nextUid)) {
                is ApiResult.Success -> _uiState.value = BindCardUiState(boundCard = result.data)
                is ApiResult.Error -> _uiState.value = BindCardUiState(errorMessage = result.message)
                is ApiResult.Exception -> _uiState.value = BindCardUiState(
                    errorMessage = result.throwable.localizedMessage ?: "Failed to bind card",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = BindCardUiState()
    }
}
