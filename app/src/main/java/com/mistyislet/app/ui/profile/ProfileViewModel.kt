package com.mistyislet.app.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.BuildConfig
import com.mistyislet.app.core.auth.BiometricHelper
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.UserApi
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.ChangePasswordRequest
import com.mistyislet.app.domain.model.UserInfo
import com.mistyislet.app.domain.model.UserLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    val biometricTypeName: String = "Biometric",
    val notificationsEnabled: Boolean = true,
    val geofenceEnabled: Boolean = false,
    val logins: List<UserLogin> = emptyList(),
    val isLoadingLogins: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val authRepository: AuthRepository,
    val biometricHelper: BiometricHelper,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_GEOFENCE_ENABLED = booleanPreferencesKey("geofence_enabled")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent

    val appVersion: String = BuildConfig.VERSION_NAME
    val buildNumber: String = BuildConfig.VERSION_CODE.toString()
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidVersion: String = "Android ${Build.VERSION.RELEASE}"

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
            val geoEnabled = prefs[KEY_GEOFENCE_ENABLED] ?: false
            val typeName = biometricHelper.biometricTypeName()
            _uiState.value = _uiState.value.copy(
                biometricAvailable = available,
                biometricEnabled = enabled && available,
                biometricTypeName = typeName,
                notificationsEnabled = notifEnabled,
                geofenceEnabled = geoEnabled,
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
            _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
        }
    }

    fun toggleGeofence(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_GEOFENCE_ENABLED] = enabled }
            _uiState.value = _uiState.value.copy(geofenceEnabled = enabled)
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
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun fetchLogins() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLogins = true)
            when (val result = safeApiCall { userApi.getMyLogins() }) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    logins = result.data.items,
                    isLoadingLogins = false,
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingLogins = false)
            }
        }
    }

    fun remoteLogout(login: UserLogin) {
        viewModelScope.launch {
            when (safeApiCall { userApi.remoteLogout(login.id) }) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        logins = _uiState.value.logins.filter { it.id != login.id },
                    )
                }
                else -> {}
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(passwordChangeError = null, passwordChangeSuccess = false)
            when (safeApiCall { userApi.changePassword(ChangePasswordRequest(currentPassword, newPassword)) }) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(passwordChangeSuccess = true)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(passwordChangeError = "Failed to change password")
                }
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", "avatar.jpg", body)
            when (val result = safeApiCall { userApi.uploadAvatar(part) }) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(user = result.data)
                else -> {}
            }
        }
    }

    fun clearPasswordState() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false, passwordChangeError = null)
    }

    fun logout() {
        authRepository.logout()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }
}
