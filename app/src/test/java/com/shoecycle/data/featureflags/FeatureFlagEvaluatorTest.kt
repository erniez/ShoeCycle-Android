package com.shoecycle.data.featureflags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the precedence rules in spec §4.1 / §4.2, independent of the fixture.
 */
class FeatureFlagEvaluatorTest {

    private val id = "a1b2c3d4-0000-4000-8000-000000000001"

    @Test
    fun `kill switch forces OFF regardless of rollout`() {
        val flag = FeatureFlag(key = "k", enabled = false, rolloutPercentage = 100)
        assertFalse(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `rollout 100 is ON for everyone enabled`() {
        val flag = FeatureFlag(key = "k", enabled = true, rolloutPercentage = 100)
        assertTrue(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `rollout 0 is OFF for everyone`() {
        val flag = FeatureFlag(key = "k", enabled = true, rolloutPercentage = 0)
        assertFalse(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `out of range percentages are absorbed by the comparisons`() {
        assertTrue(FeatureFlagEvaluator.resolve(FeatureFlag("k", true, 150), id))
        assertFalse(FeatureFlagEvaluator.resolve(FeatureFlag("k", true, -5), id))
    }

    @Test
    fun `missing percentage resolves OFF and does not crash`() {
        val flag = FeatureFlag(key = "k", enabled = true, rolloutPercentage = null)
        assertFalse(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `unknown flag returns caller default`() {
        assertFalse(FeatureFlagEvaluator.resolve(null, id, default = false))
        assertTrue(FeatureFlagEvaluator.resolve(null, id, default = true))
    }

    @Test
    fun `bucket check matches strict less than rule`() {
        // translation-rollout for this id buckets to 12 (per the oracle).
        val bucket = FeatureFlagEvaluator.bucketFor("translation-rollout", id).bucket
        assertEquals(12, bucket)
        // At rollout = 12, bucket 12 is NOT included (strict <).
        assertFalse(FeatureFlagEvaluator.resolve(FeatureFlag("translation-rollout", true, 12), id))
        // At rollout = 13, bucket 12 IS included.
        assertTrue(FeatureFlagEvaluator.resolve(FeatureFlag("translation-rollout", true, 13), id))
    }
}
