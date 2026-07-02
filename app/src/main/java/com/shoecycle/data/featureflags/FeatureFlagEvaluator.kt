package com.shoecycle.data.featureflags

import java.security.MessageDigest

/**
 * Deterministic bucketing evaluator — the Kotlin port of the pinned algorithm in
 * architecture/feature-toggles.md §3. This MUST produce identical hash / bucket / inRollout
 * decisions to the iOS, Web, and server evaluators. Any divergence is a bug in this port, not
 * in the contract.
 *
 * Pure logic, no Android framework dependencies, so it is trivially unit-testable and shared by
 * the repository/interactor layer.
 *
 * ## The signedness trap (spec §3.3)
 * `hashUint32` is an UNSIGNED 32-bit value in [0, 4294967295]. Kotlin/Java `Int` is SIGNED, and
 * reading the first four digest bytes into an `Int` yields a negative number whenever byte 0 has
 * its high bit set (>= 0x80) — and `negative % 100` is negative in Kotlin/Java, which both
 * mis-buckets and can leave the range [0, 100). We therefore widen to an unsigned value (a `Long`
 * masked with 0xFFFFFFFFL) BEFORE taking `% 100`. The two canary vectors e9b6b98f (bucket 83) and
 * 9298ff9e (bucket 46) exercise exactly this path.
 */
object FeatureFlagEvaluator {

    /**
     * Result of the raw bucketing math, exposed primarily so conformance tests can assert every
     * intermediate value against the frozen oracle.
     */
    data class BucketResult(
        /** First 4 digest bytes, big-endian, UNSIGNED, in [0, 4294967295]. */
        val hashUint32: Long,
        /** hashUint32 % 100, guaranteed in [0, 100). */
        val bucket: Int
    )

    /**
     * Computes the unsigned 32-bit hash and bucket for a given flag key + bucketing id.
     *
     * bucketingInput = "flagKey:bucketingId" (UTF-8, single colon, no whitespace/newline)
     * digest         = SHA-256(bucketingInput)   // 32 bytes
     * hashUint32     = uint32_big_endian(digest[0..4])   // first 4 bytes, UNSIGNED
     * bucket         = hashUint32 % 100                  // integer in [0, 100)
     */
    fun bucketFor(flagKey: String, bucketingId: String): BucketResult {
        val input = flagKey + FeatureFlagConstants.HASH_INPUT_SEPARATOR + bucketingId
        val digest = MessageDigest.getInstance(FeatureFlagConstants.HASH_ALGORITHM)
            .digest(input.toByteArray(Charsets.UTF_8))

        // First 4 bytes, big-endian. Mask each byte to 0..255 and widen to Long so the assembled
        // value stays UNSIGNED — never let a signed Int touch this.
        val hashUint32: Long =
            ((digest[0].toLong() and 0xFF) shl 24) or
            ((digest[1].toLong() and 0xFF) shl 16) or
            ((digest[2].toLong() and 0xFF) shl 8) or
            (digest[3].toLong() and 0xFF)

        val bucket = (hashUint32 % FeatureFlagConstants.BUCKET_MODULO).toInt()
        return BucketResult(hashUint32 = hashUint32, bucket = bucket)
    }

    /**
     * Inclusion rule (spec §3.3): inRollout = bucket < rolloutPercentage. Strict `<`.
     */
    fun isInRollout(flagKey: String, bucketingId: String, rolloutPercentage: Int): Boolean {
        return bucketFor(flagKey, bucketingId).bucket < rolloutPercentage
    }

    /**
     * Resolves a single flag to a boolean using the full precedence in spec §4.1:
     *
     * 1. enabled == false            → OFF (kill switch wins over everything)
     * 2. rolloutPercentage >= 100    → ON  (short-circuit, no hashing)
     * 3. rolloutPercentage <= 0      → OFF (short-circuit, no hashing)
     * 4. else                        → ON iff bucket < rolloutPercentage
     *
     * The >= 100 / <= 0 comparisons absorb any out-of-range percentage (150 behaves as 100, -5 as
     * 0), so no separate clamp is needed. A missing / non-numeric percentage (null) is treated as
     * OFF (§4.1) — it fails safe rather than randomly exposing a fraction of users.
     *
     * @param flag the flag definition (may be null when the key is unknown)
     * @param bucketingId the stable per-caller identity (Cognito sub or persisted anon UUID)
     * @param default the caller-supplied default used when the key is unknown (§4.2)
     */
    fun resolve(flag: FeatureFlag?, bucketingId: String, default: Boolean = false): Boolean {
        // Unknown key → caller default (§4.2). Never throws.
        if (flag == null) return default

        // 1. Kill switch.
        if (!flag.enabled) return false

        // Missing / non-numeric percentage → OFF (§4.1).
        val pct = flag.rolloutPercentage ?: return false

        // 2 & 3. Short-circuits that also absorb out-of-range values.
        if (pct >= 100) return true
        if (pct <= 0) return false

        // 4. Bucket check.
        return isInRollout(flag.key, bucketingId, pct)
    }
}
