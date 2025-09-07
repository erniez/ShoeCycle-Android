package com.shoecycle.domain

import android.content.Context
import com.shoecycle.BuildConfig
import com.shoecycle.domain.analytics.AnalyticsLogger
import com.shoecycle.domain.analytics.ConsoleAnalyticsLogger
import com.shoecycle.domain.analytics.FirebaseAnalyticsLogger
import com.shoecycle.domain.analytics.MockAnalyticsLogger
import com.shoecycle.domain.services.HealthService
import com.shoecycle.domain.services.MockHealthService
import com.shoecycle.domain.services.RealHealthConnectService
import com.shoecycle.domain.services.TestHealthService

/**
 * Service locator for providing appropriate service implementations based on build configuration and runtime settings.
 * Supports switching between production, mock, and test implementations.
 */
object ServiceLocator {
    /**
     * Service mode enum defining available implementation types.
     */
    enum class ServiceMode {
        PRODUCTION,  // Real Health Connect implementation
        MOCK,        // Mock implementation for development
        TEST         // Test implementation for unit tests
    }
    
    private var currentMode: ServiceMode? = null
    private var applicationContext: Context? = null
    
    /**
     * Initializes the ServiceLocator with application context.
     * Should be called once during app initialization.
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * Gets the current service mode.
     * - Returns the explicitly set mode if setServiceMode() was called
     * - Otherwise defaults to MOCK for debug builds, PRODUCTION for release builds
     * - TEST mode must be explicitly set via setServiceMode()
     */
    val mode: ServiceMode
        get() = currentMode ?: getDefaultMode()
    
    /**
     * Gets the default mode based on build configuration.
     */
    private fun getDefaultMode(): ServiceMode {
        return if (BuildConfig.USE_MOCK_SERVICES) ServiceMode.MOCK else ServiceMode.PRODUCTION
    }
    
    /**
     * Sets the service mode for runtime switching.
     * Useful for UI testing and development settings.
     */
    fun setServiceMode(newMode: ServiceMode) {
        currentMode = newMode
    }
    
    /**
     * Resets the service mode to default based on build configuration.
     */
    fun resetServiceMode() {
        currentMode = null
        testHealthService = null
        testAnalyticsLogger = null
    }
    
    // Test-specific health service override
    private var testHealthService: HealthService? = null
    
    /**
     * Sets a specific HealthService for testing purposes.
     * This overrides the normal service selection logic.
     */
    fun setTestHealthService(service: HealthService) {
        testHealthService = service
    }
    
    // Test-specific analytics logger override
    private var testAnalyticsLogger: AnalyticsLogger? = null
    
    /**
     * Sets a specific AnalyticsLogger for testing purposes.
     * This overrides the normal service selection logic.
     */
    fun setTestAnalyticsLogger(logger: AnalyticsLogger) {
        testAnalyticsLogger = logger
    }
    
    /**
     * Provides the appropriate HealthService implementation based on current mode.
     * @param context Optional context parameter, uses application context if not provided
     * @return HealthService implementation
     */
    fun provideHealthService(context: Context? = null): HealthService {
        // Return test service if set (for unit testing)
        testHealthService?.let { return it }
        
        val ctx = context ?: applicationContext
            ?: throw IllegalStateException("ServiceLocator not initialized. Call initialize() first.")
        
        return when (mode) {
            ServiceMode.PRODUCTION -> RealHealthConnectService(ctx)
            ServiceMode.MOCK -> MockHealthService()
            ServiceMode.TEST -> TestHealthService()
        }
    }
    
    /**
     * Provides the appropriate AnalyticsLogger implementation based on current mode.
     * @return AnalyticsLogger implementation
     */
    fun provideAnalyticsLogger(): AnalyticsLogger {
        // Return test logger if set (for unit testing)
        testAnalyticsLogger?.let { return it }
        
        return when (mode) {
            ServiceMode.PRODUCTION -> {
                if (BuildConfig.USE_MOCK_SERVICES) {
                    // Use console logger when mocks are enabled
                    ConsoleAnalyticsLogger()
                } else {
                    // Use Firebase when mocks are disabled (both debug and release)
                    val ctx = applicationContext
                        ?: throw IllegalStateException("ServiceLocator not initialized. Call initialize() first.")
                    FirebaseAnalyticsLogger(ctx)
                }
            }
            ServiceMode.MOCK -> ConsoleAnalyticsLogger()
            ServiceMode.TEST -> MockAnalyticsLogger()
        }
    }
    
    /**
     * Checks if the service locator has been initialized.
     */
    fun isInitialized(): Boolean = applicationContext != null
}