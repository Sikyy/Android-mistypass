# NFC HCE + Offline Verification Enhancement

## Goal

Add NFC Host Card Emulation (HCE) to Android so the phone acts as an NFC credential at door readers, and harden offline verification so gateways can securely authenticate users without cloud connectivity. Both features reuse the existing ECDSA P-256 challenge-response protocol with a new v2 wire format.

## Architecture

The system uses a unified authentication protocol with pluggable transport layers. A single EC P-256 keypair (stored in Android Keystore TEE/StrongBox) serves both BLE and NFC paths. The gateway runs multiple reader adapters behind a common interface, each feeding into the same signature verification logic.

```
                    +-------------------+
                    |   AuthProtocol v2 |  ECDSA P-256, transport-bound signatures
                    +--------+----------+
                             |
                    +--------+----------+
                    |  TransportBinding  |  "BLE" | "NFC_HCE"
                    +----+----------+---+
                         |          |
                    +----v---+ +----v---+
                    |  BLE   | |  NFC   |  GATT chars vs ISO-DEP APDU
                    +--------+ +--------+
                         |          |
                    +----v----------v---+
                    | Gateway Agent     |
                    | ReaderAdapter     |
                    |  +- BLEReader     |
                    |  +- NFCReader     |  PC/SC (ACS WalletMate 2, generic)
                    |  +- TCPSimulator  |  dev/test
                    +-------------------+
```

## Tech Stack

- **Android**: HostApduService (API 19+), Android Keystore, existing KeystoreManager
- **Backend/Gateway**: Go, PC/SC via `github.com/ebfe/scard`, NATS pub/sub, BoltDB local storage
- **Protocol**: ISO 7816-4 APDU, ECDSA P-256, SHA-256

---

## 1. AuthProtocol v2 (Wire Format)

### 1.1 Challenge Structure (52 bytes)

```
Offset  Size  Field         Encoding
------  ----  -----         --------
0       32    nonce         crypto/rand random bytes
32       8    issued_at     uint64 BigEndian, unix seconds
40       8    expires_at    uint64 BigEndian, unix seconds
48       4    gateway_id    uint32 BigEndian, gateway numeric ID
------
Total:  52 bytes
```

Challenge validity window: 30 seconds (`expires_at - issued_at == 30`).

Gateway MUST reject auth responses where `gateway_id` does not match its own ID (prevents cross-gateway replay within the same time window).

### 1.2 Signature Input Formula

```
message = nonce(32B) || userId(UTF-8 bytes) || transportTag(UTF-8 bytes)
hash    = SHA-256(message)
sig     = ECDSA_Sign(privateKey, hash)
```

Transport tags:
- BLE path: `"BLE"` (3 bytes)
- NFC HCE path: `"NFC_HCE"` (7 bytes)

The transport tag binds the signature to a specific channel. A signature produced for BLE cannot be replayed on the NFC path and vice versa, even if the same nonce is used.

### 1.3 Auth Response Format (variable length)

```
Offset  Size       Field
------  ----       -----
0       1          userId_len (uint8)
1       userId_len userId (UTF-8 bytes)
1+len   variable   signature (ECDSA ASN.1 DER, typically 70-72 bytes)
```

Maximum total size: 1 + 255 + 72 = 328 bytes. However, ISO-DEP with `Le=0x00` limits responses to 256 bytes. Since userId is a UUID (36 bytes), actual responses are ~111 bytes — well within the limit. **Implementation must enforce `userId_len <= 180` bytes** (assertion) to guarantee responses stay under 256 bytes without requiring extended APDU.

### 1.4 Auth Result Format

```
Offset  Size      Field
------  ----      -----
0       1         code (uint8)
1       variable  reason (UTF-8 string)
```

