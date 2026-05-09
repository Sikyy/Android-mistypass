package com.mistyislet.app.data.api

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
    suspend fun listEvents(@Path("placeId") placeId: String): List<AdminEvent>

    @GET("app/places/{placeId}/incidents")
    suspend fun listIncidents(@Path("placeId") placeId: String): List<AdminIncident>

    @GET("app/places/{placeId}/users")
    suspend fun listUsers(@Path("placeId") placeId: String): List<AdminUser>

    @GET("app/places/{placeId}/groups")
    suspend fun listGroups(@Path("placeId") placeId: String): List<AdminGroup>

    @GET("app/places/{placeId}/teams")
    suspend fun listTeams(@Path("placeId") placeId: String): List<AdminTeam>

    @GET("app/places/{placeId}/schedules")
    suspend fun listSchedules(@Path("placeId") placeId: String): List<AdminSchedule>

    @GET("app/places/{placeId}/zones")
    suspend fun listZones(@Path("placeId") placeId: String): List<AdminZone>

    @GET("app/alarms")
    suspend fun listAlarms(): List<Alarm>

    @GET("app/places/{placeId}/activity")
    suspend fun listLiveActivity(@Path("placeId") placeId: String): List<LiveActivityRecord>

    @GET("app/bookings")
    suspend fun listBookings(@Query("space_id") spaceId: String? = null): List<Booking>

    @GET("app/places/{placeId}/cards")
    suspend fun listCards(@Path("placeId") placeId: String): List<AdminCard>

    @GET("app/places/{placeId}/credentials")
    suspend fun listCredentials(@Path("placeId") placeId: String): List<AdminDigitalCredential>

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
    suspend fun listCameras(): List<Camera>

    @GET("app/orgs/{orgId}/settings")
    suspend fun getOrgSettings(@Path("orgId") orgId: String): OrgSettings

    @PATCH("app/orgs/{orgId}/settings")
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

    // Schedule CRUD
    @POST("app/places/{placeId}/schedules")
    suspend fun createSchedule(
        @Path("placeId") placeId: String,
        @Body schedule: AdminSchedule,
    ): AdminSchedule

    @PUT("app/places/{placeId}/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Path("placeId") placeId: String,
        @Path("scheduleId") scheduleId: String,
        @Body schedule: AdminSchedule,
    ): AdminSchedule

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

    @GET("app/alarms/schedules")
    suspend fun listAlarmSchedules(): List<AlarmSchedule>

    // Guest management
    @GET("app/places/{placeId}/guests")
    suspend fun listGuests(@Path("placeId") placeId: String): List<GuestVisit>

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
    @GET("app/bookings/spaces")
    suspend fun listBookingSpaces(): List<BookingSpace>

    @POST("app/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @PATCH("app/bookings/{bookingId}")
    suspend fun updateBookingStatus(
        @Path("bookingId") bookingId: String,
        @Body request: GuestCheckInRequest,
    ): Booking

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
    @GET("app/cameras/{cameraId}/stream")
    suspend fun getCameraStream(@Path("cameraId") cameraId: String): CameraVideoLink

    // Rename endpoints
    @PATCH("app/cameras/{cameraId}")
    suspend fun renameCamera(
        @Path("cameraId") cameraId: String,
        @Body request: RenameRequest,
    ): Camera

    @PATCH("app/places/{placeId}/doors/{doorId}/name")
    suspend fun renameDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
        @Body request: RenameRequest,
    )
}
