package com.mistyislet.app.core.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Nordic BLE Manager for Mistyislet Reader GATT communication.
 *
 * Protocol v2:
 *   1. Connect to Reader advertising SERVICE_UUID
 *   2. Read CHALLENGE (52 bytes): [32B nonce][8B issued_at][8B expires_at][4B gateway_id]
 *   3. Validate: expiry + gateway_id match reader identity
 *   4. Sign: SHA256withECDSA(nonce || userId || "BLE") with Keystore private key
 *   5. Write AUTH_RESPONSE: [1B userIdLen][userId bytes][signature bytes]
 *   6. Receive AUTH_RESULT notification: [1B code][reason string]
 */
class MistyisletBleManager(
    context: Context,
    private val keystoreManager: KeystoreManager,
) : BleManager(context) {

    companion object {
        private const val TAG = "MistyBLE"

        // GATT Service & Characteristic UUIDs (from ble_protocol.go)
        val SERVICE_UUID: UUID = UUID.fromString("4d495354-5950-4153-532d-424c45415554")
        val CHALLENGE_UUID: UUID = UUID.fromString("4d495354-5950-4153-532d-4348414c4c4e")
        val AUTH_RESPONSE_UUID: UUID = UUID.fromString("4d495354-5950-4153-532d-415554485245")
        val READER_IDENTITY_UUID: UUID = UUID.fromString("4d495354-5950-4153-532d-524541444552")
        val AUTH_RESULT_UUID: UUID = UUID.fromString("4d495354-5950-4153-532d-524553554c54")

        private const val CHALLENGE_V2_SIZE = 52 // 32B nonce + 8B issued + 8B expires + 4B gateway_id
        private const val NONCE_SIZE = 32
        private const val RESULT_GRANTED: Byte = 0x01
        private const val TRANSPORT_TAG = "BLE"

        private const val AUTH_TIMEOUT_MS = 10_000L
    }

    // GATT characteristics discovered during connection
    private var challengeChar: BluetoothGattCharacteristic? = null
    private var authResponseChar: BluetoothGattCharacteristic? = null
    private var authResultChar: BluetoothGattCharacteristic? = null
    private var readerIdentityChar: BluetoothGattCharacteristic? = null

    // Deferred result for auth flow
    private var authResultDeferred: CompletableDeferred<BLEAuthClient.AuthResult>? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(SERVICE_UUID) ?: run {
            Log.w(TAG, "Mistyislet BLE service not found")
            return false
        }
        challengeChar = service.getCharacteristic(CHALLENGE_UUID)
        authResponseChar = service.getCharacteristic(AUTH_RESPONSE_UUID)
        authResultChar = service.getCharacteristic(AUTH_RESULT_UUID)
        readerIdentityChar = service.getCharacteristic(READER_IDENTITY_UUID)

        val hasRequired = challengeChar != null && authResponseChar != null && authResultChar != null
        if (!hasRequired) {
            Log.w(TAG, "Missing required characteristics")
        }
        return hasRequired
    }

    override fun initialize() {
        // Enable notifications on AUTH_RESULT for receiving unlock results
        setNotificationCallback(authResultChar).with { _, data ->
            handleAuthResult(data)
        }
        enableNotifications(authResultChar).enqueue()
    }

    override fun onServicesInvalidated() {
        challengeChar = null
        authResponseChar = null
        authResultChar = null
        readerIdentityChar = null
    }

    /**
     * Perform the full BLE challenge-response authentication.
     *
     * @param device The BluetoothDevice to connect to.
     * @param userId The user's ID for signing.
     * @return Authentication result.
     */
    suspend fun authenticate(device: BluetoothDevice, userId: String): BLEAuthClient.AuthResult {
        return try {
            withTimeout(AUTH_TIMEOUT_MS) {
                // Connect with auto-connect=false for faster initial connection
                connect(device)
                    .retry(2, 300)
                    .useAutoConnect(false)
                    .timeout(5000)
                    .suspend()

                Log.d(TAG, "Connected to ${device.address}")

                // Request higher MTU for signature payload.
                // Auth response = [1B len] + userId (UTF-8) + ECDSA ASN.1 DER signature (~70-72B).
                // 64 leaves only 61 usable bytes after ATT overhead — always truncates.
                requestMtu(256).suspend()

                // Step 0: Read reader identity (if available) for gateway_id validation
                var readerId: String? = null
                readerIdentityChar?.let { idChar ->
                    val idData = readCharacteristic(idChar).suspend()
                    readerId = idData.value?.let { String(it, Charsets.UTF_8) }
                    if (!readerId.isNullOrBlank()) {
                        Log.d(TAG, "Reader identity: $readerId")
                    }
                }

                // Step 1: Read v2 challenge (52 bytes)
                val challengeData = readCharacteristic(challengeChar).suspend()
                val challengeBytes = challengeData.value ?: throw Exception("Empty challenge")
                if (challengeBytes.size < CHALLENGE_V2_SIZE) {
                    throw Exception("Challenge too short: ${challengeBytes.size} bytes, expected $CHALLENGE_V2_SIZE")
                }

                Log.d(TAG, "Challenge received: ${challengeBytes.size} bytes")

                // Validate challenge expiry (bytes 40..48 = expires_at as big-endian uint64)
                val expiresAtUnix = challengeBytes.sliceArray(40 until 48).toLong()
                val expiresAtMs = expiresAtUnix * 1000
                if (System.currentTimeMillis() > expiresAtMs) {
                    throw Exception("Challenge expired")
                }

                // Validate gateway_id (bytes 48..52) matches reader identity
                readerId?.let { rid ->
                    val challengeGatewayId = ByteBuffer.wrap(challengeBytes, 48, 4).int
                    val digest = MessageDigest.getInstance("SHA-256").digest(rid.toByteArray(Charsets.UTF_8))
                    val expectedGatewayId = ByteBuffer.wrap(digest, 0, 4).int
                    if (challengeGatewayId != expectedGatewayId) {
                        throw Exception("Gateway ID mismatch: challenge from wrong reader")
                    }
                }

                // Extract 32-byte nonce
                val nonce = challengeBytes.copyOfRange(0, NONCE_SIZE)

                // Step 2: Sign with Keystore using v2 (includes transport tag)
                val signature = keystoreManager.signChallengeV2(nonce, userId, TRANSPORT_TAG)
                Log.d(TAG, "Signature generated: ${signature.size} bytes (v2 with transport tag)")

                // Step 3: Write auth response [1B len][userId][signature]
                val userIdBytes = userId.toByteArray(Charsets.UTF_8)
                val payload = ByteArray(1 + userIdBytes.size + signature.size)
                payload[0] = userIdBytes.size.toByte()
                userIdBytes.copyInto(payload, 1)
                signature.copyInto(payload, 1 + userIdBytes.size)

                // Set up deferred before writing to avoid race condition
                authResultDeferred = CompletableDeferred()

                writeCharacteristic(
                    authResponseChar,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                ).suspend()

                Log.d(TAG, "Auth response written, waiting for result...")

                // Step 4: Wait for result notification
                val result = authResultDeferred!!.await()
                disconnect().suspend()
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE authentication failed", e)
            try { disconnect().enqueue() } catch (_: Exception) {}
            BLEAuthClient.AuthResult.Error("BLE error: ${e.message}")
        }
    }

    private fun handleAuthResult(data: Data) {
        val bytes = data.value
        if (bytes == null || bytes.isEmpty()) {
            authResultDeferred?.complete(BLEAuthClient.AuthResult.Error("Empty result"))
            return
        }

        val code = bytes[0]
        val reason = if (bytes.size > 1) String(bytes, 1, bytes.size - 1, Charsets.UTF_8) else ""

        val result = if (code == RESULT_GRANTED) {
            BLEAuthClient.AuthResult.Granted(reason)
        } else {
            BLEAuthClient.AuthResult.Denied(code, reason)
        }

        Log.d(TAG, "Auth result: $result")
        authResultDeferred?.complete(result)
    }
}

private fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).long
