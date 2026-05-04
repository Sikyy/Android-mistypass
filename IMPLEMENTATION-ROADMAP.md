# Mistyislet Android App — Implementation Roadmap

> Created: 2026-05-03
> Based on: `APP-DEVELOPMENT-PLAN.md`
> Current state: 51 Kotlin files, ~4,021 LOC
> Bottom Nav: Doors | Credentials | Dashboard | Profile (4 tabs)

---

## Current Gap Analysis

### Code vs. Plan Differences

| Plan Requirement | Current State | Gap |
|-----------------|---------------|-----|
| 5-tab nav: Doors/Scanner/History/Visitors/Profile | 4-tab: Doors/Credentials/Dashboard/Profile | Restructure nav |
| Hold-to-unlock (500ms long press) | Single-tap Cloud + BLE buttons | Rewrite unlock interaction |
| Full-screen unlock result dialog | Inline status text on card | Add dialog overlay |
| Real BLE GATT (Nordic) | TCP socket simulator | Replace transport layer |
| Background BLE scan service | None | New foreground service |
| BiometricPrompt gate | None | New module |
| FCM push notifications | None | New module + Firebase deps |
| Visitors UI screen | Model exists, no UI | New screen |
| NFC card binding | None | New module |
| CameraX + ML Kit scanner | None (no deps) | Add deps + scanner screen content |
| Google Wallet helper | None | New module (P3) |
| Home widget (Glance) | None | New module (P2) |
| Multi-site picker | None | Add to DoorsScreen |
| M3 ElevatedCard list layout | 2-column grid with small cards | Redesign to list cards |

### Dependencies Missing from `build.gradle.kts`

```kotlin
// Need to add:
// BLE
implementation("no.nordicsemi.android:ble:2.8.0")
// CameraX + ML Kit
implementation("androidx.camera:camera-camera2:1.4.0")
implementation("androidx.camera:camera-lifecycle:1.4.0")
implementation("androidx.camera:camera-view:1.4.0")
implementation("com.google.mlkit:barcode-scanning:17.3.0")
// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-messaging")
// Biometric
implementation("androidx.biometric:biometric:1.2.0-alpha05")
// Google Wallet (P3)
implementation("com.google.android.gms:play-services-pay:16.5.0")
// Glance widget (P2)
implementation("androidx.glance:glance-appwidget:1.1.0")
```

---

## Phase 1A — Navigation & UI Restructure (2 days)

> Goal: Align navigation to plan (5 tabs), redesign door cards to list layout

### 1A.1 — Restructure Bottom Navigation

**File:** `ui/navigation/AppNavigation.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1A.1.1 | Change `bottomNavItems` to 5 tabs: Doors, Scanner, History, Visitors, Profile | Nav bar shows 5 items with correct icons |
| 1A.1.2 | Add routes: `SCANNER`, `HISTORY` (top-level), `VISITORS` | NavHost has 5 composable destinations |
| 1A.1.3 | Remove `DASHBOARD` and `CREDENTIALS` as top-level tabs | Dashboard content merged into Doors; Credentials moved into Profile |
| 1A.1.4 | Move `CredentialsScreen` as sub-nav inside Profile tab | ProfileScreen has "My Credentials" section linking to Credentials |
| 1A.1.5 | Move `HistoryScreen` from Dashboard sub-page to top-level tab | History accessible directly from bottom nav |
| 1A.1.6 | Update `Routes` object with new constants | Clean route definitions |
| 1A.1.7 | Update string resources `nav_*` in all 3 locale files | Labels: Doors/Scanner/History/Visitors/Profile |

### 1A.2 — Redesign DoorsScreen to List Layout

**File:** `ui/doors/DoorsScreen.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1A.2.1 | Replace `LazyVerticalGrid(2 columns)` with `LazyColumn` | Single-column scrollable list |
| 1A.2.2 | Replace `DoorGridCard` with `DoorListCard` (M3 ElevatedCard, full-width) | Card shows: name, location subtitle, gateway status dot, unlock button |
| 1A.2.3 | Add `CenterAlignedTopAppBar` with app name + Sort dropdown | Material 3 standard top bar |
| 1A.2.4 | Add gateway status indicator dot (green/yellow/red/gray) to card trailing | Visual status at a glance |
| 1A.2.5 | Show "Controller offline" error text on offline doors | Red colored supporting text |
| 1A.2.6 | Remove separate Cloud/BLE buttons; replace with single "Hold to Unlock" `FilledTonalButton` | One unlock action per card |

