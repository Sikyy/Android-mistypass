package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.BindNFCCardRequest
import com.mistyislet.app.domain.model.Credential
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.NFCCard
import com.mistyislet.app.domain.model.UnbindNFCCardResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CredentialApi {

    @GET("app/credentials")
    suspend fun getCredentials(): ListResponse<Credential>

    @GET("app/credentials/nfc")
    suspend fun listNFCCards(): List<NFCCard>

    @POST("app/credentials/nfc")
    suspend fun bindNFCCard(@Body request: BindNFCCardRequest): NFCCard

    @DELETE("app/credentials/nfc/{credentialId}")
    suspend fun unbindNFCCard(@Path("credentialId") credentialId: String): UnbindNFCCardResponse
}
