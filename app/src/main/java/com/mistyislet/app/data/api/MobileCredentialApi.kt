package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.MobileCredential
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

/**
 * API endpoints for BLE mobile credential management.
 * These complement the existing CredentialApi (which handles wallet passes/physical cards).
 */
interface MobileCredentialApi {

    @POST("app/credentials/register")
    suspend fun registerCredential(
        @Body request: RegisterMobileCredentialRequest
    ): Response<RegisterMobileCredentialResponse>

    @GET("app/credentials/mobile")
    suspend fun listMobileCredentials(): ListResponse<MobileCredential>

    @DELETE("app/credentials/mobile/{credentialId}")
    suspend fun revokeMobileCredential(
        @Path("credentialId") credentialId: String
    ): Response<RevokeCredentialResponse>

    @POST("app/credentials/mobile/{credentialId}/refresh")
    suspend fun refreshMobileCredential(
        @Path("credentialId") credentialId: String
    ): Response<RegisterMobileCredentialResponse>
}

@Serializable
data class RegisterMobileCredentialRequest(
    @SerialName("public_key_pem") val publicKeyPem: String,
    val platform: String = "android",
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_model") val deviceModel: String,
    @SerialName("keystore_level") val keystoreLevel: String,
    @SerialName("attestation_cert_chain") val attestationCertChain: List<String> = emptyList(),
)

@Serializable
data class RegisterMobileCredentialResponse(
    val credential: MobileCredential,
)

@Serializable
data class RevokeCredentialResponse(
    val status: String,
    val id: String? = null,
    val message: String? = null,
)
