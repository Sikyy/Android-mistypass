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
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("can_unlock") val canUnlock: Boolean,
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
