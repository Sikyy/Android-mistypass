package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.Organization
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

    @GET("app/orgs")
    suspend fun listOrgs(): List<Organization>

    @GET("app/orgs/{orgId}/places")
    suspend fun listPlaces(@Path("orgId") orgId: String): List<Place>

    @GET("app/places/{placeId}/doors")
    suspend fun listPlaceDoors(@Path("placeId") placeId: String): ListResponse<AccessibleDoor>

    @GET("app/places/{placeId}/doors/search")
    suspend fun searchPlaceDoors(
        @Path("placeId") placeId: String,
        @Query("q") query: String,
    ): ListResponse<AccessibleDoor>

    @POST("app/places/{placeId}/doors/{doorId}/unlock")
    suspend fun unlockPlaceDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
        @Body request: UnlockRequest,
    ): UnlockResponse

    @PUT("app/places/{placeId}/doors/{doorId}/favorite")
    suspend fun favoriteDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    )

    @DELETE("app/places/{placeId}/doors/{doorId}/favorite")
    suspend fun unfavoriteDoor(
        @Path("placeId") placeId: String,
        @Path("doorId") doorId: String,
    )

    @POST("app/places/{placeId}/lockdown")
    suspend fun enableLockdown(@Path("placeId") placeId: String)

    @DELETE("app/places/{placeId}/lockdown")
    suspend fun disableLockdown(@Path("placeId") placeId: String)

    @GET("app/places/{placeId}/visitor-groups")
    suspend fun listVisitorGroups(@Path("placeId") placeId: String): List<VisitorGroup>

    @GET("app/places/{placeId}/visitor-groups/{groupId}/members")
    suspend fun listGroupMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    ): List<VisitorGroupMember>

    @POST("app/places/{placeId}/visitor-groups/{groupId}/cleanup-expired")
    suspend fun cleanupExpiredMembers(
        @Path("placeId") placeId: String,
        @Path("groupId") groupId: String,
    )
}
