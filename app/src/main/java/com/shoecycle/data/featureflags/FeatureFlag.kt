package com.shoecycle.data.featureflags

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

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
 *
 * ## Kill switch fails CLOSED (ShoeCycle-Web-rae)
 * `enabled` is the master kill switch (spec §4.1 step 1) and MUST fail safe to `false` for any
 * null / absent / type-mismatched value — a corrupt definition must resolve OFF, not ON. Two
 * mechanisms enforce this and neither relies on `coerceInputValues` (which would silently coerce
 * a null back to the property default and, if that default were `true`, fail the switch OPEN):
 *   1. The property default is `false`, so an *absent* `enabled` key resolves OFF.
 *   2. [FailClosedBooleanSerializer] decodes `enabled` to `true` ONLY for a genuine JSON boolean
 *      `true`; null, numbers, strings, objects, and any type mismatch all decode to `false`
 *      without throwing.
 */
@Serializable
data class FeatureFlag(
    val key: String,
    @Serializable(with = FailClosedBooleanSerializer::class)
    val enabled: Boolean = false,
    // Nullable so a missing / malformed rolloutPercentage decodes rather than throwing.
    // Per spec §4.1 a missing/non-numeric percentage resolves OFF.
    val rolloutPercentage: Int? = null
)

/**
 * A boolean serializer for the feature-flag kill switch that fails CLOSED (ShoeCycle-Web-rae).
 *
 * Decoding returns `true` only for a genuine JSON boolean `true`. Any hostile / malformed value —
 * `null`, a number, a string (`"true"`, `"yes"`, `"1"`), an object, an array — decodes to `false`
 * instead of throwing or coercing to a permissive default. This guarantees the master kill switch
 * (spec §4.1 step 1) fails safe-OFF for a corrupt server payload, matching the fail-safe behavior
 * of `rolloutPercentage`. Deliberately does NOT depend on `coerceInputValues`.
 */
object FailClosedBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FailClosedBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        // Non-JSON decoders (e.g. a hypothetical binary format) fall back to strict decoding.
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val element = jsonDecoder.decodeJsonElement()
        // Only an UNQUOTED JSON boolean `true` is ON. Reject:
        //  - JSON null            -> not a JsonPrimitive with a boolean value
        //  - numbers              -> booleanOrNull is null
        //  - objects / arrays     -> not a JsonPrimitive
        //  - quoted strings       -> isString == true; a string "true" is a TYPE MISMATCH and
        //                            must fail closed (kotlinx's booleanOrNull would otherwise
        //                            accept the string content "true"/"false").
        val primitive = element as? JsonPrimitive ?: return false
        if (primitive.isString) return false
        return primitive.booleanOrNull == true
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

/**
 * Envelope for the public /api/feature-flags serve endpoint. Matches FeatureFlagsResponse in
 * the OpenAPI contract: a single top-level `flags` array of raw definitions.
 */
@Serializable
data class FeatureFlagsResponse(
    val flags: List<FeatureFlag> = emptyList()
)