### 1A.3 — Implement Hold-to-Unlock Interaction

**File:** `ui/doors/DoorsScreen.kt` + new `ui/doors/UnlockResultDialog.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1A.3.1 | Implement long-press detector (500ms) with `pointerInput` + `detectTapGestures(onLongPress)` | Unlock triggers after 500ms hold |
| 1A.3.2 | Add `CircularProgressIndicator` that fills during hold duration | Visual feedback of hold progress |
| 1A.3.3 | Trigger haptic feedback (`HapticFeedbackType.LongPress`) on threshold reached | Tactile confirmation |
| 1A.3.4 | Create full-screen `UnlockResultDialog` composable | Granted: green + check_circle; Denied: red + cancel; Loading: primary + rotating BLE icon |
| 1A.3.5 | Auto-dismiss dialog after 2 seconds on success | Returns to door list automatically |
| 1A.3.6 | Add vibration patterns: success `[0,100,50,100]`, failure `[0,250]` | Different haptic for each outcome |

---

## Phase 1B — Production BLE (3-5 days)

> Goal: Replace TCP simulator with Nordic BLE GATT client

### 1B.1 — Add Nordic BLE Dependency & Manager

**Files:** `build.gradle.kts`, new `core/ble/MistyisletBleManager.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1B.1.1 | Add `no.nordicsemi.android:ble:2.8.0` to dependencies | Gradle sync succeeds |
| 1B.1.2 | Create `MistyisletBleManager` extending Nordic `BleManager` | Compiles with proper overrides |
| 1B.1.3 | Define GATT service UUID `4d495354-5950-4153-532d-424c45415554` | UUID constant in companion object |
| 1B.1.4 | Define characteristics: CHALLENGE, AUTH_RESPONSE, READER_IDENTITY, AUTH_RESULT | 4 UUID constants |
| 1B.1.5 | Implement `initialize()`: read CHALLENGE → sign → write response → notify result | Full handshake in BLE manager |
| 1B.1.6 | Add MTU negotiation (request 64 bytes minimum) | Handles signature payload size |
| 1B.1.7 | Add connection timeout (10s) and retry logic (max 2 retries) | Graceful failure handling |

### 1B.2 — Update BLEAuthClient for Dual Transport

**File:** `core/ble/BLEAuthClient.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1B.2.1 | Add `authenticateViaBLE(deviceAddress: String, userId: String): AuthResult` method | New BLE path alongside existing TCP |
| 1B.2.2 | Add build config flag `BLE_USE_SIMULATOR` (true for debug, false for release) | Transport selection configurable |
| 1B.2.3 | Create `authenticate(door: AccessibleDoor, userId: String)` unified entry point | Auto-selects BLE or TCP based on config |
| 1B.2.4 | Keep `authenticateViaTCP` as-is for simulator mode | Backward compatible |

### 1B.3 — BLE Foreground Scanning Service

**Files:** new `core/ble/BLEScanService.kt`, `AndroidManifest.xml`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1B.3.1 | Create `BLEScanService` extending `Service` with `startForeground()` | Service starts without crash |
| 1B.3.2 | Create notification channel `ble_scanning` (IMPORTANCE_LOW) | Silent persistent notification |
| 1B.3.3 | Add `ScanFilter` by service UUID | Only discovers Mistyislet readers |
| 1B.3.4 | Implement scan duty cycle: 5s scan / 10s pause in background | Battery-efficient scanning |
| 1B.3.5 | Switch to `SCAN_MODE_LOW_LATENCY` when DoorsScreen is visible | Fast discovery when user is looking |
| 1B.3.6 | Emit discovered devices via `SharedFlow` for ViewModel consumption | UI can show "Door nearby" |
| 1B.3.7 | Add `START_STICKY` return from `onStartCommand` | Service restarts after OEM kill |
| 1B.3.8 | Add manifest declarations: service, foreground service type `connectedDevice` | System allows foreground |
| 1B.3.9 | Add battery optimization exemption request (user prompt) | Dialog asking to disable battery opt |
| 1B.3.10 | Add OEM auto-start detection (Samsung/OPPO/Xiaomi/vivo package names) | Show guide if auto-start disabled |

### 1B.4 — BLE Permission Flow

**File:** `ui/doors/DoorsViewModel.kt`, new `core/ble/BLEPermissionHelper.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1B.4.1 | Create `BLEPermissionHelper` with required permissions list (SDK 31+ vs older) | Correct permissions per API level |
| 1B.4.2 | Check `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` before scan | Prompt if missing |
| 1B.4.3 | Show rationale dialog before permission request | User understands why BLE needs location |
| 1B.4.4 | Handle permanent denial → Settings redirect | User can manually enable |

