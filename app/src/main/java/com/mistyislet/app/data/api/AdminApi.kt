package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.AlarmCalendarEntry
import com.mistyislet.app.domain.model.AdminCard
import com.mistyislet.app.domain.model.PaginatedResponse
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
import com.mistyislet.app.domain.model.FailedAttemptEvent
import com.mistyislet.app.domain.model.GroupDoor
import com.mistyislet.app.domain.model.GroupMember
import com.mistyislet.app.domain.model.GuestCheckInRequest
import com.mistyislet.app.domain.model.GuestVisit
import com.mistyislet.app.domain.model.InviteUserRequest
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.OrgSettingsUpdateRequest
import com.mistyislet.app.domain.model.RenameRequest
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ScheduleWriteRequest
import com.mistyislet.app.domain.model.TeamAccessRight
import com.mistyislet.app.domain.model.TeamMember
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.domain.model.UserPresenceRecord
import com.mistyislet.app.domain.model.UserRoleUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {

    @GET("app/places/{placeId}/events")
    suspend fun listEvents(@Path("placeId") placeId: String): PaginatedResponse<AdminEvent>

    @GET("app/places/{placeId}/incidents")
    suspend fun listIncidents(@Path("placeId") placeId: String): PaginatedResponse<AdminIncident>

    @GET("app/places/{placeId}/users")
    suspend fun listUsers(@Path("placeId") placeId: String): PaginatedResponse<AdminUser>

    @GET("app/places/{placeId}/groups")
    suspend fun listGroups(@Path("placeId") placeId: String): PaginatedResponse<AdminGroup>

    @GET("app/places/{placeId}/teams")
    suspend fun listTeams(@Path("placeId") placeId: String): PaginatedResponse<AdminTeam>

    @GET("app/places/{placeId}/schedules")
    suspend fun listSchedules(@Path("placeId") placeId: String): PaginatedResponse<AdminSchedule>

    @GET("app/places/{placeId}/zones")
    suspend fun listZones(@Path("placeId") placeId: String): PaginatedResponse<AdminZone>

    @GET("app/alarms")
    suspend fun listAlarms(): PaginatedResponse<Alarm>

    @GET("app/places/{placeId}/activity")
    suspend fun listLiveActivity(@Path("placeId") placeId: String): PaginatedResponse<LiveActivityRecord>

    @GET("app/bookings")
    suspend fun listBookings(@Query("space_id") spaceId: String? = null): PaginatedResponse<Booking>

    @GET("app/places/{placeId}/cards")
    suspend fun listCards(@Path("placeId") placeId: String): PaginatedResponse<AdminCard>

    @GET("app/places/{placeId}/credentials")
    suspend fun listCredentials(@Path("placeId") placeId: String): PaginatedResponse<AdminDigitalCredential>

    @GET("app/places/{placeId}/analytics/summary")
    suspend fun getAnalyticsSummary(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): AnalyticsSummary

    @GET("app/places/{placeId}/analytics/presence")
    suspend fun getUserPresence(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): List<UserPresenceRecord>

    @POST("app/places/{placeId}/reports/export")
    suspend fun exportReport(
        @Path("placeId") placeId: String,
        @Body request: ReportExportRequest,
    ): ReportExportResponse

    @GET("app/cameras")
    suspend fun listCameras(): PaginatedResponse<Camera>

    @GET("app/orgs/{orgId}/settings")
    suspend fun getOrgSettings(@Path("orgId") orgId: String): OrgSettings

    @PUT("app/orgs/{orgId}/settings")
    suspend fun updateOrgSettings(
        @Path("orgId") orgId: String,
        @Body request: OrgSettingsUpdateRequest,
    ): OrgSettings

    // User management
    @PATCH("app/places/{placeId}/users/{userId}/role")
    suspend fun updateUserRole(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
        @Body request: UserRoleUpdateRequest,
    ): AdminUser

    @POST("app/places/{placeId}/users/{userId}/sign-out")
    suspend fun forceSignOutUser(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    )

    @DELETE("app/places/{placeId}/users/{userId}")
    suspend fun removeUser(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    )

    // User invite
    @POST("app/places/{placeId}/users/invite")
    suspend fun inviteUser(
        @Path("placeId") placeId: String,
        @Body request: InviteUserRequest,
    ): AdminUser

    // Group CRUD
    @POST("app/places/{placeId}/groups")
    suspend fun createGroup(
        @Path("placeId") placeId: String,
        @Body request: CreateGroupRequest,
    ): AdminGroup

    @DELETE("app/places/{placeId}/groups/{groupId}")
    suspend fun deleteGroup(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    )

    @PATCH("app/places/{placeId}/groups/{groupId}")
    suspend fun updateGroup(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: CreateGroupRequest,
    ): AdminGroup

    // Group members
    @GET("app/places/{placeId}/groups/{groupId}/members")
    suspend fun listGroupMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): PaginatedResponse<GroupMember>

    @POST("app/places/{placeId}/groups/{groupId}/members")
    suspend fun addGroupMember(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: AssignMemberRequest,
    ): GroupMember

    @DELETE("app/places/{placeId}/groups/{groupId}/members/{memberId}")
    suspend fun removeGroupMember(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Path("memberId") memberId: String,
    )

    // Group doors
    @GET("app/places/{placeId}/groups/{groupId}/doors")
    suspend fun listGroupDoors(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): PaginatedResponse<GroupDoor>

    @POST("app/places/{placeId}/groups/{groupId}/doors")
    suspend fun addGroupDoor(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: AssignDoorRequest,
    ): GroupDoor

    @DELETE("app/places/{placeId}/groups/{groupId}/doors/{doorId}")
    suspend fun removeGroupDoor(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Path("doorId") doorId: String,
    )

    // Team CRUD
    @POST("app/places/{placeId}/teams")
    suspend fun createTeam(
        @Path("placeId") placeId: String,
        @Body request: CreateTeamRequest,
    ): AdminTeam

    @DELETE("app/places/{placeId}/teams/{teamId}")
    suspend fun deleteTeam(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    )

    // Team members
    @GET("app/places/{placeId}/teams/{teamId}/members")
    suspend fun listTeamMembers(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    ): PaginatedResponse<TeamMember>

    @POST("app/places/{placeId}/teams/{teamId}/members")
    suspend fun addTeamMember(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Body request: AssignMemberRequest,
    ): TeamMember

    @DELETE("app/places/{placeId}/teams/{teamId}/members/{memberId}")
    suspend fun removeTeamMember(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Path("memberId") memberId: String,
    )

    // Team access rights
    @GET("app/places/{placeId}/teams/{teamId}/access-rights")
    suspend fun listTeamAccessRights(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    ): PaginatedResponse<TeamAccessRight>

    @POST("app/places/{placeId}/teams/{teamId}/access-rights")
    suspend fun addTeamAccessRight(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Body request: AssignAccessRightRequest,
    ): TeamAccessRight

    @DELETE("app/places/{placeId}/teams/{teamId}/access-rights/{rightId}")
    suspend fun removeTeamAccessRight(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Path("rightId") rightId: String,
    )

    // Schedule CRUD
    @POST("app/places/{placeId}/schedules")
    suspend fun createSchedule(
        @Path("placeId") placeId: String,
        @Body request: ScheduleWriteRequest,
    ): Unit

    @PUT("app/places/{placeId}/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Path("placeId") placeId: String,
        @Path("scheduleId") scheduleId: String,
        @Body request: ScheduleWriteRequest,
    ): Unit

    @DELETE("app/places/{placeId}/schedules/{scheduleId}")
    suspend fun deleteSchedule(
        @Path("placeId") placeId: String,
        @Path("scheduleId") scheduleId: String,
    )

    // Alarm actions
    @PATCH("app/alarms/{alarmId}/status")
    suspend fun updateAlarmStatus(
        @Path("alarmId") alarmId: String,
        @Body request: AlarmStatusUpdateRequest,
    ): Alarm

    @GET("app/alarm-schedules")
    suspend fun listAlarmSchedules(): PaginatedResponse<AlarmSchedule>

    @GET("app/alarm-schedules/calendar")
    suspend fun listAlarmCalendar(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): PaginatedResponse<AlarmCalendarEntry>

    // Guest management
    @GET("app/places/{placeId}/guests")
    suspend fun listGuests(@Path("placeId") placeId: String): PaginatedResponse<GuestVisit>

    @POST("app/places/{placeId}/guests")
    suspend fun createGuest(
        @Path("placeId") placeId: String,
        @Body request: CreateGuestRequest,
    ): GuestVisit

    @PATCH("app/places/{placeId}/guests/{guestId}")
    suspend fun updateGuestStatus(
        @Path("placeId") placeId: String,
        @Path("guestId") guestId: String,
        @Body request: GuestCheckInRequest,
    ): GuestVisit

    @DELETE("app/places/{placeId}/guests/{guestId}")
    suspend fun deleteGuest(
        @Path("placeId") placeId: String,
        @Path("guestId") guestId: String,
    )

    // Booking spaces & actions
    @GET("app/bookable-spaces")
    suspend fun listBookingSpaces(): PaginatedResponse<BookingSpace>

    @POST("app/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @POST("app/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Booking

    @POST("app/bookings/{bookingId}/check-in")
    suspend fun checkInBooking(@Path("bookingId") bookingId: String): Booking

    @POST("app/bookings/{bookingId}/check-out")
    suspend fun checkOutBooking(@Path("bookingId") bookingId: String): Booking

    // Card actions
    @DELETE("app/places/{placeId}/cards/{cardId}")
    suspend fun unbindCard(
        @Path("placeId") placeId: String,
        @Path("cardId") cardId: String,
    )

    // Credential actions
    @POST("app/places/{placeId}/credentials/{credentialId}/revoke")
    suspend fun revokeCredential(
        @Path("placeId") placeId: String,
        @Path("credentialId") credentialId: String,
    )

    // Camera streaming
    @GET("app/cameras/{cameraId}/video-link")
    suspend fun getCameraStream(@Path("cameraId") cameraId: String): CameraVideoLink

    // Camera streaming & snapshot
    @POST("app/cameras/{cameraId}/snapshot")
    suspend fun takeCameraSnapshot(@Path("cameraId") cameraId: String): CameraSnapshotResponse

    // Analytics failed attempts
    @GET("app/places/{placeId}/analytics/failed-attempts")
    suspend fun getFailedAttempts(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): PaginatedResponse<FailedAttemptEvent>

    // Rename endpoints
    @PATCH("app/cameras/{cameraId}")
    suspend fun renameCamera(
        @Path("cameraId") cameraId: String,
        @Body request: RenameRequest,
    ): Camera

    @PATCH("app/places/{placeId}/doors/{doorId}")
    suspend fun renameDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
        @Body request: RenameRequest,
    )

    @PATCH("app/gateways/{gatewayId}")
    suspend fun renameGateway(
        @Path("gatewayId") gatewayId: String,
        @Body request: RenameRequest,
    )
}
