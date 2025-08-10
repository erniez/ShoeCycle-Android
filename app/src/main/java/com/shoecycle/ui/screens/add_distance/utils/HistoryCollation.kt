package com.shoecycle.ui.screens.add_distance.utils

import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.domain.models.History
import java.util.*
import kotlin.collections.ArrayList

/**
 * iOS-compatible history collation utilities
 * Port of ShoeDistanceCalcExtensions.swift collation logic
 */
object HistoryCollation {
    
    /**
     * Collate history entries into weekly aggregates
     * Matches iOS collateHistories(ascending:firstDayOfWeek:) implementation
     * 
     * @param histories Set of history entries to collate
     * @param ascending Sort order for the results
     * @param firstDayOfWeek First day of the week preference
     * @return List of weekly collated data points with zero-filled gaps
     */
    fun collateHistories(
        histories: Set<History>,
        ascending: Boolean = true,
        firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.SUNDAY
    ): List<WeeklyCollatedNew> {
        val collatedArray = mutableListOf<WeeklyCollatedNew>()
        
        // Configure calendar with user's first day of week preference
        val calendar = Calendar.getInstance().apply {
            this.firstDayOfWeek = when (firstDayOfWeek) {
                FirstDayOfWeek.SUNDAY -> Calendar.SUNDAY
                FirstDayOfWeek.MONDAY -> Calendar.MONDAY
            }
        }
        
        // Sort histories by date
        val sortedRuns = sortHistories(histories, ascending)
        
        sortedRuns.forEach { history ->
            val beginningOfWeek = history.runDate.beginningOfWeek(calendar)
            
            val currentWeeklyCollated = collatedArray.lastOrNull()
            if (currentWeeklyCollated != null) {
                if (currentWeeklyCollated.date == beginningOfWeek) {
                    // We're still within the week, so we add more distance
                    currentWeeklyCollated.runDistance += history.runDistance
                } else {
                    // Check for gaps between runs and add zero mileage weeks
                    val zeroMileageDates = datesForTheBeginningOfWeeksBetweenDates(
                        calendar = calendar,
                        startDate = currentWeeklyCollated.date,
                        endDate = history.runDate
                    )
                    
                    zeroMileageDates.forEach { date ->
                        val collatedZeroDistance = WeeklyCollatedNew(
                            date = date,
                            runDistance = 0.0
                        )
                        collatedArray.add(collatedZeroDistance)
                    }
                    
                    // Create a new weekly collated entry
                    val newWeeklyCollated = WeeklyCollatedNew(
                        date = beginningOfWeek,
                        runDistance = history.runDistance
                    )
                    collatedArray.add(newWeeklyCollated)
                }
            } else {
                // The result array is empty, add the first value
                val weeklyCollated = WeeklyCollatedNew(
                    date = beginningOfWeek,
                    runDistance = history.runDistance
                )
                collatedArray.add(weeklyCollated)
            }
        }
        
        return collatedArray
    }
    
    /**
     * Sort histories by date
     * Matches iOS sortHistories(ascending:) implementation
     */
    fun sortHistories(histories: Set<History>, ascending: Boolean): List<History> {
        return histories.sortedBy { it.runDate }.let {
            if (ascending) it else it.reversed()
        }
    }
    
    /**
     * Group histories by month for display purposes
     * Matches iOS historiesByMonth(ascending:) implementation
     */
    fun historiesByMonth(histories: Set<History>, ascending: Boolean): List<List<History>> {
        val sortedHistories = sortHistories(histories, ascending)
        val runsByMonth = mutableListOf<List<History>>()
        var runsForCurrentMonth = mutableListOf<History>()
        val calendar = Calendar.getInstance()
        var previousMonth = -1
        var previousYear = -1
        
        sortedHistories.forEach { history ->
            calendar.time = history.runDate
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            
            if (month != previousMonth || year != previousYear) {
                if (runsForCurrentMonth.isNotEmpty()) {
                    runsByMonth.add(runsForCurrentMonth.toList())
                }
                runsForCurrentMonth = mutableListOf()
            }
            
            runsForCurrentMonth.add(history)
            previousYear = year
            previousMonth = month
        }
        
        if (runsForCurrentMonth.isNotEmpty()) {
            runsByMonth.add(runsForCurrentMonth.toList())
        }
        
        return runsByMonth
    }
    
    /**
     * Find all week start dates between two dates for zero-filling gaps
     * Matches iOS datesForTheBeginningOfWeeksBetweenDates implementation
     */
    private fun datesForTheBeginningOfWeeksBetweenDates(
        calendar: Calendar,
        startDate: Date,
        endDate: Date
    ): List<Date> {
        val beginningOfWeekDates = mutableListOf<Date>()
        
        val beginningOfStartDateWeek = startDate.beginningOfWeek(calendar)
        val beginningOfEndDateWeek = endDate.beginningOfWeek(calendar)
        
        // Start from the week after the start date
        val currentCalendar = calendar.clone() as Calendar
        currentCalendar.time = beginningOfStartDateWeek
        currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
        
        // Iterate through weeks until we reach the end date's week
        while (currentCalendar.time < beginningOfEndDateWeek) {
            beginningOfWeekDates.add(currentCalendar.time.clone() as Date)
            currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        
        return beginningOfWeekDates
    }
}

/**
 * Extension function to get the beginning of the week for a date
 * Matches iOS beginningOfWeek(forCalendar:) implementation
 */
fun Date.beginningOfWeek(calendar: Calendar): Date {
    val cal = calendar.clone() as Calendar
    cal.time = this
    
    // Set to the first day of the week
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    
    // Reset time to start of day
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    
    return cal.time
}