### 1B.5 — Real Device Testing

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 1B.5.1 | Test BLE connection on Samsung Galaxy A54 (StrongBox device) | End-to-end unlock < 300ms |
| 1B.5.2 | Test BLE connection on Xiaomi Redmi (TEE-only, aggressive ROM) | Service survives 10min background |
| 1B.5.3 | Test GATT error 133 recovery (most common Android BLE error) | Auto-retry succeeds |
| 1B.5.4 | Measure battery drain over 1 hour of background scanning | < 3% battery/hour |
| 1B.5.5 | Test with BLE simulator peripheral (nRF Connect or custom firmware) | Protocol validated end-to-end |

---

## Phase 2A — BiometricPrompt + FCM (4 days)

> Goal: Add biometric lock and push notifications

### 2A.1 — BiometricPrompt Integration

**Files:** new `core/auth/BiometricHelper.kt`, update `ProfileScreen.kt`, update `TokenStore.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2A.1.1 | Add `androidx.biometric:biometric:1.2.0-alpha05` dependency | Gradle sync |
| 2A.1.2 | Create `BiometricHelper` class with `authenticate(onSuccess, onError)` | Wraps BiometricPrompt API |
| 2A.1.3 | Support authentication types: `BIOMETRIC_STRONG`, `DEVICE_CREDENTIAL` fallback | Works with fingerprint, face, PIN |
| 2A.1.4 | Add `biometric_enabled` preference in DataStore | Persists user choice |
| 2A.1.5 | Gate app launch: if biometric enabled, show prompt before revealing main screen | Blocks access until verified |
| 2A.1.6 | Gate unlock action: require biometric before `viewModel.unlock()` call | Extra security on critical action |
| 2A.1.7 | Add biometric toggle switch in ProfileScreen settings section | User can enable/disable |
| 2A.1.8 | Handle `BiometricManager.canAuthenticate()` check — hide toggle if not available | Graceful on devices without biometric |
| 2A.1.9 | Handle enrollment state: prompt user to enroll if no biometrics registered | Navigate to system settings |

### 2A.2 — FCM Push Notifications

**Files:** `build.gradle.kts`, new `core/push/MistyisletMessagingService.kt`, new `core/push/NotificationHelper.kt`, `AndroidManifest.xml`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2A.2.1 | Add Firebase BOM + firebase-messaging dependency | Gradle sync |
| 2A.2.2 | Add `google-services.json` to `app/` directory | Firebase initialized at runtime |
| 2A.2.3 | Add `com.google.gms.google-services` plugin | Applied in build |
| 2A.2.4 | Create `MistyisletMessagingService` extending `FirebaseMessagingService` | Registered in manifest |
| 2A.2.5 | Implement `onNewToken`: POST token to `/app/devices/register` (create API endpoint interface) | Backend receives device token |
| 2A.2.6 | Add `DeviceApi.kt` with `registerDevice(pushToken, platform)` | Retrofit interface |
| 2A.2.7 | Implement `onMessageReceived`: route by `data["type"]` field | Dispatches to correct handler |
| 2A.2.8 | Create `NotificationHelper` with 4 channels: `security_alerts` (HIGH), `credential_alerts` (HIGH), `access_updates` (DEFAULT), `visitor_updates` (DEFAULT) | Channels created at app startup |
| 2A.2.9 | Build notification for each type with correct channel, icon, title, body | Notifications appear correctly |
| 2A.2.10 | Add deep link PendingIntent: tap notification → navigate to relevant screen | Tap opens correct screen |
| 2A.2.11 | Handle token refresh on login (re-register if token changed) | Token stays in sync |
| 2A.2.12 | Add notification permission request (Android 13+ `POST_NOTIFICATIONS`) | Runtime permission for API 33+ |

---

## Phase 2B — Scanner Screen Implementation (2 days)

> Goal: Build the QR scanner with CameraX + ML Kit

### 2B.1 — Add Scanner Dependencies

**File:** `build.gradle.kts`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2B.1.1 | Add CameraX dependencies (camera2, lifecycle, view) | Gradle sync |
| 2B.1.2 | Add ML Kit barcode scanning dependency | Gradle sync |
| 2B.1.3 | Add camera permission to AndroidManifest | `<uses-permission android:name="android.permission.CAMERA" />` |

### 2B.2 — Scanner Screen UI

**File:** `ui/scanner/ScannerScreen.kt` (rewrite existing)

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2B.2.1 | Implement `CameraX PreviewView` full-screen | Camera preview fills screen |
| 2B.2.2 | Create semi-transparent overlay with rounded rectangle viewfinder cutout | Dark overlay with clear scan area |
| 2B.2.3 | Add animated corner brackets on viewfinder edges | Visual scan target indicator |
| 2B.2.4 | Add instruction text below viewfinder: "Point camera at QR code" | User guidance |
| 2B.2.5 | Add flashlight toggle button (top-right) | Works in dark environment |
| 2B.2.6 | Add camera permission request flow with rationale | Graceful permission handling |
| 2B.2.7 | Handle permission denied → show explanation with settings button | Recovery path |

### 2B.3 — Scanner Logic

**File:** `ui/scanner/ScannerViewModel.kt` (rewrite existing)

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2B.3.1 | Configure ML Kit `BarcodeScanner` with `FORMAT_QR_CODE` only | Only recognizes QR codes |
| 2B.3.2 | Attach `ImageAnalysis` use case to CameraX | Frames processed in real-time |
| 2B.3.3 | Parse QR content: detect `https://app.mistyislet.com/access-link/{token}` format | Extracts token from URL |
| 2B.3.4 | Parse QR content: detect `mistyislet://qr/{qr_token}?lock_id={lock_id}` format | Extracts token + lock_id |
| 2B.3.5 | On valid QR detected → call `POST /app/access/qr-unlock` automatically | Auto-unlock without user tap |
| 2B.3.6 | Show unlock result (reuse `UnlockResultDialog` from Phase 1A) | Same visual feedback as BLE unlock |
| 2B.3.7 | Add 3-second cooldown after scan to prevent duplicate calls | Debounce |
| 2B.3.8 | Haptic feedback on QR detection | User knows scan succeeded |

