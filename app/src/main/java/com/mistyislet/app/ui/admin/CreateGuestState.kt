package com.mistyislet.app.ui.admin

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.domain.model.GuestVisit

/**
 * Result of a create-guest submission, consumed by the create sheet: Submitting disables
 * the form's confirm action while the request is in flight, Success closes the sheet,
 * Error keeps it open with the backend message pinned in the form (server returns 400
 * when a selected door does not belong to the place).
 */
sealed interface CreateGuestState {
    data object Idle : CreateGuestState
    data object Submitting : CreateGuestState
    data object Success : CreateGuestState
    data class Error(val message: String) : CreateGuestState
}

internal fun createGuestStateOf(result: ApiResult<GuestVisit>): CreateGuestState = when (result) {
    is ApiResult.Success -> CreateGuestState.Success
    is ApiResult.Error -> CreateGuestState.Error(result.message)
    is ApiResult.Exception -> CreateGuestState.Error(result.throwable.message ?: "Unknown error")
}
