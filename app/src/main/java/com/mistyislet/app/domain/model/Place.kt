package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("is_lockdown") val isLockdown: Boolean = false,
    @SerialName("door_count") val doorCount: Int = 0,
    val timezone: String? = null,
    val capacity: Int? = null,
    @SerialName("current_occupancy") val currentOccupancy: Int? = null,
)
