# P0 Android 功能补齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the 4 P0 iOS parity gaps on Android: Deep Linking, Magic Link login, SSO org lookup, and Geofencing.

**Architecture:** Three independent modules added in sequence. Deep Linking is foundational (Magic Link callback depends on it). Magic Link + SSO expand the existing AuthApi/AuthRepository/LoginViewModel. Geofencing is a standalone service wired into DoorRepository and ProfileViewModel.

**Tech Stack:** Navigation Compose deep links, CustomTabs (androidx.browser), Google Play Services Location (GeofencingClient), kotlinx.serialization, Hilt DI.

---

## File Map

### New Files (3)

| File | Responsibility |
|------|---------------|
| `app/src/main/java/com/mistyislet/app/core/deeplink/DeepLinkHandler.kt` | Extract magic-link tokens and SSO callback tokens from intents |
| `app/src/main/java/com/mistyislet/app/core/geofence/GeofenceManager.kt` | Register/sync/clear geofences via GeofencingClient |
| `app/src/main/java/com/mistyislet/app/core/geofence/GeofenceBroadcastReceiver.kt` | Receive geofence ENTER events → fire local notification |

### New Test Files (4)

| File | Tests |
|------|-------|
| `app/src/test/java/com/mistyislet/app/core/deeplink/DeepLinkHandlerTest.kt` | Intent parsing for all deep link types |
| `app/src/test/java/com/mistyislet/app/ui/login/LoginViewModelTest.kt` | Auth state machine transitions |
| `app/src/test/java/com/mistyislet/app/data/repository/AuthRepositoryTest.kt` | New auth repository methods |
| `app/src/test/java/com/mistyislet/app/core/geofence/GeofenceManagerTest.kt` | Sync logic — add/remove diff |

### Modified Files (11)

| File | What Changes |
|------|-------------|
| `app/src/main/AndroidManifest.xml` | +2 intent-filters, +1 permission, +1 receiver |
| `gradle/libs.versions.toml` | +2 library entries (browser, play-services-location) |
| `app/build.gradle.kts` | +2 implementation lines |
| `app/src/main/java/.../data/api/AuthApi.kt` | +3 methods (orgLookup, requestMagicLink, verifyMagicLink) |
| `app/src/main/java/.../domain/model/ApiModels.kt` | +4 data classes |
| `app/src/main/java/.../data/repository/AuthRepository.kt` | +3 methods |
| `app/src/main/java/.../ui/login/LoginViewModel.kt` | Rewrite: AuthStep enum + state machine |
| `app/src/main/java/.../ui/login/LoginScreen.kt` | Rewrite: step-based composables |
| `app/src/main/java/.../ui/navigation/AppNavigation.kt` | deepLinks param on every route |
| `app/src/main/java/.../MainActivity.kt` | onNewIntent + magic-link/SSO token forwarding |
| `app/src/main/java/.../data/repository/DoorRepository.kt` | Call geofenceManager.syncGeofences() after refresh |
| `app/src/main/java/.../ui/profile/ProfileViewModel.kt` | Wire toggleGeofence to GeofenceManager |

---

## Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add library entries to version catalog**

In `gradle/libs.versions.toml`, add to `[versions]` section:

```toml
browser = "1.8.0"
playServicesLocation = "21.3.0"
```

Add to `[libraries]` section:

```toml
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
```

- [ ] **Step 2: Add implementation lines to build.gradle.kts**

In `app/build.gradle.kts`, add after the `firebase-messaging` line (around line 149):

```kotlin
    // Browser (CustomTabs for SSO)
    implementation(libs.androidx.browser)

    // Location (Geofencing)
    implementation(libs.play.services.location)
```

- [ ] **Step 3: Sync and verify**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew app:dependencies --configuration releaseRuntimeClasspath 2>&1 | grep -E "browser|play-services-location"`

Expected: Both libraries appear in dependency tree.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add browser and play-services-location dependencies"
```

---

## Task 2: Deep Link — DeepLinkHandler

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/core/deeplink/DeepLinkHandler.kt`
- Create: `app/src/test/java/com/mistyislet/app/core/deeplink/DeepLinkHandlerTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/mistyislet/app/core/deeplink/DeepLinkHandlerTest.kt`:

```kotlin
package com.mistyislet.app.core.deeplink

