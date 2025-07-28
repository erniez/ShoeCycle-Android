package com.shoecycle.ui.settings

import androidx.compose.runtime.MutableState
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUnitsState(
    val selectedUnit: DistanceUnit = DistanceUnit.MILES
)

class SettingsUnitsInteractor(
    private val repository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class UnitChanged(val unit: DistanceUnit) : Action()
        object ViewAppeared : Action()
    }
    
    fun handle(state: MutableState<SettingsUnitsState>, action: Action) {
        when (action) {
            is Action.UnitChanged -> {
                if (state.value.selectedUnit != action.unit) {
                    state.value = state.value.copy(selectedUnit = action.unit)
                    scope.launch {
                        repository.updateDistanceUnit(action.unit)
                    }
                }
            }
            is Action.ViewAppeared -> {
                scope.launch {
                    try {
                        val settings = repository.userSettingsFlow.first()
                        state.value = state.value.copy(selectedUnit = settings.distanceUnit)
                    } catch (e: Exception) {
                        // Handle error silently or with minimal logging if needed
                    }
                }
            }
        }
    }
}