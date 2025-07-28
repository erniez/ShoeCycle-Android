package com.shoecycle.ui.settings

import androidx.compose.runtime.MutableState
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsStravaState(
    val enabled: Boolean = false
)

class SettingsStravaInteractor(
    private val repository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class ToggleChanged(val enabled: Boolean) : Action()
        object ViewAppeared : Action()
    }
    
    fun handle(state: MutableState<SettingsStravaState>, action: Action) {
        when (action) {
            is Action.ToggleChanged -> {
                if (state.value.enabled != action.enabled) {
                    state.value = state.value.copy(enabled = action.enabled)
                    scope.launch {
                        repository.updateStravaEnabled(action.enabled)
                    }
                }
            }
            is Action.ViewAppeared -> {
                scope.launch {
                    try {
                        val settings = repository.userSettingsFlow.first()
                        state.value = state.value.copy(enabled = settings.stravaEnabled)
                    } catch (e: Exception) {
                        // Handle error silently or with minimal logging if needed
                    }
                }
            }
        }
    }
}