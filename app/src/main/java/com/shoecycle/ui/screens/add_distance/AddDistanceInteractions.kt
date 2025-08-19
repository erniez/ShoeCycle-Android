package com.shoecycle.ui.screens.add_distance

import androidx.compose.runtime.MutableState
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.SelectedShoeStrategy
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
    val isLoadingShoes: Boolean = false,
    val runDate: Date = Date(),
    val runDistance: String = "",
    val isAddingRun: Boolean = false,
    val showHistoryModal: Boolean = false,
    val showFavoritesModal: Boolean = false,
    val lastAddedRunId: Long? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class AddDistanceInteractor(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val selectedShoeStrategy: SelectedShoeStrategy,
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
                state.value = state.value.copy(
                    runDistance = action.distance.toString(),
                    showFavoritesModal = false
                )
            }
            
            is Action.BounceRequested -> {
                // This will be handled by the UI layer for animation
            }
        }
    }

    private fun observeSettings(state: MutableState<AddDistanceState>) {
        scope.launch {
            userSettingsRepository.userSettingsFlow.collect { settings ->
                settings?.let {
                    state.value = state.value.copy(distanceUnit = it.distanceUnit)
                }
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
}