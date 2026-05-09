package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AdminApi
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.domain.model.AdminDigitalCredential
import com.mistyislet.app.domain.model.AdminEvent
import com.mistyislet.app.domain.model.AdminGroup
import com.mistyislet.app.domain.model.AdminIncident
import com.mistyislet.app.domain.model.AdminSchedule
import com.mistyislet.app.domain.model.AdminTeam
import com.mistyislet.app.domain.model.AdminUser
import com.mistyislet.app.domain.model.AdminZone
import com.mistyislet.app.domain.model.Alarm
import com.mistyislet.app.domain.model.AlarmSchedule
import com.mistyislet.app.domain.model.AlarmStatusUpdateRequest
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.BookingSpace
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.CameraVideoLink
import com.mistyislet.app.domain.model.CreateBookingRequest
import com.mistyislet.app.domain.model.CreateGroupRequest
import com.mistyislet.app.domain.model.CreateGuestRequest
import com.mistyislet.app.domain.model.CreateTeamRequest
import com.mistyislet.app.domain.model.GuestCheckInRequest
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.OrgSettingsUpdateRequest
import com.mistyislet.app.domain.model.RenameRequest
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.domain.model.UserRoleUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val adminApi: AdminApi,
) {
    suspend fun getEvents(placeId: String): ApiResult<List<AdminEvent>> =
        safeApiCall { adminApi.listEvents(placeId) }

    suspend fun getIncidents(placeId: String): ApiResult<List<AdminIncident>> =
        safeApiCall { adminApi.listIncidents(placeId) }

    suspend fun getUsers(placeId: String): ApiResult<List<AdminUser>> =
        safeApiCall { adminApi.listUsers(placeId) }

    suspend fun getGroups(placeId: String): ApiResult<List<AdminGroup>> =
        safeApiCall { adminApi.listGroups(placeId) }

    suspend fun getTeams(placeId: String): ApiResult<List<AdminTeam>> =
        safeApiCall { adminApi.listTeams(placeId) }

    suspend fun getSchedules(placeId: String): ApiResult<List<AdminSchedule>> =
        safeApiCall { adminApi.listSchedules(placeId) }

    suspend fun getZones(placeId: String): ApiResult<List<AdminZone>> =
        safeApiCall { adminApi.listZones(placeId) }

    suspend fun getAlarms(): ApiResult<List<Alarm>> =
        safeApiCall { adminApi.listAlarms() }

    suspend fun getLiveActivity(placeId: String): ApiResult<List<LiveActivityRecord>> =
        safeApiCall { adminApi.listLiveActivity(placeId) }

    suspend fun getBookings(): ApiResult<List<Booking>> =
        safeApiCall { adminApi.listBookings() }

    suspend fun getCards(placeId: String): ApiResult<List<AdminCard>> =
        safeApiCall { adminApi.listCards(placeId) }

    suspend fun getCredentials(placeId: String): ApiResult<List<AdminDigitalCredential>> =
        safeApiCall { adminApi.listCredentials(placeId) }

    suspend fun getAnalyticsSummary(placeId: String, days: Int = 30): ApiResult<AnalyticsSummary> =
        safeApiCall { adminApi.getAnalyticsSummary(placeId, days) }

    suspend fun getUserPresence(placeId: String, days: Int = 30): ApiResult<List<UserPresenceRecord>> =
        safeApiCall { adminApi.getUserPresence(placeId, days) }

    suspend fun exportReport(placeId: String, request: ReportExportRequest): ApiResult<ReportExportResponse> =
        safeApiCall { adminApi.exportReport(placeId, request) }

    suspend fun getCameras(): ApiResult<List<Camera>> =
        safeApiCall { adminApi.listCameras() }

    suspend fun getOrgSettings(orgId: String): ApiResult<OrgSettings> =
        safeApiCall { adminApi.getOrgSettings(orgId) }

    suspend fun updateOrgSettings(orgId: String, request: OrgSettingsUpdateRequest): ApiResult<OrgSettings> =
        safeApiCall { adminApi.updateOrgSettings(orgId, request) }

    // User management
    suspend fun updateUserRole(placeId: String, userId: String, role: String): ApiResult<AdminUser> =
        safeApiCall { adminApi.updateUserRole(placeId, userId, UserRoleUpdateRequest(role)) }

    suspend fun forceSignOutUser(placeId: String, userId: String): ApiResult<Unit> =
        safeApiCall { adminApi.forceSignOutUser(placeId, userId) }

    suspend fun removeUser(placeId: String, userId: String): ApiResult<Unit> =
        safeApiCall { adminApi.removeUser(placeId, userId) }

    // Group CRUD
    suspend fun createGroup(placeId: String, request: CreateGroupRequest): ApiResult<AdminGroup> =
        safeApiCall { adminApi.createGroup(placeId, request) }

    suspend fun deleteGroup(placeId: String, groupId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteGroup(placeId, groupId) }

    // Team CRUD
    suspend fun createTeam(placeId: String, request: CreateTeamRequest): ApiResult<AdminTeam> =
        safeApiCall { adminApi.createTeam(placeId, request) }

    suspend fun deleteTeam(placeId: String, teamId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteTeam(placeId, teamId) }

    // Schedule CRUD
    suspend fun createSchedule(placeId: String, schedule: AdminSchedule): ApiResult<AdminSchedule> =
        safeApiCall { adminApi.createSchedule(placeId, schedule) }

    suspend fun updateSchedule(placeId: String, scheduleId: String, schedule: AdminSchedule): ApiResult<AdminSchedule> =
        safeApiCall { adminApi.updateSchedule(placeId, scheduleId, schedule) }

    suspend fun deleteSchedule(placeId: String, scheduleId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteSchedule(placeId, scheduleId) }

    // Alarm actions
    suspend fun updateAlarmStatus(alarmId: String, status: String): ApiResult<Alarm> =
        safeApiCall { adminApi.updateAlarmStatus(alarmId, AlarmStatusUpdateRequest(status)) }

    suspend fun getAlarmSchedules(): ApiResult<List<AlarmSchedule>> =
        safeApiCall { adminApi.listAlarmSchedules() }

    // Guest management
    suspend fun getGuests(placeId: String): ApiResult<List<GuestVisit>> =
        safeApiCall { adminApi.listGuests(placeId) }

    suspend fun createGuest(placeId: String, request: CreateGuestRequest): ApiResult<GuestVisit> =
        safeApiCall { adminApi.createGuest(placeId, request) }

    suspend fun updateGuestStatus(placeId: String, guestId: String, action: String): ApiResult<GuestVisit> =
        safeApiCall { adminApi.updateGuestStatus(placeId, guestId, GuestCheckInRequest(action)) }

    suspend fun deleteGuest(placeId: String, guestId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteGuest(placeId, guestId) }

    // Booking spaces & actions
    suspend fun getBookingSpaces(): ApiResult<List<BookingSpace>> =
        safeApiCall { adminApi.listBookingSpaces() }

    suspend fun createBooking(request: CreateBookingRequest): ApiResult<Booking> =
        safeApiCall { adminApi.createBooking(request) }

    suspend fun updateBookingStatus(bookingId: String, action: String): ApiResult<Booking> =
        safeApiCall { adminApi.updateBookingStatus(bookingId, GuestCheckInRequest(action)) }

    // Card actions
    suspend fun unbindCard(placeId: String, cardId: String): ApiResult<Unit> =
        safeApiCall { adminApi.unbindCard(placeId, cardId) }

    // Credential actions
    suspend fun revokeCredential(placeId: String, credentialId: String): ApiResult<Unit> =
        safeApiCall { adminApi.revokeCredential(placeId, credentialId) }

    // Camera streaming
    suspend fun getCameraStream(cameraId: String): ApiResult<CameraVideoLink> =
        safeApiCall { adminApi.getCameraStream(cameraId) }

    // Rename
    suspend fun renameCamera(cameraId: String, name: String): ApiResult<Camera> =
        safeApiCall { adminApi.renameCamera(cameraId, RenameRequest(name)) }
}
