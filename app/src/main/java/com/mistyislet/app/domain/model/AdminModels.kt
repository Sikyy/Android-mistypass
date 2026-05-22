package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
)

@Serializable
data class AdminEvent(
    val id: String,
    @SerialName("object_type") val eventType: String? = null,
    @SerialName("object_name") val objectName: String = "",
    val actor: String = "",
    val action: String = "",
    val result: String = "",
    @SerialName("result_color") val resultColor: String = "",
    val timestamp: String = "",
    @SerialName("display_time") val displayTime: String = "",
)

@Serializable
data class AdminIncident(
    val id: String,
    @SerialName("type") val title: String = "",
    val severity: String = "",
    val status: String = "",
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AdminUser(
    val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "active",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AdminGroup(
    val id: String,
    val name: String = "",
    val description: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("door_count") val doorCount: Int = 0,
)

@Serializable
data class AdminTeam(
    val id: String,
    val name: String = "",
    val description: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
)

@Serializable
data class AdminSchedule(
    val id: String,
    val name: String = "",
    val description: String? = null,
    @SerialName("schedule_type") val type: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList(),
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("is_enabled") val enabled: Boolean = true,
)

@Serializable
data class AdminZone(
    val id: String,
    val name: String = "",
    val description: String = "",
    @SerialName("door_count") val doorCount: Int = 0,
)

@Serializable
data class Alarm(
    val id: String,
    val type: String = "",
    val status: String = "",
    val severity: String = "",
    val location: String = "",
    @SerialName("created_at") val triggeredAt: String? = null,
) {
    val isOpen: Boolean get() = status.lowercase() == "open"
}

@Serializable
data class LiveActivityRecord(
    @SerialName("event_id") val id: String,
    @SerialName("user_id") val userName: String = "",
    val action: String = "",
    @SerialName("last_door") val doorName: String = "",
    @SerialName("last_seen") val timestamp: String = "",
)

@Serializable
data class Booking(
    val id: String,
    @SerialName("space_id") val spaceId: String = "",
    @SerialName("user_name") val bookedBy: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = "",
    val title: String? = null,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
)

@Serializable
data class AdminCard(
    val id: String,
    @SerialName("card_uid") val uid: String = "",
    @SerialName("card_number") val cardNumber: String? = null,
    @SerialName("type") val cardType: String = "",
    val status: String = "",
    @SerialName("user_name") val assignedTo: String? = null,
    @SerialName("user_email") val assignedEmail: String? = null,
    @SerialName("last_used") val lastUsed: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class AdminDigitalCredential(
    val id: String,
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("device_model") val deviceModel: String? = null,
    val platform: String = "",
    @SerialName("type") val credentialType: String = "",
    val status: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("usage_count") val usageCount: Int = 0,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class Camera(
    val id: String,
    val name: String = "",
    val provider: String = "",
    val status: String = "",
    val host: String? = null,
    val port: Int = 0,
    @SerialName("door_id") val doorId: String? = null,
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
    @SerialName("tenant_id") val id: String = "",
    val name: String = "",
    @SerialName("primary_domain") val domain: String? = null,
    val timezone: String? = null,
    @SerialName("email_notifications") val sendEmails: Boolean = false,
    @SerialName("push_notifications") val pushNotifications: Boolean = false,
    @SerialName("weekly_reports") val weeklyReports: Boolean = false,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean = false,
    @SerialName("enforce_mfa") val enforceMfa: Boolean = false,
    @SerialName("session_timeout_minutes") val sessionTimeoutMinutes: Int? = null,
    @SerialName("webauthn_enabled") val webauthnEnabled: Boolean = false,
)

@Serializable
data class OrgSettingsUpdateRequest(
    val name: String? = null,
    @SerialName("primary_domain") val domain: String? = null,
    @SerialName("email_notifications") val sendEmails: Boolean? = null,
    @SerialName("push_notifications") val pushNotifications: Boolean? = null,
    @SerialName("weekly_reports") val weeklyReports: Boolean? = null,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean? = null,
    @SerialName("enforce_mfa") val enforceMfa: Boolean? = null,
    @SerialName("webauthn_enabled") val webauthnEnabled: Boolean? = null,
)

@Serializable
data class GuestVisit(
    val id: String,
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val purpose: String? = null,
    @SerialName("host_name") val hostName: String? = null,
    val status: String = "expected",
    @SerialName("id_document_type") val idDocumentType: String? = null,
    @SerialName("id_document_number") val idDocumentNumber: String? = null,
    @SerialName("expected_at") val expectedAt: String? = null,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("checked_out_at") val checkedOutAt: String? = null,
)

@Serializable
data class BookingSpace(
    val id: String,
    val name: String = "",
    @SerialName("space_type") val type: String = "",
    @SerialName("max_capacity") val capacity: Int = 0,
    @SerialName("current_occupancy") val currentOccupancy: Int = 0,
    val enabled: Boolean = true,
    @SerialName("requires_booking") val requiresBooking: Boolean = true,
)

@Serializable
data class BookingSpaceStatus(
    val id: String = "",
    @SerialName("space_id") val spaceId: String = "",
    val status: String = "",  // "available", "occupied", "upcoming"
    @SerialName("current_booking") val currentBooking: String? = null,
    @SerialName("next_available_at") val nextAvailableAt: String? = null,
)

@Serializable
data class AlarmSchedule(
    val id: String,
    val name: String = "",
    val enabled: Boolean = true,
    @SerialName("alarm_types") val alarmTypes: List<String> = emptyList(),
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList(),
    val timezone: String? = null,
)

@Serializable
data class ScheduleTimeWindow(
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("day_of_week_set") val dayOfWeekSet: String = "",
)

@Serializable
data class ScheduleWriteRequest(
    val name: String,
    val description: String? = null,
    @SerialName("time_windows") val timeWindows: List<ScheduleTimeWindow> = emptyList(),
)

@Serializable
data class UserRoleUpdateRequest(
    val role: String,
)

@Serializable
data class AlarmStatusUpdateRequest(
    val status: String,
)

@Serializable
data class GuestCheckInRequest(
    val action: String,
)

@Serializable
data class CreateGuestRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val company: String? = null,
    val purpose: String? = null,
    @SerialName("host_name") val hostName: String? = null,
    @SerialName("host_email") val hostEmail: String? = null,
    @SerialName("host_phone") val hostPhone: String? = null,
    @SerialName("id_document_type") val idDocumentType: String? = null,
    @SerialName("id_document_number") val idDocumentNumber: String? = null,
    @SerialName("expected_at") val expectedAt: String? = null,
    @SerialName("notify_host") val notifyHost: Boolean = true,
    @SerialName("door_ids") val doorIds: List<String> = emptyList(),
    @SerialName("access_ttl_hours") val accessTtlHours: Int = 24,
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class CreateTeamRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class CreateBookingRequest(
    @SerialName("space_id") val spaceId: String,
    val title: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
data class RenameRequest(
    val name: String,
)

@Serializable
data class GroupMember(
    val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "",
)

@Serializable
data class GroupDoor(
    val id: String,
    val name: String = "",
    val status: String = "",
)

@Serializable
data class TeamMember(
    val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "",
)

@Serializable
data class TeamAccessRight(
    val id: String,
    @SerialName("door_name") val doorName: String = "",
    @SerialName("schedule_name") val scheduleName: String? = null,
    @SerialName("access_type") val accessType: String = "",
)

@Serializable
data class InviteUserRequest(
    val email: String,
    val role: String = "member",
)

@Serializable
data class AssignMemberRequest(
    @SerialName("user_id") val userId: String,
)

@Serializable
data class AssignDoorRequest(
    @SerialName("door_id") val doorId: String,
)

@Serializable
data class AssignAccessRightRequest(
    @SerialName("door_id") val doorId: String,
    @SerialName("schedule_id") val scheduleId: String? = null,
)

@Serializable
data class FailedAttemptEvent(
    val id: String,
    @SerialName("user_name") val userName: String = "",
    @SerialName("door_name") val doorName: String = "",
    val method: String = "",
    val reason: String = "",
    val timestamp: String = "",
)

@Serializable
data class AlarmCalendarEntry(
    val id: String,
    val date: String = "",
    @SerialName("alarm_count") val alarmCount: Int = 0,
    val alarms: List<Alarm> = emptyList(),
)

@Serializable
data class CameraSnapshotResponse(
    @SerialName("image_url") val imageUrl: String,
    @SerialName("captured_at") val capturedAt: String? = null,
)

@Serializable
data class EventMedia(
    val id: String,
    @SerialName("event_id") val eventId: String = "",
    @SerialName("camera_name") val cameraName: String = "",
    @SerialName("snapshot_url") val snapshotUrl: String = "",
    val datetime: String = "",
)

@Serializable
data class DoorRestriction(
    val id: String,
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("radius_meters") val radiusMeters: Int? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = false,
)

@Serializable
data class DoorSchedule(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("schedule_type") val scheduleType: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList(),
    @SerialName("is_enabled") val isEnabled: Boolean? = null,
)
