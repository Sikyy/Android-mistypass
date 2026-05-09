package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessibleDoor(
    val id: String,
    val name: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("area_id") val areaId: String? = null,
    val status: String,
    @SerialName("gateway_status") val gatewayStatus: String,
    @SerialName("gateway_id") val gatewayId: String? = null,
    @SerialName("gateway_name") val gatewayName: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("can_unlock") val canUnlock: Boolean,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("last_unlock_at") val lastUnlockAt: String? = null,
    val kind: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

enum class DoorDisplayStatus {
    ONLINE_UNLOCKABLE,
    ONLINE_LOCKED_DOWN,
    OFFLINE,
    DISCONNECTED,
}

fun AccessibleDoor.displayStatus(): DoorDisplayStatus = when {
    status == "locked_down" -> DoorDisplayStatus.ONLINE_LOCKED_DOWN
    gatewayStatus == "online" && canUnlock -> DoorDisplayStatus.ONLINE_UNLOCKABLE
    gatewayStatus == "offline" -> DoorDisplayStatus.OFFLINE
    else -> DoorDisplayStatus.DISCONNECTED
}
