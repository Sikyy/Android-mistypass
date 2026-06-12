package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.CreateGuestRequest
import com.mistyislet.app.domain.model.ShareAccessRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

class AdminApiEndpointTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AdminApi

    @Before
    fun setUp() {
        server = MockWebServer()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AdminApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `user endpoints use place scoped admin paths`() = runTest {
        server.enqueueJson("""{"items":[{"id":"user-1","name":"Ada","email":"ada@example.com","role":"door_access"}]}""")
        server.enqueueJson("""{"id":"user-1","name":"Ada","email":"ada@example.com","role":"door_access"}""")
        server.enqueueJson("""{"items":[{"id":"login-1","device_name":"Pixel","platform":"android","last_active":"2026-05-24T01:00:00Z"}]}""")
        server.enqueueJson("""{"items":[{"id":"right-1","team_name":"Ops","door_name":"Lobby","schedule_name":"Always"}]}""")
        server.enqueueJson("""{"id":"right-2","team_name":"Ops","door_name":"Lab","schedule_name":"Always"}""")

        api.listUsers("place-1")
        api.getUser("place-1", "user-1")
        api.listUserLogins("place-1", "user-1")
        api.listUserAccessRights("place-1", "user-1")
        api.shareUserAccess("place-1", "user-1", ShareAccessRequest(doorIds = listOf("door-1")))

        assertEquals("/api/v1/app/places/place-1/users", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/users/user-1", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/users/user-1/logins", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/users/user-1/access-rights", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/users/user-1/share-access", server.takeRequest().path)
    }

    @Test
    fun `share access sends door_ids in request body`() = runTest {
        server.enqueueJson("""{"user_id":"user-1","url":"https://share","token":"tok","expires_at":"2026-05-24T02:00:00Z"}""")

        api.shareUserAccess("place-1", "user-1", ShareAccessRequest(doorIds = listOf("door-1", "door-2")))

        val recorded = server.takeRequest()
        assertEquals("/api/v1/app/places/place-1/users/user-1/share-access", recorded.path)
        val body = recorded.body.readUtf8()
        // Backend requires the snake_case key `door_ids`; an empty body (the old bug) regresses this.
        assertTrue("body should contain door_ids, was: $body", body.contains("\"door_ids\""))
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(ShareAccessRequest.serializer(), body)
        assertEquals(listOf("door-1", "door-2"), parsed.doorIds)
    }

    @Test
    fun `create guest sends door_ids in request body`() = runTest {
        server.enqueueJson("""{"id":"guest-1","name":"Tamu","status":"expected"}""")

        api.createGuest(
            "place-1",
            CreateGuestRequest(name = "Tamu", doorIds = listOf("door-1", "door-2")),
        )

        val recorded = server.takeRequest()
        assertEquals("/api/v1/app/places/place-1/guests", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue("body should contain door_ids, was: $body", body.contains("\"door_ids\""))
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(CreateGuestRequest.serializer(), body)
        assertEquals(listOf("door-1", "door-2"), parsed.doorIds)
    }

    @Test
    fun `create guest omits door_ids when none selected`() = runTest {
        server.enqueueJson("""{"id":"guest-2","name":"Tamu","status":"expected"}""")

        api.createGuest("place-1", CreateGuestRequest(name = "Tamu"))

        val body = server.takeRequest().body.readUtf8()
        // encodeDefaults=false：默认空列表整键省略 = 与改动前的请求体一致（兼容）
        assertEquals(false, body.contains("door_ids"))
    }

    @Test
    fun `zone and camera deep admin endpoints use expected paths`() = runTest {
        server.enqueueJson("""{"id":"zone-1","name":"HQ","description":"Main","door_count":2}""")
        server.enqueueJson("""{"items":[{"id":"region-1","name":"Indonesia","country_code":"ID","holiday_count":1}]}""")
        server.enqueueJson("""{"items":[{"id":"holiday-1","region_id":"region-1","name":"Nyepi","date":"2026-03-19"}]}""")
        server.enqueueJson("""{"token":"cloud-token","expires_at":"2026-05-24T02:00:00Z"}""")
        server.enqueueJson("""{"items":[{"id":"rec-1","camera_id":"camera-1","started_at":"2026-05-24T01:00:00Z","duration_seconds":30}]}""")

        api.getZone("place-1", "zone-1")
        api.listHolidayRegions("place-1")
        api.listHolidays("place-1", "region-1")
        api.getCameraCloudToken("camera-1")
        api.listCameraRecordings("camera-1")

        assertEquals("/api/v1/app/places/place-1/zones/zone-1", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/holiday-regions", server.takeRequest().path)
        assertEquals("/api/v1/app/places/place-1/holiday-regions/region-1/holidays", server.takeRequest().path)
        assertEquals("/api/v1/app/cameras/camera-1/cloud-token", server.takeRequest().path)
        assertEquals("/api/v1/app/cameras/camera-1/cloud-recordings", server.takeRequest().path)
    }

    private fun MockWebServer.enqueueJson(body: String) {
        enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }
}
