package com.shoecycle.domain.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase implementation of the AnalyticsLogger interface.
 * Sends analytics events to Firebase Analytics for production tracking.
 * 
 * Note: Firebase is automatically initialized by the Firebase SDK when the app starts
 * (via google-services plugin), similar to iOS where FirebaseApp.configure() is called once.
 * This logger simply uses the already-initialized Firebase Analytics instance.
 */
class FirebaseAnalyticsLogger(private val context: Context) : AnalyticsLogger {
    
    companion object {
        private const val TAG = "FirebaseAnalyticsLogger"
    }
    
    override fun initialize() {
        // Firebase is automatically initialized by the google-services plugin at app startup.
        // This is equivalent to iOS calling FirebaseApp.configure() once in the app delegate.
        try {
            // First check if Firebase is initialized
            val firebaseApp = FirebaseApp.getInstance()
            Log.d(TAG, "FirebaseApp found: ${firebaseApp.name}")
            Log.d(TAG, "FirebaseApp options: ${firebaseApp.options.applicationId}")
            
            // Now get Analytics
            val analytics = Firebase.analytics
            
            // Set collection enabled explicitly
            analytics.setAnalyticsCollectionEnabled(true)
            
            // Set a test user property to verify Firebase is working
            analytics.setUserProperty("app_platform", "android")

            Log.d(TAG, "FirebaseAnalyticsLogger ready - Firebase Analytics collection enabled")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Firebase NOT initialized! Check google-services.json", e)
            Log.e(TAG, "Make sure google-services.json is in app/ directory")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Analytics", e)
        }
    }
    
    override fun logEvent(name: String, parameters: Map<String, Any>?) {
        try {
            // First verify Firebase is initialized
            try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Cannot log event '$name' - Firebase not initialized!", e)
                return
            }
            
            // Get the Firebase Analytics instance (same singleton every time)
            val analytics = Firebase.analytics
            
            // Convert event name to Firebase-compatible format
            val firebaseEventName = sanitizeEventName(name)
            
            // Build parameters bundle
            val bundle = Bundle()
            parameters?.forEach { (key, value) ->
                val sanitizedKey = sanitizeParameterKey(key)
                when (value) {
                    is String -> bundle.putString(sanitizedKey, value)
                    is Long -> bundle.putLong(sanitizedKey, value)
                    is Double -> bundle.putDouble(sanitizedKey, value)
                    is Int -> bundle.putLong(sanitizedKey, value.toLong())
                    is Float -> bundle.putDouble(sanitizedKey, value.toDouble())
                    is Boolean -> bundle.putLong(sanitizedKey, if (value) 1L else 0L)
                    else -> bundle.putString(sanitizedKey, value.toString())
                }
            }
            
            // Log the event
            analytics.logEvent(firebaseEventName, bundle)
            
            // Always log for debugging (not just in debug builds)
            val paramsString = parameters?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
            Log.d(TAG, "Firebase event sent: $name -> $firebaseEventName")
            Log.d(TAG, "  Original name: $name")
            Log.d(TAG, "  Sanitized name: $firebaseEventName")
            Log.d(TAG, "  Parameters: ${if (paramsString.isNotEmpty()) paramsString else "(none)"}")
            Log.d(TAG, "  Bundle size: ${bundle.size()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $name", e)
        }
    }
    
    /**
     * Sanitizes event names to be Firebase-compatible.
     * Firebase event names must:
     * - Be 40 characters or less
     * - Start with a letter
     * - Contain only alphanumeric characters and underscores
     */
    private fun sanitizeEventName(name: String): String {
        return name
            .take(40) // Limit to 40 characters
            .replace(Regex("[^a-zA-Z0-9_]"), "_") // Replace invalid characters with underscore
            .let { 
                // Ensure it starts with a letter
                if (it.isNotEmpty() && !it[0].isLetter()) {
                    "event_$it"
                } else {
                    it
                }
            }
    }
    
    /**
     * Sanitizes parameter keys to be Firebase-compatible.
     * Firebase parameter keys must:
     * - Be 40 characters or less
     * - Start with a letter
     * - Contain only alphanumeric characters and underscores
     */
    private fun sanitizeParameterKey(key: String): String {
        return key
            .take(40) // Limit to 40 characters
            .replace(Regex("[^a-zA-Z0-9_]"), "_") // Replace invalid characters with underscore
            .let { 
                // Ensure it starts with a letter
                if (it.isNotEmpty() && !it[0].isLetter()) {
                    "param_$it"
                } else {
                    it
                }
            }
    }
}