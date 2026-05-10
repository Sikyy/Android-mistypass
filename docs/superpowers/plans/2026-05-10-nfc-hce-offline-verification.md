# NFC HCE + Offline Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add NFC HCE (HostApduService) to Android and harden gateway offline verification, using a v2 auth protocol with transport-bound signatures and gateway ID in challenges.

**Architecture:** Unified ECDSA P-256 challenge-response protocol shared by BLE and NFC transports. v2 extends the challenge from 48→52 bytes (adding gateway_id), adds transport tags to signatures ("BLE"/"NFC_HCE"), and adds a nonce replay cache. Backend deploys first (backward compatible), then Android (BLE v2 + HCE), then iOS (BLE v2 only), then gateway agent (NFC reader + offline hardening).

**Tech Stack:** Go (backend + gateway-agent), Kotlin/Compose (Android), Swift (iOS), PC/SC via `github.com/ebfe/scard` (NFC reader), NATS (credential sync push), BoltDB (gateway local storage)

**Spec:** `docs/superpowers/specs/2026-05-10-nfc-hce-offline-verification-design.md`

**Repos:**
- Backend: `/Users/siky/code/mistypass`
- Android: `/Users/siky/code/android-MistyisletPass`
- iOS: `/Users/siky/code/ios-MistyisletPass`

---

## File Structure

### Backend (`/Users/siky/code/mistypass`)

| Action | File | Purpose |
|--------|------|---------|
| Modify | `api/cmd/gateway-agent/ble_protocol.go` | v2 challenge (52B), new result codes, transport tag in verification |
| Modify | `api/cmd/gateway-agent/ble_protocol_test.go` | Tests for v2 encoding/decoding/verification |
| Modify | `api/internal/modules/credential/model.go` | GatewayCredentialSync v2 fields (RevokedAt, SuspendedAt, SyncVersion) |
| Modify | `api/internal/modules/credential/service.go` | VerifyBLESignature v2 with transport tag |
| Modify | `api/internal/modules/credential/service_test.go` | v2 verification tests |
| Modify | `api/internal/http/router.go` | Add gateway sync + audit routes |
| Create | `api/internal/http/routes_gateway_credential_sync.go` | GET /gateway/credentials/sync handler |
| Create | `api/internal/http/routes_gateway_audit.go` | POST /gateway/audit/batch handler |
| Modify | `api/cmd/gateway-agent/agent.go` | v2 verify logic with nonce cache + ordered checks |
| Create | `api/cmd/gateway-agent/nonce_cache.go` | In-memory LRU nonce cache (30s TTL) |
| Create | `api/cmd/gateway-agent/nonce_cache_test.go` | Tests |
| Create | `api/cmd/gateway-agent/nfc_protocol.go` | NFC APDU constants (AID, SELECT, AUTHENTICATE commands) |
| Create | `api/cmd/gateway-agent/nfc_reader.go` | NFCReader via PC/SC + NFCDriver interface |
| Modify | `api/cmd/gateway-agent/reader.go` | Extract ReaderAdapter interface |

### Android (`/Users/siky/code/android-MistyisletPass`)

| Action | File | Purpose |
|--------|------|---------|
| Modify | `app/src/main/java/com/mistyislet/app/core/ble/KeystoreManager.kt` | Add signChallengeV2(nonce, userId, transportTag) |
| Modify | `app/src/main/java/com/mistyislet/app/core/ble/BLEAuthClient.kt` | Read 52B challenge, use "BLE" transport tag |
| Create | `app/src/main/java/com/mistyislet/app/core/nfc/HceProtocol.kt` | APDU constants, parsing, response construction |
| Create | `app/src/main/java/com/mistyislet/app/core/nfc/HceService.kt` | HostApduService implementation |
| Create | `app/src/main/res/xml/hce_apdu_service.xml` | AID registration |
| Modify | `app/src/main/AndroidManifest.xml` | Add HceService + NFC HCE feature |
| Modify | `app/src/main/res/values/strings.xml` | HCE description strings |
| Modify | `app/src/main/res/values-zh-rCN/strings.xml` | Chinese translations |
| Modify | `app/src/main/res/values-in/strings.xml` | Indonesian translations |

### iOS (`/Users/siky/code/ios-MistyisletPass`)

| Action | File | Purpose |
|--------|------|---------|
| Modify | `MistyisletPass/Services/BLEManager.swift` | Read 52B challenge, append "BLE" transport tag to sign payload |

---

## Task 1: Backend — Protocol v2 Challenge + Result Codes

**Files:**
- Modify: `api/cmd/gateway-agent/ble_protocol.go`
- Modify: `api/cmd/gateway-agent/ble_protocol_test.go`

- [ ] **Step 1: Write test for v2 challenge encoding (52 bytes with gateway_id)**

In `api/cmd/gateway-agent/ble_protocol_test.go`, add:

```go
func TestBLEChallengeV2_Encode(t *testing.T) {
	ch := NewBLEChallengeV2(42) // gatewayID = 42
	encoded := ch.Encode()

	if len(encoded) != 52 {
		t.Fatalf("expected 52 bytes, got %d", len(encoded))
	}

	// Verify nonce is 32 bytes, non-zero
	nonce := encoded[:32]
	allZero := true
	for _, b := range nonce {
		if b != 0 {
			allZero = false
			break
		}
	}
	if allZero {
		t.Fatal("nonce should not be all zeros")
	}

	// Verify gateway_id at bytes 48-51
	gatewayID := binary.BigEndian.Uint32(encoded[48:52])
	if gatewayID != 42 {
		t.Fatalf("expected gateway_id=42, got %d", gatewayID)
	}

	// Verify timestamps are BigEndian unix seconds
	issuedAt := binary.BigEndian.Uint64(encoded[32:40])
	expiresAt := binary.BigEndian.Uint64(encoded[40:48])
	if expiresAt-issuedAt != 30 {
		t.Fatalf("expected 30s window, got %d", expiresAt-issuedAt)
	}
}

func TestBLEChallengeV2_IsExpired(t *testing.T) {
	ch := &BLEChallengeV2{
		Nonce:     [32]byte{1},
		IssuedAt:  time.Now().Add(-31 * time.Second),
		ExpiresAt: time.Now().Add(-1 * time.Second),
		GatewayID: 1,
	}
	if !ch.IsExpired() {
		t.Fatal("challenge should be expired")
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/siky/code/mistypass && go test ./api/cmd/gateway-agent/ -run TestBLEChallengeV2 -v
```

