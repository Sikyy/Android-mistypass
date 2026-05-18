package com.mistyislet.app.domain.usecase

import com.mistyislet.app.core.ble.BLEAuthClient
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the BLE unlock state machine logic.
 *
 * BLEUnlockUseCase depends on final Android classes (BLEAuthClient, MobileCredentialRepository)
 * that can't be mocked in JVM tests without MockK/Mockito. Instead, we test the
 * result mapping logic that the use case implements: given an AuthResult from the
 * BLE client, what UnlockResult should the UI receive?
 *
 * This also tests the AuthResult sealed class hierarchy itself.
 */
class BLEUnlockUseCaseTest {

    // ---- AuthResult -> UnlockResult mapping ----

    @Test
    fun `Granted auth result maps to Success`() {
        val authResult = BLEAuthClient.AuthResult.Granted("Door unlocked")
        val unlockResult = mapAuthResultToUnlockResult(authResult)
        assertTrue(unlockResult is BLEUnlockUseCase.UnlockResult.Success)
    }

    @Test
    fun `Denied auth result maps to Denied with reason`() {
        val authResult = BLEAuthClient.AuthResult.Denied(0x02, "Schedule restriction")
        val unlockResult = mapAuthResultToUnlockResult(authResult)

        assertTrue(unlockResult is BLEUnlockUseCase.UnlockResult.Denied)
        assertEquals("Schedule restriction", (unlockResult as BLEUnlockUseCase.UnlockResult.Denied).reason)
    }

    @Test
    fun `Error auth result maps to Failed with message`() {
        val authResult = BLEAuthClient.AuthResult.Error("Connection timeout")
        val unlockResult = mapAuthResultToUnlockResult(authResult)

        assertTrue(unlockResult is BLEUnlockUseCase.UnlockResult.Failed)
        assertEquals("Connection timeout", (unlockResult as BLEUnlockUseCase.UnlockResult.Failed).message)
    }

    // ---- AuthResult sealed class ----

    @Test
    fun `Granted contains reason string`() {
        val result = BLEAuthClient.AuthResult.Granted("Welcome, door Main-Entrance opened")
        assertEquals("Welcome, door Main-Entrance opened", result.reason)
    }

    @Test
    fun `Granted with empty reason`() {
        val result = BLEAuthClient.AuthResult.Granted("")
        assertEquals("", result.reason)
    }

    @Test
    fun `Denied contains code and reason`() {
        val result = BLEAuthClient.AuthResult.Denied(0x05, "Credential expired")
        assertEquals(0x05.toByte(), result.code)
        assertEquals("Credential expired", result.reason)
    }

    @Test
    fun `Error contains message`() {
        val result = BLEAuthClient.AuthResult.Error("BLE error: GATT 133")
        assertEquals("BLE error: GATT 133", result.message)
    }

    // ---- UnlockResult sealed class ----

    @Test
    fun `UnlockResult Success is a singleton object`() {
        val a = BLEUnlockUseCase.UnlockResult.Success
        val b = BLEUnlockUseCase.UnlockResult.Success
        assertSame(a, b)
    }

    @Test
    fun `UnlockResult NoCredential is a singleton object`() {
        val a = BLEUnlockUseCase.UnlockResult.NoCredential
        val b = BLEUnlockUseCase.UnlockResult.NoCredential
        assertSame(a, b)
    }

    @Test
    fun `UnlockResult Denied carries reason`() {
        val result = BLEUnlockUseCase.UnlockResult.Denied("Access denied: not in schedule")
        assertEquals("Access denied: not in schedule", result.reason)
    }

    @Test
    fun `UnlockResult Failed carries message`() {
        val result = BLEUnlockUseCase.UnlockResult.Failed("No BLE reader found nearby")
        assertEquals("No BLE reader found nearby", result.message)
    }

    // ---- Exhaustive when matching (compile-time check via runtime test) ----

    @Test
    fun `all AuthResult variants can be pattern-matched`() {
        val results = listOf(
            BLEAuthClient.AuthResult.Granted("ok"),
            BLEAuthClient.AuthResult.Denied(0x02, "no"),
            BLEAuthClient.AuthResult.Error("err"),
        )

        for (result in results) {
            val mapped = mapAuthResultToUnlockResult(result)
            assertNotNull(mapped)
        }
    }

    @Test
    fun `all UnlockResult variants can be pattern-matched`() {
        val results = listOf(
            BLEUnlockUseCase.UnlockResult.Success,
            BLEUnlockUseCase.UnlockResult.NoCredential,
            BLEUnlockUseCase.UnlockResult.Denied("reason"),
            BLEUnlockUseCase.UnlockResult.Failed("message"),
        )

        for (result in results) {
            val description = when (result) {
                is BLEUnlockUseCase.UnlockResult.Success -> "success"
                is BLEUnlockUseCase.UnlockResult.NoCredential -> "no_credential"
                is BLEUnlockUseCase.UnlockResult.Denied -> "denied: ${result.reason}"
                is BLEUnlockUseCase.UnlockResult.Failed -> "failed: ${result.message}"
            }
            assertNotNull(description)
            assertTrue(description.isNotEmpty())
        }
    }

    // ---- Edge cases ----

    @Test
    fun `Denied with zero code byte`() {
        val result = BLEAuthClient.AuthResult.Denied(0x00, "Unknown")
        assertEquals(0x00.toByte(), result.code)
    }

    @Test
    fun `Error with empty message`() {
        val result = BLEAuthClient.AuthResult.Error("")
        assertEquals("", result.message)
    }

    @Test
    fun `Failed with user-facing message from API`() {
        val result = BLEUnlockUseCase.UnlockResult.Failed("Cannot get user info: HTTP 401")
        assertTrue(result.message.contains("Cannot get user info"))
    }

    // ---- Helper: replicates the when-mapping logic from BLEUnlockUseCase.unlock() ----

    /**
     * Replicates the mapping that BLEUnlockUseCase.unlock() performs
     * when converting BLEAuthClient.AuthResult to UnlockResult.
     */
    private fun mapAuthResultToUnlockResult(
        authResult: BLEAuthClient.AuthResult,
    ): BLEUnlockUseCase.UnlockResult = when (authResult) {
        is BLEAuthClient.AuthResult.Granted -> BLEUnlockUseCase.UnlockResult.Success
        is BLEAuthClient.AuthResult.Denied -> BLEUnlockUseCase.UnlockResult.Denied(authResult.reason)
        is BLEAuthClient.AuthResult.Error -> BLEUnlockUseCase.UnlockResult.Failed(authResult.message)
    }
}
