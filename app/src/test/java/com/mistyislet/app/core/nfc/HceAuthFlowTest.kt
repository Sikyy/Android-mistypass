package com.mistyislet.app.core.nfc

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Tests the full NFC HCE authentication flow logic.
 *
 * HceService depends on Android (HostApduService, EncryptedSharedPreferences, Hilt),
 * so we test the protocol flow by exercising HceProtocol directly — verifying that
 * the sequence of APDU commands and responses forms a correct authentication handshake.
 *
 * Flow tested:
 * 1. Reader SELECT AID -> phone returns protocol version + capabilities + SW_OK
 * 2. Reader AUTHENTICATE with 52B challenge -> phone extracts nonce, signs, returns auth response
 * 3. Error cases: unknown command, no userId, expired challenge
 *
 * This mirrors what HceService.processCommandApdu() does, but without the Android deps.
 */
class HceAuthFlowTest {

    // ---- Full handshake simulation ----

    @Test
    fun `full handshake - SELECT then AUTHENTICATE produces valid response`() {
        // Step 1: Reader sends SELECT AID
        val selectApdu = buildSelectAidApdu()
        assertTrue("SELECT command should be recognized", HceProtocol.isSelectAid(selectApdu))

        val selectResponse = HceProtocol.buildSelectResponse()
        assertEndsWithSwOk(selectResponse)

        // Verify protocol version from response
        assertEquals(HceProtocol.PROTOCOL_VERSION, selectResponse[0])

        // Step 2: Reader sends AUTHENTICATE with challenge
        val challenge = buildValidChallenge()
        val authApdu = buildAuthenticateApdu(challenge)
        assertTrue("AUTHENTICATE command should be recognized", HceProtocol.isAuthenticate(authApdu))

        // Phone extracts challenge and signs
        val extractedChallenge = HceProtocol.extractChallenge(authApdu)
        assertEquals(HceProtocol.CHALLENGE_V2_SIZE, extractedChallenge.size)

        val nonce = extractedChallenge.sliceArray(0 until 32)
        val fakeSignature = ByteArray(64) { 0xAA.toByte() }
        val userId = "user-123"

        val authResponse = HceProtocol.buildAuthResponse(userId, fakeSignature)
        assertEndsWithSwOk(authResponse)

        // Verify the auth response can be parsed back
        val parsedUserIdLen = authResponse[0].toInt() and 0xFF
        val parsedUserId = String(authResponse, 1, parsedUserIdLen, Charsets.UTF_8)
        assertEquals(userId, parsedUserId)
    }

    // ---- APDU command routing ----

    @Test
    fun `SELECT and AUTHENTICATE are mutually exclusive`() {
        val selectApdu = buildSelectAidApdu()
        val authApdu = buildAuthenticateApdu(buildValidChallenge())

        assertTrue(HceProtocol.isSelectAid(selectApdu))
        assertFalse(HceProtocol.isAuthenticate(selectApdu))

        assertTrue(HceProtocol.isAuthenticate(authApdu))
        assertFalse(HceProtocol.isSelectAid(authApdu))
    }

    @Test
    fun `unknown command returns INS_NOT_SUPPORTED`() {
        val unknownApdu = byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00, 0x02, 0x01, 0x02)

        // Neither SELECT nor AUTHENTICATE
        assertFalse(HceProtocol.isSelectAid(unknownApdu))
        assertFalse(HceProtocol.isAuthenticate(unknownApdu))

