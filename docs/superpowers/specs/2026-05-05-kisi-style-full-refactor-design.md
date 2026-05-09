# Kisi-Style Full Refactor Design Spec

**Date:** 2026-05-05
**Scope:** Android (primary), Backend (supporting), iOS (follow-up)
**Approach:** Full UI rewrite with Kisi navigation hierarchy, real multi-org backend, full admin parity
**Repos:** `android-MistyisletPass`, `MistyPass` (backend), `ios-MistyisletPass`

---

## 1. Executive Summary

Refactor the Android MistyisletPass app from its current 4-tab flat navigation to Kisi's hierarchical navigation model: MyOrgs -> MyPlaces -> PlaceDetails -> AdminDashboard. Add real multi-org backend support, full admin features (11-item dashboard per place), and apply Kisi's visual design system (Inter font, #4A52FF blue, pure white background, 28dp dialog corners).

Execution order: Backend first (new endpoints) -> Android rewrite -> iOS migration -> Backend cleanup.

Existing core infrastructure (Hilt, Retrofit, Room, BLE, Keystore, FCM) is preserved. Only UI layer, ViewModels, navigation, and data models are rewritten.

---

## 2. Scope Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Navigation model | Full Kisi hierarchy (MyOrgs -> MyPlaces -> PlaceDetails) | User requirement |
| Multi-org backend | Real implementation (new endpoints, org-scoped tokens) | Required for Kisi nav model |
| Admin features | Full parity with Kisi (11 admin menu items, ~31 screens) | User requirement |
| Kisi hardware features | Skip BlinkUp, Intercom, Kiosk, Hand Wave | Kisi-specific hardware, not applicable |
| Migration strategy | Big bang: backend -> Android -> iOS | User requirement, Android-first |
| Existing infra | Preserve MVVM + Hilt + Compose + Retrofit + Room + BLE + Keystore | Solid, no reason to replace |

---

## 3. Backend API Changes

### 3.1 New Endpoints: Organization Layer

```
GET    /app/orgs                          -> [Organization]
POST   /app/orgs/{orgId}/switch           -> AuthTokens (org-scoped)
GET    /app/orgs/{orgId}/places           -> [Place]
```

### 3.2 New Endpoints: Auth Enhancements

```
POST   /app/auth/magic-link              -> {message}           # Send magic link email
POST   /app/auth/magic-link/verify       -> LoginResponse       # Verify magic link token
GET    /app/auth/org-lookup?domain=       -> OrgAuthConfig       # Look up org by domain
GET    /app/auth/org/{orgId}/methods      -> [AuthMethod]        # Available sign-in methods
POST   /app/auth/sso/{orgId}             -> {redirect_url}      # Initiate SSO
POST   /app/auth/2fa/verify              -> LoginResponse       # Verify TOTP/SMS
POST   /app/auth/2fa/backup              -> LoginResponse       # Verify backup code
POST   /app/auth/register                -> LoginResponse       # Create account
POST   /app/auth/restore-password        -> {message}           # Send password reset
```

### 3.3 New Endpoints: Place-Scoped Access

```
GET    /app/places/{placeId}/doors                -> [AccessibleDoor]
GET    /app/places/{placeId}/doors/search?q=      -> [AccessibleDoor]
POST   /app/places/{placeId}/doors/{doorId}/unlock -> UnlockResponse  # replaces /app/access/unlock
POST   /app/places/{placeId}/doors/{doorId}/qr-unlock -> UnlockResponse  # replaces /app/access/qr-unlock
POST   /app/places/{placeId}/lockdown             -> {status}
DELETE /app/places/{placeId}/lockdown              -> {status}
PUT    /app/places/{placeId}/doors/{doorId}/favorite    -> {}
DELETE /app/places/{placeId}/doors/{doorId}/favorite    -> {}
GET    /app/places/{placeId}/teams                 -> [Team]
GET    /app/places/{placeId}/teams/{teamId}        -> TeamDetail
POST   /app/places/{placeId}/teams                 -> Team
PUT    /app/places/{placeId}/teams/{teamId}        -> Team
DELETE /app/places/{placeId}/teams/{teamId}        -> {}
```

### 3.4 New Endpoints: Admin (Place-Scoped)

```
# User Management
GET    /app/places/{placeId}/users                -> [PlaceUser]
GET    /app/places/{placeId}/users/search?q=      -> [PlaceUser]
GET    /app/places/{placeId}/users/{userId}       -> PlaceUserDetail
POST   /app/places/{placeId}/users                -> PlaceUser
PUT    /app/places/{placeId}/users/{userId}/role   -> PlaceUser
GET    /app/places/{placeId}/users/{userId}/logins -> [UserLogin]
GET    /app/places/{placeId}/users/{userId}/access-rights -> [AccessRight]
POST   /app/places/{placeId}/users/{userId}/share-access  -> AccessRight

# Events
GET    /app/places/{placeId}/events               -> [AccessEvent]  (paginated, filterable)
GET    /app/places/{placeId}/events/{eventId}      -> AccessEventDetail
GET    /app/places/{placeId}/events/{eventId}/related -> [RelatedEvent]
GET    /app/places/{placeId}/events/{eventId}/media   -> [EventMedia]

# Incidents
GET    /app/places/{placeId}/incidents             -> [Incident]  (paginated, filterable)
GET    /app/places/{placeId}/incidents/{id}        -> IncidentDetail
GET    /app/places/{placeId}/incidents/{id}/occurrences -> [Occurrence]

# Schedules
GET    /app/places/{placeId}/schedules             -> [UnlockSchedule]
POST   /app/places/{placeId}/schedules             -> UnlockSchedule
PUT    /app/places/{placeId}/schedules/{id}        -> UnlockSchedule
DELETE /app/places/{placeId}/schedules/{id}        -> {}
GET    /app/places/{placeId}/holiday-regions       -> [HolidayRegion]
GET    /app/places/{placeId}/holiday-regions/{id}/holidays -> [Holiday]

# Zones
GET    /app/places/{placeId}/zones                 -> [Zone]
GET    /app/places/{placeId}/zones/{id}            -> ZoneDetail

# Cards
GET    /app/places/{placeId}/cards                 -> [CardAssignment]
POST   /app/places/{placeId}/cards/assign          -> CardAssignment
DELETE /app/places/{placeId}/cards/{cardUid}        -> {}
GET    /app/places/{placeId}/cards/{cardUid}/status -> CardStatus
POST   /app/places/{placeId}/cards/manual-token    -> CardAssignment

# Digital Credentials
GET    /app/places/{placeId}/credentials           -> [DigitalCredential]
POST   /app/places/{placeId}/credentials           -> DigitalCredential
GET    /app/places/{placeId}/credentials/{id}      -> DigitalCredentialDetail
GET    /app/places/{placeId}/credentials/search?q= -> [DigitalCredential]

# User Activity
GET    /app/places/{placeId}/activity              -> [UserPresence]
GET    /app/places/{placeId}/activity/{eventId}    -> PresenceEventDetail
```

### 3.5 Backwards Compatibility

- All existing `/app/access/*` and `/app/auth/login` endpoints remain functional
- Old tokens (no `org_id` claim) continue working on old endpoints
- New tokens (with `org_id` claim) work on both old and new endpoints
- Old endpoints get `Deprecated: true` response header
- Old endpoints removed only in Phase 4 after iOS migration complete

### 3.6 Token Changes

JWT payload gains `org_id` claim after org switch:
```json
{
  "sub": "user_123",
  "org_id": "org_456",
  "email": "user@example.com",
  "role": "admin",
  "exp": 1717620000
}
```

Backend scopes all place/door/user queries to `org_id` when present.

---

## 4. Navigation Architecture

### 4.1 Graph Structure

```
Auth NavGraph (unauthenticated)
├── signInLinkRequest (magic link email) ← START
├── signInDomain (org domain input)
├── signInMethodPicker (bottom sheet)
├── signInClassic (email + password)
├── signInSso (WebView)
├── signInChallenge (2FA)
├── signInBackupCode
├── createAccount
├── restorePassword
└── authenticationStatus (magic link result)

Main NavGraph (authenticated)
├── myOrgs ← START (multi-org)
├── myPlaces ← START (single-org)
│   └── searchPlaces
├── placeDetails/{placeId}
│   ├── searchDoors
│   ├── doorDetails/{doorId} (bottom sheet)
│   └── tamperedReaders
├── adminDashboard/{placeId}
│   ├── addUsers → roleDataPicker → groupPicker/placePicker/timeZonePicker
│   ├── manageUsers → searchUsers → userDetails → accessRights/shareAccess/userLogins
│   ├── digitalCredentials → create/send/details/search
│   ├── userActivity → presenceEvent
│   ├── eventsHistory → eventFilters (bottom sheet) → eventDetails → relatedEvents/eventMedia
│   ├── incidentsList → incidentFilters (bottom sheet) → incidentDetails → occurrenceEvents
│   ├── unlockSchedules → editSchedule (dialog) → regionPicker/holidayPicker
│   ├── zonesList → zoneDetails
│   └── cardAssignment → cardStatus (dialog) / manualTokenEntry → userPicker (dialog)
├── userSettings
│   ├── passwordSettings
│   ├── onboarding (horizontal pager)
│   ├── motionSenseSettings
│   ├── appInfo
│   └── webView
└── shortcuts (dialog)
```

### 4.2 Transitions

| Transition | Animation |
|------------|-----------|
| Forward navigation | slide_in_right + slide_out_left (300ms) |
| Back navigation | slide_in_left + slide_out_right (300ms) |
| Search screens | fade_in + fade_out (200ms) |
| Bottom sheets | slide up from bottom, 28dp top corners |
| Dialogs | fade in with scale (200ms) |
| WebView | slide_in_top |

### 4.3 Key Changes from Current App

- Bottom navigation bar removed entirely
- Top `TopAppBar` with back arrow, search icon, settings icon per context
- No tabs at root — hierarchical drill-down only
- Scanner becomes an action (FAB or toolbar icon) within PlaceDetails
- History moves to Admin > Events History
- Visitors moves to Admin > Digital Credentials
- Profile becomes Settings (toolbar icon access)

---

## 5. Design System

### 5.1 Colors

```kotlin
// Primary
val KisiBlue = Color(0xFF4A52FF)         // buttons, links, focused inputs
val KisiWhite = Color(0xFFFFFFFF)        // background, surface
val KisiBlack = Color(0xFF191919)        // primary text

// Semantic
val KisiSuccess = Color(0xFF35A853)      // unlock granted, online
val KisiDanger = Color(0xFFD93025)       // denied, error, offline
val KisiWarning = Color(0xFFD98B06)      // warnings

// Neutral
val KisiGray50 = Color(0xFFF7F7F8)      // subtle backgrounds (list headers)
val KisiGray200 = Color(0xFFE0E0E0)     // borders (unfocused), disabled buttons
val KisiGray500 = Color(0xFF6F717C)     // secondary text, placeholders
```

### 5.2 Typography

```kotlin
// Font: Inter (bundled in res/font/)
// Weights: Light (300), Regular (400), Medium (500), SemiBold (600)
// Number font: Roboto Medium (for OTP, countdowns)

DisplayLarge  = Inter Light,    36sp   // "Enter your email" (login titles)
TitleLarge    = Inter SemiBold, 22sp   // section headers
TitleMedium   = Inter Medium,   16sp   // card titles, door names
BodyLarge     = Inter Regular,  16sp   // body text, subtitles
BodyMedium    = Inter Regular,  14sp   // secondary text
LabelLarge    = Inter Medium,   14sp   // button text
LabelSmall    = Inter Medium,   11sp   // chips, badges
```

### 5.3 Shapes

```kotlin
DialogCorner     = 28.dp   // all corners
BottomSheetTop   = 28.dp   // top corners only
ButtonPill       = 50.dp   // full rounded pill
InputField       = 8.dp    // rounded corners, 1dp outlined border
Card             = 12.dp   // standard cards
Snackbar         = 12.dp
```

### 5.4 Layout Patterns

**Login screens (from Kisi screenshots):**
- Top padding: ~80dp from status bar to brand mark
- Brand mark: top-left, small (24dp height)
- Title: DisplayLarge, left-aligned, ~16dp below brand mark
- Subtitle: BodyLarge, KisiGray500, 8dp below title
- Input field: ~40dp below subtitle, full width with 24dp horizontal padding
- Bottom bar: pinned to bottom, 16dp padding — text link (left) + pill button (right)
- Massive center whitespace — intentional, pulls attention to top and bottom

**Input field style:**
- Outlined, 1dp KisiGray200 border
- On focus: border becomes KisiBlue, label floats above in KisiBlue
- 8dp corner radius, 16dp internal horizontal padding
- Placeholder: KisiGray500

**Button states:**
- Disabled: KisiGray200 background, KisiGray500 text
- Enabled: KisiBlue background, white text
- Pill shape (full rounded)

---

## 6. Screen Inventory

### 6.1 Auth Flow (12 screens)

| # | Screen | Type | Notes |
|---|--------|------|-------|
| 1 | MyOrgsScreen | New | Org list + "Add Account" button |
| 2 | SignInLinkRequestScreen | New | Magic link email entry (Kisi primary login) |
| 3 | SignInDomainScreen | New | Org domain input + "I don't know my domain" |
| 4 | SignInMethodPickerSheet | New | Bottom sheet: Classic / SSO / WebAuthn |
| 5 | SignInClassicScreen | Rewrite | From current LoginScreen, restyle to Kisi layout |
| 6 | SignInSsoScreen | New | WebView loading org's SSO page |
| 7 | SignInChallengeScreen | New | 2FA: 6-digit OTP input (TOTP or SMS) |
| 8 | SignInBackupCodeScreen | New | One-time backup code input |
| 9 | CreateAccountScreen | New | Registration form + legal links |
| 10 | RestorePasswordScreen | New | Email input for password reset |
| 11 | AuthenticationStatusScreen | New | Magic link result (success/fail/expired) |
| 12 | DontKnowDomainDialog | New | Alert dialog with email instructions |

### 6.2 Places & Doors (6 screens)

| # | Screen | Type | Notes |
|---|--------|------|-------|
| 13 | MyPlacesScreen | New | Place list + search + settings icons |
| 14 | SearchPlacesScreen | New | Fade transition, search bar + results |
| 15 | PlaceDetailsScreen | Rewrite | From DoorsScreen: add tabs (All/Favorites), banner system, lockdown |
| 16 | SearchDoorsScreen | New | Fade transition within place |
| 17 | DoorDetailsSheet | Rewrite | Bottom sheet with door info + schedule link |
| 18 | TamperedReadersScreen | New | Security: tampered reader list |

### 6.3 Admin Dashboard (31 screens)

| # | Screen | Type |
|---|--------|------|
| 19 | AdminDashboardScreen | New |
| 20 | AddUsersScreen | New |
| 21 | RoleDataPickerScreen | New |
| 22 | GroupPickerSheet | New |
| 23 | PlacePickerSheet | New |
| 24 | TimeZonePickerSheet | New |
| 25 | ManageUsersScreen | New |
| 26 | SearchUsersScreen | New |
| 27 | UserDetailsScreen | New |
| 28 | AccessRightsScreen | New |
| 29 | ShareAccessScreen | New |
| 30 | UserLoginsScreen | New |
| 31 | DigitalCredentialsListScreen | Repurpose (from CredentialsScreen) |
| 32 | SendCredentialOptionsSheet | New |
| 33 | CreateDigitalCredentialScreen | Repurpose (from VisitorsScreen create logic) |
| 34 | SendCredentialSheet | New |
| 35 | CredentialDetailsScreen | New |
| 36 | DigitalCredentialsSearchScreen | New |
| 37 | UserActivityScreen | New |
| 38 | PresenceEventScreen | New |
| 39 | EventsHistoryScreen | Repurpose (from HistoryScreen) |
| 40 | EventFiltersSheet | New |
| 41 | EventFilterUserPickerSheet | New |
| 42 | EventFilterObjectTypeSheet | New |
| 43 | EventFilterObjectActionSheet | New |
| 44 | EventFilterObjectSheet | New |
| 45 | EventDetailsScreen | New |
| 46 | RelatedEventsScreen | New |
| 47 | EventMediaScreen | New |
| 48 | IncidentsListScreen | New |
| 49 | IncidentFiltersSheet | New |
| 50 | IncidentDetailsScreen | New |
| 51 | OccurrenceEventsScreen | New |
| 52 | UnlockSchedulesScreen | New |
| 53 | EditUnlockScheduleDialog | New |
| 54 | HolidayRegionPickerSheet | New |
| 55 | HolidayPickerSheet | New |
| 56 | ZonesListScreen | New |
| 57 | ZoneDetailsScreen | New |
| 58 | CardAssignmentScreen | Repurpose (from BindCardScreen NFC logic) |
| 59 | CardStatusDialog | New |
| 60 | UserPickerSheet | New |
| 61 | ManualTokenEntryScreen | New |

### 6.4 Settings (6 screens)

| # | Screen | Type | Notes |
|---|--------|------|-------|
| 62 | SettingsRootScreen | Rewrite | From ProfileScreen, restructure as Kisi menu |
| 63 | PasswordSettingsScreen | New | Change password form |
| 64 | OnboardingScreen | New | HorizontalPager: Intro/Location/Notifications/NFC/Battery |
| 65 | MotionSenseSettingsScreen | New | Hand wave config |
| 66 | AppInfoScreen | New | Version, permissions, troubleshooting |
| 67 | WebViewScreen | Rewrite | Generic WebView for help/terms/ambassador |

### 6.5 Common (2 screens)

| # | Screen | Type | Notes |
|---|--------|------|-------|
| 68 | ShortcutsDialog | New | Quick access management |
| 69 | UnlockWidget | Keep | Current Glance widget unchanged |

### 6.6 Totals

| Category | Rewrite | New | Repurpose | Keep | Total |
|----------|---------|-----|-----------|------|-------|
| Auth | 1 | 11 | 0 | 0 | 12 |
| Places & Doors | 2 | 4 | 0 | 0 | 6 |
| Admin | 0 | 25 | 6 | 0 | 31 |
| Settings | 2 | 4 | 0 | 0 | 6 |
| Common | 0 | 1 | 0 | 1 | 2 |
| **Total** | **5** | **45** | **6** | **1** | **57** |

Plus ~20 filter/picker dialogs (small, pattern-repeating bottom sheets).

### 6.7 Deleted Screens

| Current Screen | Replacement |
|----------------|-------------|
| LoginScreen | SignInClassicScreen |
| DoorsScreen | PlaceDetailsScreen |
| ScannerScreen | Action within PlaceDetails |
| PassesScreen | Admin > DigitalCredentials |
| CredentialsScreen | Admin > DigitalCredentials |
| VisitorsScreen | Admin > DigitalCredentials (create flow) |
| HistoryScreen | Admin > EventsHistory |
| ProfileScreen | SettingsRootScreen |
| AdminPanelScreen | AdminDashboardScreen |
| BindCardScreen | CardAssignmentScreen |
| BottomNavigation | Deleted entirely |

---

## 7. Data Layer Changes

### 7.1 New Domain Models

```kotlin
// Multi-org
data class Organization(val id: String, val name: String, val logo: String?, val domain: String, val role: String)
data class Place(val id: String, val name: String, val address: String, val orgId: String, val isLockdown: Boolean, val doorCount: Int)

// Auth
data class OrgAuthConfig(val orgId: String, val domain: String, val name: String, val logo: String?, val methods: List<AuthMethod>)
enum class AuthMethod { CLASSIC, SSO, WEBAUTHN }
data class TwoFactorChallenge(val type: TwoFactorType, val userId: String)
enum class TwoFactorType { TOTP, SMS }

// Admin - Users
data class PlaceUser(val id: String, val name: String, val email: String, val avatar: String?, val role: String, val lastActivity: Instant?)
data class UserRole(val scope: String, val placeIds: List<String>, val groupIds: List<String>, val timeZone: String)
data class AccessRight(val id: String, val userId: String, val doorId: String, val doorName: String, val schedule: String?)
data class UserLogin(val id: String, val deviceName: String, val platform: String, val lastActive: Instant, val isCurrent: Boolean)

// Admin - Events
data class AccessEvent(val id: String, val placeId: String, val timestamp: Instant, val actor: String, val action: String, val objectName: String, val objectType: String)
data class EventFilter(val userId: String?, val objectType: String?, val objectAction: String?, val objectId: String?)
data class RelatedEvent(val id: String, val timestamp: Instant, val description: String, val eventType: String)
data class EventMedia(val id: String, val eventId: String, val cameraName: String, val snapshotUrl: String, val datetime: Instant)

// Admin - Incidents
data class Incident(val id: String, val placeId: String, val type: String, val state: String, val status: String, val severity: String, val description: String, val createdAt: Instant)
data class IncidentFilter(val state: String?, val type: String?, val subjectType: String?, val status: String?, val severity: String?)
data class Occurrence(val id: String, val incidentId: String, val events: List<AccessEvent>, val timestamp: Instant)

// Admin - Schedules
data class UnlockSchedule(val id: String, val doorId: String, val scheduleType: String, val startTime: String, val endTime: String, val daysOfWeek: List<Int>, val holidays: List<Holiday>)
data class HolidayRegion(val id: String, val name: String, val country: String)
data class Holiday(val id: String, val name: String, val date: LocalDate, val isObserved: Boolean)

// Admin - Zones & Cards
data class Zone(val id: String, val placeId: String, val name: String, val description: String, val status: String, val doorIds: List<String>)
data class CardAssignment(val cardUid: String, val userId: String?, val userName: String?, val status: String)

// Admin - Credentials
data class DigitalCredential(val id: String, val type: String, val recipientEmail: String?, val doorIds: List<String>, val expiresAt: Instant?, val createdAt: Instant, val usageCount: Int)
```

### 7.2 New API Interfaces

```kotlin
interface OrgApi {
    @GET("app/orgs") suspend fun listOrgs(): List<Organization>
    @POST("app/orgs/{orgId}/switch") suspend fun switchOrg(@Path("orgId") orgId: String): AuthTokens
}

interface PlaceApi {
    @GET("app/orgs/{orgId}/places") suspend fun listPlaces(@Path("orgId") orgId: String): List<Place>
    @GET("app/orgs/{orgId}/places/search") suspend fun searchPlaces(@Path("orgId") orgId: String, @Query("q") query: String): List<Place>
}

interface PlaceDoorApi {
    @GET("app/places/{placeId}/doors") suspend fun listDoors(@Path("placeId") placeId: String): List<AccessibleDoor>
    @GET("app/places/{placeId}/doors/search") suspend fun searchDoors(@Path("placeId") placeId: String, @Query("q") query: String): List<AccessibleDoor>
    @PUT("app/places/{placeId}/doors/{doorId}/favorite") suspend fun favoriteDoor(...)
    @DELETE("app/places/{placeId}/doors/{doorId}/favorite") suspend fun unfavoriteDoor(...)
    @POST("app/places/{placeId}/lockdown") suspend fun enableLockdown(...)
    @DELETE("app/places/{placeId}/lockdown") suspend fun disableLockdown(...)
}

interface PlaceUserApi { /* listUsers, searchUsers, getUser, addUser, updateRole, getUserLogins, getAccessRights, shareAccess */ }
interface PlaceEventApi { /* listEvents, getEvent, getRelatedEvents, getEventMedia */ }
interface PlaceIncidentApi { /* listIncidents, getIncident, getOccurrenceEvents */ }
interface PlaceScheduleApi { /* listSchedules, createSchedule, updateSchedule, deleteSchedule, getHolidayRegions, getHolidays */ }
interface PlaceZoneApi { /* listZones, getZone */ }
interface PlaceCardApi { /* listCards, assignCard, unassignCard, getCardStatus, manualToken */ }
interface PlaceCredentialApi { /* listCredentials, createCredential, getDetails, searchCredentials */ }
interface PlaceActivityApi { /* getUserActivity, getPresenceEvent */ }
```

### 7.3 Enhanced Auth API

```kotlin
interface AuthApi {
    // Existing (kept)
    @POST("app/auth/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @POST("app/auth/refresh") suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    // New
    @POST("app/auth/magic-link") suspend fun requestMagicLink(@Body request: MagicLinkRequest): MessageResponse
    @POST("app/auth/magic-link/verify") suspend fun verifyMagicLink(@Body request: MagicLinkVerifyRequest): LoginResponse
    @GET("app/auth/org-lookup") suspend fun lookupOrg(@Query("domain") domain: String): OrgAuthConfig
    @GET("app/auth/org/{orgId}/methods") suspend fun getOrgMethods(@Path("orgId") orgId: String): List<AuthMethod>
    @POST("app/auth/sso/{orgId}") suspend fun initiateSso(@Path("orgId") orgId: String): SsoRedirectResponse
    @POST("app/auth/2fa/verify") suspend fun verify2FA(@Body request: TwoFactorRequest): LoginResponse
    @POST("app/auth/2fa/backup") suspend fun verifyBackupCode(@Body request: BackupCodeRequest): LoginResponse
    @POST("app/auth/register") suspend fun createAccount(@Body request: CreateAccountRequest): LoginResponse
    @POST("app/auth/restore-password") suspend fun restorePassword(@Body request: RestorePasswordRequest): MessageResponse
}
```

### 7.4 Room Database Changes

**Modified tables:**
- `CachedDoor`: add `placeId: String`, `orgId: String` columns
- `CachedAccessLog`: add `placeId: String` column

**New tables:**
```kotlin
@Entity(tableName = "cached_organizations")
data class CachedOrganization(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String?,
    val domain: String,
    val role: String,
    val lastUpdated: Long
)

@Entity(tableName = "cached_places")
data class CachedPlace(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val orgId: String,
    val isLockdown: Boolean,
    val doorCount: Int,
    val lastUpdated: Long
)

@Entity(tableName = "active_sessions")
data class ActiveSession(
    @PrimaryKey val orgId: String,
    val orgName: String,
    val orgLogo: String?,
    val accessToken: String,   // encrypted
    val refreshToken: String,  // encrypted
    val userId: String,
    val userEmail: String,
    val lastUsed: Long
)

@Entity(tableName = "cached_user_info")
data class CachedUserInfo(
    @PrimaryKey val orgId: String,
    val userId: String,
    val name: String,
    val email: String,
    val role: String
)
```

### 7.5 TokenStore Changes

```kotlin
interface TokenStore {
    // Active session (existing, delegates to active org)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?

    // Multi-session (new)
    fun saveSession(orgId: String, tokens: AuthTokens)
    fun getSession(orgId: String): AuthTokens?
    fun getAllSessions(): List<ActiveSession>
    fun removeSession(orgId: String)
    fun setActiveOrg(orgId: String)
    fun getActiveOrgId(): String?
}
```

`AuthInterceptor` reads from `getAccessToken()` which returns the active org's token. Org switch calls `setActiveOrg()` -> all subsequent requests use new org's token.

### 7.6 New Repositories

| Repository | Purpose |
|------------|---------|
| OrgRepository | Org list, switch, cache |
| PlaceRepository | Place list, search, lockdown, cache |
| UserManagementRepository | CRUD users, roles, access rights per place |
| IncidentRepository | Incidents, filters, occurrences per place |
| ScheduleRepository | Schedules, holidays, regions per place |
| ZoneRepository | Zones per place |
| CardRepository | Card assignment, status per place |

Existing repositories modified:
- `AuthRepository`: multi-session management, magic link, 2FA, org lookup
- `DoorRepository`: add placeId scoping
- `AccessLogRepository`: add placeId scoping, event filters
- `CredentialRepository`: split into user-credentials vs admin-credentials

---

## 8. Cross-Platform Sync Strategy

### 8.1 Execution Phases

```
Phase 1: Backend (1 week)
    Add multi-org DB tables + migrations
    Implement new /app/ endpoints
    Keep ALL old endpoints working
    Deploy to staging

Phase 2: Android Rewrite (3-4 weeks)
    Week 1: Core infra + Auth flow (12 screens)
    Week 2: Places + Doors + Unlock (6 screens)
    Week 3: Admin dashboard (31 screens)
    Week 4: Settings + polish + integration testing

Phase 3: iOS Migration (2 weeks)
    Week 1: Auth flow + navigation restructure
    Week 2: Admin screens + new endpoint alignment

Phase 4: Backend Cleanup (1 week)
    Remove deprecated old endpoints
    Remove backwards-compat token handling
```

### 8.2 API Compatibility Rules

1. Old endpoints alive until Phase 4 (iOS depends on them)
2. New endpoints use consistent `/app/places/{placeId}/...` patterns
3. Same response shapes where possible (additive fields only)
4. Old tokens (no org_id) work on old endpoints; new tokens work everywhere
5. Backend checks token for org_id claim: if present, scope queries; if absent, legacy behavior

### 8.3 Model Alignment

| Model | Android | iOS | Backend JSON |
|-------|---------|-----|-------------|
| Organization | `@Serializable data class` | `Codable struct` | `{"id","name","logo","domain","role"}` |
| Place | `@Serializable data class` | `Codable struct` | `{"id","name","address","org_id","is_lockdown","door_count"}` |
| Door | Existing `AccessibleDoor` + `placeId` | Existing `Door` + `placeId` | Add `place_id` |
| AuthTokens | Existing + `orgId` | Existing + `orgId` | Add `org_id` to JWT |
| ActiveSession | Room entity | SwiftData model | Client-side only |

### 8.4 BLE Protocol — No Changes

Unchanged across all platforms:
- Service UUID: `4D495354-5950-4153-532D-424C45415554`
- Challenge: 32B nonce + 8B issued_at + 8B expires_at
- Auth response: userId_len + userId + ECDSA signature
- Result codes: 0x01-0x06

### 8.5 Push Notification Alignment

New notification types added to both platforms:
- Lockdown activated/deactivated (security_alerts channel)
- Incident created (security_alerts channel)
- User added to place (access_updates channel)

### 8.6 Integration Test Checkpoints

**After Phase 1 (Backend):**
- Old iOS app login + door fetch works (no regression)
- New /app/orgs returns org list
- New /app/places/{id}/doors returns scoped doors
- Magic link email sends
- Org switch returns scoped tokens

**After Phase 2 (Android):**
- Full login: magic link + domain lookup + classic + 2FA
- Multi-org: switch orgs, each shows correct places
- Unlock: BLE + remote + QR per place
- Admin: manage users, view events, create credentials
- Offline: cached doors load without network
- iOS still works on same staging backend

**After Phase 3 (iOS):**
- iOS matches Android navigation
- Same endpoints, same models
- Multi-org switching works
- Admin features accessible

**After Phase 4 (Cleanup):**
- Old endpoints removed
- Both clients on new endpoints only
- Zero 404s in error logs

---

## 9. What's Preserved (No Changes)

| Component | Reason |
|-----------|--------|
| Hilt DI setup | Working, industry standard |
| Retrofit + OkHttp stack | Working, just add new interfaces |
| Room database | Working, add tables + migration |
| EncryptedSharedPreferences | Working, extend for multi-session |
| KeystoreManager (EC P-256) | Working, BLE crypto unchanged |
| BLEAuthClient | Working, protocol unchanged |
| MistyisletBleManager (Nordic) | Working, keep dual-mode (TCP sim + real BLE) |
| BiometricHelper | Working, reused in new unlock flow |
| FCM push (MistyisletMessagingService) | Working, add new notification types |
| NFCReader | Working, reused in CardAssignment |
| CameraX + ML Kit (QR) | Working, reused for scanner action |
| Glance widget | Working, keep as-is |
| Build variants (debug/staging/release) | Working, keep all three |
| Certificate pinning | Working, keep for release builds |
| Proguard/R8 config | Working, keep |
