# Mistyislet Android App — Final Development Document

> Updated: 2026-05-03
> Architecture baseline: `Indonesia_SaaS_Access_Control_Architecture.md` v2
> Design baseline: Material Design 3 (M3) + Material 3 Expressive preview
> Competitive baseline: Kisi Android App feature set
> Source code: ~4,021 LOC Kotlin already implemented

---

## 1. Product Vision

Mistyislet Android App is the **primary mobile access client** for the Indonesian market (95%+ Android penetration). It is a resident-facing app, not an admin console.

**Core user story:** A factory worker walks toward a door, the phone auto-discovers the Controller via BLE, authenticates with a hardware-backed private key (Android Keystore / StrongBox), and the door opens — all in < 300ms, even offline.

### 1.1 Product Scope

Users can:
1. View accessible doors with real-time gateway status
2. Unlock doors via BLE (primary), Remote HTTPS, or QR scan
3. Manage credentials (Keystore credential, Google Wallet Pass, physical card binding)
4. View personal access history
5. Create visitor passes

**Not in scope (Admin backend handles):**
- Door/floor/hardware creation and management
- User/team/permission assignment
- Organization-level reports and alert policies

### 1.2 V2 Architecture — Three-Tier Credential Support

| Tier | Credential | Android Implementation | Priority |
|------|-----------|----------------------|----------|
| Tier 1 | BLE mobile credential | Android Keystore (StrongBox/TEE) EC P-256 + BLE GATT Client | Phase 1 (done) |
| Tier 2 | DESFire EV3 physical card | NFC Tag discovery + self-service card binding | Phase 2 |
| Tier 3 | Dynamic QR code | CameraX scan + short-lived token | Phase 1 (done) |

### 1.3 Kisi Feature Parity Matrix

| Kisi Android Feature | Mistyislet | Status | Notes |
|---------------------|-----------|--------|-------|
| Door list with status | Yes | Done | Gateway online/offline |
| Tap-to-unlock (BLE) | Yes | Done | TCP simulator; real BLE next |
| Remote unlock | Yes | Done | HTTPS → Cloud → Controller |
| Google Wallet Pass | Deferred | — | Google Pay limited in Indonesia |
| QR code scan unlock | Yes | Done | CameraX + ML Kit |
| Access history timeline | Yes | Done | Room + pagination |
| Visitor pass creation | Yes | Done | QR token + expiry |
| Widget (home screen) | Planned | P2 | Glance widget |
| Auto-unlock geofence | Planned | P3 | Geofencing API |
| Credential management | Yes | Done | View/revoke registered devices |
| Multi-site switching | Planned | P2 | Tenant/building picker |
| Offline mode | Yes | Done | BLE auth works without internet |
| Biometric gate | Planned | P1 | BiometricPrompt |
| Push notifications | Planned | P1 | FCM |
| Share access link | Planned | P2 | Intent share QR image |
| NFC card binding | Planned | P2 | Android NFC API |
| Background BLE scan | Planned | P1 | Foreground service |

### 1.4 Current Implementation Status

**Already built** (~4,021 LOC Kotlin):
- Login + JWT auth with auto-refresh
- Door list with gateway status
- BLE unlock (TCP simulator mode)
- QR scanner unlock
- Credential registration (Keystore EC P-256)
- Access history
- Visitor pass creation
- Profile screen
- Room database + offline cache
- Hilt DI + MVVM architecture

**Remaining for production:**
- Real BLE GATT client (replace TCP simulator)
- BiometricPrompt gate
- FCM push notifications
- Background BLE scanning (foreground service)
- NFC card binding
- Home screen widget
- Indonesian localization polish
- Play Store submission

---

## 2. Technology Stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | Kotlin | Android official, coroutine support |
| UI | Jetpack Compose + M3 | Declarative, dynamic color |
| Architecture | MVVM + Clean Architecture + Hilt | Already implemented, testable |
| Network | Retrofit + OkHttp | Interceptor-friendly, mature ecosystem |
| Serialization | kotlinx.serialization | Kotlin-native, performant |
| Local storage | DataStore + Room | Type-safe, coroutine-friendly |
| Navigation | Navigation Compose | Compose-native |
| BLE | Nordic `no.nordicsemi.android:ble:2.8+` | Most reliable Android BLE library |
| NFC | Android NFC API | Native API sufficient |
| QR Scanning | ML Kit Barcode | Google maintained, fast, offline-capable |
| Image | Coil | Kotlin-first, lightweight |
| Push | FCM | Indonesia standard, free tier |
| Testing | JUnit5 + Turbine + MockK | Compose + Flow testing standard |

### 2.1 Minimum System Requirements

| Item | Requirement | Reason |
|------|-------------|--------|
| Min SDK | 26 (Android 8.0) | BLE scanning standardized, 99%+ Indonesia coverage |
| Target SDK | 35 (latest) | Google Play requirement |
| BLE | 4.2+ | Door BLE communication |
| NFC | HCE support | Google Wallet NFC access |
| Google Play Services | Required | Wallet API, ML Kit |

---

## 3. Design Language — Material Design 3

### 3.1 Core M3 Principles

