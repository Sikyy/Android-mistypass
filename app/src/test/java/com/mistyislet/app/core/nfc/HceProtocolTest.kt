package com.mistyislet.app.core.nfc

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the NFC HCE APDU protocol layer.
 *
 * HceProtocol is pure logic (no Android deps) that handles:
 * - SELECT AID command detection and response building
 * - AUTHENTICATE command detection and challenge extraction
 * - Auth response encoding (userId length prefix + userId + signature + SW_OK)
 * - Error response encoding
 */
class HceProtocolTest {

    // ---- SELECT AID detection ----

    @Test
    fun `isSelectAid returns true for valid SELECT command with correct AID`() {
        val apdu = buildSelectAidApdu(HceProtocol.AID)
        assertTrue(HceProtocol.isSelectAid(apdu))
    }

    @Test
    fun `isSelectAid returns false for wrong AID`() {
        val wrongAid = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val apdu = buildSelectAidApdu(wrongAid)
        assertFalse(HceProtocol.isSelectAid(apdu))
    }

    @Test
    fun `isSelectAid returns false for truncated APDU`() {
        // Too short to contain header + AID
        val apdu = byteArrayOf(0x00, HceProtocol.INS_SELECT, 0x04, 0x00)
        assertFalse(HceProtocol.isSelectAid(apdu))
    }

    @Test
    fun `isSelectAid returns false when CLA is not ISO`() {
        val aid = HceProtocol.AID
        val apdu = byteArrayOf(
            HceProtocol.CLA_PROPRIETARY, // wrong CLA
            HceProtocol.INS_SELECT,
            0x04, 0x00,
            aid.size.toByte(),
            *aid
        )
        assertFalse(HceProtocol.isSelectAid(apdu))
    }

    @Test
    fun `isSelectAid returns false when INS is not SELECT`() {
        val aid = HceProtocol.AID
        val apdu = byteArrayOf(
            HceProtocol.CLA_ISO,
            0xB0.toByte(), // wrong INS (READ_BINARY)
            0x04, 0x00,
            aid.size.toByte(),
            *aid
        )
        assertFalse(HceProtocol.isSelectAid(apdu))
    }

    @Test
    fun `isSelectAid returns false when P1 is not 04`() {
        val aid = HceProtocol.AID
        val apdu = byteArrayOf(
            HceProtocol.CLA_ISO,
            HceProtocol.INS_SELECT,
            0x00, // wrong P1 (should be 0x04 for select by name)
            0x00,
            aid.size.toByte(),
            *aid
        )
        assertFalse(HceProtocol.isSelectAid(apdu))
    }

    // ---- AUTHENTICATE detection ----

    @Test
    fun `isAuthenticate returns true for valid AUTHENTICATE with 52-byte challenge`() {
        val challenge = ByteArray(HceProtocol.CHALLENGE_V2_SIZE) { it.toByte() }
        val apdu = buildAuthenticateApdu(challenge)
        assertTrue(HceProtocol.isAuthenticate(apdu))
    }

    @Test
    fun `isAuthenticate returns false for short payload`() {
        val shortChallenge = ByteArray(20)
        val apdu = byteArrayOf(
            HceProtocol.CLA_PROPRIETARY,
            HceProtocol.INS_AUTHENTICATE,
            0x00, 0x00,
            shortChallenge.size.toByte(),
            *shortChallenge
        )
        assertFalse(HceProtocol.isAuthenticate(apdu))
    }

    @Test
    fun `isAuthenticate returns false when CLA is ISO instead of proprietary`() {
        val challenge = ByteArray(HceProtocol.CHALLENGE_V2_SIZE)
        val apdu = byteArrayOf(
            HceProtocol.CLA_ISO, // wrong CLA
            HceProtocol.INS_AUTHENTICATE,
            0x00, 0x00,
            challenge.size.toByte(),
            *challenge
        )
        assertFalse(HceProtocol.isAuthenticate(apdu))
    }

    @Test
    fun `isAuthenticate returns false when INS is not AUTHENTICATE`() {
        val challenge = ByteArray(HceProtocol.CHALLENGE_V2_SIZE)
        val apdu = byteArrayOf(
            HceProtocol.CLA_PROPRIETARY,
            0xCA.toByte(), // wrong INS
            0x00, 0x00,
            challenge.size.toByte(),
            *challenge
        )
        assertFalse(HceProtocol.isAuthenticate(apdu))
    }

