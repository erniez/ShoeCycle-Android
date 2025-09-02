package com.shoecycle.data.strava

import android.util.Log
import com.shoecycle.BuildConfig
import com.shoecycle.data.strava.models.StravaActivity
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock implementation of StravaService for debug builds.
 * Provides configurable responses for testing various scenarios.
 */
class MockStravaServiceImpl(
    private val config: MockConfig = MockConfig()
) : StravaService {
    
    companion object {
        private const val TAG = "MockStravaService"
        private const val DEFAULT_DELAY_MS = 1500L
    }
    
    /**
     * Configuration for mock behavior
     */
    data class MockConfig(
        val networkDelayMs: Long = DEFAULT_DELAY_MS,
        val failureMode: FailureMode? = null,
        val enableLogging: Boolean = BuildConfig.DEBUG
    )
    
    /**
     * Specific failure modes for testing
     */
    enum class FailureMode {
        ALWAYS_UNAUTHORIZED,
        ALWAYS_NETWORK_ERROR,
        ALWAYS_UNKNOWN_ERROR,
        RANDOM_ERRORS
    }
    
    override suspend fun sendActivity(activity: StravaActivity) {
        if (config.enableLogging) {
            Log.d(TAG, "Mock: Sending activity - Distance: ${activity.distance}m, " +
                    "Duration: ${activity.elapsedTime}s, Name: ${activity.name}")
        }
        
        // Simulate network delay
        delay(config.networkDelayMs)
        
        // If no failure mode is set, succeed
        if (config.failureMode == null) {
            if (config.enableLogging) {
                Log.d(TAG, "Mock: Activity uploaded successfully")
            }
            // Success - no exception thrown
            return
        }
        
        // Determine which error to throw
        val error = when (config.failureMode) {
            FailureMode.ALWAYS_UNAUTHORIZED -> {
                Log.e(TAG, "Mock: Simulating unauthorized error")
                StravaService.DomainError.Unauthorized
            }
            FailureMode.ALWAYS_NETWORK_ERROR -> {
                Log.e(TAG, "Mock: Simulating network error")
                StravaService.DomainError.Reachability
            }
            FailureMode.ALWAYS_UNKNOWN_ERROR -> {
                Log.e(TAG, "Mock: Simulating unknown error")
                StravaService.DomainError.Unknown
            }
            FailureMode.RANDOM_ERRORS -> {
                // Random error selection
                when (Random.nextInt(3)) {
                    0 -> {
                        Log.e(TAG, "Mock: Simulating unauthorized error")
                        StravaService.DomainError.Unauthorized
                    }
                    1 -> {
                        Log.e(TAG, "Mock: Simulating network error")
                        StravaService.DomainError.Reachability
                    }
                    else -> {
                        Log.e(TAG, "Mock: Simulating unknown error")
                        StravaService.DomainError.Unknown
                    }
                }
            }
        }
        
        throw error
    }
}