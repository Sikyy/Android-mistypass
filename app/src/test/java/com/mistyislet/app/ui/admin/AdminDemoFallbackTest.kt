package com.mistyislet.app.ui.admin

import com.mistyislet.app.core.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AdminDemoFallbackTest {

    private val allow = AdminDemoFallback(allowDemoData = true)
    private val deny = AdminDemoFallback(allowDemoData = false)

    // --- demoOrNull: the single policy gate ---

    @Test
    fun `demoOrNull returns demo when demo data is allowed`() {
        assertEquals("demo", allow.demoOrNull { "demo" })
    }

    @Test
    fun `demoOrNull returns null when demo data is not allowed`() {
        assertNull(deny.demoOrNull { "demo" })
    }

    @Test
    fun `demoOrNull does not evaluate demo when disallowed`() {
        var evaluated = false
        deny.demoOrNull { evaluated = true; "demo" }
        assertEquals(false, evaluated)
    }

    // --- resolveList: success always wins, failure depends on the gate ---

    @Test
    fun `resolveList keeps server data on non-empty success regardless of policy`() {
        val data = listOf("a", "b")
        assertEquals(
            AdminListResult(data, null),
            deny.resolveList(ApiResult.Success(data)) { listOf("demo") },
        )
    }

    @Test
    fun `resolveList shows demo for empty success only when allowed`() {
        assertEquals(
            AdminListResult(listOf("demo"), null),
            allow.resolveList(ApiResult.Success(emptyList())) { listOf("demo") },
        )
    }

    @Test
    fun `resolveList keeps empty success empty without error when demo disallowed`() {
        assertEquals(
            AdminListResult(emptyList<String>(), null),
            deny.resolveList(ApiResult.Success(emptyList())) { listOf("demo") },
        )
    }

    @Test
    fun `resolveList falls back to demo on error in mock builds`() {
        assertEquals(
            AdminListResult(listOf("demo"), null),
            allow.resolveList(ApiResult.Error(500, "boom")) { listOf("demo") },
        )
    }

    @Test
    fun `resolveList surfaces error message on error in real builds`() {
        assertEquals(
            AdminListResult(emptyList<String>(), "boom"),
            deny.resolveList(ApiResult.Error(500, "boom")) { listOf("demo") },
        )
    }

    @Test
    fun `resolveList surfaces a non-null error on exception in real builds`() {
        val result = deny.resolveList(ApiResult.Exception(IOException("no network"))) { listOf("demo") }
        assertTrue(result.items.isEmpty())
        assertNotNull(result.error)
    }

    @Test
    fun `resolveList falls back to demo on exception in mock builds`() {
        assertEquals(
            AdminListResult(listOf("demo"), null),
            allow.resolveList(ApiResult.Exception(IOException("no network"))) { listOf("demo") },
        )
    }
}