    // ---- Challenge extraction ----

    @Test
    fun `extractChallenge returns 52 bytes starting at offset 5`() {
        val challenge = ByteArray(HceProtocol.CHALLENGE_V2_SIZE) { (it + 0x10).toByte() }
        val apdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(apdu)

        assertEquals(HceProtocol.CHALLENGE_V2_SIZE, extracted.size)
        assertArrayEquals(challenge, extracted)
    }

    @Test
    fun `extractChallenge preserves nonce bytes in first 32 positions`() {
        val nonce = ByteArray(32) { 0xAB.toByte() }
        val timestamps = ByteArray(16) { 0xCD.toByte() }
        val gatewayId = ByteArray(4) { 0xEF.toByte() }
        val challenge = nonce + timestamps + gatewayId
        val apdu = buildAuthenticateApdu(challenge)

        val extracted = HceProtocol.extractChallenge(apdu)
        val extractedNonce = extracted.sliceArray(0 until 32)

        assertArrayEquals(nonce, extractedNonce)
    }

    // ---- SELECT response building ----

    @Test
    fun `buildSelectResponse includes protocol version and SW_OK`() {
        val response = HceProtocol.buildSelectResponse()

        // Format: [version][0x01][capabilities][SW_OK (90 00)]
        assertEquals(5, response.size)
        assertEquals(HceProtocol.PROTOCOL_VERSION, response[0])
        assertEquals(0x01.toByte(), response[1])
        assertEquals(HceProtocol.CAPABILITIES, response[2])
        // Last 2 bytes = SW_OK
        assertEquals(0x90.toByte(), response[3])
        assertEquals(0x00.toByte(), response[4])
    }

    // ---- Auth response building ----

    @Test
    fun `buildAuthResponse encodes userId length prefix correctly`() {
        val userId = "user-123"
        val signature = ByteArray(64) { 0xFF.toByte() }

        val response = HceProtocol.buildAuthResponse(userId, signature)

        // Format: [1B userIdLen][userId bytes][signature bytes][SW_OK]
        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        val expectedSize = 1 + userIdBytes.size + signature.size + 2 // +2 for SW_OK
        assertEquals(expectedSize, response.size)

        // First byte = userId length
        assertEquals(userIdBytes.size.toByte(), response[0])

        // userId content
        val extractedUserId = response.sliceArray(1 until 1 + userIdBytes.size)
        assertArrayEquals(userIdBytes, extractedUserId)

        // signature content
        val extractedSig = response.sliceArray(1 + userIdBytes.size until 1 + userIdBytes.size + signature.size)
        assertArrayEquals(signature, extractedSig)

        // SW_OK trailer
        assertEquals(0x90.toByte(), response[response.size - 2])
        assertEquals(0x00.toByte(), response[response.size - 1])
    }

    @Test
    fun `buildAuthResponse handles empty userId`() {
        val userId = ""
        val signature = ByteArray(72) { 0xAA.toByte() }

        val response = HceProtocol.buildAuthResponse(userId, signature)

        assertEquals(0.toByte(), response[0]) // length prefix = 0
        // Signature starts at offset 1
        val extractedSig = response.sliceArray(1 until 1 + signature.size)
        assertArrayEquals(signature, extractedSig)
    }

