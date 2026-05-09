package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import com.mistyislet.app.domain.model.RefreshRequest
import com.mistyislet.app.domain.model.RefreshResponse
import com.mistyislet.app.domain.model.RestorePasswordRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("app/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("app/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST("app/auth/restore-password")
    suspend fun restorePassword(@Body request: RestorePasswordRequest)
}