| M3 Principle | Application |
|-------------|-------------|
| **Dynamic color** | Material You color extraction from wallpaper on Android 12+ |
| **Elevation** | Tonal elevation for door cards (surface tint, not shadow) |
| **Motion** | Shared element transitions between door list → door detail |
| **Shape** | M3 corner radii: small (8dp), medium (12dp), large (16dp) |
| **Typography** | M3 type scale with Roboto Flex / system font |
| **Accessible** | 48dp minimum touch target, 4.5:1 contrast ratio |

### 3.2 Navigation Architecture

```
Navigation Bar (bottom, M3 style — 5 destinations)
├── Doors        (home icon)         — Primary: door list + BLE unlock
├── Scanner      (qr_code_scanner)   — QR code scan unlock
├── History      (schedule icon)     — Access event timeline
├── Visitors     (person_add icon)   — Visitor pass management
└── Profile      (account_circle)    — Credentials, settings, personal info
```

**M3 Navigation Bar spec:**
- Height: 80dp
- Icon: 24dp, active state filled, inactive outlined
- Label: always visible (M3 recommendation for ≤ 5 destinations)
- Active indicator: pill shape with tonal color
- Haptic: light click on tab switch

### 3.3 Color System (Material You + Brand)

| Token | Value | Usage |
|-------|-------|-------|
| Primary | `#4F55FF` | Main buttons, selected state |
| On Primary | `#FFFFFF` | Text on primary |
| Primary Container | `#E0E1FF` | Card highlights |
| On Primary Container | `#0A0F7A` | Text on container |
| Secondary | `#5C5D72` | Secondary elements |
| Tertiary | `#78536B` | Accent |
| Surface | `#FEFBFF` / `#FFFFFF` | Card backgrounds |
| Background | `#F7F7F8` | Page backgrounds |
| Text Primary | `#17171C` | Headlines |
| Text Secondary | `#6F717C` | Supporting text |
| Success | `#35A853` | Unlock success |
| Error / Danger | `#BA1A1A` / `#D93025` | Denied/errors |
| Warning | `#D98B06` | Offline/caution |

```kotlin
// Theme.kt — Dynamic color with brand fallback
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val colorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

### 3.4 Typography (M3 Type Scale)

| M3 Role | Usage | Size/Weight |
|---------|-------|-------------|
| Headline Large | Screen titles | 32sp / 400 |
| Headline Medium | Section headers | 28sp / 400 |
| Title Large | Door names in cards | 22sp / 400 |
| Title Medium | Card subtitles | 16sp / 500 |
| Body Large | Descriptions | 16sp / 400 |
| Body Medium | Default text | 14sp / 400 |
| Label Large | Buttons, tabs | 14sp / 500 |
| Label Medium | Badges, chips | 12sp / 500 |
| Label Small | Timestamps | 11sp / 500 |

### 3.5 Material Symbols

| Context | Symbol | Style |
|---------|--------|-------|
| Door (locked) | lock | filled |
| Door (unlocked) | lock_open | filled |
| BLE connecting | bluetooth_searching | outlined |
| BLE success | check_circle | filled |
| Gateway online | circle (green) | filled |
| Gateway offline | circle (gray) | filled |
| Unlock action | lock_open_right | filled |
| QR scanner | qr_code_scanner | outlined |
| Visitor | person_add | outlined |
| History | schedule | outlined |
| Credential | key | filled |
| Settings | settings | outlined |
| Copy | content_copy | outlined |
| Share | share | outlined |

---

## 4. Screen-by-Screen Design

### 4.1 Login Screen

**Features:**
- Email/password login → `POST /app/auth/login`
- Remember login state (token in EncryptedSharedPreferences)
- Auto-login on startup if valid token exists
- BiometricPrompt to unlock local token (optional, P1)
- Error display with rate-limit messaging

### 4.2 Doors Screen (Home Tab)

**Layout:** Scaffold with TopAppBar + LazyColumn

```
┌─────────────────────────────────┐
│ 🔒 Mistyislet          [Sort ▼]│  ← M3 CenterAlignedTopAppBar
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Main Entrance          🟢  │ │  ← M3 ElevatedCard
│ │ Lobby · Floor 1             │ │  ← supportingText
│ │                             │ │
│ │  ┌─────────────────────┐    │ │
│ │  │   Hold to Unlock    │    │ │  ← M3 FilledTonalButton (full width)
│ │  └─────────────────────┘    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Server Room            🔴  │ │
│ │ Data Center · B2            │ │
│ │ Controller offline          │ │  ← error color text
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Parking Gate           🟡  │ │
│ │ Parking · G                 │ │
│ │  ┌─────────────────────┐    │ │
│ │  │   Hold to Unlock    │    │ │
│ │  └─────────────────────┘    │ │
│ └─────────────────────────────┘ │
│                                 │
└──┤ Doors │Scanner│History│...├──┘
```

**Features:**
- Door list from `/app/access/my-doors`, grouped by Place
- 15-second auto-refresh for gateway status
- Local search/filter by door name
- Pull-to-refresh

**Door Status Display Logic:**

```kotlin
enum class DoorDisplayStatus {
    ONLINE_UNLOCKABLE,     // Gateway online + permission → green, tappable
    ONLINE_LOCKED_DOWN,    // Gateway online + lockdown → red, disabled
    OFFLINE,               // Gateway offline → gray, disabled
    DISCONNECTED,          // Gateway disconnected → gray dashed, disabled
}
```

**Hold-to-Unlock Interaction (500ms long press):**
- CircularProgressIndicator fills during hold
- HapticFeedback on trigger
- Calls `/app/access/unlock` with `{ lock_id: "xxx" }`

**Unlock Result — Full-screen Dialog:**
- **Granted**: Green tonal surface + `check_circle` 48dp + door name + vibration `[0, 100, 50, 100]`
- **Denied**: Red tonal surface + `cancel` 48dp + reason + vibration `[0, 250]`
- **Loading**: Brand primary + `bluetooth_searching` animated rotation + "Connecting..."
- Auto-dismiss: 2 seconds on success

### 4.3 QR Scanner Screen

**Layout:** Full-screen CameraX with M3 overlay

**Features:**
- CameraX + ML Kit Barcode Scanning
- Flashlight toggle for dark environments
- Camera permission handling with guidance
- Auto-unlock on successful scan → `POST /app/access/qr-unlock`

**QR Code Formats (both supported):**
```
// Access Link QR (visitor)
https://app.mistyislet.com/access-link/{token}

