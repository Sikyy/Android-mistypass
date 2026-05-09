package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.PlaceApi
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.Organization
import com.mistyislet.app.domain.model.Place
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
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
}
