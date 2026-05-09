package com.mistyislet.app.core.network

import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.RefreshRequest
import com.mistyislet.app.domain.model.RefreshResponse
import com.mistyislet.app.domain.model.RestorePasswordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private var refreshCallCount = AtomicInteger(0)

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenStore = FakeTokenStore()
        refreshCallCount.set(0)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun buildClient(fakeAuthApi: AuthApi): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore) { fakeAuthApi })
            .build()
    }

    @Test
    fun `attaches bearer token to request`() {
        tokenStore.accessToken = "valid-token"
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = buildClient(noopAuthApi())
        val response = client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertEquals(200, response.code)
        val recorded = server.takeRequest()
        assertEquals("Bearer valid-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `no token means no auth header`() {
        tokenStore.accessToken = null
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = buildClient(noopAuthApi())
        val response = client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertEquals(200, response.code)
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `refreshes token on 401 and retries`() {
        tokenStore.accessToken = "expired-token"
        tokenStore.refreshToken = "valid-refresh"
        tokenStore._expiresAt = 0L

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("success"))

        val fakeAuth = object : AuthApi {
            override suspend fun restorePassword(request: RestorePasswordRequest) = Unit
            override suspend fun login(request: com.mistyislet.app.domain.model.LoginRequest) =
                throw UnsupportedOperationException()
            override suspend fun refresh(request: RefreshRequest): RefreshResponse {
                refreshCallCount.incrementAndGet()
                return RefreshResponse(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    expiresIn = 3600,
                )
            }
        }

        val client = buildClient(fakeAuth)
        val response = client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals(1, refreshCallCount.get())
        assertEquals("new-access", tokenStore.accessToken)
        assertEquals("new-refresh", tokenStore.refreshToken)

        val retryRequest = server.takeRequest() // 401
        server.takeRequest() // retry with new token
    }

    @Test
    fun `concurrent 401s trigger only one refresh`() {
        tokenStore.accessToken = "expired-token"
        tokenStore.refreshToken = "valid-refresh"
        tokenStore._expiresAt = 0L

        val fakeAuth = object : AuthApi {
            override suspend fun restorePassword(request: RestorePasswordRequest) = Unit
            override suspend fun login(request: com.mistyislet.app.domain.model.LoginRequest) =
                throw UnsupportedOperationException()
            override suspend fun refresh(request: RefreshRequest): RefreshResponse {
                refreshCallCount.incrementAndGet()
                Thread.sleep(100) // simulate network delay
                return RefreshResponse(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    expiresIn = 3600,
                )
            }
        }

        // All requests return 401, then 200 on retry
        server.dispatcher = object : Dispatcher() {
            private val requestCount = AtomicInteger(0)
            override fun dispatch(request: RecordedRequest): MockResponse {
                val count = requestCount.incrementAndGet()
                // First 3 requests get 401, rest get 200
                return if (count <= 3) {
                    MockResponse().setResponseCode(401)
                } else {
                    MockResponse().setResponseCode(200).setBody("ok")
                }
            }
        }

        val client = buildClient(fakeAuth)

        runBlocking {
            val results = (1..3).map { i ->
                async(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url(server.url("/test$i")).build()
                    ).execute()
                }
            }
            results.forEach { it.await() }
        }

        // Mutex ensures only one refresh happens even with concurrent 401s
        assertEquals(1, refreshCallCount.get())
    }

    @Test
    fun `clears tokens when refresh fails`() {
        tokenStore.accessToken = "expired-token"
        tokenStore.refreshToken = "invalid-refresh"
        tokenStore._expiresAt = 0L

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("login page"))

        val fakeAuth = object : AuthApi {
            override suspend fun restorePassword(request: RestorePasswordRequest) = Unit
            override suspend fun login(request: com.mistyislet.app.domain.model.LoginRequest) =
                throw UnsupportedOperationException()
            override suspend fun refresh(request: RefreshRequest): RefreshResponse {
                throw RuntimeException("refresh failed")
            }
        }

        val client = buildClient(fakeAuth)
        val response = client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertNull(tokenStore.accessToken)
        assertNull(tokenStore.refreshToken)
    }

    @Test
    fun `clears tokens when no refresh token available`() {
        tokenStore.accessToken = "expired-token"
        tokenStore.refreshToken = null
        tokenStore._expiresAt = 0L

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("login page"))

        val client = buildClient(noopAuthApi())
        client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertNull(tokenStore.accessToken)
        assertNull(tokenStore.refreshToken)
    }

    @Test
    fun `skips refresh when another thread already refreshed`() {
        tokenStore.accessToken = "expired-token"
        tokenStore.refreshToken = "valid-refresh"
        tokenStore._expiresAt = 0L

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("success"))

        val fakeAuth = object : AuthApi {
            override suspend fun restorePassword(request: RestorePasswordRequest) = Unit
            override suspend fun login(request: com.mistyislet.app.domain.model.LoginRequest) =
                throw UnsupportedOperationException()
            override suspend fun refresh(request: RefreshRequest): RefreshResponse {
                refreshCallCount.incrementAndGet()
                return RefreshResponse(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    expiresIn = 3600,
                )
            }
        }

        // Simulate: another thread already refreshed the token before we get the mutex
        tokenStore._expiresAt = System.currentTimeMillis() + 3600_000L
        tokenStore.accessToken = "already-refreshed"

        val client = buildClient(fakeAuth)
        val response = client.newCall(
            Request.Builder().url(server.url("/test")).build()
        ).execute()

        assertEquals(200, response.code)
        // isValid() returns true, so it should retry with existing token without calling refresh
        assertEquals(0, refreshCallCount.get())
    }

    private fun noopAuthApi() = object : AuthApi {
        override suspend fun login(request: com.mistyislet.app.domain.model.LoginRequest) =
            throw UnsupportedOperationException()
        override suspend fun refresh(request: RefreshRequest): RefreshResponse =
            throw UnsupportedOperationException()
        override suspend fun restorePassword(request: RestorePasswordRequest) = Unit
    }
}

class FakeTokenStore : TokenStore {
    override var accessToken: String? = null
    override var refreshToken: String? = null
    var _expiresAt: Long = 0L
    override var expiresAt: Long
        get() = _expiresAt
        set(value) { _expiresAt = value }

    override fun isValid(): Boolean = accessToken != null && _expiresAt > System.currentTimeMillis()

    override fun clear() {
        accessToken = null
        refreshToken = null
        _expiresAt = 0L
    }
}