Expected: FAIL — `NewBLEChallengeV2` and `BLEChallengeV2` don't exist yet.

- [ ] **Step 3: Implement v2 challenge in ble_protocol.go**

Add to `api/cmd/gateway-agent/ble_protocol.go` after the existing v1 types:

```go
// --- Protocol V2 ---

const (
	ChallengeV2Size = 52 // 32B nonce + 8B issued_at + 8B expires_at + 4B gateway_id

	BLEResultCredentialRevoked  byte = 0x07
	BLEResultCredentialSuspended byte = 0x08
	BLEResultGatewayOfflineLimit byte = 0x09
)

// TransportTag constants for signature binding
const (
	TransportTagBLE    = "BLE"
	TransportTagNFCHCE = "NFC_HCE"
)

type BLEChallengeV2 struct {
	Nonce     [32]byte
	IssuedAt  time.Time
	ExpiresAt time.Time
	GatewayID uint32
}

func NewBLEChallengeV2(gatewayID uint32) *BLEChallengeV2 {
	var nonce [32]byte
	if _, err := rand.Read(nonce[:]); err != nil {
		panic("crypto/rand failed: " + err.Error())
	}
	now := time.Now()
	return &BLEChallengeV2{
		Nonce:     nonce,
		IssuedAt:  now,
		ExpiresAt: now.Add(challengeValidDuration),
		GatewayID: gatewayID,
	}
}

func (c *BLEChallengeV2) Encode() []byte {
	buf := make([]byte, ChallengeV2Size)
	copy(buf[:32], c.Nonce[:])
	binary.BigEndian.PutUint64(buf[32:40], uint64(c.IssuedAt.Unix()))
	binary.BigEndian.PutUint64(buf[40:48], uint64(c.ExpiresAt.Unix()))
	binary.BigEndian.PutUint32(buf[48:52], c.GatewayID)
	return buf
}

func DecodeBLEChallengeV2(data []byte) (*BLEChallengeV2, error) {
	if len(data) < ChallengeV2Size {
		return nil, fmt.Errorf("challenge too short: %d bytes, need %d", len(data), ChallengeV2Size)
	}
	ch := &BLEChallengeV2{}
	copy(ch.Nonce[:], data[:32])
	ch.IssuedAt = time.Unix(int64(binary.BigEndian.Uint64(data[32:40])), 0)
	ch.ExpiresAt = time.Unix(int64(binary.BigEndian.Uint64(data[40:48])), 0)
	ch.GatewayID = binary.BigEndian.Uint32(data[48:52])
	return ch, nil
}

func (c *BLEChallengeV2) IsExpired() bool {
	return time.Now().After(c.ExpiresAt)
}

// VerifyBLESignatureV2 verifies an ECDSA signature with transport tag binding.
// message = SHA256(nonce || userID || transportTag)
func VerifyBLESignatureV2(publicKeyPEM string, nonce [32]byte, userID string, transportTag string, signature []byte) error {
	pubKey, err := parseBLEPublicKey(publicKeyPEM)
	if err != nil {
		return fmt.Errorf("parse public key: %w", err)
	}

	message := make([]byte, 0, 32+len(userID)+len(transportTag))
	message = append(message, nonce[:]...)
	message = append(message, []byte(userID)...)
	message = append(message, []byte(transportTag)...)
	hash := sha256.Sum256(message)

	// Try ASN.1 DER format first
	if ecdsa.VerifyASN1(pubKey, hash[:], signature) {
		return nil
	}

	// Fallback: raw r||s (64 bytes for P-256)
	if len(signature) == 64 {
		r := new(big.Int).SetBytes(signature[:32])
		s := new(big.Int).SetBytes(signature[32:])
		if ecdsa.Verify(pubKey, hash[:], r, s) {
			return nil
		}
	}

	return fmt.Errorf("ECDSA signature verification failed")
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/siky/code/mistypass && go test ./api/cmd/gateway-agent/ -run TestBLEChallengeV2 -v
```

Expected: PASS

- [ ] **Step 5: Write test for v2 signature verification with transport tag**

Add to `api/cmd/gateway-agent/ble_protocol_test.go`:

```go
func TestVerifyBLESignatureV2_WithTransportTag(t *testing.T) {
	// Generate test keypair
	privKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	pubDER, err := x509.MarshalPKIXPublicKey(&privKey.PublicKey)
	if err != nil {
		t.Fatal(err)
	}
	pubPEM := pem.EncodeToMemory(&pem.Block{Type: "PUBLIC KEY", Bytes: pubDER})

	nonce := [32]byte{1, 2, 3, 4, 5}
	userID := "user-123"
	tag := TransportTagBLE

	// Sign: SHA256(nonce || userID || tag)
	message := make([]byte, 0, 64)
	message = append(message, nonce[:]...)
	message = append(message, []byte(userID)...)
	message = append(message, []byte(tag)...)
	hash := sha256.Sum256(message)
	sig, err := ecdsa.SignASN1(rand.Reader, privKey, hash[:])
	if err != nil {
		t.Fatal(err)
	}

	// Correct tag: should pass
	err = VerifyBLESignatureV2(string(pubPEM), nonce, userID, TransportTagBLE, sig)
	if err != nil {
		t.Fatalf("verification should pass: %v", err)
	}

	// Wrong tag: should fail (cross-transport replay protection)
	err = VerifyBLESignatureV2(string(pubPEM), nonce, userID, TransportTagNFCHCE, sig)
	if err == nil {
		t.Fatal("verification should fail with wrong transport tag")
	}
}
```

- [ ] **Step 6: Run tests**

```bash
cd /Users/siky/code/mistypass && go test ./api/cmd/gateway-agent/ -run TestVerifyBLESignatureV2 -v
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/cmd/gateway-agent/ble_protocol.go api/cmd/gateway-agent/ble_protocol_test.go
git commit -m "feat: add auth protocol v2 — 52B challenge with gateway_id, transport-bound signatures"
```

---

## Task 2: Backend — Credential Service v2 Verification

**Files:**
- Modify: `api/internal/modules/credential/service.go`
- Modify: `api/internal/modules/credential/service_test.go`
- Modify: `api/internal/modules/credential/model.go`

- [ ] **Step 1: Update GatewayCredentialSync model**

In `api/internal/modules/credential/model.go`, replace the existing `GatewayCredentialSync` struct (lines 61-67):

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

- [ ] **Step 2: Write test for v2 verification with transport tag**

