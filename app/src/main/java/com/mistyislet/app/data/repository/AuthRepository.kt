package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return safeApiCall {
            val response = authApi.login(LoginRequest(email, password))
            tokenStore.accessToken = response.accessToken
            tokenStore.refreshToken = response.refreshToken
            tokenStore.expiresAt = System.currentTimeMillis() + response.expiresIn * 1000L
            response
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.isValid()

    fun logout() {
        tokenStore.clear()
    }
}
