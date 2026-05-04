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
