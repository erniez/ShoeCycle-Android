package com.shoecycle.data.featureflags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException

/**
 * Repository tests: caching, offline/stale fallback, default fallback, and anonymous-UUID
 * persistence + lowercase-canonical form. Uses a real file-backed DataStore (Robolectric provides
 * the Android context) with a Mockito-mocked [FeatureFlagsService].
 */
@RunWith(RobolectricTestRunner::class)
class FeatureFlagRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storeFile: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val translationFlag = FeatureFlag(key = "translation-rollout", enabled = true, rolloutPercentage = 100)
    private val idForRollout = "a1b2c3d4-0000-4000-8000-000000000001"

    @Before
    fun setUp() {
        storeFile = File(
            RuntimeEnvironment.getApplication().filesDir,
            "feature_flags_test_${System.nanoTime()}.preferences_pb"
        )
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { storeFile }
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun `refresh caches the fetched definitions`() = runTest {
        val service = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doReturn FeatureFlagsResponse(listOf(translationFlag))
        }
        val repo = FeatureFlagRepository(dataStore, service)

        val flags = repo.refresh()

        assertEquals(1, flags.size)
        assertEquals("translation-rollout", flags[0].key)
        // Cached and readable without another fetch.
        assertEquals(1, repo.readCachedFlags().size)
    }

    @Test
    fun `offline fetch falls back to last cached definitions`() = runTest {
        // First: a good fetch populates the cache.
        val goodService = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doReturn FeatureFlagsResponse(listOf(translationFlag))
        }
        FeatureFlagRepository(dataStore, goodService).refresh()

        // Then: an offline service degrades to the cache rather than crashing.
        // doAnswer{throw} avoids Mockito's checked-exception validation on the suspend stub.
        val offlineService = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doAnswer { throw IOException("offline") }
        }
        val repo = FeatureFlagRepository(dataStore, offlineService)

        val flags = repo.refresh()
        assertEquals(1, flags.size)
        assertEquals("translation-rollout", flags[0].key)
    }

    @Test
    fun `offline with no cache degrades to caller default and does not crash`() = runTest {
        val offlineService = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doAnswer { throw IOException("offline") }
        }
        val repo = FeatureFlagRepository(dataStore, offlineService)

        // Unknown flag with an empty cache resolves to the caller-supplied default (§4.2 / §4.3).
        assertFalse(repo.isEnabled("some-flag", default = false))
        assertTrue(repo.isEnabled("some-flag", default = true))
    }

    @Test
    fun `isEnabled resolves a cached ON flag`() = runTest {
        val service = mock<FeatureFlagsService> {
            onBlocking { fetchFlags() } doReturn FeatureFlagsResponse(listOf(translationFlag))
        }
        val repo = FeatureFlagRepository(dataStore, service)

        assertTrue(repo.isEnabled("translation-rollout"))
    }

    @Test
    fun `cognito sub is used verbatim as the bucketing id`() = runTest {
        val repo = FeatureFlagRepository(dataStore, mock())
        val sub = "cognito-sub-9f8e7d6c5b4a"
        assertEquals(sub, repo.bucketingId(sub))
    }

    @Test
    fun `anonymous id is generated persisted and stable across reads`() = runTest {
        val repo = FeatureFlagRepository(dataStore, mock())

        val first = repo.bucketingId(cognitoSub = null)
        val second = repo.bucketingId(cognitoSub = null)

        assertNotNull(first)
        // Same id on every read — no cohort flicker across launches.
        assertEquals(first, second)

        // A fresh repository over the SAME DataStore reads the same persisted id.
        val repo2 = FeatureFlagRepository(dataStore, mock())
        assertEquals(first, repo2.bucketingId(cognitoSub = null))
    }

    @Test
    fun `anonymous id is lowercase canonical form`() = runTest {
        val repo = FeatureFlagRepository(dataStore, mock())
        val anonId = repo.bucketingId(cognitoSub = null)

        // Lowercase — a wrongly uppercased id would bucket differently from Web (spec §3.1).
        assertEquals(anonId.lowercase(), anonId)
        // Canonical 8-4-4-4-12 hyphenated form (36 chars).
        assertEquals(36, anonId.length)
        assertTrue(
            "Not canonical UUID form: $anonId",
            anonId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
        )
    }

    @Test
    fun `blank cognito sub falls back to the anonymous id`() = runTest {
        val repo = FeatureFlagRepository(dataStore, mock())
        val anon = repo.bucketingId(cognitoSub = null)
        assertEquals(anon, repo.bucketingId(cognitoSub = ""))
        assertEquals(anon, repo.bucketingId(cognitoSub = "   "))
    }
}
