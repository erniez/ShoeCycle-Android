package com.shoecycle.ui.screens.add_distance

import androidx.compose.runtime.MutableState
import android.util.Log
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.data.strava.StravaService
import com.shoecycle.data.strava.models.StravaActivity
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.domain.ServiceLocator
import com.shoecycle.domain.analytics.AnalyticsKeys
import com.shoecycle.domain.analytics.AnalyticsLogger
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

data class AddDistanceState(
    val activeShoes: List<Shoe> = emptyList(),
    val selectedShoeIndex: Int = 0,
    val selectedShoe: Shoe? = null,
    val isLoadingShoes: Boolean = true,  // Start as true to prevent placeholder flash
    val runDate: Date = Date(),
    val runDistance: String = "",
    val isAddingRun: Boolean = false,
    val showHistoryModal: Boolean = false,
    val showFavoritesModal: Boolean = false,
    val lastAddedRunId: Long? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val stravaUploadState: StravaUploadState = StravaUploadState.Idle,
    val stravaUploadError: String? = null,
    val graphAllShoes: Boolean = false
)

enum class StravaUploadState {
    Idle,
    Uploading,
    Success,
    Failed
}

class AddDistanceInteractor(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val selectedShoeStrategy: SelectedShoeStrategy,
    private val stravaService: StravaService? = null,
    private val analytics: AnalyticsLogger = ServiceLocator.provideAnalyticsLogger(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        object SwipeUp : Action()
        object SwipeDown : Action()
        data class SelectShoeAtIndex(val index: Int) : Action()
        data class DateChanged(val date: Date) : Action()
        data class DistanceChanged(val distance: String) : Action()
        object AddRunClicked : Action()
        object ShowHistoryModal : Action()
        object HideHistoryModal : Action()
        object ShowFavoritesModal : Action()
        object HideFavoritesModal : Action()
        data class FavoriteDistanceSelected(val distance: Double) : Action()
        object BounceRequested : Action()
        data class UploadToStrava(val distance: Double, val date: Date, val shoeName: String) : Action()
        object ClearStravaUploadState : Action()
        data class UpdateShoeImage(val imageKey: String, val thumbnailData: ByteArray) : Action()
        data class GraphAllShoesToggled(val enabled: Boolean) : Action()
    }

    fun handle(state: MutableState<AddDistanceState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                loadActiveShoes(state)
                observeSettings(state)
            }
            
            is Action.SwipeUp -> {
                val currentIndex = state.value.selectedShoeIndex
                val newIndex = if (currentIndex < state.value.activeShoes.size - 1) {
                    currentIndex + 1
                } else {
                    0
                }
                selectShoeAtIndex(state, newIndex)
            }
            
            is Action.SwipeDown -> {
                val currentIndex = state.value.selectedShoeIndex
                val newIndex = if (currentIndex > 0) {
                    currentIndex - 1
                } else {
                    state.value.activeShoes.size - 1
                }
                selectShoeAtIndex(state, newIndex)
            }
            
            is Action.SelectShoeAtIndex -> {
                selectShoeAtIndex(state, action.index)
            }
            
            is Action.DateChanged -> {
                state.value = state.value.copy(runDate = action.date)
            }
            
            is Action.DistanceChanged -> {
                state.value = state.value.copy(runDistance = action.distance)
            }
            
            is Action.AddRunClicked -> {
                addRun(state)
            }
            
            is Action.ShowHistoryModal -> {
                state.value = state.value.copy(showHistoryModal = true)
            }
            
            is Action.HideHistoryModal -> {
                state.value = state.value.copy(showHistoryModal = false)
            }
            
            is Action.ShowFavoritesModal -> {
                state.value = state.value.copy(showFavoritesModal = true)
            }
            
            is Action.HideFavoritesModal -> {
                state.value = state.value.copy(showFavoritesModal = false)
            }
            
            is Action.FavoriteDistanceSelected -> {
                // Favorite distances are stored in miles, convert for display
                val displayDistance = DistanceUtility.displayString(action.distance, state.value.distanceUnit)
                state.value = state.value.copy(
                    runDistance = displayDistance,
                    showFavoritesModal = false
                )
            }
            
            is Action.BounceRequested -> {
                // This will be handled by the UI layer for animation
            }
            
            is Action.UploadToStrava -> {
                uploadToStrava(state, action.distance, action.date, action.shoeName)
            }
            
            is Action.ClearStravaUploadState -> {
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Idle,
                    stravaUploadError = null
                )
            }
            
            is Action.UpdateShoeImage -> {
                updateShoeImage(state, action.imageKey, action.thumbnailData)
            }

            is Action.GraphAllShoesToggled -> {
                state.value = state.value.copy(graphAllShoes = action.enabled)

                // Persist to settings
                scope.launch {
                    userSettingsRepository.updateGraphAllShoes(action.enabled)
                }
            }
        }
    }

    private fun observeSettings(state: MutableState<AddDistanceState>) {
        scope.launch {
            userSettingsRepository.userSettingsFlow.collect { settings ->
                state.value = state.value.copy(
                    distanceUnit = settings.distanceUnit,
                    graphAllShoes = settings.graphAllShoes
                )
            }
        }
    }
    
    private fun loadActiveShoes(state: MutableState<AddDistanceState>) {
        state.value = state.value.copy(isLoadingShoes = true)
        scope.launch {
            try {
                val activeShoes = shoeRepository.getActiveShoes().first()
                val selectedShoeId = userSettingsRepository.userSettingsFlow.first().selectedShoeId
                
                var selectedIndex = 0
                var selectedShoe: Shoe? = null
                
                if (selectedShoeId != null) {
                    val index = activeShoes.indexOfFirst { it.id == selectedShoeId }
                    if (index >= 0) {
                        selectedIndex = index
                        selectedShoe = activeShoes[index]
                    }
                } else if (activeShoes.isNotEmpty()) {
                    selectedShoe = activeShoes[0]
                }
                
                state.value = state.value.copy(
                    activeShoes = activeShoes,
                    selectedShoeIndex = selectedIndex,
                    selectedShoe = selectedShoe,
                    isLoadingShoes = false
                )
            } catch (e: Exception) {
                state.value = state.value.copy(isLoadingShoes = false)
            }
        }
    }

    private fun selectShoeAtIndex(state: MutableState<AddDistanceState>, index: Int) {
        val shoes = state.value.activeShoes
        if (index in shoes.indices) {
            val shoe = shoes[index]
            state.value = state.value.copy(
                selectedShoeIndex = index,
                selectedShoe = shoe
            )
            
            scope.launch {
                selectedShoeStrategy.selectShoe(shoe.id)
            }
        }
    }

    private fun addRun(state: MutableState<AddDistanceState>) {
        val shoe = state.value.selectedShoe ?: return
        val distanceStr = state.value.runDistance
        val enteredDistance = distanceStr.toDoubleOrNull() ?: return
        
        if (enteredDistance <= 0) return
        
        // Convert the entered distance to miles for storage
        // DistanceUtility.distance() converts from display unit to miles
        val distanceInMiles = DistanceUtility.distance(distanceStr, state.value.distanceUnit)
        
        state.value = state.value.copy(isAddingRun = true)
        
        scope.launch {
            try {
                val runId = historyRepository.addRun(
                    shoeId = shoe.id,
                    runDate = state.value.runDate,
                    runDistance = distanceInMiles
                )
                
                shoeRepository.recalculateShoeTotal(shoe.id)
                
                // Log distance added event
                analytics.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, mapOf(
                    AnalyticsKeys.Param.MILEAGE to distanceInMiles,
                    AnalyticsKeys.Param.DISTANCE_UNIT to state.value.distanceUnit.name,
                    AnalyticsKeys.Param.TOTAL_MILEAGE to (shoe.totalDistance + distanceInMiles)
                ))
                
                state.value = state.value.copy(
                    isAddingRun = false,
                    runDistance = "",
                    runDate = Date(),
                    lastAddedRunId = runId
                )
                
                loadActiveShoes(state)
            } catch (e: Exception) {
                state.value = state.value.copy(isAddingRun = false)
            }
        }
    }
    
    private fun uploadToStrava(
        state: MutableState<AddDistanceState>, 
        distanceInMiles: Double, 
        date: Date,
        shoeName: String
    ) {
        if (stravaService == null) return
        
        state.value = state.value.copy(
            stravaUploadState = StravaUploadState.Uploading,
            stravaUploadError = null
        )
        
        scope.launch {
            try {
                // Convert miles to meters for Strava API
                val distanceInMeters = distanceInMiles * 1609.344
                
                // Create activity with shoe name as the activity name
                val activity = StravaActivity.create(
                    name = "ShoeCycle Run - $shoeName",
                    distanceInMeters = distanceInMeters,
                    startDate = date
                )
                
                // Upload to Strava
                stravaService.sendActivity(activity)
                
                Log.d("AddDistanceInteractor", "Successfully uploaded to Strava: $distanceInMeters meters")
                
                // Log Strava mileage event (equivalent to iOS stravaEvent)
                analytics.logEvent(AnalyticsKeys.Event.STRAVA_EVENT, mapOf(
                    AnalyticsKeys.Param.MILEAGE to distanceInMiles
                ))
                
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Success
                )
                
                // Brief delay to give UI time to react to the state change
                kotlinx.coroutines.delay(100)
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Idle
                )
                
            } catch (e: StravaService.DomainError.Unauthorized) {
                Log.e("AddDistanceInteractor", "Strava upload failed: Unauthorized")
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Failed,
                    stravaUploadError = "Strava authentication expired. Please reconnect in Settings."
                )
            } catch (e: StravaService.DomainError.Reachability) {
                Log.e("AddDistanceInteractor", "Strava upload failed: Network error")
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Failed,
                    stravaUploadError = "Network error. Please check your connection."
                )
            } catch (e: Exception) {
                Log.e("AddDistanceInteractor", "Strava upload failed", e)
                state.value = state.value.copy(
                    stravaUploadState = StravaUploadState.Failed,
                    stravaUploadError = "Failed to upload to Strava: ${e.message}"
                )
            }
        }
    }
    
    private fun updateShoeImage(state: MutableState<AddDistanceState>, imageKey: String, thumbnailData: ByteArray) {
        val shoe = state.value.selectedShoe ?: return
        
        scope.launch {
            try {
                // Update the shoe with the new image
                val updatedShoe = shoe.copy(
                    imageKey = imageKey,
                    thumbnailData = thumbnailData
                )
                
                // Save the updated shoe
                shoeRepository.updateShoe(updatedShoe)
                
                // Log SHOE_PICTURE_ADDED event
                analytics.logEvent(AnalyticsKeys.Event.SHOE_PICTURE_ADDED, mapOf(
                    AnalyticsKeys.Param.SHOE_BRAND to updatedShoe.brand
                ))
                
                // Update the state with the updated shoe
                val updatedShoes = state.value.activeShoes.map { 
                    if (it.id == shoe.id) updatedShoe else it 
                }
                
                state.value = state.value.copy(
                    activeShoes = updatedShoes,
                    selectedShoe = updatedShoe
                )
                
                Log.d("AddDistanceInteractor", "Shoe image updated successfully")
            } catch (e: Exception) {
                Log.e("AddDistanceInteractor", "Failed to update shoe image", e)
            }
        }
    }
}