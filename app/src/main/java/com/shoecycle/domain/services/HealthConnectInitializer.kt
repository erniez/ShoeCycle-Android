package com.shoecycle.domain.services

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Initializes Health Connect integration for the app.
 * This ensures the app appears in Health Connect's app list.
 */
object HealthConnectInitializer {
    private const val TAG = "HealthConnectInit"
    private var isInitialized = false
    
    /**
     * Initializes Health Connect by checking availability and registering the app.
     * This should be called on app startup.
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Health Connect already initialized")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if Health Connect is available
                val availability = HealthConnectClient.getSdkStatus(context)
                Log.d(TAG, "Health Connect SDK status: $availability")
                
                when (availability) {
                    HealthConnectClient.SDK_AVAILABLE -> {
                        Log.d(TAG, "Health Connect is available")
                        
                        // Create a client instance to register the app
                        val client = HealthConnectClient.getOrCreate(context)
                        
                        // Try to get granted permissions - this registers the app with Health Connect
                        try {
                            val permissions = client.permissionController.getGrantedPermissions()
                            Log.d(TAG, "App registered with Health Connect. Current permissions: $permissions")
                        } catch (e: Exception) {
                            Log.d(TAG, "App registered with Health Connect (no permissions yet)")
                        }
                        
                        isInitialized = true
                    }
                    HealthConnectClient.SDK_UNAVAILABLE -> {
                        Log.w(TAG, "Health Connect SDK is not available on this device")
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                        Log.w(TAG, "Health Connect provider needs to be updated")
                    }
                    else -> {
                        Log.w(TAG, "Unknown Health Connect SDK status: $availability")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Health Connect", e)
            }
        }
    }
    
    /**
     * Checks if Health Connect is available on this device.
     */
    fun isAvailable(context: Context): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect availability", e)
            false
        }
    }
}