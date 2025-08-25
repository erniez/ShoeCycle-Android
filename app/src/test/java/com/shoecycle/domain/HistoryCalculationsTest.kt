package com.shoecycle.domain

import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.models.History
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class HistoryCalculationsTest {

    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var historyCalculations: HistoryCalculations

    private val testHistories = listOf(
        createHistory(1L, 5.0, 2024, Calendar.JANUARY, 1),
        createHistory(2L, 3.0, 2024, Calendar.JANUARY, 3),
        createHistory(3L, 8.0, 2024, Calendar.JANUARY, 8),
        createHistory(4L, 4.0, 2024, Calendar.JANUARY, 22)
    )

    @Before
    fun setUp() {
        userSettingsRepository = mock()
        historyCalculations = HistoryCalculations(userSettingsRepository)
    }

    @Test
    fun `sortHistories ascending sorts by date correctly`() {
        // Given: Unsorted histories
        val unsortedHistories = listOf(testHistories[2], testHistories[0], testHistories[1])

        // When: Sorting ascending
        val result = historyCalculations.sortHistories(unsortedHistories, ascending = true)

        // Then: Histories are sorted by date ascending
        assertEquals(testHistories[0].runDate, result[0].runDate)
        assertEquals(testHistories[1].runDate, result[1].runDate)
        assertEquals(testHistories[2].runDate, result[2].runDate)
    }

    @Test
    fun `sortHistories descending sorts by date correctly`() {
        // Given: Unsorted histories
        val unsortedHistories = listOf(testHistories[0], testHistories[2], testHistories[1])

        // When: Sorting descending
        val result = historyCalculations.sortHistories(unsortedHistories, ascending = false)

        // Then: Histories are sorted by date descending
        assertEquals(testHistories[2].runDate, result[0].runDate)
        assertEquals(testHistories[1].runDate, result[1].runDate)
        assertEquals(testHistories[0].runDate, result[2].runDate)
    }

    @Test
    fun `calculateTotalDistance sums all distances correctly`() {
        // Given: Test histories
        val histories = testHistories.take(3) // 5.0 + 3.0 + 8.0 = 16.0

        // When: Calculating total
        val result = historyCalculations.calculateTotalDistance(histories)

        // Then: Total is correct
        assertEquals(16.0, result, 0.01)
    }

    @Test
    fun `calculateAverageDistance calculates correctly`() {
        // Given: Test histories
        val histories = testHistories.take(3) // (5.0 + 3.0 + 8.0) / 3 = 5.33

        // When: Calculating average
        val result = historyCalculations.calculateAverageDistance(histories)

        // Then: Average is correct
        assertEquals(5.33, result, 0.01)
    }

    @Test
    fun `calculateAverageDistance returns zero for empty list`() {
        // Given: Empty histories
        val histories = emptyList<History>()

        // When: Calculating average
        val result = historyCalculations.calculateAverageDistance(histories)

        // Then: Zero is returned
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `findLongestRun returns correct run`() {
        // Given: Test histories
        val histories = testHistories.take(4)

        // When: Finding longest run
        val result = historyCalculations.findLongestRun(histories)

        // Then: Longest run is returned (8.0 distance)
        assertNotNull(result)
        assertEquals(8.0, result!!.runDistance, 0.01)
    }

    @Test
    fun `findShortestRun returns correct run`() {
        // Given: Test histories
        val histories = testHistories.take(4)

        // When: Finding shortest run
        val result = historyCalculations.findShortestRun(histories)

        // Then: Shortest run is returned (3.0 distance)
        assertNotNull(result)
        assertEquals(3.0, result!!.runDistance, 0.01)
    }

    @Test
    fun `getRunsInDateRange filters by date correctly`() {
        // Given: Date range covering first 3 runs
        val startDate = createDate(2024, Calendar.JANUARY, 1)
        val endDate = createDate(2024, Calendar.JANUARY, 10)

        // When: Getting runs in range
        val result = historyCalculations.getRunsInDateRange(testHistories, startDate, endDate)

        // Then: Only runs in range are returned
        assertEquals(3, result.size)
        assertTrue(result.all { it.runDate >= startDate && it.runDate <= endDate })
    }

    @Test
    fun `getHistoryStatistics calculates all statistics correctly`() {
        // Given: Test histories
        val histories = testHistories.take(4)

        // When: Getting statistics
        val stats = historyCalculations.getHistoryStatistics(histories)

        // Then: All statistics are correct
        assertEquals(4, stats.totalRuns)
        assertEquals(20.0, stats.totalDistance, 0.01) // 5 + 3 + 8 + 4
        assertEquals(5.0, stats.averageDistance, 0.01) // 20 / 4
        assertEquals(8.0, stats.longestRun?.runDistance ?: 0.0, 0.01)
        assertEquals(3.0, stats.shortestRun?.runDistance ?: 0.0, 0.01)
    }

    @Test
    fun `getHistoryStatistics handles empty history correctly`() {
        // Given: Empty histories
        val histories = emptyList<History>()

        // When: Getting statistics
        val stats = historyCalculations.getHistoryStatistics(histories)

        // Then: Statistics handle empty case
        assertEquals(0, stats.totalRuns)
        assertEquals(0.0, stats.totalDistance, 0.01)
        assertEquals(0.0, stats.averageDistance, 0.01)
        assertNull(stats.longestRun)
        assertNull(stats.shortestRun)
    }

    @Test
    fun `collateHistoriesByWeek groups runs correctly`() = runTest {
        // Given: Sunday as first day of week
        val userSettings = UserSettingsData(firstDayOfWeek = FirstDayOfWeek.SUNDAY)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Collating by week (using first 3 histories for simplicity)
        val result = historyCalculations.collateHistoriesByWeek(testHistories.take(3))

        // Then: Runs are grouped by week
        assertTrue(result.isNotEmpty())
        // First week should contain runs from Jan 1 and Jan 3
        assertTrue(result[0].totalDistance > 0)
    }

    private fun createHistory(id: Long, distance: Double, year: Int, month: Int, day: Int): History {
        val date = createDate(year, month, day)
        return History(
            id = id,
            shoeId = "test-shoe-id",
            runDistance = distance,
            runDate = date
        )
    }

    private fun createDate(year: Int, month: Int, day: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}