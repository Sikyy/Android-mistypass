package com.mistyislet.app.data.repository

import com.mistyislet.app.core.geofence.GeofenceManager
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.data.dao.CachedDoor
import com.mistyislet.app.data.dao.DoorDao
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.CreateVisitorPassRequest
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import com.mistyislet.app.domain.model.VisitorPass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoorRepository @Inject constructor(
    private val accessApi: AccessApi,
    private val doorDao: DoorDao,
    private val geofenceManager: GeofenceManager,
) {
    fun getCachedDoors(): Flow<List<AccessibleDoor>> {
        return doorDao.getAll().map { cached ->
            cached.map { it.toDomain() }
        }
    }

    suspend fun refreshDoors(): ApiResult<List<AccessibleDoor>> {
        return safeApiCall {
            val response = accessApi.getMyDoors()
            val doors = response.items
            doorDao.deleteAll()
            doorDao.insertAll(doors.map { it.toCache() })
            geofenceManager.syncGeofences(doors)
            doors
        }
    }

    suspend fun unlock(lockId: String): ApiResult<UnlockResponse> {
        return safeApiCall {
            accessApi.unlock(UnlockRequest(lockId = lockId))
        }
    }

    suspend fun getVisitorPasses(): ApiResult<List<VisitorPass>> {
        return safeApiCall {
            accessApi.getVisitorPasses().items
        }
    }

    suspend fun createVisitorPass(request: CreateVisitorPassRequest): ApiResult<VisitorPass> {
        return safeApiCall {
            accessApi.createVisitorPass(request)
        }
    }
}

private fun CachedDoor.toDomain() = AccessibleDoor(
    id = id,
    name = name,
    buildingId = buildingId,
    areaId = areaId,
    status = status,
    gatewayStatus = gatewayStatus,
    groupName = groupName,
    canUnlock = canUnlock,
)

private fun AccessibleDoor.toCache() = CachedDoor(
    id = id,
    name = name,
    buildingId = buildingId,
    areaId = areaId,
    status = status,
    gatewayStatus = gatewayStatus,
    groupName = groupName,
    canUnlock = canUnlock,
)
