package com.shoecycle.domain.analytics

import android.util.Log

/**
 * Debug implementation of AnalyticsLogger that outputs events to the console.
 * Used in debug builds to monitor analytics events without external dependencies.
 */
class ConsoleAnalyticsLogger : AnalyticsLogger {
    
    companion object {
        private const val TAG = "ShoeCycle"
        private const val ANALYTICS_PREFIX = "*** Analytics"
    }
    
    override fun initialize() {
        Log.d(TAG, "$ANALYTICS_PREFIX initialized (Console Logger)")
    }
    
    override fun logEvent(name: String, parameters: Map<String, Any>?) {
        Log.d(TAG, "$ANALYTICS_PREFIX Event: $name")
        parameters?.forEach { (key, value) ->
            Log.d(TAG, "$ANALYTICS_PREFIX   $key: $value")
        }
    }
}