package com.shoecycle.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import kotlinx.coroutines.launch

/**
 * Direct approach to request Health Connect permissions
 */
@Composable
fun rememberDirectHealthPermissionLauncher(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
    // Create a more explicit launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permissions after returning from settings
        scope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                val requiredPermissions = setOf(
                    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                    HealthPermission.getWritePermission(DistanceRecord::class)
                )
                val hasAllPermissions = granted.containsAll(requiredPermissions)
                onResult(hasAllPermissions)
            } catch (e: Exception) {
                Log.e("DirectHealthPermission", "Error checking permissions", e)
                onResult(false)
            }
        }
    }
    
    return {
        if (activity != null) {
            scope.launch {
                try {
                    // First, check if Health Connect is available
                    val sdkStatus = HealthConnectClient.getSdkStatus(context)

                    if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                        // Try the direct permission request first
                        val client = HealthConnectClient.getOrCreate(context)
                        val permissions = setOf(
                            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                            HealthPermission.getWritePermission(DistanceRecord::class)
                        )
                        
                        // Check if we already have permissions
                        val granted = client.permissionController.getGrantedPermissions()

                        if (!granted.containsAll(permissions)) {
                            // If we're missing WRITE_DISTANCE specifically, open settings directly
                            // because Health Connect doesn't always show the dialog for additional permissions
                            val missingDistance = !granted.contains(
                                HealthPermission.getWritePermission(DistanceRecord::class)
                            )

                            if (missingDistance && granted.isNotEmpty()) {
                                openHealthConnectSettingsDirect(activity)
                            } else {
                                // Try the permission intent for first-time permissions
                                val permissionContract = PermissionController.createRequestPermissionResultContract()
                                val intent = permissionContract.createIntent(context, permissions)
                                // Launch the intent
                                launcher.launch(intent)
                            }
                        } else {
                            onResult(true)
                        }
                    } else {
                        // Open settings as fallback
                        openHealthConnectSettingsDirect(activity)
                    }
                } catch (e: Exception) {
                    Log.e("DirectHealthPermission", "Error requesting permissions", e)
                    // Fallback to opening settings
                    openHealthConnectSettingsDirect(activity)
                }
            }
        }
    }
}

private fun openHealthConnectSettingsDirect(activity: Activity) {
    try {
        val intent = when {
            Build.VERSION.SDK_INT >= 34 -> {
                // Android 14+ - Health Connect in system settings
                Intent("android.settings.HEALTH_CONNECT_SETTINGS")
            }
            else -> {
                // Older versions - try ACTION_APPLICATION_DETAILS_SETTINGS for Health Connect
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:com.google.android.apps.healthdata")
                }
            }
        }
        activity.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DirectHealthPermission", "Failed to open Health Connect settings", e)
        // Last resort - open general settings
        try {
            activity.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (e2: Exception) {
            Log.e("DirectHealthPermission", "Failed to open settings", e2)
        }
    }
}