package com.mistyislet.app.core.network

import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore,
    private val authApiProvider: () -> AuthApi,
) : Interceptor {

    private val mutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.accessToken
            ?: return chain.proceed(chain.request())

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(request)
        if (response.code != 401) return response

        response.close()

        return runBlocking {
            mutex.withLock {
                if (tokenStore.isValid()) {
                    val retryRequest = chain.request().newBuilder()
                        .header("Authorization", "Bearer ${tokenStore.accessToken}")
                        .build()
                    return@runBlocking chain.proceed(retryRequest)
                }

                val refreshToken = tokenStore.refreshToken
                if (refreshToken == null) {
                    tokenStore.clear()
                    return@runBlocking chain.proceed(chain.request())
                }

                try {
                    val refreshResult = authApiProvider().refresh(RefreshRequest(refreshToken))
                    tokenStore.accessToken = refreshResult.accessToken
                    tokenStore.refreshToken = refreshResult.refreshToken
                    tokenStore.expiresAt = System.currentTimeMillis() + refreshResult.expiresIn * 1000L

                    val retryRequest = chain.request().newBuilder()
                        .header("Authorization", "Bearer ${refreshResult.accessToken}")
                        .build()
                    chain.proceed(retryRequest)
                } catch (e: Exception) {
                    tokenStore.clear()
                    chain.proceed(chain.request())
                }
            }
        }
    }
}
