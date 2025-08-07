package com.shoecycle.ui.settings
import com.shoecycle.ui.screens.settings.*

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.FirstDayOfWeek
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

class SettingsFirstDayInteractorTest {

    private val mockRepository = mock<UserSettingsRepository>()

    @Test
    fun `when DayChanged action with different day, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsFirstDayInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsFirstDayState(selectedDay = FirstDayOfWeek.MONDAY))
        val newDay = FirstDayOfWeek.SUNDAY

        // Act
        interactor.handle(state, SettingsFirstDayInteractor.Action.DayChanged(newDay))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(FirstDayOfWeek.SUNDAY, state.value.selectedDay)
        verify(mockRepository).updateFirstDayOfWeek(newDay)
    }

    @Test
    fun `when DayChanged action with same day, should not update state or call repository`() = runTest {
        // Arrange
        val interactor = SettingsFirstDayInteractor(mockRepository, this)
        val initialDay = FirstDayOfWeek.MONDAY
        val state = mutableStateOf(SettingsFirstDayState(selectedDay = initialDay))

        // Act
        interactor.handle(state, SettingsFirstDayInteractor.Action.DayChanged(initialDay))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert - State should remain unchanged and no repository call
        assertEquals(initialDay, state.value.selectedDay)
        verify(mockRepository, org.mockito.kotlin.never()).updateFirstDayOfWeek(org.mockito.kotlin.any())
    }

    @Test
    fun `when ViewAppeared action, should load initial state from repository`() = runTest {
        // Arrange
        val interactor = SettingsFirstDayInteractor(mockRepository, this)
        val expectedDay = FirstDayOfWeek.SUNDAY
        val mockUserSettings = UserSettingsData(firstDayOfWeek = expectedDay)
        whenever(mockRepository.userSettingsFlow).thenReturn(flowOf(mockUserSettings))
        
        val state = mutableStateOf(SettingsFirstDayState())

        // Act
        interactor.handle(state, SettingsFirstDayInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(expectedDay, state.value.selectedDay)
    }

    @Test
    fun `when ViewAppeared action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = SettingsFirstDayInteractor(mockRepository, this)
        whenever(mockRepository.userSettingsFlow).thenThrow(RuntimeException("Test exception"))
        val state = mutableStateOf(SettingsFirstDayState())
        val initialState = state.value

        // Act - Should not throw exception
        interactor.handle(state, SettingsFirstDayInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert - State should remain unchanged
        assertEquals(initialState, state.value)
    }
}