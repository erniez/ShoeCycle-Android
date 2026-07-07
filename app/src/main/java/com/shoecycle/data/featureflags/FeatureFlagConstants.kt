package com.shoecycle.data.featureflags

import java.util.concurrent.TimeUnit

/**
 * Named constants for the feature-toggle system. No magic numbers per the workspace data-model
 * rules and acceptance criterion 5.
 */
object FeatureFlagConstants {
    /** Public serve endpoint path (unauthenticated). See spec C / OpenAPI /api/feature-flags. */
    const val ENDPOINT_PATH = "/api/feature-flags"

    /**
     * Base URL for the ShoeCycle API. The feature-flag read is PUBLIC — no auth header is sent.
     */
    const val API_BASE_URL = "https://api.shoecycleapp.com"

    /** Full serve URL. */
    const val SERVE_URL = "$API_BASE_URL$ENDPOINT_PATH"

    /**
     * How long a cached set of definitions is considered fresh. After this, the repository
     * attempts a network refresh but still falls back to the cache on failure (spec §4.3).
     */
    val CACHE_TTL_MILLIS: Long = TimeUnit.MINUTES.toMillis(60)

    /** Network timeouts for the serve request. */
    val REQUEST_TIMEOUT_SECONDS: Long = 15

    /**
     * The single ASCII colon that separates flagKey and bucketingId in the hash input
     * (spec §3.2). Pinned — no whitespace, no newline.
     */
    const val HASH_INPUT_SEPARATOR = ":"

    /** Hash algorithm pinned by spec §3.2. */
    const val HASH_ALGORITHM = "SHA-256"

    /** bucket = hashUint32 % BUCKET_MODULO ⇒ integer in [0, 100). */
    const val BUCKET_MODULO = 100L
}

/**
 * DataStore preference keys for the feature-toggle system. Kept as string names in one place so
 * they cannot drift. Reuses the app's existing `user_settings` DataStore (spec §3.1 pins
 * Android anonymous-UUID persistence to the same store used for UserSettings).
 */
object FeatureFlagPreferenceKeys {
    const val ANON_ID = "feature_toggles_anon_id"
    const val CACHED_FLAGS_JSON = "feature_toggles_cached_flags_json"
    const val CACHE_TIMESTAMP = "feature_toggles_cache_timestamp"
}
