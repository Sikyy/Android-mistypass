package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credential(
    val id: String,
    @SerialName("credential_kind") val credentialKind: String,
    val provider: String? = null,
    val status: String,
    @SerialName("save_link") val saveLink: String? = null,
    @SerialName("card_number") val cardNumber: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class BindNFCCardRequest(
    @SerialName("card_uid") val cardUid: String,
    @SerialName("card_type") val cardType: String = "desfire_ev3",
    val label: String = "NFC Card",
)

@Serializable
data class NFCCard(
    val id: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("public_key_fingerprint") val publicKeyFingerprint: String? = null,
    @SerialName("card_uid") val cardUid: String? = null,
    @SerialName("card_type") val cardType: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class UnbindNFCCardResponse(
    val status: String,
    val id: String? = null,
    val message: String? = null,
)
