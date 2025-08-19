package com.shoecycle.ui.screens.add_distance.modals

import androidx.compose.runtime.MutableState
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.DistanceUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class FavoriteDistancesState(
    val distanceToAdd: Double = 0.0,
    val favorite1DisplayString: String? = null,
    val favorite2DisplayString: String? = null,
    val favorite3DisplayString: String? = null,
    val favorite4DisplayString: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class FavoriteDistancesInteractor(
    private val userSettingsRepository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        data class DistanceSelected(val distance: Double) : Action()
        object CancelPressed : Action()
    }
    
    fun handle(state: MutableState<FavoriteDistancesState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                loadFavorites(state)
            }
            
            is Action.DistanceSelected -> {
                state.value = state.value.copy(distanceToAdd = action.distance)
            }
            
            is Action.CancelPressed -> {
                state.value = state.value.copy(distanceToAdd = 0.0)
            }
        }
    }
    
    private fun loadFavorites(state: MutableState<FavoriteDistancesState>) {
        scope.launch {
            userSettingsRepository.userSettingsFlow.collect { settings ->
                settings?.let {
                    val unit = it.distanceUnit
                    val fav1 = displayString(it.favorite1, unit)
                    val fav2 = displayString(it.favorite2, unit)
                    val fav3 = displayString(it.favorite3, unit)
                    val fav4 = displayString(it.favorite4, unit)
                    
                    state.value = state.value.copy(
                        favorite1DisplayString = fav1,
                        favorite2DisplayString = fav2,
                        favorite3DisplayString = fav3,
                        favorite4DisplayString = fav4,
                        distanceUnit = unit
                    )
                }
            }
        }
    }
    
    private fun displayString(distance: Double, unit: DistanceUnit): String? {
        return if (distance > 0) {
            DistanceUtility.favoriteDistanceDisplayString(distance, unit)
        } else {
            null
        }
    }
}