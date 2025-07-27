package com.shoecycle.ui.settings

import androidx.compose.runtime.MutableState
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.first

data class SettingsFavoriteDistancesState(
    val favorite1: Double = 0.0,
    val favorite2: Double = 0.0,
    val favorite3: Double = 0.0,
    val favorite4: Double = 0.0
)

class SettingsFavoriteDistancesInteractor(
    private val repository: UserSettingsRepository
) {
    sealed class Action {
        data class FavoriteChanged(val index: Int, val distance: Double) : Action()
        object ViewAppeared : Action()
    }
    
    suspend fun handle(state: MutableState<SettingsFavoriteDistancesState>, action: Action) {
        when (action) {
            is Action.FavoriteChanged -> {
                val currentState = state.value
                val newState = when (action.index) {
                    1 -> {
                        if (currentState.favorite1 != action.distance) {
                            repository.updateFavorite1(action.distance)
                            currentState.copy(favorite1 = action.distance)
                        } else currentState
                    }
                    2 -> {
                        if (currentState.favorite2 != action.distance) {
                            repository.updateFavorite2(action.distance)
                            currentState.copy(favorite2 = action.distance)
                        } else currentState
                    }
                    3 -> {
                        if (currentState.favorite3 != action.distance) {
                            repository.updateFavorite3(action.distance)
                            currentState.copy(favorite3 = action.distance)
                        } else currentState
                    }
                    4 -> {
                        if (currentState.favorite4 != action.distance) {
                            repository.updateFavorite4(action.distance)
                            currentState.copy(favorite4 = action.distance)
                        } else currentState
                    }
                    else -> currentState
                }
                state.value = newState
            }
            is Action.ViewAppeared -> {
                try {
                    val settings = repository.userSettingsFlow.first()
                    state.value = state.value.copy(
                        favorite1 = settings.favorite1,
                        favorite2 = settings.favorite2,
                        favorite3 = settings.favorite3,
                        favorite4 = settings.favorite4
                    )
                } catch (e: Exception) {
                    // Handle error silently or with minimal logging if needed
                }
            }
        }
    }
}