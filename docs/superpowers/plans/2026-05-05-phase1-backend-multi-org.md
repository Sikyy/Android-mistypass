# Phase 1: Backend Multi-Org Endpoints

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-org, magic-link auth, and place-scoped admin endpoints to the MistyPass Go backend so the Android app can implement Kisi-style navigation.

**Architecture:** Additive endpoint layer on existing chi router. New `/app/orgs/`, `/app/auth/` extensions, and `/app/places/{placeId}/` scoped admin endpoints. Old endpoints stay working for iOS backward compatibility. Org context flows via JWT `org_id` claim.

**Tech Stack:** Go 1.25+, Chi v5, PostgreSQL 16 (sqlc), Redis 7, existing auth middleware

**Repo:** `/Users/siky/code/MistyPass`

**Design Spec:** `/Users/siky/code/android-MistyisletPass/docs/superpowers/specs/2026-05-05-kisi-style-full-refactor-design.md` (Section 3)

---

### Task 1: Database Migrations for Multi-Org

**Files:**
- Create: `api/internal/state/migrations/0XX_multi_org.sql`

The backend already has tenant-level scoping (`tenant_id` on users). "Organization" in the mobile context maps to "tenant" in the backend. This task adds the tables needed for multi-org mobile login:

- [ ] **Step 1: Create migration file**

```sql
-- 0XX_multi_org.sql
-- Multi-org support: user_org_memberships allows a single user (by email)
-- to belong to multiple tenants/orgs with different roles.

CREATE TABLE IF NOT EXISTS user_org_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role TEXT NOT NULL DEFAULT 'resident',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    UNIQUE(user_id, tenant_id)
);

CREATE INDEX idx_user_org_memberships_user ON user_org_memberships(user_id);
CREATE INDEX idx_user_org_memberships_tenant ON user_org_memberships(tenant_id);

-- Magic link tokens
CREATE TABLE IF NOT EXISTS magic_link_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_magic_link_tokens_token ON magic_link_tokens(token);
CREATE INDEX idx_magic_link_tokens_email ON magic_link_tokens(email);

-- Backfill: create org membership for every existing user
INSERT INTO user_org_memberships (user_id, tenant_id, role)
SELECT id, tenant_id, role FROM users
ON CONFLICT (user_id, tenant_id) DO NOTHING;
```

- [ ] **Step 2: Run migration**

```bash
cd /Users/siky/code/MistyPass
docker compose exec -T postgres psql -U postgres -d mistypass < api/internal/state/migrations/0XX_multi_org.sql
```

Expected: Tables created, existing users backfilled into memberships.

- [ ] **Step 3: Commit**

```bash
git add api/internal/state/migrations/0XX_multi_org.sql
git commit -m "feat: add multi-org and magic link database tables"
```

---

### Task 2: Org Endpoints (list orgs, switch org)

**Files:**
- Create: `api/internal/http/routes_app_org.go`
- Modify: `api/internal/http/router.go` (add route registration)

- [ ] **Step 1: Create routes_app_org.go**

