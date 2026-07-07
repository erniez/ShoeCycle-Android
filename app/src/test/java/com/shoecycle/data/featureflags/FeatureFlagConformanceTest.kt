package com.shoecycle.data.featureflags

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Conformance test for the Kotlin bucketing port. Loads the FROZEN oracle fixture
 * (feature-toggles.vectors.json, vendored byte-identical from
 * architecture/feature-toggles.vectors.json) and asserts this Kotlin evaluator reproduces the
 * hash / bucket / inRollout for every row — it does NOT hand-transcribe the markdown table.
 *
 * If iOS, Web, Node, and this port all pass the same fixture, the rollout decisions are provably
 * identical across the fleet (spec §5).
 *
 * Two of the six vectors (e9b6b98f, 9298ff9e) are deliberate signedness canaries: their
 * hashUint32 has the high bit set, so a naive signed-Int read + `% 100` would yield the wrong
 * (negative) bucket. This test would fail loudly if the port reintroduced that bug.
 */
class FeatureFlagConformanceTest {

    @Serializable
    private data class Fixture(
        val schemaVersion: Int,
        val vectors: List<Vector>
    )

    @Serializable
    private data class Vector(
        val flagKey: String,
        val bucketingId: String,
        val bucketingInput: String,
        val hashHex8: String,
        val hashUint32: Long,
        val bucket: Int,
        val rolloutPercentage: Int,
        val inRollout: Boolean
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadFixture(): Fixture {
        val stream = javaClass.classLoader
            ?.getResourceAsStream("featureflags/feature-toggles.vectors.json")
            ?: error("Missing vendored fixture featureflags/feature-toggles.vectors.json")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(Fixture.serializer(), text)
    }

    @Test
    fun `fixture schema version is the expected version`() {
        // Fail loudly if the fixture shape changes out from under this loader (spec §5.2).
        assertEquals(1, loadFixture().schemaVersion)
    }

    @Test
    fun `all six vectors reproduce the frozen oracle hash bucket and inRollout`() {
        val fixture = loadFixture()
        assertEquals("Expected the frozen six-vector oracle", 6, fixture.vectors.size)

        for (v in fixture.vectors) {
            val result = FeatureFlagEvaluator.bucketFor(v.flagKey, v.bucketingId)

            assertEquals(
                "hashUint32 mismatch for ${v.bucketingInput}",
                v.hashUint32,
                result.hashUint32
            )
            assertEquals(
                "bucket mismatch for ${v.bucketingInput}",
                v.bucket,
                result.bucket
            )

            // Defensive range check required by spec §3.3 — every bucket MUST be in [0, 100).
            assertTrue(
                "bucket out of range for ${v.bucketingInput}: ${result.bucket}",
                result.bucket in 0 until 100
            )

            val inRollout = FeatureFlagEvaluator.isInRollout(
                v.flagKey,
                v.bucketingId,
                v.rolloutPercentage
            )
            assertEquals(
                "inRollout mismatch for ${v.bucketingInput}",
                v.inRollout,
                inRollout
            )

            // Also assert the hex rendering of the first 4 bytes matches, proving it is genuine
            // SHA-256 output and not merely internally consistent (spec §5).
            val hex = "%08x".format(result.hashUint32)
            assertEquals("hashHex8 mismatch for ${v.bucketingInput}", v.hashHex8, hex)
        }
    }

    @Test
    fun `signedness canaries bucket correctly and never go negative`() {
        val fixture = loadFixture()
        // The two high-bit vectors documented in spec §3.3.
        val canaries = fixture.vectors.filter { it.hashHex8 == "e9b6b98f" || it.hashHex8 == "9298ff9e" }
        assertEquals("Expected both signedness canaries present", 2, canaries.size)

        for (v in canaries) {
            val result = FeatureFlagEvaluator.bucketFor(v.flagKey, v.bucketingId)
            // Would be -13 / -50 under a signed read + modulo bug.
            assertTrue("Canary bucket went negative: ${result.bucket}", result.bucket >= 0)
            assertEquals("Canary bucket wrong for ${v.hashHex8}", v.bucket, result.bucket)
        }

        // Spot-check the exact documented values so a regression is unambiguous.
        assertEquals(83, FeatureFlagEvaluator.bucketFor("translation-rollout", "a1b2c3d4-0000-4000-8000-000000000002").bucket)
        assertEquals(46, FeatureFlagEvaluator.bucketFor("offline-sync", "cognito-sub-9f8e7d6c5b4a").bucket)
    }
}
