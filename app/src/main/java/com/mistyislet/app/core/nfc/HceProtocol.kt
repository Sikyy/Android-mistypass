package com.mistyislet.app.core.nfc

/**
 * NFC HCE APDU protocol constants and parsing utilities.
 */
object HceProtocol {

    // AID: F0 4D 49 53 54 59 01 00 (8 bytes)
    val AID = byteArrayOf(
        0xF0.toByte(), 0x4D, 0x49, 0x53, 0x54, 0x59, 0x01, 0x00
    )

    // APDU CLA/INS constants
    const val CLA_ISO: Byte = 0x00
    const val CLA_PROPRIETARY: Byte = 0x80.toByte()
    const val INS_SELECT: Byte = 0xA4.toByte()
    const val INS_AUTHENTICATE: Byte = 0x88.toByte()

    // Status words
    val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
    val SW_SECURITY_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())
    val SW_CONDITIONS_NOT_MET = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val SW_APP_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00)
    val SW_INTERNAL_ERROR = byteArrayOf(0x6F.toByte(), 0x00)

    const val PROTOCOL_VERSION: Byte = 0x02
    const val CAPABILITIES: Byte = 0x03 // bit0=challenge-response, bit1=offline-token

    const val CHALLENGE_V2_SIZE = 52
    const val TRANSPORT_TAG = "NFC_HCE"

    fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + AID.size) return false
        return apdu[0] == CLA_ISO &&
               apdu[1] == INS_SELECT &&
               apdu[2] == 0x04.toByte() &&
               apdu[3] == 0x00.toByte() &&
               apdu[4] == AID.size.toByte() &&
               apdu.sliceArray(5 until 5 + AID.size).contentEquals(AID)
    }

    fun isAuthenticate(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + CHALLENGE_V2_SIZE) return false
        return apdu[0] == CLA_PROPRIETARY &&
               apdu[1] == INS_AUTHENTICATE &&
               apdu[4] == CHALLENGE_V2_SIZE.toByte()
    }

    fun extractChallenge(apdu: ByteArray): ByteArray {
        return apdu.sliceArray(5 until 5 + CHALLENGE_V2_SIZE)
    }

    fun buildSelectResponse(): ByteArray {
        return byteArrayOf(PROTOCOL_VERSION, 0x01, CAPABILITIES) + SW_OK
    }

    fun buildAuthResponse(userId: String, signature: ByteArray): ByteArray {
        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        require(userIdBytes.size <= 180) { "userId too long: ${userIdBytes.size} bytes, max 180" }
        val response = ByteArray(1 + userIdBytes.size + signature.size)
        response[0] = userIdBytes.size.toByte()
        userIdBytes.copyInto(response, 1)
        signature.copyInto(response, 1 + userIdBytes.size)
        return response + SW_OK
    }

    fun buildErrorResponse(sw: ByteArray): ByteArray = sw
}