// Custom scheme QR (terminal scan)
mistyislet://qr/{qr_token}?lock_id={lock_id}
```

**Overlay:**
- Semi-transparent background with rounded rectangle viewfinder cutout
- Animated corner brackets
- Instructions text below viewfinder

### 4.4 History Screen

**Layout:** M3 LazyColumn with date header sticky sections

**Features:**
- Data from `/app/access/logs`, paginated
- Each event: M3 ListItem with leading icon (check_circle/cancel), headline (door name), supporting (time), trailing (BLE/QR/Remote chip)
- Time filter: Today / This week / This month / Custom
- Pull-to-refresh

### 4.5 Visitors Screen

**Layout:** M3 Scaffold + LazyColumn + FAB

```
┌─────────────────────────────────┐
│ Visitors                        │
├─────────────────────────────────┤
│ ACTIVE                          │
│ ┌─────────────────────────────┐ │
│ │ John Doe                    │ │  ← M3 ListItem
│ │ Acme Corp · Meeting         │ │
│ │ ⏱ 22h remaining            │ │  ← countdown chip
│ │ [QR]  [Copy]  [Share]       │ │  ← M3 IconButton row
│ └─────────────────────────────┘ │
│                                 │
│ EXPIRED                         │
│ ┌─────────────────────────────┐ │
│ │ Jane Smith  — expired       │ │
│ └─────────────────────────────┘ │
│                                 │
│                          [+ ○]  │  ← M3 FAB
└─────────────────────────────────┘
```

**Create Visitor Bottom Sheet (M3: ModalBottomSheet):**
- Name (OutlinedTextField, required)
- Phone (OutlinedTextField, required)
- Host name (OutlinedTextField, required)
- Door selection (MultiSelectChip group)
- Duration: SegmentedButton row (4h | 8h | 24h | 48h | 72h)
- ID Document: DropdownMenu (None | KTP | KITAS | ITAS)
- Create → navigate to QR display screen

**QR Display Screen:**
- Full-width QR code generated from `access_token`
- Token text with copy button
- Expiry countdown
- Share button (Intent.ACTION_SEND with QR bitmap)

### 4.6 Profile Screen

**Layout:** M3 ListItem sections

**Sections:**
- **User info**: Name, email from `/app/me`
- **My Credentials**: Device model, keystore level badge (StrongBox/TEE/Software), Google Wallet Pass (save_link), physical card binding status, expiry, revoke button
- **Settings**: Language picker (en-US / zh-CN / id-ID), Biometric lock toggle, Notification toggle, About
- **Logout**: Clear token, navigate to login

---

## 5. Backend API Integration

### 5.1 Base URL & Protocol

```
Production: https://api.mistyislet.com/api/v1
Development: http://127.0.0.1:8080/api/v1
```

- All requests: `Accept: application/json` + `Content-Type: application/json`
- Field naming: `snake_case`
- Collection response: `{ "items": [...], "pagination": { "offset", "limit", "total", "has_more" } }`
- Error response: `{ "error": "...", "message": "...", "code": "...", "status": "422" }`

### 5.2 Authentication

App uses independent Resident login channel, separate from Admin backend.

#### Login
```
POST /api/v1/app/auth/login
{ "email": "user@example.com", "password": "..." }

→ 200: { "access_token", "refresh_token", "expires_in": 3600, "user": {...} }
```

#### Token Refresh
```
POST /api/v1/app/auth/refresh
Authorization: Bearer <refresh_token>