```go
package httpx

import (
    "net/http"
    "time"

    "github.com/go-chi/chi/v5"
)

// appListOrgs returns the organizations the authenticated user belongs to.
// GET /api/v1/app/orgs
func (s *server) appListOrgs(w http.ResponseWriter, r *http.Request) {
    user, ok := authenticatedUser(r)
    if !ok {
        writeError(w, http.StatusUnauthorized, "invalid access token")
        return
    }

    rows, err := s.db.Query(r.Context(), `
        SELECT m.tenant_id, t.name, t.domain, t.logo_url, m.role, m.last_used_at
        FROM user_org_memberships m
        JOIN tenants t ON t.id = m.tenant_id
        WHERE m.user_id = $1
        ORDER BY m.last_used_at DESC NULLS LAST, m.joined_at DESC
    `, user.ID)
    if err != nil {
        writeInternalError(w, r, err)
        return
    }
    defer rows.Close()

    type org struct {
        ID         string  `json:"id"`
        Name       string  `json:"name"`
        Domain     string  `json:"domain"`
        Logo       *string `json:"logo"`
        Role       string  `json:"role"`
        LastUsedAt *string `json:"last_used_at"`
    }

    var orgs []org
    for rows.Next() {
        var o org
        var lastUsed *time.Time
        if err := rows.Scan(&o.ID, &o.Name, &o.Domain, &o.Logo, &o.Role, &lastUsed); err != nil {
            writeInternalError(w, r, err)
            return
        }
        if lastUsed != nil {
            t := lastUsed.Format(time.RFC3339)
            o.LastUsedAt = &t
        }
        orgs = append(orgs, o)
    }
    if orgs == nil {
        orgs = []org{}
    }

    writeJSON(w, http.StatusOK, orgs)
}

// appSwitchOrg switches the user's active org context and returns new scoped tokens.
// POST /api/v1/app/orgs/{orgId}/switch
func (s *server) appSwitchOrg(w http.ResponseWriter, r *http.Request) {
    user, ok := authenticatedUser(r)
    if !ok {
        writeError(w, http.StatusUnauthorized, "invalid access token")
        return
    }

    orgID := chi.URLParam(r, "orgId")

    // Verify membership
    var role string
    err := s.db.QueryRow(r.Context(), `
        SELECT role FROM user_org_memberships
        WHERE user_id = $1 AND tenant_id = $2
    `, user.ID, orgID).Scan(&role)
    if err != nil {
        writeError(w, http.StatusForbidden, "not a member of this organization")
        return
    }

    // Update last_used_at
    _, _ = s.db.Exec(r.Context(), `
        UPDATE user_org_memberships SET last_used_at = now()
        WHERE user_id = $1 AND tenant_id = $2
    `, user.ID, orgID)

    // Issue new org-scoped tokens
    tokens, err := s.authSvc.IssueOrgScopedTokens(r.Context(), user.ID, orgID, role)
    if err != nil {
        writeInternalError(w, r, err)
        return
    }

    writeJSON(w, http.StatusOK, map[string]any{
        "access_token":  tokens.AccessToken,
        "refresh_token": tokens.RefreshToken,
        "expires_in":    tokens.ExpiresIn,
        "org_id":        orgID,
    })
}
```

- [ ] **Step 2: Register routes in router.go**

Add inside the `/api/v1/app` route group in `router.go`:

```go
// Multi-org
r.Get("/orgs", s.appListOrgs)
r.Post("/orgs/{orgId}/switch", s.appSwitchOrg)
```

- [ ] **Step 3: Implement IssueOrgScopedTokens in auth service**

Add to the auth service the ability to include `org_id` claim in JWT. The existing `IssueTokens` method needs an optional `orgID` parameter. The JWT payload becomes:

```json
{"sub": "user_123", "org_id": "org_456", "role": "admin", "exp": ...}
```

- [ ] **Step 4: Test with curl**

```bash
# Login first
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/app/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password"}' | jq -r '.access_token')

# List orgs
curl -s http://localhost:8080/api/v1/app/orgs \
  -H "Authorization: Bearer $TOKEN" | jq

# Switch org
curl -s -X POST http://localhost:8080/api/v1/app/orgs/ORG_ID/switch \
  -H "Authorization: Bearer $TOKEN" | jq
```

- [ ] **Step 5: Commit**

```bash
git add api/internal/http/routes_app_org.go api/internal/http/router.go
git commit -m "feat: add /app/orgs list and switch endpoints for multi-org mobile"
```

---

### Task 3: Place Endpoints (list places, search, lockdown)

**Files:**
- Create: `api/internal/http/routes_app_places.go`
- Modify: `api/internal/http/router.go`

- [ ] **Step 1: Create routes_app_places.go**

Implement these handlers following the existing pattern from `routes_app_access.go`:

