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

    @GET(MobileApiRoutes.fetchAppCredentialsRetrofitPath)
    suspend fun getCredentials(): ListResponse<Credential>

    @GET(MobileApiRoutes.getAppCredentialsNfcRetrofitPath)
    suspend fun listNFCCards(): List<NFCCard>

    @POST(MobileApiRoutes.postAppCredentialsNfcRetrofitPath)
    suspend fun bindNFCCard(@Body request: BindNFCCardRequest): NFCCard

    @DELETE(MobileApiRoutes.deleteAppCredentialsNfcCredentialIdRetrofitPath)
    suspend fun unbindNFCCard(@Path("credentialId") credentialId: String): UnbindNFCCardResponse
}
