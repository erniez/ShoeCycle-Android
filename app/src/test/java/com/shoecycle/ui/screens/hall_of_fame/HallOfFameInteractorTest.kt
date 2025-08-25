package com.shoecycle.ui.screens.hall_of_fame

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class HallOfFameInteractorTest {

    private val mockShoeRepository = mock<IShoeRepository>()
    private val mockUserSettingsRepository = mock<UserSettingsRepository>()
    
    private fun createTestShoe(id: String = "test-shoe-1", brand: String = "Test Shoe", totalDistance: Double = 100.0): Shoe {
        return Shoe(
            id = id,
            brand = brand,
            maxDistance = 300.0,
            totalDistance = totalDistance,
            startDistance = 0.0,
            startDate = Date(),
            expirationDate = Date(),
            orderingValue = 1.0,
            hallOfFame = true // These are hall of fame shoes
        )
    }

    // Given: Fresh interactor and state
    // When: ViewAppeared action is handled
    // Then: Should load hall of fame shoes from repository
    @Test
    fun testViewAppearedLoadsHallOfFameShoes() = runTest {
        val testShoes = listOf(
            createTestShoe("test-shoe-1", "Nike Air", 250.0),
            createTestShoe("test-shoe-2", "Adidas Ultra", 180.0)
        )
        
        whenever(mockShoeRepository.getRetiredShoes()).thenReturn(flowOf(testShoes))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        
        // Wait for async operations to complete
        testScheduler.advanceUntilIdle()
        
        assertFalse("Should not be loading after data loads", state.value.isLoading)
        assertEquals("Should have 2 shoes", 2, state.value.shoes.size)
        assertEquals("First shoe should be Nike Air", "Nike Air", state.value.shoes[0].brand)
        assertEquals("Second shoe should be Adidas Ultra", "Adidas Ultra", state.value.shoes[1].brand)
        assertEquals("Distance unit should be miles", DistanceUnit.MILES, state.value.distanceUnit)
        assertNull("Should have no error", state.value.errorMessage)
    }

    // Given: Interactor with empty hall of fame
    // When: ViewAppeared action is handled
    // Then: Should handle empty state correctly
    @Test
    fun testViewAppearedHandlesEmptyHallOfFame() = runTest {
        whenever(mockShoeRepository.getRetiredShoes()).thenReturn(flowOf(emptyList()))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.KM)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        
        testScheduler.advanceUntilIdle()
        
        assertFalse("Should not be loading", state.value.isLoading)
        assertTrue("Should have empty shoes list", state.value.shoes.isEmpty())
        assertEquals("Distance unit should be km", DistanceUnit.KM, state.value.distanceUnit)
        assertNull("Should have no error", state.value.errorMessage)
    }

    // Given: Repository that throws exception
    // When: ViewAppeared action is handled
    // Then: Should handle error gracefully
    @Test
    fun testViewAppearedHandlesRepositoryError() = runTest {
        val errorMessage = "Database connection failed"
        whenever(mockShoeRepository.getRetiredShoes()).thenThrow(RuntimeException(errorMessage))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        
        testScheduler.advanceUntilIdle()
        
        assertFalse("Should not be loading after error", state.value.isLoading)
        assertTrue("Should have empty shoes list on error", state.value.shoes.isEmpty())
        assertTrue("Should have error message", state.value.errorMessage?.contains("Error loading shoes") == true)
        assertTrue("Should contain original error", state.value.errorMessage?.contains(errorMessage) == true)
    }

    // Given: Interactor with loaded data
    // When: Refresh action is handled
    // Then: Should reload data from repository
    @Test
    fun testRefreshReloadsData() = runTest {
        val initialShoes = listOf(createTestShoe("test-shoe-1", "Old Shoe"))
        val refreshedShoes = listOf(
            createTestShoe("test-shoe-1", "Old Shoe"),
            createTestShoe("test-shoe-2", "New Shoe")
        )
        
        whenever(mockShoeRepository.getRetiredShoes())
            .thenReturn(flowOf(initialShoes))
            .thenReturn(flowOf(refreshedShoes))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        // Initial load
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()
        assertEquals("Should have 1 shoe initially", 1, state.value.shoes.size)
        
        // Refresh
        interactor.handle(state, HallOfFameInteractor.Action.Refresh)
        testScheduler.advanceUntilIdle()
        
        assertEquals("Should have 2 shoes after refresh", 2, state.value.shoes.size)
        verify(mockShoeRepository, times(2)).getRetiredShoes()
    }

    // Given: Interactor with initial state
    // When: ViewAppeared action starts loading
    // Then: Should set loading state to true initially
    @Test
    fun testLoadingStateManagement() = runTest {
        whenever(mockShoeRepository.getRetiredShoes()).thenReturn(flowOf(emptyList()))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        // Verify initial state
        assertTrue("Should start loading", state.value.isLoading)
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()
        
        // After completion, should not be loading
        assertFalse("Should not be loading after completion", state.value.isLoading)
        assertNull("Should have no error after successful load", state.value.errorMessage)
    }

    // Given: Multiple shoes with different distances
    // When: Data is loaded
    // Then: Should preserve shoe order from repository
    @Test
    fun testShoeOrderingFromRepository() = runTest {
        val testShoes = listOf(
            createTestShoe("test-shoe-1", "First Shoe", 300.0),
            createTestShoe("test-shoe-2", "Second Shoe", 150.0),
            createTestShoe("test-shoe-3", "Third Shoe", 450.0)
        )
        
        whenever(mockShoeRepository.getRetiredShoes()).thenReturn(flowOf(testShoes))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()
        
        assertEquals("Should maintain repository order", "First Shoe", state.value.shoes[0].brand)
        assertEquals("Should maintain repository order", "Second Shoe", state.value.shoes[1].brand)
        assertEquals("Should maintain repository order", "Third Shoe", state.value.shoes[2].brand)
    }

    // Given: Different distance units
    // When: Data is loaded
    // Then: Should correctly get unit label from DistanceUtility
    @Test
    fun testDistanceUnitHandling() = runTest {
        whenever(mockShoeRepository.getRetiredShoes()).thenReturn(flowOf(emptyList()))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.KM)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        
        val interactor = HallOfFameInteractor(
            mockShoeRepository, 
            mockUserSettingsRepository,
            this
        )
        val state = mutableStateOf(HallOfFameState())
        
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()
        
        assertEquals("Should use correct distance unit", DistanceUnit.KM, state.value.distanceUnit)
        verify(mockUserSettingsRepository, atLeastOnce()).userSettingsFlow
    }
}