```go
// GET  /api/v1/app/orgs/{orgId}/places          -> list places user can access in this org
// GET  /api/v1/app/orgs/{orgId}/places/search?q= -> search places
// GET  /api/v1/app/places/{placeId}/doors        -> list doors in place (replaces my-doors)
// GET  /api/v1/app/places/{placeId}/doors/search?q= -> search doors in place
// POST /api/v1/app/places/{placeId}/doors/{doorId}/unlock -> place-scoped unlock
// POST /api/v1/app/places/{placeId}/doors/{doorId}/qr-unlock -> place-scoped QR unlock
// POST /api/v1/app/places/{placeId}/lockdown     -> enable lockdown
// DELETE /api/v1/app/places/{placeId}/lockdown   -> disable lockdown
// PUT  /api/v1/app/places/{placeId}/doors/{doorId}/favorite -> favorite
// DELETE /api/v1/app/places/{placeId}/doors/{doorId}/favorite -> unfavorite
```

Each handler:
1. Extracts authenticated user via `authenticatedUser(r)`
2. Extracts `placeId` via `chi.URLParam(r, "placeId")`
3. Verifies user has access to the place (via org membership + building scope)
4. Delegates to existing service methods (`s.accessSvc`, `s.spaceSvc`)
5. Returns JSON response using `writeJSON`

The `appPlaceListDoors` handler reuses the logic from `appAccessMyDoors` but filters by `placeId` instead of returning all doors.

The `appPlaceUnlockDoor` handler reuses the logic from `appUnlockDoor` but adds placeId validation.

- [ ] **Step 2: Register routes in router.go**

```go
// Place-scoped access
r.Get("/orgs/{orgId}/places", s.appListPlaces)
r.Get("/orgs/{orgId}/places/search", s.appSearchPlaces)
r.Get("/places/{placeId}/doors", s.appPlaceListDoors)
r.Get("/places/{placeId}/doors/search", s.appPlaceSearchDoors)
r.Post("/places/{placeId}/doors/{doorId}/unlock", s.appPlaceUnlockDoor)
r.Post("/places/{placeId}/doors/{doorId}/qr-unlock", s.appPlaceQRUnlock)
r.Post("/places/{placeId}/lockdown", s.appPlaceEnableLockdown)
r.Delete("/places/{placeId}/lockdown", s.appPlaceDisableLockdown)
r.Put("/places/{placeId}/doors/{doorId}/favorite", s.appPlaceFavoriteDoor)
r.Delete("/places/{placeId}/doors/{doorId}/favorite", s.appPlaceUnfavoriteDoor)
```

- [ ] **Step 3: Test endpoints**

```bash
# List places
curl -s http://localhost:8080/api/v1/app/orgs/$ORG_ID/places \
  -H "Authorization: Bearer $TOKEN" | jq

# List doors in place
curl -s http://localhost:8080/api/v1/app/places/$PLACE_ID/doors \
  -H "Authorization: Bearer $TOKEN" | jq
```

- [ ] **Step 4: Commit**

```bash
git add api/internal/http/routes_app_places.go api/internal/http/router.go
git commit -m "feat: add place-scoped door, unlock, and lockdown endpoints"
```

---

### Task 4: Auth Enhancement Endpoints (magic link, org lookup, 2FA, SSO, registration)

**Files:**
- Create: `api/internal/http/routes_app_auth_enhanced.go`
- Modify: `api/internal/http/router.go`

- [ ] **Step 1: Create routes_app_auth_enhanced.go**

Implement these handlers:

```go
// POST /api/v1/app/auth/magic-link           -> send magic link email
// POST /api/v1/app/auth/magic-link/verify    -> verify magic link token
// GET  /api/v1/app/auth/org-lookup?domain=   -> look up org by domain
// GET  /api/v1/app/auth/org/{orgId}/methods  -> available sign-in methods
// POST /api/v1/app/auth/sso/{orgId}          -> initiate SSO redirect
// POST /api/v1/app/auth/2fa/verify           -> verify TOTP/SMS code
// POST /api/v1/app/auth/2fa/backup           -> verify backup code
// POST /api/v1/app/auth/register             -> create account
// POST /api/v1/app/auth/restore-password     -> send password reset email
```

