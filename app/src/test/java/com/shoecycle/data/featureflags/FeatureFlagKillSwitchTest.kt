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
import org.junit.Assert.assertNull
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
    fun `malformed rolloutPercentage decodes to null and fails closed (ShoeCycle-Web-54b)`() {
        // A quoted string is a TYPE MISMATCH and must decode to null → OFF. Use "100": a genuine
        // integer 100 resolves ON for EVERYONE regardless of bucket, so an OFF result here proves
        // the string was rejected — not that this id's bucket happened to exceed the threshold.
        // (The previous test used "50" and passed only because bucket("k", id) == 93 >= 50.)
        val stringPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":"100"}""")
        assertNull("quoted-string percentage must decode to null", stringPct.rolloutPercentage)
        assertFalse("string percentage must fail closed to OFF", FeatureFlagEvaluator.resolve(stringPct, id))

        // A float is not an integer bucket bound → null → OFF (again "100.0" would be ON if kept).
        val floatPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":100.5}""")
        assertNull("float percentage must decode to null", floatPct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(floatPct, id))

        // Explicit null, object, and array shapes also decode to null → OFF.
        val nullPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":null}""")
        assertNull(nullPct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(nullPct, id))
        val objectPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":{}}""")
        assertNull(objectPct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(objectPct, id))
        val arrayPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":[]}""")
        assertNull(arrayPct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(arrayPct, id))

        // A genuine integer percentage is unaffected: still parsed, still resolves ON at 100.
        val onFlag = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":100}""")
        assertEquals(100, onFlag.rolloutPercentage)
        assertTrue(FeatureFlagEvaluator.resolve(onFlag, id))
    }

    @Test
    fun `genuine out-of-range integer percentages survive decode and fail closed via the evaluator`() {
        // These must decode to REAL integers (not null) so the evaluator's own <=0 short-circuit
        // resolves them OFF — a different, deliberate mechanism from the "decoded to null" path
        // above. Guards against a serializer regression that mis-handled sign or zero.
        val zeroPct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":0}""")
        assertEquals(0, zeroPct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(zeroPct, id))
        val negativePct = decodeSingleFlag("""{"key":"k","enabled":true,"rolloutPercentage":-5}""")
        assertEquals(-5, negativePct.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(negativePct, id))
    }

    // ---- Per-flag isolation: one bad definition must not poison the payload (54b) ------------

    private fun decodeFlags(flagsJson: String): List<FeatureFlag> =
        FeatureFlagJson
            .decodeFromString(FeatureFlagsResponse.serializer(), """{"flags":[$flagsJson]}""")
            .flags

    @Test
    fun `a structurally broken flag is dropped without dropping the healthy ones`() {
        // A definition with no key (the one required field) sits between two healthy 100% flags.
        val flags = decodeFlags(
            """{"key":"healthy-a","enabled":true,"rolloutPercentage":100},""" +
            """{"enabled":true,"rolloutPercentage":100},""" +               // missing key -> dropped
            """{"key":"healthy-b","enabled":true,"rolloutPercentage":100}"""
        )
        // Before the fix, the keyless element threw and discarded ALL flags. Now only it is dropped.
        assertEquals(listOf("healthy-a", "healthy-b"), flags.map { it.key })
        assertTrue(FeatureFlagEvaluator.resolve(flags.first { it.key == "healthy-a" }, id))
        assertTrue(FeatureFlagEvaluator.resolve(flags.first { it.key == "healthy-b" }, id))
    }

    @Test
    fun `non-object flag elements are dropped without poisoning the payload`() {
        val flags = decodeFlags(
            """{"key":"healthy","enabled":true,"rolloutPercentage":100},""" +
            """"garbage",""" +   // a bare string, not a flag object -> dropped
            """42"""             // a bare number -> dropped
        )
        assertEquals(listOf("healthy"), flags.map { it.key })
        assertTrue(FeatureFlagEvaluator.resolve(flags.single(), id))
    }

    @Test
    fun `field-level malformed values survive as a fail-closed flag, not a dropped one`() {
        // Both fields malformed but the key is present: the flag still decodes (so it is not
        // silently absent) and resolves OFF, without taking its healthy sibling down with it.
        val flags = decodeFlags(
            """{"key":"broken","enabled":"true","rolloutPercentage":"90"},""" +
            """{"key":"healthy","enabled":true,"rolloutPercentage":100}"""
        )
        assertEquals(listOf("broken", "healthy"), flags.map { it.key })
        val broken = flags.first { it.key == "broken" }
        assertFalse("string enabled fails closed", broken.enabled)
        assertNull("string percentage fails closed", broken.rolloutPercentage)
        assertFalse(FeatureFlagEvaluator.resolve(broken, id))
        assertTrue(FeatureFlagEvaluator.resolve(flags.first { it.key == "healthy" }, id))
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
