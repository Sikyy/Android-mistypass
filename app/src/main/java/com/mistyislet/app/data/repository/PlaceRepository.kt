package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.PlaceApi
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.Organization
import com.mistyislet.app.domain.model.Place
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import com.mistyislet.app.domain.model.VisitorGroup
import com.mistyislet.app.domain.model.VisitorGroupMember
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepository @Inject constructor(
    private val placeApi: PlaceApi,
) {
    suspend fun listOrgs(): ApiResult<List<Organization>> = safeApiCall {
        placeApi.listOrgs()
    }

    suspend fun listPlaces(orgId: String): ApiResult<List<Place>> = safeApiCall {
        placeApi.listPlaces(orgId)
    }

    suspend fun listPlaceDoors(placeId: String): ApiResult<List<AccessibleDoor>> = safeApiCall {
        placeApi.listPlaceDoors(placeId).items
    }

    suspend fun searchPlaceDoors(placeId: String, query: String): ApiResult<List<AccessibleDoor>> = safeApiCall {
        placeApi.searchPlaceDoors(placeId, query).items
    }

    suspend fun unlockPlaceDoor(placeId: String, doorId: String): ApiResult<UnlockResponse> = safeApiCall {
        placeApi.unlockPlaceDoor(placeId, doorId, UnlockRequest(lockId = doorId))
    }

    suspend fun favoriteDoor(placeId: String, doorId: String): ApiResult<Unit> = safeApiCall {
        placeApi.favoriteDoor(placeId, doorId)
    }

    suspend fun unfavoriteDoor(placeId: String, doorId: String): ApiResult<Unit> = safeApiCall {
        placeApi.unfavoriteDoor(placeId, doorId)
    }

    suspend fun enableLockdown(placeId: String): ApiResult<Unit> = safeApiCall {
        placeApi.enableLockdown(placeId)
    }

    suspend fun disableLockdown(placeId: String): ApiResult<Unit> = safeApiCall {
        placeApi.disableLockdown(placeId)
    }

    suspend fun getEventMedia(placeId: String, eventId: String) = safeApiCall {
        placeApi.getEventMedia(placeId, eventId)
    }

    suspend fun getDoorRestrictions(placeId: String, doorId: String) = safeApiCall {
        placeApi.getDoorRestrictions(placeId, doorId).items
    }

    suspend fun getDoorSchedules(placeId: String, doorId: String) = safeApiCall {
        placeApi.getDoorSchedules(placeId, doorId).items
    }

    suspend fun listVisitorGroups(placeId: String): ApiResult<List<VisitorGroup>> = safeApiCall {
        placeApi.listVisitorGroups(placeId)
    }

    suspend fun listGroupMembers(placeId: String, groupId: String): ApiResult<List<VisitorGroupMember>> = safeApiCall {
        placeApi.listGroupMembers(placeId, groupId)
    }

    suspend fun cleanupExpiredMembers(placeId: String, groupId: String): ApiResult<Unit> = safeApiCall {
        placeApi.cleanupExpiredMembers(placeId, groupId)
    }
}
