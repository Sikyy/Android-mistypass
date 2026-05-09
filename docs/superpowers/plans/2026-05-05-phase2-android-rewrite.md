# Phase 2: Android Kisi-Style Full Rewrite

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite the Android MistyisletPass UI from a 4-tab flat nav to Kisi's hierarchical navigation (MyOrgs -> MyPlaces -> PlaceDetails -> AdminDashboard) with 57 screens, Kisi design system, and multi-org support.

**Architecture:** Keep existing MVVM + Hilt + Compose + Retrofit + Room infrastructure. Rewrite navigation graph, all screens, all ViewModels. Add new domain models, API interfaces, repositories. Apply Kisi design tokens (Inter font, #4A52FF, pure white, 28dp dialogs).

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Hilt, Retrofit 2, Room, Navigation Compose, kotlinx.serialization

**Repo:** `/Users/siky/code/android-MistyisletPass`

**Design Spec:** `docs/superpowers/specs/2026-05-05-kisi-style-full-refactor-design.md`

**Depends on:** Phase 1 backend deployed to staging

---

## Week 1: Core Infrastructure + Auth Flow

### Task 1: Design System - Colors, Typography, Shapes

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/mistyislet/app/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/mistyislet/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/theme/Shape.kt`
- Create: `app/src/main/res/font/inter_light.ttf`
- Create: `app/src/main/res/font/inter_regular.ttf`
- Create: `app/src/main/res/font/inter_medium.ttf`
- Create: `app/src/main/res/font/inter_semibold.ttf`

- [ ] **Step 1: Download Inter font files**

```bash
cd /Users/siky/code/android-MistyisletPass/app/src/main/res
mkdir -p font
# Download Inter from Google Fonts
curl -L "https://fonts.google.com/download?family=Inter" -o /tmp/inter.zip
unzip -o /tmp/inter.zip -d /tmp/inter
cp /tmp/inter/static/Inter_18pt-Light.ttf font/inter_light.ttf
cp /tmp/inter/static/Inter_18pt-Regular.ttf font/inter_regular.ttf
cp /tmp/inter/static/Inter_18pt-Medium.ttf font/inter_medium.ttf
cp /tmp/inter/static/Inter_18pt-SemiBold.ttf font/inter_semibold.ttf
```

- [ ] **Step 2: Replace Color.kt**

```kotlin
package com.mistyislet.app.ui.theme

import androidx.compose.ui.graphics.Color

// Kisi-aligned brand colors
val KisiBlue = Color(0xFF4A52FF)
val KisiWhite = Color(0xFFFFFFFF)
val KisiBlack = Color(0xFF191919)

// Semantic
val KisiSuccess = Color(0xFF35A853)
val KisiDanger = Color(0xFFD93025)
val KisiWarning = Color(0xFFD98B06)

// Neutral
val KisiGray50 = Color(0xFFF7F7F8)
val KisiGray200 = Color(0xFFE0E0E0)
val KisiGray500 = Color(0xFF6F717C)

// Aliases for Material 3 color scheme
val Primary = KisiBlue
val OnPrimary = KisiWhite
val PrimaryContainer = Color(0xFFE0E0FF)
val Background = KisiWhite
val Surface = KisiWhite
val OnSurface = KisiBlack
val OnSurfaceVariant = KisiGray500
val Outline = KisiGray200
val Success = KisiSuccess
val Danger = KisiDanger
val Warning = KisiWarning
```

- [ ] **Step 3: Replace Type.kt with Inter font family**

```kotlin
package com.mistyislet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mistyislet.app.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
```

- [ ] **Step 4: Create Shape.kt**

```kotlin
package com.mistyislet.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val KisiShapes = Shapes(
    small = RoundedCornerShape(8.dp),       // input fields
    medium = RoundedCornerShape(12.dp),     // cards, snackbars
    large = RoundedCornerShape(28.dp),      // dialogs, bottom sheets
    extraLarge = RoundedCornerShape(50.dp), // pill buttons
)

// Explicit tokens for clarity
val DialogCorner = 28.dp
val BottomSheetTopCorner = 28.dp
val ButtonPillCorner = 50.dp
val InputFieldCorner = 8.dp
val CardCorner = 12.dp
```

- [ ] **Step 5: Update Theme.kt**

```kotlin
package com.mistyislet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KisiLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Danger,
)

@Composable
fun MistyisletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KisiLightColorScheme,
        typography = Typography,
        shapes = KisiShapes,
        content = content,
    )
}
```

Note: Dark theme removed — Kisi uses light-only. Can be re-added later if needed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/theme/ app/src/main/res/font/
git commit -m "feat: apply Kisi design system — Inter font, #4A52FF blue, pure white background, 28dp shapes"
```

---

### Task 2: Domain Models (multi-org, auth, admin)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/domain/model/Organization.kt`
- Create: `app/src/main/java/com/mistyislet/app/domain/model/Place.kt`
- Create: `app/src/main/java/com/mistyislet/app/domain/model/AuthModels.kt`
- Create: `app/src/main/java/com/mistyislet/app/domain/model/AdminModels.kt`

- [ ] **Step 1: Create Organization.kt**

```kotlin
package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val domain: String,
    val logo: String? = null,
    val role: String = "resident",
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)
```

