package com.mistyislet.app.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.DeviceApi
import com.mistyislet.app.domain.model.RegisterDeviceRequest
import com.mistyislet.app.domain.model.RegisterDeviceResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceApi: DeviceApi,
    @ApplicationContext private val context: Context,
) {
    suspend fun registerFCMToken(token: String): ApiResult<RegisterDeviceResponse> {
        return safeApiCall {
            deviceApi.registerDevice(
                RegisterDeviceRequest(
                    fcmToken = token,
                    deviceId = deviceId(),
                    deviceModel = deviceModel(),
                ),
            )
        }
    }

    private fun deviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "android-${Build.FINGERPRINT.hashCode()}"
    }

    private fun deviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