Key implementation details:

**Magic link flow:**
1. `appRequestMagicLink`: Generate random token, store in `magic_link_tokens` table with 15-min expiry, send email
2. `appVerifyMagicLink`: Look up token, verify not expired/used, mark as used, return LoginResponse with JWT

**Org lookup:**
1. `appOrgLookup`: Query `tenants` table by `domain` column, return org info + available auth methods

**Org auth methods:**
1. `appOrgMethods`: Return array of available methods based on org config (always `classic`, plus `sso` if configured, plus `webauthn` if enabled)

**2FA:**
1. `appVerify2FA`: Accept `{user_id, code, type}`, verify TOTP code against user's MFA secret, return tokens on success
2. `appVerifyBackupCode`: Accept `{user_id, code}`, verify against stored backup codes, consume on success

**Registration:**
1. `appCreateAccount`: Accept `{name, email, password, domain}`, create user in org, return LoginResponse

- [ ] **Step 2: Register routes (these go in the UNAUTHENTICATED group)**

```go
// Auth enhancements (no bearer token required)
r.Post("/auth/magic-link", s.appRequestMagicLink)
r.Post("/auth/magic-link/verify", s.appVerifyMagicLink)
r.Get("/auth/org-lookup", s.appOrgLookup)
r.Get("/auth/org/{orgId}/methods", s.appOrgMethods)
r.Post("/auth/sso/{orgId}", s.appInitiateSSO)
r.Post("/auth/2fa/verify", s.appVerify2FA)
r.Post("/auth/2fa/backup", s.appVerifyBackupCode)
r.Post("/auth/register", s.appCreateAccount)
r.Post("/auth/restore-password", s.appRestorePassword)
```

- [ ] **Step 3: Commit**

```bash
git add api/internal/http/routes_app_auth_enhanced.go api/internal/http/router.go
git commit -m "feat: add magic link, org lookup, 2FA, SSO, and registration auth endpoints"
```

---

### Task 5: Admin Endpoints - User Management

**Files:**
- Create: `api/internal/http/routes_app_admin_users.go`
- Modify: `api/internal/http/router.go`

- [ ] **Step 1: Create routes_app_admin_users.go**

```go
// GET    /api/v1/app/places/{placeId}/users                       -> list users
// GET    /api/v1/app/places/{placeId}/users/search?q=             -> search users
// GET    /api/v1/app/places/{placeId}/users/{userId}              -> user details
// POST   /api/v1/app/places/{placeId}/users                       -> add user
// PUT    /api/v1/app/places/{placeId}/users/{userId}/role         -> update role
// GET    /api/v1/app/places/{placeId}/users/{userId}/logins       -> user login devices
// GET    /api/v1/app/places/{placeId}/users/{userId}/access-rights -> user access rights
// POST   /api/v1/app/places/{placeId}/users/{userId}/share-access  -> share access
```

All handlers check admin role via `requireRoles("admin", "tenant_admin")` middleware. Each delegates to existing user/access services scoped by placeId.

- [ ] **Step 2: Register routes with admin middleware**

```go
// Admin: user management (place-scoped, admin only)
r.With(requireRoles("admin", "tenant_admin")).Route("/places/{placeId}/users", func(r chi.Router) {
    r.Get("/", s.appAdminListUsers)
    r.Get("/search", s.appAdminSearchUsers)
    r.Post("/", s.appAdminAddUser)
    r.Get("/{userId}", s.appAdminGetUser)
    r.Put("/{userId}/role", s.appAdminUpdateUserRole)
    r.Get("/{userId}/logins", s.appAdminGetUserLogins)
    r.Get("/{userId}/access-rights", s.appAdminGetAccessRights)
    r.Post("/{userId}/share-access", s.appAdminShareAccess)
})
```

