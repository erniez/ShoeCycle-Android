package com.shoecycle.data.featureflags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Kill-switch fail-CLOSED regression tests (ShoeCycle-Web-rae).
 *
 * The master kill switch `enabled` MUST fail safe to OFF for any null / absent / type-mismatched
 * value in a served or cached payload — the opposite would silently expose a killed feature to
 * everyone. Before the fix, `enabled: Boolean = true` + `coerceInputValues = true` coerced a
 * `"enabled": null` back to the permissive default `true`, failing the switch OPEN.
 *
 * These tests feed hostile JSON through:
 *  - the raw [FeatureFlagJson] deserializer (the actual bug surface), and
 *  - the [FeatureFlagRepository] cache path (`decodeFromString` on stored JSON, then evaluation),
 * and assert every hostile shape degrades to safe-OFF without crashing.
 */
@RunWith(RobolectricTestRunner::class)
class FeatureFlagKillSwitchTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storeFile: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cachedFlagsKey = stringPreferencesKey(FeatureFlagPreferenceKeys.CACHED_FLAGS_JSON)
    private val id = "a1b2c3d4-0000-4000-8000-000000000001"

    @Before
    fun setUp() {
        storeFile = File(
            RuntimeEnvironment.getApplication().filesDir,
            "feature_flags_killswitch_test_${System.nanoTime()}.preferences_pb"
        )
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { storeFile }
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    private fun decodeSingleFlag(flagJson: String): FeatureFlag {
        val envelope = """{"flags":[$flagJson]}"""
        return FeatureFlagJson
            .decodeFromString(FeatureFlagsResponse.serializer(), envelope)
            .flags
            .single()
    }

    // ---- Raw deserializer: the bug surface ---------------------------------------------------

    @Test
    fun `enabled null decodes to false not true`() {
        // The exact reported bug: a null kill switch must NOT coerce to the permissive default.
        val flag = decodeSingleFlag("""{"key":"k","enabled":null,"rolloutPercentage":100}""")
        assertFalse("enabled:null must fail closed", flag.enabled)
        assertFalse("resolved value must be OFF", FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `absent enabled decodes to false`() {
        val flag = decodeSingleFlag("""{"key":"k","rolloutPercentage":100}""")
        assertFalse("absent enabled must default to OFF", flag.enabled)
        assertFalse(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `type-mismatched enabled values all fail closed`() {
        // Every hostile, wrong-typed shape must resolve OFF — never crash, never fail open.
        val hostile = listOf(
            """{"key":"k","enabled":1,"rolloutPercentage":100}""",          // number
            """{"key":"k","enabled":0,"rolloutPercentage":100}""",          // number
            """{"key":"k","enabled":"true","rolloutPercentage":100}""",     // quoted string
            """{"key":"k","enabled":"yes","rolloutPercentage":100}""",      // arbitrary string
            """{"key":"k","enabled":[],"rolloutPercentage":100}""",         // array
            """{"key":"k","enabled":{},"rolloutPercentage":100}""",         // object
        )
        for (json in hostile) {
            val flag = decodeSingleFlag(json)
            assertFalse("type-mismatched enabled must fail closed: $json", flag.enabled)
            assertFalse("must resolve OFF: $json", FeatureFlagEvaluator.resolve(flag, id))
        }
    }

    @Test
    fun `a genuine boolean true still decodes to true`() {
        // The fix must not weaken the happy path: a real ON flag stays ON.
        val flag = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":100}""")
        assertTrue(flag.enabled)
        assertTrue(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `a genuine boolean false decodes to false`() {
        val flag = decodeSingleFlag("""{"key":"k","enabled":false,"rolloutPercentage":100}""")
        assertFalse(flag.enabled)
        assertFalse(FeatureFlagEvaluator.resolve(flag, id))
    }

    @Test
    fun `rolloutPercentage handling is not weakened by the enabled fix`() {
        // A malformed rolloutPercentage still fails safe to OFF (regression guard for AC1's
        // "do NOT weaken the rolloutPercentage handling").
        val nullPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":null}""")
        assertFalse(FeatureFlagEvaluator.resolve(nullPct, id))
        val stringPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":"50"}""")
        assertFalse(FeatureFlagEvaluator.resolve(stringPct, id))
        // A valid enabled+percentage combination still resolves ON.
        val onFlag = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":100}""")
        assertTrue(FeatureFlagEvaluator.resolve(onFlag, id))
    }

    // ---- Through the repository (cache decode + evaluation) ----------------------------------

    private suspend fun seedCache(flagsJson: String) {
        dataStore.edit { prefs ->
            prefs[cachedFlagsKey] = """{"flags":[$flagsJson]}"""
        }
    }

    @Test
    fun `repository degrades a cached null enabled to safe-OFF`() = runTest {
        // A poisoned cache entry (enabled:null) must resolve OFF through the repository, not ON.
        seedCache("""{"key":"kill-me","enabled":null,"rolloutPercentage":100}""")
        val repo = FeatureFlagRepository(dataStore, mock())

        val cached = repo.readCachedFlags().single()
        assertEquals("kill-me", cached.key)
        assertFalse("cached enabled must fail closed", cached.enabled)
        assertFalse(FeatureFlagEvaluator.resolve(cached, repo.bucketingId(null)))
    }

    @Test
    fun `repository degrades a cached type-mismatched enabled to safe-OFF`() = runTest {
        seedCache("""{"key":"kill-me","enabled":"true","rolloutPercentage":100}""")
        val repo = FeatureFlagRepository(dataStore, mock())

        val cached = repo.readCachedFlags().single()
        assertFalse(cached.enabled)
        assertFalse(FeatureFlagEvaluator.resolve(cached, repo.bucketingId(null)))
    }

    @Test
    fun `repository refresh over a hostile served payload resolves OFF`() = runTest {
        // Simulate the service having parsed a hostile payload: enabled comes back false-safe.
        val hostileDefinition = decodeSingleFlag(
            """{"key":"kill-me","enabled":null,"rolloutPercentage":100}"""
        )
        val service = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doReturn FeatureFlagsResponse(listOf(hostileDefinition))
        }
        val repo = FeatureFlagRepository(dataStore, service)

        assertFalse("kill switch must stay OFF end-to-end", repo.isEnabled("kill-me"))
    }
}