import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkHandlerTest {

    @Test
    fun `extractMagicLinkToken returns token from custom scheme`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://magic-link?token=abc123"))
        assertEquals("abc123", DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractMagicLinkToken returns null for non-magic-link intent`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://unlock/door-1"))
        assertNull(DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractMagicLinkToken returns null for null data`() {
        val intent = Intent(Intent.ACTION_MAIN)
        assertNull(DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractSSOCallbackToken returns token from https callback`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mistyislet.com/sso/callback?token=sso-jwt-xyz"))
        assertEquals("sso-jwt-xyz", DeepLinkHandler.extractSSOCallbackToken(intent))
    }

    @Test
    fun `extractSSOCallbackToken returns null for non-callback path`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mistyislet.com/visitor/abc"))
        assertNull(DeepLinkHandler.extractSSOCallbackToken(intent))
    }

    @Test
    fun `isMagicLinkIntent returns true for magic-link URI`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://magic-link?token=x"))
        assertTrue(DeepLinkHandler.isMagicLinkIntent(intent))
    }

    @Test
    fun `isMagicLinkIntent returns false for regular deep link`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://pass"))
        assertFalse(DeepLinkHandler.isMagicLinkIntent(intent))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest --tests "com.mistyislet.app.core.deeplink.DeepLinkHandlerTest" 2>&1 | tail -5`

Expected: FAIL — `DeepLinkHandler` class not found.

- [ ] **Step 3: Implement DeepLinkHandler**

Create `app/src/main/java/com/mistyislet/app/core/deeplink/DeepLinkHandler.kt`:

```kotlin
package com.mistyislet.app.core.deeplink

import android.content.Intent
import android.net.Uri

object DeepLinkHandler {

    private const val CUSTOM_SCHEME = "mistyislet"
    private const val APP_HOST = "app.mistyislet.com"
    private const val MAGIC_LINK_HOST = "magic-link"
    private const val SSO_CALLBACK_PATH = "/sso/callback"

    fun extractMagicLinkToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        if (uri.scheme == CUSTOM_SCHEME && uri.host == MAGIC_LINK_HOST) {
            return uri.getQueryParameter("token")
        }
        return null
    }

    fun extractSSOCallbackToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        if (uri.scheme == "https" && uri.host == APP_HOST && uri.path == SSO_CALLBACK_PATH) {
            return uri.getQueryParameter("token")
        }
        return null
    }

    fun isMagicLinkIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return uri.scheme == CUSTOM_SCHEME && uri.host == MAGIC_LINK_HOST
    }

    fun extractAuthToken(intent: Intent): String? {
        return extractMagicLinkToken(intent) ?: extractSSOCallbackToken(intent)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest --tests "com.mistyislet.app.core.deeplink.DeepLinkHandlerTest" 2>&1 | tail -5`

Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/core/deeplink/DeepLinkHandler.kt \
       app/src/test/java/com/mistyislet/app/core/deeplink/DeepLinkHandlerTest.kt
git commit -m "feat: add DeepLinkHandler for magic-link and SSO token extraction"
```

---

## Task 3: Deep Link — Manifest + Navigation Wiring

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/.../ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/.../MainActivity.kt`

- [ ] **Step 1: Add intent-filters to AndroidManifest.xml**

In `AndroidManifest.xml`, inside the `<activity>` tag for MainActivity, after the existing MAIN/LAUNCHER intent-filter, add:

```xml
            <!-- Deep Links: Custom Scheme -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="mistyislet" />
            </intent-filter>

            <!-- Deep Links: App Links (HTTPS) -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="app.mistyislet.com" />
            </intent-filter>
```

- [ ] **Step 2: Add deep links to AppNavigation.kt**

Add import at top of file:

```kotlin
import androidx.navigation.navDeepLink
```

Replace the inner NavHost composable block (lines 162-209) — add `deepLinks` to each route that needs one. The key changes are:

```kotlin
composable(
    Routes.DOORS,
    deepLinks = listOf(navDeepLink { uriPattern = "mistyislet://unlock/{doorId}" }),
) { DoorsRootScreen() }

composable(
    Routes.PASS,
    deepLinks = listOf(navDeepLink { uriPattern = "mistyislet://pass" }),
) {
    CredentialsScreen(
        onNavigateToBindCard = { navController.navigate(Routes.BIND_CARD) },
    )
}

composable(
    Routes.DASHBOARD,
    deepLinks = listOf(navDeepLink { uriPattern = "mistyislet://dashboard" }),
) {
    DashboardScreen(
        onNavigate = { route -> navController.navigate(route) },
    )
}

composable(
    Routes.PROFILE,
    deepLinks = listOf(navDeepLink { uriPattern = "mistyislet://profile" }),
) {
    ProfileScreen(onLogout = onLogout)
}

composable(
    Routes.VISITORS,
    deepLinks = listOf(
        navDeepLink { uriPattern = "https://app.mistyislet.com/visitor/{token}" },
    ),
) { VisitorsScreen() }
```

All other composable calls (HISTORY, BIND_CARD, admin screens) remain unchanged.

- [ ] **Step 3: Add onNewIntent to MainActivity**

In `MainActivity.kt`, add `onNewIntent` override and modify `onCreate` to handle deep link intent for magic-link/SSO tokens. Add import and companion:

```kotlin
import android.content.Intent
import com.mistyislet.app.core.deeplink.DeepLinkHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
```

Add to class body (after `biometricRequired` field):

```kotlin
    private val _authTokenFromDeepLink = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authTokenFromDeepLink: SharedFlow<String> = _authTokenFromDeepLink

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeepLink(intent)
    }

    private fun handleAuthDeepLink(intent: Intent) {
        val token = DeepLinkHandler.extractAuthToken(intent)
        if (token != null) {
            _authTokenFromDeepLink.tryEmit(token)
        }
    }
```

Also add `handleAuthDeepLink(intent)` at the end of `onCreate`, after `setContent`:

```kotlin
        handleAuthDeepLink(intent)
```

- [ ] **Step 4: Build to verify compilation**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug 2>&1 | tail -3`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
       app/src/main/java/com/mistyislet/app/ui/navigation/AppNavigation.kt \
       app/src/main/java/com/mistyislet/app/MainActivity.kt
git commit -m "feat: wire deep links in manifest and navigation routes"
```

---

## Task 4: Auth API + Models for Magic Link and SSO

**Files:**
- Modify: `app/src/main/java/.../domain/model/ApiModels.kt`
- Modify: `app/src/main/java/.../data/api/AuthApi.kt`
- Modify: `app/src/main/java/.../data/repository/AuthRepository.kt`
- Create: `app/src/test/java/com/mistyislet/app/data/repository/AuthRepositoryTest.kt`

- [ ] **Step 1: Add data models to ApiModels.kt**

Append after the `ApiError` class (after line 67):

```kotlin
@Serializable
data class OrgAuthConfig(
    @SerialName("auth_type") val authType: String,
    @SerialName("sso_url") val ssoUrl: String? = null,
    @SerialName("org_name") val orgName: String? = null,
)

@Serializable
data class MagicLinkRequest(val email: String)

@Serializable
data class MagicLinkResponse(val status: String)

@Serializable
data class VerifyMagicLinkRequest(val token: String)
```

- [ ] **Step 2: Add methods to AuthApi.kt**

Add imports and methods. The full file becomes:

```kotlin
package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import com.mistyislet.app.domain.model.MagicLinkRequest
import com.mistyislet.app.domain.model.MagicLinkResponse
import com.mistyislet.app.domain.model.OrgAuthConfig
import com.mistyislet.app.domain.model.RefreshRequest
import com.mistyislet.app.domain.model.RefreshResponse
import com.mistyislet.app.domain.model.RestorePasswordRequest
import com.mistyislet.app.domain.model.VerifyMagicLinkRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("app/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("app/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST("app/auth/restore-password")
    suspend fun restorePassword(@Body request: RestorePasswordRequest)

    @GET("app/auth/org-lookup")
    suspend fun orgLookup(@Query("domain") domain: String): OrgAuthConfig

    @POST("app/auth/magic-link")
    suspend fun requestMagicLink(@Body request: MagicLinkRequest): MagicLinkResponse

    @POST("app/auth/magic-link/verify")
    suspend fun verifyMagicLink(@Body request: VerifyMagicLinkRequest): LoginResponse
}
```

- [ ] **Step 3: Add methods to AuthRepository.kt**

Add imports and methods. The full file becomes:

```kotlin
package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.data.api.AuthApi
import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import com.mistyislet.app.domain.model.MagicLinkRequest
import com.mistyislet.app.domain.model.MagicLinkResponse
import com.mistyislet.app.domain.model.OrgAuthConfig
import com.mistyislet.app.domain.model.RestorePasswordRequest
import com.mistyislet.app.domain.model.VerifyMagicLinkRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return safeApiCall {
            val response = authApi.login(LoginRequest(email, password))
            storeTokens(response)
            response
        }
    }

    suspend fun lookupOrg(domain: String): ApiResult<OrgAuthConfig> {
        return safeApiCall { authApi.orgLookup(domain) }
    }

    suspend fun requestMagicLink(email: String): ApiResult<MagicLinkResponse> {
        return safeApiCall { authApi.requestMagicLink(MagicLinkRequest(email)) }
    }

    suspend fun verifyMagicLink(token: String): ApiResult<LoginResponse> {
        return safeApiCall {
            val response = authApi.verifyMagicLink(VerifyMagicLinkRequest(token))
            storeTokens(response)
            response
        }
    }

    suspend fun restorePassword(email: String): ApiResult<Unit> =
        safeApiCall { authApi.restorePassword(RestorePasswordRequest(email)) }

    fun isLoggedIn(): Boolean = tokenStore.isValid()

    fun logout() {
        tokenStore.clear()
    }

    private fun storeTokens(response: LoginResponse) {
        tokenStore.accessToken = response.accessToken
        tokenStore.refreshToken = response.refreshToken
        tokenStore.expiresAt = System.currentTimeMillis() + response.expiresIn * 1000L
    }
}
```

- [ ] **Step 4: Write AuthRepository tests**

Create `app/src/test/java/com/mistyislet/app/data/repository/AuthRepositoryTest.kt`:

```kotlin
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
```

- [ ] **Step 5: Run tests**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest --tests "com.mistyislet.app.data.repository.AuthRepositoryTest" 2>&1 | tail -5`

Expected: All 4 tests PASS.

Note: If `TokenStore` is a concrete class (not interface), the `FakeTokenStore` needs to extend it instead. Check `TokenStore.kt` — if it's a class with `var` fields, adjust the fake to instantiate the real class and set fields directly.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/domain/model/ApiModels.kt \
       app/src/main/java/com/mistyislet/app/data/api/AuthApi.kt \
       app/src/main/java/com/mistyislet/app/data/repository/AuthRepository.kt \
       app/src/test/java/com/mistyislet/app/data/repository/AuthRepositoryTest.kt
git commit -m "feat: add org-lookup, magic-link, and verify-magic-link auth endpoints"
```

---

## Task 5: Login ViewModel — State Machine Rewrite

**Files:**
- Modify: `app/src/main/java/.../ui/login/LoginViewModel.kt`
- Create: `app/src/test/java/com/mistyislet/app/ui/login/LoginViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Create `app/src/test/java/com/mistyislet/app/ui/login/LoginViewModelTest.kt`:

```kotlin
package com.mistyislet.app.ui.login

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
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
}

// --- Test Double ---
class FakeAuthRepository : AuthRepository(
    authApi = throw IllegalStateException("Not used"),
    tokenStore = throw IllegalStateException("Not used"),
) {
    // We override all methods so the constructor params are never accessed.
    var loginResult: ApiResult<LoginResponse> = ApiResult.Error(0, "not set")
    var lookupOrgResult: ApiResult<OrgAuthConfig> = ApiResult.Error(0, "not set")
    var magicLinkResult: ApiResult<MagicLinkResponse> = ApiResult.Error(0, "not set")
    var verifyResult: ApiResult<LoginResponse> = ApiResult.Error(0, "not set")

    override suspend fun login(email: String, password: String) = loginResult
    override suspend fun lookupOrg(domain: String) = lookupOrgResult
    override suspend fun requestMagicLink(email: String) = magicLinkResult
    override suspend fun verifyMagicLink(token: String) = verifyResult
    override suspend fun restorePassword(email: String) = ApiResult.Success(Unit)
    override fun isLoggedIn() = false
    override fun logout() {}
}
```

Note: If `AuthRepository` can't be subclassed (it's not `open`), add the `open` modifier to the class and all methods being overridden. Alternatively, extract an interface — but `open` is simpler here.

- [ ] **Step 2: Rewrite LoginViewModel.kt**

Replace the entire content of `app/src/main/java/.../ui/login/LoginViewModel.kt`:

```kotlin
package com.mistyislet.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.repository.AuthRepository
import com.mistyislet.app.domain.model.OrgAuthConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthStep {
    EmailInput,
    OrgLookupLoading,
    PasswordInput,
    MagicLinkSent,
    SSORedirect,
}

data class LoginUiState(
    val authStep: AuthStep = AuthStep.EmailInput,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orgAuthConfig: OrgAuthConfig? = null,
    val forgotPasswordSent: Boolean = false,
    val forgotPasswordError: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess: SharedFlow<Unit> = _loginSuccess

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun submitEmail() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) return

        val domain = email.substringAfter("@")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authStep = AuthStep.OrgLookupLoading, errorMessage = null)

            when (val result = authRepository.lookupOrg(domain)) {
                is ApiResult.Success -> {
                    val config = result.data
                    val nextStep = when (config.authType) {
                        "sso", "saml" -> AuthStep.SSORedirect
                        else -> AuthStep.PasswordInput
                    }
                    _uiState.value = _uiState.value.copy(authStep = nextStep, orgAuthConfig = config)
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    // No org config found — fall back to password login
                    _uiState.value = _uiState.value.copy(authStep = AuthStep.PasswordInput, orgAuthConfig = null)
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            when (val result = authRepository.login(state.email, state.password)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _loginSuccess.emit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.throwable.localizedMessage)
                }
            }
        }
    }

    fun requestMagicLink() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (authRepository.requestMagicLink(email)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(authStep = AuthStep.MagicLinkSent, isLoading = false)
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to send login link")
                }
            }
        }
    }

    fun verifyMagicLink(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (authRepository.verifyMagicLink(token)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _loginSuccess.emit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Login link expired or invalid")
                }
                is ApiResult.Exception -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Network error")
                }
            }
        }
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(
            authStep = AuthStep.EmailInput,
            password = "",
            errorMessage = null,
            orgAuthConfig = null,
        )
    }

    fun restorePassword(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            when (authRepository.restorePassword(email)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(forgotPasswordSent = true, forgotPasswordError = null)
                is ApiResult.Error, is ApiResult.Exception -> _uiState.value = _uiState.value.copy(forgotPasswordError = "Failed to send reset email")
            }
        }
    }

    fun clearForgotPasswordState() {
        _uiState.value = _uiState.value.copy(forgotPasswordSent = false, forgotPasswordError = null)
    }
}
```

- [ ] **Step 3: Make AuthRepository open for testing**

In `AuthRepository.kt`, add `open` to the class and all public/internal methods:

```kotlin
@Singleton
open class AuthRepository @Inject constructor(
```

And prefix each method with `open`:

```kotlin
    open suspend fun login(...
    open suspend fun lookupOrg(...
    open suspend fun requestMagicLink(...
    open suspend fun verifyMagicLink(...
    open suspend fun restorePassword(...
    open fun isLoggedIn(...
    open fun logout(...
```

- [ ] **Step 4: Run ViewModel tests**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest --tests "com.mistyislet.app.ui.login.LoginViewModelTest" 2>&1 | tail -10`

Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/login/LoginViewModel.kt \
       app/src/main/java/com/mistyislet/app/data/repository/AuthRepository.kt \
       app/src/test/java/com/mistyislet/app/ui/login/LoginViewModelTest.kt
git commit -m "feat: rewrite LoginViewModel with AuthStep state machine for magic-link and SSO"
```

---

## Task 6: Login Screen UI — Step-Based Composables

**Files:**
- Modify: `app/src/main/java/.../ui/login/LoginScreen.kt`

- [ ] **Step 1: Rewrite LoginScreen.kt with step-based UI**

Replace the entire content of `LoginScreen.kt`. The key structure:

```kotlin
package com.mistyislet.app.ui.login

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    magicLinkToken: String? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loginSuccess.collect { onLoginSuccess() }
    }

    // Auto-verify if a magic link token was passed via deep link
    LaunchedEffect(magicLinkToken) {
        if (magicLinkToken != null) {
            viewModel.verifyMagicLink(magicLinkToken)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState.authStep) {
            AuthStep.EmailInput -> EmailInputStep(uiState, viewModel)
            AuthStep.OrgLookupLoading -> OrgLookupLoadingStep()
            AuthStep.PasswordInput -> PasswordInputStep(uiState, viewModel)
            AuthStep.MagicLinkSent -> MagicLinkSentStep(uiState, viewModel)
            AuthStep.SSORedirect -> SSORedirectStep(uiState, viewModel)
        }
    }
}

@Composable
private fun EmailInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { focusManager.clearFocus(); vm.submitEmail() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { focusManager.clearFocus(); vm.submitEmail() },
            enabled = state.email.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.continue_button)) }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OrgLookupLoadingStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.looking_up_org), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PasswordInputStep(state: LoginUiState, vm: LoginViewModel) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf(state.email) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = vm::goBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(state.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.orgAuthConfig?.orgName?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); vm.login() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { focusManager.clearFocus(); vm.login() },
            enabled = state.password.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.login))
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { vm.requestMagicLink() }) {
            Text(stringResource(R.string.send_magic_link))
        }

        TextButton(onClick = { showForgotPassword = true }) {
            Text(stringResource(R.string.forgot_password))
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false; vm.clearForgotPasswordState() },
            title = { Text(stringResource(R.string.forgot_password)) },
            text = {
                Column {
                    if (state.forgotPasswordSent) {
                        Text(stringResource(R.string.reset_email_sent))
                    } else {
                        OutlinedTextField(value = forgotEmail, onValueChange = { forgotEmail = it }, label = { Text(stringResource(R.string.email)) }, modifier = Modifier.fillMaxWidth())
                        state.forgotPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                if (!state.forgotPasswordSent) TextButton(onClick = { vm.restorePassword(forgotEmail) }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = { TextButton(onClick = { showForgotPassword = false; vm.clearForgotPasswordState() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MagicLinkSentStep(state: LoginUiState, vm: LoginViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.magic_link_sent_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.magic_link_sent_body, state.email), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            OutlinedButton(onClick = { vm.requestMagicLink() }) { Text(stringResource(R.string.resend)) }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = vm::goBack) { Text(stringResource(R.string.back_to_login)) }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SSORedirectStep(state: LoginUiState, vm: LoginViewModel) {
    val context = LocalContext.current
    val ssoUrl = state.orgAuthConfig?.ssoUrl

    LaunchedEffect(ssoUrl) {
        if (ssoUrl != null) {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, ssoUrl.toUri())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.sso_redirecting, state.orgAuthConfig?.orgName ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = vm::goBack) { Text(stringResource(R.string.cancel)) }
    }
}
```

- [ ] **Step 2: Add string resources**

In `app/src/main/res/values/strings.xml`, add:

```xml
    <string name="continue_button">Continue</string>
    <string name="looking_up_org">Looking up organization…</string>
    <string name="send_magic_link">Send login link instead</string>
    <string name="magic_link_sent_title">Check your email</string>
    <string name="magic_link_sent_body">A login link has been sent to %1$s. Click the link to sign in.</string>
    <string name="resend">Resend</string>
    <string name="back_to_login">Back to login</string>
    <string name="sso_redirecting">Redirecting to %1$s login…</string>
    <string name="send">Send</string>
    <string name="cancel">Cancel</string>
    <string name="reset_email_sent">Password reset email sent</string>
```

Also add matching strings to `res/values-zh/strings.xml` and `res/values-in/strings.xml`.

- [ ] **Step 3: Build to verify**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug 2>&1 | tail -3`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/login/LoginScreen.kt \
       app/src/main/res/values/strings.xml \
       app/src/main/res/values-zh/strings.xml \
       app/src/main/res/values-in/strings.xml
git commit -m "feat: step-based login UI with magic link and SSO redirect screens"
```

---

## Task 7: Geofence — GeofenceManager + BroadcastReceiver

**Files:**
- Create: `app/src/main/java/.../core/geofence/GeofenceManager.kt`
- Create: `app/src/main/java/.../core/geofence/GeofenceBroadcastReceiver.kt`
- Create: `app/src/test/java/.../core/geofence/GeofenceManagerTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write GeofenceManager test**

Create `app/src/test/java/com/mistyislet/app/core/geofence/GeofenceManagerTest.kt`:

```kotlin
package com.mistyislet.app.core.geofence

import com.mistyislet.app.domain.model.AccessibleDoor
import org.junit.Assert.*
import org.junit.Test

class GeofenceManagerTest {

    @Test
    fun `computeGeofenceDiff adds new doors`() {
        val active = emptySet<String>()
        val doors = listOf(doorWithLocation("d1", 1.0, 2.0), doorWithLocation("d2", 3.0, 4.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(setOf("d1", "d2"), diff.toAdd.map { it.id }.toSet())
        assertTrue(diff.toRemove.isEmpty())
    }

    @Test
    fun `computeGeofenceDiff removes stale doors`() {
        val active = setOf("d1", "d2", "d3")
        val doors = listOf(doorWithLocation("d2", 1.0, 2.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(setOf("d1", "d3"), diff.toRemove)
        assertTrue(diff.toAdd.isEmpty())
    }

    @Test
    fun `computeGeofenceDiff skips doors without coordinates`() {
        val active = emptySet<String>()
        val doors = listOf(doorWithoutLocation("d1"), doorWithLocation("d2", 1.0, 2.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(1, diff.toAdd.size)
        assertEquals("d2", diff.toAdd.first().id)
    }

    @Test
    fun `computeGeofenceDiff caps at 100 doors`() {
        val active = emptySet<String>()
        val doors = (1..150).map { doorWithLocation("d$it", it.toDouble(), it.toDouble()) }
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(100, diff.toAdd.size)
    }

    private fun doorWithLocation(id: String, lat: Double, lng: Double) = AccessibleDoor(
        id = id, name = "Door $id", buildingId = "b1", status = "online",
        gatewayStatus = "online", canUnlock = true, latitude = lat, longitude = lng,
    )

    private fun doorWithoutLocation(id: String) = AccessibleDoor(
        id = id, name = "Door $id", buildingId = "b1", status = "online",
        gatewayStatus = "online", canUnlock = true,
    )
}
```

- [ ] **Step 2: Add latitude/longitude to AccessibleDoor**

In `app/src/main/java/.../domain/model/AccessibleDoor.kt`, add two nullable fields to the data class:

```kotlin
    val latitude: Double? = null,
    val longitude: Double? = null,
```

Add after `kind` field, before the closing parenthesis.

- [ ] **Step 3: Implement GeofenceManager**

Create `app/src/main/java/com/mistyislet/app/core/geofence/GeofenceManager.kt`:

```kotlin
package com.mistyislet.app.core.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mistyislet.app.domain.model.AccessibleDoor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GeofenceDiff(
    val toAdd: List<AccessibleDoor>,
    val toRemove: Set<String>,
)

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getGeofencingClient(context) }
    private val activeGeofenceIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "GeofenceManager"
        private const val RADIUS_METERS = 50f
        private const val MAX_GEOFENCES = 100

        fun computeGeofenceDiff(
            activeIds: Set<String>,
            doors: List<AccessibleDoor>,
        ): GeofenceDiff {
            val doorsWithCoords = doors.filter { it.latitude != null && it.longitude != null }
                .take(MAX_GEOFENCES)
            val newIds = doorsWithCoords.map { it.id }.toSet()
            val toAdd = doorsWithCoords.filter { it.id !in activeIds }
            val toRemove = activeIds - newIds
            return GeofenceDiff(toAdd, toRemove)
        }
    }

    fun syncGeofences(doors: List<AccessibleDoor>) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Missing location permission, skipping geofence sync")
            return
        }

        val diff = computeGeofenceDiff(activeGeofenceIds, doors)

        if (diff.toRemove.isNotEmpty()) {
            try {
                client.removeGeofences(diff.toRemove.toList())
                activeGeofenceIds.removeAll(diff.toRemove)
                Log.d(TAG, "Removed ${diff.toRemove.size} geofences")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException removing geofences", e)
            }
        }

        if (diff.toAdd.isNotEmpty()) {
            val geofences = diff.toAdd.map { door ->
                Geofence.Builder()
                    .setRequestId(door.id)
                    .setCircularRegion(door.latitude!!, door.longitude!!, RADIUS_METERS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            try {
                client.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        activeGeofenceIds.addAll(diff.toAdd.map { it.id })
                        Log.d(TAG, "Added ${diff.toAdd.size} geofences")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to add geofences", e)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException adding geofences", e)
            }
        }
    }

    fun clearAll() {
        if (activeGeofenceIds.isNotEmpty()) {
            try {
                client.removeGeofences(geofencePendingIntent)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException clearing geofences", e)
            }
            activeGeofenceIds.clear()
            Log.d(TAG, "Cleared all geofences")
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }
}
```

- [ ] **Step 4: Implement GeofenceBroadcastReceiver**

Create `app/src/main/java/com/mistyislet/app/core/geofence/GeofenceBroadcastReceiver.kt`:

```kotlin
package com.mistyislet.app.core.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mistyislet.app.MainActivity
import com.mistyislet.app.R
import com.mistyislet.app.core.push.MistyisletMessagingService

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val NOTIFICATION_ID_BASE = 9000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null || event.hasError()) {
            Log.e(TAG, "Geofencing event error: ${event?.errorCode}")
            return
        }

        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        for (geofence in event.triggeringGeofences.orEmpty()) {
            val doorId = geofence.requestId
            Log.d(TAG, "Entered geofence for door: $doorId")
            sendNotification(context, doorId)
        }
    }

    private fun sendNotification(context: Context, doorId: String) {
        val deepLinkUri = "mistyislet://unlock/$doorId".toUri()
        val tapIntent = Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, doorId.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, MistyisletMessagingService.CHANNEL_ACCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.geofence_notification_title))
            .setContentText(context.getString(R.string.geofence_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + doorId.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission", e)
        }
    }
}
```

- [ ] **Step 5: Add geofence strings**

Add to `res/values/strings.xml`:

```xml
    <string name="geofence_notification_title">Door nearby</string>
    <string name="geofence_notification_body">Tap to unlock</string>
```

Add matching translations in `values-zh` and `values-in`.

- [ ] **Step 6: Add manifest entries**

In `AndroidManifest.xml`, add permission (after existing location permissions):

```xml
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

Add receiver (inside `<application>`, after the widget receiver):

```xml
        <receiver
            android:name=".core.geofence.GeofenceBroadcastReceiver"
            android:exported="false" />
```

- [ ] **Step 7: Run GeofenceManager tests**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest --tests "com.mistyislet.app.core.geofence.GeofenceManagerTest" 2>&1 | tail -5`

Expected: All 4 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/core/geofence/ \
       app/src/main/java/com/mistyislet/app/domain/model/AccessibleDoor.kt \
       app/src/main/AndroidManifest.xml \
       app/src/main/res/values/strings.xml \
       app/src/main/res/values-zh/strings.xml \
       app/src/main/res/values-in/strings.xml \
       app/src/test/java/com/mistyislet/app/core/geofence/
git commit -m "feat: add GeofenceManager and BroadcastReceiver for proximity-based door notifications"
```

---

## Task 8: Wire Geofencing Into Repository + Profile

**Files:**
- Modify: `app/src/main/java/.../data/repository/DoorRepository.kt`
- Modify: `app/src/main/java/.../ui/profile/ProfileViewModel.kt`

- [ ] **Step 1: Inject GeofenceManager into DoorRepository**

Update `DoorRepository.kt` — add GeofenceManager injection and call after refresh:

```kotlin
@Singleton
class DoorRepository @Inject constructor(
    private val accessApi: AccessApi,
    private val doorDao: DoorDao,
    private val geofenceManager: GeofenceManager,
) {
```

Add import:

```kotlin
import com.mistyislet.app.core.geofence.GeofenceManager
```

In `refreshDoors()`, after `doorDao.insertAll(...)`, add:

```kotlin
            geofenceManager.syncGeofences(doors)
```

So the method becomes:

```kotlin
    suspend fun refreshDoors(): ApiResult<List<AccessibleDoor>> {
        return safeApiCall {
            val response = accessApi.getMyDoors()
            val doors = response.items
            doorDao.deleteAll()
            doorDao.insertAll(doors.map { it.toCache() })
            geofenceManager.syncGeofences(doors)
            doors
        }
    }
```

- [ ] **Step 2: Wire GeofenceManager into ProfileViewModel**

Update `ProfileViewModel.kt` — add GeofenceManager injection:

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val authRepository: AuthRepository,
    val biometricHelper: BiometricHelper,
    private val dataStore: DataStore<Preferences>,
    private val geofenceManager: GeofenceManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
```

Add import:

```kotlin
import com.mistyislet.app.core.geofence.GeofenceManager
```

Update `toggleGeofence()` to actually call the manager:

```kotlin
    fun toggleGeofence(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_GEOFENCE_ENABLED] = enabled }
            _uiState.value = _uiState.value.copy(geofenceEnabled = enabled)
            if (!enabled) {
                geofenceManager.clearAll()
            }
        }
    }
```

Update `logout()` to clear geofences:

```kotlin
    fun logout() {
        geofenceManager.clearAll()
        authRepository.logout()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }
```

- [ ] **Step 3: Build to verify**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug 2>&1 | tail -3`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/data/repository/DoorRepository.kt \
       app/src/main/java/com/mistyislet/app/ui/profile/ProfileViewModel.kt
git commit -m "feat: wire GeofenceManager into DoorRepository and ProfileViewModel"
```

---

## Task 9: Final Build + All Tests

- [ ] **Step 1: Run all unit tests**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew testDebugUnitTest 2>&1 | tail -10`

Expected: All tests pass. If any pre-existing test fails due to constructor changes (e.g., DoorRepository now requires GeofenceManager), update the test's mock/fake to provide the new dependency.

- [ ] **Step 2: Run full debug build**

Run: `cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug 2>&1 | tail -3`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Fix any issues and commit**

If any fixes were needed, commit them:

```bash
git add -A
git commit -m "fix: resolve test/build issues from P0 feature integration"
```