- [ ] **Step 2: Create Place.kt**

```kotlin
package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String,
    val name: String,
    val address: String = "",
    @SerialName("org_id") val orgId: String,
    @SerialName("is_lockdown") val isLockdown: Boolean = false,
    @SerialName("door_count") val doorCount: Int = 0,
)
```

- [ ] **Step 3: Create AuthModels.kt (new auth flow models)**

```kotlin
package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MagicLinkRequest(val email: String)

@Serializable
data class MagicLinkVerifyRequest(val token: String)

@Serializable
data class OrgAuthConfig(
    @SerialName("org_id") val orgId: String,
    val domain: String,
    val name: String,
    val logo: String? = null,
    val methods: List<String> = listOf("classic"),
)

@Serializable
data class SsoRedirectResponse(
    @SerialName("redirect_url") val redirectUrl: String,
)

@Serializable
data class TwoFactorRequest(
    @SerialName("user_id") val userId: String,
    val code: String,
    val type: String = "totp",
)

@Serializable
data class BackupCodeRequest(
    @SerialName("user_id") val userId: String,
    val code: String,
)

@Serializable
data class CreateAccountRequest(
    val name: String,
    val email: String,
    val password: String,
    val domain: String,
)

@Serializable
data class RestorePasswordRequest(val email: String)

@Serializable
data class OrgSwitchResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("org_id") val orgId: String,
)

@Serializable
data class MessageResponse(val message: String)
```

- [ ] **Step 4: Create AdminModels.kt**

```kotlin
package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Users
@Serializable
data class PlaceUser(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val role: String = "resident",
    @SerialName("last_activity") val lastActivity: String? = null,
)

@Serializable
data class UserRole(
    val scope: String,
    @SerialName("place_ids") val placeIds: List<String> = emptyList(),
    @SerialName("group_ids") val groupIds: List<String> = emptyList(),
    @SerialName("time_zone") val timeZone: String = "UTC",
)

@Serializable
data class AccessRight(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("door_id") val doorId: String,
    @SerialName("door_name") val doorName: String,
    val schedule: String? = null,
)

@Serializable
data class UserLogin(
    val id: String,
    @SerialName("device_name") val deviceName: String,
    val platform: String,
    @SerialName("last_active") val lastActive: String,
    @SerialName("is_current") val isCurrent: Boolean = false,
)

// Events
@Serializable
data class AccessEvent(
    val id: String,
    @SerialName("place_id") val placeId: String,
    val timestamp: String,
    val actor: String,
    val action: String,
    @SerialName("object_name") val objectName: String,
    @SerialName("object_type") val objectType: String,
)

@Serializable
data class EventFilter(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("object_type") val objectType: String? = null,
    @SerialName("object_action") val objectAction: String? = null,
    @SerialName("object_id") val objectId: String? = null,
)

@Serializable
data class RelatedEvent(
    val id: String,
    val timestamp: String,
    val description: String,
    @SerialName("event_type") val eventType: String,
)

@Serializable
data class EventMedia(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("camera_name") val cameraName: String,
    @SerialName("snapshot_url") val snapshotUrl: String,
    val datetime: String,
)

// Incidents
@Serializable
data class Incident(
    val id: String,
    @SerialName("place_id") val placeId: String,
    val type: String,
    val state: String,
    val status: String,
    val severity: String,
    val description: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class IncidentFilter(
    val state: String? = null,
    val type: String? = null,
    @SerialName("subject_type") val subjectType: String? = null,
    val status: String? = null,
    val severity: String? = null,
)

@Serializable
data class Occurrence(
    val id: String,
    @SerialName("incident_id") val incidentId: String,
    val events: List<AccessEvent> = emptyList(),
    val timestamp: String,
)

// Schedules
@Serializable
data class UnlockSchedule(
    val id: String,
    @SerialName("door_id") val doorId: String,
    @SerialName("schedule_type") val scheduleType: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
)

@Serializable
data class HolidayRegion(
    val id: String,
    val name: String,
    val country: String,
)

@Serializable
data class Holiday(
    val id: String,
    val name: String,
    val date: String,
    @SerialName("is_observed") val isObserved: Boolean = false,
)

// Zones
@Serializable
data class Zone(
    val id: String,
    @SerialName("place_id") val placeId: String,
    val name: String,
    val description: String = "",
    val status: String = "active",
    @SerialName("door_ids") val doorIds: List<String> = emptyList(),
)

// Cards
@Serializable
data class CardAssignment(
    @SerialName("card_uid") val cardUid: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val status: String = "unassigned",
)

@Serializable
data class CardAssignRequest(
    @SerialName("card_uid") val cardUid: String,
    @SerialName("user_id") val userId: String,
)

// Digital Credentials
@Serializable
data class DigitalCredential(
    val id: String,
    val type: String,
    @SerialName("recipient_email") val recipientEmail: String? = null,
    @SerialName("door_ids") val doorIds: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("usage_count") val usageCount: Int = 0,
)

@Serializable
data class CreateDigitalCredentialRequest(
    val type: String,
    @SerialName("recipient_email") val recipientEmail: String? = null,
    @SerialName("door_ids") val doorIds: List<String>,
    @SerialName("expires_at") val expiresAt: String? = null,
)

// Teams
@Serializable
data class Team(
    val id: String,
    val name: String,
    @SerialName("member_count") val memberCount: Int = 0,
)
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/domain/model/
git commit -m "feat: add domain models for multi-org, auth flows, and admin features"
```

