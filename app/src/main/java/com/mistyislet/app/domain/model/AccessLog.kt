package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessLog(
    val id: String,
    @SerialName("door_id") val doorId: String? = null,
    @SerialName("door_name") val doorName: String? = null,
    @SerialName("lock_name") val lockName: String? = null,
    val type: String = "",
    @SerialName("event_type") val eventType: String = "",
    val result: String,
    val method: String? = null,
    @SerialName("credential_type") val credentialType: String? = null,
    val reason: String? = null,
    val actor: String? = null,
    val at: String? = null,
    val timestamp: String? = null,
) {
    val displayType: String get() = type.ifEmpty { eventType }
    val displayTime: String get() = at ?: timestamp ?: ""
    val displayName: String get() = doorName ?: lockName ?: doorId ?: "Unknown"
    val displayMethod: String? get() = method ?: credentialType
}
