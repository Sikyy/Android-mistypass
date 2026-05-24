package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.DoorRestriction
import com.mistyislet.app.domain.model.DoorSchedule
import com.mistyislet.app.domain.model.EventMedia
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.Organization
import com.mistyislet.app.domain.model.PaginatedResponse
import com.mistyislet.app.domain.model.Place
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import com.mistyislet.app.domain.model.VisitorGroup
import com.mistyislet.app.domain.model.VisitorGroupMember
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {

    @GET(MobileApiRoutes.getAppOrgsRetrofitPath)
    suspend fun listOrgs(): List<Organization>

    @GET(MobileApiRoutes.getAppOrgsOrgIdPlacesRetrofitPath)
    suspend fun listPlaces(@Path("orgId") orgId: String): List<Place>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdDoorsRetrofitPath)
    suspend fun listPlaceDoors(@Path("placeId") placeId: String): ListResponse<AccessibleDoor>

    @GET(MobileApiRoutes.getAppPlacesPlaceIdDoorsSearchRetrofitPath)
    suspend fun searchPlaceDoors(
        @Path("placeId") placeId: String,
        @Query("q") query: String,
    ): ListResponse<AccessibleDoor>

    @POST(MobileApiRoutes.postAppPlacesPlaceIdDoorsDoorIdUnlockRetrofitPath)
    suspend fun unlockPlaceDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
        @Body request: UnlockRequest,
    ): UnlockResponse

    @PUT(MobileApiRoutes.putAppPlacesPlaceIdDoorsDoorIdFavoriteRetrofitPath)
    suspend fun favoriteDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    )

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdDoorsDoorIdFavoriteRetrofitPath)
    suspend fun unfavoriteDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    )

    @POST(MobileApiRoutes.postAppPlacesPlaceIdLockdownRetrofitPath)
    suspend fun enableLockdown(@Path("placeId") placeId: String)

    @DELETE(MobileApiRoutes.deleteAppPlacesPlaceIdLockdownRetrofitPath)
    suspend fun disableLockdown(@Path("placeId") placeId: String)

    @GET("app/places/{placeId}/visitor-groups")
    suspend fun listVisitorGroups(@Path("placeId") placeId: String): List<VisitorGroup>

    @GET("app/places/{placeId}/visitor-groups/{groupId}/members")
    suspend fun listGroupMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): List<VisitorGroupMember>

    @GET("app/places/{placeId}/events/{eventId}/media")
    suspend fun getEventMedia(
        @Path("placeId") placeId: String,
        @Path("eventId") eventId: String,
    ): List<EventMedia>

    @GET("app/places/{placeId}/doors/{doorId}/restrictions")
    suspend fun getDoorRestrictions(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    ): PaginatedResponse<DoorRestriction>

    @GET("app/places/{placeId}/doors/{doorId}/schedules")
    suspend fun getDoorSchedules(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    ): PaginatedResponse<DoorSchedule>

    @POST("app/places/{placeId}/visitor-groups/{groupId}/cleanup-expired")
    suspend fun cleanupExpiredMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    )
}