    @Test
    fun `buildAuthResponse handles long userId up to 180 bytes`() {
        val userId = "a".repeat(180)
        val signature = ByteArray(70)

        val response = HceProtocol.buildAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        assertEquals(userIdBytes.size.toByte(), response[0])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildAuthResponse rejects userId exceeding 180 bytes`() {
        val userId = "a".repeat(181)
        val signature = ByteArray(70)
        HceProtocol.buildAuthResponse(userId, signature)
    }

    @Test
    fun `buildAuthResponse handles multi-byte UTF-8 userId`() {
        // Chinese characters are 3 bytes each in UTF-8
        val userId = "张三" // 2 chars, 6 bytes
        val signature = ByteArray(64)

        val response = HceProtocol.buildAuthResponse(userId, signature)

        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        assertEquals(6, userIdBytes.size)
        assertEquals(6.toByte(), response[0])

        val extractedBytes = response.sliceArray(1 until 1 + 6)
        assertEquals(userId, String(extractedBytes, Charsets.UTF_8))
    }

    // ---- Error response building ----

    @Test
    fun `buildErrorResponse returns status word bytes unchanged`() {
        val sw = HceProtocol.SW_SECURITY_NOT_SATISFIED
        val response = HceProtocol.buildErrorResponse(sw)
        assertArrayEquals(sw, response)
    }

    @Test
    fun `buildErrorResponse for conditions not met`() {
        val response = HceProtocol.buildErrorResponse(HceProtocol.SW_CONDITIONS_NOT_MET)
        assertEquals(0x69.toByte(), response[0])
        assertEquals(0x85.toByte(), response[1])
    }

    @Test
    fun `buildErrorResponse for internal error`() {
        val response = HceProtocol.buildErrorResponse(HceProtocol.SW_INTERNAL_ERROR)
        assertEquals(0x6F.toByte(), response[0])
        assertEquals(0x00.toByte(), response[1])
    }

    // ---- Round-trip: encode then parse back ----

    @Test
    fun `SELECT AID round trip - built APDU is recognized by isSelectAid`() {
        val apdu = buildSelectAidApdu(HceProtocol.AID)
        assertTrue(HceProtocol.isSelectAid(apdu))
        assertFalse(HceProtocol.isAuthenticate(apdu))
    }

    @Test
    fun `AUTHENTICATE round trip - built APDU is recognized and challenge is extractable`() {
        val originalChallenge = ByteArray(52) { (it * 3).toByte() }
        val apdu = buildAuthenticateApdu(originalChallenge)

        assertTrue(HceProtocol.isAuthenticate(apdu))
        assertFalse(HceProtocol.isSelectAid(apdu))

        val extracted = HceProtocol.extractChallenge(apdu)
        assertArrayEquals(originalChallenge, extracted)
    }

    @Test
    fun `unknown command is neither SELECT nor AUTHENTICATE`() {
        val unknownApdu = byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x00, 0x00)
        assertFalse(HceProtocol.isSelectAid(unknownApdu))
        assertFalse(HceProtocol.isAuthenticate(unknownApdu))
    }

    // ---- Constants validation ----

    @Test
    fun `AID is 8 bytes`() {
        assertEquals(8, HceProtocol.AID.size)
    }

    @Test
    fun `CHALLENGE_V2_SIZE is 52 matching protocol spec`() {
        // 32B nonce + 8B issued_at + 8B expires_at + 4B gateway_id = 52
        assertEquals(52, HceProtocol.CHALLENGE_V2_SIZE)
    }

    @Test
    fun `TRANSPORT_TAG is NFC_HCE`() {
        assertEquals("NFC_HCE", HceProtocol.TRANSPORT_TAG)
    }

    @Test
    fun `status words are 2 bytes each`() {
        assertEquals(2, HceProtocol.SW_OK.size)
        assertEquals(2, HceProtocol.SW_SECURITY_NOT_SATISFIED.size)
        assertEquals(2, HceProtocol.SW_CONDITIONS_NOT_MET.size)
        assertEquals(2, HceProtocol.SW_APP_NOT_FOUND.size)
        assertEquals(2, HceProtocol.SW_INS_NOT_SUPPORTED.size)
        assertEquals(2, HceProtocol.SW_INTERNAL_ERROR.size)
    }

    // ---- Helpers ----

    /** Builds a valid SELECT AID APDU: [CLA=00][INS=A4][P1=04][P2=00][Lc=len][AID] */
    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        return byteArrayOf(
            HceProtocol.CLA_ISO,
            HceProtocol.INS_SELECT,
            0x04, 0x00,
            aid.size.toByte(),
            *aid
        )
    }

    /** Builds a valid AUTHENTICATE APDU: [CLA=80][INS=88][P1=00][P2=00][Lc=34][challenge] */
    private fun buildAuthenticateApdu(challenge: ByteArray): ByteArray {
        return byteArrayOf(
            HceProtocol.CLA_PROPRIETARY,
            HceProtocol.INS_AUTHENTICATE,
            0x00, 0x00,
            challenge.size.toByte(),
            *challenge
        )
    }
}
