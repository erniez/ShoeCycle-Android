package com.shoecycle.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.shoecycle.data.TestDatabaseSetup
import com.shoecycle.data.TestDataFactory
import com.shoecycle.data.database.ShoeCycleDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class RepositoryIntegrationTest {

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

    // Shoe-History Relationship Tests

    @Test
    fun addingRunsToShoe_updatesShoeTotal_automatically() = runTest {
        // Given
        val startDistance = 50.0
        val shoe = TestDataFactory.createTestShoe(startDistance = startDistance)
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When
        historyRepository.addRun(shoeId, 5.0)
        historyRepository.addRun(shoeId, 3.0)
        historyRepository.addRun(shoeId, 7.0)
        
        // Then
        val updatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        assertNotNull("Updated shoe should exist", updatedShoe)
        assertEquals("Total should include start distance plus all runs", 
            65.0, updatedShoe?.totalDistance ?: 0.0, 0.01) // 50 + 5 + 3 + 7 = 65
    }

    @Test
    fun multipleShoes_eachMaintainsOwnTotal() = runTest {
        // Given
        val shoe1 = TestDataFactory.createTestShoe(brand = "Shoe 1", startDistance = 10.0)
        val shoe2 = TestDataFactory.createTestShoe(brand = "Shoe 2", startDistance = 20.0)
        val shoeId1 = shoeRepository.insertShoe(shoe1)
        val shoeId2 = shoeRepository.insertShoe(shoe2)
        
        // When
        historyRepository.addRun(shoeId1, 5.0)
        historyRepository.addRun(shoeId1, 3.0)
        historyRepository.addRun(shoeId2, 8.0)
        
        // Then
        val updatedShoe1 = shoeRepository.getShoeByIdOnce(shoeId1)
        val updatedShoe2 = shoeRepository.getShoeByIdOnce(shoeId2)
        
        assertEquals("Shoe 1 total should be 18.0", 18.0, updatedShoe1?.totalDistance ?: 0.0, 0.01)
        assertEquals("Shoe 2 total should be 28.0", 28.0, updatedShoe2?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun deletingShoe_removesAllRelatedHistory() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // Add several runs
        historyRepository.addRun(shoeId, 5.0)
        historyRepository.addRun(shoeId, 3.0)
        historyRepository.addRun(shoeId, 7.0)
        
        // Verify history exists
        val historyBefore = historyRepository.getHistoryForShoe(shoeId).first()
        assertEquals("Should have 3 history records", 3, historyBefore.size)
        
        // When
        val shoeToDelete = shoeRepository.getShoeByIdOnce(shoeId)!!
        shoeRepository.deleteShoe(shoeToDelete)
        
        // Then
        val historyAfter = historyRepository.getHistoryForShoe(shoeId).first()
        assertEquals("History should be empty after shoe deletion", 0, historyAfter.size)
    }

    // Flow Synchronization Tests

    @Test
    fun activeShoesFlow_updatesWhenShoeRetired() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Active Shoe")
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        shoeRepository.getActiveShoes().test {
            // Initial state with active shoe
            val initialShoes = awaitItem()
            assertEquals("Should have 1 active shoe", 1, initialShoes.size)
            assertEquals("Should be the correct shoe", "Active Shoe", initialShoes[0].brand)
            
            // Retire the shoe
            shoeRepository.retireShoe(shoeId)
            
            // Verify active shoes flow updates
            val updatedShoes = awaitItem()
            assertEquals("Should have no active shoes", 0, updatedShoes.size)
        }
    }

    @Test
    fun retiredShoesFlow_updatesWhenShoeRetired() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Active Shoe")
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        shoeRepository.getRetiredShoes().test {
            // Initial empty state
            val initialShoes = awaitItem()
            assertEquals("Should have no retired shoes initially", 0, initialShoes.size)
            
            // Retire the shoe
            shoeRepository.retireShoe(shoeId)
            
            // Verify retired shoes flow updates
            val updatedShoes = awaitItem()
            assertEquals("Should have 1 retired shoe", 1, updatedShoes.size)
            assertEquals("Should be the correct shoe", "Active Shoe", updatedShoes[0].brand)
            assertTrue("Should be retired", updatedShoes[0].isRetired)
        }
    }

    @Test
    fun historyFlow_updatesImmediatelyAfterRunAdded() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        historyRepository.getHistoryForShoe(shoeId).test {
            // Initial empty state
            assertEquals("Initial history should be empty", 0, awaitItem().size)
            
            // Add run
            historyRepository.addRun(shoeId, 5.0)
            
            // Verify immediate update
            val updatedHistory = awaitItem()
            assertEquals("Should have 1 history record", 1, updatedHistory.size)
            assertEquals("Run distance should match", 5.0, updatedHistory[0].runDistance, 0.01)
        }
    }

    // Complex Workflow Tests

    @Test
    fun completeShoeLifecycle_maintainsDataIntegrity() = runTest {
        // Given - Create a new shoe
        val shoeId = shoeRepository.createShoe("Nike Pegasus", 400.0)
        val createdShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        assertNotNull("Created shoe should exist", createdShoe)
        assertEquals("Brand should match", "Nike Pegasus", createdShoe?.brand)
        assertEquals("Max distance should match", 400.0, createdShoe?.maxDistance ?: 0.0, 0.01)
        assertTrue("Shoe should be active", createdShoe?.isActive == true)
        
        // When - Add multiple runs over time
        val runs = listOf(5.0, 3.5, 7.2, 4.8, 6.1)
        runs.forEach { distance ->
            historyRepository.addRun(shoeId, distance)
        }
        
        // Then - Verify totals and history
        val shoeWithRuns = shoeRepository.getShoeByIdOnce(shoeId)
        val totalExpected = runs.sum()
        assertEquals("Shoe total should equal sum of runs", totalExpected, shoeWithRuns?.totalDistance ?: 0.0, 0.01)
        
        val historyRecords = historyRepository.getHistoryForShoe(shoeId).first()
        assertEquals("Should have all history records", runs.size, historyRecords.size)
        
        val totalDistance = historyRepository.getTotalDistanceForShoe(shoeId)
        assertEquals("Repository total should match shoe total", totalExpected, totalDistance, 0.01)
        
        val runCount = historyRepository.getRunCountForShoe(shoeId)
        assertEquals("Run count should match", runs.size, runCount)
        
        // When - Retire the shoe
        shoeRepository.retireShoe(shoeId)
        
        // Then - Verify retired status but data preserved
        val retiredShoe = shoeRepository.getShoeByIdOnce(shoeId)
        assertTrue("Shoe should be retired", retiredShoe?.isRetired == true)
        assertEquals("Distance should be preserved", totalExpected, retiredShoe?.totalDistance ?: 0.0, 0.01)
        
        val historyAfterRetirement = historyRepository.getHistoryForShoe(shoeId).first()
        assertEquals("History should be preserved", runs.size, historyAfterRetirement.size)
        
        // When - Reactivate the shoe
        shoeRepository.reactivateShoe(shoeId)
        
        // Then - Verify reactivated state
        val reactivatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        assertTrue("Shoe should be active again", reactivatedShoe?.isActive == true)
        assertEquals("Distance should still be preserved", totalExpected, reactivatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun multipleShoeWorkflow_maintainsIndependentData() = runTest {
        // Given - Create multiple shoes
        val shoe1Id = shoeRepository.createShoe("Running Shoe 1", 300.0)
        val shoe2Id = shoeRepository.createShoe("Running Shoe 2", 500.0)
        val shoe3Id = shoeRepository.createShoe("Running Shoe 3", 400.0)
        
        // When - Add different runs to each shoe
        historyRepository.addRun(shoe1Id, 5.0)
        historyRepository.addRun(shoe1Id, 3.0)
        
        historyRepository.addRun(shoe2Id, 7.0)
        historyRepository.addRun(shoe2Id, 4.0)
        historyRepository.addRun(shoe2Id, 6.0)
        
        historyRepository.addRun(shoe3Id, 8.0)
        
        // Then - Verify each shoe maintains its own totals
        val shoe1 = shoeRepository.getShoeByIdOnce(shoe1Id)
        val shoe2 = shoeRepository.getShoeByIdOnce(shoe2Id)
        val shoe3 = shoeRepository.getShoeByIdOnce(shoe3Id)
        
        assertEquals("Shoe 1 total should be 8.0", 8.0, shoe1?.totalDistance ?: 0.0, 0.01)
        assertEquals("Shoe 2 total should be 17.0", 17.0, shoe2?.totalDistance ?: 0.0, 0.01)
        assertEquals("Shoe 3 total should be 8.0", 8.0, shoe3?.totalDistance ?: 0.0, 0.01)
        
        val shoe1History = historyRepository.getHistoryForShoe(shoe1Id).first()
        val shoe2History = historyRepository.getHistoryForShoe(shoe2Id).first()
        val shoe3History = historyRepository.getHistoryForShoe(shoe3Id).first()
        
        assertEquals("Shoe 1 should have 2 runs", 2, shoe1History.size)
        assertEquals("Shoe 2 should have 3 runs", 3, shoe2History.size)
        assertEquals("Shoe 3 should have 1 run", 1, shoe3History.size)
        
        // When - Retire one shoe
        shoeRepository.retireShoe(shoe2Id)
        
        // Then - Verify counts update correctly
        val activeCount = shoeRepository.getActiveShoesCount()
        val retiredCount = shoeRepository.getRetiredShoesCount()
        
        assertEquals("Should have 2 active shoes", 2, activeCount)
        assertEquals("Should have 1 retired shoe", 1, retiredCount)
        
        // Verify other shoes unaffected
        val shoe1After = shoeRepository.getShoeByIdOnce(shoe1Id)
        val shoe3After = shoeRepository.getShoeByIdOnce(shoe3Id)
        
        assertTrue("Shoe 1 should still be active", shoe1After?.isActive == true)
        assertTrue("Shoe 3 should still be active", shoe3After?.isActive == true)
        assertEquals("Shoe 1 total unchanged", 8.0, shoe1After?.totalDistance ?: 0.0, 0.01)
        assertEquals("Shoe 3 total unchanged", 8.0, shoe3After?.totalDistance ?: 0.0, 0.01)
    }

    // Date Range Integration Tests

    @Test
    fun dateRangeQueries_workAcrossRepositories() = runTest {
        // Given - Create shoes and runs across different dates
        val shoe1Id = shoeRepository.createShoe("Shoe 1", 300.0)
        val shoe2Id = shoeRepository.createShoe("Shoe 2", 400.0)
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val oneWeekAgo = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val twoWeeksAgo = calendar.time
        
        // Add runs at different dates
        historyRepository.addRun(shoe1Id, today, 5.0)
        historyRepository.addRun(shoe1Id, oneWeekAgo, 4.0)
        historyRepository.addRun(shoe1Id, twoWeeksAgo, 3.0)
        
        historyRepository.addRun(shoe2Id, today, 6.0)
        historyRepository.addRun(shoe2Id, twoWeeksAgo, 7.0)
        
        // When - Query different date ranges
        val lastWeekHistory = historyRepository.getHistoryInDateRange(oneWeekAgo, today).first()
        val allHistory = historyRepository.getAllHistory().first()
        
        // Then - Verify correct filtering
        assertEquals("Should have 3 runs in last week", 3, lastWeekHistory.size)
        assertEquals("Should have 5 runs total", 5, allHistory.size)
        
        val lastWeekDistances = lastWeekHistory.map { it.runDistance }.sorted()
        assertEquals("Last week should include recent runs", listOf(4.0, 5.0, 6.0), lastWeekDistances)
        
        // Verify shoe totals include all runs regardless of date range
        val shoe1Total = shoeRepository.getShoeByIdOnce(shoe1Id)?.totalDistance
        val shoe2Total = shoeRepository.getShoeByIdOnce(shoe2Id)?.totalDistance
        
        assertEquals("Shoe 1 total should include all runs", 12.0, shoe1Total ?: 0.0, 0.01)
        assertEquals("Shoe 2 total should include all runs", 13.0, shoe2Total ?: 0.0, 0.01)
    }

    // Concurrent Operations Tests

    @Test
    fun concurrentHistoryUpdates_maintainDataIntegrity() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 0.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When - Add multiple runs concurrently (simulated with sequential calls)
        val runs = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        runs.forEach { distance ->
            historyRepository.addRun(shoeId, distance)
        }
        
        // Then - Verify final totals are consistent
        val finalShoe = shoeRepository.getShoeByIdOnce(shoeId)
        val historyTotal = historyRepository.getTotalDistanceForShoe(shoeId)
        val historyCount = historyRepository.getRunCountForShoe(shoeId)
        
        val expectedTotal = runs.sum()
        
        assertEquals("Shoe total should match expected", expectedTotal, finalShoe?.totalDistance ?: 0.0, 0.01)
        assertEquals("History total should match shoe total", finalShoe?.totalDistance ?: 0.0, historyTotal, 0.01)
        assertEquals("History count should match", runs.size, historyCount)
    }

    // Error Recovery Tests

    @Test
    fun dataConsistency_afterHistoryUpdatesAndDeletes() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // When - Add runs, update some, delete others
        val run1Id = historyRepository.addRun(shoeId, 5.0)
        val run2Id = historyRepository.addRun(shoeId, 3.0)
        val run3Id = historyRepository.addRun(shoeId, 7.0)
        
        // Verify intermediate state
        var currentShoe = shoeRepository.getShoeByIdOnce(shoeId)
        assertEquals("Initial total should be 25.0", 25.0, currentShoe?.totalDistance ?: 0.0, 0.01)
        
        // Update one run
        val updatedRun = historyRepository.getHistoryById(run2Id)!!.copy(runDistance = 8.0)
        historyRepository.updateHistory(updatedRun)
        
        currentShoe = shoeRepository.getShoeByIdOnce(shoeId)
        assertEquals("After update total should be 30.0", 30.0, currentShoe?.totalDistance ?: 0.0, 0.01)
        
        // Delete one run
        val runToDelete = historyRepository.getHistoryById(run1Id)!!
        historyRepository.deleteHistory(runToDelete)
        
        // Then - Verify final state
        val finalShoe = shoeRepository.getShoeByIdOnce(shoeId)
        val remainingHistory = historyRepository.getHistoryForShoe(shoeId).first()
        
        assertEquals("Final total should be 25.0", 25.0, finalShoe?.totalDistance ?: 0.0, 0.01) // 10 + 8 + 7
        assertEquals("Should have 2 remaining history records", 2, remainingHistory.size)
        
        val remainingDistances = remainingHistory.map { it.runDistance }.sorted()
        assertEquals("Remaining distances should be 7.0 and 8.0", listOf(7.0, 8.0), remainingDistances)
    }
}