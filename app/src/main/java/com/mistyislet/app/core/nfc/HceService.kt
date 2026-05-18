package com.mistyislet.app.core.nfc

import android.content.Context
import android.content.SharedPreferences
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mistyislet.app.core.ble.KeystoreManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * NFC Host Card Emulation service.
 * Responds to ISO-DEP APDU commands from NFC readers.
 * Uses same ECDSA P-256 keypair as BLE authentication.
 *
 * Flow:
 * 1. Reader SELECT AID -> return protocol version
 * 2. Reader AUTHENTICATE with 52B challenge -> sign and return userId + signature
 *
 * 2FA: Keystore setUserAuthenticationRequired(true). Signing throws
 * UserNotAuthenticatedException when device locked -> return SW 69 82.
 */
@AndroidEntryPoint
class HceService : HostApduService() {

    companion object {
        private const val TAG = "HceService"
        private const val PREFS_NAME = "mistyislet_credential_enc"
        private const val KEY_USER_ID = "credential_user_id"

        private fun encryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        /** Store the userId for HCE to read. Call after credential registration. */
        fun saveUserId(context: Context, userId: String) {
            encryptedPrefs(context)
                .edit()
                .putString(KEY_USER_ID, userId)
                .apply()
        }

        fun clearUserId(context: Context) {
            encryptedPrefs(context)
                .edit()
                .remove(KEY_USER_ID)
                .apply()
        }
    }

    @Inject lateinit var keystoreManager: KeystoreManager

    private val userId: String?
        get() = encryptedPrefs(applicationContext)
            .getString(KEY_USER_ID, null)

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "APDU received: ${commandApdu.size} bytes")

        return when {
            HceProtocol.isSelectAid(commandApdu) -> handleSelect()
            HceProtocol.isAuthenticate(commandApdu) -> handleAuthenticate(commandApdu)
            else -> {
                Log.w(TAG, "Unknown APDU command")
                HceProtocol.SW_INS_NOT_SUPPORTED
            }
        }
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "link_loss"
            DEACTIVATION_DESELECTED -> "deselected"
            else -> "unknown($reason)"
        }
        Log.d(TAG, "HCE deactivated: $reasonStr")
    }

    private fun handleSelect(): ByteArray {
        Log.d(TAG, "SELECT AID — returning protocol v2")
        return HceProtocol.buildSelectResponse()
    }

    private fun handleAuthenticate(apdu: ByteArray): ByteArray {
        val currentUserId = userId
        if (currentUserId.isNullOrEmpty()) {
            Log.w(TAG, "No userId available — credential not registered")
            return HceProtocol.buildErrorResponse(HceProtocol.SW_CONDITIONS_NOT_MET)
        }

        return try {
            val challenge = HceProtocol.extractChallenge(apdu)

            // Validate challenge expiry (bytes 40..48 = expires_at big-endian uint64)
            var expiresAtUnix: Long = 0
            for (i in 40 until 48) {
                expiresAtUnix = (expiresAtUnix shl 8) or (challenge[i].toLong() and 0xFF)
            }
            if (System.currentTimeMillis() > expiresAtUnix * 1000) {
                Log.w(TAG, "Challenge expired, rejecting")
                return HceProtocol.buildErrorResponse(HceProtocol.SW_SECURITY_NOT_SATISFIED)
            }

            val nonce = challenge.sliceArray(0 until 32)

            val signature = keystoreManager.signChallengeV2(
                nonce = nonce,
                userId = currentUserId,
                transportTag = HceProtocol.TRANSPORT_TAG
            )

            Log.d(TAG, "AUTHENTICATE success for user=$currentUserId")
            HceProtocol.buildAuthResponse(currentUserId, signature)
        } catch (e: UserNotAuthenticatedException) {
            Log.w(TAG, "Device locked — 2FA required")
            HceProtocol.buildErrorResponse(HceProtocol.SW_SECURITY_NOT_SATISFIED)
        } catch (e: Exception) {
            Log.e(TAG, "AUTHENTICATE failed", e)
            HceProtocol.buildErrorResponse(HceProtocol.SW_INTERNAL_ERROR)
        }
    }
}
