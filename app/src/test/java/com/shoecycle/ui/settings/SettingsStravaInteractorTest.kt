package com.shoecycle.ui.settings

import androidx.compose.runtime.mutableStateOf
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

class SettingsStravaInteractorTest {

    private val mockRepository = mock<UserSettingsRepository>()

    @Test
    fun `when ToggleChanged action with different enabled state, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsStravaInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsStravaState(enabled = false))
        val newEnabled = true

        // Act
        interactor.handle(state, SettingsStravaInteractor.Action.ToggleChanged(newEnabled))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(true, state.value.enabled)
        verify(mockRepository).updateStravaEnabled(newEnabled)
    }

    @Test
    fun `when ToggleChanged action from enabled to disabled, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsStravaInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsStravaState(enabled = true))
        val newEnabled = false

        // Act
        interactor.handle(state, SettingsStravaInteractor.Action.ToggleChanged(newEnabled))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(false, state.value.enabled)
        verify(mockRepository).updateStravaEnabled(newEnabled)
    }

    @Test
    fun `when ToggleChanged action with same enabled state, should not update state or call repository`() = runTest {
        // Arrange
        val interactor = SettingsStravaInteractor(mockRepository, this)
        val initialEnabled = true
        val state = mutableStateOf(SettingsStravaState(enabled = initialEnabled))

        // Act
        interactor.handle(state, SettingsStravaInteractor.Action.ToggleChanged(initialEnabled))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert - State should remain unchanged and no repository call
        assertEquals(initialEnabled, state.value.enabled)
        verify(mockRepository, org.mockito.kotlin.never()).updateStravaEnabled(org.mockito.kotlin.any())
    }

    @Test
    fun `when ViewAppeared action, should load initial state from repository`() = runTest {
        // Arrange
        val interactor = SettingsStravaInteractor(mockRepository, this)
        val expectedEnabled = true
        val mockUserSettings = UserSettingsData(stravaEnabled = expectedEnabled)
        whenever(mockRepository.userSettingsFlow).thenReturn(flowOf(mockUserSettings))
        
        val state = mutableStateOf(SettingsStravaState())

        // Act
        interactor.handle(state, SettingsStravaInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(expectedEnabled, state.value.enabled)
    }

    @Test
    fun `when ViewAppeared action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = SettingsStravaInteractor(mockRepository, this)
        whenever(mockRepository.userSettingsFlow).thenThrow(RuntimeException("Test exception"))
        val state = mutableStateOf(SettingsStravaState())
        val initialState = state.value

        // Act - Should not throw exception
        interactor.handle(state, SettingsStravaInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert - State should remain unchanged
        assertEquals(initialState, state.value)
    }
}