---

### Task 3: API Interfaces (new Retrofit interfaces)

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/data/api/AuthApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/OrgApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceDoorApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceUserApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceEventApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceIncidentApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceScheduleApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceZoneApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceCardApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceCredentialApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceActivityApi.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/api/PlaceTeamApi.kt`

- [ ] **Step 1: Extend AuthApi.kt**

Add to existing `AuthApi` interface:

```kotlin
@POST("app/auth/magic-link")
suspend fun requestMagicLink(@Body request: MagicLinkRequest): MessageResponse

@POST("app/auth/magic-link/verify")
suspend fun verifyMagicLink(@Body request: MagicLinkVerifyRequest): LoginResponse

@GET("app/auth/org-lookup")
suspend fun lookupOrg(@Query("domain") domain: String): OrgAuthConfig

@GET("app/auth/org/{orgId}/methods")
suspend fun getOrgMethods(@Path("orgId") orgId: String): List<String>

@POST("app/auth/sso/{orgId}")
suspend fun initiateSso(@Path("orgId") orgId: String): SsoRedirectResponse

@POST("app/auth/2fa/verify")
suspend fun verify2FA(@Body request: TwoFactorRequest): LoginResponse

@POST("app/auth/2fa/backup")
suspend fun verifyBackupCode(@Body request: BackupCodeRequest): LoginResponse

@POST("app/auth/register")
suspend fun createAccount(@Body request: CreateAccountRequest): LoginResponse

@POST("app/auth/restore-password")
suspend fun restorePassword(@Body request: RestorePasswordRequest): MessageResponse
```

- [ ] **Step 2: Create OrgApi.kt**

```kotlin
package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.Organization
import com.mistyislet.app.domain.model.OrgSwitchResponse
import com.mistyislet.app.domain.model.Place
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrgApi {
    @GET("app/orgs")
    suspend fun listOrgs(): List<Organization>

    @POST("app/orgs/{orgId}/switch")
    suspend fun switchOrg(@Path("orgId") orgId: String): OrgSwitchResponse
}
```

- [ ] **Step 3: Create PlaceApi.kt**

```kotlin
package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.Place
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {
    @GET("app/orgs/{orgId}/places")
    suspend fun listPlaces(@Path("orgId") orgId: String): List<Place>

    @GET("app/orgs/{orgId}/places/search")
    suspend fun searchPlaces(@Path("orgId") orgId: String, @Query("q") query: String): List<Place>
}
```

- [ ] **Step 4: Create PlaceDoorApi.kt**

```kotlin
package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import com.mistyislet.app.domain.model.QRUnlockRequest
import retrofit2.http.*

interface PlaceDoorApi {
    @GET("app/places/{placeId}/doors")
    suspend fun listDoors(@Path("placeId") placeId: String): ListResponse<AccessibleDoor>

    @GET("app/places/{placeId}/doors/search")
    suspend fun searchDoors(@Path("placeId") placeId: String, @Query("q") query: String): ListResponse<AccessibleDoor>

    @POST("app/places/{placeId}/doors/{doorId}/unlock")
    suspend fun unlock(@Path("placeId") placeId: String, @Path("doorId") doorId: String, @Body request: UnlockRequest): UnlockResponse

    @POST("app/places/{placeId}/doors/{doorId}/qr-unlock")
    suspend fun qrUnlock(@Path("placeId") placeId: String, @Path("doorId") doorId: String, @Body request: QRUnlockRequest): UnlockResponse

    @PUT("app/places/{placeId}/doors/{doorId}/favorite")
    suspend fun favoriteDoor(@Path("placeId") placeId: String, @Path("doorId") doorId: String)

    @DELETE("app/places/{placeId}/doors/{doorId}/favorite")
    suspend fun unfavoriteDoor(@Path("placeId") placeId: String, @Path("doorId") doorId: String)

    @POST("app/places/{placeId}/lockdown")
    suspend fun enableLockdown(@Path("placeId") placeId: String)

    @DELETE("app/places/{placeId}/lockdown")
    suspend fun disableLockdown(@Path("placeId") placeId: String)
}
```

- [ ] **Step 5: Create remaining admin API interfaces**

Create one file per interface following the same pattern. Each maps 1:1 to the endpoints defined in the design spec Section 3.4. Use `@Path("placeId")` for place scoping, `@Query` for filters/pagination, `@Body` for create/update requests.

Files to create:
- `PlaceUserApi.kt` — listUsers, searchUsers, getUser, addUser, updateRole, getUserLogins, getAccessRights, shareAccess
- `PlaceEventApi.kt` — listEvents, getEvent, getRelatedEvents, getEventMedia
- `PlaceIncidentApi.kt` — listIncidents, getIncident, getOccurrenceEvents
- `PlaceScheduleApi.kt` — listSchedules, createSchedule, updateSchedule, deleteSchedule, getHolidayRegions, getHolidays
- `PlaceZoneApi.kt` — listZones, getZone
- `PlaceCardApi.kt` — listCards, assignCard, unassignCard, getCardStatus, manualToken
- `PlaceCredentialApi.kt` — listCredentials, createCredential, getDetails, searchCredentials
- `PlaceActivityApi.kt` — getUserActivity, getPresenceEvent
- `PlaceTeamApi.kt` — listTeams, createTeam, getTeam, updateTeam, deleteTeam

- [ ] **Step 6: Register all new APIs in ApiClient.kt**

Add `@Provides @Singleton` methods in `ApiClientModule` for each new interface, using the authenticated `Retrofit` instance.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/data/api/ app/src/main/java/com/mistyislet/app/core/network/ApiClient.kt
git commit -m "feat: add Retrofit interfaces for org, place, and admin APIs"
```

