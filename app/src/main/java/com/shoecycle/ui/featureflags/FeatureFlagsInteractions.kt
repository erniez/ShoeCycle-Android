package com.shoecycle.ui.featureflags

import android.util.Log
import androidx.compose.runtime.MutableState
import com.shoecycle.data.featureflags.FeatureFlag
import com.shoecycle.data.featureflags.FeatureFlagEvaluator
import com.shoecycle.data.featureflags.FeatureFlagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
}

/**
 * Feature state for the flags system (VSI State role). Carries RESOLVED booleans only — views
 * observe these and never see raw definitions or the bucketing id. Immutable data class.
 */
data class FeatureFlagsState(
    val isLoading: Boolean = true,
    /** Resolved flag values keyed by flag key. Absent key ⇒ treated as its default (false). */
    val resolvedFlags: Map<String, Boolean> = emptyMap()
) {
    /** Resolves a flag from already-computed state; unknown ⇒ [default]. Never touches network. */
    fun isEnabled(key: String, default: Boolean = false): Boolean =
        resolvedFlags[key] ?: default
}

/**
 * VSI interactor owning fetch + evaluation of feature flags. Views dispatch [Action]s and observe
 * [FeatureFlagsState]; they never call the repository directly (Android Rule 4).
 *
 * Owns its own [CoroutineScope] (default Dispatchers.IO) per the Android VSI conventions.
 */
class FeatureFlagsInteractor(
    private val repository: FeatureFlagRepository,
    /** Keys the app cares about resolving into state. */
    private val trackedKeys: List<String> = listOf(FeatureFlagKeys.NEW_HALL_OF_FAME),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "FeatureFlagsInteractor"
    }

    sealed class Action {
        /** Fired when a view first appears — loads (cache-first, refresh if stale) and evaluates. */
        object ViewAppeared : Action()
        /** Forces a network refresh and re-evaluation. */
        object Refresh : Action()
        /** Re-evaluates against the current identity (e.g. after login sets a Cognito sub). */
        data class IdentityChanged(val cognitoSub: String?) : Action()
    }

    private var cognitoSub: String? = null

    fun handle(state: MutableState<FeatureFlagsState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> evaluate(state, forceRefresh = false)
            is Action.Refresh -> evaluate(state, forceRefresh = true)
            is Action.IdentityChanged -> {
                cognitoSub = action.cognitoSub
                evaluate(state, forceRefresh = false)
            }
        }
    }

    private fun evaluate(state: MutableState<FeatureFlagsState>, forceRefresh: Boolean) {
        state.value = state.value.copy(isLoading = true)
        scope.launch {
            try {
                val flags: List<FeatureFlag> =
                    if (forceRefresh) repository.refresh() else repository.refreshIfNeeded()
                val id = repository.bucketingId(cognitoSub)
                val resolved = trackedKeys.associateWith { key ->
                    FeatureFlagEvaluator.resolve(flags.firstOrNull { it.key == key }, id, default = false)
                }
                state.value = FeatureFlagsState(isLoading = false, resolvedFlags = resolved)
            } catch (e: Exception) {
                // Never crash on flag evaluation — degrade to whatever is already resolved.
                Log.e(TAG, "Feature-flag evaluation failed", e)
                state.value = state.value.copy(isLoading = false)
            }
        }
    }
}
