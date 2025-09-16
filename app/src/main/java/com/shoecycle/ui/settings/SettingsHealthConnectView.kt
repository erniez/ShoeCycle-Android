package com.shoecycle.ui.settings

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
import android.app.Activity
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
        // If granted is empty, the dialog never showed (Health Connect not installed or other issue)
        if (granted.isEmpty()) {
            // Try opening Health Connect directly
            val activity = context as? Activity
            if (activity != null) {
                HealthConnectPermissionLauncher.openHealthConnectApp(activity)
            }
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        } else if (granted.containsAll(permissions)) {
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
        } else {
            // Open Health Connect settings to grant the missing permissions
            directPermissionLauncher()
            interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        }
    }
    
    // Initialize and check permissions on first composition
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsHealthConnectInteractor.Action.ViewAppeared)
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
                    interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
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
                    
                    // Show permission status
                    when (state.value.permissionStatus) {
                        SettingsHealthConnectState.PermissionStatus.Granted -> {
                            Text(
                                text = "✓ Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        SettingsHealthConnectState.PermissionStatus.Denied -> {
                            Text(
                                text = "⚠ Permission required",
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
                        if (state.value.permissionStatus == 
                            SettingsHealthConnectState.PermissionStatus.Denied) {
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                TextButton(
                                    onClick = {
                                        interactor.handle(
                                            state,
                                            SettingsHealthConnectInteractor.Action.RetryPermission
                                        )
                                    }
                                ) {
                                    Text("Grant Permission")
                                }

                                TextButton(
                                    onClick = {
                                        // Use direct permission launcher
                                        directPermissionLauncher()
                                    }
                                ) {
                                    Text("Open Settings")
                                }
                            }
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