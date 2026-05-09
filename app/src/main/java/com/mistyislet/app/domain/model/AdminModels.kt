package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminEvent(
    val id: String,
    @SerialName("event_type") val eventType: String? = null,
    @SerialName("object_name") val objectName: String = "",
    val actor: String = "",
    val result: String = "",
    val timestamp: String = "",
    @SerialName("display_time") val displayTime: String = "",
)

@Serializable
data class AdminIncident(
    val id: String,
    val title: String = "",
    val severity: String = "",
    val status: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AdminUser(
    val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_active") val lastActive: String? = null,
)

@Serializable
data class AdminGroup(
    val id: String,
    val name: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("door_count") val doorCount: Int = 0,
)

@Serializable
data class AdminTeam(
    val id: String,
    val name: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
)

@Serializable
data class AdminSchedule(
    val id: String,
    val name: String = "",
    val days: List<String> = emptyList(),
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
)

@Serializable
data class AdminZone(
    val id: String,
    val name: String = "",
    @SerialName("door_count") val doorCount: Int = 0,
)

@Serializable
data class Alarm(
    val id: String,
    val name: String = "",
    val status: String = "",
    val severity: String = "",
    @SerialName("triggered_at") val triggeredAt: String? = null,
)

@Serializable
data class LiveActivityRecord(
    val id: String,
    @SerialName("user_name") val userName: String = "",
    val action: String = "",
    @SerialName("door_name") val doorName: String = "",
    val timestamp: String = "",
)

@Serializable
data class Booking(
    val id: String,
    @SerialName("space_name") val spaceName: String = "",
    @SerialName("booked_by") val bookedBy: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = "",
)

@Serializable
data class AdminCard(
    val id: String,
    val uid: String = "",
    @SerialName("card_type") val cardType: String = "",
    val status: String = "",
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("last_used") val lastUsed: String? = null,
)

@Serializable
data class AdminDigitalCredential(
    val id: String,
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("credential_type") val credentialType: String = "",
    val status: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class Camera(
    val id: String,
    val name: String = "",
    val vendor: String = "",
    val model: String? = null,
    val status: String = "",
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("door_name") val doorName: String? = null,
)

@Serializable
data class CameraVideoLink(
    @SerialName("video_url") val videoUrl: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class AnalyticsSummary(
    @SerialName("total_unlocks") val totalUnlocks: Int = 0,
    @SerialName("unique_users") val uniqueUsers: Int = 0,
    @SerialName("failed_attempts") val failedAttempts: Int = 0,
    @SerialName("avg_daily_unlocks") val avgDailyUnlocks: Double = 0.0,
    @SerialName("daily_trend") val dailyTrend: List<DailyTrendPoint> = emptyList(),
    @SerialName("unlocks_by_method") val unlocksByMethod: List<MethodCount> = emptyList(),
    @SerialName("top_doors") val topDoors: List<TopDoor> = emptyList(),
    val heatmap: List<HeatmapCell>? = null,
    @SerialName("weekly_users") val weeklyUsers: List<WeeklyUserPoint>? = null,
)

@Serializable
data class DailyTrendPoint(
    val id: String = "",
    val date: String,
    val unlocks: Int = 0,
    @SerialName("unique_users") val uniqueUsers: Int = 0,
    val failed: Int = 0,
)

@Serializable
data class MethodCount(
    val method: String,
    val count: Int,
)

@Serializable
data class TopDoor(
    val id: String,
    val name: String,
    val count: Int,
)

@Serializable
data class HeatmapCell(
    @SerialName("day_of_week") val dayOfWeek: Int,
    val hour: Int,
    val value: Int,
)

@Serializable
data class WeeklyUserPoint(
    val id: String = "",
    @SerialName("week_start") val weekStart: String,
    @SerialName("unique_users") val uniqueUsers: Int,
)

@Serializable
data class UserPresenceRecord(
    val id: String,
    @SerialName("user_name") val userName: String = "",
    val email: String = "",
    @SerialName("days_present") val daysPresent: Int = 0,
    @SerialName("total_unlocks") val totalUnlocks: Int = 0,
    @SerialName("first_unlock") val firstUnlock: String? = null,
    @SerialName("weekday_breakdown") val weekdayBreakdown: List<Int>? = null,
)

@Serializable
data class ReportExportRequest(
    val type: String,
    val from: String,
    val to: String,
    val format: String = "csv",
)

@Serializable
data class ReportExportResponse(
    val url: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    val format: String = "",
)

@Serializable
data class AccessRight(
    val id: String,
    @SerialName("team_name") val teamName: String = "",
    @SerialName("door_name") val doorName: String = "",
    @SerialName("schedule_name") val scheduleName: String? = null,
)

@Serializable
data class OrgSettings(
    val id: String = "",
    val name: String = "",
    val timezone: String? = null,
    val language: String? = null,
)