→ 200: { "access_token", "refresh_token", "expires_in": 3600 }
```

#### Token Management Strategy

| Strategy | Implementation |
|----------|---------------|
| Storage | EncryptedSharedPreferences (AES-256-GCM) |
| Auto-refresh | OkHttp Authenticator intercepts 401, uses refresh_token |
| Concurrent refresh | Mutex ensures single refresh request at a time |
| Refresh failure | Clear local tokens, navigate to login |
| Proactive refresh | access_token refreshed 60s before expiry |

### 5.3 API Endpoint Summary

| Operation | Method | Path | Auth |
|-----------|--------|------|------|
| Login | POST | `/app/auth/login` | Public |
| Refresh token | POST | `/app/auth/refresh` | Public |
| Get current user | GET | `/app/me` | Bearer |
| Get credentials | GET | `/app/credentials` | Bearer |
| Get my doors | GET | `/app/access/my-doors` | Bearer |
| Get BLE token | GET | `/app/access/ble-token` | Bearer |
| Unlock door | POST | `/app/access/unlock` | Bearer |
| QR unlock | POST | `/app/access/qr-unlock` | Bearer |
| Get access logs | GET | `/app/access/logs` | Bearer |
| Create visitor pass | POST | `/app/visitor-passes` | Bearer |
| Register device (FCM) | POST | `/app/devices/register` | Bearer |

### 5.4 Core Data Models

```kotlin
data class AccessibleDoor(
    val id: String,
    val name: String,
    @SerialName("building_id") val buildingId: String,
    @SerialName("area_id") val areaId: String?,
    val status: String,            // "normal", "locked_down"
    @SerialName("gateway_status") val gatewayStatus: String, // "online", "offline", "disconnected"
    @SerialName("group_name") val groupName: String?,
    @SerialName("can_unlock") val canUnlock: Boolean,
)

data class UnlockRequest(
    @SerialName("lock_id") val lockId: String,
    @SerialName("ble_token") val bleToken: String? = null,
)

data class QRUnlockRequest(
    @SerialName("lock_id") val lockId: String,
    @SerialName("qr_token") val qrToken: String,
)

data class UnlockResponse(
    val decision: String,          // "allow" | "deny"
    val reason: String,
    val status: String?,           // "accepted" | "dispatched"
    @SerialName("request_id") val requestId: String?,
    @SerialName("lock_id") val lockId: String,
    @SerialName("lock_name") val lockName: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("group_name") val groupName: String?,
    @SerialName("group_id") val groupId: String?,
    @SerialName("issued_at") val issuedAt: String?,
)

data class Credential(
    val id: String,
    @SerialName("credential_kind") val credentialKind: String, // "google_wallet" | "apple_wallet" | "physical_card" | "mobile_key"
    val provider: String?,
    val status: String,            // "active" | "suspended" | "revoked"
    @SerialName("save_link") val saveLink: String?,
    @SerialName("card_number") val cardNumber: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("created_at") val createdAt: String?,
)
```

---

## 6. BLE Authentication

### 6.1 Architecture

```
┌─────────────────┐         BLE          ┌──────────────────┐        RS485/OSDP       ┌────────┐
│  Android App    │ ◄──────────────────► │  Mistyislet      │ ◄────────────────────► │  Door  │
│  (Central)      │    GATT Service      │  Reader (BLE)    │     Serial Protocol    │  Lock  │
└─────────────────┘                      └──────────────────┘                        └────────┘
                                                 │
                                                 │ WiFi/Ethernet
                                                 ▼
                                         ┌──────────────────┐        NATS           ┌──────────┐
                                         │  Controller      │ ◄──────────────────► │  Cloud   │
                                         │  (Gateway)       │                       │  API     │
                                         └──────────────────┘                       └──────────┘
```

### 6.2 Dual BLE Authentication Modes

**Mode A — Challenge-Response (Primary, Offline-capable):**
1. App discovers Reader via BLE scan (Service UUID filter)
2. Connects GATT Server
3. Reads CHALLENGE characteristic: `[32B nonce][8B issued_at][8B expires_at]`
4. Signs `SHA256(nonce || userId)` with Keystore private key (EC P-256)
5. Writes AUTH_RESPONSE: `[1B userIdLen][userIdBytes][signatureBytes]`
6. Enables notifications on AUTH_RESULT
7. Receives result notification → unlock

**Mode B — Token-Based (Fallback, Online required):**
1. App fetches BLE Token: `GET /app/access/ble-token`
2. Connects Reader BLE
3. Writes token to Command characteristic
4. Reader validates token via Controller → Cloud
5. Result notification returned

### 6.3 BLE Service Definition

```kotlin
// Nordic BLE library implementation
class MistyisletBleManager(context: Context) : BleManager(context) {

    companion object {
        // Service & Characteristic UUIDs (from ble_protocol.go)
        val SERVICE_UUID = UUID.fromString("4d495354-5950-4153-532d-424c45415554")
        val CHALLENGE_UUID = UUID.fromString("4d495354-5950-4153-532d-4348414c4c4e")
        val AUTH_RESPONSE_UUID = UUID.fromString("4d495354-5950-4153-532d-415554485245")
        val READER_IDENTITY_UUID = UUID.fromString("4d495354-5950-4153-532d-524541444552")
        val AUTH_RESULT_UUID = UUID.fromString("4d495354-5950-4153-532d-524553554c54")
    }