Result codes:
- `0x01` GRANTED
- `0x02` DENIED
- `0x03` EXPIRED_CHALLENGE
- `0x04` INVALID_SIGNATURE
- `0x05` UNKNOWN_USER
- `0x06` CREDENTIAL_EXPIRED
- `0x07` CREDENTIAL_REVOKED (new)
- `0x08` CREDENTIAL_SUSPENDED (new)
- `0x09` GATEWAY_OFFLINE_LIMIT (new, MaxOfflineDuration exceeded)

### 1.5 Protocol Version Negotiation

The v2 challenge is 52 bytes; v1 was 48 bytes. Gateway and app detect version by challenge length:
- 48 bytes = v1 (legacy, no gateway_id, no transport binding)
- 52 bytes = v2

During migration, gateway sends v2 challenges. If an older app returns an error or no response, gateway falls back to v1 for that session. This allows rolling upgrades without a hard cutover.

Backend `VerifyBLESignature()` accepts both formats:
- If transport tag is present in the registered credential metadata, verify with tag
- If not (v1 credential), verify without tag (backward compatible)

---

## 2. NFC APDU Frame Format

### 2.1 AID Registration

```
AID: F0 4D 49 53 54 59 01 00   (8 bytes)
      |  M  I  S  T  Y  v1 rsvd
      F0 = proprietary RID prefix
```

### 2.2 SELECT Command

```
-> 00 A4 04 00 08 F04D495354590100 00
   CLA=00 INS=A4(SELECT) P1=04(by-name) P2=00 Lc=08 [AID] Le=00

<- 02 01 03 90 00
   [protocolVersion=0x02][reserved=0x01][capabilities=0x03] SW=9000(OK)
   
   capabilities bitmask:
     bit 0: challenge-response supported
     bit 1: offline-token supported (reserved for future)
```

### 2.3 AUTHENTICATE Command

```
-> 80 88 00 00 34 [52-byte challenge] 00
   CLA=80(proprietary) INS=88(AUTHENTICATE) P1=00 P2=00 Lc=0x34(52)

<- [1B userId_len][userId bytes][ECDSA DER signature] 90 00
```

The phone's HceService:
1. Extracts the 52-byte challenge from the command data
2. Parses nonce (bytes 0-31)
3. Reads userId from local storage
4. Computes `SHA256(nonce || userId || "NFC_HCE")`
5. Signs with KeystoreManager using the existing `mistyislet_ble_credential` key
6. Returns `[1B len][userId][signature]` + SW 9000

### 2.4 Error Status Words

| SW     | Meaning                        | When                              |
|--------|--------------------------------|-----------------------------------|
| 90 00  | Success                        | Normal response                   |
| 69 82  | Security status not satisfied  | Device locked + 2FA required      |
| 69 85  | Conditions of use not satisfied| Credential expired/suspended      |
| 6A 82  | Application not found          | AID mismatch                      |
| 6F 00  | Internal error                 | Keystore failure, unexpected      |

### 2.5 Timing

NFC ISO-DEP has a ~5 second transaction timeout. The SELECT + AUTHENTICATE round trip must complete within this window. Expected timing:
- SELECT: < 5ms
- AUTHENTICATE (Keystore sign): 50-200ms (TEE), 100-500ms (StrongBox)
- Total: < 1 second, well within the 5s limit

---

## 3. Android HceService

### 3.1 File Structure

```
app/src/main/java/com/mistyislet/app/core/nfc/
    HceService.kt          -- HostApduService implementation
    HceProtocol.kt         -- APDU constants, parsing, construction
    (existing) NFCReader.kt -- unchanged, physical card binding
    
app/src/main/res/xml/
    hce_apdu_service.xml   -- AID registration for Android NFC routing
```

### 3.2 HceService Contract

```kotlin
class HceService : HostApduService() {

    // Injected via Hilt entry point (HostApduService can't use constructor injection)
    private val keystoreManager: KeystoreManager  // existing, shared with BLE
    private val credentialStore: CredentialStore   // reads userId, credential status

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray
    // Routes to handleSelect() or handleAuthenticate() based on parsed APDU

    override fun onDeactivated(reason: Int)
    // Logs deactivation reason (link loss or another AID selected)
}
```