Add to `api/internal/modules/credential/service_test.go`:

```go
func TestVerifyBLESignatureV2_TransportBinding(t *testing.T) {
	privKey, pubPEM := generateTestKeyPair(t)

	svc := NewService(NewInMemoryStore())
	// Register a credential
	input := RegisterDeviceInput{
		TenantID:      "tenant-1",
		UserID:        "user-1",
		UserEmail:     "test@example.com",
		PublicKeyPEM:  pubPEM,
		Platform:      "android",
		DeviceID:      "device-1",
		DeviceModel:   "Test Phone",
		KeystoreLevel: "tee",
	}
	cred, err := svc.RegisterDevice(input)
	if err != nil {
		t.Fatal(err)
	}

	// Create nonce and sign with BLE transport tag
	nonce := make([]byte, 32)
	rand.Read(nonce)

	message := make([]byte, 0, 64)
	message = append(message, nonce...)
	message = append(message, []byte("user-1")...)
	message = append(message, []byte("BLE")...)
	hash := sha256.Sum256(message)
	sig, _ := ecdsa.SignASN1(rand.Reader, privKey, hash[:])

	// V2 verify with correct tag
	result, err := svc.VerifyBLESignatureV2("tenant-1", "user-1", nonce, "BLE", sig)
	if err != nil {
		t.Fatalf("v2 verify should pass: %v", err)
	}
	if result.ID != cred.ID {
		t.Fatal("should return matching credential")
	}

	// V2 verify with wrong tag should fail
	_, err = svc.VerifyBLESignatureV2("tenant-1", "user-1", nonce, "NFC_HCE", sig)
	if err == nil {
		t.Fatal("v2 verify should fail with wrong transport tag")
	}
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/siky/code/mistypass && go test ./api/internal/modules/credential/ -run TestVerifyBLESignatureV2 -v
```

Expected: FAIL — `VerifyBLESignatureV2` doesn't exist.

- [ ] **Step 4: Implement VerifyBLESignatureV2 in service.go**

Add to `api/internal/modules/credential/service.go` after the existing `VerifyBLESignature`:

```go
// VerifyBLESignatureV2 verifies with transport tag binding.
// message = SHA256(nonce || userID || transportTag)
func (s *Service) VerifyBLESignatureV2(tenantID, userID string, nonce []byte, transportTag string, signature []byte) (*MobileCredential, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	cred, err := s.findActiveCredential(tenantID, userID)
	if err != nil {
		return nil, err
	}

	pubKey, err := ParseECPublicKey(cred.PublicKeyPEM)
	if err != nil {
		return nil, fmt.Errorf("parse stored public key: %w", err)
	}

	message := make([]byte, 0, 32+len(userID)+len(transportTag))
	message = append(message, nonce...)
	message = append(message, []byte(userID)...)
	message = append(message, []byte(transportTag)...)
	hash := sha256.Sum256(message)

	// Try ASN.1 DER first
	if ecdsa.VerifyASN1(pubKey, hash[:], signature) {
		return cred, nil
	}

	// Fallback: raw r||s (64 bytes)
	if len(signature) == 64 {
		r := new(big.Int).SetBytes(signature[:32])
		ss := new(big.Int).SetBytes(signature[32:])
		if ecdsa.Verify(pubKey, hash[:], r, ss) {
			return cred, nil
		}
	}

	return nil, fmt.Errorf("ECDSA signature verification failed for user %s with transport %s", userID, transportTag)
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd /Users/siky/code/mistypass && go test ./api/internal/modules/credential/ -run TestVerifyBLESignatureV2 -v
```

Expected: PASS

- [ ] **Step 6: Run all existing credential tests to ensure no regressions**

```bash
cd /Users/siky/code/mistypass && go test ./api/internal/modules/credential/ -v
```

Expected: All PASS

- [ ] **Step 7: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/internal/modules/credential/
git commit -m "feat: add v2 credential verification with transport tag + enhanced GatewayCredentialSync model"
```

---

## Task 3: Backend — Gateway Credential Sync + Audit Endpoints

**Files:**
- Create: `api/internal/http/routes_gateway_credential_sync.go`
- Create: `api/internal/http/routes_gateway_audit.go`
- Modify: `api/internal/http/router.go` (add routes at lines ~768-781 in the gateway route group)

- [ ] **Step 1: Create credential sync handler**

Create `api/internal/http/routes_gateway_credential_sync.go`:

```go
package http

import (
	"encoding/json"
	"net/http"
	"strconv"
)

// GET /gateway/credentials/sync?since_version={version}
// Returns credentials changed since the given sync version.
func (s *Server) gatewayCredentialSync(w http.ResponseWriter, r *http.Request) {
	gatewayID := r.Context().Value(ctxKeyGatewayID).(string)

	sinceVersion := int64(0)
	if v := r.URL.Query().Get("since_version"); v != "" {
		parsed, err := strconv.ParseInt(v, 10, 64)
		if err != nil {
			http.Error(w, "invalid since_version", http.StatusBadRequest)
			return
		}
		sinceVersion = parsed
	}

	creds, err := s.credentialSvc.GetGatewayCredentialSyncSince(gatewayID, sinceVersion)
	if err != nil {
		slog.Error("credential sync failed", "gateway_id", gatewayID, "err", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"credentials": creds,
	})
}
```

- [ ] **Step 2: Create audit batch handler**

Create `api/internal/http/routes_gateway_audit.go`:

```go
package http

import (
	"encoding/json"
	"net/http"
)

type OfflineAuditEntry struct {
	EventID   string `json:"event_id"`
	UserID    string `json:"user_id"`
	LockID    string `json:"lock_id"`
	Method    string `json:"method"`
	Result    string `json:"result"`
	Reason    string `json:"reason"`
	GatewayID uint32 `json:"gateway_id"`
	Timestamp int64  `json:"timestamp"`
	IsOffline bool   `json:"is_offline"`
}

