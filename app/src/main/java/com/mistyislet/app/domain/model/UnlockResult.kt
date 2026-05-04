package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnlockRequest(
    @SerialName("lock_id") val lockId: String,
    @SerialName("ble_token") val bleToken: String? = null,
)

@Serializable
data class QRUnlockRequest(
    @SerialName("lock_id") val lockId: String,
    @SerialName("qr_token") val qrToken: String,
)

@Serializable
data class UnlockResponse(
    val decision: String,
    val reason: String,
    val status: String? = null,
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("lock_id") val lockId: String,
    @SerialName("lock_name") val lockName: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
)
