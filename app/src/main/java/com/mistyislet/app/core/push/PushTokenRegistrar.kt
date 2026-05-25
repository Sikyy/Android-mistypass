package com.mistyislet.app.core.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.repository.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface PushTokenRegistrar {
    fun registerCurrentToken()
    fun registerToken(token: String)
}

@Singleton
class FirebasePushTokenRegistrar @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val tokenStore: TokenStore,
) : PushTokenRegistrar {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun registerCurrentToken() {
        val firebaseMessaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "FCM token fetch skipped: Firebase is not configured", e)
            return
        }

        firebaseMessaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM token fetch failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (!token.isNullOrBlank()) {
                registerToken(token)
            }
        }
    }

    override fun registerToken(token: String) {
        if (token.isBlank() || tokenStore.accessToken == null) return

        scope.launch {
            when (val result = deviceRepository.registerFCMToken(token)) {
                is ApiResult.Success -> Log.i(TAG, "FCM token registered with backend")
                is ApiResult.Error -> Log.w(TAG, "FCM register failed: ${result.code} ${result.message}")
                is ApiResult.Exception -> Log.e(TAG, "FCM register error", result.throwable)
            }
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    @Binds
    @Singleton
    abstract fun bindPushTokenRegistrar(impl: FirebasePushTokenRegistrar): PushTokenRegistrar
}
