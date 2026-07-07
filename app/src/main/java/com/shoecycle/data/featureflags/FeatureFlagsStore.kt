package com.shoecycle.data.featureflags

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Well-known flag keys the app gates behind. Kept as named constants (no magic strings).
 */
object FeatureFlagKeys {
    /**
     * Trivial, reversible demo flag used to prove the toggle system end-to-end (acceptance
     * criterion: gate one reversible UI element). Off by default when the flag is absent.
     */
    const val NEW_HALL_OF_FAME = "new-hall-of-fame"

    /**
     * Every key the app resolves at load time. Add a new key here so [FeatureFlagsStore] tracks
     * it — an untracked key always falls back to the caller's `default` in [FeatureFlagsState.isEnabled],
     * exactly as if the server didn't define it.
     */
    val ALL_KEYS: List<String> = listOf(NEW_HALL_OF_FAME)
}

/**
 * Resolved feature-flag values only. Views observe this and read [isEnabled] — a trivial map
 * lookup, never the raw definitions or the bucketing hash (the evaluator runs once per load in
 * [FeatureFlagsStore], not per read).
 */
data class FeatureFlagsState(
    /** Resolved flag values keyed by flag key. Absent key ⇒ treated as its default (false). */
    val resolvedFlags: Map<String, Boolean> = emptyMap()
) {
    /** Resolves a flag from already-computed state; unknown ⇒ [default]. Never touches network. */
    fun isEnabled(key: String, default: Boolean = false): Boolean =
        resolvedFlags[key] ?: default
}

/**
 * App-level owner of feature-flag state (ShoeCycle-Web-tk6). Flags are global config, not a
 * per-screen concern: this store loads once at launch and refreshes on a background timer
 * thereafter, so every gated screen shares one fetch, one cache, one bucketing identity, and one
 * resolved answer per key within a session — two screens can never disagree about the same flag.
 *
 * Provided as a process-wide singleton by [com.shoecycle.domain.ServiceLocator] and started from
 * `MainActivity` (the Android analog of iOS's app-root `.environmentObject` + launch load). A
 * gated composable collects [state] (or reads [isEnabled]); it never owns a repository or a fetch.
 *
 * State is exposed as a [StateFlow] rather than a Compose `State` so the data layer stays free of
 * UI dependencies and so background updates are safe from any dispatcher — Compose observes via
 * `collectAsState()`, which delivers on the main thread. That also sidesteps the off-main-thread
 * mutation hazard the previous per-screen interactor had.
 */
class FeatureFlagsStore(
    private val repository: FeatureFlagRepository,
    private val trackedKeys: List<String> = FeatureFlagKeys.ALL_KEYS,
    /** How often definitions are re-fetched once the app is running. The interval is the real
     * staleness bound; a stale-but-present cache is still served as the offline fallback (§4.3). */
    private val refreshIntervalMillis: Long = FeatureFlagConstants.CACHE_TTL_MILLIS,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "FeatureFlagsStore"
    }

    private val _state = MutableStateFlow(FeatureFlagsState())
    val state: StateFlow<FeatureFlagsState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    /**
     * Seeds resolved state instantly from the last-good cache, then network-refreshes, then keeps
     * refreshing every [refreshIntervalMillis] for the life of the app. Idempotent — safe to call
     * from an Activity `onCreate` that can run more than once (config changes, process restart).
     */
    fun start() {
        if (refreshJob != null) return
        refreshJob = scope.launch {
            // Instant, offline-safe seed so gated UI resolves without waiting on the network.
            publish(repository.readCachedFlags())
            while (isActive) {
                publish(repository.refresh())
                delay(refreshIntervalMillis)
            }
        }
    }

    /** Cancels the periodic refresh loop. Exposed for tests; production keeps the store for the
     * app's lifetime. */
    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /** Resolve a tracked flag key to its already-computed boolean. See [FeatureFlagsState.isEnabled]. */
    fun isEnabled(key: String, default: Boolean = false): Boolean =
        _state.value.isEnabled(key, default)

    /** Evaluates every tracked key ONCE against [flags] + the current bucketing id and publishes. */
    private suspend fun publish(flags: List<FeatureFlag>) {
        try {
            val id = repository.bucketingId(null)
            _state.value = FeatureFlagsState(
                resolvedFlags = trackedKeys.associateWith { key ->
                    FeatureFlagEvaluator.resolve(flags.firstOrNull { it.key == key }, id, default = false)
                }
            )
        } catch (e: Exception) {
            // Never let flag resolution crash the refresh loop — keep the last-good state.
            Log.e(TAG, "Feature-flag resolution failed", e)
        }
    }
}
