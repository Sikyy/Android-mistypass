package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminEvent(
    val id: String,
    @SerialName("event_type") val eventType: String? = null,
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
    val title: String = "",
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
    val type: String? = null,
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
    val type: String = "",
    @SerialName("type_label") val typeLabel: String = "",
    val status: String = "",
    val severity: String = "",
    val location: String = "",
    @SerialName("time_ago") val timeAgo: String = "",
    @SerialName("triggered_at") val triggeredAt: String? = null,
) {
    val isOpen: Boolean get() = status.lowercase() == "open"
}

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
    @SerialName("card_number") val cardNumber: String? = null,
    @SerialName("card_type") val cardType: String = "",
    val status: String = "",
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("assigned_email") val assignedEmail: String? = null,
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
    @SerialName("credential_type") val credentialType: String = "",
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
    val domain: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    @SerialName("send_emails") val sendEmails: Boolean = false,
    @SerialName("email_access_assignment") val emailAccessAssignment: Boolean = false,
    @SerialName("email_credential_assignment") val emailCredentialAssignment: Boolean = false,
    @SerialName("email_incident_alerts") val emailIncidentAlerts: Boolean = false,
    @SerialName("email_reports") val emailReports: Boolean = false,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean = false,
    @SerialName("whatsapp_access_assignment") val whatsappAccessAssignment: Boolean = false,
    @SerialName("whatsapp_credential_assignment") val whatsappCredentialAssignment: Boolean = false,
    @SerialName("whatsapp_incident_alerts") val whatsappIncidentAlerts: Boolean = false,
    @SerialName("session_timeout_minutes") val sessionTimeoutMinutes: Int? = null,
    @SerialName("webauthn_enabled") val webauthnEnabled: Boolean = false,
)

@Serializable
data class OrgSettingsUpdateRequest(
    val name: String? = null,
    val domain: String? = null,
    @SerialName("send_emails") val sendEmails: Boolean? = null,
    @SerialName("email_access_assignment") val emailAccessAssignment: Boolean? = null,
    @SerialName("email_credential_assignment") val emailCredentialAssignment: Boolean? = null,
    @SerialName("email_incident_alerts") val emailIncidentAlerts: Boolean? = null,
    @SerialName("email_reports") val emailReports: Boolean? = null,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean? = null,
    @SerialName("whatsapp_access_assignment") val whatsappAccessAssignment: Boolean? = null,
    @SerialName("whatsapp_credential_assignment") val whatsappCredentialAssignment: Boolean? = null,
    @SerialName("whatsapp_incident_alerts") val whatsappIncidentAlerts: Boolean? = null,
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
    val type: String = "",
    val capacity: Int = 0,
    @SerialName("current_occupancy") val currentOccupancy: Int = 0,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("active_bookings") val activeBookings: Int = 0,
    @SerialName("next_available") val nextAvailable: String? = null,
)

@Serializable
data class AlarmSchedule(
    val id: String,
    val name: String = "",
    val enabled: Boolean = true,
    @SerialName("alarm_type") val alarmType: String = "",
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val days: List<String> = emptyList(),
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
    @SerialName("id_document_type") val idDocumentType: String? = null,
    @SerialName("id_document_number") val idDocumentNumber: String? = null,
    @SerialName("expected_at") val expectedAt: String? = null,
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
