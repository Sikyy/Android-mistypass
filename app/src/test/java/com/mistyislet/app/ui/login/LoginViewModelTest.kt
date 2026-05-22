package com.mistyislet.app.ui.login

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var vm: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        vm = LoginViewModel(fakeRepo)
    }

    @org.junit.After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is EmailInput`() {
        assertEquals(AuthStep.EmailInput, vm.uiState.value.authStep)
    }

    @Test
    fun `submitEmail transitions to PasswordInput for password org`() = runTest {
        fakeRepo.lookupOrgResult = ApiResult.Success(OrgAuthConfig("password", null, "Acme"))
        vm.onEmailChange("user@acme.com")
        vm.submitEmail()
        advanceUntilIdle()
        assertEquals(AuthStep.PasswordInput, vm.uiState.value.authStep)
        assertEquals("Acme", vm.uiState.value.orgAuthConfig?.orgName)
    }

    @Test
    fun `submitEmail transitions to SSORedirect for sso org`() = runTest {
        fakeRepo.lookupOrgResult = ApiResult.Success(OrgAuthConfig("sso", "https://sso.acme.com/login", "Acme"))
        vm.onEmailChange("user@acme.com")
        vm.submitEmail()
        advanceUntilIdle()
        assertEquals(AuthStep.SSORedirect, vm.uiState.value.authStep)
        assertEquals("https://sso.acme.com/login", vm.uiState.value.orgAuthConfig?.ssoUrl)
    }

    @Test
    fun `submitEmail falls back to PasswordInput on lookup error`() = runTest {
        fakeRepo.lookupOrgResult = ApiResult.Error(404, "not found")
        vm.onEmailChange("user@unknown.com")
        vm.submitEmail()
        advanceUntilIdle()
        assertEquals(AuthStep.PasswordInput, vm.uiState.value.authStep)
    }

    @Test
    fun `requestMagicLink transitions to MagicLinkSent`() = runTest {
        fakeRepo.magicLinkResult = ApiResult.Success(MagicLinkResponse("sent"))
        vm.onEmailChange("user@test.com")
        vm.requestMagicLink()
        advanceUntilIdle()
        assertEquals(AuthStep.MagicLinkSent, vm.uiState.value.authStep)
    }

    @Test
    fun `goBack returns to EmailInput`() = runTest {
        fakeRepo.lookupOrgResult = ApiResult.Success(OrgAuthConfig("password"))
        vm.onEmailChange("u@t.com")
        vm.submitEmail()
        advanceUntilIdle()
        vm.goBack()
        assertEquals(AuthStep.EmailInput, vm.uiState.value.authStep)
    }

    @Test
    fun `login mfa required transitions to MfaInput`() = runTest {
        fakeRepo.loginResult = ApiResult.Error(401, "admin mfa code is required", "mfa_required")
        vm.onEmailChange("admin@test.com")
        vm.onPasswordChange("admin123")
        vm.login()
        advanceUntilIdle()
        assertEquals(AuthStep.MfaInput, vm.uiState.value.authStep)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `login from MfaInput forwards mfa code`() = runTest {
        fakeRepo.loginResult = ApiResult.Success(LoginResponse("a", "r", 3600, UserInfo("1", "u@t.com", "U", "t1")))
        vm.onEmailChange("admin@test.com")
        vm.onPasswordChange("admin123")
        vm.onMfaCodeChange("123456")
        fakeRepo.loginResult = ApiResult.Error(401, "admin mfa code is required", "mfa_required")
        vm.login()
        advanceUntilIdle()

        fakeRepo.loginResult = ApiResult.Success(LoginResponse("a", "r", 3600, UserInfo("1", "u@t.com", "U", "t1")))
        vm.onMfaCodeChange("654321")
        vm.login()
        advanceUntilIdle()

        assertEquals("654321", fakeRepo.lastMfaCode)
    }
}

// --- Test Double ---
// AuthRepository is open, so we can subclass it.
// We need to provide fake constructor args that are never actually used.
class FakeAuthRepository : AuthRepository(
    authApi = object : com.mistyislet.app.data.api.AuthApi {
        override suspend fun login(request: LoginRequest) = throw NotImplementedError()
        override suspend fun refresh(request: RefreshRequest) = throw NotImplementedError()
        override suspend fun restorePassword(request: RestorePasswordRequest) {}
        override suspend fun orgLookup(domain: String) = throw NotImplementedError()
        override suspend fun requestMagicLink(request: MagicLinkRequest) = throw NotImplementedError()
        override suspend fun verifyMagicLink(request: VerifyMagicLinkRequest) = throw NotImplementedError()
    },
    tokenStore = object : com.mistyislet.app.core.storage.TokenStore {
        override var accessToken: String? = null
        override var refreshToken: String? = null
        override var expiresAt: Long = 0L
        override fun isValid(): Boolean = false
        override fun clear() {}
    },
) {
    var loginResult: ApiResult<LoginResponse> = ApiResult.Error(0, "not set")
    var lookupOrgResult: ApiResult<OrgAuthConfig> = ApiResult.Error(0, "not set")
    var magicLinkResult: ApiResult<MagicLinkResponse> = ApiResult.Error(0, "not set")
    var verifyResult: ApiResult<LoginResponse> = ApiResult.Error(0, "not set")
    var lastMfaCode: String? = null

    override suspend fun login(email: String, password: String, mfaCode: String?): ApiResult<LoginResponse> {
        lastMfaCode = mfaCode
        return loginResult
    }
    override suspend fun lookupOrg(domain: String) = lookupOrgResult
    override suspend fun requestMagicLink(email: String) = magicLinkResult
    override suspend fun verifyMagicLink(token: String) = verifyResult
    override suspend fun restorePassword(email: String) = ApiResult.Success(Unit)
    override fun isLoggedIn() = false
    override fun logout() {}
}
