package com.shoecycle.ui.screens.add_distance.modals

import android.content.Context
import android.util.Log
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.CSVUtility
import com.shoecycle.domain.HistoryCalculations
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.MutableState
import java.io.File
import java.io.FileWriter
import java.util.Calendar

import com.shoecycle.domain.models.History

data class HistoryListState(
    val sections: List<HistorySectionViewModel> = emptyList(),
    val yearlyTotals: YearlyTotalDistance = emptyMap(),
    val showExportDialog: Boolean = false,
    val isLoading: Boolean = false,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val deletedHistory: History? = null,
    val showUndoSnackbar: Boolean = false,
    val exportFilePath: String? = null,
    val exportError: String? = null
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
        data class ExportToCSV(val context: Context) : Action()
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
                state.value = state.value.copy(showExportDialog = false, exportFilePath = null, exportError = null)
            }
            
            is Action.ExportToCSV -> {
                exportToCSV(state, action.context, shoe)
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
    
    private fun exportToCSV(state: MutableState<HistoryListState>, context: Context, shoe: Shoe) {
        scope.launch {
            try {
                // Get histories for the shoe
                val histories = historyRepository.getHistoryForShoe(shoe.id).firstOrNull() ?: emptyList()
                
                // Create CSV data
                val csvUtility = CSVUtility()
                val csvData = csvUtility.createCSVData(shoe, histories)
                val fileName = csvUtility.generateFileName(shoe)
                
                // Create file in cache directory
                val cacheDir = context.cacheDir
                val csvFile = File(cacheDir, fileName)
                
                // Write CSV data to file
                FileWriter(csvFile).use { writer ->
                    writer.write(csvData)
                }
                
                Log.d("HistoryListInteractor", "CSV file created: ${csvFile.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        exportFilePath = csvFile.absolutePath,
                        exportError = null
                    )
                }
            } catch (e: Exception) {
                Log.e("HistoryListInteractor", "Error creating CSV file", e)
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        exportFilePath = null,
                        exportError = "Failed to create export file: ${e.message}"
                    )
                }
            }
        }
    }
}