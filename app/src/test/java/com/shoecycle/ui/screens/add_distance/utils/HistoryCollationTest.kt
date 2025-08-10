package com.shoecycle.ui.screens.add_distance.utils

import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class HistoryCollationTest {
    
    @Test
    fun `collateHistories groups runs by week correctly`() {
        // Given: Multiple runs in the same week and different weeks
        val calendar = Calendar.getInstance()
        
        // Create a shoe for testing using the createDefault helper
        val shoe = Shoe.createDefault(
            brand = "Test Runner",
            maxDistance = 500.0
        )
        
        // Week 1 - Two runs
        calendar.set(2024, Calendar.JANUARY, 1) // Monday
        val run1 = History(
            id = 1L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 5.0
        )
        
        calendar.set(2024, Calendar.JANUARY, 3) // Wednesday same week
        val run2 = History(
            id = 2L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 3.0
        )
        
        // Week 2 - One run
        calendar.set(2024, Calendar.JANUARY, 8) // Next Monday
        val run3 = History(
            id = 3L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 10.0
        )
        
        val histories = setOf(run1, run2, run3)
        
        // When: Collating histories by week
        val result = HistoryCollation.collateHistories(
            histories = histories,
            ascending = true,
            firstDayOfWeek = FirstDayOfWeek.MONDAY
        )
        
        // Then: Should have 2 weekly entries with correct totals
        assertEquals(2, result.size)
        assertEquals(8.0, result[0].runDistance, 0.01) // Week 1: 5 + 3
        assertEquals(10.0, result[1].runDistance, 0.01) // Week 2: 10
    }
    
    @Test
    fun `collateHistories fills zero mileage weeks between runs`() {
        // Given: Runs with a gap of multiple weeks
        val calendar = Calendar.getInstance()
        
        val shoe = Shoe.createDefault(
            brand = "Test Runner",
            maxDistance = 500.0
        )
        
        // First run
        calendar.set(2024, Calendar.JANUARY, 1)
        val run1 = History(
            id = 1L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 5.0
        )
        
        // Second run 3 weeks later
        calendar.set(2024, Calendar.JANUARY, 22)
        val run2 = History(
            id = 2L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 7.0
        )
        
        val histories = setOf(run1, run2)
        
        // When: Collating histories
        val result = HistoryCollation.collateHistories(
            histories = histories,
            ascending = true,
            firstDayOfWeek = FirstDayOfWeek.MONDAY
        )
        
        // Then: Should have 4 weeks total (2 with runs, 2 with zero mileage)
        assertEquals(4, result.size)
        assertEquals(5.0, result[0].runDistance, 0.01) // Week 1
        assertEquals(0.0, result[1].runDistance, 0.01) // Week 2 (zero)
        assertEquals(0.0, result[2].runDistance, 0.01) // Week 3 (zero)
        assertEquals(7.0, result[3].runDistance, 0.01) // Week 4
    }
    
    @Test
    fun `historiesByMonth groups runs correctly`() {
        // Given: Runs across multiple months
        val calendar = Calendar.getInstance()
        
        val shoe = Shoe.createDefault(
            brand = "Test Runner",
            maxDistance = 500.0
        )
        
        // January runs
        calendar.set(2024, Calendar.JANUARY, 5)
        val run1 = History(
            id = 1L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 5.0
        )
        
        calendar.set(2024, Calendar.JANUARY, 15)
        val run2 = History(
            id = 2L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 3.0
        )
        
        // February run
        calendar.set(2024, Calendar.FEBRUARY, 10)
        val run3 = History(
            id = 3L,
            shoeId = shoe.id,
            runDate = calendar.time,
            runDistance = 7.0
        )
        
        val histories = setOf(run1, run2, run3)
        
        // When: Grouping by month
        val result = HistoryCollation.historiesByMonth(histories, ascending = true)
        
        // Then: Should have 2 groups
        assertEquals(2, result.size)
        assertEquals(2, result[0].size) // January: 2 runs
        assertEquals(1, result[1].size) // February: 1 run
    }
    
    @Test
    fun `beginningOfWeek extension returns correct date`() {
        // Given: A date in the middle of the week
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }
        
        calendar.set(2024, Calendar.JANUARY, 10, 14, 30, 0) // Wednesday 2:30 PM
        val testDate = calendar.time
        
        // When: Getting beginning of week
        val beginningOfWeek = testDate.beginningOfWeek(calendar)
        
        // Then: Should be Monday at midnight
        calendar.time = beginningOfWeek
        assertEquals(Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
    }
}