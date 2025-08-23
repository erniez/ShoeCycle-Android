package com.shoecycle.ui.screens.add_distance.components

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.shoecycle.domain.services.HealthService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class DateDistanceEntryInteractorTest {

    private val mockHealthService = mock<HealthService>()
    private val context: Context = RuntimeEnvironment.getApplication()
    
    @Test
    fun `DateChanged action should update date in state`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState())
        val newDate = Date(System.currentTimeMillis() - 86400000) // Yesterday
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.DateChanged(newDate))
        
        // Then
        assertEquals(newDate, state.value.runDate)
    }

    @Test
    fun `DistanceChanged action should update distance in state`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState())
        val newDistance = "5.5"
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.DistanceChanged(newDistance))
        
        // Then
        assertEquals(newDistance, state.value.runDistance)
    }

    @Test
    fun `ShowDatePicker action should set showDatePicker to true`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showDatePicker = false))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.ShowDatePicker)
        
        // Then
        assertTrue(state.value.showDatePicker)
    }

    @Test
    fun `HideDatePicker action should set showDatePicker to false`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showDatePicker = true))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.HideDatePicker)
        
        // Then
        assertFalse(state.value.showDatePicker)
    }

    @Test
    fun `ShowFavoritesModal action should set showFavoritesModal to true`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showFavoritesModal = false))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.ShowFavoritesModal)
        
        // Then
        assertTrue(state.value.showFavoritesModal)
    }

    @Test
    fun `HideFavoritesModal action should set showFavoritesModal to false`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showFavoritesModal = true))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.HideFavoritesModal)
        
        // Then
        assertFalse(state.value.showFavoritesModal)
    }

    @Test
    fun `ShowHistoryModal action should set showHistoryModal to true`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showHistoryModal = false))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.ShowHistoryModal)
        
        // Then
        assertTrue(state.value.showHistoryModal)
    }

    @Test
    fun `HideHistoryModal action should set showHistoryModal to false`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState(showHistoryModal = true))
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.HideHistoryModal)
        
        // Then
        assertFalse(state.value.showHistoryModal)
    }

    @Test
    fun `FavoriteDistanceSelected action should update distance`() {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService
        )
        val state = mutableStateOf(DateDistanceEntryState())
        val favoriteDistance = 5.0
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.FavoriteDistanceSelected(favoriteDistance))
        
        // Then
        assertEquals("5", state.value.runDistance)
        assertFalse(state.value.showFavoritesModal)
    }

    @Test
    fun `AddRunClicked action with Health Connect enabled should trigger sync`() = runTest {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService,
            currentShoeId = { "shoe123" }
        )
        val state = mutableStateOf(
            DateDistanceEntryState(
                runDate = Date(),
                runDistance = "5.0",
                healthConnectEnabled = true
            )
        )
        
        whenever(mockHealthService.isAuthorized()).thenReturn(true)
        whenever(mockHealthService.addWorkout(any(), any(), any())).thenReturn(
            Result.success(
                HealthService.WorkoutResult(
                    workoutId = "workout123",
                    syncedAt = Date(),
                    distance = 5.0,
                    date = Date()
                )
            )
        )
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then - Verify Health Connect was called
        verify(mockHealthService).addWorkout(any(), eq(5.0), eq("shoe123"))
    }

    @Test
    fun `AddRunClicked action with Health Connect disabled should not trigger sync`() = runTest {
        // Given
        val interactor = DateDistanceEntryInteractor(
            context = context,
            healthService = mockHealthService,
            currentShoeId = { "shoe123" }
        )
        val state = mutableStateOf(
            DateDistanceEntryState(
                runDate = Date(),
                runDistance = "5.0",
                healthConnectEnabled = false
            )
        )
        
        // When
        interactor.handle(state, DateDistanceEntryInteractor.Action.AddRunClicked)
        testScheduler.advanceUntilIdle()
        
        // Then - Verify Health Connect was NOT called
        verify(mockHealthService, never()).addWorkout(any(), any(), any())
    }
}