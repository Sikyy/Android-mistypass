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
import com.mistyislet.app.domain.model.AnalyticsSummary
import com.mistyislet.app.domain.model.Booking
import com.mistyislet.app.domain.model.Camera
import com.mistyislet.app.domain.model.LiveActivityRecord
import com.mistyislet.app.domain.model.OrgSettings
import com.mistyislet.app.domain.model.ReportExportRequest
import com.mistyislet.app.domain.model.ReportExportResponse
import com.mistyislet.app.domain.model.UserPresenceRecord
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
}
