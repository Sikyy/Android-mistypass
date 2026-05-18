package com.mistyislet.app.core.ble

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Tests the BLE authentication wire protocol logic.
 *
 * BLEAuthClient methods are private, so we test the protocol encoding/decoding
 * by exercising the same byte-level logic the client uses:
 *
 * Protocol v2:
 *   1. Challenge:    52 bytes [32B nonce][8B issued_at][8B expires_at][4B gateway_id]
 *   2. Auth response: [1B userId_len][userId bytes][signature bytes]
 *   3. Auth result:   [1B code][reason string]
 */
class BLEAuthProtocolTest {

    companion object {
        private const val CHALLENGE_SIZE = 52
        private const val RESULT_GRANTED: Byte = 0x01
        private const val RESULT_DENIED: Byte = 0x02
    }

    // ---- Challenge wire format ----

    @Test
    fun `challenge v2 is exactly 52 bytes`() {
        val challenge = buildValidChallenge()
        assertEquals(CHALLENGE_SIZE, challenge.size)
    }

    @Test
    fun `challenge nonce occupies first 32 bytes`() {
        val nonce = ByteArray(32) { (it + 1).toByte() }
        val challenge = buildChallenge(nonce = nonce)

        val extractedNonce = challenge.copyOfRange(0, 32)
        assertArrayEquals(nonce, extractedNonce)
    }

    @Test
    fun `challenge issued_at occupies bytes 32-40 as big-endian uint64`() {
        val issuedAt = 1700000000L
        val challenge = buildChallenge(issuedAt = issuedAt)

        val extractedIssuedAt = ByteBuffer.wrap(challenge, 32, 8).long
        assertEquals(issuedAt, extractedIssuedAt)
    }

    @Test
    fun `challenge expires_at occupies bytes 40-48 as big-endian uint64`() {
        val expiresAt = 1700003600L // 1 hour later
        val challenge = buildChallenge(expiresAt = expiresAt)

        val extractedExpiresAt = ByteBuffer.wrap(challenge, 40, 8).long
        assertEquals(expiresAt, extractedExpiresAt)
    }

    @Test
    fun `challenge gateway_id occupies bytes 48-52 as big-endian int32`() {
        val gatewayId = 42
        val challenge = buildChallenge(gatewayId = gatewayId)

        val extractedGatewayId = ByteBuffer.wrap(challenge, 48, 4).int
        assertEquals(gatewayId, extractedGatewayId)
    }

    @Test
    fun `expired challenge is detected by comparing expires_at to current time`() {
        val pastExpiresAt = (System.currentTimeMillis() / 1000) - 3600 // 1 hour ago
        val challenge = buildChallenge(expiresAt = pastExpiresAt)

        val expiresAtUnix = readBigEndianLong(challenge, 40)
        val isExpired = System.currentTimeMillis() > expiresAtUnix * 1000

        assertTrue("Challenge should be detected as expired", isExpired)
    }

    @Test
    fun `valid challenge is not expired`() {
        val futureExpiresAt = (System.currentTimeMillis() / 1000) + 3600 // 1 hour from now
        val challenge = buildChallenge(expiresAt = futureExpiresAt)

        val expiresAtUnix = readBigEndianLong(challenge, 40)
        val isExpired = System.currentTimeMillis() > expiresAtUnix * 1000

        assertFalse("Challenge should not be expired", isExpired)
    }

    // ---- Challenge reading from stream ----

    @Test
    fun `readChallenge reads exactly 52 bytes from stream`() {
        val challenge = buildValidChallenge()
        val input = ByteArrayInputStream(challenge)

        val buf = ByteArray(CHALLENGE_SIZE)
        var read = 0
        while (read < CHALLENGE_SIZE) {
            val n = input.read(buf, read, CHALLENGE_SIZE - read)
            if (n <= 0) break
            read += n
        }

        assertEquals(CHALLENGE_SIZE, read)
        assertArrayEquals(challenge, buf)
    }

    @Test
    fun `readChallenge handles fragmented stream delivery`() {
        val challenge = buildValidChallenge()
        // Simulate fragmented reads: stream delivers 10 bytes at a time
        val fragmentedInput = FragmentedInputStream(challenge, 10)

        val buf = ByteArray(CHALLENGE_SIZE)
        var read = 0
        while (read < CHALLENGE_SIZE) {
            val n = fragmentedInput.read(buf, read, CHALLENGE_SIZE - read)
            if (n <= 0) break
            read += n
        }

        assertEquals(CHALLENGE_SIZE, read)
        assertArrayEquals(challenge, buf)
    }