- [ ] **Step 3: Commit**

```bash
git add api/internal/http/routes_app_admin_users.go api/internal/http/router.go
git commit -m "feat: add place-scoped admin user management endpoints"
```

---

### Task 6: Admin Endpoints - Events, Incidents, Activity

**Files:**
- Create: `api/internal/http/routes_app_admin_events.go`
- Modify: `api/internal/http/router.go`

- [ ] **Step 1: Create routes_app_admin_events.go**

```go
// Events
// GET  /api/v1/app/places/{placeId}/events                    -> paginated, filterable
// GET  /api/v1/app/places/{placeId}/events/{eventId}          -> event details
// GET  /api/v1/app/places/{placeId}/events/{eventId}/related  -> related timeline
// GET  /api/v1/app/places/{placeId}/events/{eventId}/media    -> camera snapshots

// Incidents
// GET  /api/v1/app/places/{placeId}/incidents                 -> paginated, filterable
// GET  /api/v1/app/places/{placeId}/incidents/{id}            -> details
// GET  /api/v1/app/places/{placeId}/incidents/{id}/occurrences -> occurrence list

// Activity
// GET  /api/v1/app/places/{placeId}/activity                  -> user presence
// GET  /api/v1/app/places/{placeId}/activity/{eventId}        -> presence event detail
```

Event list endpoint supports query params for filtering: `?user_id=`, `?object_type=`, `?object_action=`, `?object_id=`, `?offset=`, `?limit=`

Incident list supports: `?state=`, `?type=`, `?subject_type=`, `?status=`, `?severity=`, `?offset=`, `?limit=`

- [ ] **Step 2: Register routes**

```go
r.With(requireRoles("admin", "tenant_admin")).Route("/places/{placeId}", func(r chi.Router) {
    // Events
    r.Get("/events", s.appAdminListEvents)
    r.Get("/events/{eventId}", s.appAdminGetEvent)
    r.Get("/events/{eventId}/related", s.appAdminGetRelatedEvents)
    r.Get("/events/{eventId}/media", s.appAdminGetEventMedia)
    // Incidents
    r.Get("/incidents", s.appAdminListIncidents)
    r.Get("/incidents/{incidentId}", s.appAdminGetIncident)
    r.Get("/incidents/{incidentId}/occurrences", s.appAdminGetOccurrences)
    // Activity
    r.Get("/activity", s.appAdminGetUserActivity)
    r.Get("/activity/{eventId}", s.appAdminGetPresenceEvent)
})
```

- [ ] **Step 3: Commit**

```bash
git add api/internal/http/routes_app_admin_events.go api/internal/http/router.go
git commit -m "feat: add place-scoped admin events, incidents, and activity endpoints"
```

---

### Task 7: Admin Endpoints - Schedules, Zones, Cards, Credentials, Teams

**Files:**
- Create: `api/internal/http/routes_app_admin_resources.go`
- Modify: `api/internal/http/router.go`

- [ ] **Step 1: Create routes_app_admin_resources.go**

```go
// Schedules
// GET/POST       /api/v1/app/places/{placeId}/schedules
// PUT/DELETE     /api/v1/app/places/{placeId}/schedules/{id}
// GET            /api/v1/app/places/{placeId}/holiday-regions
// GET            /api/v1/app/places/{placeId}/holiday-regions/{id}/holidays

// Zones
// GET            /api/v1/app/places/{placeId}/zones
// GET            /api/v1/app/places/{placeId}/zones/{id}

// Cards
// GET            /api/v1/app/places/{placeId}/cards
// POST           /api/v1/app/places/{placeId}/cards/assign
// DELETE         /api/v1/app/places/{placeId}/cards/{cardUid}
// GET            /api/v1/app/places/{placeId}/cards/{cardUid}/status
// POST           /api/v1/app/places/{placeId}/cards/manual-token

// Digital Credentials
// GET/POST       /api/v1/app/places/{placeId}/credentials
// GET            /api/v1/app/places/{placeId}/credentials/{id}
// GET            /api/v1/app/places/{placeId}/credentials/search?q=

// Teams
// GET/POST       /api/v1/app/places/{placeId}/teams
// GET/PUT/DELETE /api/v1/app/places/{placeId}/teams/{teamId}
```