### 3.3 Manifest Declaration

```xml
<service
    android:name=".core.nfc.HceService"
    android:exported="true"
    android:permission="android.permission.BIND_NFC_SERVICE">
    <intent-filter>
        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
    </intent-filter>
    <meta-data
        android:name="android.nfc.cardemulation.host_apdu_service"
        android:resource="@xml/hce_apdu_service" />
</service>
```

### 3.4 AID XML

```xml
<!-- res/xml/hce_apdu_service.xml -->
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/hce_service_description"
    android:requireDeviceUnlock="false">
    <aid-group
        android:category="other"
        android:description="@string/hce_aid_group_description">
        <aid-filter android:name="F04D495354590100" />
    </aid-group>
</host-apdu-service>
```

`requireDeviceUnlock` is permanently `false` in XML — this is a static attribute and cannot be changed at runtime.

**2FA is implemented entirely through Android Keystore**, not the XML attribute:

- **Single-factor (default):** Keystore key generated with `setUserAuthenticationRequired(false)`. HceService signs freely, NFC tap works even with locked screen.
- **2FA enabled:** User toggles 2FA in settings → app regenerates the Keystore key with `setUserAuthenticationRequired(true)` and `setUserAuthenticationValidityDurationSeconds(-1)` (require auth on every sign). The key is re-registered with the backend (new public key).
- **HceService behavior with 2FA:** The APDU is always received (XML allows it), but `keystoreManager.sign()` throws `UserNotAuthenticatedException` when device is locked. HceService catches this and returns SW `69 82` (security status not satisfied).

```kotlin
// In HceService.handleAuthenticate():
try {
    val sig = keystoreManager.sign(payload)
    return buildSuccessResponse(userId, sig)
} catch (e: UserNotAuthenticatedException) {
    return HceProtocol.SW_SECURITY_NOT_SATISFIED  // 69 82
}
```

This means the NFC service is always listening, but signing only succeeds when the device is unlocked (biometric/PIN verified). The user must unlock their phone before tapping the reader — matching Kisi's optional 2FA behavior.

### 3.5 Credential Store

HceService needs to read the current userId without network access. This comes from Room DB (`CachedCredential` table) which is populated during login and credential registration. If no credential is cached, HceService returns SW `69 85`.

---

## 4. Gateway ReaderAdapter

### 4.1 Interface

```go
// reader.go
package gateway

type AuthResponse struct {
    UserID    string
    Signature []byte // ECDSA ASN.1 DER
}

type ReaderAdapter interface {
    Name() string
    Type() string // "ble" | "nfc" | "tcp_simulator"
    Authenticate(challenge []byte) (*AuthResponse, error)
    Close() error
}
```

All three implementations (BLEReader, NFCReader, TCPSimulatorReader) conform to this interface. The gateway agent iterates over enabled readers, and whichever receives a credential response first wins.

### 4.2 NFC Reader (PC/SC)

```go
// nfc_reader.go
type NFCReader struct {
    driver NFCDriver
    aid    []byte // F04D495354590100
}

type NFCDriver interface {
    WaitForCard(ctx context.Context) error
    Transmit(command []byte) ([]byte, error)
    Disconnect() error
}

// Implementations
type PCSCDriver struct { ... }       // Generic PC/SC via scard library
type TCPNFCSimDriver struct { ... }  // TCP socket for dev testing
```

NFCReader.Authenticate flow:
1. `driver.WaitForCard()` blocks until a phone/card is presented
2. Send SELECT AID command, parse response, check protocol version
3. Generate 52-byte challenge (nonce + timestamps + self gateway_id)
4. Send AUTHENTICATE command with challenge
5. Parse response: extract userId + signature
6. Return `AuthResponse` to gateway for verification