    override fun initialize() {
        // 1. Read CHALLENGE (32B nonce + 8B issued_at + 8B expires_at)
        // 2. Sign with Keystore: SHA256(nonce || userId)
        // 3. Write AUTH_RESPONSE: [1B len][userId][signature]
        // 4. Enable notifications on AUTH_RESULT
        // 5. Wait for result notification
    }
}
```

### 6.4 Android Keystore Key Management

```kotlin
class KeystoreManager {
    fun generateKeyPair(): KeyPair {
        val spec = KeyGenParameterSpec.Builder(ALIAS, PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setIsStrongBoxBacked(isStrongBoxAvailable())
            .build()
        // ...
    }

    fun signChallenge(nonce: ByteArray, userId: String): ByteArray {
        val data = nonce + userId.toByteArray()
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()  // ASN.1 DER format
    }

    fun getKeystoreLevel(): String {
        // StrongBox > TEE > Software
    }

    fun getAttestationChain(): List<Certificate> {
        // For registration: proves key is hardware-backed
    }
}
```

### 6.5 BLE Scanning Strategy

| Scenario | Scan Mode | Interval | Note |
|----------|-----------|----------|------|
| App foreground + Doors page | SCAN_MODE_LOW_LATENCY | Continuous | Fast Reader discovery |
| App foreground + other pages | SCAN_MODE_LOW_POWER | 5s scan / 10s pause | Battery-friendly |
| App background | SCAN_MODE_OPPORTUNISTIC | System-scheduled | Relies on system scans |
| User taps unlock | SCAN_MODE_LOW_LATENCY | 10s burst | Fast connect target Reader |

### 6.6 BLE Permissions

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

### 6.7 Background BLE Service

```kotlin
class BLEScanService : ForegroundService() {
    private val scanner = BluetoothLeScannerCompat.getScanner()
    private val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(SERVICE_UUID))
        .build()

    // ScanSettings.SCAN_MODE_LOW_POWER for battery efficiency
    // On discovery → show notification "Door nearby, tap to unlock"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY  // restart if killed
    }
}
```

---

## 7. NFC Integration

### 7.1 Google Wallet NFC Access (Passive)

When user taps phone with saved Wallet Pass against Reader:
1. Phone NFC activated by Reader
2. Google Wallet sends Pass NFC payload automatically
3. NFC payload contains `auth_token` + `pass_serial`
4. Reader forwards to Controller → Cloud verify
5. Verification passes → door opens

**App does nothing in this flow** — NFC communication handled by Wallet + Reader hardware. App only provides "Add to Google Wallet" button on Credentials screen.

```kotlin
// Google Wallet integration
class WalletHelper(private val activity: Activity) {
    private val walletClient = Pay.getClient(activity)

    fun addToGoogleWallet(jwt: String) {
        val request = SavePassesRequest.Builder()
            .addNewSavePassesJwt(jwt)
            .build()
        walletClient.savePasses(request, activity, ADD_TO_WALLET_REQUEST_CODE)
    }

    suspend fun isAvailable(): Boolean {
        return try {
            walletClient.getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES)
            true
        } catch (e: Exception) { false }
    }
}
```

### 7.2 NFC Card Self-Binding (Active)

For physical DESFire EV3 card binding:

```kotlin
class NFCReader(private val activity: Activity) {
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

    fun enableReaderMode() {
        nfcAdapter?.enableReaderMode(activity, { tag ->
            val uid = tag.id.toHexString()
            // Send UID to backend for binding
        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null)
    }
}
```

---

## 8. Push Notifications (FCM)

### 8.1 Notification Events

| Event | Notification | Channel | Priority |
|-------|-------------|---------|----------|
| New door access granted | "You now have access to Server Room" | access_updates | DEFAULT |
| Access revoked | "Your access to Server Room removed" | access_updates | DEFAULT |
| Credential expiring (< 24h) | "Credential expires tomorrow, open app to renew" | credential_alerts | HIGH |
| Credential revoked | "Your device credential revoked" | credential_alerts | HIGH |
| Visitor arrived | "Your visitor John Doe checked in" | visitor_updates | DEFAULT |
| Door held open | "Main Entrance held open > 30s" | security_alerts | HIGH |

### 8.2 Implementation

```kotlin
class MistyisletMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // POST /app/devices/register { "push_token": token, "platform": "android" }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "door_unlocked" -> showUnlockNotification(message.data)
            "visitor_arrived" -> showVisitorNotification(message.data)
            "credential_updated" -> showCredentialNotification(message.data)
            "access_changed" -> refreshDoorList()
        }
    }
}
```

---

## 9. Offline Mode & Caching

### 9.1 Offline Scenarios

| Scenario | Behavior |
|----------|----------|
| Phone online, Controller online | BLE challenge-response → Controller local verify → unlock |
| Phone online, Controller offline | Remote unlock via cloud (Controller uses cached rules) |
| Phone offline, Controller online | BLE still works (Keystore key is local, Controller verifies) |
| Both offline | BLE works if Controller has cached public key (< 72h cache) |
| Credential expired + offline | Deny; must reconnect to refresh via `/app/credentials/refresh` |

### 9.2 Cache Strategy

| Data | Strategy | TTL | Storage |
|------|----------|-----|---------|
| User info | Cache-First | 24h | DataStore |
| Accessible doors | Network-First + Cache Fallback | 5min | Room DB |
| Credentials | Network-First + Cache Fallback | 1h | Room DB |
| Access logs | Network-First | Latest 100 | Room DB |
| BLE Token (Mode B) | Network-Only | No cache | Memory |

### 9.3 Room Database

```kotlin
@Entity(tableName = "doors")
data class DoorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val buildingName: String,
    val floorName: String,
    val gatewayStatus: String,
    val canUnlock: Boolean,
    val lastSyncAt: Long,
)

