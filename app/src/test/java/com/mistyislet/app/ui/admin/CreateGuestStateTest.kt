package com.mistyislet.app.ui.admin

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.domain.model.GuestVisit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class CreateGuestStateTest {

    @Test
    fun `success maps to Success`() {
        assertEquals(
            CreateGuestState.Success,
            createGuestStateOf(ApiResult.Success(GuestVisit(id = "guest-1"))),
        )
    }

    @Test
    fun `error maps to Error carrying backend message`() {
        assertEquals(
            CreateGuestState.Error("door door-9 does not belong to place place-1"),
            createGuestStateOf(ApiResult.Error(400, "door door-9 does not belong to place place-1")),
        )
    }

    @Test
    fun `exception maps to Error with throwable message`() {
        assertEquals(
            CreateGuestState.Error("no network"),
            createGuestStateOf(ApiResult.Exception(IOException("no network"))),
        )
    }

    @Test
    fun `exception without message maps to fallback text`() {
        assertEquals(
            CreateGuestState.Error("Unknown error"),
            createGuestStateOf(ApiResult.Exception(IOException())),
        )
    }
}
