package com.shoecycle.ui.screens.hall_of_fame

import android.util.Log
import androidx.compose.runtime.MutableState
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HallOfFameState(
    val shoes: List<Shoe> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class HallOfFameInteractor(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        object Refresh : Action()
    }
    
    fun handle(state: MutableState<HallOfFameState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                loadHallOfFameShoes(state)
                observeSettings(state)
            }
            is Action.Refresh -> {
                loadHallOfFameShoes(state)
            }
        }
    }
    
    private fun observeSettings(state: MutableState<HallOfFameState>) {
        scope.launch {
            userSettingsRepository.userSettingsFlow.collect { settings ->
                settings?.let {
                    state.value = state.value.copy(distanceUnit = it.distanceUnit)
                }
            }
        }
    }
    
    private fun loadHallOfFameShoes(state: MutableState<HallOfFameState>) {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        
        scope.launch {
            try {
                // Collect hall of fame shoes (where hallOfFame = true)
                shoeRepository.getRetiredShoes().catch { exception ->
                    Log.e("HallOfFameInteractor", "Error loading hall of fame shoes", exception)
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = "Error loading shoes: ${exception.message}"
                    )
                }.collect { hallOfFameShoes ->
                    Log.d("HallOfFameInteractor", "Loaded ${hallOfFameShoes.size} hall of fame shoes")
                    state.value = state.value.copy(
                        shoes = hallOfFameShoes,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("HallOfFameInteractor", "Error in loadHallOfFameShoes", e)
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading shoes: ${e.message}"
                )
            }
        }
    }
}