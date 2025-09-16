package com.shoecycle.ui.settings

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.ServiceLocator

@Composable
fun SettingsHealthConnectView(
    repository: UserSettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = remember { mutableStateOf(SettingsHealthConnectState()) }
    val interactor = remember { SettingsHealthConnectInteractor(context, repository) }
    
    // Use the direct permission launcher as a backup
    val directPermissionLauncher = rememberDirectHealthPermissionLauncher { granted ->
        if (granted) {
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
        } else {
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        }
    }

    // App settings launcher for when permissions are permanently denied
    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check permissions again when returning from settings
        interactor.handle(state, SettingsHealthConnectInteractor.Action.ViewAppeared)
    }
    
    // Permission launcher
    val permissions = remember {
        setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class)
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // Check if all permissions were granted
        if (granted.containsAll(permissions)) {
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
        } else {
            // Some or all permissions were denied
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        }
    }
    
    // Initialize and check permissions on first composition
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsHealthConnectInteractor.Action.ViewAppeared)
    }

    // Open app settings when requested
    LaunchedEffect(state.value.showAppSettings) {
        if (state.value.showAppSettings) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            appSettingsLauncher.launch(intent)
            // Reset the flag
            state.value = state.value.copy(showAppSettings = false)
        }
    }
    
    // Launch permission request when needed
    LaunchedEffect(state.value.showPermissionDialog) {
        if (state.value.showPermissionDialog) {
            // Check if we're in mock mode
            val isMockMode = ServiceLocator.mode == ServiceLocator.ServiceMode.MOCK

            if (isMockMode) {
                // In mock mode, simulate permission request with the mock service
                val healthService = ServiceLocator.provideHealthService(context)
                val result = healthService.requestAuthorization()
                if (result.isSuccess && result.getOrNull() == true) {
                    interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
                } else {
                    interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
                }
            } else {
                // Real mode: Launch actual permission dialog
                try {
                    permissionLauncher.launch(permissions)
                } catch (e: Exception) {
                    Log.e("SettingsHealthConnect", "Failed to launch permission dialog", e)
                    // If we can't launch the permission dialog, try the direct approach
                    directPermissionLauncher()
                }
            }
        }
    }
    
    Column(modifier = modifier) {
        // Health Connect Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Connect",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Sync your runs with Health Connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show status based on switch state
                    if (state.value.isEnabled) {
                        Text(
                            text = "✓ Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else when (state.value.permissionStatus) {
                        SettingsHealthConnectState.PermissionStatus.Denied -> {
                            Text(
                                text = "⚠ Permission required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        SettingsHealthConnectState.PermissionStatus.PermanentlyDenied -> {
                            Text(
                                text = "⚠ Permissions denied - check settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        SettingsHealthConnectState.PermissionStatus.NotAvailable -> {
                            Text(
                                text = "⚠ Not available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        else -> {}
                    }
                }
                
                if (state.value.isCheckingPermissions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Switch(
                        checked = state.value.isEnabled,
                        onCheckedChange = { enabled ->
                            interactor.handle(
                                state, 
                                SettingsHealthConnectInteractor.Action.ToggleChanged(enabled)
                            )
                        },
                        enabled = state.value.permissionStatus != 
                            SettingsHealthConnectState.PermissionStatus.NotAvailable
                    )
                }
            }
        }
        
        // Error message
        state.value.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        // Retry button for permission denied
                        when (state.value.permissionStatus) {
                            SettingsHealthConnectState.PermissionStatus.Denied -> {
                                TextButton(
                                    onClick = {
                                        interactor.handle(
                                            state,
                                            SettingsHealthConnectInteractor.Action.RetryPermission
                                        )
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                            SettingsHealthConnectState.PermissionStatus.PermanentlyDenied -> {
                                TextButton(
                                    onClick = {
                                        interactor.handle(
                                            state,
                                            SettingsHealthConnectInteractor.Action.OpenAppSettings
                                        )
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Open App Settings")
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    // Dismiss button
                    IconButton(
                        onClick = {
                            interactor.handle(
                                state,
                                SettingsHealthConnectInteractor.Action.DismissError
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("×", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}