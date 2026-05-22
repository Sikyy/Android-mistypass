package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateVisitorPassRequest(
    @SerialName("building_id") val buildingId: String? = null,
    val visitor: String,
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("ttl_hours") val ttlHours: Double? = null,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
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
data class VisitorGroup(
    val id: String,
    val name: String,
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("auto_remove_expired") val autoRemoveExpired: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class VisitorGroupMember(
    val id: String,
    @SerialName("visitor_id") val visitorId: String? = null,
    @SerialName("visitor_name") val visitorName: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
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

@Serializable
data class QRTokenRequest(
    @SerialName("door_id") val doorId: String? = null,
)

@Serializable
data class QRTokenResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class PinCodeResponse(
    val pin: String,
    @SerialName("valid_until") val validUntil: String,
    @SerialName("period_secs") val periodSecs: Int = 30,
)