---

### Task 4: Multi-Session TokenStore + Room Migration

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/core/storage/TokenStore.kt`
- Modify: `app/src/main/java/com/mistyislet/app/core/storage/AppDatabase.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/dao/OrgDao.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/dao/PlaceDao.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/dao/SessionDao.kt`

- [ ] **Step 1: Extend TokenStore interface**

```kotlin
interface TokenStore {
    var accessToken: String?
    var refreshToken: String?
    var expiresAt: Long
    fun clear()
    fun isValid(): Boolean

    // Multi-session
    var activeOrgId: String?
    fun saveSession(orgId: String, orgName: String, accessToken: String, refreshToken: String, expiresIn: Long, userId: String, userEmail: String)
    fun getSession(orgId: String): SessionData?
    fun getAllSessionOrgIds(): List<String>
    fun removeSession(orgId: String)
}

data class SessionData(
    val orgId: String,
    val orgName: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val userEmail: String,
)
```

- [ ] **Step 2: Implement multi-session in EncryptedTokenStore**

Use EncryptedSharedPreferences with org-prefixed keys: `"session_{orgId}_access_token"`, etc. `activeOrgId` tracks which org is current. `accessToken` getter/setter delegates to the active org's session.

- [ ] **Step 3: Add Room entities and DAOs**

`OrgDao.kt`:
```kotlin
@Entity(tableName = "cached_organizations")
data class CachedOrganization(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String?,
    val domain: String,
    val role: String,
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Dao
interface OrgDao {
    @Query("SELECT * FROM cached_organizations ORDER BY name")
    suspend fun getAll(): List<CachedOrganization>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orgs: List<CachedOrganization>)

    @Query("DELETE FROM cached_organizations")
    suspend fun deleteAll()
}
```

`PlaceDao.kt`:
```kotlin
@Entity(tableName = "cached_places")
data class CachedPlace(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val orgId: String,
    val isLockdown: Boolean,
    val doorCount: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM cached_places WHERE orgId = :orgId ORDER BY name")
    suspend fun getByOrg(orgId: String): List<CachedPlace>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<CachedPlace>)

    @Query("DELETE FROM cached_places WHERE orgId = :orgId")
    suspend fun deleteByOrg(orgId: String)
}
```

- [ ] **Step 4: Modify existing Room entities for place-scoping**

Add `placeId` and `orgId` columns to `CachedDoor`:

```kotlin
@Entity(tableName = "cached_doors")
data class CachedDoor(
    @PrimaryKey val id: String,
    // ... existing fields ...
    val placeId: String = "",   // new: place scope
    val orgId: String = "",     // new: org scope
)
```

Add `placeId` column to `CachedAccessLog`:

```kotlin
@Entity(tableName = "cached_access_logs")
data class CachedAccessLog(
    @PrimaryKey val id: String,
    // ... existing fields ...
    val placeId: String = "",   // new: place scope
)
```

Update `DoorDao` queries to support filtering by `placeId`:

```kotlin
@Query("SELECT * FROM cached_doors WHERE placeId = :placeId ORDER BY name")
suspend fun getByPlace(placeId: String): List<CachedDoor>
```

- [ ] **Step 5: Update AppDatabase**

Bump version to 3, add new entities and DAOs, keep `fallbackToDestructiveMigration()`.

```kotlin
@Database(
    entities = [CachedDoor::class, CachedCredential::class, CachedAccessLog::class,
                CachedOrganization::class, CachedPlace::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doorDao(): DoorDao
    abstract fun credentialDao(): CredentialDao
    abstract fun accessLogDao(): AccessLogDao
    abstract fun orgDao(): OrgDao
    abstract fun placeDao(): PlaceDao
}
```

Add `@Provides` for new DAOs in `DatabaseModule`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/core/storage/ app/src/main/java/com/mistyislet/app/data/dao/
git commit -m "feat: multi-session TokenStore + Room entities for orgs and places"
```

---

### Task 5: New Repositories (Org, Place, Admin)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/data/repository/OrgRepository.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/repository/PlaceRepository.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/repository/AdminUserRepository.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/repository/AdminEventRepository.kt`
- Create: `app/src/main/java/com/mistyislet/app/data/repository/AdminResourceRepository.kt`
- Modify: `app/src/main/java/com/mistyislet/app/data/repository/AuthRepository.kt`
- Modify: `app/src/main/java/com/mistyislet/app/data/repository/DoorRepository.kt`

- [ ] **Step 1: Create OrgRepository.kt**

```kotlin
@Singleton
class OrgRepository @Inject constructor(
    private val orgApi: OrgApi,
    private val orgDao: OrgDao,
    private val tokenStore: TokenStore,
) {
    suspend fun listOrgs(): ApiResult<List<Organization>> = safeApiCall {
        orgApi.listOrgs().also { orgs ->
            orgDao.deleteAll()
            orgDao.insertAll(orgs.map { it.toCached() })
        }
    }

    suspend fun switchOrg(orgId: String): ApiResult<OrgSwitchResponse> = safeApiCall {
        val response = orgApi.switchOrg(orgId)
        tokenStore.saveSession(orgId, /* ... */)
        tokenStore.activeOrgId = orgId
        response
    }

    suspend fun getCachedOrgs(): List<Organization> =
        orgDao.getAll().map { it.toDomain() }
}
```

- [ ] **Step 2: Create PlaceRepository.kt**

Similar pattern — fetch from API, cache to Room, provide cached fallback.

- [ ] **Step 3: Create admin repositories**

`AdminUserRepository`, `AdminEventRepository`, `AdminResourceRepository` — each wraps the corresponding API interface with `safeApiCall`. No Room caching needed for admin data (always fresh).

- [ ] **Step 4: Update AuthRepository for multi-session**

Add magic link, org lookup, 2FA, registration methods. Update `login()` to store session per-org. Add `logout()` to remove active session.

- [ ] **Step 5: Update DoorRepository for place-scoping**

Add `placeId` parameter to `fetchDoors()`. Switch from `AccessApi.getMyDoors()` to `PlaceDoorApi.listDoors(placeId)`. Update `CachedDoor` queries to filter by placeId.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/data/repository/
git commit -m "feat: add org, place, and admin repositories with place-scoped data access"
```

---

### Task 6: Navigation Graph Rewrite

**Files:**
- Rewrite: `app/src/main/java/com/mistyislet/app/ui/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/Routes.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/AuthNavGraph.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/MainNavGraph.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/AdminNavGraph.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/SettingsNavGraph.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/navigation/NavTransitions.kt`

- [ ] **Step 1: Create Routes.kt (type-safe route definitions)**

```kotlin
package com.mistyislet.app.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    // Auth
    @Serializable data object SignInLinkRequest : AppRoute
    @Serializable data object SignInDomain : AppRoute
    @Serializable data class SignInClassic(val domain: String, val orgName: String, val orgLogo: String?) : AppRoute
    @Serializable data class SignInSso(val orgId: String) : AppRoute
    @Serializable data class SignInChallenge(val userId: String, val type: String) : AppRoute
    @Serializable data object SignInBackupCode : AppRoute
    @Serializable data object CreateAccount : AppRoute
    @Serializable data object RestorePassword : AppRoute
    @Serializable data class AuthenticationStatus(val token: String) : AppRoute

    // Main
    @Serializable data object MyOrgs : AppRoute
    @Serializable data object MyPlaces : AppRoute
    @Serializable data object SearchPlaces : AppRoute
    @Serializable data class PlaceDetails(val placeId: String) : AppRoute
    @Serializable data class SearchDoors(val placeId: String) : AppRoute
    @Serializable data class TamperedReaders(val placeId: String) : AppRoute

    // Admin
    @Serializable data class AdminDashboard(val placeId: String) : AppRoute
    @Serializable data class ManageUsers(val placeId: String) : AppRoute
    @Serializable data class SearchUsers(val placeId: String) : AppRoute
    @Serializable data class UserDetails(val placeId: String, val userId: String) : AppRoute
    @Serializable data class AddUsers(val placeId: String) : AppRoute
    @Serializable data class AccessRights(val placeId: String, val userId: String) : AppRoute
    @Serializable data class EventsHistory(val placeId: String) : AppRoute
    @Serializable data class EventDetails(val placeId: String, val eventId: String) : AppRoute
    @Serializable data class IncidentsList(val placeId: String) : AppRoute
    @Serializable data class IncidentDetails(val placeId: String, val incidentId: String) : AppRoute
    @Serializable data class UnlockSchedules(val placeId: String) : AppRoute
    @Serializable data class ZonesList(val placeId: String) : AppRoute
    @Serializable data class ZoneDetails(val placeId: String, val zoneId: String) : AppRoute
    @Serializable data class CardAssignment(val placeId: String) : AppRoute
    @Serializable data class DigitalCredentials(val placeId: String) : AppRoute
    @Serializable data class UserActivity(val placeId: String) : AppRoute

    // Settings
    @Serializable data object UserSettings : AppRoute
    @Serializable data object PasswordSettings : AppRoute
    @Serializable data object Onboarding : AppRoute
    @Serializable data object AppInfo : AppRoute
    @Serializable data class WebView(val url: String) : AppRoute
}
```

- [ ] **Step 2: Create NavTransitions.kt (Kisi-style animations)**

```kotlin
package com.mistyislet.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

