package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateVisitorPassRequest(
    @SerialName("building_id") val buildingId: String? = null,
    val visitor: String,
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class VisitorPass(
    val id: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("building_id") val buildingId: String? = null,
    val host: String? = null,
    val visitor: String,
    @SerialName("delivery_method") val deliveryMethod: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class BleTokenResponse(
    @SerialName("ble_token") val bleToken: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("expires_in") val expiresIn: Int = 300,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("user_id") val userId: String? = null,
)
