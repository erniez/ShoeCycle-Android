package com.shoecycle.ui.settings

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.health.connect.client.HealthConnectClient
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.ServiceLocator
import com.shoecycle.domain.analytics.AnalyticsKeys
import com.shoecycle.domain.analytics.AnalyticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsHealthConnectState(
    val isEnabled: Boolean = false,
    val isCheckingPermissions: Boolean = false,
    val permissionStatus: PermissionStatus = PermissionStatus.Unknown,
    val showPermissionDialog: Boolean = false,
    val errorMessage: String? = null
) {
    enum class PermissionStatus {
        Unknown,
        Granted,
        Denied,
        NotAvailable
    }
}

class SettingsHealthConnectInteractor(
    private val context: Context,
    private val repository: UserSettingsRepository,
    private val analytics: AnalyticsLogger = ServiceLocator.provideAnalyticsLogger(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "SettingsHealthConnect"
    }
    
    sealed class Action {
        object ViewAppeared : Action()
        data class ToggleChanged(val enabled: Boolean) : Action()
        object PermissionGranted : Action()
        object PermissionDenied : Action()
        object DismissError : Action()
        object RetryPermission : Action()
    }
    
    fun handle(state: MutableState<SettingsHealthConnectState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                checkCurrentPermissionStatus(state)
            }
            
            is Action.ToggleChanged -> {
                if (action.enabled) {
                    // User is trying to enable - check if we need permissions
                    checkAndRequestPermissions(state)
                } else {
                    // User is disabling - just update the setting
                    state.value = state.value.copy(
                        isEnabled = false,
                        permissionStatus = SettingsHealthConnectState.PermissionStatus.Unknown
                    )
                    scope.launch {
                        repository.updateHealthConnectEnabled(false)
                    }
                }
            }
            
            is Action.PermissionGranted -> {
                Log.d(TAG, "Health Connect permissions granted")
                state.value = state.value.copy(
                    isEnabled = true,
                    isCheckingPermissions = false,
                    permissionStatus = SettingsHealthConnectState.PermissionStatus.Granted,
                    showPermissionDialog = false,
                    errorMessage = null
                )
                scope.launch {
                    repository.updateHealthConnectEnabled(true)
                }
            }
            
            is Action.PermissionDenied -> {
                Log.d(TAG, "Health Connect permissions denied")
                state.value = state.value.copy(
                    isEnabled = false,
                    isCheckingPermissions = false,
                    permissionStatus = SettingsHealthConnectState.PermissionStatus.Denied,
                    showPermissionDialog = false,
                    errorMessage = "Health Connect permissions are required to sync your runs. You can grant permissions in Settings."
                )
                scope.launch {
                    repository.updateHealthConnectEnabled(false)
                }
            }
            
            is Action.DismissError -> {
                state.value = state.value.copy(errorMessage = null)
            }
            
            is Action.RetryPermission -> {
                checkAndRequestPermissions(state)
            }
        }
    }
    
    private fun checkCurrentPermissionStatus(state: MutableState<SettingsHealthConnectState>) {
        scope.launch {
            try {
                state.value = state.value.copy(isCheckingPermissions = true)
                
                // Initialize service locator if needed
                if (!ServiceLocator.isInitialized()) {
                    ServiceLocator.initialize(context)
                }
                
                // Check if we're using mock service (debug mode)
                val isMockMode = ServiceLocator.mode == ServiceLocator.ServiceMode.MOCK
                
                if (!isMockMode) {
                    // Only check Health Connect availability for real service
                    val availabilityStatus = HealthConnectClient.getSdkStatus(context)
                    if (availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
                        Log.e(TAG, "Health Connect is not available on this device")
                        state.value = state.value.copy(
                            isEnabled = false,
                            isCheckingPermissions = false,
                            permissionStatus = SettingsHealthConnectState.PermissionStatus.NotAvailable,
                            errorMessage = "Health Connect is not available on this device. Please install the Health Connect app from the Play Store."
                        )
                        repository.updateHealthConnectEnabled(false)
                        return@launch
                    }
                }
                
                // Check if we have permissions
                val healthService = ServiceLocator.provideHealthService(context)
                val isAuthorized = healthService.isAuthorized()
                
                // Get current enabled state from repository
                val currentSettings = repository.userSettingsFlow.first()
                val currentEnabled = currentSettings.healthConnectEnabled
                
                state.value = state.value.copy(
                    isEnabled = currentEnabled && isAuthorized,
                    isCheckingPermissions = false,
                    permissionStatus = if (isAuthorized) {
                        SettingsHealthConnectState.PermissionStatus.Granted
                    } else {
                        SettingsHealthConnectState.PermissionStatus.Unknown
                    }
                )
                
                // If setting is enabled but permissions were revoked, disable it
                if (currentEnabled && !isAuthorized) {
                    repository.updateHealthConnectEnabled(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permission status", e)
                state.value = state.value.copy(
                    isCheckingPermissions = false,
                    errorMessage = "Error checking Health Connect status"
                )
            }
        }
    }
    
    private fun checkAndRequestPermissions(state: MutableState<SettingsHealthConnectState>) {
        scope.launch {
            try {
                state.value = state.value.copy(isCheckingPermissions = true)
                
                // Initialize service locator if needed
                if (!ServiceLocator.isInitialized()) {
                    ServiceLocator.initialize(context)
                }
                
                // Check if we're using mock service (debug mode)
                val isMockMode = ServiceLocator.mode == ServiceLocator.ServiceMode.MOCK
                
                if (!isMockMode) {
                    // Only check Health Connect availability for real service
                    val availabilityStatus = HealthConnectClient.getSdkStatus(context)
                    if (availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
                        state.value = state.value.copy(
                            isEnabled = false,
                            isCheckingPermissions = false,
                            permissionStatus = SettingsHealthConnectState.PermissionStatus.NotAvailable,
                            errorMessage = "Health Connect is not available. Please install it from the Play Store."
                        )
                        return@launch
                    }
                }
                
                // Check current permissions
                val healthService = ServiceLocator.provideHealthService(context)
                
                if (healthService.isAuthorized()) {
                    // Already have permissions
                    handle(state, Action.PermissionGranted)
                } else {
                    // Need to request permissions - show the permission dialog trigger
                    state.value = state.value.copy(
                        isCheckingPermissions = false,
                        showPermissionDialog = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting permissions", e)
                state.value = state.value.copy(
                    isEnabled = false,
                    isCheckingPermissions = false,
                    errorMessage = "Error connecting to Health Connect"
                )
            }
        }
    }
}