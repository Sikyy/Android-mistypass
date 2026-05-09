package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.ChangePasswordRequest
import com.mistyislet.app.domain.model.UserInfo
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.domain.model.UserLoginListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {

    @GET("app/me")
    suspend fun getCurrentUser(): UserInfo

    @GET("app/me/logins")
    suspend fun getMyLogins(): UserLoginListResponse

    @DELETE("app/me/logins/{loginId}")
    suspend fun remoteLogout(@Path("loginId") loginId: String)

    @POST("app/me/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)
}