@Entity(tableName = "credentials")
data class CachedCredential(
    @PrimaryKey val id: String,
    val credentialKind: String,
    val provider: String?,
    val status: String,
    val saveLink: String?,
    val cardNumber: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "access_logs")
data class CachedAccessLog(
    @PrimaryKey val id: String,
    val doorName: String,
    val eventType: String,
    val result: String,
    val credentialType: String?,
    val timestamp: Long,
)
```

---

## 10. Security Requirements

### 10.1 Communication Security

| Item | Requirement |
|------|-------------|
| TLS | All HTTP forced HTTPS (dev exception) |
| Certificate Pinning | Pin backend certificate public key in production |
| BLE encryption | AES-128-CCM (aligned with firmware) |
| Token transmission | BLE Token single-use, timestamped, anti-replay |

### 10.2 Local Security

| Item | Requirement |
|------|-------------|
| Token storage | EncryptedSharedPreferences, AES-256-GCM |
| Database | Room + SQLCipher (if storing sensitive data) |
| Root detection | Warn user device is rooted, don't block usage |
| Debug protection | Release builds disable debuggable |
| Logging | Release builds don't output sensitive info to Logcat |

### 10.3 BLE Security

| Item | Requirement |
|------|-------------|
| Challenge validity | BLE challenge expires after 60 seconds |
| Anti-replay | Challenge includes timestamp + nonce, Reader validates |
| Anti-relay | RSSI distance check (signal too weak → reject) |
| Pairing | LESC pairing on first Reader connection (if firmware supports) |
| Key attestation | Registration sends attestation chain to prove hardware-backed key |

---

## 11. Accessibility (WCAG 2.2 + Indonesia Context)

### 11.1 Requirements

| Requirement | Implementation |
|------------|---------------|
| TalkBack | All composables have `contentDescription` |
| Font scaling | Use `sp` units, test at 200% scale |
| Reduce animations | Check `Settings.Global.ANIMATOR_DURATION_SCALE` |
| High contrast | M3 color roles auto-adapt |
| Switch Access | All interactive elements focusable in logical order |
| Touch target | Minimum 48dp for all tappable areas |
| Dark environment | Unlock overlay readable in 0 lux |
| One-handed use | Critical actions (unlock) in lower 60% of screen |
| Screen reader unlock | TalkBack double-tap on door card triggers unlock |

### 11.2 TalkBack Semantics

```kotlin
Card(
    modifier = Modifier.semantics {
        contentDescription = "Main Entrance, Floor 1, Lobby, Controller online"
        customActions = listOf(
            CustomAccessibilityAction("Unlock door") { viewModel.unlock(doorId); true }
        )
    }
)