All handlers delegate to existing service layer methods. The existing reference API already has full CRUD for schedules, zones, cards, teams - these mobile admin endpoints are thin wrappers that add place-scoping and mobile-friendly response shapes.

- [ ] **Step 2: Register all routes**

```go
r.With(requireRoles("admin", "tenant_admin")).Route("/places/{placeId}", func(r chi.Router) {
    // Schedules
    r.Get("/schedules", s.appAdminListSchedules)
    r.Post("/schedules", s.appAdminCreateSchedule)
    r.Put("/schedules/{scheduleId}", s.appAdminUpdateSchedule)
    r.Delete("/schedules/{scheduleId}", s.appAdminDeleteSchedule)
    r.Get("/holiday-regions", s.appAdminListHolidayRegions)
    r.Get("/holiday-regions/{regionId}/holidays", s.appAdminListHolidays)
    // Zones
    r.Get("/zones", s.appAdminListZones)
    r.Get("/zones/{zoneId}", s.appAdminGetZone)
    // Cards
    r.Get("/cards", s.appAdminListCards)
    r.Post("/cards/assign", s.appAdminAssignCard)
    r.Delete("/cards/{cardUid}", s.appAdminUnassignCard)
    r.Get("/cards/{cardUid}/status", s.appAdminGetCardStatus)
    r.Post("/cards/manual-token", s.appAdminManualCardToken)
    // Credentials
    r.Get("/credentials", s.appAdminListCredentials)
    r.Post("/credentials", s.appAdminCreateCredential)
    r.Get("/credentials/{credentialId}", s.appAdminGetCredential)
    r.Get("/credentials/search", s.appAdminSearchCredentials)
    // Teams
    r.Get("/teams", s.appAdminListTeams)
    r.Post("/teams", s.appAdminCreateTeam)
    r.Get("/teams/{teamId}", s.appAdminGetTeam)
    r.Put("/teams/{teamId}", s.appAdminUpdateTeam)
    r.Delete("/teams/{teamId}", s.appAdminDeleteTeam)
})
```

- [ ] **Step 3: Commit**

```bash
git add api/internal/http/routes_app_admin_resources.go api/internal/http/router.go
git commit -m "feat: add place-scoped admin schedules, zones, cards, credentials, teams endpoints"
```

---

### Task 8: Integration Test & Staging Deploy

**Files:**
- Create: `api/internal/http/router_app_org_test.go`

- [ ] **Step 1: Write integration test**

```go
func TestAppOrgFlow(t *testing.T) {
    // 1. Login with test user
    // 2. GET /app/orgs -> verify returns at least 1 org
    // 3. POST /app/orgs/{id}/switch -> verify returns new tokens with org_id claim
    // 4. GET /app/orgs/{id}/places -> verify returns places
    // 5. GET /app/places/{id}/doors -> verify returns doors scoped to place
    // 6. Verify old /app/access/my-doors still works (backward compat)
}
```

- [ ] **Step 2: Run tests**

```bash
cd /Users/siky/code/MistyPass/api
go test ./internal/http/ -run TestAppOrgFlow -v
```

- [ ] **Step 3: Verify iOS backward compatibility**

```bash
# Old endpoints must still work
curl -s http://localhost:8080/api/v1/app/access/my-doors \
  -H "Authorization: Bearer $OLD_TOKEN" | jq '.items | length'
# Expected: returns doors (non-zero)
```

- [ ] **Step 4: Deploy to staging**

```bash
cd /Users/siky/code/MistyPass
docker compose up -d --build api
```

- [ ] **Step 5: Commit test**

```bash
git add api/internal/http/router_app_org_test.go
git commit -m "test: add multi-org flow integration test"
```
