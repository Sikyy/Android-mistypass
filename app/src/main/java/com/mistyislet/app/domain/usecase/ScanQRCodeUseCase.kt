package com.mistyislet.app.domain.usecase

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.AccessApi
import com.mistyislet.app.domain.model.QRUnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import javax.inject.Inject

data class ParsedQR(
    val lockId: String?,
    val qrToken: String,
)

/**
 * Handles QR code parsing and unlock requests.
 * Used when the door terminal scans the App's QR code and the backend
 * needs to verify the token.
 */
class ScanQRCodeUseCase @Inject constructor(
    private val accessApi: AccessApi,
) {
    fun parseQRContent(content: String): ParsedQR? {
        // Format 1: mistyislet://qr/{qr_token}?lock_id={lock_id}
        if (content.startsWith("mistyislet://qr/")) {
            val uri = android.net.Uri.parse(content)
            val token = uri.pathSegments.getOrNull(1) ?: return null
            val lockId = uri.getQueryParameter("lock_id")
            return ParsedQR(lockId = lockId, qrToken = token)
        }

        // Format 2: https://app.mistyislet.com/access-link/{token}
        if (content.contains("app.mistyislet.com/access-link/")) {
            val uri = android.net.Uri.parse(content)
            val token = uri.lastPathSegment ?: return null
            return ParsedQR(lockId = null, qrToken = token)
        }

        return null
    }

    suspend fun unlock(lockId: String, qrToken: String): ApiResult<UnlockResponse> {
        return safeApiCall {
            accessApi.qrUnlock(QRUnlockRequest(lockId = lockId, qrToken = qrToken))
        }
    }
}
