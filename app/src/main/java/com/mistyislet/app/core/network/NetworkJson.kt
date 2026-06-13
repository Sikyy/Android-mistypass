package com.mistyislet.app.core.network

import kotlinx.serialization.json.Json

/**
 * Single source of truth for the JSON wire format spoken to the MistyIslet backend.
 *
 * Shared by the Retrofit converters ([ApiClientModule]), error-body parsing
 * ([safeApiCall]), and the alarm SSE stream ([AlarmStreamManager]). Tests that
 * assert request/response bodies (e.g. AdminApiEndpointTest) must use this
 * instance too: a hand-mirrored copy once dropped `encodeDefaults` and wrongly
 * asserted that an unset `door_ids` is omitted, while production sends
 * `door_ids:[]`.
 *
 * - `ignoreUnknownKeys`: the backend adds response fields without versioning;
 *   older app builds must keep parsing.
 * - `coerceInputValues`: `null` or out-of-range values in responses coerce to
 *   the property default instead of throwing.
 * - `encodeDefaults`: defaulted request properties are serialized explicitly
 *   (e.g. `door_ids:[]` when no doors are selected) — the backend expects the
 *   key to be present. Decode-only call sites (error bodies, SSE events) are
 *   unaffected by this flag, which is why their previously separate configs
 *   could fold into this one.
 */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
