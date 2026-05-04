package com.mistyislet.app.domain.usecase

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.DoorRepository
import com.mistyislet.app.domain.model.UnlockResponse
import javax.inject.Inject

class UnlockDoorUseCase @Inject constructor(
    private val doorRepository: DoorRepository,
) {
    suspend operator fun invoke(lockId: String): ApiResult<UnlockResponse> {
        return doorRepository.unlock(lockId)
    }
}