// POST /gateway/audit/batch
// Receives a batch of offline audit entries from the gateway.
func (s *Server) gatewayAuditBatch(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Entries []OfflineAuditEntry `json:"entries"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "invalid request body", http.StatusBadRequest)
		return
	}

	if len(req.Entries) == 0 {
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]interface{}{"accepted": 0})
		return
	}

	if len(req.Entries) > 500 {
		http.Error(w, "batch too large, max 500 entries", http.StatusBadRequest)
		return
	}

	accepted, err := s.auditSvc.IngestOfflineBatch(r.Context(), req.Entries)
	if err != nil {
		slog.Error("audit batch ingestion failed", "err", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"accepted": accepted,
	})
}
```

- [ ] **Step 3: Register routes in router.go**

In `api/internal/http/router.go`, inside the `/gateway` route group (around line 780, after existing routes), add:

```go
gatewayRouter.Get("/credentials/sync", s.gatewayCredentialSync)
gatewayRouter.Post("/audit/batch", s.gatewayAuditBatch)
```

- [ ] **Step 4: Verify compilation**

```bash
cd /Users/siky/code/mistypass && go build ./api/...
```

Expected: Compiles (handler methods may reference services that don't exist yet — stub them if needed to compile).

- [ ] **Step 5: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/internal/http/
git commit -m "feat: add gateway credential sync and audit batch endpoints"
```

---

## Task 4: Gateway Agent — Nonce Cache

**Files:**
- Create: `api/cmd/gateway-agent/nonce_cache.go`
- Create: `api/cmd/gateway-agent/nonce_cache_test.go`

- [ ] **Step 1: Write nonce cache tests**

Create `api/cmd/gateway-agent/nonce_cache_test.go`:

```go
package main

import (
	"testing"
	"time"
)

func TestNonceCache_AddAndContains(t *testing.T) {
	cache := NewNonceCache(100, 30*time.Second)

	nonce := [32]byte{1, 2, 3}
	if cache.Contains(nonce[:]) {
		t.Fatal("should not contain unused nonce")
	}

	cache.Add(nonce[:])
	if !cache.Contains(nonce[:]) {
		t.Fatal("should contain added nonce")
	}
}

func TestNonceCache_Expiry(t *testing.T) {
	cache := NewNonceCache(100, 50*time.Millisecond) // short TTL for test

	nonce := [32]byte{1, 2, 3}
	cache.Add(nonce[:])

	if !cache.Contains(nonce[:]) {
		t.Fatal("should contain nonce immediately")
	}

	time.Sleep(60 * time.Millisecond)

	if cache.Contains(nonce[:]) {
		t.Fatal("should not contain expired nonce")
	}
}

func TestNonceCache_MaxSize(t *testing.T) {
	cache := NewNonceCache(2, 10*time.Second)

	n1 := [32]byte{1}
	n2 := [32]byte{2}
	n3 := [32]byte{3}

	cache.Add(n1[:])
	cache.Add(n2[:])
	cache.Add(n3[:]) // should evict n1

	if cache.Contains(n1[:]) {
		t.Fatal("n1 should have been evicted")
	}
	if !cache.Contains(n2[:]) {
		t.Fatal("n2 should still exist")
	}
	if !cache.Contains(n3[:]) {
		t.Fatal("n3 should exist")
	}
}
```

- [ ] **Step 2: Run tests to verify failure**

```bash
cd /Users/siky/code/mistypass && go test ./api/cmd/gateway-agent/ -run TestNonceCache -v
```

Expected: FAIL — `NewNonceCache` doesn't exist.

- [ ] **Step 3: Implement nonce cache**

Create `api/cmd/gateway-agent/nonce_cache.go`:

```go
package main

import (
	"encoding/hex"
	"sync"
	"time"
)

type nonceCacheEntry struct {
	key       string
	expiresAt time.Time
}

// NonceCache is an in-memory LRU cache with TTL for preventing nonce replay.
// Thread-safe. Memory: ~320KB at max 10,000 entries (32 bytes/nonce).
type NonceCache struct {
	mu      sync.Mutex
	entries map[string]time.Time
	order   []nonceCacheEntry
	maxSize int
	ttl     time.Duration
}

func NewNonceCache(maxSize int, ttl time.Duration) *NonceCache {
	return &NonceCache{
		entries: make(map[string]time.Time, maxSize),
		order:   make([]nonceCacheEntry, 0, maxSize),
		maxSize: maxSize,
		ttl:     ttl,
	}
}

func (c *NonceCache) Contains(nonce []byte) bool {
	key := hex.EncodeToString(nonce)
	c.mu.Lock()
	defer c.mu.Unlock()

	c.evictExpired()

	expiresAt, ok := c.entries[key]
	if !ok {
		return false
	}
	return time.Now().Before(expiresAt)
}

func (c *NonceCache) Add(nonce []byte) {
	key := hex.EncodeToString(nonce)
	c.mu.Lock()
	defer c.mu.Unlock()

	c.evictExpired()

	// Evict oldest if at capacity
	for len(c.entries) >= c.maxSize && len(c.order) > 0 {
		oldest := c.order[0]
		c.order = c.order[1:]
		delete(c.entries, oldest.key)
	}

	expiresAt := time.Now().Add(c.ttl)
	c.entries[key] = expiresAt
	c.order = append(c.order, nonceCacheEntry{key: key, expiresAt: expiresAt})
}

func (c *NonceCache) evictExpired() {
	now := time.Now()
	cutoff := 0
	for cutoff < len(c.order) && now.After(c.order[cutoff].expiresAt) {
		delete(c.entries, c.order[cutoff].key)
		cutoff++
	}
	if cutoff > 0 {
		c.order = c.order[cutoff:]
	}
}
```

- [ ] **Step 4: Run tests**

```bash
cd /Users/siky/code/mistypass && go test ./api/cmd/gateway-agent/ -run TestNonceCache -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/cmd/gateway-agent/nonce_cache.go api/cmd/gateway-agent/nonce_cache_test.go
git commit -m "feat: add nonce replay cache for gateway — LRU with TTL eviction"
```

---

## Task 5: Gateway Agent — NFC Protocol + Reader

**Files:**
- Create: `api/cmd/gateway-agent/nfc_protocol.go`
- Create: `api/cmd/gateway-agent/nfc_reader.go`
- Modify: `api/cmd/gateway-agent/reader.go`

- [ ] **Step 1: Create NFC APDU protocol constants**

Create `api/cmd/gateway-agent/nfc_protocol.go`:

```go
package main

import (
	"encoding/binary"
	"fmt"
)

// NFC HCE Application ID: F0 4D 49 53 54 59 01 00 (8 bytes)
var NFCAID = []byte{0xF0, 0x4D, 0x49, 0x53, 0x54, 0x59, 0x01, 0x00}

// APDU command builders

// BuildSelectAID constructs: 00 A4 04 00 08 [AID] 00
func BuildSelectAID() []byte {
	cmd := []byte{0x00, 0xA4, 0x04, 0x00, byte(len(NFCAID))}
	cmd = append(cmd, NFCAID...)
	cmd = append(cmd, 0x00) // Le
	return cmd
}

// BuildAuthenticate constructs: 80 88 00 00 34 [52-byte challenge] 00
func BuildAuthenticate(challenge []byte) []byte {
	if len(challenge) != ChallengeV2Size {
		panic(fmt.Sprintf("challenge must be %d bytes, got %d", ChallengeV2Size, len(challenge)))
	}
	cmd := []byte{0x80, 0x88, 0x00, 0x00, byte(ChallengeV2Size)}
	cmd = append(cmd, challenge...)
	cmd = append(cmd, 0x00) // Le
	return cmd
}

// APDU response status words
const (
	SW_OK                    = 0x9000
	SW_SECURITY_NOT_SATISFIED = 0x6982
	SW_CONDITIONS_NOT_MET    = 0x6985
	SW_APP_NOT_FOUND         = 0x6A82
	SW_INTERNAL_ERROR        = 0x6F00
)

// ParseAPDUResponse extracts data and status word from a response.
func ParseAPDUResponse(resp []byte) (data []byte, sw uint16, err error) {
	if len(resp) < 2 {
		return nil, 0, fmt.Errorf("APDU response too short: %d bytes", len(resp))
	}
	sw = binary.BigEndian.Uint16(resp[len(resp)-2:])
	data = resp[:len(resp)-2]
	return data, sw, nil
}

// ParseAuthResponse extracts userId and signature from AUTHENTICATE response data.
// Format: [1B userId_len][userId bytes][ECDSA signature]
func ParseNFCAuthResponse(data []byte) (*BLEAuthResponse, error) {
	if len(data) < 2 {
		return nil, fmt.Errorf("auth response too short")
	}
	userIDLen := int(data[0])
	if len(data) < 1+userIDLen+1 {
		return nil, fmt.Errorf("auth response truncated: need %d bytes for userId, have %d", userIDLen, len(data)-1)
	}
	if userIDLen > 180 {
		return nil, fmt.Errorf("userId too long: %d bytes, max 180", userIDLen)
	}
	userID := string(data[1 : 1+userIDLen])
	signature := data[1+userIDLen:]
	return &BLEAuthResponse{UserID: userID, Signature: signature}, nil
}
```

- [ ] **Step 2: Define ReaderAdapter interface**

Modify `api/cmd/gateway-agent/reader.go` — add the interface at the top of the file (the existing file contains concrete reader implementations):

```go
// ReaderAdapter is the common interface for all credential reader types.
type ReaderAdapter interface {
	Name() string
	Type() string // "ble" | "nfc" | "tcp_simulator"
	// Authenticate sends a challenge and waits for a signed response.
	Authenticate(challenge []byte) (*BLEAuthResponse, error)
	Close() error
}
```

- [ ] **Step 3: Create NFC reader implementation**

Create `api/cmd/gateway-agent/nfc_reader.go`:

```go
package main

import (
	"context"
	"fmt"
	"log/slog"
	"time"
)

// NFCDriver abstracts the hardware communication layer.
type NFCDriver interface {
	WaitForCard(ctx context.Context) error
	Transmit(command []byte) ([]byte, error)
	Disconnect() error
}

// NFCReader implements ReaderAdapter for NFC ISO-DEP via PC/SC.
type NFCReader struct {
	driver NFCDriver
	name   string
}

func NewNFCReader(driver NFCDriver, name string) *NFCReader {
	return &NFCReader{driver: driver, name: name}
}

func (r *NFCReader) Name() string { return r.name }
func (r *NFCReader) Type() string { return "nfc" }

func (r *NFCReader) Authenticate(challenge []byte) (*BLEAuthResponse, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// 1. Wait for card/phone to be presented
	if err := r.driver.WaitForCard(ctx); err != nil {
		return nil, fmt.Errorf("wait for card: %w", err)
	}
	defer r.driver.Disconnect()

	// 2. SELECT AID
	selectCmd := BuildSelectAID()
	selectResp, err := r.driver.Transmit(selectCmd)
	if err != nil {
		return nil, fmt.Errorf("SELECT AID transmit: %w", err)
	}
	_, sw, err := ParseAPDUResponse(selectResp)
	if err != nil {
		return nil, fmt.Errorf("parse SELECT response: %w", err)
	}
	if sw != SW_OK {
		return nil, fmt.Errorf("SELECT AID failed: SW=%04X", sw)
	}
	slog.Debug("NFC: SELECT AID success")

	// 3. AUTHENTICATE
	authCmd := BuildAuthenticate(challenge)
	authResp, err := r.driver.Transmit(authCmd)
	if err != nil {
		return nil, fmt.Errorf("AUTHENTICATE transmit: %w", err)
	}
	data, sw, err := ParseAPDUResponse(authResp)
	if err != nil {
		return nil, fmt.Errorf("parse AUTH response: %w", err)
	}

	switch sw {
	case SW_OK:
		// Parse userId + signature from data
		return ParseNFCAuthResponse(data)
	case SW_SECURITY_NOT_SATISFIED:
		return nil, fmt.Errorf("device locked (2FA required)")
	case SW_CONDITIONS_NOT_MET:
		return nil, fmt.Errorf("credential expired or suspended")
	default:
		return nil, fmt.Errorf("AUTHENTICATE failed: SW=%04X", sw)
	}
}

func (r *NFCReader) Close() error {
	return r.driver.Disconnect()
}

// TCPNFCSimDriver simulates NFC over TCP for development testing.
type TCPNFCSimDriver struct {
	addr string
	// connection state managed per WaitForCard/Transmit cycle
}

func NewTCPNFCSimDriver(addr string) *TCPNFCSimDriver {
	return &TCPNFCSimDriver{addr: addr}
}

func (d *TCPNFCSimDriver) WaitForCard(ctx context.Context) error {
	// In simulator mode, always "ready"
	return nil
}

func (d *TCPNFCSimDriver) Transmit(command []byte) ([]byte, error) {
	// TCP simulator: send command length + command, read response length + response
	// Implementation follows same pattern as existing TCP BLE simulator
	return nil, fmt.Errorf("TCP NFC simulator not yet connected")
}

func (d *TCPNFCSimDriver) Disconnect() error {
	return nil
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd /Users/siky/code/mistypass && go build ./api/cmd/gateway-agent/
```

Expected: Compiles

- [ ] **Step 5: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/cmd/gateway-agent/nfc_protocol.go api/cmd/gateway-agent/nfc_reader.go api/cmd/gateway-agent/reader.go
git commit -m "feat: add NFC APDU protocol + NFCReader with PC/SC driver interface"
```

---

## Task 6: Gateway Agent — Unified v2 Verification

**Files:**
- Modify: `api/cmd/gateway-agent/agent.go`

- [ ] **Step 1: Add v2 verification function to agent**

Add to `api/cmd/gateway-agent/agent.go` (this replaces or supplements the existing verification logic):

```go
// VerifyAuthResponseV2 performs the full verification chain in fast-to-slow order:
// nonce cache → gateway_id → credential lookup → status → expiry → ECDSA verify
func (a *Agent) VerifyAuthResponseV2(resp *BLEAuthResponse, challenge []byte, transport string) BLEAuthResult {
	nonce := challenge[:32]

	// 1. Nonce reuse check (fastest — in-memory LRU)
	if a.nonceCache.Contains(nonce) {
		return BLEAuthResult{Code: BLEResultDenied, Reason: "nonce_reuse"}
	}

	// 2. Gateway ID check (fast — compare uint32)
	if len(challenge) >= ChallengeV2Size {
		gatewayID := binary.BigEndian.Uint32(challenge[48:52])
		if gatewayID != a.config.GatewayID {
			return BLEAuthResult{Code: BLEResultDenied, Reason: "gateway_id_mismatch"}
		}
	}

	// 3. Credential lookup
	cred := a.credentialCache.FindByUserID(resp.UserID)
	if cred == nil {
		return BLEAuthResult{Code: BLEResultUnknownUser, Reason: "unknown_user"}
	}

	// 4. Revocation check (hard deny — no grace period)
	if cred.RevokedAt != nil {
		return BLEAuthResult{Code: BLEResultCredentialRevoked, Reason: "revoked"}
	}
	if cred.SuspendedAt != nil {
		return BLEAuthResult{Code: BLEResultCredentialSuspended, Reason: "suspended"}
	}

	// 5. Expiry check (grace period for natural expiration only)
	now := time.Now()
	gracePeriod := false
	if now.Unix() > cred.ExpiresAt {
		elapsed := now.Sub(time.Unix(cred.ExpiresAt, 0))
		if elapsed <= a.config.Offline.GracePeriodExpired {
			gracePeriod = true
		} else {
			return BLEAuthResult{Code: BLEResultCredentialExpired, Reason: "expired"}
		}
	}

	// 6. Offline limits check
	if a.isOffline() {
		if a.offlineDuration() > a.config.Offline.MaxOfflineDuration {
			return BLEAuthResult{Code: BLEResultGatewayOfflineLimit, Reason: "offline_duration_exceeded"}
		}
		if a.offlineUnlockCount >= a.config.Offline.MaxOfflineUnlocks {
			return BLEAuthResult{Code: BLEResultGatewayOfflineLimit, Reason: "offline_unlock_limit"}
		}
	}

	// 7. ECDSA signature verification (most expensive — last)
	var nonceArr [32]byte
	copy(nonceArr[:], nonce)
	err := VerifyBLESignatureV2(cred.PublicKeyPEM, nonceArr, resp.UserID, transport, resp.Signature)
	if err != nil {
		return BLEAuthResult{Code: BLEResultInvalidSignature, Reason: err.Error()}
	}

	// 8. Mark nonce as used (only after successful verification)
	a.nonceCache.Add(nonce)

	reason := "access_granted"
	if gracePeriod {
		reason = "grace_period"
	}
	return BLEAuthResult{Code: BLEResultGranted, Reason: reason}
}
```

- [ ] **Step 2: Initialize nonceCache in agent startup**

In `agent.go`'s init or constructor, add:

```go
a.nonceCache = NewNonceCache(10000, 30*time.Second)
```

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/siky/code/mistypass && go build ./api/cmd/gateway-agent/
```

Expected: Compiles (may need to stub `a.config.Offline`, `a.credentialCache`, etc. — follow existing patterns in agent.go)

- [ ] **Step 4: Commit**

```bash
cd /Users/siky/code/mistypass
git add api/cmd/gateway-agent/agent.go
git commit -m "feat: add v2 unified verification — nonce cache, transport binding, ordered checks"
```

---

## Task 7: Android — KeystoreManager v2 Signing

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/core/ble/KeystoreManager.kt`

- [ ] **Step 1: Add signChallengeV2 method**

In `KeystoreManager.kt`, add after the existing `signChallenge` method (around line 86):

```kotlin
/**
 * V2 signing: SHA256(nonce || userId || transportTag)
 * Transport tag binds signature to a specific channel ("BLE" or "NFC_HCE").
 */
fun signChallengeV2(nonce: ByteArray, userId: String, transportTag: String): ByteArray {
    require(nonce.size == 32) { "Nonce must be 32 bytes" }
    require(transportTag == "BLE" || transportTag == "NFC_HCE") { "Invalid transport tag" }

    val userIdBytes = userId.toByteArray(Charsets.UTF_8)
    val tagBytes = transportTag.toByteArray(Charsets.UTF_8)
    val message = nonce + userIdBytes + tagBytes

    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
        ?: throw IllegalStateException("BLE credential key not found")

    return Signature.getInstance(SIGNATURE_ALGORITHM).run {
        initSign(privateKey)
        update(message)
        sign()
    }
}
```

- [ ] **Step 2: Verify build**

```bash
cd /Users/siky/code/android-MistyisletPass && ./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/siky/code/android-MistyisletPass
git add app/src/main/java/com/mistyislet/app/core/ble/KeystoreManager.kt
git commit -m "feat: add signChallengeV2 with transport tag binding"
```

---

## Task 8: Android — BLEAuthClient v2 Migration

**Files:**
- Modify: `app/src/main/java/com/mistyislet/app/core/ble/BLEAuthClient.kt`

- [ ] **Step 1: Update challenge size constant and signing call**

In `BLEAuthClient.kt`:

Change `CHALLENGE_SIZE` from 48 to 52:

```kotlin
private const val CHALLENGE_SIZE = 52 // v2: 32B nonce + 8B issued + 8B expires + 4B gateway_id
```

Update the `authenticateViaBLE` and `authenticateViaTcp` methods wherever they read the challenge and sign. Change the signing call from:

```kotlin
val signature = keystoreManager.signChallenge(nonce, userId)
```

to:

```kotlin
val signature = keystoreManager.signChallengeV2(nonce, userId, "BLE")
```

The nonce extraction stays the same (first 32 bytes of challenge). The remaining 20 bytes (timestamps + gateway_id) are not used by the client — they're for the gateway to verify.

- [ ] **Step 2: Verify build**

```bash
cd /Users/siky/code/android-MistyisletPass && ./gradlew compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/siky/code/android-MistyisletPass
git add app/src/main/java/com/mistyislet/app/core/ble/BLEAuthClient.kt
git commit -m "feat: migrate BLEAuthClient to v2 protocol — 52B challenge, BLE transport tag"
```

---

## Task 9: Android — HCE Protocol + Service

**Files:**
- Create: `app/src/main/java/com/mistyislet/app/core/nfc/HceProtocol.kt`
- Create: `app/src/main/java/com/mistyislet/app/core/nfc/HceService.kt`

- [ ] **Step 1: Create HceProtocol**

Create `app/src/main/java/com/mistyislet/app/core/nfc/HceProtocol.kt`:

```kotlin
package com.mistyislet.app.core.nfc

/**
 * NFC HCE APDU protocol constants and parsing utilities.
 * Spec: docs/superpowers/specs/2026-05-10-nfc-hce-offline-verification-design.md §2
 */
object HceProtocol {

    // AID: F0 4D 49 53 54 59 01 00 (8 bytes)
    val AID = byteArrayOf(
        0xF0.toByte(), 0x4D, 0x49, 0x53, 0x54, 0x59, 0x01, 0x00
    )

    // APDU CLA/INS constants
    const val CLA_ISO = 0x00.toByte()
    const val CLA_PROPRIETARY = 0x80.toByte()
    const val INS_SELECT = 0xA4.toByte()
    const val INS_AUTHENTICATE = 0x88.toByte()

    // Status words (as ByteArray for response construction)
    val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
    val SW_SECURITY_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())
    val SW_CONDITIONS_NOT_MET = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val SW_APP_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00)
    val SW_INTERNAL_ERROR = byteArrayOf(0x6F.toByte(), 0x00)

    // Protocol version returned in SELECT response
    const val PROTOCOL_VERSION: Byte = 0x02
    const val CAPABILITIES: Byte = 0x03 // bit0=challenge-response, bit1=offline-token

    // Challenge size for v2 protocol
    const val CHALLENGE_V2_SIZE = 52

    // Transport tag for NFC HCE signatures
    const val TRANSPORT_TAG = "NFC_HCE"

    /**
     * Check if APDU is a SELECT AID command for our AID.
     * Format: 00 A4 04 00 08 [AID] 00
     */
    fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + AID.size) return false
        return apdu[0] == CLA_ISO &&
               apdu[1] == INS_SELECT &&
               apdu[2] == 0x04.toByte() && // P1 = select by name
               apdu[3] == 0x00.toByte() && // P2
               apdu[4] == AID.size.toByte() &&
               apdu.sliceArray(5 until 5 + AID.size).contentEquals(AID)
    }

    /**
     * Check if APDU is an AUTHENTICATE command.
     * Format: 80 88 00 00 34 [52-byte challenge] 00
     */
    fun isAuthenticate(apdu: ByteArray): Boolean {
        if (apdu.size < 5 + CHALLENGE_V2_SIZE) return false
        return apdu[0] == CLA_PROPRIETARY &&
               apdu[1] == INS_AUTHENTICATE &&
               apdu[4] == CHALLENGE_V2_SIZE.toByte()
    }

    /**
     * Extract the 52-byte challenge from an AUTHENTICATE APDU.
     */
    fun extractChallenge(apdu: ByteArray): ByteArray {
        return apdu.sliceArray(5 until 5 + CHALLENGE_V2_SIZE)
    }

    /**
     * Build SELECT AID success response: [version][reserved][capabilities] + 90 00
     */
    fun buildSelectResponse(): ByteArray {
        return byteArrayOf(PROTOCOL_VERSION, 0x01, CAPABILITIES) + SW_OK
    }

    /**
     * Build AUTHENTICATE success response: [1B userId_len][userId][signature] + 90 00
     */
    fun buildAuthResponse(userId: String, signature: ByteArray): ByteArray {
        val userIdBytes = userId.toByteArray(Charsets.UTF_8)
        require(userIdBytes.size <= 180) { "userId too long: ${userIdBytes.size} bytes, max 180" }

        val response = ByteArray(1 + userIdBytes.size + signature.size)
        response[0] = userIdBytes.size.toByte()
        userIdBytes.copyInto(response, 1)
        signature.copyInto(response, 1 + userIdBytes.size)
        return response + SW_OK
    }

    /**
     * Build error response from status word.
     */
    fun buildErrorResponse(sw: ByteArray): ByteArray = sw
}
```

- [ ] **Step 2: Create HceService**

Create `app/src/main/java/com/mistyislet/app/core/nfc/HceService.kt`:

```kotlin
package com.mistyislet.app.core.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.mistyislet.app.core.ble.KeystoreManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

/**
 * NFC Host Card Emulation service.
 *
 * Responds to ISO-DEP APDU commands from NFC readers, using the same
 * ECDSA P-256 keypair as BLE authentication (mistyislet_ble_credential).
 *
 * APDU flow:
 * 1. Reader sends SELECT AID → we return protocol version
 * 2. Reader sends AUTHENTICATE with 52B challenge → we sign and return userId + signature
 *
 * 2FA: XML requireDeviceUnlock is always false. When user enables 2FA,
 * Keystore key has setUserAuthenticationRequired(true). Signing throws
 * UserNotAuthenticatedException when device is locked → we return SW 69 82.
 */
@AndroidEntryPoint
class HceService : HostApduService() {

    companion object {
        private const val TAG = "HceService"
    }

    // HostApduService can't use constructor injection.
    // Use Hilt EntryPoint for field access.
    @Inject lateinit var keystoreManager: KeystoreManager

    private val userId: String?
        get() {
            // Read from shared preferences or Room DB — same source as BLE auth
            val prefs = applicationContext.getSharedPreferences("mistyislet_prefs", MODE_PRIVATE)
            return prefs.getString("user_id", null)
        }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "APDU received: ${commandApdu.size} bytes")

        return when {
            HceProtocol.isSelectAid(commandApdu) -> handleSelect()
            HceProtocol.isAuthenticate(commandApdu) -> handleAuthenticate(commandApdu)
            else -> {
                Log.w(TAG, "Unknown APDU command: CLA=${commandApdu[0]}, INS=${commandApdu[1]}")
                HceProtocol.SW_INS_NOT_SUPPORTED
            }
        }
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "link_loss"
            DEACTIVATION_DESELECTED -> "deselected"
            else -> "unknown($reason)"
        }
        Log.d(TAG, "HCE deactivated: $reasonStr")
    }

    private fun handleSelect(): ByteArray {
        Log.d(TAG, "SELECT AID — returning protocol v2")
        return HceProtocol.buildSelectResponse()
    }

    private fun handleAuthenticate(apdu: ByteArray): ByteArray {
        val currentUserId = userId
        if (currentUserId.isNullOrEmpty()) {
            Log.w(TAG, "No userId available — credential not registered")
            return HceProtocol.buildErrorResponse(HceProtocol.SW_CONDITIONS_NOT_MET)
        }

        return try {
            val challenge = HceProtocol.extractChallenge(apdu)
            val nonce = challenge.sliceArray(0 until 32)

            val signature = keystoreManager.signChallengeV2(
                nonce = nonce,
                userId = currentUserId,
                transportTag = HceProtocol.TRANSPORT_TAG // "NFC_HCE"
            )

            Log.d(TAG, "AUTHENTICATE success for user=$currentUserId")
            HceProtocol.buildAuthResponse(currentUserId, signature)
        } catch (e: UserNotAuthenticatedException) {
            Log.w(TAG, "Device locked — 2FA required")
            HceProtocol.buildErrorResponse(HceProtocol.SW_SECURITY_NOT_SATISFIED)
        } catch (e: Exception) {
            Log.e(TAG, "AUTHENTICATE failed", e)
            HceProtocol.buildErrorResponse(HceProtocol.SW_INTERNAL_ERROR)
        }
    }
}
```

- [ ] **Step 3: Verify build**

```bash
cd /Users/siky/code/android-MistyisletPass && ./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd /Users/siky/code/android-MistyisletPass
git add app/src/main/java/com/mistyislet/app/core/nfc/HceProtocol.kt app/src/main/java/com/mistyislet/app/core/nfc/HceService.kt
git commit -m "feat: add NFC HCE — HostApduService with APDU protocol and 2FA via Keystore"
```

---

## Task 10: Android — Manifest, AID XML, Strings

**Files:**
- Create: `app/src/main/res/xml/hce_apdu_service.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

- [ ] **Step 1: Create AID registration XML**

Create `app/src/main/res/xml/hce_apdu_service.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
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

- [ ] **Step 2: Add HceService to AndroidManifest.xml**

In `AndroidManifest.xml`, add inside `<application>` (after the GeofenceBroadcastReceiver, before `</application>`):

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

Also add NFC HCE feature declaration (after the existing `uses-feature` for camera):

```xml
    <uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
```

- [ ] **Step 3: Add string resources**

In `app/src/main/res/values/strings.xml`:
```xml
    <string name="hce_service_description">Mistyislet NFC door unlock</string>
    <string name="hce_aid_group_description">Mistyislet access credential</string>
```

In `app/src/main/res/values-zh-rCN/strings.xml`:
```xml
    <string name="hce_service_description">Mistyislet NFC 门禁解锁</string>
    <string name="hce_aid_group_description">Mistyislet 门禁凭证</string>
```

In `app/src/main/res/values-in/strings.xml`:
```xml
    <string name="hce_service_description">Buka kunci pintu NFC Mistyislet</string>
    <string name="hce_aid_group_description">Kredensial akses Mistyislet</string>
```

- [ ] **Step 4: Build full APK**

```bash
cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
cd /Users/siky/code/android-MistyisletPass
git add app/src/main/res/xml/hce_apdu_service.xml app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/values-in/strings.xml
git commit -m "feat: register HceService in manifest + AID XML + localized strings"
```

---

## Task 11: iOS — BLE v2 Migration

**Files:**
- Modify: `MistyisletPass/Services/BLEManager.swift`

- [ ] **Step 1: Update challenge size check**

In `BLEManager.swift`, find the challenge reception (around line 246):

```swift
case Constants.BLE.challengeUUID:
    guard let data = characteristic.value, data.count >= 48 else {
```

Change `48` to `52`:

```swift
case Constants.BLE.challengeUUID:
    guard let data = characteristic.value, data.count >= 52 else {
```

Note: We use `>=` so it's backward compatible — if a gateway sends 48-byte v1 challenges, the app still works (it only reads the first 32 bytes as nonce).

- [ ] **Step 2: Add transport tag to signing payload**

In the `signAndRespond` method (around line 112), change the sign payload construction from:

```swift
var signPayload = Data()
signPayload.append(nonce)
signPayload.append(userIdData)
```

to:

```swift
var signPayload = Data()
signPayload.append(nonce)
signPayload.append(userIdData)
signPayload.append("BLE".data(using: .utf8)!) // v2 transport binding
```

- [ ] **Step 3: Build iOS project**

```bash
cd /Users/siky/code/ios-MistyisletPass && xcodebuild build -scheme MistyisletPass -destination 'generic/platform=iOS' -quiet 2>&1 | tail -5
```

Expected: BUILD SUCCEEDED

- [ ] **Step 4: Commit**

```bash
cd /Users/siky/code/ios-MistyisletPass
git add MistyisletPass/Services/BLEManager.swift
git commit -m "feat: migrate BLE auth to v2 protocol — 52B challenge, BLE transport tag"
```

---

## Verification

After all tasks complete:

1. **Backend**: `cd /Users/siky/code/mistypass && go test ./... 2>&1 | tail -20` — all tests pass
2. **Android**: `cd /Users/siky/code/android-MistyisletPass && ./gradlew assembleDebug` — build succeeds, install on device, verify NFC HCE appears in system NFC settings
3. **iOS**: `cd /Users/siky/code/ios-MistyisletPass && xcodebuild build` — build succeeds
4. **Integration**: Gateway agent connects NFC reader (or TCP simulator), phone NFC tap triggers SELECT → AUTHENTICATE → signature verified → door unlocks

## PR Strategy

Create one PR per repo:
1. **Backend PR**: Tasks 1-6 (protocol v2 + gateway endpoints + nonce cache + NFC reader)
2. **Android PR**: Tasks 7-10 (KeystoreManager v2 + BLE v2 + HCE + manifest)
3. **iOS PR**: Task 11 (BLE v2 only)

Backend merges first (backward compatible). Android and iOS can merge in any order after backend.
