package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import com.mistyislet.app.domain.model.MagicLinkRequest
import com.mistyislet.app.domain.model.MagicLinkResponse
import com.mistyislet.app.domain.model.OrgAuthConfig
import com.mistyislet.app.domain.model.RestorePasswordRequest
import com.mistyislet.app.domain.model.VerifyMagicLinkRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    open suspend fun login(email: String, password: String, mfaCode: String? = null): ApiResult<LoginResponse> {
        return safeApiCall {
            val response = authApi.login(LoginRequest(email, password, mfaCode))
            storeTokens(response)
            response
        }
    }

    open suspend fun lookupOrg(domain: String): ApiResult<OrgAuthConfig> {
        return safeApiCall { authApi.orgLookup(domain) }
    }

    open suspend fun requestMagicLink(email: String): ApiResult<MagicLinkResponse> {
        return safeApiCall { authApi.requestMagicLink(MagicLinkRequest(email)) }
    }

    open suspend fun verifyMagicLink(token: String): ApiResult<LoginResponse> {
        return safeApiCall {
            val response = authApi.verifyMagicLink(VerifyMagicLinkRequest(token))
            storeTokens(response)
            response
        }
    }

    open suspend fun restorePassword(email: String): ApiResult<Unit> =
        safeApiCall { authApi.restorePassword(RestorePasswordRequest(email)) }

    open fun isLoggedIn(): Boolean = tokenStore.isValid()

    open fun logout() {
        tokenStore.clear()
    }

    private fun storeTokens(response: LoginResponse) {
        tokenStore.accessToken = response.accessToken
        tokenStore.refreshToken = response.refreshToken
        tokenStore.expiresAt = System.currentTimeMillis() + response.expiresIn * 1000L
    }
}
