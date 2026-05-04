package com.mistyislet.app.core.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 生物识别认证助手。
 * 支持指纹、人脸识别，设备 PIN 作为后备。
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class BiometricStatus {
        AVAILABLE,          // 可用
        NO_HARDWARE,        // 设备无生物识别硬件
        NOT_ENROLLED,       // 硬件有但用户未注册
        UNAVAILABLE,        // 暂时不可用
    }

    fun getStatus(): BiometricStatus {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isAvailable(): Boolean = getStatus() == BiometricStatus.AVAILABLE

    /**
     * 显示生物识别弹窗并等待结果。
     * 需要从 FragmentActivity 调用。
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Verify Identity",
        subtitle: String = "Use biometric to continue",
    ): Boolean = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (cont.isActive) cont.resume(false)
            }

            override fun onAuthenticationFailed() {
                // 单次失败不结束流程，等用户重试或取消
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)

        cont.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
}
