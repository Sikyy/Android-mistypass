package com.mistyislet.app.data.repository

import android.os.Build
import android.util.Log
import com.mistyislet.app.core.ble.KeystoreManager
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.MobileCredentialApi
import com.mistyislet.app.data.api.RegisterMobileCredentialRequest
import com.mistyislet.app.domain.model.MobileCredential
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the BLE mobile credential lifecycle:
 * - Generate Keystore keypair → register public key with cloud
 * - Refresh TTL periodically
 * - Revoke on demand (lost phone, employee termination)
 */
@Singleton
class MobileCredentialRepository @Inject constructor(
    private val api: MobileCredentialApi,
    private val keystoreManager: KeystoreManager,
) {
    /**
     * Full registration flow:
     * 1. Generate EC P-256 keypair in Android Keystore (StrongBox if available)
     * 2. Extract public key PEM + attestation cert chain
     * 3. Register with cloud API
     */
    suspend fun registerCredential(): ApiResult<MobileCredential> = safeApiCall {
        // Generate fresh keypair
        keystoreManager.generateKeyPair()

        val publicKeyPem = keystoreManager.getPublicKeyPEM()
        val attestationChain = keystoreManager.getAttestationChain()
        val keystoreLevel = keystoreManager.getKeystoreLevel()
        val deviceId = "${Build.BOARD}_${Build.FINGERPRINT.hashCode().toUInt()}"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

        val request = RegisterMobileCredentialRequest(
            publicKeyPem = publicKeyPem,
            deviceId = deviceId,
            deviceModel = deviceModel,
            keystoreLevel = keystoreLevel,
            attestationCertChain = attestationChain,
        )

        val response = api.registerCredential(request)
        if (response.isSuccessful) {
            response.body()!!.credential
        } else {
            throw Exception("Registration failed: ${response.code()} ${response.errorBody()?.string()}")
        }
    }

    /** Lists all mobile credentials for the current user. */
    suspend fun listCredentials(): ApiResult<List<MobileCredential>> = safeApiCall {
        api.listMobileCredentials().items
    }

    /** Revokes a credential and deletes the local keypair. */
    suspend fun revokeCredential(credentialId: String): ApiResult<Unit> = safeApiCall {
        val response = api.revokeMobileCredential(credentialId)
        if (response.isSuccessful) {
            keystoreManager.deleteKeyPair()
        } else {
            throw Exception("Revoke failed: ${response.code()}")
        }
    }

    /** Refreshes credential TTL. */
    suspend fun refreshCredential(credentialId: String): ApiResult<MobileCredential> = safeApiCall {
        val response = api.refreshMobileCredential(credentialId)
        if (response.isSuccessful) {
            response.body()!!.credential
        } else {
            throw Exception("Refresh failed: ${response.code()}")
        }
    }

    /** Whether a local keypair exists in Keystore. */
    fun hasLocalKeyPair(): Boolean = keystoreManager.hasKeyPair()

    /**
     * Checks if any active credential is expiring soon (within 24h) and re-registers if needed.
     * Call on app launch and foreground resume.
     */
    suspend fun renewIfNeeded() {
        if (!hasLocalKeyPair()) return
        when (val result = listCredentials()) {
            is ApiResult.Success -> {
                val hasValid = result.data.any { cred ->
                    cred.status == "active" && !isExpiringSoon(cred)
                }
                if (!hasValid) {
                    Log.i(TAG, "BLE credential expired or expiring soon, re-registering")
                    registerCredential()
                }
            }
            else -> Log.w(TAG, "Credential renewal check failed: could not fetch credentials")
        }
    }

    private fun isExpiringSoon(credential: MobileCredential): Boolean {
        val expiresAt = credential.expiresAt ?: return false
        return try {
            val expiry = Instant.parse(expiresAt)
            Instant.now().plus(24, ChronoUnit.HOURS).isAfter(expiry)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "MobileCredRepo"
    }
}
