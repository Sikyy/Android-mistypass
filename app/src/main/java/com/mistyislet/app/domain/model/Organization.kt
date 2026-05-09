package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val domain: String? = null,
    val logo: String? = null,
    val role: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)
