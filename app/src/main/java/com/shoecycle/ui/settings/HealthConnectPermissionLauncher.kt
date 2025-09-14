package com.shoecycle.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient

/**
 * Provides different methods to request Health Connect permissions
 */
object HealthConnectPermissionLauncher {
    private const val TAG = "HealthConnectPermission"
    
    /**
     * Opens Health Connect app directly for permission management
     */
    fun openHealthConnectApp(activity: Activity) {
        val packageName = "com.google.android.apps.healthdata"
        
        try {
            // Try to open Health Connect app directly
            val intent = activity.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                Log.d(TAG, "Opening Health Connect app directly")
                activity.startActivity(intent)
            } else {
                // If Health Connect isn't installed, open Play Store
                openPlayStore(activity, packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Health Connect app", e)
            openPlayStore(activity, packageName)
        }
    }
    
    /**
     * Opens the Play Store to install/update Health Connect
     */
    private fun openPlayStore(activity: Activity, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store isn't available, open in browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            }
            activity.startActivity(intent)
        }
    }
    
    /**
     * Checks if Health Connect is installed
     */
    fun isHealthConnectInstalled(activity: Activity): Boolean {
        val packageName = "com.google.android.apps.healthdata"
        return try {
            activity.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Opens Health Connect settings directly
     * On Android 14+, Health Connect is in system settings
     */
    fun openHealthConnectSettings(activity: Activity) {
        try {
            // For Android 14+, open system Health Connect settings
            val intent = if (android.os.Build.VERSION.SDK_INT >= 34) {
                Intent().apply {
                    action = "android.settings.HEALTH_CONNECT_SETTINGS"
                }
            } else {
                // For older versions, try the Health Connect app action
                Intent().apply {
                    action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
                }
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Health Connect settings", e)
            // Fallback to opening system settings
            try {
                val fallbackIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                activity.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open system settings", e2)
            }
        }
    }
}

/**
 * Composable that provides a manual way to open Health Connect for permissions
 */
@Composable
fun rememberHealthConnectLauncher(
    onResult: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    
    val _launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // When returning from Health Connect, check permissions again
        onResult()
    }
    
    return remember {
        {
            val activity = context as? Activity
            if (activity != null) {
                if (HealthConnectPermissionLauncher.isHealthConnectInstalled(activity)) {
                    HealthConnectPermissionLauncher.openHealthConnectSettings(activity)
                } else {
                    HealthConnectPermissionLauncher.openHealthConnectApp(activity)
                }
            }
        }
    }
}