val slideForwardEnter = slideInHorizontally(tween(300)) { it }
val slideForwardExit = slideOutHorizontally(tween(300)) { -it }
val slideBackEnter = slideInHorizontally(tween(300)) { -it }
val slideBackExit = slideOutHorizontally(tween(300)) { it }
val fadeEnter = fadeIn(tween(200))
val fadeExit = fadeOut(tween(200))
```

- [ ] **Step 3: Create subgraph files and main AppNavigation**

`AuthNavGraph.kt` — registers all auth routes (SignInLinkRequest → SignInDomain → SignInClassic → etc.)
`MainNavGraph.kt` — registers MyOrgs, MyPlaces, PlaceDetails, SearchDoors
`AdminNavGraph.kt` — registers AdminDashboard and all admin sub-routes
`SettingsNavGraph.kt` — registers UserSettings and sub-settings

`AppNavigation.kt` — root NavHost that composes all subgraphs:

```kotlin
@Composable
fun AppNavigation(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val startRoute = if (authRepository.isLoggedIn()) AppRoute.MyOrgs else AppRoute.SignInLinkRequest

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { slideForwardEnter },
        exitTransition = { slideForwardExit },
        popEnterTransition = { slideBackEnter },
        popExitTransition = { slideBackExit },
    ) {
        authNavGraph(navController)
        mainNavGraph(navController)
        adminNavGraph(navController)
        settingsNavGraph(navController)
    }
}
```

No more `Scaffold` with `NavigationBar` — the bottom bar is gone.

- [ ] **Step 4: Update MainActivity.kt**

Replace `MainScreen` with `AppNavigation` using the new hierarchical nav.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/navigation/ app/src/main/java/com/mistyislet/app/MainActivity.kt
git commit -m "feat: rewrite navigation to Kisi hierarchical model — remove bottom bar, add org/place/admin graphs"
```