---

## Phase 2C — Visitors Screen (2 days)

> Goal: Build visitor pass management UI

### 2C.1 — Visitors List Screen

**Files:** new `ui/visitors/VisitorsScreen.kt`, new `ui/visitors/VisitorsViewModel.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2C.1.1 | Create `VisitorsViewModel` with `loadVisitorPasses()` from `/app/visitor-passes` | Fetches visitor list |
| 2C.1.2 | Create `VisitorsScreen` with M3 `Scaffold` | Basic screen skeleton |
| 2C.1.3 | LazyColumn with section headers: "ACTIVE" and "EXPIRED" | Grouped by status |
| 2C.1.4 | Active visitor item: name, company, countdown chip (`⏱ 22h remaining`), QR/Copy/Share buttons | Full info display |
| 2C.1.5 | Expired visitor item: name with "— expired" suffix, grayed out | Visual distinction |
| 2C.1.6 | FAB (bottom-right) → triggers create bottom sheet | Standard M3 FAB |
| 2C.1.7 | Pull-to-refresh | Reload on swipe down |
| 2C.1.8 | Empty state: illustration + "No visitor passes yet" text | Friendly empty state |

### 2C.2 — Create Visitor Bottom Sheet

**File:** `ui/visitors/CreateVisitorSheet.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2C.2.1 | `ModalBottomSheet` with form fields | Sheet slides up from bottom |
| 2C.2.2 | Field: Visitor name (OutlinedTextField, required, validation) | Non-empty validation |
| 2C.2.3 | Field: Phone number (OutlinedTextField, required, number keyboard) | Phone input |
| 2C.2.4 | Field: Host name (OutlinedTextField, required) | Who they're visiting |
| 2C.2.5 | Field: Door selection (MultiSelectChip group from user's accessible doors) | Can select multiple doors |
| 2C.2.6 | Field: Duration (M3 SegmentedButton: 4h / 8h / 24h / 48h / 72h) | Preset durations |
| 2C.2.7 | Field: ID Document type (DropdownMenu: None / KTP / KITAS / ITAS) | Indonesia-specific |
| 2C.2.8 | Submit button → `POST /app/visitor-passes` → navigate to QR display | Creates pass and shows QR |
| 2C.2.9 | Loading state on submit button | Disable while creating |
| 2C.2.10 | Error handling: show toast on failure | User sees error message |

### 2C.3 — Visitor QR Display Screen

**File:** new `ui/visitors/VisitorQRScreen.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2C.3.1 | Full-width QR code generated from access token (ZXing `BarcodeEncoder`) | QR renders correctly |
| 2C.3.2 | Token text with "Copy" button (copies to clipboard) | Clipboard copy works |
| 2C.3.3 | Expiry countdown timer (updates every second) | Live countdown |
| 2C.3.4 | "Share" button → `Intent.ACTION_SEND` with QR bitmap as image | System share sheet opens |
| 2C.3.5 | Save QR to gallery option | Optional save |

---

## Phase 2D — Profile Refactor + Credentials Move (1 day)

> Goal: Move Credentials into Profile, add settings section

### 2D.1 — Profile Screen Restructure

**File:** `ui/profile/ProfileScreen.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 2D.1.1 | Section 1 "User Info": avatar placeholder, name, email from `/app/me` | User sees their info |
| 2D.1.2 | Section 2 "My Credentials": navigate to existing `CredentialsScreen` | Tap opens credentials |
| 2D.1.3 | Add credential summary in section: count of active credentials, keystore level badge | Quick overview |
| 2D.1.4 | Section 3 "Settings": Language picker (en-US / zh-CN / id-ID) | Dropdown with 3 options |
| 2D.1.5 | Settings: Biometric lock toggle (from Phase 2A) | Toggle switch |
| 2D.1.6 | Settings: Notification toggle | Enable/disable push |
| 2D.1.7 | Settings: About (app version, licenses) | Info display |
| 2D.1.8 | Section 4 "Logout": Danger button, clears token, navigates to login | Clean logout |

---

## Phase 3A — NFC Card Binding (2 days)

> Goal: Allow users to self-bind physical DESFire EV3 cards

### 3A.1 — NFC Reader Module

**Files:** new `core/nfc/NFCReader.kt`, update `AndroidManifest.xml`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 3A.1.1 | Add NFC permissions to manifest: `android.permission.NFC` | Permission declared |
| 3A.1.2 | Add `<uses-feature android:name="android.hardware.nfc" android:required="false" />` | Optional feature |
| 3A.1.3 | Create `NFCReader` class with `enableReaderMode()` | Activates NFC reading |
| 3A.1.4 | Read NFC-A and NFC-B tag types | Supports DESFire EV3 |
| 3A.1.5 | Extract tag UID as hex string | Returns card identifier |
| 3A.1.6 | Disable reader mode on pause/destroy | Clean lifecycle |

### 3A.2 — Card Binding UI Flow

**File:** new `ui/credentials/BindCardScreen.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 3A.2.1 | Entry point: "Bind Physical Card" button in CredentialsScreen | Navigates to bind flow |
| 3A.2.2 | Step 1: Instruction screen "Hold your card against the back of your phone" with animation | User knows what to do |
| 3A.2.3 | Step 2: Listening state with pulsing NFC icon | Visual feedback waiting |
| 3A.2.4 | Step 3: Card detected → show UID → confirm button | User verifies card |
| 3A.2.5 | Step 4: POST card UID to backend binding API | Card registered |
| 3A.2.6 | Success state: "Card bound successfully" with checkmark | Flow complete |
| 3A.2.7 | Handle devices without NFC: hide "Bind Card" button entirely | No broken flows |
| 3A.2.8 | Handle NFC disabled: prompt user to enable in settings | Recovery path |

---

## Phase 3B — Home Screen Widget (2 days)

> Goal: Glance widget showing quick-unlock for favorite doors

### 3B.1 — Widget Implementation

**Files:** new `widget/UnlockWidget.kt`, new `widget/UnlockWidgetReceiver.kt`, `AndroidManifest.xml`, new `res/xml/unlock_widget_info.xml`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 3B.1.1 | Add Glance dependency `androidx.glance:glance-appwidget:1.1.0` | Gradle sync |
| 3B.1.2 | Create `UnlockWidget` composable showing top 3 most-used doors | Widget renders door list |
| 3B.1.3 | Each door item: name + tap action → launch app with door ID extra | Tap opens app to that door |
| 3B.1.4 | Show gateway status dot (green/gray) per door | At-a-glance status |
| 3B.1.5 | Create `UnlockWidgetReceiver` extending `GlanceAppWidgetReceiver` | Android system can instantiate |
| 3B.1.6 | Register widget in manifest with `<receiver>` + `<meta-data>` | Widget appears in picker |
| 3B.1.7 | Create `unlock_widget_info.xml` with size constraints (min 250dp x 110dp) | Proper sizing |
| 3B.1.8 | Update widget data via WorkManager periodic (every 15min) | Status stays fresh |
| 3B.1.9 | Style with M3 colors (surface container, rounded corners) | Consistent branding |

---

## Phase 3C — Multi-Site Switching (1 day)

> Goal: Users with access to multiple buildings can switch context

### 3C.1 — Site Picker

**Files:** update `ui/doors/DoorsScreen.kt`, update `DoorsViewModel.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 3C.1.1 | Add site/building picker dropdown in `TopAppBar` trailing action | Dropdown visible in header |
| 3C.1.2 | Populate buildings from door data (distinct `building_id` values) | Shows available buildings |
| 3C.1.3 | Filter door list by selected building | Only selected site's doors shown |
| 3C.1.4 | Persist selection in DataStore | Remembers across app restarts |
| 3C.1.5 | "All Sites" option as default | Shows everything if not filtered |

---

## Phase 3D — Localization Polish (1 day)

> Goal: Complete i18n for all 3 languages

### 3D.1 — String Resources

**Files:** `res/values/strings.xml`, `res/values-zh-rCN/strings.xml`, `res/values-in/strings.xml`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 3D.1.1 | Audit all hardcoded strings in Kotlin files → extract to string resources | Zero hardcoded user-facing strings |
| 3D.1.2 | Complete English strings.xml (base, 100%) | All keys defined |
| 3D.1.3 | Translate all keys to Simplified Chinese | 100% coverage |
| 3D.1.4 | Translate all keys to Bahasa Indonesia | 100% coverage |
| 3D.1.5 | Add in-app language picker (Per-app language API on Android 13+, manual AppCompat on older) | Language switchable |
| 3D.1.6 | Test all screens in each language (layout overflow, text truncation) | No visual bugs |

---

## Phase 4A — Google Wallet Integration (2 days)

> Goal: "Add to Google Wallet" button for eligible credentials

### 4A.1 — Wallet Helper

**Files:** new `core/wallet/WalletHelper.kt`, update `ui/credentials/CredentialsScreen.kt`

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4A.1.1 | Add `play-services-pay` dependency | Gradle sync |
| 4A.1.2 | Create `WalletHelper` with `isAvailable()` check | Returns false if Wallet unavailable |
| 4A.1.3 | Implement `addToGoogleWallet(saveLink: String)` using `SavePassesRequest` | Launches Wallet save flow |
| 4A.1.4 | Add "Add to Google Wallet" button in CredentialsScreen (only if `isAvailable && saveLink != null`) | Conditional display |
| 4A.1.5 | Handle result callback (success/cancel/error) | Toast feedback |
| 4A.1.6 | Hide entirely in Indonesia if Wallet unavailable (no error state) | Graceful degradation |

---

## Phase 4B — Security Hardening (2 days)

> Goal: Production-ready security measures

### 4B.1 — Network Security

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4B.1.1 | Add certificate pinning for `api.mistyislet.com` in `network_security_config.xml` | Pinned in release builds |
| 4B.1.2 | Add `CleartextTrafficPermitted=false` for release | No HTTP in production |
| 4B.1.3 | Add OkHttp `CertificatePinner` as backup | Double-layer pinning |

### 4B.2 — Local Security

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4B.2.1 | Add root detection (SafetyNet/Play Integrity check) → warning dialog | User informed |
| 4B.2.2 | Verify `debuggable=false` in release build type | Already set, validate |
| 4B.2.3 | Add Timber with no-op release tree (strip all logs) | No sensitive logs in release |
| 4B.2.4 | Validate ProGuard/R8 rules: test release APK doesn't crash | Obfuscation works |
| 4B.2.5 | Ensure EncryptedSharedPreferences key is tied to hardware keystore | Secure token storage |

### 4B.3 — BLE Security

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4B.3.1 | Validate challenge expiry (reject if `expires_at` < now) client-side | Stale challenges rejected |
| 4B.3.2 | Add RSSI threshold check (-80 dBm minimum) to prevent relay attacks | Too-weak signals rejected |
| 4B.3.3 | Verify attestation chain is sent during credential registration | Backend can verify hardware-backed key |

---

## Phase 4C — Play Store Submission (3 days)

> Goal: App published to Play Store internal track

### 4C.1 — Release Preparation

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4C.1.1 | Generate release keystore `mistyislet-release.jks` | Securely stored |
| 4C.1.2 | Configure signing in `build.gradle.kts` release block | Signed APK/AAB builds |
| 4C.1.3 | Enable Play App Signing (upload key vs signing key) | Google manages signing |
| 4C.1.4 | Test release AAB on 3+ real devices | No crashes |
| 4C.1.5 | Verify app size < 15MB (excluding dynamic features) | Reasonable download size |

### 4C.2 — Store Listing

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4C.2.1 | Create screenshots (phone): Login, Doors, Unlock success, Scanner, Visitors | 5+ screenshots |
| 4C.2.2 | Create feature graphic (1024x500) | Required by Play Store |
| 4C.2.3 | Write app description in English, Chinese, Indonesian | 3 locales |
| 4C.2.4 | Write privacy policy (hosted URL) | Required for BLE/camera permissions |
| 4C.2.5 | Prepare BLE permission declaration for review team | Explains why BLE needed |
| 4C.2.6 | Prepare location permission declaration | Explains BLE scanning needs location |
| 4C.2.7 | Set content rating questionnaire | Appropriate rating |

### 4C.3 — Submission

| Sub-task | Detail | Acceptance Criteria |
|----------|--------|-------------------|
| 4C.3.1 | Upload AAB to internal testing track | Build uploaded |
| 4C.3.2 | Add internal testers (team emails) | Testers can install |
| 4C.3.3 | Run pre-launch report (Firebase Test Lab) | No critical issues |
| 4C.3.4 | Fix any pre-launch report crashes | All resolved |
| 4C.3.5 | Promote to closed beta | Wider testing |
| 4C.3.6 | Collect beta feedback (1 week) | Iterate on issues |
| 4C.3.7 | Promote to production (staged rollout 10% → 50% → 100%) | Live on Play Store |

---

## Timeline Summary

```
Week 1:  Phase 1A (nav restructure) + Phase 1B (BLE)
Week 2:  Phase 2A (biometric + FCM)
Week 3:  Phase 2B (scanner) + Phase 2C (visitors) + Phase 2D (profile)
Week 4:  Phase 3A (NFC) + Phase 3B (widget) + Phase 3C (multi-site) + Phase 3D (i18n)
Week 5:  Phase 4A (Wallet) + Phase 4B (security)
Week 6:  Phase 4C (Play Store submission)
```

| Phase | Tasks | Total Sub-items |
|-------|-------|----------------|
| 1A Nav & UI | 3 sprints | 19 items |
| 1B BLE | 5 sprints | 26 items |
| 2A Biometric + FCM | 2 sprints | 21 items |
| 2B Scanner | 3 sprints | 18 items |
| 2C Visitors | 3 sprints | 23 items |
| 2D Profile | 1 sprint | 8 items |
| 3A NFC | 2 sprints | 14 items |
| 3B Widget | 1 sprint | 9 items |
| 3C Multi-site | 1 sprint | 5 items |
| 3D i18n | 1 sprint | 6 items |
| 4A Wallet | 1 sprint | 6 items |
| 4B Security | 3 sprints | 11 items |
| 4C Play Store | 3 sprints | 19 items |
| **TOTAL** | **26 sprints** | **185 items** |
