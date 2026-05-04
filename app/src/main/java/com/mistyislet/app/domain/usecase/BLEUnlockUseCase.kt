package com.mistyislet.app.domain.usecase

import android.bluetooth.BluetoothDevice
import com.mistyislet.app.BuildConfig
import com.mistyislet.app.core.ble.BLEAuthClient
import com.mistyislet.app.core.ble.BLEScanService
import com.mistyislet.app.data.api.UserApi
import com.mistyislet.app.data.repository.MobileCredentialRepository
import javax.inject.Inject

/**
 * Performs a BLE unlock attempt against a Gateway reader.
 *
 * Flow:
 * 1. Verify local keypair exists (if not, return NoCredential)
 * 2. Get current user ID from API cache
 * 3. Select transport: real BLE (if reader discovered) or TCP simulator (debug)
 * 4. Perform challenge-response authentication
 * 5. Return result to UI
 */
class BLEUnlockUseCase @Inject constructor(
    private val bleClient: BLEAuthClient,
    private val credentialRepo: MobileCredentialRepository,
    private val userApi: UserApi,
) {
    sealed class UnlockResult {
        data object Success : UnlockResult()
        data class Denied(val reason: String) : UnlockResult()
        data class Failed(val message: String) : UnlockResult()
        data object NoCredential : UnlockResult()
    }

    /**
     * Attempt BLE unlock.
     *
     * Priority:
     * 1. If a real BLE reader is discovered nearby, connect via GATT.
     * 2. Otherwise, fall back to TCP simulator (debug builds only).
     */
    suspend fun unlock(
        targetDevice: BluetoothDevice? = null,
        gatewayHost: String = "localhost",
        gatewayPort: Int = 9900,
    ): UnlockResult {
        if (!credentialRepo.hasLocalKeyPair()) {
            return UnlockResult.NoCredential
        }

        val userId = try {
            userApi.getCurrentUser().id
        } catch (e: Exception) {
            return UnlockResult.Failed("Cannot get user info: ${e.message}")
        }

        // Try real BLE first: use passed device or nearest discovered reader
        val device = targetDevice ?: getNearestReader()
        if (device != null) {
            return when (val result = bleClient.authenticateViaBLE(device, userId)) {
                is BLEAuthClient.AuthResult.Granted -> UnlockResult.Success
                is BLEAuthClient.AuthResult.Denied -> UnlockResult.Denied(result.reason)
                is BLEAuthClient.AuthResult.Error -> UnlockResult.Failed(result.message)
            }
        }

        // Fall back to TCP simulator in debug builds
        if (BuildConfig.DEBUG) {
            return when (val result = bleClient.authenticateViaTCP(gatewayHost, gatewayPort, userId)) {
                is BLEAuthClient.AuthResult.Granted -> UnlockResult.Success
                is BLEAuthClient.AuthResult.Denied -> UnlockResult.Denied(result.reason)
                is BLEAuthClient.AuthResult.Error -> UnlockResult.Failed(result.message)
            }
        }

        return UnlockResult.Failed("No BLE reader found nearby")
    }

    private fun getNearestReader(): BluetoothDevice? {
        val readers = BLEScanService.discoveredDevices.value
        if (readers.isEmpty()) return null
        // Return the reader with strongest RSSI (closest)
        return readers.maxByOrNull { it.rssi }?.device
    }
}