---

### Task 7: Auth Screens (12 screens)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/auth/` (new package for all auth screens)
  - `SignInLinkRequestScreen.kt`
  - `SignInDomainScreen.kt`
  - `SignInMethodPickerSheet.kt`
  - `SignInClassicScreen.kt` (rewrite from LoginScreen)
  - `SignInSsoScreen.kt`
  - `SignInChallengeScreen.kt`
  - `SignInBackupCodeScreen.kt`
  - `CreateAccountScreen.kt`
  - `RestorePasswordScreen.kt`
  - `AuthenticationStatusScreen.kt`
  - `DontKnowDomainDialog.kt`
  - `AuthViewModel.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/auth/MyOrgsScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/auth/MyOrgsViewModel.kt`

- [ ] **Step 1: Create AuthViewModel.kt**

Manages the full auth flow state: email input, domain lookup, org config, login, 2FA, magic link. Single ViewModel for the auth graph since screens share state.

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orgRepository: OrgRepository,
) : ViewModel() {
    var email by mutableStateOf("")
    var domain by mutableStateOf("")
    var password by mutableStateOf("")
    var orgConfig by mutableStateOf<OrgAuthConfig?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var twoFactorUserId by mutableStateOf<String?>(null)

    fun requestMagicLink(onSuccess: () -> Unit) { /* call authRepository */ }
    fun lookupOrg(onResult: (OrgAuthConfig) -> Unit) { /* call authRepository.lookupOrg */ }
    fun login(onSuccess: () -> Unit, on2FA: (String, String) -> Unit) { /* ... */ }
    fun verify2FA(code: String, onSuccess: () -> Unit) { /* ... */ }
    fun createAccount(name: String, onSuccess: () -> Unit) { /* ... */ }
    fun restorePassword(onSuccess: () -> Unit) { /* ... */ }
}
```

- [ ] **Step 2: Create SignInLinkRequestScreen.kt (Kisi primary login)**

Layout matches Kisi screenshot exactly:
- Top: brand mark (24dp)
- Title: "Enter your email" (displayLarge, Inter Light)
- Subtitle: "Enter your email address to request a sign-in link" (bodyLarge, gray)
- Input: outlined email field with Kisi styling
- Bottom bar: "Manual sign in" (text link, left) + "Continue" (pill button, right)
- Massive whitespace in center

- [ ] **Step 3: Create SignInDomainScreen.kt**

Layout matches Kisi screenshot:
- Top: back arrow
- Title: "Sign in to your organization" (displayLarge)
- Subtitle: "Please enter your organization domain below"
- Input: "Organization domain" outlined field
- Bottom bar: "I don't know my domain" (text link) + "Continue" (pill)

- [ ] **Step 4: Create SignInClassicScreen.kt (rewrite from LoginScreen)**

Layout matches Kisi screenshot:
- Top: back arrow
- Title: "Sign in to your organization"
- Subtitle: "Please enter your email address and password"
- Org avatar + domain name
- Email input + Password input (with visibility toggle)
- Bottom: "Forgot password?" (text link) + "Sign in" (pill)
- Preserve existing LoginScreen's environment switcher as hidden long-press on back arrow (dev only)

- [ ] **Step 5: Create remaining auth screens**

All follow the same Kisi layout pattern (big title top, content middle, actions bottom):
- `SignInMethodPickerSheet.kt` — BottomSheet with 28dp top corners, org avatar, method list
- `SignInSsoScreen.kt` — WebView loading SSO redirect URL
- `SignInChallengeScreen.kt` — 6-digit OTP input, "Use backup code" link
- `SignInBackupCodeScreen.kt` — Single text input for backup code
- `CreateAccountScreen.kt` — Name/email/password form + legal links (ToS, Privacy, EULA)
- `RestorePasswordScreen.kt` — Email input + send button
- `AuthenticationStatusScreen.kt` — Success/failure/expired state display
- `DontKnowDomainDialog.kt` — AlertDialog with 28dp corners

- [ ] **Step 6: Create MyOrgsScreen.kt + MyOrgsViewModel.kt**

Org list with each item showing org name + avatar. Bottom: "Add Account" button → navigates to SignInLinkRequest.

- [ ] **Step 7: Register all auth screens in AuthNavGraph.kt**

- [ ] **Step 8: Delete old LoginScreen.kt and LoginViewModel.kt**

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/auth/
git rm app/src/main/java/com/mistyislet/app/ui/login/LoginScreen.kt
git rm app/src/main/java/com/mistyislet/app/ui/login/LoginViewModel.kt
git commit -m "feat: add 12 auth screens with Kisi layout — magic link, domain lookup, classic, 2FA, registration"
```

