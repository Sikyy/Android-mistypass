package com.mistyislet.app.core.ble

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

/**
 * Integration tests for the BLE TCP authentication handshake.
 *
 * Since BLEAuthClient depends on Android Context and KeystoreManager depends on
 * Android Keystore (both unavailable in JVM tests), we test the wire protocol
 * by reimplementing the client-side handshake logic identically to BLEAuthClient
 * and running it against a mock Gateway TCP server.
 *
 * This verifies:
 * - Challenge reading and expiry validation
 * - Auth response encoding (userId length prefix + userId + signature)
 * - Auth result decoding (granted / denied / error)
 * - Error handling (connection refused, expired challenge, server disconnect)
 *
 * The mock Gateway implements the server side of the protocol and captures
 * the userId and signature it receives for assertion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BLEAuthClientTest {

    companion object {
        private const val CHALLENGE_SIZE = 52
        private const val RESULT_GRANTED: Byte = 0x01
        private const val RESULT_DENIED: Byte = 0x02
        private const val TIMEOUT_MS = 10_000L
    }

    // ---- Granted flow ----

    @Test
    fun `authenticateViaTCP returns Granted on successful handshake`() = runTest {
        val server = MockGateway { _, _ ->
            GatewayResponse(RESULT_GRANTED, "Welcome")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Granted, got $result", result is BLEAuthClient.AuthResult.Granted)
        assertEquals("Welcome", (result as BLEAuthClient.AuthResult.Granted).reason)

        server.stop()
    }

    @Test
    fun `authenticateViaTCP returns Granted with empty reason`() = runTest {
        val server = MockGateway { _, _ ->
            GatewayResponse(RESULT_GRANTED, "")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue(result is BLEAuthClient.AuthResult.Granted)
        assertEquals("", (result as BLEAuthClient.AuthResult.Granted).reason)

        server.stop()
    }

    // ---- Denied flow ----

    @Test
    fun `authenticateViaTCP returns Denied when gateway rejects`() = runTest {
        val server = MockGateway { _, _ ->
            GatewayResponse(RESULT_DENIED, "Credential revoked")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Denied, got $result", result is BLEAuthClient.AuthResult.Denied)
        assertEquals(RESULT_DENIED, (result as BLEAuthClient.AuthResult.Denied).code)
        assertEquals("Credential revoked", result.reason)

        server.stop()
    }

    @Test
    fun `authenticateViaTCP returns Denied with specific error code`() = runTest {
        val customCode: Byte = 0x05
        val server = MockGateway { _, _ ->
            GatewayResponse(customCode, "Schedule restriction")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue(result is BLEAuthClient.AuthResult.Denied)
        assertEquals(customCode, (result as BLEAuthClient.AuthResult.Denied).code)
        assertEquals("Schedule restriction", result.reason)

        server.stop()
    }

    // ---- Error cases ----

    @Test
    fun `authenticateViaTCP returns Error when connection refused`() = runTest {
        // Connect to a port where nothing is listening
        val result = doHandshake("localhost", 19999, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Error, got $result", result is BLEAuthClient.AuthResult.Error)
        assertTrue((result as BLEAuthClient.AuthResult.Error).message.contains("Connection"))
    }

    @Test
    fun `authenticateViaTCP returns Error when challenge is expired`() = runTest {
        val pastExpiry = (System.currentTimeMillis() / 1000) - 3600 // 1 hour ago
        val server = MockGateway(challengeExpiresAt = pastExpiry) { _, _ ->
            GatewayResponse(RESULT_GRANTED, "Should not reach here")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Error for expired challenge, got $result", result is BLEAuthClient.AuthResult.Error)
        assertTrue((result as BLEAuthClient.AuthResult.Error).message.contains("challenge"))

        server.stop()
    }

    @Test
    fun `authenticateViaTCP returns Error when server closes before sending result`() = runTest {
        val server = MockGateway(closeBeforeResult = true) { _, _ ->
            GatewayResponse(RESULT_GRANTED, "")
        }
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Error, got $result", result is BLEAuthClient.AuthResult.Error)

        server.stop()
    }

    @Test
    fun `authenticateViaTCP returns Error when server sends truncated challenge`() = runTest {
        val server = TruncatedChallengeServer()
        server.start()

        val result = doHandshake("localhost", server.port, "user-1") { _, _, _ -> ByteArray(64) }

        assertTrue("Expected Error, got $result", result is BLEAuthClient.AuthResult.Error)

        server.stop()
    }

    // ---- Protocol correctness ----

    @Test
    fun `gateway receives correct userId in auth response`() = runTest {
        var receivedUserId: String? = null

        val server = MockGateway { userId, _ ->
            receivedUserId = userId
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, "user-42") { _, _, _ -> ByteArray(64) }

        assertEquals("user-42", receivedUserId)

        server.stop()
    }

    @Test
    fun `gateway receives signature from sign function`() = runTest {
        val expectedSig = ByteArray(72) { (it + 0x10).toByte() }
        var receivedSig: ByteArray? = null

        val server = MockGateway { _, signature ->
            receivedSig = signature
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, "user-1") { _, _, _ -> expectedSig }

        assertNotNull(receivedSig)
        assertArrayEquals(expectedSig, receivedSig)

        server.stop()
    }

    @Test
    fun `nonce passed to sign function matches challenge nonce`() = runTest {
        val challengeNonce = ByteArray(32) { (it * 5).toByte() }
        var signedNonce: ByteArray? = null

        val server = MockGateway(challengeNonce = challengeNonce) { _, _ ->
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, "user-1") { nonce, _, _ ->
            signedNonce = nonce
            ByteArray(64)
        }

        assertNotNull(signedNonce)
        assertArrayEquals(challengeNonce, signedNonce)

        server.stop()
    }

    @Test
    fun `sign function receives BLE transport tag`() = runTest {
        var receivedTag: String? = null

        val server = MockGateway { _, _ ->
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, "user-1") { _, _, tag ->
            receivedTag = tag
            ByteArray(64)
        }

        assertEquals("BLE", receivedTag)

        server.stop()
    }

    @Test
    fun `sign function receives correct userId`() = runTest {
        var signedUserId: String? = null

        val server = MockGateway { _, _ ->
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, "test-user-xyz") { _, userId, _ ->
            signedUserId = userId
            ByteArray(64)
        }

        assertEquals("test-user-xyz", signedUserId)

        server.stop()
    }

    @Test
    fun `handles unicode userId correctly in wire protocol`() = runTest {
        var receivedUserId: String? = null
        val unicodeUserId = "user-张三-123"

        val server = MockGateway { userId, _ ->
            receivedUserId = userId
            GatewayResponse(RESULT_GRANTED, "OK")
        }
        server.start()

        doHandshake("localhost", server.port, unicodeUserId) { _, _, _ -> ByteArray(64) }

        assertEquals(unicodeUserId, receivedUserId)

        server.stop()
    }

    // ---- Handshake implementation (mirrors BLEAuthClient.doHandshake) ----

    /**
     * Replicates BLEAuthClient's TCP handshake logic for testing.
     * The signFn replaces KeystoreManager.signChallengeV2.
     */
    private suspend fun doHandshake(
        host: String,
        port: Int,
        userId: String,
        signFn: (nonce: ByteArray, userId: String, transportTag: String) -> ByteArray,
    ): BLEAuthClient.AuthResult = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) {
                val socket = Socket(host, port)
                socket.soTimeout = TIMEOUT_MS.toInt()
                try {
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()

                    // Step 1: read 52-byte v2 challenge
                    val nonce = readChallenge(input)
                        ?: return@withTimeoutOrNull BLEAuthClient.AuthResult.Error("Failed to read challenge")

                    // Step 2: sign with test function
                    val signature = signFn(nonce, userId, "BLE")

                    // Step 3: send auth response
                    sendAuthResponse(output, userId, signature)

                    // Step 4: read result
                    readAuthResult(input)
                } finally {
                    socket.close()
                }
            } ?: BLEAuthClient.AuthResult.Error("Authentication timed out")
        } catch (e: Exception) {
            BLEAuthClient.AuthResult.Error("Connection failed: ${e.message}")
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
        var expiresAtUnix: Long = 0
        for (i in 40 until 48) {
            expiresAtUnix = (expiresAtUnix shl 8) or (buf[i].toLong() and 0xFF)
        }
        if (System.currentTimeMillis() > expiresAtUnix * 1000) return null
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

    private fun readAuthResult(input: InputStream): BLEAuthClient.AuthResult {
        val buf = ByteArray(256)
        val n = input.read(buf)
        if (n <= 0) return BLEAuthClient.AuthResult.Error("No result received")

        val code = buf[0]
        val reason = if (n > 1) String(buf, 1, n - 1, Charsets.UTF_8) else ""

        return if (code == RESULT_GRANTED) {
            BLEAuthClient.AuthResult.Granted(reason)
        } else {
            BLEAuthClient.AuthResult.Denied(code, reason)
        }
    }

    // ---- Test infrastructure ----

    private data class GatewayResponse(val code: Byte, val reason: String)

    /**
     * Mock TCP server simulating the Gateway side of the BLE auth protocol.
     */
    private class MockGateway(
        private val challengeNonce: ByteArray = ByteArray(32) { it.toByte() },
        private val challengeExpiresAt: Long = (System.currentTimeMillis() / 1000) + 300,
        private val closeBeforeResult: Boolean = false,
        private val onAuth: (userId: String, signature: ByteArray) -> GatewayResponse,
    ) {
        private lateinit var serverSocket: ServerSocket
        private var serverThread: Thread? = null
        val port: Int get() = serverSocket.localPort

        fun start() {
            serverSocket = ServerSocket(0) // OS-assigned port
            serverThread = Thread {
                try {
                    val client = serverSocket.accept()
                    client.soTimeout = 5000
                    val input = client.getInputStream()
                    val output = client.getOutputStream()

                    // Send 52-byte challenge
                    output.write(buildChallenge())
                    output.flush()

                    // Read auth response
                    val authResponse = readAuthResponse(input)

                    if (closeBeforeResult) {
                        client.close()
                        return@Thread
                    }

                    // Send result
                    val gatewayResponse = onAuth(authResponse.userId, authResponse.signature)
                    output.write(encodeResult(gatewayResponse.code, gatewayResponse.reason))
                    output.flush()

                    // Brief delay to ensure client reads before socket closes
                    Thread.sleep(50)
                    client.close()
                } catch (_: Exception) {
                    // Expected in error test cases
                }
            }.also { it.isDaemon = true; it.start() }
        }

        fun stop() {
            try { serverSocket.close() } catch (_: Exception) {}
            serverThread?.join(2000)
        }

        private fun buildChallenge(): ByteArray {
            val issuedAt = System.currentTimeMillis() / 1000
            val buf = ByteBuffer.allocate(CHALLENGE_SIZE)
            buf.put(challengeNonce)
            buf.putLong(issuedAt)
            buf.putLong(challengeExpiresAt)
            buf.putInt(1) // gateway_id
            return buf.array()
        }

        private data class AuthResponse(val userId: String, val signature: ByteArray)

        private fun readAuthResponse(input: InputStream): AuthResponse {
            val buf = ByteArray(512)
            val n = input.read(buf)
            if (n <= 0) throw Exception("No auth response")
            val userIdLen = buf[0].toInt() and 0xFF
            val userId = String(buf, 1, userIdLen, Charsets.UTF_8)
            val signature = buf.copyOfRange(1 + userIdLen, n)
            return AuthResponse(userId, signature)
        }

        private fun encodeResult(code: Byte, reason: String): ByteArray {
            val reasonBytes = reason.toByteArray(Charsets.UTF_8)
            val result = ByteArray(1 + reasonBytes.size)
            result[0] = code
            reasonBytes.copyInto(result, 1)
            return result
        }
    }

    /**
     * Server that sends a truncated challenge (only 30 of 52 bytes) then closes.
     */
    private class TruncatedChallengeServer {
        private lateinit var serverSocket: ServerSocket
        private var serverThread: Thread? = null
        val port: Int get() = serverSocket.localPort

        fun start() {
            serverSocket = ServerSocket(0)
            serverThread = Thread {
                try {
                    val client = serverSocket.accept()
                    val output = client.getOutputStream()
                    output.write(ByteArray(30)) // truncated
                    output.flush()
                    client.close()
                } catch (_: Exception) {}
            }.also { it.isDaemon = true; it.start() }
        }

        fun stop() {
            try { serverSocket.close() } catch (_: Exception) {}
            serverThread?.join(2000)
        }
    }
}