### 4.3 Gateway Configuration

```yaml
# gateway-agent/config.yaml
gateway_id: 1234  # uint32, used in challenge

readers:
  - type: ble
    enabled: true
  - type: nfc
    enabled: true
    driver: pcsc          # pcsc | tcp_simulator
    pcsc_reader_name: ""  # empty = auto-detect first reader
  - type: tcp_simulator
    enabled: false
    port: 9900

offline:
  max_offline_duration: 72h
  grace_period_expired: 24h
  max_offline_unlocks: 100
  credential_sync_interval: 5m
  audit_batch_size: 50
```

### 4.4 Unified Verification

After any ReaderAdapter returns an AuthResponse, the gateway runs the same verification. **Check order is fast-to-slow: nonce cache → gateway_id → credential lookup → status → expiry → ECDSA verify (most expensive last).**

```go
func (g *Gateway) verify(resp *AuthResponse, challenge []byte, transport string) VerifyResult {
    nonce := challenge[:32]

    // 1. Nonce reuse check (fast, in-memory LRU, prevents replay within 30s window)
    if g.nonceCache.Contains(nonce) {
        return VerifyResult{Code: ResultDenied, Reason: "nonce_reuse"}
    }

    // 2. Gateway ID check (fast, prevents cross-gateway replay)
    gatewayID := binary.BigEndian.Uint32(challenge[48:52])
    if gatewayID != g.config.GatewayID {
        return VerifyResult{Code: ResultDenied, Reason: "gateway_id_mismatch"}
    }

    // 3. Credential lookup
    cred := g.credentialCache.FindByUserID(resp.UserID)
    if cred == nil {
        return VerifyResult{Code: ResultUnknownUser}
    }

    // 4. Revocation check (hard deny, no grace period ever)
    if cred.RevokedAt != nil {
        return VerifyResult{Code: ResultCredentialRevoked}
    }
    if cred.SuspendedAt != nil {
        return VerifyResult{Code: ResultCredentialSuspended}
    }

    // 5. Expiry check (grace period for natural expiration only)
    if time.Now().Unix() > cred.ExpiresAt {
        elapsed := time.Since(time.Unix(cred.ExpiresAt, 0))
        if elapsed <= g.config.Offline.GracePeriodExpired {
            // Allow with grace_period flag (logged in audit)
        } else {
            return VerifyResult{Code: ResultCredentialExpired}
        }
    }

    // 6. Offline limits check
    if g.isOffline() {
        if g.offlineDuration() > g.config.Offline.MaxOfflineDuration {
            return VerifyResult{Code: ResultGatewayOfflineLimit}
        }
        if g.offlineUnlockCount >= g.config.Offline.MaxOfflineUnlocks {
            return VerifyResult{Code: ResultGatewayOfflineLimit}
        }
    }

    // 7. ECDSA signature verification (most expensive, last)
    message := append(nonce, []byte(resp.UserID)...)
    message = append(message, []byte(transport)...)  // "BLE" or "NFC_HCE"
    hash := sha256.Sum256(message)

    if !verifyECDSA(cred.PublicKeyPEM, hash[:], resp.Signature) {
        return VerifyResult{Code: ResultInvalidSignature}
    }

    // 8. Mark nonce as used (TTL=30s, LRU max 10000 entries)
    g.nonceCache.Add(nonce, 30*time.Second)

    return VerifyResult{Code: ResultGranted}
}
```

**nonceCache** is an in-memory LRU with 30-second TTL per entry and a maximum of 10,000 entries. Memory footprint: ~320KB (32 bytes per nonce × 10,000). Entries auto-evict after TTL, so the cache self-cleans. The nonce is marked as used only AFTER successful verification to avoid locking out legitimate retries on transient failures.

---

## 5. Offline Verification

### 5.1 Gateway Credential Sync (Enhanced)

