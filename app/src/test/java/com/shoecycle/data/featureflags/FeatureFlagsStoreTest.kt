package com.shoecycle.data.featureflags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for [FeatureFlagsStore]: start()'s re-entrancy guard, stop()'s cancellation, and that
 * isEnabled reflects loaded state (ShoeCycle-Web-tk6). The store is the new app-level owner
 * replacing the per-screen interactor; shipping it without lifecycle coverage would be the same
 * gap the iOS review flagged, so it is pinned here.
 */
@RunWith(RobolectricTestRunner::class)
class FeatureFlagsStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storeFile: File
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val demoBadgeOn = FeatureFlagsResponse(
        listOf(FeatureFlag(key = FeatureFlagKeys.NEW_HALL_OF_FAME, enabled = true, rolloutPercentage = 100))
    )

    @Before
    fun setUp() {
        storeFile = File(
            RuntimeEnvironment.getApplication().filesDir,
            "feature_flags_store_test_${System.nanoTime()}.preferences_pb"
        )
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { storeFile }
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        storeFile.delete()
    }

    private fun makeStore(service: FeatureFlagsService, refreshIntervalMillis: Long): FeatureFlagsStore =
        FeatureFlagsStore(
            repository = FeatureFlagRepository(dataStore, service),
            refreshIntervalMillis = refreshIntervalMillis,
            scope = storeScope
        )

    /** Polls until [condition] holds or the timeout elapses (real time, real threads). */
    private suspend fun awaitUntil(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - start < timeoutMs) {
            delay(10)
        }
    }

    @Test
    fun `start twice does not launch a second load`() = runBlocking {
        val service = CountingService(demoBadgeOn)
        val store = makeStore(service, refreshIntervalMillis = TimeUnit.HOURS.toMillis(1))

        store.start()
        store.start()

        awaitUntil { service.fetchCount.get() >= 1 }
        // A large interval means the loop fetches exactly once; the second start() must be a no-op.
        delay(100)
        assertEquals("start() called twice must not duplicate the initial fetch", 1, service.fetchCount.get())
    }

    @Test
    fun `stop cancels the periodic refresh loop`() = runBlocking {
        val service = CountingService(demoBadgeOn)
        val store = makeStore(service, refreshIntervalMillis = 50)

        store.start()
        awaitUntil { service.fetchCount.get() >= 1 }

        store.stop()
        delay(100)
        val baseline = service.fetchCount.get()
        // At a 50ms interval, an un-cancelled loop would fetch several more times in 300ms.
        delay(300)
        assertEquals("stop() must halt further scheduled refreshes", baseline, service.fetchCount.get())
    }

    @Test
    fun `isEnabled reflects the loaded flag state`() = runBlocking {
        val service = CountingService(demoBadgeOn)
        val store = makeStore(service, refreshIntervalMillis = TimeUnit.HOURS.toMillis(1))

        store.start()
        awaitUntil { store.isEnabled(FeatureFlagKeys.NEW_HALL_OF_FAME) }

        assertTrue(store.isEnabled(FeatureFlagKeys.NEW_HALL_OF_FAME))
    }

    /** A [FeatureFlagsService] that counts fetches so lifecycle tests can assert on call counts. */
    private class CountingService(private val response: FeatureFlagsResponse) : FeatureFlagsService {
        val fetchCount = AtomicInteger(0)
        override suspend fun fetchFlags(): FeatureFlagsResponse {
            fetchCount.incrementAndGet()
            return response
        }
    }
}