---

## Week 2: Places + Doors + Unlock

### Task 8: MyPlaces Screen + PlaceDetails Screen (core experience)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/places/MyPlacesScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/MyPlacesViewModel.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/SearchPlacesScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/PlaceDetailsScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/PlaceDetailsViewModel.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/SearchDoorsScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/DoorDetailsSheet.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/TamperedReadersScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/places/components/` (reusable composables)

- [ ] **Step 1: Create PlaceDetailsScreen (the core screen)**

This is the most important screen — where users unlock doors. It reuses unlock logic from current DoorsScreen but with Kisi layout:

- Top bar: place name + admin button (gear) + settings button + search button
- Tab row: "All Doors" / "Favorites" (two tabs)
- Banner system (conditional): Bluetooth missing, Location missing, No internet
- Door list (LazyColumn): door name + status icon + unlock button
- Lockdown banner (if active)
- Hold-to-unlock gesture (reuse from DoorsViewModel)
- UnlockResultDialog (reuse existing)

PlaceDetailsViewModel extends current DoorsViewModel with placeId scoping.

- [ ] **Step 2: Create MyPlacesScreen**

- Top bar: org name + search icon + settings icon + shortcuts icon
- Favorites section (pinned places)
- All places list
- Pull-to-refresh

- [ ] **Step 3: Create search screens (fade transitions)**

SearchPlacesScreen and SearchDoorsScreen follow same pattern: search bar at top, results list below. Use `fadeEnter`/`fadeExit` transitions.

- [ ] **Step 4: Create DoorDetailsSheet**

BottomSheet (28dp top corners) showing door info, unlock schedule link, status details.

- [ ] **Step 5: Delete old DoorsScreen, ScannerScreen, PassesScreen, HistoryScreen, ProfileScreen**

These are all replaced by the new hierarchy.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/places/
git rm app/src/main/java/com/mistyislet/app/ui/doors/DoorsScreen.kt
git rm app/src/main/java/com/mistyislet/app/ui/doors/DoorsViewModel.kt
git rm app/src/main/java/com/mistyislet/app/ui/scanner/
git rm app/src/main/java/com/mistyislet/app/ui/passes/
git rm app/src/main/java/com/mistyislet/app/ui/history/
git rm app/src/main/java/com/mistyislet/app/ui/profile/
git commit -m "feat: add places and doors screens with Kisi hierarchy — replace flat 4-tab navigation"
```

---

## Week 3: Admin Dashboard

### Task 9: Admin Dashboard Hub

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/AdminDashboardScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/AdminDashboardViewModel.kt`

- [ ] **Step 1: Build AdminDashboardScreen**

