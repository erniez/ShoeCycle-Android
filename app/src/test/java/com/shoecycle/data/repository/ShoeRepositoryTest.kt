package com.shoecycle.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.shoecycle.data.TestDatabaseSetup
import com.shoecycle.data.TestDataFactory
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ShoeRepositoryTest {

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
    fun insertShoe_returnsValidId() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Nike Air Zoom")
        
        // When
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // Then
        assertNotNull("Inserted ID should not be null", insertedId)
        assertTrue("Inserted ID should not be empty", insertedId.isNotEmpty())
    }

    @Test
    fun insertShoe_canBeRetrievedById() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Nike Air Zoom", maxDistance = 400.0)
        
        // When
        val insertedId = shoeRepository.insertShoe(shoe)
        val retrievedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Retrieved shoe should not be null", retrievedShoe)
        assertEquals("Brand should match", "Nike Air Zoom", retrievedShoe?.brand)
        assertEquals("Max distance should match", 400.0, retrievedShoe?.maxDistance ?: 0.0, 0.01)
        assertEquals("ID should match", insertedId, retrievedShoe?.id)
    }

    @Test
    fun updateShoe_persistsChanges() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Original Brand")
        val insertedId = shoeRepository.insertShoe(shoe)
        val updatedShoe = shoe.copy(id = insertedId, brand = "Updated Brand", totalDistance = 50.0)
        
        // When
        shoeRepository.updateShoe(updatedShoe)
        val retrievedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Retrieved shoe should not be null", retrievedShoe)
        assertEquals("Brand should be updated", "Updated Brand", retrievedShoe?.brand)
        assertEquals("Total distance should be updated", 50.0, retrievedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun deleteShoe_removesFromDatabase() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val insertedId = shoeRepository.insertShoe(shoe)
        val shoeToDelete = shoe.copy(id = insertedId)
        
        // When
        shoeRepository.deleteShoe(shoeToDelete)
        val retrievedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNull("Deleted shoe should not be retrievable", retrievedShoe)
    }

    // Flow Observation Tests

    @Test
    fun getAllShoes_emitsUpdatesOnInsert() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Test Shoe")
        
        // When/Then
        shoeRepository.getAllShoes().test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<Shoe>(), awaitItem())
            
            // Insert shoe
            val insertedId = shoeRepository.insertShoe(shoe)
            val expectedShoe = shoe.copy(id = insertedId)
            
            // Verify emission
            val emittedShoes = awaitItem()
            assertEquals("Should emit one shoe", 1, emittedShoes.size)
            assertEquals("Shoe brand should match", "Test Shoe", emittedShoes[0].brand)
        }
    }

    @Test
    fun getActiveShoes_filtersCorrectly() = runTest {
        // Given
        val activeShoe = TestDataFactory.createTestShoe(brand = "Active Shoe", hallOfFame = false)
        val retiredShoe = TestDataFactory.createTestShoe(brand = "Retired Shoe", hallOfFame = true)
        
        // When/Then
        shoeRepository.getActiveShoes().test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<Shoe>(), awaitItem())
            
            // Insert active shoe first
            val activeId = shoeRepository.insertShoe(activeShoe)
            val afterActive = awaitItem()
            assertEquals("Should emit active shoe", 1, afterActive.size)
            assertEquals("Should be the active shoe", "Active Shoe", afterActive[0].brand)
            
            // Insert retired shoe (should not affect active shoes flow)
            val retiredId = shoeRepository.insertShoe(retiredShoe)
            val afterRetired = awaitItem()
            assertEquals("Should still emit only active shoe", 1, afterRetired.size)
            assertEquals("Should still be the active shoe", "Active Shoe", afterRetired[0].brand)
            assertFalse("Should not be in hall of fame", afterRetired[0].hallOfFame)
        }
    }

    @Test
    fun getRetiredShoes_filtersCorrectly() = runTest {
        // Given
        val activeShoe = TestDataFactory.createTestShoe(brand = "Active Shoe", hallOfFame = false)
        val retiredShoe = TestDataFactory.createTestShoe(brand = "Retired Shoe", hallOfFame = true)
        
        // When/Then
        shoeRepository.getRetiredShoes().test {
            // Initial empty state
            assertEquals("Initial state should be empty", emptyList<Shoe>(), awaitItem())
            
            // Insert active shoe first (should not emit anything for retired shoes flow)
            shoeRepository.insertShoe(activeShoe)
            val afterActive = awaitItem()
            assertEquals("Should not emit any retired shoes yet", 0, afterActive.size)
            
            // Insert retired shoe
            shoeRepository.insertShoe(retiredShoe)
            val afterRetired = awaitItem()
            assertEquals("Should emit only retired shoe", 1, afterRetired.size)
            assertEquals("Should be the retired shoe", "Retired Shoe", afterRetired[0].brand)
            assertTrue("Should be in hall of fame", afterRetired[0].isRetired)
        }
    }

    @Test
    fun getShoeById_emitsUpdatesOnChange() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(brand = "Original Brand")
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When/Then
        shoeRepository.getShoeById(insertedId).test {
            // Initial state after insert
            val initialShoe = awaitItem()
            assertNotNull("Initial shoe should not be null", initialShoe)
            assertEquals("Initial brand should match", "Original Brand", initialShoe?.brand)
            
            // Update shoe
            val updatedShoe = shoe.copy(id = insertedId, brand = "Updated Brand")
            shoeRepository.updateShoe(updatedShoe)
            
            // Verify update emission
            val updatedEmission = awaitItem()
            assertEquals("Updated brand should match", "Updated Brand", updatedEmission?.brand)
        }
    }

    // Business Logic Tests

    @Test
    fun createShoe_setsDefaultValues() = runTest {
        // When
        val insertedId = shoeRepository.createShoe("New Balance 990", 500.0)
        val createdShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Created shoe should not be null", createdShoe)
        assertEquals("Brand should match", "New Balance 990", createdShoe?.brand)
        assertEquals("Max distance should match", 500.0, createdShoe?.maxDistance ?: 0.0, 0.01)
        assertEquals("Total distance should be 0", 0.0, createdShoe?.totalDistance ?: 0.0, 0.01)
        assertEquals("Start distance should be 0", 0.0, createdShoe?.startDistance ?: 0.0, 0.01)
        assertFalse("Should not be retired", createdShoe?.hallOfFame == true)
        assertTrue("Ordering value should be set", (createdShoe?.orderingValue ?: 0.0) > 0)
    }

    @Test
    fun retireShoe_updatesHallOfFameStatus() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(hallOfFame = false)
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When
        shoeRepository.retireShoe(insertedId)
        val retiredShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Retired shoe should still exist", retiredShoe)
        assertTrue("Shoe should be in hall of fame", retiredShoe?.hallOfFame == true)
        assertTrue("isRetired should be true", retiredShoe?.isRetired == true)
        assertFalse("isActive should be false", retiredShoe?.isActive == true)
    }

    @Test
    fun reactivateShoe_updatesHallOfFameStatus() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(hallOfFame = true)
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When
        shoeRepository.reactivateShoe(insertedId)
        val reactivatedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Reactivated shoe should still exist", reactivatedShoe)
        assertFalse("Shoe should not be in hall of fame", reactivatedShoe?.hallOfFame == true)
        assertFalse("isRetired should be false", reactivatedShoe?.isRetired == true)
        assertTrue("isActive should be true", reactivatedShoe?.isActive == true)
    }

    @Test
    fun updateTotalDistance_persistsChanges() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe()
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When
        shoeRepository.updateTotalDistance(insertedId, 125.5)
        val updatedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Updated shoe should exist", updatedShoe)
        assertEquals("Total distance should be updated", 125.5, updatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    // Count Tests

    @Test
    fun getActiveShoesCount_returnsCorrectCount() = runTest {
        // Given
        val activeShoes = TestDataFactory.createMultipleTestShoes(3, hallOfFame = false)
        val retiredShoes = TestDataFactory.createMultipleTestShoes(2, hallOfFame = true)
        
        // When
        activeShoes.forEach { shoeRepository.insertShoe(it) }
        retiredShoes.forEach { shoeRepository.insertShoe(it) }
        val activeCount = shoeRepository.getActiveShoesCount()
        
        // Then
        assertEquals("Active shoes count should be 3", 3, activeCount)
    }

    @Test
    fun getRetiredShoesCount_returnsCorrectCount() = runTest {
        // Given
        val activeShoes = TestDataFactory.createMultipleTestShoes(3, hallOfFame = false)
        val retiredShoes = TestDataFactory.createMultipleTestShoes(2, hallOfFame = true)
        
        // When
        activeShoes.forEach { shoeRepository.insertShoe(it) }
        retiredShoes.forEach { shoeRepository.insertShoe(it) }
        val retiredCount = shoeRepository.getRetiredShoesCount()
        
        // Then
        assertEquals("Retired shoes count should be 2", 2, retiredCount)
    }

    // Edge Cases and Error Handling

    @Test
    fun getShoeByIdOnce_withInvalidId_returnsNull() = runTest {
        // When
        val retrievedShoe = shoeRepository.getShoeByIdOnce("invalid-id-999")
        
        // Then
        assertNull("Non-existent shoe should return null", retrievedShoe)
    }

    @Test
    fun retireShoe_withInvalidId_doesNotThrow() = runTest {
        // When/Then - Should not throw exception
        shoeRepository.retireShoe("invalid-id-999")
        // Test passes if no exception is thrown
    }

    @Test
    fun updateTotalDistance_withInvalidId_doesNotThrow() = runTest {
        // When/Then - Should not throw exception
        shoeRepository.updateTotalDistance("invalid-id-999", 100.0)
        // Test passes if no exception is thrown
    }

    // Ordering Tests

    @Test
    fun updateShoeOrdering_persistsNewValue() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(orderingValue = 1.0)
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When
        shoeRepository.updateShoeOrdering(insertedId, 5.0)
        val updatedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Updated shoe should exist", updatedShoe)
        assertEquals("Ordering value should be updated", 5.0, updatedShoe?.orderingValue ?: 0.0, 0.01)
    }

    @Test
    fun getNextOrderingValue_returnsUniqueValue() = runTest {
        // Given - empty database should return 1.0
        val initialValue = shoeRepository.getNextOrderingValue()
        assertEquals("Initial ordering value should be 1.0", 1.0, initialValue, 0.01)
        
        // When - insert a shoe with ordering value 1.0
        val shoe = TestDataFactory.createTestShoe(orderingValue = 1.0)
        shoeRepository.insertShoe(shoe)
        
        // Then - next value should be 2.0
        val nextValue = shoeRepository.getNextOrderingValue()
        assertEquals("Next ordering value should be 2.0", 2.0, nextValue, 0.01)
        
        // Insert another shoe and verify increment continues
        val shoe2 = TestDataFactory.createTestShoe(orderingValue = 2.0)
        shoeRepository.insertShoe(shoe2)
        
        val thirdValue = shoeRepository.getNextOrderingValue()
        assertEquals("Third ordering value should be 3.0", 3.0, thirdValue, 0.01)
    }

    // Integration with History Tests

    @Test
    fun recalculateShoeTotal_withNoHistory_setsToStartDistance() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 25.0, totalDistance = 100.0)
        val insertedId = shoeRepository.insertShoe(shoe)
        
        // When
        shoeRepository.recalculateShoeTotal(insertedId)
        val recalculatedShoe = shoeRepository.getShoeByIdOnce(insertedId)
        
        // Then
        assertNotNull("Recalculated shoe should exist", recalculatedShoe)
        assertEquals("Total should equal start distance when no history", 25.0, recalculatedShoe?.totalDistance ?: 0.0, 0.01)
    }

    @Test
    fun recalculateShoeTotal_withHistory_updatesCorrectly() = runTest {
        // Given
        val shoe = TestDataFactory.createTestShoe(startDistance = 10.0)
        val shoeId = shoeRepository.insertShoe(shoe)
        
        // Add some history
        val histories = TestDataFactory.createMultipleTestHistories(shoeId, 3, distanceRange = 5.0)
        histories.forEach { historyRepository.insertHistory(it) }
        
        // When
        shoeRepository.recalculateShoeTotal(shoeId)
        val recalculatedShoe = shoeRepository.getShoeByIdOnce(shoeId)
        
        // Then
        assertNotNull("Recalculated shoe should exist", recalculatedShoe)
        // Expected: start distance (10.0) + history distances (5.0 + 5.5 + 6.0) = 26.5
        assertEquals("Total should include start distance plus history", 26.5, recalculatedShoe?.totalDistance ?: 0.0, 0.01)
    }
}