```go
type GatewayCredentialSync struct {
    UserID       string   `json:"user_id"`
    UserEmail    string   `json:"user_email"`
    PublicKeyPEM string   `json:"public_key_pem"`
    LockIDs      []string `json:"lock_ids"`
    ExpiresAt    int64    `json:"expires_at"`
    RevokedAt    *int64   `json:"revoked_at,omitempty"`
    SuspendedAt  *int64   `json:"suspended_at,omitempty"`
    SyncVersion  int64    `json:"sync_version"`
}
```

### 5.2 Sync Mechanism

**Periodic pull** (every 5 minutes while online):
```
GET /api/v1/gateways/{id}/credentials/sync?since_version={lastSyncVersion}
```
Returns only credentials changed since `lastSyncVersion`. Gateway stores in BoltDB.

**Real-time push** (revocation/suspension):
```
NATS subject: mistypass.gateway.{gatewayID}.credential_update
Payload: GatewayCredentialSync (single credential with updated RevokedAt/SuspendedAt)
```

This ensures revocations propagate within seconds when gateway is online.

### 5.3 Offline Decision Matrix

```
Credential State    | Online Gateway  | Offline Gateway
--------------------|-----------------|------------------
active + valid      | GRANTED         | GRANTED
active + expired    | DENIED          | GRACE (if < 24h)
suspended           | DENIED          | DENIED (hard)
revoked             | DENIED          | DENIED (hard)
unknown user        | DENIED          | DENIED
any + offline > 72h | N/A             | DENIED (all users)
any + unlocks > 100 | N/A             | DENIED (all users)
```

Key principle: **revocation and suspension always hard-deny, even offline. Only natural expiration gets a grace period.** This ensures that when an admin revokes a lost phone's credential, it takes effect at every gateway that has synced, with zero grace.

### 5.4 Offline Audit Log

```go
type OfflineAuditEntry struct {
    EventID     string `json:"event_id"`      // UUIDv7 (time-ordered)
    UserID      string `json:"user_id"`
    LockID      string `json:"lock_id"`
    Method      string `json:"method"`         // "ble" | "nfc_hce"
    Result      string `json:"result"`         // "granted" | "denied" | "grace_period"
    Reason      string `json:"reason"`
    GatewayID   uint32 `json:"gateway_id"`
    Timestamp   int64  `json:"timestamp"`
    IsOffline   bool   `json:"is_offline"`
    SyncedAt    *int64 `json:"synced_at,omitempty"`
}
```

- Stored in BoltDB on gateway, FIFO queue
- When online, batched upload: `POST /api/v1/gateways/{id}/audit/batch`
- Cloud deduplicates by EventID (UUIDv7 guarantees uniqueness)
- Maximum local queue: 10,000 entries (oldest evicted if full)

---

## 6. BLE Protocol v2 Migration

Since v2 changes the challenge structure (48 → 52 bytes) and signature formula (adds transport tag), both BLE and NFC paths need updates.

### 6.1 Backend Changes

- `ble_protocol.go`: Add `GatewayID` field to challenge struct, update `ChallengeSize` constant to 52
- `credential/service.go`: `VerifyBLESignature()` accepts optional `transportTag` parameter. If v2, verify with tag; if v1, verify without
- New endpoint: `GET /api/v1/gateways/{id}/credentials/sync` with `since_version` query param
- New endpoint: `POST /api/v1/gateways/{id}/audit/batch`
- NATS publish on credential status change: `mistypass.gateway.{gatewayID}.credential_update`

### 6.2 Android Changes

- `BLEAuthClient.kt`: Read 52-byte challenge (was 48), append `"BLE"` to sign payload
- `KeystoreManager.kt`: New `signChallengeV2(nonce, userId, transportTag)` method
- New `HceService.kt`, `HceProtocol.kt`, `hce_apdu_service.xml`
- Manifest: Add HceService with `BIND_NFC_SERVICE` permission
- Strings: Add HCE description strings (EN/ZH/ID)

