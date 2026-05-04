package com.mistyislet.app.domain.usecase

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.DoorRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMyDoorsUseCase @Inject constructor(
    private val doorRepository: DoorRepository,
) {
    fun getCached(): Flow<List<AccessibleDoor>> = doorRepository.getCachedDoors()

    suspend fun refresh(): ApiResult<List<AccessibleDoor>> = doorRepository.refreshDoors()
}
