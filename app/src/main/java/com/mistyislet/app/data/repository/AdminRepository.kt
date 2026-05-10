package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AdminApi
import com.mistyislet.app.domain.model.AlarmCalendarEntry
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.domain.model.AdminDigitalCredential
import com.mistyislet.app.domain.model.AdminEvent
import com.mistyislet.app.domain.model.AdminGroup
import com.mistyislet.app.domain.model.AdminIncident
import com.mistyislet.app.domain.model.AdminSchedule
import com.mistyislet.app.domain.model.ScheduleTimeWindow
import com.mistyislet.app.domain.model.ScheduleWriteRequest
import com.mistyislet.app.domain.model.AdminTeam
import com.mistyislet.app.domain.model.AdminUser
import com.mistyislet.app.domain.model.AdminZone
import com.mistyislet.app.domain.model.Alarm
import com.mistyislet.app.domain.model.AlarmSchedule
import com.mistyislet.app.domain.model.AlarmStatusUpdateRequest
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.BookingSpace
import com.mistyislet.app.domain.model.AssignAccessRightRequest
import com.mistyislet.app.domain.model.AssignDoorRequest
import com.mistyislet.app.domain.model.AssignMemberRequest
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.CameraSnapshotResponse
import com.mistyislet.app.domain.model.CameraVideoLink
import com.mistyislet.app.domain.model.CreateBookingRequest
import com.mistyislet.app.domain.model.CreateGroupRequest
import com.mistyislet.app.domain.model.CreateGuestRequest
import com.mistyislet.app.domain.model.CreateTeamRequest
import com.mistyislet.app.domain.model.GuestCheckInRequest
import com.mistyislet.app.domain.model.FailedAttemptEvent
import com.mistyislet.app.domain.model.GroupDoor
import com.mistyislet.app.domain.model.GroupMember
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.domain.model.InviteUserRequest
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.OrgSettingsUpdateRequest
import com.mistyislet.app.domain.model.RenameRequest
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.domain.model.TeamAccessRight
import com.mistyislet.app.domain.model.TeamMember
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.domain.model.UserRoleUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val adminApi: AdminApi,
) {
    suspend fun getEvents(placeId: String): ApiResult<List<AdminEvent>> =
        safeApiCall { adminApi.listEvents(placeId).items }

    suspend fun getIncidents(placeId: String): ApiResult<List<AdminIncident>> =
        safeApiCall { adminApi.listIncidents(placeId).items }

    suspend fun getUsers(placeId: String): ApiResult<List<AdminUser>> =
        safeApiCall { adminApi.listUsers(placeId).items }

    suspend fun getGroups(placeId: String): ApiResult<List<AdminGroup>> =
        safeApiCall { adminApi.listGroups(placeId).items }

    suspend fun getTeams(placeId: String): ApiResult<List<AdminTeam>> =
        safeApiCall { adminApi.listTeams(placeId).items }

    suspend fun getSchedules(placeId: String): ApiResult<List<AdminSchedule>> =
        safeApiCall { adminApi.listSchedules(placeId).items }

    suspend fun getZones(placeId: String): ApiResult<List<AdminZone>> =
        safeApiCall { adminApi.listZones(placeId).items }

    suspend fun getAlarms(): ApiResult<List<Alarm>> =
        safeApiCall { adminApi.listAlarms().items }

    suspend fun getLiveActivity(placeId: String): ApiResult<List<LiveActivityRecord>> =
        safeApiCall { adminApi.listLiveActivity(placeId).items }

    suspend fun getBookings(): ApiResult<List<Booking>> =
        safeApiCall { adminApi.listBookings().items }

    suspend fun getCards(placeId: String): ApiResult<List<AdminCard>> =
        safeApiCall { adminApi.listCards(placeId).items }

    suspend fun getCredentials(placeId: String): ApiResult<List<AdminDigitalCredential>> =
        safeApiCall { adminApi.listCredentials(placeId).items }

    suspend fun getAnalyticsSummary(placeId: String, days: Int = 30): ApiResult<AnalyticsSummary> =
        safeApiCall { adminApi.getAnalyticsSummary(placeId, days) }

    suspend fun getUserPresence(placeId: String, days: Int = 30): ApiResult<List<UserPresenceRecord>> =
        safeApiCall { adminApi.getUserPresence(placeId, days) }

    suspend fun exportReport(placeId: String, request: ReportExportRequest): ApiResult<ReportExportResponse> =
        safeApiCall { adminApi.exportReport(placeId, request) }

    suspend fun getCameras(): ApiResult<List<Camera>> =
        safeApiCall { adminApi.listCameras().items }

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

    // User invite
    suspend fun inviteUser(placeId: String, email: String, role: String = "member"): ApiResult<AdminUser> =
        safeApiCall { adminApi.inviteUser(placeId, InviteUserRequest(email, role)) }

    // Group CRUD
    suspend fun createGroup(placeId: String, request: CreateGroupRequest): ApiResult<AdminGroup> =
        safeApiCall { adminApi.createGroup(placeId, request) }

    suspend fun deleteGroup(placeId: String, groupId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteGroup(placeId, groupId) }

    suspend fun updateGroup(placeId: String, groupId: String, name: String, description: String?): ApiResult<AdminGroup> =
        safeApiCall { adminApi.updateGroup(placeId, groupId, CreateGroupRequest(name, description)) }

    // Group members
    suspend fun getGroupMembers(placeId: String, groupId: String): ApiResult<List<GroupMember>> =
        safeApiCall { adminApi.listGroupMembers(placeId, groupId).items }

    suspend fun addGroupMember(placeId: String, groupId: String, userId: String): ApiResult<GroupMember> =
        safeApiCall { adminApi.addGroupMember(placeId, groupId, AssignMemberRequest(userId)) }

    suspend fun removeGroupMember(placeId: String, groupId: String, memberId: String): ApiResult<Unit> =
        safeApiCall { adminApi.removeGroupMember(placeId, groupId, memberId) }

    // Group doors
    suspend fun getGroupDoors(placeId: String, groupId: String): ApiResult<List<GroupDoor>> =
        safeApiCall { adminApi.listGroupDoors(placeId, groupId).items }

    suspend fun addGroupDoor(placeId: String, groupId: String, doorId: String): ApiResult<GroupDoor> =
        safeApiCall { adminApi.addGroupDoor(placeId, groupId, AssignDoorRequest(doorId)) }

    suspend fun removeGroupDoor(placeId: String, groupId: String, doorId: String): ApiResult<Unit> =
        safeApiCall { adminApi.removeGroupDoor(placeId, groupId, doorId) }

    // Team CRUD
    suspend fun createTeam(placeId: String, request: CreateTeamRequest): ApiResult<AdminTeam> =
        safeApiCall { adminApi.createTeam(placeId, request) }

    suspend fun deleteTeam(placeId: String, teamId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteTeam(placeId, teamId) }

    // Team members
    suspend fun getTeamMembers(placeId: String, teamId: String): ApiResult<List<TeamMember>> =
        safeApiCall { adminApi.listTeamMembers(placeId, teamId).items }

    suspend fun addTeamMember(placeId: String, teamId: String, userId: String): ApiResult<TeamMember> =
        safeApiCall { adminApi.addTeamMember(placeId, teamId, AssignMemberRequest(userId)) }

    suspend fun removeTeamMember(placeId: String, teamId: String, memberId: String): ApiResult<Unit> =
        safeApiCall { adminApi.removeTeamMember(placeId, teamId, memberId) }

    // Team access rights
    suspend fun getTeamAccessRights(placeId: String, teamId: String): ApiResult<List<TeamAccessRight>> =
        safeApiCall { adminApi.listTeamAccessRights(placeId, teamId).items }

    suspend fun addTeamAccessRight(placeId: String, teamId: String, doorId: String, scheduleId: String? = null): ApiResult<TeamAccessRight> =
        safeApiCall { adminApi.addTeamAccessRight(placeId, teamId, AssignAccessRightRequest(doorId, scheduleId)) }

    suspend fun removeTeamAccessRight(placeId: String, teamId: String, rightId: String): ApiResult<Unit> =
        safeApiCall { adminApi.removeTeamAccessRight(placeId, teamId, rightId) }

    // Schedule CRUD
    suspend fun createSchedule(placeId: String, schedule: AdminSchedule): ApiResult<Unit> =
        safeApiCall { adminApi.createSchedule(placeId, schedule.toWriteRequest()) }

    suspend fun updateSchedule(placeId: String, scheduleId: String, schedule: AdminSchedule): ApiResult<Unit> =
        safeApiCall { adminApi.updateSchedule(placeId, scheduleId, schedule.toWriteRequest()) }

    suspend fun deleteSchedule(placeId: String, scheduleId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteSchedule(placeId, scheduleId) }

    // Alarm actions
    suspend fun updateAlarmStatus(alarmId: String, status: String): ApiResult<Alarm> =
        safeApiCall { adminApi.updateAlarmStatus(alarmId, AlarmStatusUpdateRequest(status)) }

    suspend fun getAlarmSchedules(): ApiResult<List<AlarmSchedule>> =
        safeApiCall { adminApi.listAlarmSchedules().items }

    suspend fun getAlarmCalendar(from: String? = null, to: String? = null): ApiResult<List<AlarmCalendarEntry>> =
        safeApiCall { adminApi.listAlarmCalendar(from, to).items }

    // Guest management
    suspend fun getGuests(placeId: String): ApiResult<List<GuestVisit>> =
        safeApiCall { adminApi.listGuests(placeId).items }

    suspend fun createGuest(placeId: String, request: CreateGuestRequest): ApiResult<GuestVisit> =
        safeApiCall { adminApi.createGuest(placeId, request) }

    suspend fun updateGuestStatus(placeId: String, guestId: String, action: String): ApiResult<GuestVisit> =
        safeApiCall { adminApi.updateGuestStatus(placeId, guestId, GuestCheckInRequest(action)) }

    suspend fun deleteGuest(placeId: String, guestId: String): ApiResult<Unit> =
        safeApiCall { adminApi.deleteGuest(placeId, guestId) }

    // Booking spaces & actions
    suspend fun getBookingSpaces(): ApiResult<List<BookingSpace>> =
        safeApiCall { adminApi.listBookingSpaces().items }

    suspend fun createBooking(request: CreateBookingRequest): ApiResult<Booking> =
        safeApiCall { adminApi.createBooking(request) }

    suspend fun updateBookingStatus(bookingId: String, action: String): ApiResult<Booking> =
        safeApiCall {
            when (action) {
                "check_in" -> adminApi.checkInBooking(bookingId)
                "check_out" -> adminApi.checkOutBooking(bookingId)
                else -> adminApi.cancelBooking(bookingId)
            }
        }

    // Card actions
    suspend fun unbindCard(placeId: String, cardId: String): ApiResult<Unit> =
        safeApiCall { adminApi.unbindCard(placeId, cardId) }

    // Credential actions
    suspend fun revokeCredential(placeId: String, credentialId: String): ApiResult<Unit> =
        safeApiCall { adminApi.revokeCredential(placeId, credentialId) }

    // Camera streaming
    suspend fun getCameraStream(cameraId: String): ApiResult<CameraVideoLink> =
        safeApiCall { adminApi.getCameraStream(cameraId) }

    // Camera snapshot
    suspend fun takeCameraSnapshot(cameraId: String): ApiResult<CameraSnapshotResponse> =
        safeApiCall { adminApi.takeCameraSnapshot(cameraId) }

    // Analytics failed attempts
    suspend fun getFailedAttempts(placeId: String, days: Int = 30): ApiResult<List<FailedAttemptEvent>> =
        safeApiCall { adminApi.getFailedAttempts(placeId, days).items }

    // Rename
    suspend fun renameCamera(cameraId: String, name: String): ApiResult<Camera> =
        safeApiCall { adminApi.renameCamera(cameraId, RenameRequest(name)) }

    suspend fun renameGateway(gatewayId: String, name: String): ApiResult<Unit> =
        safeApiCall { adminApi.renameGateway(gatewayId, RenameRequest(name)) }

    suspend fun renameDoor(placeId: String, doorId: String, name: String): ApiResult<Unit> =
        safeApiCall { adminApi.renameDoor(placeId, doorId, RenameRequest(name)) }
}

private fun AdminSchedule.toWriteRequest(): ScheduleWriteRequest {
    val daySet = daysOfWeek.joinToString(",")
    val windows = if (startTime != null || endTime != null || daySet.isNotEmpty()) {
        listOf(ScheduleTimeWindow(startTime = startTime ?: "", endTime = endTime ?: "", dayOfWeekSet = daySet))
    } else {
        emptyList()
    }
    return ScheduleWriteRequest(name = name, description = description, timeWindows = windows)
}
