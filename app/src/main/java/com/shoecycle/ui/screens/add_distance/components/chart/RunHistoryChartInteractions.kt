package com.shoecycle.ui.screens.add_distance.components.chart

import com.shoecycle.ui.screens.add_distance.utils.WeeklyCollatedNew
import java.util.Date

/**
 * State for the Run History Chart
 * Follows VSI pattern for state management
 */
data class RunHistoryChartState(
    val chartData: List<WeeklyCollatedNew> = emptyList(),
    val maxDistance: Double = 0.0,
    val xValues: List<Date> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Interactor for Run History Chart
 * Handles all business logic for the chart component
 */
class RunHistoryChartInteractor {
    
    sealed class Action {
        object ViewAppeared : Action()
        data class DataUpdated(val collatedHistory: List<WeeklyCollatedNew>) : Action()
    }
    
    fun handle(state: RunHistoryChartState, action: Action): RunHistoryChartState {
        return when (action) {
            is Action.ViewAppeared -> {
                // Initial load state
                state.copy(isLoading = true)
            }
            
            is Action.DataUpdated -> {
                val xValues = action.collatedHistory.map { it.date }
                val maxDistance = action.collatedHistory.maxOfOrNull { it.runDistance } ?: 0.0
                
                state.copy(
                    chartData = action.collatedHistory,
                    xValues = xValues,
                    maxDistance = maxDistance,
                    isLoading = false
                )
            }
        }
    }
}