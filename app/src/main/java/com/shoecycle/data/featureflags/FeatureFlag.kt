package com.shoecycle.data.featureflags

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

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
 * ## Fail CLOSED at the decode layer (ShoeCycle-Web-rae, ShoeCycle-Web-54b)
 * Every field fails safe for a hostile / malformed value, and no field relies on
 * `coerceInputValues` (which would silently coerce a null back to a permissive property default).
 * The guarantee is enforced per FIELD and per FLAG so a single corrupt definition resolves OFF in
 * isolation instead of poisoning the whole payload:
 *   - `enabled`   — master kill switch (spec §4.1 step 1). [FailClosedBooleanSerializer] decodes to
 *                   `true` ONLY for a genuine JSON boolean `true`; null, numbers, strings, objects,
 *                   arrays, and an absent key all resolve `false`.
 *   - `rolloutPercentage` — [FailClosedRolloutPercentageSerializer] decodes to a value ONLY for a
 *                   genuine JSON integer; a quoted string ("50"), a float (50.5), an object, or
 *                   null all decode to `null`, which the evaluator treats as OFF (spec §4.1).
 *   - the flag list — [LenientFeatureFlagListSerializer] drops any element that still fails to
 *                   decode (e.g. a missing `key`) rather than throwing and discarding every other
 *                   healthy flag in the same response.
 */
@Serializable
data class FeatureFlag(
    val key: String,
    @Serializable(with = FailClosedBooleanSerializer::class)
    val enabled: Boolean = false,
    @Serializable(with = FailClosedRolloutPercentageSerializer::class)
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
        //  - JSON null            -> booleanOrNull is null
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
 * A nullable-Int serializer for `rolloutPercentage` that fails CLOSED (ShoeCycle-Web-54b).
 *
 * Decoding returns a number ONLY for a genuine unquoted JSON integer. Every other shape decodes to
 * `null`, which [FeatureFlagEvaluator] treats as OFF (spec §4.1 — a missing / non-numeric
 * percentage resolves OFF, never a partial rollout):
 *   - JSON null / absent   -> `null`
 *   - a quoted string "50" -> `null` (a type mismatch; kotlinx's `intOrNull` would otherwise
 *                             accept the string content "50" and roll the feature out to ~50%)
 *   - a float 50.5         -> `null` (not an integer bucket bound)
 *   - an object / array    -> `null`
 *
 * Deliberately does NOT depend on `coerceInputValues`, and never throws — a malformed value must
 * fail this one flag safe, not abort the whole payload.
 */
@OptIn(ExperimentalSerializationApi::class)
object FailClosedRolloutPercentageSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FailClosedRolloutPercentage", PrimitiveKind.INT).nullable

    override fun deserialize(decoder: Decoder): Int? {
        // Non-JSON decoders fall back to strict decoding.
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return null
        // A quoted string is a TYPE MISMATCH → OFF (do not accept the string content as a number).
        if (primitive.isString) return null
        // intOrNull rejects floats, non-numeric content, and JSON null → all resolve OFF.
        return primitive.intOrNull
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/**
 * A list serializer that fails CLOSED per FLAG (ShoeCycle-Web-54b).
 *
 * Decodes the `flags` array element by element, dropping any element that fails to decode instead
 * of letting one malformed definition throw and discard every healthy flag in the response. A
 * dropped flag is simply absent from the resolved set, so [FeatureFlagEvaluator] falls back to the
 * caller default (OFF) for its key — the same fail-safe outcome as an unknown key (spec §4.2).
 *
 * The per-field serializers above already absorb malformed `enabled` / `rolloutPercentage` values,
 * so an element only drops here when it is structurally unusable — e.g. not a JSON object, or a
 * missing / null `key` (the one required field with no safe default).
 */
object LenientFeatureFlagListSerializer : KSerializer<List<FeatureFlag>> {
    private val delegate = ListSerializer(FeatureFlag.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<FeatureFlag> {
        // Non-JSON decoders fall back to strict list decoding.
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val array = jsonDecoder.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            try {
                jsonDecoder.json.decodeFromJsonElement(FeatureFlag.serializer(), element)
            } catch (e: Exception) {
                // Intentionally broad: a structurally-unusable element can surface as either a
                // SerializationException (e.g. missing `key`) or an IllegalArgumentException
                // (e.g. a primitive where an object was expected), and the fail-closed contract is
                // "drop anything this decoder cannot turn into a flag" — not "drop a specific
                // exception type". This runs synchronously inside decodeFromString, so there is no
                // CancellationException to preserve.
                null
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<FeatureFlag>) {
        delegate.serialize(encoder, value)
    }
}

/**
 * Envelope for the public /api/feature-flags serve endpoint. Matches FeatureFlagsResponse in
 * the OpenAPI contract: a single top-level `flags` array of raw definitions.
 */
@Serializable
data class FeatureFlagsResponse(
    @Serializable(with = LenientFeatureFlagListSerializer::class)
    val flags: List<FeatureFlag> = emptyList()
)
