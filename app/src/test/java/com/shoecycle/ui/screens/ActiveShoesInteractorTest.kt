package com.shoecycle.ui.screens

import com.shoecycle.ui.screens.active_shoes.ActiveShoesInteractor
import com.shoecycle.ui.screens.active_shoes.ActiveShoesState

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.MockShoeGenerator
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class ActiveShoesInteractorTest {

    private val mockShoeRepository = mock<IShoeRepository>()
    private val mockHistoryRepository = mock<IHistoryRepository>()
    private val mockUserSettingsRepository = mock<UserSettingsRepository>()

    private val testShoe = Shoe(
        id = 1L,
        brand = "Test Brand",
        maxDistance = 350.0,
        totalDistance = 100.0,
        startDistance = 50.0,
        startDate = Date(),
        expirationDate = Date(),
        orderingValue = 1.0,
        hallOfFame = false
    )

    private val testUserSettings = UserSettingsData(
        distanceUnit = DistanceUnit.MILES,
        selectedShoeId = 1L
    )

    @Test
    fun `when ViewAppeared action, should load shoes and user settings successfully`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        val shoes = listOf(testShoe)

        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(testUserSettings))
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(shoes))

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(shoes, state.value.shoes)
        assertEquals(DistanceUnit.MILES, state.value.distanceUnit)
        assertEquals(1L, state.value.selectedShoeId)
        assertFalse(state.value.isLoading)
    }

    @Test
    fun `when ViewAppeared action with empty shoes, should update state correctly`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        val emptyShoes = emptyList<Shoe>()

        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(testUserSettings))
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(emptyShoes))

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue(state.value.shoes.isEmpty())
        assertEquals(DistanceUnit.MILES, state.value.distanceUnit)
        assertEquals(1L, state.value.selectedShoeId)
        assertFalse(state.value.isLoading)
    }

    @Test
    fun `when ViewAppeared action with KM unit, should update state with correct unit`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        val kmSettings = testUserSettings.copy(distanceUnit = DistanceUnit.KM)

        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(kmSettings))
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(listOf(testShoe)))

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(DistanceUnit.KM, state.value.distanceUnit)
    }

    @Test
    fun `when ViewAppeared action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())

        whenever(mockUserSettingsRepository.userSettingsFlow).thenThrow(RuntimeException("Test exception"))

        // Act - Should not throw exception
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()

        // Assert - Loading should be set to false
        assertFalse(state.value.isLoading)
    }

    @Test
    fun `when ShoeSelected action, should call updateSelectedShoeId`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        val selectedShoeId = 5L

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.ShoeSelected(selectedShoeId))
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockUserSettingsRepository).updateSelectedShoeId(selectedShoeId)
    }

    @Test
    fun `when ShoeSelected action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        val selectedShoeId = 5L

        whenever(mockUserSettingsRepository.updateSelectedShoeId(selectedShoeId))
            .thenThrow(RuntimeException("Test exception"))

        // Act - Should not throw exception
        interactor.handle(state, ActiveShoesInteractor.Action.ShoeSelected(selectedShoeId))
        testScheduler.advanceUntilIdle()

        // Assert - Verify the call was made despite the exception
        verify(mockUserSettingsRepository).updateSelectedShoeId(selectedShoeId)
    }

    @Test
    fun `when GenerateTestData action, should set loading state and complete successfully`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())

        // Mock successful repository operations
        whenever(mockShoeRepository.insertShoe(any())).thenReturn(1L)
        whenever(mockHistoryRepository.insertHistory(any())).thenReturn(1L)

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.GenerateTestData)
        
        // Complete the coroutine
        testScheduler.advanceUntilIdle()

        // Assert - Loading should be false after completion
        assertFalse("Loading state should be false after generation", state.value.isGeneratingTestData)
    }

    @Test
    fun `when GenerateTestData action fails, should reset loading state`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())

        // Mock MockShoeGenerator failure by making repository fail
        whenever(mockShoeRepository.insertShoe(any())).thenThrow(RuntimeException("Test exception"))

        // Act - Should not throw exception
        interactor.handle(state, ActiveShoesInteractor.Action.GenerateTestData)
        testScheduler.advanceUntilIdle()

        // Assert - Loading should be false after failure
        assertFalse("Loading state should be false after generation failure", state.value.isGeneratingTestData)
    }

    @Test
    fun `initial state should have correct default values`() {
        // Arrange & Act
        val state = ActiveShoesState()

        // Assert
        assertTrue(state.shoes.isEmpty())
        assertTrue(state.isLoading)
        assertFalse(state.isGeneratingTestData)
        assertNull(state.selectedShoeId)
        assertEquals(DistanceUnit.MILES, state.distanceUnit)
    }

    @Test
    fun `when flows emit new values, should update state accordingly`() = runTest {
        // Arrange
        val interactor = ActiveShoesInteractor(
            mockShoeRepository, 
            mockHistoryRepository, 
            mockUserSettingsRepository, 
            this
        )
        val state = mutableStateOf(ActiveShoesState())
        
        val initialShoes = listOf(testShoe)
        val updatedShoes = listOf(testShoe, testShoe.copy(id = 2L))
        val updatedSettings = testUserSettings.copy(selectedShoeId = 2L)

        whenever(mockUserSettingsRepository.userSettingsFlow)
            .thenReturn(flowOf(testUserSettings, updatedSettings))
        whenever(mockShoeRepository.getActiveShoes())
            .thenReturn(flowOf(initialShoes, updatedShoes))

        // Act
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        testScheduler.advanceUntilIdle()

        // Assert - Should reflect the latest values from both flows
        assertEquals(2, state.value.shoes.size)
        assertEquals(2L, state.value.selectedShoeId)
        assertFalse(state.value.isLoading)
    }
}