    @Test
    fun `readChallenge fails on empty stream`() {
        val input = ByteArrayInputStream(ByteArray(0))

        val buf = ByteArray(CHALLENGE_SIZE)
        val n = input.read(buf, 0, CHALLENGE_SIZE)

        assertEquals(-1, n) // EOF
    }

    @Test
    fun `readChallenge fails on truncated stream`() {
        // Only 30 bytes available, need 52
        val truncated = ByteArray(30) { it.toByte() }
        val input = ByteArrayInputStream(truncated)

        val buf = ByteArray(CHALLENGE_SIZE)
        var read = 0
        while (read < CHALLENGE_SIZE) {
            val n = input.read(buf, read, CHALLENGE_SIZE - read)
            if (n <= 0) break
            read += n
        }

        assertTrue("Should not have read full challenge", read < CHALLENGE_SIZE)
    }

    // ---- Auth response wire format ----

    @Test
    fun `auth response encodes userId with length prefix`() {
        val userId = "user-42"
        val signature = ByteArray(64) { 0xAB.toByte() }

        val payload = encodeAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        assertEquals(userIdBytes.size.toByte(), payload[0])
    }

    @Test
    fun `auth response contains userId bytes after length prefix`() {
        val userId = "test-user"
        val signature = ByteArray(72)

        val payload = encodeAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val extractedUserId = String(payload, 1, userIdBytes.size, Charsets.UTF_8)
        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `auth response contains signature after userId`() {
        val userId = "u1"
        val signature = ByteArray(70) { (it + 1).toByte() }

        val payload = encodeAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val sigOffset = 1 + userIdBytes.size
        val extractedSig = payload.copyOfRange(sigOffset, payload.size)
        assertArrayEquals(signature, extractedSig)
    }

    @Test
    fun `auth response total size is 1 + userId length + signature length`() {
        val userId = "user-abc-123"
        val signature = ByteArray(71)

        val payload = encodeAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        assertEquals(1 + userIdBytes.size + signature.size, payload.size)
    }

    @Test
    fun `auth response round trip - decode what was encoded`() {
        val userId = "round-trip-user"
        val signature = ByteArray(64) { (it * 7).toByte() }

        val payload = encodeAuthResponse(userId, signature)

        // Decode
        val decodedUserIdLen = payload[0].toInt() and 0xFF
        val decodedUserId = String(payload, 1, decodedUserIdLen, Charsets.UTF_8)
        val decodedSig = payload.copyOfRange(1 + decodedUserIdLen, payload.size)

        assertEquals(userId, decodedUserId)
        assertArrayEquals(signature, decodedSig)
    }

    // ---- Auth result wire format ----

    @Test
    fun `auth result granted is decoded correctly`() {
        val reason = "Welcome"
        val resultBytes = encodeAuthResult(RESULT_GRANTED, reason)
        val input = ByteArrayInputStream(resultBytes)

        val buf = ByteArray(256)
        val n = input.read(buf)

        assertTrue(n > 0)
        assertEquals(RESULT_GRANTED, buf[0])
        val decodedReason = String(buf, 1, n - 1, Charsets.UTF_8)
        assertEquals(reason, decodedReason)
    }

    @Test
    fun `auth result denied is decoded correctly`() {
        val reason = "Credential revoked"
        val resultBytes = encodeAuthResult(RESULT_DENIED, reason)
        val input = ByteArrayInputStream(resultBytes)

        val buf = ByteArray(256)
        val n = input.read(buf)

        assertTrue(n > 0)
        assertNotEquals(RESULT_GRANTED, buf[0])
        assertEquals(RESULT_DENIED, buf[0])
        val decodedReason = String(buf, 1, n - 1, Charsets.UTF_8)
        assertEquals(reason, decodedReason)
    }

    @Test
    fun `auth result with empty reason`() {
        val resultBytes = encodeAuthResult(RESULT_GRANTED, "")
        val input = ByteArrayInputStream(resultBytes)

        val buf = ByteArray(256)
        val n = input.read(buf)

        assertEquals(1, n) // just the code byte
        assertEquals(RESULT_GRANTED, buf[0])
    }

    @Test
    fun `auth result with long reason string`() {
        val reason = "Access denied: credential expired at 2025-01-01T00:00:00Z for door Main-Entrance"
        val resultBytes = encodeAuthResult(RESULT_DENIED, reason)
        val input = ByteArrayInputStream(resultBytes)

        val buf = ByteArray(256)
        val n = input.read(buf)

        val decodedReason = String(buf, 1, n - 1, Charsets.UTF_8)
        assertEquals(reason, decodedReason)
    }

    // ---- Auth response writing to stream ----

    @Test
    fun `auth response is written as single payload to output stream`() {
        val userId = "writer-test"
        val signature = ByteArray(64)
        val output = ByteArrayOutputStream()

        val payload = encodeAuthResponse(userId, signature)
        output.write(payload)
        output.flush()

        val written = output.toByteArray()
        assertEquals(payload.size, written.size)
        assertArrayEquals(payload, written)
    }

    // ---- V2 challenge signing message construction ----

    @Test
    fun `v2 signing message is nonce concatenated with userId and transport tag`() {
        val nonce = ByteArray(32) { it.toByte() }
        val userId = "user-1"
        val transportTag = "BLE"

        val message = nonce + userId.toByteArray(Charsets.UTF_8) + transportTag.toByteArray(Charsets.UTF_8)

        assertEquals(32 + 6 + 3, message.size) // nonce + "user-1" + "BLE"
        assertArrayEquals(nonce, message.copyOfRange(0, 32))
    }

    @Test
    fun `v2 signing message differs between BLE and NFC_HCE transport tags`() {
        val nonce = ByteArray(32) { 0x42 }
        val userId = "same-user"

        val bleMessage = nonce + userId.toByteArray(Charsets.UTF_8) + "BLE".toByteArray(Charsets.UTF_8)
        val nfcMessage = nonce + userId.toByteArray(Charsets.UTF_8) + "NFC_HCE".toByteArray(Charsets.UTF_8)

        assertFalse(
            "BLE and NFC_HCE signing messages must differ for transport binding",
            bleMessage.contentEquals(nfcMessage)
        )
        // NFC_HCE tag is longer
        assertTrue(nfcMessage.size > bleMessage.size)
    }

    // ---- Helpers ----

    private fun buildValidChallenge(): ByteArray {
        val futureExpiry = (System.currentTimeMillis() / 1000) + 300
        return buildChallenge(expiresAt = futureExpiry)
    }

    private fun buildChallenge(
        nonce: ByteArray = ByteArray(32) { it.toByte() },
        issuedAt: Long = System.currentTimeMillis() / 1000,
        expiresAt: Long = (System.currentTimeMillis() / 1000) + 300,
        gatewayId: Int = 1,
    ): ByteArray {
        require(nonce.size == 32)
        val buf = ByteBuffer.allocate(CHALLENGE_SIZE)
        buf.put(nonce)
        buf.putLong(issuedAt)
        buf.putLong(expiresAt)
        buf.putInt(gatewayId)
        return buf.array()
    }

    private fun readBigEndianLong(data: ByteArray, offset: Int): Long {
        var value: Long = 0
        for (i in offset until offset + 8) {
            value = (value shl 8) or (data[i].toLong() and 0xFF)
        }
        return value
    }

    /** Encodes auth response: [1B userId_len][userId bytes][signature bytes] */
    private fun encodeAuthResponse(userId: String, signature: ByteArray): ByteArray {
        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + userIdBytes.size + signature.size)
        payload[0] = userIdBytes.size.toByte()
        userIdBytes.copyInto(payload, 1)
        signature.copyInto(payload, 1 + userIdBytes.size)
        return payload
    }

    /** Encodes auth result: [1B code][reason bytes] */
    private fun encodeAuthResult(code: Byte, reason: String): ByteArray {
        val reasonBytes = reason.toByteArray(Charsets.UTF_8)
        val result = ByteArray(1 + reasonBytes.size)
        result[0] = code
        reasonBytes.copyInto(result, 1)
        return result
    }

    /**
     * InputStream that delivers data in fixed-size fragments, simulating
     * real BLE/TCP streams that may not deliver all bytes at once.
     */
    private class FragmentedInputStream(
        private val data: ByteArray,
        private val fragmentSize: Int,
    ) : java.io.InputStream() {
        private var pos = 0

        override fun read(): Int {
            if (pos >= data.size) return -1
            return data[pos++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (pos >= data.size) return -1
            val available = minOf(len, fragmentSize, data.size - pos)
            System.arraycopy(data, pos, b, off, available)
            pos += available
            return available
        }
    }
}
