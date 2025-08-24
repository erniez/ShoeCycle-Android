package com.shoecycle.ui.screens.add_distance

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class AddDistanceInteractorTest {

    private val mockShoeRepository = mock<IShoeRepository>()
    private val mockHistoryRepository = mock<IHistoryRepository>()
    private val mockUserSettingsRepository = mock<UserSettingsRepository>()
    private val mockSelectedShoeStrategy = mock<SelectedShoeStrategy>()
    
    private fun createTestShoe(id: Long = 1L, brand: String = "Test Shoe"): Shoe {
        return Shoe(
            id = id,
            brand = brand,
            maxDistance = 500.0,
            totalDistance = 100.0,
            startDistance = 0.0,
            startDate = Date(),
            expirationDate = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000), // 1 year from now
            imageKey = null,
            thumbnailData = null,
            orderingValue = id.toDouble(),
            hallOfFame = false
        )
    }


    @Test
    fun `SwipeUp action should select next shoe`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoes = listOf(createTestShoe(1), createTestShoe(2), createTestShoe(3))
        val state = mutableStateOf(
            AddDistanceState(
                activeShoes = testShoes,
                selectedShoeIndex = 0,
                selectedShoe = testShoes[0]
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.SwipeUp)
        
        // Then
        assertEquals(1, state.value.selectedShoeIndex)
        assertEquals(testShoes[1], state.value.selectedShoe)
    }


    @Test
    fun `SwipeDown action should select previous shoe`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoes = listOf(createTestShoe(1), createTestShoe(2), createTestShoe(3))
        val state = mutableStateOf(
            AddDistanceState(
                activeShoes = testShoes,
                selectedShoeIndex = 2,
                selectedShoe = testShoes[2]
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.SwipeDown)
        
        // Then
        assertEquals(1, state.value.selectedShoeIndex)
        assertEquals(testShoes[1], state.value.selectedShoe)
    }

    @Test
    fun `SelectShoeAtIndex action should select specific shoe`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoes = listOf(createTestShoe(1), createTestShoe(2), createTestShoe(3))
        val state = mutableStateOf(
            AddDistanceState(
                activeShoes = testShoes,
                selectedShoeIndex = 0,
                selectedShoe = testShoes[0]
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.SelectShoeAtIndex(2))
        
        // Then
        assertEquals(2, state.value.selectedShoeIndex)
        assertEquals(testShoes[2], state.value.selectedShoe)
    }

    @Test
    fun `DateChanged action should update run date`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(AddDistanceState())
        val newDate = Date(System.currentTimeMillis() - 86400000) // Yesterday
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.DateChanged(newDate))
        
        // Then
        assertEquals(newDate, state.value.runDate)
    }

    @Test
    fun `DistanceChanged action should update run distance`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(AddDistanceState())
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.DistanceChanged("5.5"))
        
        // Then
        assertEquals("5.5", state.value.runDistance)
    }


    @Test
    fun `AddRunClicked should convert kilometers to miles for storage`() = runTest {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoe = createTestShoe(1)
        val testDate = Date()
        val state = mutableStateOf(
            AddDistanceState(
                activeShoes = listOf(testShoe),
                selectedShoe = testShoe,
                runDistance = "10.0",
                runDate = testDate,
                distanceUnit = DistanceUnit.KM // User entered 10 km
            )
        )
        
        whenever(mockHistoryRepository.addRun(any(), any(), any())).thenReturn(124L)
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(listOf(testShoe)))
        whenever(mockSelectedShoeStrategy.getSelectedShoe()).thenReturn(testShoe)
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.AddRunClicked)
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then - 10 km = 6.21371 miles
        // Using ArgumentCaptor to capture the actual value
        val distanceCaptor = ArgumentCaptor.forClass(Double::class.java)
        verify(mockHistoryRepository).addRun(
            shoeId = eq(1L),
            runDate = eq(testDate),
            runDistance = distanceCaptor.capture()
        )
        
        val capturedDistance = distanceCaptor.value
        assertTrue("Distance should be around 6.21 miles but was $capturedDistance", 
                   capturedDistance > 6.2 && capturedDistance < 6.22)
    }

    @Test
    fun `AddRunClicked should not add run if no shoe selected`() = runTest {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(
            AddDistanceState(
                selectedShoe = null,
                runDistance = "5.0"
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify(mockHistoryRepository, never()).addRun(any(), any(), any())
        verify(mockShoeRepository, never()).recalculateShoeTotal(any())
    }

    @Test
    fun `AddRunClicked should not add run if distance is invalid`() = runTest {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoe = createTestShoe(1)
        val state = mutableStateOf(
            AddDistanceState(
                selectedShoe = testShoe,
                runDistance = "invalid"
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify(mockHistoryRepository, never()).addRun(any(), any(), any())
        verify(mockShoeRepository, never()).recalculateShoeTotal(any())
    }

    @Test
    fun `AddRunClicked should not add run if distance is zero or negative`() = runTest {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val testShoe = createTestShoe(1)
        
        // Test zero
        val stateZero = mutableStateOf(
            AddDistanceState(
                selectedShoe = testShoe,
                runDistance = "0"
            )
        )
        
        // When
        interactor.handle(stateZero, AddDistanceInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify(mockHistoryRepository, never()).addRun(any(), any(), any())
        
        // Test negative
        val stateNegative = mutableStateOf(
            AddDistanceState(
                selectedShoe = testShoe,
                runDistance = "-5"
            )
        )
        
        // When
        interactor.handle(stateNegative, AddDistanceInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify(mockHistoryRepository, never()).addRun(any(), any(), any())
    }

    @Test
    fun `ShowHistoryModal action should set showHistoryModal to true`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(AddDistanceState(showHistoryModal = false))
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.ShowHistoryModal)
        
        // Then
        assertTrue(state.value.showHistoryModal)
    }

    @Test
    fun `HideHistoryModal action should set showHistoryModal to false`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(AddDistanceState(showHistoryModal = true))
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.HideHistoryModal)
        
        // Then
        assertFalse(state.value.showHistoryModal)
    }

    @Test
    fun `FavoriteDistanceSelected action should update run distance`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(
            AddDistanceState(
                runDistance = "",
                distanceUnit = DistanceUnit.MILES
            )
        )
        
        // When
        interactor.handle(state, AddDistanceInteractor.Action.FavoriteDistanceSelected(3.5))
        
        // Then
        assertEquals("3.5", state.value.runDistance)
    }

    @Test
    fun `FavoriteDistanceSelected should convert to display unit`() {
        // Given
        val interactor = AddDistanceInteractor(
            mockShoeRepository,
            mockHistoryRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy
        )
        val state = mutableStateOf(
            AddDistanceState(
                runDistance = "",
                distanceUnit = DistanceUnit.KM // Display in KM
            )
        )
        
        // When - 5 miles selected as favorite
        interactor.handle(state, AddDistanceInteractor.Action.FavoriteDistanceSelected(5.0))
        
        // Then - Should be converted to km for display (5 miles = 8.05 km)
        assertEquals("8.05", state.value.runDistance)
    }
}