### 6.3 iOS Changes

- BLE auth client: Read 52-byte challenge, append `"BLE"` to sign payload
- No NFC HCE (iOS does not support HostApduService)

### 6.4 Migration Strategy

1. Deploy backend with v2 support (backward compatible, accepts both v1 and v2)
2. Update gateway-agent to send v2 challenges
3. Update Android app (BLE v2 + new HCE)
4. Update iOS app (BLE v2 only)
5. After all clients updated, deprecate v1 path in backend

---

## 7. Operational Prerequisites

### 7.1 Gateway NFC Reader

For PC/SC-based NFC reading, the gateway host must have:

```bash
# Debian/Ubuntu (including Raspberry Pi)
sudo apt install pcscd pcsc-tools libpcsclite-dev
sudo systemctl enable pcscd

# Verify reader detected
pcsc_scan
```

Supported readers:
- ACS WalletMate II (ACR1552U, USB CCID, Apple MFi + Google Smart Tap certified, demo/dev)
- Any PC/SC compatible USB contactless reader (generic CCID driver)
- TCP simulator (no hardware needed, for development)

Note: ACS ACR1255U-J1 is a *Bluetooth* NFC reader and requires BLE GATT communication, NOT PC/SC. The WalletMate II (ACR1552U) connects via USB and is detected by `pcscd` as a standard CCID device — this is the correct driver path.

### 7.2 Android Requirements

- Minimum API 19 (KitKat) for HCE — app already targets minSdk 26
- NFC hardware required on device (`<uses-feature android:name="android.hardware.nfc.hce" android:required="false" />`)
- User must have NFC enabled in device settings
- App handles graceful degradation when NFC unavailable

### 7.3 Gateway Minimum Hardware

- Raspberry Pi 3B+ or better (for production gateways)
- USB port for NFC reader
- Network connectivity (Ethernet or WiFi, with offline tolerance)
- 100MB+ free disk for BoltDB credential cache + audit log

---

## 8. Security Summary

| Threat                | Mitigation                                          | Kisi Parity |
|-----------------------|-----------------------------------------------------|-------------|
| Static ID cloning     | Dynamic challenge-response, never static             | Equivalent  |
| Cross-transport replay| Transport tag in signature ("BLE" vs "NFC_HCE")     | Exceeds     |
| Cross-gateway replay  | gateway_id in challenge, verified on receipt          | Exceeds     |
| NFC relay attack      | 30s window + 4cm physical distance                   | Equivalent  |
| Token replay          | Random nonce per challenge + nonce cache (30s TTL LRU)| Exceeds     |
| Phone root/reverse    | TEE/StrongBox, private key not exportable             | Equivalent  |
| Phone lost            | Cloud revoke -> NATS push -> gateway hard deny        | Equivalent  |
| Gateway long offline  | 72h hard limit + 100 unlock limit                    | Exceeds     |
| Expired credential    | 24h grace for natural expiry, zero grace for revoke   | Exceeds     |
| Optional 2FA          | requireDeviceUnlock on HceService + Keystore           | Equivalent  |

---

## Scope Boundaries

**In scope:**
- Android NFC HCE (HostApduService + APDU protocol)
- Gateway NFC reader module (PC/SC + TCP simulator)
- AuthProtocol v2 (52-byte challenge, transport binding)
- BLE v1→v2 migration (Android + iOS + backend)
- Gateway offline credential sync enhancement
- Offline decision logic (revoke vs expire grace)
- Offline audit log with batch sync

**Out of scope:**
- Apple Wallet Express Mode (separate feature, requires Apple partnership)
- MotionSense / BLE proximity auto-unlock (separate feature)
- Physical DESFire card writing (existing read-only binding is sufficient)
- NFC reader hardware procurement decisions (spec covers protocol only)
- Gateway hardware design / manufacturing
