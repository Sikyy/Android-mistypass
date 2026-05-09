package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserLogin(
    val id: String,
    @SerialName("device_name") val deviceName: String,
    val platform: String,
    @SerialName("last_active") val lastActive: String,
    @SerialName("is_current") val isCurrent: Boolean = false,
)

@Serializable
data class UserLoginListResponse(
    val items: List<UserLogin>,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
