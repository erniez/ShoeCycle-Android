package com.shoecycle.domain.analytics

/**
 * Analytics logger interface following the facade pattern.
 * Provides a unified interface for different analytics implementations.
 */
interface AnalyticsLogger {
    /**
     * Initializes the analytics service.
     * Should be called once during app startup.
     */
    fun initialize()
    
    /**
     * Logs an analytics event with optional parameters.
     * 
     * @param name The event name (use constants from AnalyticsKeys.Event)
     * @param parameters Optional map of event parameters (use constants from AnalyticsKeys.Param for keys)
     */
    fun logEvent(name: String, parameters: Map<String, Any>? = null)
}