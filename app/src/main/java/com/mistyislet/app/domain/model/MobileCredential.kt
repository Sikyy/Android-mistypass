package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A registered mobile BLE credential.
 * The private key lives in Android Keystore; this model tracks the cloud-side state.
 */
@Serializable
data class MobileCredential(
    val id: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    val platform: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("keystore_level") val keystoreLevel: String? = null,
    val status: String, // "active", "revoked", "expired"
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)
