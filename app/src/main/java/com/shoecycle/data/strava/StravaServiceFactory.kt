package com.shoecycle.data.strava

import com.shoecycle.BuildConfig
import kotlin.random.Random

/**
 * Factory for creating StravaService instances.
 * Returns mock implementation in debug builds, real implementation in release.
 */
object StravaServiceFactory {
    
    /**
     * Creates the appropriate StravaService based on build configuration.
     * In debug builds with USE_MOCK_SERVICES enabled, returns a mock service.
     * Otherwise returns the real Strava API service.
     */
    fun create(tokenKeeper: StravaTokenKeeper): StravaService {
        return if (BuildConfig.DEBUG && BuildConfig.USE_MOCK_SERVICES) {
            // Use mock service only when explicitly enabled
            MockStravaServiceImpl(
                config = MockStravaServiceImpl.MockConfig(
                    networkDelayMs = 1500,
                    failureMode = null, // No specific failure mode by default
                    enableLogging = true
                )
            )
        } else {
            // Use real service in release builds or when mocks are disabled
            StravaServiceImpl(tokenKeeper)
        }
    }
    
    /**
     * Creates a mock service with specific configuration for testing.
     * Only available in debug builds.
     */
    fun createMockService(
        networkDelayMs: Long = 1500,
        failureMode: MockStravaServiceImpl.FailureMode? = null,
        enableLogging: Boolean = true
    ): StravaService {
        require(BuildConfig.DEBUG) { "Mock service is only available in debug builds" }
        
        return MockStravaServiceImpl(
            config = MockStravaServiceImpl.MockConfig(
                networkDelayMs = networkDelayMs,
                failureMode = failureMode,
                enableLogging = enableLogging
            )
        )
    }
    
    /**
     * Preset configurations for common testing scenarios
     */
    object TestConfigurations {
        /**
         * Always succeeds with minimal delay
         */
        fun alwaysSuccessful() = createMockService(
            networkDelayMs = 100,
            failureMode = null
        )
        
        /**
         * Always fails with unauthorized error
         */
        fun alwaysUnauthorized() = createMockService(
            failureMode = MockStravaServiceImpl.FailureMode.ALWAYS_UNAUTHORIZED,
            networkDelayMs = 500
        )
        
        /**
         * Always fails with network error
         */
        fun alwaysNetworkError() = createMockService(
            failureMode = MockStravaServiceImpl.FailureMode.ALWAYS_NETWORK_ERROR,
            networkDelayMs = 2000
        )
        
        /**
         * Simulates random errors
         */
        fun randomErrors() = createMockService(
            failureMode = MockStravaServiceImpl.FailureMode.RANDOM_ERRORS,
            networkDelayMs = Random.nextLong(500, 3000)
        )
        
        /**
         * Simulates slow network that succeeds
         */
        fun slowButReliable() = createMockService(
            networkDelayMs = 5000,
            failureMode = null
        )
    }
}