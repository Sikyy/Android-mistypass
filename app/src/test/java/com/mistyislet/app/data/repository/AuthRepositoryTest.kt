package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var repo: AuthRepository
    private lateinit var fakeApi: FakeAuthApi
    private lateinit var fakeTokenStore: FakeTokenStore

    @Before
    fun setup() {
        fakeApi = FakeAuthApi()
        fakeTokenStore = FakeTokenStore()
        repo = AuthRepository(fakeApi, fakeTokenStore)
    }

    @Test
    fun `lookupOrg returns org config`() = runTest {
        fakeApi.orgLookupResult = OrgAuthConfig("password", null, "Acme Corp")
        val result = repo.lookupOrg("acme.com")
        assertTrue(result is ApiResult.Success)
        assertEquals("password", (result as ApiResult.Success).data.authType)
    }

    @Test
    fun `requestMagicLink returns sent status`() = runTest {
        fakeApi.magicLinkResult = MagicLinkResponse("sent")
        val result = repo.requestMagicLink("user@test.com")
        assertTrue(result is ApiResult.Success)
        assertEquals("sent", (result as ApiResult.Success).data.status)
    }

    @Test
    fun `verifyMagicLink stores tokens on success`() = runTest {
        val loginResponse = LoginResponse("access-123", "refresh-456", 3600, UserInfo("1", "u@t.com", "U", "t1"))
        fakeApi.verifyMagicLinkResult = loginResponse
        val result = repo.verifyMagicLink("magic-token")
        assertTrue(result is ApiResult.Success)
        assertEquals("access-123", fakeTokenStore.accessToken)
        assertEquals("refresh-456", fakeTokenStore.refreshToken)
    }

    @Test
    fun `login stores tokens on success`() = runTest {
        val loginResponse = LoginResponse("a", "r", 3600, UserInfo("1", "u@t.com", "U", "t1"))
        fakeApi.loginResult = loginResponse
        val result = repo.login("u@t.com", "pass")
        assertTrue(result is ApiResult.Success)
        assertEquals("a", fakeTokenStore.accessToken)
    }
}

// --- Test Doubles ---

class FakeAuthApi : AuthApi {
    var loginResult: LoginResponse? = null
    var orgLookupResult: OrgAuthConfig? = null
    var magicLinkResult: MagicLinkResponse? = null
    var verifyMagicLinkResult: LoginResponse? = null

    override suspend fun login(request: LoginRequest): LoginResponse = loginResult!!
    override suspend fun refresh(request: RefreshRequest): RefreshResponse = throw NotImplementedError()
    override suspend fun restorePassword(request: RestorePasswordRequest) {}
    override suspend fun orgLookup(domain: String): OrgAuthConfig = orgLookupResult!!
    override suspend fun requestMagicLink(request: MagicLinkRequest): MagicLinkResponse = magicLinkResult!!
    override suspend fun verifyMagicLink(request: VerifyMagicLinkRequest): LoginResponse = verifyMagicLinkResult!!
}

class FakeTokenStore : TokenStore {
    override var accessToken: String? = null
    override var refreshToken: String? = null
    override var expiresAt: Long = 0L
    override fun isValid(): Boolean = accessToken != null && System.currentTimeMillis() < expiresAt
    override fun clear() { accessToken = null; refreshToken = null; expiresAt = 0L }
}