Text(
    text = "Access Granted",
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Assertive  // announce immediately
    }
)
```

### 11.3 Indonesia-Specific

- **Low-end devices**: Keep animations light, reduce GPU usage
- **Sunlight readability**: Contrast exceeds 7:1 for outdoor environments
- **Slow networks**: Show cached door list instantly; non-blocking loading states

---

## 12. Indonesia Market Adaptations

### 12.1 Device Compatibility

| Brand | Market Share | BLE | StrongBox | Test Priority |
|-------|------------|-----|-----------|--------------|
| Samsung Galaxy A series | ~25% | Yes | A54+ Yes | P0 |
| OPPO A/Reno series | ~20% | Yes | Reno5+ Yes | P0 |
| vivo Y/V series | ~15% | Yes | V25+ Yes | P0 |
| Xiaomi Redmi/POCO | ~15% | Yes | Mi 11+ Yes | P0 |
| realme | ~10% | Yes | GT2+ Yes | P1 |
| Infinix/Tecno | ~5% | Yes | No (TEE only) | P2 |

### 12.2 BLE Background Keep-Alive (OEM ROM Mitigations)

Indonesian OEM ROMs aggressively kill background services:

1. **Foreground service** with persistent notification (`START_STICKY`)
2. **Battery optimization exemption** request (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
3. **Vendor-specific auto-start** permission guidance (dontkillmyapp.com for Samsung/OPPO/Xiaomi/vivo)

### 12.3 Network Optimization

- Compressed payloads (gzip via OkHttp interceptor)
- No heavy images; use Material Symbols throughout
- Offline-first: show cached data immediately, sync in background
- Reduce polling frequency on metered connections

### 12.4 Localization

| Language | Resource File | Coverage |
|----------|--------------|----------|
| English (US) | `values/strings.xml` | 100%, default |
| 简体中文 | `values-zh-rCN/strings.xml` | 100% |
| Bahasa Indonesia | `values-in/strings.xml` | 100% |

Language follows system setting; also supports in-app switching (stored in DataStore).

---

## 13. Project Structure

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/mistyislet/app/
│   │   ├── MistyisletApp.kt              // Application, Hilt entry
│   │   ├── MainActivity.kt               // Single Activity, Compose host
│   │   │
│   │   ├── core/                          // Infrastructure
│   │   │   ├── network/
│   │   │   │   ├── ApiClient.kt           // Retrofit + OkHttp config
│   │   │   │   ├── AuthInterceptor.kt     // Token auto-refresh
│   │   │   │   └── ApiResult.kt           // Unified Result wrapper
│   │   │   ├── storage/
│   │   │   │   ├── TokenStore.kt          // Encrypted token storage
│   │   │   │   └── AppDatabase.kt         // Room database
│   │   │   ├── ble/
│   │   │   │   ├── MistyisletBleManager.kt // Nordic BLE GATT manager
│   │   │   │   ├── BLEScanService.kt      // Foreground scanning service
│   │   │   │   └── BLEProtocol.kt         // Command encode/decode
│   │   │   ├── keystore/
│   │   │   │   └── KeystoreManager.kt     // EC P-256 key management
│   │   │   ├── nfc/
│   │   │   │   └── NFCReader.kt           // NFC card reading
│   │   │   └── push/
│   │   │       └── MessagingService.kt    // FCM push service
│   │   │
│   │   ├── data/                          // Data layer
│   │   │   ├── api/
│   │   │   │   ├── AuthApi.kt
│   │   │   │   ├── AccessApi.kt
│   │   │   │   ├── CredentialApi.kt
│   │   │   │   └── UserApi.kt
│   │   │   ├── dao/
│   │   │   │   ├── DoorDao.kt
│   │   │   │   ├── CredentialDao.kt
│   │   │   │   └── AccessLogDao.kt
│   │   │   └── repository/
│   │   │       ├── AuthRepository.kt
│   │   │       ├── DoorRepository.kt
│   │   │       ├── CredentialRepository.kt
│   │   │       └── AccessLogRepository.kt
│   │   │
│   │   ├── domain/                        // Domain layer
│   │   │   ├── model/
│   │   │   │   ├── AccessibleDoor.kt
│   │   │   │   ├── Credential.kt
│   │   │   │   ├── UnlockResult.kt
│   │   │   │   └── AccessLog.kt
│   │   │   └── usecase/
│   │   │       ├── UnlockDoorUseCase.kt
│   │   │       ├── ScanQRCodeUseCase.kt
│   │   │       └── GetMyDoorsUseCase.kt
│   │   │
│   │   └── ui/                            // Presentation layer
│   │       ├── theme/
│   │       │   ├── Theme.kt
│   │       │   ├── Color.kt
│   │       │   └── Type.kt
│   │       ├── navigation/
│   │       │   └── AppNavigation.kt
│   │       ├── login/
│   │       │   ├── LoginScreen.kt
│   │       │   └── LoginViewModel.kt
│   │       ├── doors/
│   │       │   ├── DoorsScreen.kt
│   │       │   └── DoorsViewModel.kt
│   │       ├── scanner/
│   │       │   ├── ScannerScreen.kt
│   │       │   └── ScannerViewModel.kt
│   │       ├── visitors/
│   │       │   ├── VisitorsScreen.kt
│   │       │   └── VisitorsViewModel.kt
│   │       ├── history/
│   │       │   ├── HistoryScreen.kt
│   │       │   └── HistoryViewModel.kt
│   │       └── profile/
│   │           ├── ProfileScreen.kt
│   │           └── ProfileViewModel.kt
│   │
│   └── res/
│       ├── values/strings.xml             // en-US
│       ├── values-zh-rCN/strings.xml      // zh-CN
│       └── values-in/strings.xml          // id-ID
```

---

## 14. Testing Strategy

### 14.1 Test Layers

| Layer | Tools | Coverage |
|-------|-------|----------|
| Unit Test | JUnit5 + MockK + Turbine | ViewModel, UseCase, Repository |
| Integration Test | Robolectric | API calls, database operations |
| UI Test | Compose Testing | Key page interactions |
| E2E Test | Manual + Gateway Simulator | Full unlock flow |

### 14.2 Key Test Scenarios

| Scenario | Verification |
|----------|-------------|
| Login success | Token stored, auto-navigate to home |
| Login failure | Error displayed, rate-limit handling |
| Token expiry refresh | Silent refresh, concurrent requests |
| Door list load | Correct grouping, status display |
| Hold-to-unlock | Network request, animation, haptic |
| QR scan unlock | Camera permission, parse, auto-unlock |
| Offline cache | Show cached doors when no network |
| BLE scan | Discover Reader, connect, timeout |
| Challenge-response | Keystore signing, result parsing |

---

## 15. Build & Release

### 15.1 Build Variants

| Variant | API Base URL | Features |
|---------|-------------|----------|
| debug | `http://10.0.2.2:8080/api/v1` | Debuggable, Logcat output |
| staging | `https://staging-api.mistyislet.com/api/v1` | Pre-release environment |
| release | `https://api.mistyislet.com/api/v1` | Signed, obfuscated, no debug |

### 15.2 ProGuard / R8

```proguard
# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.mistyislet.app.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
```

### 15.3 Signing

| Item | Detail |
|------|--------|
| Keystore | `mistyislet-release.jks`, securely stored |
| Scheme | V2 + V3 |
| Play App Signing | Recommended: Google-managed signing key |

