package com.shoecycle.ui.settings

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsUnitsInteractorTest {

    private val mockRepository = mock<UserSettingsRepository>()

    @Test
    fun `when UnitChanged action with different unit, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsUnitsInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsUnitsState(selectedUnit = DistanceUnit.MILES))
        val newUnit = DistanceUnit.KM

        // Act
        interactor.handle(state, SettingsUnitsInteractor.Action.UnitChanged(newUnit))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(DistanceUnit.KM, state.value.selectedUnit)
        verify(mockRepository).updateDistanceUnit(newUnit)
    }

    @Test
    fun `when UnitChanged action with same unit, should not update state or call repository`() = runTest {
        // Arrange
        val interactor = SettingsUnitsInteractor(mockRepository, this)
        val initialUnit = DistanceUnit.MILES
        val state = mutableStateOf(SettingsUnitsState(selectedUnit = initialUnit))

        // Act
        interactor.handle(state, SettingsUnitsInteractor.Action.UnitChanged(initialUnit))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert - State should remain unchanged and no repository call
        assertEquals(initialUnit, state.value.selectedUnit)
        verify(mockRepository, org.mockito.kotlin.never()).updateDistanceUnit(org.mockito.kotlin.any())
    }

    @Test
    fun `when ViewAppeared action, should load initial state from repository`() = runTest {
        // Arrange
        val interactor = SettingsUnitsInteractor(mockRepository, this)
        val expectedUnit = DistanceUnit.KM
        val mockUserSettings = UserSettingsData(distanceUnit = expectedUnit)
        whenever(mockRepository.userSettingsFlow).thenReturn(flowOf(mockUserSettings))
        
        val state = mutableStateOf(SettingsUnitsState())

        // Act
        interactor.handle(state, SettingsUnitsInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(expectedUnit, state.value.selectedUnit)
    }

    @Test
    fun `when ViewAppeared action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = SettingsUnitsInteractor(mockRepository, this)
        whenever(mockRepository.userSettingsFlow).thenThrow(RuntimeException("Test exception"))
        val state = mutableStateOf(SettingsUnitsState())
        val initialState = state.value

        // Act - Should not throw exception
        interactor.handle(state, SettingsUnitsInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert - State should remain unchanged
        assertEquals(initialState, state.value)
    }
}