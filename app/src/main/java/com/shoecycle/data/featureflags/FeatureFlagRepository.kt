package com.shoecycle.data.featureflags

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

/**
 * Owns fetching, caching, and evaluating feature-flag definitions.
 *
 * Responsibilities (acceptance criteria 1–3):
 *  - Fetches the PUBLIC serve endpoint (no auth) via [FeatureFlagsService].
 *  - Caches the last good response in DataStore Preferences for offline / stale fallback (§4.3).
 *  - Resolves the stable bucketing identity: Cognito sub if present, else a persisted anonymous
 *    UUID that survives restarts (§3.1). The anon UUID is lowercase-canonical — Kotlin's
 *    UUID.randomUUID().toString() is already lowercase, so it is persisted verbatim.
 *
 * The evaluation math itself lives in the pure [FeatureFlagEvaluator]; this class is the data +
 * identity layer around it. The [dataStore] is injected so it can be exercised with a real
 * DataStore in Robolectric tests.
 */
class FeatureFlagRepository(
    private val dataStore: DataStore<Preferences>,
    private val service: FeatureFlagsService = FeatureFlagsServiceImpl(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    companion object {
        private const val TAG = "FeatureFlagRepository"

        private val ANON_ID_KEY = stringPreferencesKey(FeatureFlagPreferenceKeys.ANON_ID)
        private val CACHED_FLAGS_KEY = stringPreferencesKey(FeatureFlagPreferenceKeys.CACHED_FLAGS_JSON)
        private val CACHE_TIMESTAMP_KEY = longPreferencesKey(FeatureFlagPreferenceKeys.CACHE_TIMESTAMP)
    }

    /**
     * Serializes anon-UUID generation (ShoeCycle-Web-tk6). DataStore serializes individual
     * `edit`/`data` operations, but NOT a read-then-write sequence — two concurrent first-launch
     * callers could both observe "no id yet", generate different UUIDs, and race to persist,
     * leaving the session bucketed under an id that isn't the one persisted (a one-time cohort
     * flip on the next launch). This lock makes the check-then-write atomic. It is per-repository-
     * instance, which is sufficient because the app holds exactly one repository via the shared
     * [FeatureFlagsStore].
     */
    private val anonIdMutex = Mutex()

    /**
     * Refreshes definitions from the network when the cache is missing or older than the TTL, and
     * caches any successful response. Returns the freshest definitions available, degrading to the
     * cache (then to empty) on failure. Never throws (§4.3).
     */
    suspend fun refreshIfNeeded(): List<FeatureFlag> {
        val cachedTimestamp = readCacheTimestamp()
        val cacheAge = clock() - cachedTimestamp
        val cacheIsFresh = cachedTimestamp > 0 && cacheAge < FeatureFlagConstants.CACHE_TTL_MILLIS
        if (cacheIsFresh) {
            return readCachedFlags()
        }
        return refresh()
    }

    /**
     * Forces a network fetch and caches the result. On any network/parse failure, degrades to the
     * last cached definitions (then empty). Never throws.
     */
    suspend fun refresh(): List<FeatureFlag> {
        return try {
            val response = service.fetchFlags()
            cacheFlags(response)
            response.flags
        } catch (e: CancellationException) {
            // Never swallow cancellation: it must propagate so a caller's scope (e.g. the store's
            // stop()) can actually tear this coroutine down instead of degrading to cache.
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Feature-flag refresh failed; falling back to cache", e)
            readCachedFlags()
        } catch (e: Exception) {
            // Parse errors etc. also degrade to cache rather than crash (§4.3).
            Log.e(TAG, "Feature-flag refresh error; falling back to cache", e)
            readCachedFlags()
        }
    }

    /** Returns the last successfully-cached definitions, or empty if there is no cache. */
    suspend fun readCachedFlags(): List<FeatureFlag> {
        return try {
            val json = dataStore.data.first()[CACHED_FLAGS_KEY] ?: return emptyList()
            FeatureFlagJson.decodeFromString(FeatureFlagsResponse.serializer(), json).flags
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached feature flags", e)
            emptyList()
        }
    }

    private suspend fun readCacheTimestamp(): Long {
        return try {
            dataStore.data.first()[CACHE_TIMESTAMP_KEY] ?: 0L
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun cacheFlags(response: FeatureFlagsResponse) {
        try {
            val json = FeatureFlagJson.encodeToString(FeatureFlagsResponse.serializer(), response)
            dataStore.edit { prefs ->
                prefs[CACHED_FLAGS_KEY] = json
                prefs[CACHE_TIMESTAMP_KEY] = clock()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to cache feature flags", e)
        }
    }

    /**
     * Resolves the stable bucketing identity (spec §3.1):
     *  1. Cognito sub verbatim, if signed in.
     *  2. Else a persisted anonymous UUID, generated once and reused across launches.
     *
     * An evaluator must never hash an empty/absent id, so when no sub is available we
     * generate-and-persist an anon UUID synchronously before returning it. Kotlin's
     * `UUID.randomUUID().toString()` is already lowercase — persisted verbatim, never uppercased.
     */
    suspend fun bucketingId(cognitoSub: String?): String {
        if (!cognitoSub.isNullOrBlank()) {
            return cognitoSub
        }
        return getOrCreateAnonId()
    }

    private suspend fun getOrCreateAnonId(): String {
        // Fast path: an id is already persisted, no need to take the lock.
        readAnonId()?.let { return it }
        // Slow path: serialize generation so concurrent first-launch callers can't each mint a
        // different UUID (see [anonIdMutex]).
        return anonIdMutex.withLock {
            // Re-check inside the lock: a competing caller may have created it while we waited.
            readAnonId() ?: run {
                // Already lowercase canonical 8-4-4-4-12 form on Kotlin — persist verbatim (§3.1).
                val newId = UUID.randomUUID().toString()
                try {
                    dataStore.edit { prefs -> prefs[ANON_ID_KEY] = newId }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to persist anonymous feature-flag id", e)
                }
                newId
            }
        }
    }

    private suspend fun readAnonId(): String? = try {
        dataStore.data.first()[ANON_ID_KEY]
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /**
     * Convenience: resolve a single flag to a boolean, refreshing definitions if the cache is
     * stale, using the given identity + default. Never throws.
     */
    suspend fun isEnabled(
        flagKey: String,
        cognitoSub: String? = null,
        default: Boolean = false
    ): Boolean {
        val flags = refreshIfNeeded()
        val flag = flags.firstOrNull { it.key == flagKey }
        val id = bucketingId(cognitoSub)
        return FeatureFlagEvaluator.resolve(flag, id, default)
    }
}
