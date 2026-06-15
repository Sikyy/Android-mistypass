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
class AdminEventsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeAdminRepository, allowDemo: Boolean) =
        AdminEventsViewModel(repo, fakeSelectedPlaceRepository(), AdminDemoFallback(allowDemoData = allowDemo))

    @Test
    fun `staging shows error and no demo events on server error`() = runTest {
        val repo = FakeAdminRepository().apply { eventsResult = ApiResult.Error(500, "boom") }
        val vm = viewModel(repo, allowDemo = false)
        advanceUntilIdle()

        assertTrue("expected no events", vm.items.value.isEmpty())
        assertEquals("boom", vm.error.value)
    }

    @Test
    fun `staging shows error and no demo events on network exception`() = runTest {
        val repo = FakeAdminRepository().apply { eventsResult = ApiResult.Exception(IOException("offline")) }
        val vm = viewModel(repo, allowDemo = false)
        advanceUntilIdle()

        assertTrue("expected no events", vm.items.value.isEmpty())
        assertNotNull("expected an error message", vm.error.value)
    }

    @Test
    fun `mock build still falls back to demo events on error`() = runTest {
        val repo = FakeAdminRepository().apply { eventsResult = ApiResult.Error(500, "boom") }
        val vm = viewModel(repo, allowDemo = true)
        advanceUntilIdle()

        assertEquals(AdminDemoData.events, vm.items.value)
        assertNull(vm.error.value)
    }
}
