package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.UserInfo
import retrofit2.http.GET

interface UserApi {

    @GET("app/me")
    suspend fun getCurrentUser(): UserInfo
}
