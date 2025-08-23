package com.shoecycle.ui.screens.add_distance.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import com.shoecycle.domain.ServiceLocator
import com.shoecycle.domain.services.HealthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

data class DateDistanceEntryState(
    val runDate: Date = Date(),
    val runDistance: String = "",
    val showDatePicker: Boolean = false,
    val showFavoritesModal: Boolean = false,
    val showHistoryModal: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val stravaEnabled: Boolean = false,
    val healthConnectSyncStatus: SyncStatus = SyncStatus.Idle,
    val stravaSyncStatus: SyncStatus = SyncStatus.Idle
) {
    enum class SyncStatus {
        Idle,
        Syncing,
        Success,
        Failed
    }
}

class DateDistanceEntryInteractor(
    private val context: Context,
    private val healthService: HealthService = ServiceLocator.provideHealthService(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val currentShoeId: (() -> String?)? = null
) {
    companion object {
        private const val TAG = "DateDistanceEntryInteractor"
    }
    sealed class Action {
        data class DateChanged(val date: Date) : Action()
        data class DistanceChanged(val distance: String) : Action()
        object ShowDatePicker : Action()
        object HideDatePicker : Action()
        object ShowFavoritesModal : Action()
        object HideFavoritesModal : Action()
        object ShowHistoryModal : Action()
        object HideHistoryModal : Action()
        object AddRunClicked : Action()
        data class FavoriteDistanceSelected(val distance: Double) : Action()
        data class UpdateHealthConnectStatus(val enabled: Boolean) : Action()
        data class UpdateStravaStatus(val enabled: Boolean) : Action()
    }

    fun handle(
        state: MutableState<DateDistanceEntryState>,
        action: Action,
        onDateChanged: ((Date) -> Unit)? = null,
        onDistanceChanged: ((String) -> Unit)? = null,
        onAddRun: (() -> Unit)? = null,
        onShowFavorites: (() -> Unit)? = null,
        onShowHistory: (() -> Unit)? = null,
        onBounceRequested: (() -> Unit)? = null
    ) {
        when (action) {
            is Action.DateChanged -> {
                state.value = state.value.copy(runDate = action.date)
                onDateChanged?.invoke(action.date)
            }

            is Action.DistanceChanged -> {
                // Only allow numeric input with optional decimal
                val filteredDistance = action.distance.filter { it.isDigit() || it == '.' }
                // Ensure only one decimal point
                val decimalCount = filteredDistance.count { it == '.' }
                val finalDistance = if (decimalCount > 1) {
                    state.value.runDistance
                } else {
                    filteredDistance
                }
                state.value = state.value.copy(runDistance = finalDistance)
                onDistanceChanged?.invoke(finalDistance)
            }

            is Action.ShowDatePicker -> {
                state.value = state.value.copy(showDatePicker = true)
            }

            is Action.HideDatePicker -> {
                state.value = state.value.copy(showDatePicker = false)
            }

            is Action.ShowFavoritesModal -> {
                onShowFavorites?.invoke()
            }

            is Action.HideFavoritesModal -> {
                state.value = state.value.copy(showFavoritesModal = false)
            }

            is Action.ShowHistoryModal -> {
                onShowHistory?.invoke()
            }

            is Action.HideHistoryModal -> {
                state.value = state.value.copy(showHistoryModal = false)
            }

            is Action.AddRunClicked -> {
                val distance = state.value.runDistance.toDoubleOrNull()
                if (distance != null && distance > 0) {
                    // Trigger the parent's add run action
                    onAddRun?.invoke()
                    
                    // Sync with Health Connect if enabled
                    if (state.value.healthConnectEnabled) {
                        syncWithHealthConnect(
                            state = state,
                            date = state.value.runDate,
                            distance = distance
                        )
                    }
                    if (state.value.stravaEnabled) {
                        simulateStravaSync(state)
                    }
                    
                    // Request bounce animation immediately
                    onBounceRequested?.invoke()
                }
            }

            is Action.FavoriteDistanceSelected -> {
                state.value = state.value.copy(
                    runDistance = action.distance.toString(),
                    showFavoritesModal = false
                )
                onDistanceChanged?.invoke(action.distance.toString())
            }

            is Action.UpdateHealthConnectStatus -> {
                state.value = state.value.copy(healthConnectEnabled = action.enabled)
            }

            is Action.UpdateStravaStatus -> {
                state.value = state.value.copy(stravaEnabled = action.enabled)
            }
        }
    }

    private fun syncWithHealthConnect(
        state: MutableState<DateDistanceEntryState>,
        date: Date,
        distance: Double
    ) {
        scope.launch {
            Log.d(TAG, "Starting Health Connect sync for distance: $distance mi on date: $date")
            
            // Update UI to show syncing
            state.value = state.value.copy(
                healthConnectSyncStatus = DateDistanceEntryState.SyncStatus.Syncing
            )
            
            try {
                // Check if authorized first
                if (!healthService.isAuthorized()) {
                    Log.d(TAG, "Health Connect not authorized, requesting permissions")
                    val authResult = healthService.requestAuthorization()
                    if (authResult.isFailure) {
                        Log.e(TAG, "Failed to get Health Connect authorization")
                        state.value = state.value.copy(
                            healthConnectSyncStatus = DateDistanceEntryState.SyncStatus.Failed
                        )
                        delay(2000)
                        state.value = state.value.copy(
                            healthConnectSyncStatus = DateDistanceEntryState.SyncStatus.Idle
                        )
                        return@launch
                    }
                }
                
                // Add workout to Health Connect
                val result = healthService.addWorkout(
                    date = date,
                    distance = distance,
                    shoeId = currentShoeId?.invoke()
                )
                
                // Update UI based on result
                state.value = state.value.copy(
                    healthConnectSyncStatus = if (result.isSuccess) {
                        Log.d(TAG, "Successfully synced to Health Connect: ${result.getOrNull()?.workoutId}")
                        DateDistanceEntryState.SyncStatus.Success
                    } else {
                        Log.e(TAG, "Failed to sync to Health Connect", result.exceptionOrNull())
                        DateDistanceEntryState.SyncStatus.Failed
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Health Connect sync", e)
                state.value = state.value.copy(
                    healthConnectSyncStatus = DateDistanceEntryState.SyncStatus.Failed
                )
            }
            
            // Reset status after showing result
            delay(2000)
            state.value = state.value.copy(
                healthConnectSyncStatus = DateDistanceEntryState.SyncStatus.Idle
            )
        }
    }

    private fun simulateStravaSync(state: MutableState<DateDistanceEntryState>) {
        scope.launch {
            state.value = state.value.copy(
                stravaSyncStatus = DateDistanceEntryState.SyncStatus.Syncing
            )
            delay(1500)
            // Randomly succeed or fail for mock
            val success = kotlin.random.Random.nextBoolean()
            state.value = state.value.copy(
                stravaSyncStatus = if (success) {
                    DateDistanceEntryState.SyncStatus.Success
                } else {
                    DateDistanceEntryState.SyncStatus.Failed
                }
            )
            // Reset status after showing result
            delay(2000)
            state.value = state.value.copy(
                stravaSyncStatus = DateDistanceEntryState.SyncStatus.Idle
            )
        }
    }

    fun resetAfterAdd(state: MutableState<DateDistanceEntryState>) {
        state.value = state.value.copy(
            runDistance = "",
            runDate = Date()
        )
    }
}