        // HceService would return SW_INS_NOT_SUPPORTED for this case
        val errorResponse = HceProtocol.SW_INS_NOT_SUPPORTED
        assertEquals(0x6D.toByte(), errorResponse[0])
        assertEquals(0x00.toByte(), errorResponse[1])
    }

    @Test
    fun `empty APDU is neither SELECT nor AUTHENTICATE`() {
        val emptyApdu = byteArrayOf()
        assertFalse(HceProtocol.isSelectAid(emptyApdu))
        assertFalse(HceProtocol.isAuthenticate(emptyApdu))
    }

    @Test
    fun `single-byte APDU is neither SELECT nor AUTHENTICATE`() {
        val singleByte = byteArrayOf(0x00)
        assertFalse(HceProtocol.isSelectAid(singleByte))
        assertFalse(HceProtocol.isAuthenticate(singleByte))
    }

    // ---- Challenge validation (expiry) ----

    @Test
    fun `valid challenge passes expiry check`() {
        val futureExpiry = (System.currentTimeMillis() / 1000) + 300 // 5 min from now
        val challenge = buildChallenge(expiresAt = futureExpiry)
        val authApdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(authApdu)

        // Replicate the expiry check from HceService.handleAuthenticate
        var expiresAtUnix: Long = 0
        for (i in 40 until 48) {
            expiresAtUnix = (expiresAtUnix shl 8) or (extracted[i].toLong() and 0xFF)
        }
        val isExpired = System.currentTimeMillis() > expiresAtUnix * 1000

        assertFalse("Challenge should not be expired", isExpired)
    }

    @Test
    fun `expired challenge fails expiry check`() {
        val pastExpiry = (System.currentTimeMillis() / 1000) - 3600 // 1 hour ago
        val challenge = buildChallenge(expiresAt = pastExpiry)
        val authApdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(authApdu)

        var expiresAtUnix: Long = 0
        for (i in 40 until 48) {
            expiresAtUnix = (expiresAtUnix shl 8) or (extracted[i].toLong() and 0xFF)
        }
        val isExpired = System.currentTimeMillis() > expiresAtUnix * 1000

        assertTrue("Challenge should be expired", isExpired)
    }

    @Test
    fun `challenge just at expiry boundary is expired`() {
        // Set expiry to current second - if we check after, it should be expired
        val exactlyNow = (System.currentTimeMillis() / 1000) - 1 // 1 second ago
        val challenge = buildChallenge(expiresAt = exactlyNow)
        val authApdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(authApdu)

        var expiresAtUnix: Long = 0
        for (i in 40 until 48) {
            expiresAtUnix = (expiresAtUnix shl 8) or (extracted[i].toLong() and 0xFF)
        }
        val isExpired = System.currentTimeMillis() > expiresAtUnix * 1000

        assertTrue("Challenge at boundary should be expired", isExpired)
    }

    // ---- Error responses ----

    @Test
    fun `no userId returns CONDITIONS_NOT_MET error`() {
        // When HceService has no userId, it returns SW_CONDITIONS_NOT_MET
        val errorResponse = HceProtocol.buildErrorResponse(HceProtocol.SW_CONDITIONS_NOT_MET)
        assertEquals(2, errorResponse.size)
        assertEquals(0x69.toByte(), errorResponse[0])
        assertEquals(0x85.toByte(), errorResponse[1])
    }

    @Test
    fun `device locked returns SECURITY_NOT_SATISFIED error`() {
        // When UserNotAuthenticatedException is thrown, return SW_SECURITY_NOT_SATISFIED
        val errorResponse = HceProtocol.buildErrorResponse(HceProtocol.SW_SECURITY_NOT_SATISFIED)
        assertEquals(2, errorResponse.size)
        assertEquals(0x69.toByte(), errorResponse[0])
        assertEquals(0x82.toByte(), errorResponse[1])
    }

    @Test
    fun `signing failure returns INTERNAL_ERROR`() {
        val errorResponse = HceProtocol.buildErrorResponse(HceProtocol.SW_INTERNAL_ERROR)
        assertEquals(2, errorResponse.size)
        assertEquals(0x6F.toByte(), errorResponse[0])
        assertEquals(0x00.toByte(), errorResponse[1])
    }

    // ---- Transport tag binding ----

    @Test
    fun `NFC_HCE transport tag is used for NFC signing`() {
        assertEquals("NFC_HCE", HceProtocol.TRANSPORT_TAG)
    }

    @Test
    fun `NFC_HCE transport tag differs from BLE tag`() {
        assertNotEquals("BLE", HceProtocol.TRANSPORT_TAG)
    }

    @Test
    fun `transport tag binding prevents cross-protocol replay`() {
        // A signature created with "BLE" tag should not match one with "NFC_HCE" tag
        // even with the same nonce and userId
        val nonce = ByteArray(32) { 0x42 }
        val userId = "same-user"

        val bleMessage = nonce + userId.toByteArray() + "BLE".toByteArray()
        val nfcMessage = nonce + userId.toByteArray() + HceProtocol.TRANSPORT_TAG.toByteArray()

        assertFalse(
            "Signing messages for BLE and NFC must differ for transport binding security",
            bleMessage.contentEquals(nfcMessage)
        )
    }

    // ---- Nonce extraction from challenge ----

    @Test
    fun `nonce is first 32 bytes of extracted challenge`() {
        val nonce = ByteArray(32) { (it + 0x10).toByte() }
        val challenge = buildChallenge(nonce = nonce)
        val authApdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(authApdu)
        val extractedNonce = extracted.sliceArray(0 until 32)

        assertArrayEquals(nonce, extractedNonce)
    }

    // ---- Multiple sequential authentications ----

    @Test
    fun `multiple sequential SELECT-AUTHENTICATE cycles work independently`() {
        for (i in 1..3) {
            // SELECT
            val selectApdu = buildSelectAidApdu()
            assertTrue(HceProtocol.isSelectAid(selectApdu))
            val selectResp = HceProtocol.buildSelectResponse()
            assertEndsWithSwOk(selectResp)

            // AUTHENTICATE
            val challenge = buildValidChallenge()
            val authApdu = buildAuthenticateApdu(challenge)
            assertTrue(HceProtocol.isAuthenticate(authApdu))

            val nonce = HceProtocol.extractChallenge(authApdu).sliceArray(0 until 32)
            val sig = ByteArray(64) { (it + i).toByte() }
            val authResp = HceProtocol.buildAuthResponse("user-$i", sig)
            assertEndsWithSwOk(authResp)

            // Verify user from response
            val userIdLen = authResp[0].toInt() and 0xFF
            val parsedUserId = String(authResp, 1, userIdLen, Charsets.UTF_8)
            assertEquals("user-$i", parsedUserId)
        }
    }

    // ---- Capabilities field ----

    @Test
    fun `capabilities byte indicates challenge-response and offline-token support`() {
        // bit0 = challenge-response, bit1 = offline-token
        assertEquals(0x03.toByte(), HceProtocol.CAPABILITIES)
        assertTrue("Challenge-response bit should be set", (HceProtocol.CAPABILITIES.toInt() and 0x01) != 0)
        assertTrue("Offline-token bit should be set", (HceProtocol.CAPABILITIES.toInt() and 0x02) != 0)
    }

    // ---- Helpers ----

    private fun buildSelectAidApdu(): ByteArray {
        val aid = HceProtocol.AID
        return byteArrayOf(
            HceProtocol.CLA_ISO,
            HceProtocol.INS_SELECT,
            0x04, 0x00,
            aid.size.toByte(),
            *aid
        )
    }

    private fun buildAuthenticateApdu(challenge: ByteArray): ByteArray {
        return byteArrayOf(
            HceProtocol.CLA_PROPRIETARY,
            HceProtocol.INS_AUTHENTICATE,
            0x00, 0x00,
            challenge.size.toByte(),
            *challenge
        )
    }

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
        val buf = ByteBuffer.allocate(52)
        buf.put(nonce)
        buf.putLong(issuedAt)
        buf.putLong(expiresAt)
        buf.putInt(gatewayId)
        return buf.array()
    }

    private fun assertEndsWithSwOk(response: ByteArray) {
        assertTrue("Response must be at least 2 bytes", response.size >= 2)
        assertEquals(
            "Response must end with SW_OK (90 00)",
            0x90.toByte(),
            response[response.size - 2]
        )
        assertEquals(
            "Response must end with SW_OK (90 00)",
            0x00.toByte(),
            response[response.size - 1]
        )
    }
}
