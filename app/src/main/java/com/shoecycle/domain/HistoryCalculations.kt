package com.shoecycle.domain

import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.models.History
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Date

class HistoryCalculations(
    private val userSettingsRepository: UserSettingsRepository
) {
    
    fun sortHistories(histories: List<History>, ascending: Boolean = true): List<History> {
        return if (ascending) {
            histories.sortedBy { it.runDate }
        } else {
            histories.sortedByDescending { it.runDate }
        }
    }

    suspend fun collateHistoriesByWeek(
        histories: List<History>, 
        ascending: Boolean = true
    ): List<WeeklyCollated> {
        val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
        val firstDayOfWeek = when (userSettings?.firstDayOfWeek) {
            FirstDayOfWeek.SUNDAY -> Calendar.SUNDAY
            FirstDayOfWeek.MONDAY -> Calendar.MONDAY
            else -> Calendar.SUNDAY
        }

        val collatedList = mutableListOf<WeeklyCollated>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = firstDayOfWeek

        val sortedHistories = sortHistories(histories, ascending)
        
        for (history in sortedHistories) {
            val beginningOfWeek = getBeginningOfWeek(history.runDate, calendar)
            
            val existingWeek = collatedList.lastOrNull()
            if (existingWeek != null && existingWeek.weekStartDate == beginningOfWeek) {
                // Add to existing week
                existingWeek.totalDistance += history.runDistance
                existingWeek.runCount++
            } else {
                // Fill in zero-distance weeks between runs if there's a gap
                if (existingWeek != null) {
                    val zeroWeeks = getZeroDistanceWeeksBetween(
                        existingWeek.weekStartDate, 
                        history.runDate, 
                        calendar
                    )
                    collatedList.addAll(zeroWeeks)
                }
                
                // Create new week entry
                val newWeek = WeeklyCollated(
                    weekStartDate = beginningOfWeek,
                    totalDistance = history.runDistance,
                    runCount = 1
                )
                collatedList.add(newWeek)
            }
        }

        return collatedList
    }

    fun historiesByMonth(histories: List<History>, ascending: Boolean = true): List<List<History>> {
        val sortedHistories = sortHistories(histories, ascending)
        val groupedByMonth = mutableListOf<List<History>>()
        var currentMonthRuns = mutableListOf<History>()
        val calendar = Calendar.getInstance()
        var previousMonth = -1
        var previousYear = -1

        for (history in sortedHistories) {
            calendar.time = history.runDate
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            if (month != previousMonth || year != previousYear) {
                if (currentMonthRuns.isNotEmpty()) {
                    groupedByMonth.add(currentMonthRuns.toList())
                }
                currentMonthRuns = mutableListOf()
            }

            currentMonthRuns.add(history)
            previousMonth = month
            previousYear = year
        }

        if (currentMonthRuns.isNotEmpty()) {
            groupedByMonth.add(currentMonthRuns.toList())
        }

        return groupedByMonth
    }

    fun calculateTotalDistance(histories: List<History>): Double {
        return histories.sumOf { it.runDistance }
    }

    fun calculateAverageDistance(histories: List<History>): Double {
        return if (histories.isEmpty()) 0.0 else calculateTotalDistance(histories) / histories.size
    }

    fun findLongestRun(histories: List<History>): History? {
        return histories.maxByOrNull { it.runDistance }
    }

    fun findShortestRun(histories: List<History>): History? {
        return histories.minByOrNull { it.runDistance }
    }

    fun getRunsInDateRange(histories: List<History>, startDate: Date, endDate: Date): List<History> {
        return histories.filter { history ->
            history.runDate >= startDate && history.runDate <= endDate
        }
    }

    fun getHistoryStatistics(histories: List<History>): HistoryStatistics {
        return HistoryStatistics(
            totalRuns = histories.size,
            totalDistance = calculateTotalDistance(histories),
            averageDistance = calculateAverageDistance(histories),
            longestRun = findLongestRun(histories),
            shortestRun = findShortestRun(histories)
        )
    }

    private fun getBeginningOfWeek(date: Date, calendar: Calendar): Date {
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getZeroDistanceWeeksBetween(
        startDate: Date, 
        endDate: Date, 
        calendar: Calendar
    ): List<WeeklyCollated> {
        val zeroWeeks = mutableListOf<WeeklyCollated>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.firstDayOfWeek = calendar.firstDayOfWeek
        
        val beginningOfStartWeek = getBeginningOfWeek(startDate, tempCalendar)
        val beginningOfEndWeek = getBeginningOfWeek(endDate, tempCalendar)
        
        tempCalendar.time = beginningOfStartWeek
        tempCalendar.add(Calendar.WEEK_OF_YEAR, 1) // Start from the week after startDate
        
        while (tempCalendar.time.before(beginningOfEndWeek)) {
            zeroWeeks.add(
                WeeklyCollated(
                    weekStartDate = tempCalendar.time,
                    totalDistance = 0.0,
                    runCount = 0
                )
            )
            tempCalendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        
        return zeroWeeks
    }

    data class WeeklyCollated(
        val weekStartDate: Date,
        var totalDistance: Double,
        var runCount: Int
    )

    data class HistoryStatistics(
        val totalRuns: Int,
        val totalDistance: Double,
        val averageDistance: Double,
        val longestRun: History?,
        val shortestRun: History?
    )
}