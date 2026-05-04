package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.Credential
import com.mistyislet.app.domain.model.ListResponse
import retrofit2.http.GET

interface CredentialApi {

    @GET("app/credentials")
    suspend fun getCredentials(): ListResponse<Credential>
}
