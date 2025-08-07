package com.shoecycle.ui.settings
import com.shoecycle.ui.screens.settings.*

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

class SettingsFavoriteDistancesInteractorTest {

    private val mockRepository = mock<UserSettingsRepository>()

    @Test
    fun `when FavoriteChanged action for index 1 with different distance, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsFavoriteDistancesState(favorite1 = 0.0))
        val newDistance = 5.0

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(1, newDistance))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(5.0, state.value.favorite1, 0.01)
        verify(mockRepository).updateFavorite1(newDistance)
    }

    @Test
    fun `when FavoriteChanged action for index 2 with different distance, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsFavoriteDistancesState(favorite2 = 0.0))
        val newDistance = 10.0

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(2, newDistance))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(10.0, state.value.favorite2, 0.01)
        verify(mockRepository).updateFavorite2(newDistance)
    }

    @Test
    fun `when FavoriteChanged action for index 3 with different distance, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsFavoriteDistancesState(favorite3 = 0.0))
        val newDistance = 13.1

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(3, newDistance))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(13.1, state.value.favorite3, 0.01)
        verify(mockRepository).updateFavorite3(newDistance)
    }

    @Test
    fun `when FavoriteChanged action for index 4 with different distance, should update state and call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val state = mutableStateOf(SettingsFavoriteDistancesState(favorite4 = 0.0))
        val newDistance = 26.2

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(4, newDistance))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert
        assertEquals(26.2, state.value.favorite4, 0.01)
        verify(mockRepository).updateFavorite4(newDistance)
    }

    @Test
    fun `when FavoriteChanged action with same distance, should not update state or call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val initialDistance = 5.0
        val state = mutableStateOf(SettingsFavoriteDistancesState(favorite1 = initialDistance))

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(1, initialDistance))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert - State should remain unchanged and no repository call
        assertEquals(initialDistance, state.value.favorite1, 0.01)
        verify(mockRepository, org.mockito.kotlin.never()).updateFavorite1(org.mockito.kotlin.any())
    }

    @Test
    fun `when FavoriteChanged action with invalid index, should not change state or call repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val initialState = SettingsFavoriteDistancesState(favorite1 = 1.0, favorite2 = 2.0, favorite3 = 3.0, favorite4 = 4.0)
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(5, 10.0))
        testScheduler.advanceUntilIdle() // Execute the coroutine

        // Assert - State should remain unchanged
        assertEquals(initialState, state.value)
        verify(mockRepository, org.mockito.kotlin.never()).updateFavorite1(org.mockito.kotlin.any())
        verify(mockRepository, org.mockito.kotlin.never()).updateFavorite2(org.mockito.kotlin.any())
        verify(mockRepository, org.mockito.kotlin.never()).updateFavorite3(org.mockito.kotlin.any())
        verify(mockRepository, org.mockito.kotlin.never()).updateFavorite4(org.mockito.kotlin.any())
    }

    @Test
    fun `when ViewAppeared action, should load initial state from repository`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        val mockUserSettings = UserSettingsData(
            favorite1 = 5.0,
            favorite2 = 10.0,
            favorite3 = 13.1,
            favorite4 = 26.2
        )
        whenever(mockRepository.userSettingsFlow).thenReturn(flowOf(mockUserSettings))
        
        val state = mutableStateOf(SettingsFavoriteDistancesState())

        // Act
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(5.0, state.value.favorite1, 0.01)
        assertEquals(10.0, state.value.favorite2, 0.01)
        assertEquals(13.1, state.value.favorite3, 0.01)
        assertEquals(26.2, state.value.favorite4, 0.01)
    }

    @Test
    fun `when ViewAppeared action fails, should handle exception gracefully`() = runTest {
        // Arrange
        val interactor = SettingsFavoriteDistancesInteractor(mockRepository, this)
        whenever(mockRepository.userSettingsFlow).thenThrow(RuntimeException("Test exception"))
        val state = mutableStateOf(SettingsFavoriteDistancesState())
        val initialState = state.value

        // Act - Should not throw exception
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.ViewAppeared)
        
        // Advance coroutine execution
        testScheduler.advanceUntilIdle()

        // Assert - State should remain unchanged
        assertEquals(initialState, state.value)
    }
}