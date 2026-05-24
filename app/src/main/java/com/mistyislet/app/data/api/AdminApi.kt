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
import com.mistyislet.app.domain.model.AccessRight
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.BookingSpace
import com.mistyislet.app.domain.model.BookingSpaceStatus
import com.mistyislet.app.domain.model.AssignAccessRightRequest
import com.mistyislet.app.domain.model.AssignDoorRequest
import com.mistyislet.app.domain.model.AssignMemberRequest
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.CameraCloudToken
import com.mistyislet.app.domain.model.CameraRecording
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
import com.mistyislet.app.domain.model.Holiday
import com.mistyislet.app.domain.model.HolidayRegion
import com.mistyislet.app.domain.model.InviteUserRequest
import com.mistyislet.app.domain.model.IncidentOccurrencesResponse
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.OrgSettingsUpdateRequest
import com.mistyislet.app.domain.model.RenameRequest
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.RelatedEventsResponse
import com.mistyislet.app.domain.model.ScheduleWriteRequest
import com.mistyislet.app.domain.model.ShareAccessRequest
import com.mistyislet.app.domain.model.TeamAccessRight
import com.mistyislet.app.domain.model.TeamMember
import com.mistyislet.app.domain.model.UserLogin
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

    @GET(MobileApiRoutes.getAppPlacesPlaceIdEventsRetrofitPath)
    suspend fun listEvents(@Path("placeId") placeId: String): PaginatedResponse<AdminEvent>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdEventsEventIdRetrofitPath)
    suspend fun getEvent(
        @Path("placeId") placeId: String,
        @Path("eventId") eventId: String,
    ): AdminEvent

    @GET(MobileApiRoutes.getAppPlacesPlaceIdEventsEventIdRelatedRetrofitPath)
    suspend fun getRelatedEvents(
        @Path("placeId") placeId: String,
        @Path("eventId") eventId: String,
    ): RelatedEventsResponse

    @GET(MobileApiRoutes.getAppPlacesPlaceIdIncidentsRetrofitPath)
    suspend fun listIncidents(@Path("placeId") placeId: String): PaginatedResponse<AdminIncident>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdIncidentsIncidentIdRetrofitPath)
    suspend fun getIncident(
        @Path("placeId") placeId: String,
        @Path("incidentId") incidentId: String,
    ): AdminIncident

    @GET(MobileApiRoutes.getAppPlacesPlaceIdIncidentsIncidentIdOccurrencesRetrofitPath)
    suspend fun getIncidentOccurrences(
        @Path("placeId") placeId: String,
        @Path("incidentId") incidentId: String,
    ): IncidentOccurrencesResponse

    @GET("app/places/{placeId}/users")
    suspend fun listUsers(@Path("placeId") placeId: String): PaginatedResponse<AdminUser>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdUsersUserIdRetrofitPath)
    suspend fun getUser(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    ): AdminUser

    @GET(MobileApiRoutes.getAppPlacesPlaceIdUsersUserIdLoginsRetrofitPath)
    suspend fun listUserLogins(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    ): PaginatedResponse<UserLogin>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdUsersUserIdAccessRightsRetrofitPath)
    suspend fun listUserAccessRights(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    ): PaginatedResponse<AccessRight>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdUsersUserIdShareAccessRetrofitPath)
    suspend fun shareUserAccess(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
        @Body request: ShareAccessRequest,
    ): AccessRight

    @GET(MobileApiRoutes.getAppPlacesPlaceIdGroupsRetrofitPath)
    suspend fun listGroups(@Path("placeId") placeId: String): PaginatedResponse<AdminGroup>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdTeamsRetrofitPath)
    suspend fun listTeams(@Path("placeId") placeId: String): PaginatedResponse<AdminTeam>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdSchedulesRetrofitPath)
    suspend fun listSchedules(@Path("placeId") placeId: String): PaginatedResponse<AdminSchedule>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdZonesRetrofitPath)
    suspend fun listZones(@Path("placeId") placeId: String): PaginatedResponse<AdminZone>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdZonesZoneIdRetrofitPath)
    suspend fun getZone(
        @Path("placeId") placeId: String,
        @Path("zoneId") zoneId: String,
    ): AdminZone

    @GET(MobileApiRoutes.getAppPlacesPlaceIdHolidayRegionsRetrofitPath)
    suspend fun listHolidayRegions(@Path("placeId") placeId: String): PaginatedResponse<HolidayRegion>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdHolidayRegionsRegionIdHolidaysRetrofitPath)
    suspend fun listHolidays(
        @Path("placeId") placeId: String,
        @Path("regionId") regionId: String,
    ): PaginatedResponse<Holiday>

    @GET(MobileApiRoutes.getAppAlarmsRetrofitPath)
    suspend fun listAlarms(): PaginatedResponse<Alarm>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdActivityRetrofitPath)
    suspend fun listLiveActivity(@Path("placeId") placeId: String): PaginatedResponse<LiveActivityRecord>

    @GET(MobileApiRoutes.getAppBookingsRetrofitPath)
    suspend fun listBookings(@Query("space_id") spaceId: String? = null): PaginatedResponse<Booking>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdCardsRetrofitPath)
    suspend fun listCards(@Path("placeId") placeId: String): PaginatedResponse<AdminCard>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdCredentialsRetrofitPath)
    suspend fun listCredentials(@Path("placeId") placeId: String): PaginatedResponse<AdminDigitalCredential>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdAnalyticsSummaryRetrofitPath)
    suspend fun getAnalyticsSummary(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): AnalyticsSummary

    @GET(MobileApiRoutes.getAppPlacesPlaceIdAnalyticsPresenceRetrofitPath)
    suspend fun getUserPresence(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): List<UserPresenceRecord>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdReportsExportRetrofitPath)
    suspend fun exportReport(
        @Path("placeId") placeId: String,
        @Body request: ReportExportRequest,
    ): ReportExportResponse

    @GET(MobileApiRoutes.getAppCamerasRetrofitPath)
    suspend fun listCameras(): PaginatedResponse<Camera>

    @GET(MobileApiRoutes.getAppOrgsOrgIdSettingsRetrofitPath)
    suspend fun getOrgSettings(@Path("orgId") orgId: String): OrgSettings

    @PUT(MobileApiRoutes.putAppOrgsOrgIdSettingsRetrofitPath)
    suspend fun updateOrgSettings(
        @Path("orgId") orgId: String,
        @Body request: OrgSettingsUpdateRequest,
    ): OrgSettings

    // User management
    @PATCH(MobileApiRoutes.patchAppPlacesPlaceIdUsersUserIdRoleRetrofitPath)
    suspend fun updateUserRole(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
        @Body request: UserRoleUpdateRequest,
    ): AdminUser

    @POST(MobileApiRoutes.postAppPlacesPlaceIdUsersUserIdSignOutRetrofitPath)
    suspend fun forceSignOutUser(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    )

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdUsersUserIdRetrofitPath)
    suspend fun removeUser(
        @Path("placeId") placeId: String,
        @Path("userId") userId: String,
    )

    // User invite
    @POST(MobileApiRoutes.postAppPlacesPlaceIdUsersInviteRetrofitPath)
    suspend fun inviteUser(
        @Path("placeId") placeId: String,
        @Body request: InviteUserRequest,
    ): AdminUser

    // Group CRUD
    @POST(MobileApiRoutes.postAppPlacesPlaceIdGroupsRetrofitPath)
    suspend fun createGroup(
        @Path("placeId") placeId: String,
        @Body request: CreateGroupRequest,
    ): AdminGroup

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdGroupsGroupIdRetrofitPath)
    suspend fun deleteGroup(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    )

    @PATCH(MobileApiRoutes.patchAppPlacesPlaceIdGroupsGroupIdRetrofitPath)
    suspend fun updateGroup(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: CreateGroupRequest,
    ): AdminGroup

    // Group members
    @GET(MobileApiRoutes.getAppPlacesPlaceIdGroupsGroupIdMembersRetrofitPath)
    suspend fun listGroupMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): PaginatedResponse<GroupMember>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdGroupsGroupIdMembersRetrofitPath)
    suspend fun addGroupMember(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: AssignMemberRequest,
    ): GroupMember

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdGroupsGroupIdMembersMemberIdRetrofitPath)
    suspend fun removeGroupMember(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Path("memberId") memberId: String,
    )

    // Group doors
    @GET(MobileApiRoutes.getAppPlacesPlaceIdGroupsGroupIdDoorsRetrofitPath)
    suspend fun listGroupDoors(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): PaginatedResponse<GroupDoor>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdGroupsGroupIdDoorsRetrofitPath)
    suspend fun addGroupDoor(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Body request: AssignDoorRequest,
    ): GroupDoor

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdGroupsGroupIdDoorsDoorIdRetrofitPath)
    suspend fun removeGroupDoor(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
        @Path("doorId") doorId: String,
    )

    // Team CRUD
    @POST(MobileApiRoutes.postAppPlacesPlaceIdTeamsRetrofitPath)
    suspend fun createTeam(
        @Path("placeId") placeId: String,
        @Body request: CreateTeamRequest,
    ): AdminTeam

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdTeamsTeamIdRetrofitPath)
    suspend fun deleteTeam(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    )

    // Team members
    @GET(MobileApiRoutes.getAppPlacesPlaceIdTeamsTeamIdMembersRetrofitPath)
    suspend fun listTeamMembers(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    ): PaginatedResponse<TeamMember>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdTeamsTeamIdMembersRetrofitPath)
    suspend fun addTeamMember(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Body request: AssignMemberRequest,
    ): TeamMember

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdTeamsTeamIdMembersMemberIdRetrofitPath)
    suspend fun removeTeamMember(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Path("memberId") memberId: String,
    )

    // Team access rights
    @GET(MobileApiRoutes.getAppPlacesPlaceIdTeamsTeamIdAccessRightsRetrofitPath)
    suspend fun listTeamAccessRights(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
    ): PaginatedResponse<TeamAccessRight>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdTeamsTeamIdAccessRightsRetrofitPath)
    suspend fun addTeamAccessRight(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Body request: AssignAccessRightRequest,
    ): TeamAccessRight

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdTeamsTeamIdAccessRightsAccessRightIdRetrofitPath)
    suspend fun removeTeamAccessRight(
        @Path("placeId") placeId: String,
        @Path("teamId") teamId: String,
        @Path("accessRightId") rightId: String,
    )

    // Schedule CRUD
    @POST(MobileApiRoutes.postAppPlacesPlaceIdSchedulesRetrofitPath)
    suspend fun createSchedule(
        @Path("placeId") placeId: String,
        @Body request: ScheduleWriteRequest,
    ): Unit

    @PUT(MobileApiRoutes.putAppPlacesPlaceIdSchedulesScheduleIdRetrofitPath)
    suspend fun updateSchedule(
        @Path("placeId") placeId: String,
        @Path("scheduleId") scheduleId: String,
        @Body request: ScheduleWriteRequest,
    ): Unit

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdSchedulesScheduleIdRetrofitPath)
    suspend fun deleteSchedule(
        @Path("placeId") placeId: String,
        @Path("scheduleId") scheduleId: String,
    )

    // Alarm actions
    @PATCH(MobileApiRoutes.patchAppAlarmsAlarmIDStatusRetrofitPath)
    suspend fun updateAlarmStatus(
        @Path("alarmID") alarmId: String,
        @Body request: AlarmStatusUpdateRequest,
    ): Alarm

    @GET(MobileApiRoutes.getAppAlarmSchedulesRetrofitPath)
    suspend fun listAlarmSchedules(): PaginatedResponse<AlarmSchedule>

    @GET(MobileApiRoutes.getAppAlarmSchedulesCalendarRetrofitPath)
    suspend fun listAlarmCalendar(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("timezone") timezone: String? = java.util.TimeZone.getDefault().id,
    ): PaginatedResponse<AlarmCalendarEntry>

    // Guest management
    @GET(MobileApiRoutes.getAppPlacesPlaceIdGuestsRetrofitPath)
    suspend fun listGuests(@Path("placeId") placeId: String): PaginatedResponse<GuestVisit>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdGuestsRetrofitPath)
    suspend fun createGuest(
        @Path("placeId") placeId: String,
        @Body request: CreateGuestRequest,
    ): GuestVisit

    @PATCH(MobileApiRoutes.patchAppPlacesPlaceIdGuestsGuestIdRetrofitPath)
    suspend fun updateGuestStatus(
        @Path("placeId") placeId: String,
        @Path("guestId") guestId: String,
        @Body request: GuestCheckInRequest,
    ): GuestVisit

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdGuestsGuestIdRetrofitPath)
    suspend fun deleteGuest(
        @Path("placeId") placeId: String,
        @Path("guestId") guestId: String,
    )

    // Booking spaces & actions
    @GET(MobileApiRoutes.getAppBookableSpacesRetrofitPath)
    suspend fun listBookingSpaces(): PaginatedResponse<BookingSpace>

    @GET(MobileApiRoutes.getAppBookableSpacesSpaceIDStatusRetrofitPath)
    suspend fun getBookableSpaceStatus(
        @Path("spaceID") spaceId: String,
    ): BookingSpaceStatus

    @POST(MobileApiRoutes.postAppBookingsRetrofitPath)
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @POST(MobileApiRoutes.postAppBookingsBookingIDCancelRetrofitPath)
    suspend fun cancelBooking(@Path("bookingID") bookingId: String): Booking

    @POST(MobileApiRoutes.postAppBookingsBookingIDCheckInRetrofitPath)
    suspend fun checkInBooking(@Path("bookingID") bookingId: String): Booking

    @POST(MobileApiRoutes.postAppBookingsBookingIDCheckOutRetrofitPath)
    suspend fun checkOutBooking(@Path("bookingID") bookingId: String): Booking

    // Card actions
    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdCardsCardUidRetrofitPath)
    suspend fun unbindCard(
        @Path("placeId") placeId: String,
        @Path("cardUid") cardId: String,
    )

    // Credential actions
    @POST(MobileApiRoutes.postAppPlacesPlaceIdCredentialsCredentialIdRevokeRetrofitPath)
    suspend fun revokeCredential(
        @Path("placeId") placeId: String,
        @Path("credentialId") credentialId: String,
    )

    @POST(MobileApiRoutes.postAppPlacesPlaceIdCredentialsCredentialIdSuspendRetrofitPath)
    suspend fun suspendCredential(
        @Path("placeId") placeId: String,
        @Path("credentialId") credentialId: String,
    )

    @POST(MobileApiRoutes.postAppPlacesPlaceIdCredentialsCredentialIdActivateRetrofitPath)
    suspend fun activateCredential(
        @Path("placeId") placeId: String,
        @Path("credentialId") credentialId: String,
    )

    // Camera streaming
    @GET(MobileApiRoutes.getAppCamerasCameraIDVideoLinkRetrofitPath)
    suspend fun getCameraStream(@Path("cameraID") cameraId: String): CameraVideoLink

    @GET(MobileApiRoutes.getAppCamerasCameraIDCloudTokenRetrofitPath)
    suspend fun getCameraCloudToken(@Path("cameraID") cameraId: String): CameraCloudToken

    @GET(MobileApiRoutes.getAppCamerasCameraIDCloudRecordingsRetrofitPath)
    suspend fun listCameraRecordings(@Path("cameraID") cameraId: String): PaginatedResponse<CameraRecording>

    // Camera streaming & snapshot
    @POST(MobileApiRoutes.postAppCamerasCameraIDSnapshotRetrofitPath)
    suspend fun takeCameraSnapshot(@Path("cameraID") cameraId: String): CameraSnapshotResponse

    // Analytics failed attempts
    @GET(MobileApiRoutes.getAppPlacesPlaceIdAnalyticsFailedAttemptsRetrofitPath)
    suspend fun getFailedAttempts(
        @Path("placeId") placeId: String,
        @Query("days") days: Int = 30,
    ): PaginatedResponse<FailedAttemptEvent>

    // Rename endpoints
    @PATCH(MobileApiRoutes.patchAppCamerasCameraIdRetrofitPath)
    suspend fun renameCamera(
        @Path("cameraId") cameraId: String,
        @Body request: RenameRequest,
    ): Camera

    @PATCH(MobileApiRoutes.patchAppPlacesPlaceIdDoorsDoorIdRetrofitPath)
    suspend fun renameDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
        @Body request: RenameRequest,
    )

    @PATCH(MobileApiRoutes.patchAppGatewaysGatewayIdRetrofitPath)
    suspend fun renameGateway(
        @Path("gatewayId") gatewayId: String,
        @Body request: RenameRequest,
    )
}
