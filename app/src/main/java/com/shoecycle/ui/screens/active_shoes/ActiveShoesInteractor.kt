package com.shoecycle.ui.screens.active_shoes

import androidx.compose.runtime.MutableState
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.MockShoeGenerator
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ActiveShoesState(
    val shoes: List<Shoe> = emptyList(),
    val isLoading: Boolean = true,
    val isGeneratingTestData: Boolean = false,
    val selectedShoeId: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class ActiveShoesInteractor(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        object GenerateTestData : Action()
        data class ShoeSelected(val shoeId: String) : Action()
    }
    
    fun handle(state: MutableState<ActiveShoesState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                scope.launch {
                    try {
                        // Combine both flows to react to changes in either
                        combine(
                            userSettingsRepository.userSettingsFlow,
                            shoeRepository.getActiveShoes()
                        ) { settings, shoes ->
                            state.value = state.value.copy(
                                shoes = shoes,
                                isLoading = false,
                                distanceUnit = settings.distanceUnit,
                                selectedShoeId = settings.selectedShoeId
                            )
                        }.collect { }
                    } catch (e: Exception) {
                        state.value = state.value.copy(isLoading = false)
                    }
                }
            }
            is Action.GenerateTestData -> {
                scope.launch {
                    try {
                        state.value = state.value.copy(isGeneratingTestData = true)
                        val mockGenerator = MockShoeGenerator(shoeRepository, historyRepository)
                        mockGenerator.generateNewShoeWithData()
                        state.value = state.value.copy(isGeneratingTestData = false)
                    } catch (e: Exception) {
                        state.value = state.value.copy(isGeneratingTestData = false)
                        // TODO: Handle error state
                    }
                }
            }
            is Action.ShoeSelected -> {
                scope.launch {
                    try {
                        // Update user settings - the Flow will update state naturally
                        userSettingsRepository.updateSelectedShoeId(action.shoeId)
                    } catch (e: Exception) {
                        // TODO: Handle error state
                    }
                }
            }
        }
    }
}