Menu list with 11 items (matching Kisi's dashboard). Top: Lockdown toggle switch. Each item: icon + title + subtitle, navigates to respective admin screen.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/admin/
git commit -m "feat: add admin dashboard with 11-item menu and lockdown toggle"
```

---

### Task 10: Admin User Management Screens (8 screens)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/users/`
  - `AddUsersScreen.kt`, `RoleDataPickerScreen.kt`
  - `ManageUsersScreen.kt`, `SearchUsersScreen.kt`, `UserDetailsScreen.kt`
  - `AccessRightsScreen.kt`, `ShareAccessScreen.kt`, `UserLoginsScreen.kt`
  - `AdminUsersViewModel.kt`
  - `GroupPickerSheet.kt`, `PlacePickerSheet.kt`, `TimeZonePickerSheet.kt`

All follow standard list/detail patterns. ManageUsers = list + search. UserDetails = info card + action buttons.

- [ ] **Step 1: Implement all 8 screens + 3 picker sheets + ViewModel**
- [ ] **Step 2: Register in AdminNavGraph**
- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/admin/users/
git commit -m "feat: add admin user management screens — add, manage, details, access rights"
```

---

### Task 11: Admin Events & Incidents Screens (12 screens)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/events/`
  - `EventsHistoryScreen.kt`, `EventDetailsScreen.kt`, `RelatedEventsScreen.kt`, `EventMediaScreen.kt`
  - `EventFiltersSheet.kt` + 4 sub-filter sheets
  - `AdminEventsViewModel.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/incidents/`
  - `IncidentsListScreen.kt`, `IncidentDetailsScreen.kt`, `OccurrenceEventsScreen.kt`
  - `IncidentFiltersSheet.kt`
  - `AdminIncidentsViewModel.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/activity/`
  - `UserActivityScreen.kt`, `PresenceEventScreen.kt`

EventsHistoryScreen reuses pagination logic from current HistoryScreen. Filters are BottomSheets with selectable option lists.

- [ ] **Step 1: Implement events screens + filters**
- [ ] **Step 2: Implement incidents screens + filters**
- [ ] **Step 3: Implement activity screens**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/admin/events/ app/src/main/java/com/mistyislet/app/ui/admin/incidents/ app/src/main/java/com/mistyislet/app/ui/admin/activity/
git commit -m "feat: add admin events, incidents, and activity screens with filter bottom sheets"
```

---

### Task 12: Admin Schedules, Zones, Cards, Credentials, Teams (15 screens)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/schedules/`
  - `UnlockSchedulesScreen.kt`, `EditUnlockScheduleDialog.kt`, `HolidayRegionPickerSheet.kt`, `HolidayPickerSheet.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/zones/`
  - `ZonesListScreen.kt`, `ZoneDetailsScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/cards/`
  - `CardAssignmentScreen.kt`, `CardStatusDialog.kt`, `UserPickerSheet.kt`, `ManualTokenEntryScreen.kt`
- Create: `app/src/main/java/com/mistyislet/app/ui/admin/credentials/`
  - `DigitalCredentialsListScreen.kt`, `CreateDigitalCredentialScreen.kt`, `SendCredentialSheet.kt`, `CredentialDetailsScreen.kt`, `DigitalCredentialsSearchScreen.kt`, `SendCredentialOptionsSheet.kt`

CardAssignmentScreen reuses NFC logic from current BindCardScreen. DigitalCredentials screens repurpose logic from current CredentialsScreen and VisitorsScreen.

- [ ] **Step 1: Implement schedules screens**
- [ ] **Step 2: Implement zones screens**
- [ ] **Step 3: Implement cards screens (reuse NFCReader)**
- [ ] **Step 4: Implement digital credentials screens (repurpose visitor/credential logic)**
- [ ] **Step 5: Delete old screens that were repurposed**

```bash
git rm app/src/main/java/com/mistyislet/app/ui/credentials/
git rm app/src/main/java/com/mistyislet/app/ui/visitors/
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/admin/
git commit -m "feat: add admin schedules, zones, cards, and digital credentials screens"
```

---

## Week 4: Settings + Polish + Integration

### Task 13: Settings Screens (6 screens)

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/settings/`
  - `SettingsRootScreen.kt`, `PasswordSettingsScreen.kt`, `OnboardingScreen.kt`
  - `MotionSenseSettingsScreen.kt`, `AppInfoScreen.kt`, `WebViewScreen.kt`
  - `SettingsViewModel.kt`

SettingsRootScreen: 8-item menu list matching Kisi (Password, Onboarding, Hand Wave, About, Help, Ambassador, Acknowledgments, Sign out). Reuses settings logic from current ProfileViewModel.

- [ ] **Step 1: Implement all 6 settings screens**
- [ ] **Step 2: Register in SettingsNavGraph**
- [ ] **Step 3: Delete old ProfileScreen**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mistyislet/app/ui/settings/
git commit -m "feat: add settings screens with Kisi menu layout"
```

---

### Task 14: Common Screens + Cleanup

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/ui/common/ShortcutsDialog.kt`
- Delete: `app/src/main/java/com/mistyislet/app/ui/admin/AdminPanelScreen.kt` (old debug panel)

- [ ] **Step 1: Create ShortcutsDialog**
- [ ] **Step 2: Delete all old unused screens and ViewModels**
- [ ] **Step 3: Verify all imports and references compile**

```bash
cd /Users/siky/code/android-MistyisletPass
./gradlew assembleDebug 2>&1 | tail -20
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add shortcuts dialog, delete all legacy screens, verify build"
```

---

### Task 15: Integration Testing

- [ ] **Step 1: Verify auth flow end-to-end**

Launch app on emulator → SignInLinkRequest → Manual sign in → enter domain → SignInClassic → login → MyOrgs → select org → MyPlaces → select place → PlaceDetails with doors.

- [ ] **Step 2: Verify unlock flow**

Hold-to-unlock on a door → biometric gate → BLE/remote unlock → result dialog.

- [ ] **Step 3: Verify admin flow**

PlaceDetails → admin dashboard → manage users → search → user details. Events history with filters. Schedules view.

- [ ] **Step 4: Verify offline mode**

Enable airplane mode → app shows cached doors → unlock shows error gracefully.

- [ ] **Step 5: Verify iOS backward compatibility**

iOS app still works on same staging backend (old endpoints still functional).

- [ ] **Step 6: Commit integration test notes**

```bash
git commit --allow-empty -m "test: verify end-to-end auth, unlock, admin, offline, and iOS backward compat"
```

---

### Task 16: String Resources + i18n Update

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (English)
- Modify: `app/src/main/res/values-zh-rCN/strings.xml` (Chinese)
- Modify: `app/src/main/res/values-in/strings.xml` (Indonesian)

- [ ] **Step 1: Add all new string resources**

Add strings for all 57 new screens: titles, subtitles, button labels, error messages, empty states. Organize by screen group (auth, places, admin, settings).

- [ ] **Step 2: Translate to zh-CN and id-ID**

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values*/strings.xml
git commit -m "feat: add i18n strings for all 57 screens in en, zh-CN, id-ID"
```
