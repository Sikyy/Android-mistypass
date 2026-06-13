package com.mistyislet.app.ui.admin

import com.mistyislet.app.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminGatewaysViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakePlaceRepository, allowDemo: Boolean) =
        AdminGatewaysViewModel(
            repo,
            FakeAdminRepository(),
            fakeSelectedPlaceRepository(),
            AdminDemoFallback(allowDemoData = allowDemo),
        )

    @Test
    fun `staging shows error and no demo gateways on server error`() = runTest {
        val repo = FakePlaceRepository().apply { doorsResult = ApiResult.Error(500, "boom") }
        val vm = viewModel(repo, allowDemo = false)
        advanceUntilIdle()

        assertTrue("expected no gateways", vm.items.value.isEmpty())
        assertEquals("boom", vm.error.value)
    }

    @Test
    fun `staging shows error and no demo gateways on network exception`() = runTest {
        val repo = FakePlaceRepository().apply { doorsResult = ApiResult.Exception(IOException("offline")) }
        val vm = viewModel(repo, allowDemo = false)
        advanceUntilIdle()

        assertTrue("expected no gateways", vm.items.value.isEmpty())
        assertNotNull("expected an error message", vm.error.value)
    }

    @Test
    fun `mock build still falls back to demo gateways on error`() = runTest {
        val repo = FakePlaceRepository().apply { doorsResult = ApiResult.Error(500, "boom") }
        val vm = viewModel(repo, allowDemo = true)
        advanceUntilIdle()

        // The 5 demo doors group into 3 distinct gateways (gw-001, gw-002, gw-003).
        assertEquals(3, vm.items.value.size)
        assertNull(vm.error.value)
    }
}
