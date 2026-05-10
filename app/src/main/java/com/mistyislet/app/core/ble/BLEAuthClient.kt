package com.mistyislet.app.core.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE Authentication Client — performs the challenge-response handshake with the Gateway.
 *
 * Transport modes:
 * 1. TCP Simulator (debug): connects to Gateway via TCP (same protocol as BLE GATT)
 * 2. Real BLE GATT (production): connects via Bluetooth Low Energy using Nordic library
 *
 * Protocol (identical for both transports):
 *   1. Read CHALLENGE:     48 bytes [32 nonce][8 issued_at][8 expires_at]
 *   2. Write AUTH_RESPONSE: [1 byte userId_len][userId bytes][signature bytes]
 *   3. Read AUTH_RESULT:   [1 byte code][reason string]
 */
@Singleton
class BLEAuthClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager,
) {
    companion object {
        private const val CHALLENGE_SIZE = 52 // v2: 32B nonce + 8B issued + 8B expires + 4B gateway_id
        private const val RESULT_GRANTED: Byte = 0x01
        private const val TIMEOUT_MS = 10_000L
    }

    sealed class AuthResult {
        data class Granted(val reason: String) : AuthResult()
        data class Denied(val code: Byte, val reason: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    /**
     * Authenticate via real BLE GATT connection using Nordic library.
     *
     * @param device The BluetoothDevice (Mistyislet Reader) to connect to.
     * @param userId User ID for challenge signing.
     */
    suspend fun authenticateViaBLE(device: BluetoothDevice, userId: String): AuthResult {
        val bleManager = MistyisletBleManager(context, keystoreManager)
        return bleManager.authenticate(device, userId)
    }

    /**
     * Authenticate via TCP simulator (debug/MVP mode).
     * The Android app connects to Gateway's TCP listener (default port 9900)
     * and performs the same challenge-response protocol as real BLE.
     */
    suspend fun authenticateViaTCP(host: String, port: Int, userId: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    doHandshake(host, port, userId)
                } ?: AuthResult.Error("Authentication timed out")
            } catch (e: Exception) {
                AuthResult.Error("Connection failed: ${e.message}")
            }
        }

    private fun doHandshake(host: String, port: Int, userId: String): AuthResult {
        val socket = Socket(host, port)
        socket.soTimeout = TIMEOUT_MS.toInt()
        return try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // Step 1: read 48-byte challenge
            val nonce = readChallenge(input) ?: return AuthResult.Error("Failed to read challenge")

            // Step 2: sign nonce with Keystore private key
            val signature = keystoreManager.signChallengeV2(nonce, userId, "BLE")

            // Step 3: send auth response
            sendAuthResponse(output, userId, signature)

            // Step 4: read result
            readAuthResult(input)
        } finally {
            socket.close()
        }
    }

    private fun readChallenge(input: InputStream): ByteArray? {
        val buf = ByteArray(CHALLENGE_SIZE)
        var read = 0
        while (read < CHALLENGE_SIZE) {
            val n = input.read(buf, read, CHALLENGE_SIZE - read)
            if (n <= 0) return null
            read += n
        }
        // Return only the 32-byte nonce (first part of the challenge)
        return buf.copyOfRange(0, 32)
    }

    private fun sendAuthResponse(output: OutputStream, userId: String, signature: ByteArray) {
        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + userIdBytes.size + signature.size)
        payload[0] = userIdBytes.size.toByte()
        userIdBytes.copyInto(payload, 1)
        signature.copyInto(payload, 1 + userIdBytes.size)
        output.write(payload)
        output.flush()
    }

    private fun readAuthResult(input: InputStream): AuthResult {
        val buf = ByteArray(256)
        val n = input.read(buf)
        if (n <= 0) return AuthResult.Error("No result received")

        val code = buf[0]
        val reason = if (n > 1) String(buf, 1, n - 1, Charsets.UTF_8) else ""

        return if (code == RESULT_GRANTED) {
            AuthResult.Granted(reason)
        } else {
            AuthResult.Denied(code, reason)
        }
    }
}
