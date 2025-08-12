package com.shoecycle.ui.screens.add_distance.modals

import android.util.Log
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.HistoryCalculations
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.MutableState
import java.util.Calendar

import com.shoecycle.domain.models.History

data class HistoryListState(
    val sections: List<HistorySectionViewModel> = emptyList(),
    val yearlyTotals: YearlyTotalDistance = emptyMap(),
    val showExportDialog: Boolean = false,
    val isLoading: Boolean = false,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val deletedHistory: History? = null,
    val showUndoSnackbar: Boolean = false
)

class HistoryListInteractor(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val historyCalculations: HistoryCalculations = HistoryCalculations(userSettingsRepository),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        data class RemoveHistory(val section: HistorySectionViewModel, val index: Int) : Action()
        object ShowExport : Action()
        object DismissExport : Action()
        object RefreshData : Action()
        object UndoDelete : Action()
        object DismissSnackbar : Action()
    }
    
    fun handle(state: MutableState<HistoryListState>, action: Action, shoe: Shoe) {
        when (action) {
            is Action.ViewAppeared -> {
                loadHistoryData(state, shoe)
            }
            
            is Action.RemoveHistory -> {
                removeHistory(state, action.section, action.index, shoe)
            }
            
            is Action.ShowExport -> {
                state.value = state.value.copy(showExportDialog = true)
            }
            
            is Action.DismissExport -> {
                state.value = state.value.copy(showExportDialog = false)
            }
            
            is Action.RefreshData -> {
                loadHistoryData(state, shoe)
            }
            
            is Action.UndoDelete -> {
                undoDelete(state, shoe)
            }
            
            is Action.DismissSnackbar -> {
                state.value = state.value.copy(
                    showUndoSnackbar = false,
                    deletedHistory = null
                )
            }
        }
    }
    
    private fun loadHistoryData(state: MutableState<HistoryListState>, shoe: Shoe) {
        state.value = state.value.copy(isLoading = true)
        
        scope.launch {
            try {
                val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
                val graphAllShoes = userSettings?.graphAllShoes ?: false
                
                val histories = if (graphAllShoes) {
                    historyRepository.getAllHistory().firstOrNull() ?: emptyList()
                } else {
                    historyRepository.getHistoryForShoe(shoe.id).firstOrNull() ?: emptyList()
                }
                
                Log.d("HistoryListInteractor", "Loaded ${histories.size} history entries for shoe ${shoe.id}")
                
                val monthlyHistories = historyCalculations.historiesByMonth(histories, ascending = false)
                val interimSections = monthlyHistories.map { monthHistories ->
                    HistorySectionViewModel.create(shoe, monthHistories)
                }
                
                val yearlyTotals = collatedHistoriesByYear(interimSections)
                val sections = HistorySectionViewModel.populate(yearlyTotals, interimSections)
                
                Log.d("HistoryListInteractor", "Created ${sections.size} sections with totals: $yearlyTotals")
                
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        sections = sections,
                        yearlyTotals = yearlyTotals,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("HistoryListInteractor", "Error loading history data", e)
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(isLoading = false)
                }
            }
        }
    }
    
    private fun removeHistory(
        state: MutableState<HistoryListState>,
        section: HistorySectionViewModel,
        index: Int,
        shoe: Shoe
    ) {
        if (!state.value.sections.contains(section) || index >= section.histories.size) {
            Log.e("HistoryListInteractor", "Invalid section or index for removal")
            return
        }
        
        val historyToDelete = section.histories[index]
        
        // Store the deleted item and show snackbar
        state.value = state.value.copy(
            deletedHistory = historyToDelete,
            showUndoSnackbar = true
        )
        
        // Remove from UI immediately
        val updatedSections = state.value.sections.map { s ->
            if (s == section) {
                s.copy(
                    histories = s.histories.filterIndexed { i, _ -> i != index },
                    historyViewModels = s.historyViewModels.filterIndexed { i, _ -> i != index }
                )
            } else {
                s
            }
        }
        
        state.value = state.value.copy(sections = updatedSections)
        
        // Actually delete after snackbar timeout (handled by dismissal)
    }
    
    private fun undoDelete(state: MutableState<HistoryListState>, shoe: Shoe) {
        state.value.deletedHistory?.let {
            // Reload data to restore the item
            loadHistoryData(state, shoe)
        }
        state.value = state.value.copy(
            deletedHistory = null,
            showUndoSnackbar = false
        )
    }
    
    fun performActualDelete(state: MutableState<HistoryListState>, shoe: Shoe) {
        val historyToDelete = state.value.deletedHistory ?: return
        
        scope.launch {
            try {
                historyRepository.deleteHistory(historyToDelete)
                
                // Update shoe's total distance
                val updatedHistories = historyRepository.getHistoryForShoe(shoe.id).firstOrNull() ?: emptyList()
                val totalDistance = updatedHistories.sumOf { it.runDistance }
                val updatedShoe = shoe.copy(totalDistance = totalDistance)
                shoeRepository.updateShoe(updatedShoe)
                
                // Reload data to reflect changes
                withContext(Dispatchers.Main) {
                    loadHistoryData(state, updatedShoe)
                    state.value = state.value.copy(
                        deletedHistory = null,
                        showUndoSnackbar = false
                    )
                }
            } catch (e: Exception) {
                Log.e("HistoryListInteractor", "Error removing history", e)
            }
        }
    }
}