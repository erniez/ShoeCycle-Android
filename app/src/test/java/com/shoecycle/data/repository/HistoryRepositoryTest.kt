package com.shoecycle.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.shoecycle.data.TestDatabaseSetup
import com.shoecycle.data.TestDataFactory
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.domain.models.History
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ShoeCycleDatabase
    private lateinit var shoeRepository: ShoeRepository
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setup() {
        TestDataFactory.resetCounters()
        database = TestDatabaseSetup.createInMemoryDatabase()
        val (shoeRepo, historyRepo) = TestDatabaseSetup.createRepositories(database)
        shoeRepository = shoeRepo
        historyRepository = historyRepo
    }

    @After
    fun teardown() {
        database.close()
    }

    // CRUD Operations Tests

    @Test
    fun insertHistory_returnsValidId() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 5.0)
        
        // When
        val insertedId = historyRepository.insertHistory(history)
        
        // Then
        assertTrue("Inserted ID should be positive", insertedId > 0)
    }

    @Test
    fun insertHistory_canBeRetrievedById() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 7.5)
        
        // When
        val insertedId = historyRepository.insertHistory(history)
        val retrievedHistory = historyRepository.getHistoryById(insertedId)
        
        // Then
        assertNotNull("Retrieved history should not be null", retrievedHistory)
        assertEquals("Shoe ID should match", shoeId, retrievedHistory?.shoeId ?: 0L)
        assertEquals("Run distance should match", 7.5, retrievedHistory?.runDistance ?: 0.0, 0.01)
        assertEquals("ID should match", insertedId, retrievedHistory?.id)
    }

    @Test
    fun updateHistory_persistsChanges() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 5.0)
        val insertedId = historyRepository.insertHistory(history)
        val updatedHistory = history.copy(id = insertedId, runDistance = 8.0)
        
        // When
        historyRepository.updateHistory(updatedHistory)
        val retrievedHistory = historyRepository.getHistoryById(insertedId)
        
        // Then
        assertNotNull("Retrieved history should not be null", retrievedHistory)
        assertEquals("Run distance should be updated", 8.0, retrievedHistory?.runDistance ?: 0.0, 0.01)
    }

    @Test
    fun deleteHistory_removesFromDatabase() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId)
        val insertedId = historyRepository.insertHistory(history)
        val historyToDelete = history.copy(id = insertedId)
        
        // When
        historyRepository.deleteHistory(historyToDelete)
        val retrievedHistory = historyRepository.getHistoryById(insertedId)
        
        // Then
        assertNull("Deleted history should not be retrievable", retrievedHistory)
    }

    // Flow Observation Tests

    @Test
    fun getAllHistory_emitsUpdatesOnInsert() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 6.0)
        
        // When/Then
        historyRepository.getAllHistory().test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<History>(), awaitItem())
            
            // Insert history
            val insertedId = historyRepository.insertHistory(history)
            
            // Verify emission
            val emittedHistories = awaitItem()
            assertEquals("Should emit one history", 1, emittedHistories.size)
            assertEquals("Run distance should match", 6.0, emittedHistories[0].runDistance, 0.01)
            assertEquals("Shoe ID should match", shoeId, emittedHistories[0].shoeId)
        }
    }

    @Test
    fun getHistoryForShoe_filtersCorrectly() = runTest {
        // Given
        val shoe1 = TestDataFactory.createTestShoe(brand = "Shoe 1")
        val shoe2 = TestDataFactory.createTestShoe(brand = "Shoe 2")
        val shoeId1 = shoeRepository.insertShoe(shoe1)
        val shoeId2 = shoeRepository.insertShoe(shoe2)
        
        val history1 = TestDataFactory.createTestHistory(shoeId = shoeId1, runDistance = 5.0)
        val history2 = TestDataFactory.createTestHistory(shoeId = shoeId2, runDistance = 7.0)
        
        // When/Then
        historyRepository.getHistoryForShoe(shoeId1).test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<History>(), awaitItem())
            
            // Insert history for shoe1 first
            historyRepository.insertHistory(history1)
            
            // Verify shoe1's history is emitted
            val firstEmission = awaitItem()
            assertEquals("Should emit shoe1's history", 1, firstEmission.size)
            assertEquals("Should match shoe1's distance", 5.0, firstEmission[0].runDistance, 0.01)
            assertEquals("Should match shoe1's ID", shoeId1, firstEmission[0].shoeId)
            
            // Insert history for shoe2 (Room will invalidate all queries on table change)
            historyRepository.insertHistory(history2)
            
            // Room triggers re-query even for different shoeId, so we expect another emission
            val secondEmission = awaitItem()
            assertEquals("Should still emit only shoe1's history", 1, secondEmission.size)
            assertEquals("Should still match shoe1's distance", 5.0, secondEmission[0].runDistance, 0.01)
            assertEquals("Should still match shoe1's ID", shoeId1, secondEmission[0].shoeId)
        }
    }

    // Date Range Tests

    @Test
    fun getHistoryInDateRange_filtersCorrectly() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // Create well-separated dates using fixed timestamps to avoid boundary issues
        val baseTime = 1725000000000L // Fixed timestamp: Aug 30, 2024
        val oneDayMs = 24L * 60L * 60L * 1000L
        
        val startDate = Date(baseTime) // Range start
        val middleDate = Date(baseTime + (2 * oneDayMs)) // Within range
        val endDate = Date(baseTime + (4 * oneDayMs)) // Range end
        val outsideDate = Date(baseTime - oneDayMs) // Outside range (before start)
        
        val historyInRange1 = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = startDate, runDistance = 5.0)
        val historyInRange2 = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = middleDate, runDistance = 6.0)
        val historyOutOfRange = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = outsideDate, runDistance = 7.0)
        
        // When/Then
        historyRepository.getHistoryInDateRange(startDate, endDate).test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<History>(), awaitItem())
            
            // Insert first history in range
            historyRepository.insertHistory(historyInRange1)
            val afterFirst = awaitItem()
            assertEquals("Should emit first history", 1, afterFirst.size)
            
            // Insert second history in range
            historyRepository.insertHistory(historyInRange2)
            val afterSecond = awaitItem()
            assertEquals("Should emit both histories after second insert", 2, afterSecond.size)
            
            // Insert out-of-range history (should not change the count)
            historyRepository.insertHistory(historyOutOfRange)
            val afterThird = awaitItem()
            assertEquals("Should still emit only 2 histories in range", 2, afterThird.size)
            
            val distances = afterThird.map { it.runDistance }.sorted()
            assertEquals("Should include both in-range histories", listOf(5.0, 6.0), distances)
        }
    }

    @Test
    fun getHistoryForShoeInDateRange_filtersCorrectly() = runTest {
        // Given
        val shoe1 = TestDataFactory.createTestShoe()
        val shoe2 = TestDataFactory.createTestShoe()
        val shoeId1 = shoeRepository.insertShoe(shoe1)
        val shoeId2 = shoeRepository.insertShoe(shoe2)
        
        // Create well-separated dates using fixed timestamps to avoid boundary issues
        val baseTime = 1725000000000L // Fixed timestamp: Aug 30, 2024
        val oneDayMs = 24L * 60L * 60L * 1000L
        
        val startDate = Date(baseTime) // Range start
        val endDate = Date(baseTime + (3 * oneDayMs)) // Range end
        val outsideDate = Date(baseTime - oneDayMs) // Outside range
        
        val shoe1InRange = TestDataFactory.createTestHistory(shoeId = shoeId1, runDate = startDate, runDistance = 5.0)
        val shoe1InRange2 = TestDataFactory.createTestHistory(shoeId = shoeId1, runDate = endDate, runDistance = 4.0)
        val shoe2InRange = TestDataFactory.createTestHistory(shoeId = shoeId2, runDate = startDate, runDistance = 6.0)
        val shoe1OutOfRange = TestDataFactory.createTestHistory(shoeId = shoeId1, runDate = outsideDate, runDistance = 3.0)
        
        // When/Then
        historyRepository.getHistoryForShoeInDateRange(shoeId1, startDate, endDate).test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<History>(), awaitItem())
            
            // Insert first shoe1 history in range
            historyRepository.insertHistory(shoe1InRange)
            val afterFirst = awaitItem()
            assertEquals("Should emit first shoe1 history", 1, afterFirst.size)
            
            // Insert second shoe1 history in range
            historyRepository.insertHistory(shoe1InRange2)
            val afterSecond = awaitItem()
            assertEquals("Should emit both shoe1 histories", 2, afterSecond.size)
            
            // Insert shoe2 history (should not change shoe1's count)
            historyRepository.insertHistory(shoe2InRange)
            val afterThird = awaitItem()
            assertEquals("Should still emit only shoe1 histories", 2, afterThird.size)
            
            // Insert out-of-range shoe1 history (should not change count)
            historyRepository.insertHistory(shoe1OutOfRange)
            val afterFourth = awaitItem()
            assertEquals("Should still emit only 2 shoe1 histories in range", 2, afterFourth.size)
            
            afterFourth.forEach { history ->
                assertEquals("All histories should be for shoe1", shoeId1, history.shoeId)
            }
            
            val distances = afterFourth.map { it.runDistance }.sorted()
            assertEquals("Should include both shoe1 distances", listOf(4.0, 5.0), distances)
        }
    }

    // Business Logic Tests

    @Test
    fun addRun_withValidData_insertsSuccessfully() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val runDate = Date()
        
        // When
        val insertedId = historyRepository.addRun(shoeId, runDate, 5.5)
        val retrievedHistory = historyRepository.getHistoryById(insertedId)
        
        // Then
        assertNotNull("Retrieved history should not be null", retrievedHistory)
        assertEquals("Shoe ID should match", shoeId, retrievedHistory?.shoeId ?: 0L)
        assertEquals("Run distance should match", 5.5, retrievedHistory?.runDistance ?: 0.0, 0.01)
        assertEquals("Run date should match", runDate, retrievedHistory?.runDate)
    }

    @Test
    fun addRun_withCurrentDate_usesToday() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val before = System.currentTimeMillis()
        
        // When
        val insertedId = historyRepository.addRun(shoeId, 3.0)
        val after = System.currentTimeMillis()
        val retrievedHistory = historyRepository.getHistoryById(insertedId)
        
        // Then
        assertNotNull("Retrieved history should not be null", retrievedHistory)
        val historyTime = retrievedHistory?.runDate?.time ?: 0
        assertTrue("Run date should be between before and after", historyTime in before..after)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRun_withZeroDistance_throwsException() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        historyRepository.addRun(shoeId, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addRun_withNegativeDistance_throwsException() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        historyRepository.addRun(shoeId, -5.0)
    }

    // Statistics Tests

    @Test
    fun getTotalDistanceForShoe_calculatesCorrectly() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val histories = TestDataFactory.createMultipleTestHistories(shoeId, 3, distanceRange = 5.0)
        
        // When
        histories.forEach { historyRepository.insertHistory(it) }
        val totalDistance = historyRepository.getTotalDistanceForShoe(shoeId)
        
        // Then
        // Expected: 5.0 + 5.5 + 6.0 = 16.5
        assertEquals("Total distance should be sum of all runs", 16.5, totalDistance, 0.01)
    }

    @Test
    fun getRunCountForShoe_countsCorrectly() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        val histories = TestDataFactory.createMultipleTestHistories(shoeId, 5)
        
        // When
        histories.forEach { historyRepository.insertHistory(it) }
        val runCount = historyRepository.getRunCountForShoe(shoeId)
        
        // Then
        assertEquals("Run count should match inserted histories", 5, runCount)
    }

    @Test
    fun getAverageDistanceForShoe_calculatesCorrectly() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // Create histories with known distances: 4.0, 6.0, 8.0 (average = 6.0)
        val history1 = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 4.0)
        val history2 = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 6.0)
        val history3 = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 8.0)
        
        // When
        historyRepository.insertHistory(history1)
        historyRepository.insertHistory(history2)
        historyRepository.insertHistory(history3)
        val averageDistance = historyRepository.getAverageDistanceForShoe(shoeId)
        
        // Then
        assertEquals("Average distance should be 6.0", 6.0, averageDistance, 0.01)
    }

    @Test
    fun getAverageDistanceForShoe_withNoRuns_returnsZero() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When
        val averageDistance = historyRepository.getAverageDistanceForShoe(shoeId)
        
        // Then
        assertEquals("Average distance should be 0 for no runs", 0.0, averageDistance, 0.01)
    }

    @Test
    fun getFirstRunForShoe_returnsEarliestRun() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -5)
        val fiveDaysAgo = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -5)
        val tenDaysAgo = calendar.time
        
        val recentHistory = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = today, runDistance = 5.0)
        val middleHistory = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = fiveDaysAgo, runDistance = 6.0)
        val oldestHistory = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = tenDaysAgo, runDistance = 7.0)
        
        // When
        historyRepository.insertHistory(recentHistory)
        historyRepository.insertHistory(middleHistory)
        historyRepository.insertHistory(oldestHistory)
        val firstRun = historyRepository.getFirstRunForShoe(shoeId)
        
        // Then
        assertNotNull("First run should not be null", firstRun)
        assertEquals("First run should be the oldest", 7.0, firstRun?.runDistance ?: 0.0, 0.01)
        assertEquals("First run date should be the earliest", tenDaysAgo, firstRun?.runDate)
    }

    @Test
    fun getLastRunForShoe_returnsLatestRun() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -5)
        val fiveDaysAgo = calendar.time
        
        val recentHistory = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = today, runDistance = 5.0)
        val olderHistory = TestDataFactory.createTestHistory(shoeId = shoeId, runDate = fiveDaysAgo, runDistance = 6.0)
        
        // When
        historyRepository.insertHistory(recentHistory)
        historyRepository.insertHistory(olderHistory)
        val lastRun = historyRepository.getLastRunForShoe(shoeId)
        
        // Then
        assertNotNull("Last run should not be null", lastRun)
        assertEquals("Last run should be the most recent", 5.0, lastRun?.runDistance ?: 0.0, 0.01)
        assertEquals("Last run date should be the latest", today, lastRun?.runDate)
    }

    // Shoe Integration Tests

    @Test
    fun insertHistory_updatesShoeTotal() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 5.0)
        
        // When
        historyRepository.insertHistory(history)
        val updatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        // Then
        assertNotNull("Updated shoe should not be null", updatedShoe)
        assertEquals("Shoe total should include new run", 15.0, updatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun updateHistory_recalculatesShoeTotal() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        val history = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 5.0)
        val insertedId = historyRepository.insertHistory(history)
        
        // When
        val updatedHistory = history.copy(id = insertedId, runDistance = 8.0)
        historyRepository.updateHistory(updatedHistory)
        val updatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        // Then
        assertNotNull("Updated shoe should not be null", updatedShoe)
        assertEquals("Shoe total should reflect updated run distance", 18.0, updatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun deleteHistory_recalculatesShoeTotal() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        val history1 = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 5.0)
        val history2 = TestDataFactory.createTestHistory(shoeId = shoeId, runDistance = 3.0)
        
        val insertedId1 = historyRepository.insertHistory(history1)
        historyRepository.insertHistory(history2)
        
        // When
        val historyToDelete = history1.copy(id = insertedId1)
        historyRepository.deleteHistory(historyToDelete)
        val updatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        // Then
        assertNotNull("Updated shoe should not be null", updatedShoe)
        assertEquals("Shoe total should reflect deleted run", 13.0, updatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun deleteAllHistoryForShoe_resetsShoeToStartDistance() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        val histories = TestDataFactory.createMultipleTestHistories(shoeId, 3)
        
        // When
        histories.forEach { historyRepository.insertHistory(it) }
        historyRepository.deleteAllHistoryForShoe(shoeId)
        val updatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        // Then
        assertNotNull("Updated shoe should not be null", updatedShoe)
        assertEquals("Shoe total should reset to start distance", 10.0, updatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    // Edge Cases

    @Test
    fun getHistoryById_withInvalidId_returnsNull() = runTest {
        // When
        val retrievedHistory = historyRepository.getHistoryById(999L)
        
        // Then
        assertNull("Non-existent history should return null", retrievedHistory)
    }

    @Test
    fun getTotalDistanceForShoe_withNoHistory_returnsZero() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When
        val totalDistance = historyRepository.getTotalDistanceForShoe(shoeId)
        
        // Then
        assertEquals("Total distance should be 0 for no history", 0.0, totalDistance, 0.01)
    }

    @Test
    fun getRunCountForShoe_withNoHistory_returnsZero() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When
        val runCount = historyRepository.getRunCountForShoe(shoeId)
        
        // Then
        assertEquals("Run count should be 0 for no history", 0, runCount)
    }
}