### 15.4 Development Environment

| Item | Version |
|------|---------|
| Android Studio | Ladybug (2024.2)+ |
| Kotlin | 2.0+ |
| Gradle | 8.7+ |
| JDK | 17 |
| Compose BOM | 2024.12+ |

---

## 16. Development Phases (Remaining Work)

### Phase 1B — Production BLE (3-5 days)

| # | Task | Effort |
|---|------|--------|
| 1 | Replace TCP simulator with Nordic BLE library | 2d |
| 2 | BLE foreground service for background scanning | 1d |
| 3 | Real device testing (Samsung A54, Xiaomi 15) | 1d |
| 4 | Battery optimization guide (per-vendor) | 1d |

### Phase 2 — Feature Complete (2-3 weeks)

| # | Task | Effort |
|---|------|--------|
| 5 | BiometricPrompt gate (Face/Fingerprint) | 2d |
| 6 | FCM push notifications | 2d |
| 7 | NFC card binding (self-service) | 2d |
| 8 | Visitor QR improvement (share + countdown) | 1d |
| 9 | Home screen widget (Glance API) | 2d |
| 10 | Multi-site switching | 2d |
| 11 | Indonesian localization polish | 1d |
| 12 | Play Store listing + screenshots | 2d |

### Phase 3 — Polish (1-2 weeks)

| # | Task | Effort |
|---|------|--------|
| 13 | Google Wallet Pass integration (when available) | 3d |
| 14 | Geofence auto-unlock | 2d |
| 15 | Predictive Back gesture support | 1d |
| 16 | Per-app language (Android 13+) | 1d |
| 17 | Play Store submission | 2d |

---

## 17. Dependencies & Prerequisites

### 17.1 Backend Dependencies

| Dependency | Status | Note |
|-----------|--------|------|
| `/app/auth/login` | **Implemented** | Resident login |
| `/app/auth/refresh` | **Implemented** | Token refresh |
| `/app/me` | **Implemented** | User info |
| `/app/credentials` | **Implemented** | Credential list with kind + save_link |
| `/app/access/my-doors` | **Implemented** | Doors + time windows + gateway status |
| `/app/access/unlock` | **Implemented** | BLE/remote unlock via NATS |
| `/app/access/qr-unlock` | **Implemented** | QR unlock, verifies group_link token |
| `/app/access/ble-token` | **Declared** | BLE Token fetch (confirm implementation) |
| `/app/access/logs` | **Implemented** | Personal access logs |
| `/app/visitor-passes` | **Declared** | Visitor pass creation (confirm details) |
| `/app/devices/register` | **Not implemented** | FCM push token registration (new) |

### 17.2 Hardware Dependencies

| Dependency | Status | Note |
|-----------|--------|------|
| Mistyislet Reader (BLE firmware) | **Not complete** | GATT Service definition needs firmware alignment |
| Mistyislet Controller | **Not complete** | Gateway hardware, NATS ↔ Cloud |
| NFC Reader Pro | **Not complete** | Reads Wallet Pass NFC payload |
| RS485 door lock control | **Not complete** | Controller-to-lock physical comm |

### 17.3 Third-Party Services

| Service | Purpose | Credentials Needed |
|---------|---------|-------------------|
| Google Wallet API | Pass issuance and save | Service Account JSON + Issuer ID |
| Firebase Cloud Messaging | Push notifications | google-services.json |
| Google Play Services | Wallet availability check | Built-in |
| ML Kit Barcode Scanning | QR scanning | None |

### 17.4 Hardware Not Ready — Mitigation

1. **API-first**: All `/app/` endpoints implemented, unlock API returns `accepted`/`dispatched`
2. **Gateway Simulator**: `cmd/gateway-simulator` simulates hardware responses
3. **BLE Mock**: TCP simulator replicates Reader GATT byte protocol
4. **NFC deferred**: Wallet NFC depends on Reader Pro hardware; MVP only does save_link

---

## 18. Risk Register

| Risk | Impact | Mitigation | Status |
|------|--------|-----------|--------|
| BLE killed by OPPO/vivo/Xiaomi ROM | Background unlock fails | Foreground service + battery exemption + user guide | P1 task |
| StrongBox unavailable on budget phones | Lower security tier | Auto-detect → TEE fallback → shorter TTL (30d vs 90d) | Implemented |
| TCP→BLE migration breaks protocol | Unlock fails on real hardware | Same byte-level protocol, only transport changes | Architectural |
| Play Store BLE permission rejection | App not published | Clear privacy description, minimal permissions | Pre-submission review |
| Low-end phone BLE latency > 500ms | Poor UX | Optimize connection parameters, skip service discovery cache | Test with Redmi |
| Google Wallet unavailable in Indonesia | No NFC tap | BLE is primary method, Wallet is bonus | By design |

---

## 19. Relationship with Admin Backend

- App and Admin share the same backend API, but use different auth channels (`/app/auth` vs `/api/v1/auth`)
- App users can only see resources they have permission for
- App user permissions assigned by Admin via Role Assignment / Group / Share
- App door status depends on Controller/Reader hardware being online
- Both platforms use the same `/app/` API; data models are consistent between Android and iOS
