package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.data.dao.AccessLogDao
import com.mistyislet.app.data.dao.CachedAccessLog
import com.mistyislet.app.domain.model.AccessLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessLogRepository @Inject constructor(
    private val accessApi: AccessApi,
    private val accessLogDao: AccessLogDao,
) {
    fun getCachedLogs(): Flow<List<AccessLog>> {
        return accessLogDao.getAll().map { cached ->
            cached.map { it.toDomain() }
        }
    }

    suspend fun refreshLogs(offset: Int = 0, limit: Int = 20): ApiResult<List<AccessLog>> {
        return safeApiCall {
            val response = accessApi.getAccessLogs(offset, limit)
            val logs = response.items
            if (offset == 0) {
                accessLogDao.deleteAll()
            }
            accessLogDao.insertAll(logs.map { it.toCache() })
            logs
        }
    }
}

private fun CachedAccessLog.toDomain() = AccessLog(
    id = id,
    doorName = doorName,
    eventType = eventType,
    result = result,
    method = method,
    credentialType = credentialType,
    reason = reason,
    actor = actor,
    timestamp = timestamp,
)

private fun AccessLog.toCache() = CachedAccessLog(
    id = id,
    doorName = doorName ?: lockName ?: doorId,
    eventType = displayType,
    result = result,
    method = method,
    credentialType = credentialType,
    reason = reason,
    actor = actor,
    timestamp = displayTime,
)
