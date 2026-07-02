package com.shoecycle.data.featureflags

import kotlinx.serialization.Serializable

/**
 * A single feature-flag DEFINITION (global config, not per-user data).
 *
 * Mirrors the FeatureFlag schema in the web OpenAPI contract and the flag shape pinned by
 * architecture/feature-toggles.md §1. The per-caller rollout decision is computed locally by
 * [FeatureFlagEvaluator] from this definition — the server serves raw definitions only.
 *
 * `targeting` is reserved for v2+ and is intentionally ignored by the v1 evaluator. Unknown
 * keys are ignored via the lenient [FeatureFlagJson] configuration so a forward-compatible
 * payload never crashes an older client.
 */
@Serializable
data class FeatureFlag(
    val key: String,
    val enabled: Boolean = true,
    // Nullable so a missing / malformed rolloutPercentage decodes rather than throwing.
    // Per spec §4.1 a missing/non-numeric percentage resolves OFF.
    val rolloutPercentage: Int? = null
)

/**
 * Envelope for the public /api/feature-flags serve endpoint. Matches FeatureFlagsResponse in
 * the OpenAPI contract: a single top-level `flags` array of raw definitions.
 */
@Serializable
data class FeatureFlagsResponse(
    val flags: List<FeatureFlag> = emptyList()
)
