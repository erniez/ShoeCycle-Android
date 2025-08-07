package com.shoecycle.ui.screens.shoe_detail

import android.util.Log
import androidx.compose.runtime.MutableState
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ShoeDetailState(
    val shoe: Shoe? = null,
    val editedShoe: Shoe? = null,
    val hasUnsavedChanges: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val errorMessage: String? = null,
    val distanceUnit: String = "mi",
    val isCreateMode: Boolean = false,
    val onShoeSaved: (() -> Unit)? = null
)

class ShoeDetailInteractor(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val distanceUtility: DistanceUtility,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class ViewAppeared(val shoeId: Long) : Action()
        object InitializeNewShoe : Action()
        object Refresh : Action()
        data class UpdateShoeName(val name: String) : Action()
        data class UpdateStartDistance(val distance: String) : Action()
        data class UpdateMaxDistance(val distance: String) : Action()
        data class UpdateStartDate(val date: java.util.Date) : Action()
        data class UpdateEndDate(val date: java.util.Date) : Action()
        object SaveChanges : Action()
        object RequestNavigateBack : Action()
        object CancelCreate : Action()
        data class UpdateShoeImage(val imageKey: String, val thumbnailData: ByteArray) : Action() {            
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as UpdateShoeImage
                return imageKey == other.imageKey && thumbnailData.contentEquals(other.thumbnailData)
            }
            
            override fun hashCode(): Int {
                var result = imageKey.hashCode()
                result = 31 * result + thumbnailData.contentHashCode()
                return result
            }
        }
    }
    
    fun handle(state: MutableState<ShoeDetailState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> loadShoe(state, action.shoeId)
            is Action.InitializeNewShoe -> initializeNewShoe(state)
            is Action.Refresh -> {
                state.value.shoe?.let { shoe ->
                    loadShoe(state, shoe.id)
                }
            }
            is Action.UpdateShoeName -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(brand = action.name)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.UpdateStartDistance -> {
                val currentEdited = state.value.editedShoe ?: return
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = currentEdited.copy(startDistance = distance)
                        state.value = state.value.copy(
                            editedShoe = updatedShoe,
                            hasUnsavedChanges = updatedShoe != state.value.shoe
                        )
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            is Action.UpdateMaxDistance -> {
                val currentEdited = state.value.editedShoe ?: return
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = currentEdited.copy(maxDistance = distance)
                        state.value = state.value.copy(
                            editedShoe = updatedShoe,
                            hasUnsavedChanges = updatedShoe != state.value.shoe
                        )
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            is Action.UpdateStartDate -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(startDate = action.date)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.UpdateEndDate -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(expirationDate = action.date)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.SaveChanges -> {
                val editedShoe = state.value.editedShoe ?: return
                
                // Validate shoe name
                if (editedShoe.brand.isBlank()) {
                    state.value = state.value.copy(
                        errorMessage = "Please enter a shoe name"
                    )
                    return
                }
                
                state.value = state.value.copy(isSaving = true, errorMessage = null)
                scope.launch {
                    try {
                        if (state.value.isCreateMode) {
                            // Create new shoe
                            val insertedId = shoeRepository.insertShoe(editedShoe)
                            Log.d("ShoeDetailInteractor", "Successfully created shoe with ID: $insertedId")
                            
                            // Call the saved callback if provided
                            state.value.onShoeSaved?.invoke()
                            
                            state.value = state.value.copy(
                                isSaving = false,
                                shouldNavigateBack = true
                            )
                        } else {
                            // Update existing shoe
                            shoeRepository.updateShoe(editedShoe)
                            
                            // Recalculate total distance in case start distance changed
                            shoeRepository.recalculateShoeTotal(editedShoe.id)
                            
                            // Get the updated shoe with recalculated total
                            val updatedShoe = shoeRepository.getShoeByIdOnce(editedShoe.id)
                            
                            state.value = state.value.copy(
                                shoe = updatedShoe,
                                editedShoe = updatedShoe,
                                hasUnsavedChanges = false,
                                isSaving = false
                            )
                        }
                    } catch (e: Exception) {
                        state.value = state.value.copy(
                            isSaving = false,
                            errorMessage = if (state.value.isCreateMode) {
                                "Error creating shoe: ${e.message}"
                            } else {
                                "Error saving changes: ${e.message}"
                            }
                        )
                    }
                }
            }
            is Action.RequestNavigateBack -> {
                if (state.value.hasUnsavedChanges && !state.value.isSaving) {
                    // Set flag to navigate after save, then save
                    state.value = state.value.copy(shouldNavigateBack = true, isSaving = true)
                    val editedShoe = state.value.editedShoe ?: return
                    scope.launch {
                        try {
                            // Update the shoe details
                            shoeRepository.updateShoe(editedShoe)
                            
                            // Recalculate total distance in case start distance changed
                            shoeRepository.recalculateShoeTotal(editedShoe.id)
                            
                            // Get the updated shoe with recalculated total
                            val updatedShoe = shoeRepository.getShoeByIdOnce(editedShoe.id)
                            
                            state.value = state.value.copy(
                                shoe = updatedShoe,
                                editedShoe = updatedShoe,
                                hasUnsavedChanges = false,
                                isSaving = false
                                // shouldNavigateBack remains true for UI to handle
                            )
                        } catch (e: Exception) {
                            state.value = state.value.copy(
                                isSaving = false,
                                shouldNavigateBack = false,
                                errorMessage = "Error saving changes: ${e.message}"
                            )
                        }
                    }
                } else if (!state.value.isSaving) {
                    // No unsaved changes, can navigate immediately
                    state.value = state.value.copy(shouldNavigateBack = true)
                }
            }
            is Action.CancelCreate -> {
                // In create mode, simply dismiss without saving
                state.value = state.value.copy(shouldNavigateBack = true)
            }
            is Action.UpdateShoeImage -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(
                    imageKey = action.imageKey,
                    thumbnailData = action.thumbnailData
                )
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
        }
    }
    
    private fun loadShoe(state: MutableState<ShoeDetailState>, shoeId: Long) {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        
        scope.launch {
            try {
                shoeRepository.getShoeById(shoeId).collect { shoe ->
                    if (shoe != null) {
                        val unitLabel = distanceUtility.getUnitLabel()
                        
                        state.value = state.value.copy(
                            shoe = shoe,
                            editedShoe = shoe,
                            hasUnsavedChanges = false,
                            isLoading = false,
                            errorMessage = null,
                            distanceUnit = unitLabel
                        )
                    } else {
                        state.value = state.value.copy(
                            isLoading = false,
                            errorMessage = "Shoe not found"
                        )
                    }
                }
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading shoe: ${e.message}"
                )
            }
        }
    }
    
    private fun initializeNewShoe(state: MutableState<ShoeDetailState>) {
        scope.launch {
            try {
                val unitLabel = distanceUtility.getUnitLabel()
                val nextOrderingValue = shoeRepository.getNextOrderingValue()
                val newShoe = Shoe.createDefault().copy(orderingValue = nextOrderingValue)
                
                state.value = state.value.copy(
                    shoe = newShoe,
                    editedShoe = newShoe,
                    hasUnsavedChanges = false,
                    isLoading = false,
                    distanceUnit = unitLabel,
                    isCreateMode = true
                )
            } catch (e: Exception) {
                Log.e("ShoeDetailInteractor", "Error initializing new shoe", e)
                // Fallback to default if there's an error
                val unitLabel = distanceUtility.getUnitLabel()
                val newShoe = Shoe.createDefault()
                
                state.value = state.value.copy(
                    shoe = newShoe,
                    editedShoe = newShoe,
                    hasUnsavedChanges = false,
                    isLoading = false,
                    distanceUnit = unitLabel,
                    isCreateMode = true
                )
            }
        }
    }
}