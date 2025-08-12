package com.shoecycle.ui.screens.add_distance.modals

import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class HistoryRowViewModel(
    val id: String = UUID.randomUUID().toString(),
    val runDate: Date,
    val runDistance: Double
) {
    val runDateString: String
        get() = DateFormat.getDateInstance(DateFormat.SHORT).format(runDate)
    
    val runDistanceString: String
        get() = String.format(Locale.getDefault(), "%.2f", runDistance)
    
    constructor(history: History) : this(
        runDate = history.runDate,
        runDistance = history.runDistance
    )
}

data class HistorySectionViewModel(
    val id: String = UUID.randomUUID().toString(),
    val monthDate: Date,
    val runTotal: Double,
    var yearlyRunTotal: Double? = null,
    val historyViewModels: List<HistoryRowViewModel>,
    val shoe: Shoe,
    val histories: List<History>
) {
    val monthString: String
        get() {
            val calendar = Calendar.getInstance()
            calendar.time = monthDate
            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
            return monthFormat.format(monthDate)
        }
    
    val runTotalString: String
        get() = String.format(Locale.getDefault(), "%.2f", runTotal)
    
    companion object {
        fun create(shoe: Shoe, histories: List<History>): HistorySectionViewModel {
            val monthDate = histories.firstOrNull()?.runDate ?: Date()
            val runTotal = histories.sumOf { it.runDistance }
            val historyViewModels = histories.map { HistoryRowViewModel(it) }
            
            return HistorySectionViewModel(
                monthDate = monthDate,
                runTotal = runTotal,
                historyViewModels = historyViewModels,
                shoe = shoe,
                histories = histories
            )
        }
        
        fun populate(
            yearlyTotals: Map<Int, Double>,
            sections: List<HistorySectionViewModel>
        ): List<HistorySectionViewModel> {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val calendar = Calendar.getInstance()
            
            return sections.map { viewModel ->
                calendar.time = viewModel.monthDate
                val sectionYear = calendar.get(Calendar.YEAR)
                val sectionMonth = calendar.get(Calendar.MONTH)
                
                if (currentYear == sectionYear) {
                    viewModel
                } else if (sectionMonth == Calendar.DECEMBER) {
                    viewModel.copy(
                        yearlyRunTotal = yearlyTotals[sectionYear]
                    )
                } else {
                    viewModel
                }
            }
        }
    }
}

typealias YearlyTotalDistance = Map<Int, Double>

fun collatedHistoriesByYear(runsByMonth: List<HistorySectionViewModel>): YearlyTotalDistance {
    val runsByYear = mutableMapOf<Int, Double>()
    var totalDistanceForYear = 0.0
    val calendar = Calendar.getInstance()
    var currentYear = calendar.get(Calendar.YEAR)
    
    runsByMonth.forEach { runMonth ->
        calendar.time = runMonth.monthDate
        val year = calendar.get(Calendar.YEAR)
        
        if (year != currentYear) {
            if (totalDistanceForYear > 0) {
                runsByYear[currentYear] = totalDistanceForYear
            } else {
                runsByYear[currentYear] = 0.0
            }
            totalDistanceForYear = 0.0
            currentYear = year
        }
        totalDistanceForYear += runMonth.runTotal
    }
    
    runsByYear[currentYear] = totalDistanceForYear
    return runsByYear
}