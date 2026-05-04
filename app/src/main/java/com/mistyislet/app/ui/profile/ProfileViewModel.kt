package com.mistyislet.app.ui.profile

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.auth.BiometricHelper
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.UserApi
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserInfo? = null,
    val isLoading: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent

    init {
        loadUser()
        loadBiometricState()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = safeApiCall { userApi.getCurrentUser() }) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(user = result.data, isLoading = false)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadBiometricState() {
        viewModelScope.launch {
            val available = biometricHelper.isAvailable()
            val prefs = dataStore.data.first()
            val enabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false
            val notifEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
            _uiState.value = _uiState.value.copy(
                biometricAvailable = available,
                biometricEnabled = enabled && available,
                notificationsEnabled = notifEnabled,
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
            _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_LANGUAGE] = languageCode }
        }
        // 使用 AppCompat per-app language API
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun logout() {
        authRepository.logout()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }
}
