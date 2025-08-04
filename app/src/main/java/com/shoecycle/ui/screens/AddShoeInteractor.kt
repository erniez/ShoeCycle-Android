package com.shoecycle.ui.screens

import androidx.compose.runtime.MutableState
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

data class AddShoeState(
    val newShoe: Shoe = Shoe.createDefault(),
    val isSaving: Boolean = false,
    val shouldDismiss: Boolean = false,
    val errorMessage: String? = null,
    val distanceUnit: String = "mi"
)

class AddShoeInteractor(
    private val shoeRepository: IShoeRepository,
    private val distanceUtility: DistanceUtility,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object InitializeNewShoe : Action()
        data class UpdateShoeName(val name: String) : Action()
        data class UpdateStartDistance(val distance: String) : Action()
        data class UpdateMaxDistance(val distance: String) : Action()
        data class UpdateStartDate(val date: java.util.Date) : Action()
        data class UpdateEndDate(val date: java.util.Date) : Action()
        object SaveShoe : Action()
        object Cancel : Action()
    }
    
    fun handle(state: MutableState<AddShoeState>, action: Action) {
        when (action) {
            is Action.InitializeNewShoe -> {
                scope.launch {
                    try {
                        val unitLabel = distanceUtility.getUnitLabel()
                        val nextOrderingValue = shoeRepository.getNextOrderingValue()
                        val newShoe = Shoe.createDefault().copy(orderingValue = nextOrderingValue)
                        
                        state.value = AddShoeState(
                            newShoe = newShoe,
                            distanceUnit = unitLabel
                        )
                    } catch (e: Exception) {
                        Log.e("AddShoeInteractor", "Error initializing new shoe", e)
                        // Fallback to default if there's an error
                        val unitLabel = distanceUtility.getUnitLabel()
                        state.value = AddShoeState(
                            newShoe = Shoe.createDefault(),
                            distanceUnit = unitLabel
                        )
                    }
                }
            }
            
            is Action.UpdateShoeName -> {
                val updatedShoe = state.value.newShoe.copy(brand = action.name)
                state.value = state.value.copy(newShoe = updatedShoe)
            }
            
            is Action.UpdateStartDistance -> {
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = state.value.newShoe.copy(startDistance = distance)
                        state.value = state.value.copy(newShoe = updatedShoe)
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            
            is Action.UpdateMaxDistance -> {
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = state.value.newShoe.copy(maxDistance = distance)
                        state.value = state.value.copy(newShoe = updatedShoe)
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            
            is Action.UpdateStartDate -> {
                val updatedShoe = state.value.newShoe.copy(startDate = action.date)
                state.value = state.value.copy(newShoe = updatedShoe)
            }
            
            is Action.UpdateEndDate -> {
                val updatedShoe = state.value.newShoe.copy(expirationDate = action.date)
                state.value = state.value.copy(newShoe = updatedShoe)
            }
            
            is Action.SaveShoe -> {
                val shoe = state.value.newShoe
                
                // Validate shoe before saving
                if (shoe.brand.isBlank()) {
                    state.value = state.value.copy(
                        errorMessage = "Please enter a shoe name"
                    )
                    return
                }
                
                state.value = state.value.copy(isSaving = true, errorMessage = null)
                
                scope.launch {
                    try {
                        val insertedId = shoeRepository.insertShoe(shoe)
                        Log.d("AddShoeInteractor", "Successfully created shoe with ID: $insertedId")
                        
                        state.value = state.value.copy(
                            isSaving = false,
                            shouldDismiss = true
                        )
                    } catch (e: Exception) {
                        Log.e("AddShoeInteractor", "Error creating shoe", e)
                        state.value = state.value.copy(
                            isSaving = false,
                            errorMessage = "Error creating shoe: ${e.message}"
                        )
                    }
                }
            }
            
            is Action.Cancel -> {
                state.value = state.value.copy(shouldDismiss = true)
            }
        }
    }
}