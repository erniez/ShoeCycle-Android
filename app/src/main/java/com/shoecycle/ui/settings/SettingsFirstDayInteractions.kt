package com.shoecycle.ui.settings

import androidx.compose.runtime.MutableState
import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.first

data class SettingsFirstDayState(
    val selectedDay: FirstDayOfWeek = FirstDayOfWeek.MONDAY
)

class SettingsFirstDayInteractor(
    private val repository: UserSettingsRepository
) {
    sealed class Action {
        data class DayChanged(val day: FirstDayOfWeek) : Action()
        object ViewAppeared : Action()
    }
    
    suspend fun handle(state: MutableState<SettingsFirstDayState>, action: Action) {
        when (action) {
            is Action.DayChanged -> {
                if (state.value.selectedDay != action.day) {
                    state.value = state.value.copy(selectedDay = action.day)
                    repository.updateFirstDayOfWeek(action.day)
                }
            }
            is Action.ViewAppeared -> {
                try {
                    val settings = repository.userSettingsFlow.first()
                    state.value = state.value.copy(selectedDay = settings.firstDayOfWeek)
                } catch (e: Exception) {
                    // Handle error silently or with minimal logging if needed
                }
            }
        }
    }
}