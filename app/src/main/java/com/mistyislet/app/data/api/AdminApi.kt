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
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.domain.model.UserPresenceRecord
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}
