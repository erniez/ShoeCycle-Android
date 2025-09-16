package com.shoecycle.ui.settings

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.analytics.AnalyticsLogger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsHealthConnectInteractorTest {

    private val mockContext = mock<Context>()
    private val mockRepository = mock<UserSettingsRepository>()
    private val mockAnalytics = mock<AnalyticsLogger>()

    @Before
    fun setup() {
        // Set up default repository response
        val mockUserSettings = UserSettingsData()
        whenever(mockRepository.userSettingsFlow).thenReturn(flowOf(mockUserSettings))
    }

    @Test
    fun `when PermissionGranted action, should reset denial count and enable Health Connect`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState())

        // Act
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue(state.value.isEnabled)
        assertEquals(SettingsHealthConnectState.PermissionStatus.Granted, state.value.permissionStatus)
        assertFalse(state.value.showPermissionDialog)
        assertNull(state.value.errorMessage)
        verify(mockRepository).updateHealthConnectEnabled(true)
    }

    @Test
    fun `when PermissionDenied action first time, should set Denied status`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState())

        // Act
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isEnabled)
        assertEquals(SettingsHealthConnectState.PermissionStatus.Denied, state.value.permissionStatus)
        assertEquals("Health Connect permissions are required to sync your runs.", state.value.errorMessage)
        verify(mockRepository).updateHealthConnectEnabled(false)
    }

    @Test
    fun `when PermissionDenied action twice, should set PermanentlyDenied status`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState())

        // Act - Deny twice
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isEnabled)
        assertEquals(SettingsHealthConnectState.PermissionStatus.PermanentlyDenied, state.value.permissionStatus)
        assertEquals(
            "Health Connect permissions have been denied. Please grant permissions in your device settings.",
            state.value.errorMessage
        )
        verify(mockRepository, times(2)).updateHealthConnectEnabled(false)
    }

    @Test
    fun `when PermissionGranted after denials, should reset denial count`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState())

        // Act - Deny once, then grant
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionGranted)
        // Now deny again - should be treated as first denial
        interactor.handle(state, SettingsHealthConnectInteractor.Action.PermissionDenied)
        testScheduler.advanceUntilIdle()

        // Assert - Should be regular Denied, not PermanentlyDenied
        assertEquals(SettingsHealthConnectState.PermissionStatus.Denied, state.value.permissionStatus)
        assertEquals("Health Connect permissions are required to sync your runs.", state.value.errorMessage)
    }

    @Test
    fun `when OpenAppSettings action, should set showAppSettings flag`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState())

        // Act
        interactor.handle(state, SettingsHealthConnectInteractor.Action.OpenAppSettings)

        // Assert
        assertTrue(state.value.showAppSettings)
    }

    @Test
    fun `when DismissError action, should clear error message`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState(errorMessage = "Some error"))

        // Act
        interactor.handle(state, SettingsHealthConnectInteractor.Action.DismissError)

        // Assert
        assertNull(state.value.errorMessage)
    }

    @Test
    fun `when ToggleChanged to false, should disable and update repository`() = runTest {
        // Arrange
        val interactor = SettingsHealthConnectInteractor(mockContext, mockRepository, mockAnalytics, this)
        val state = mutableStateOf(SettingsHealthConnectState(isEnabled = true))

        // Act
        interactor.handle(state, SettingsHealthConnectInteractor.Action.ToggleChanged(false))
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isEnabled)
        assertEquals(SettingsHealthConnectState.PermissionStatus.Unknown, state.value.permissionStatus)
        verify(mockRepository).updateHealthConnectEnabled(false)
    }

}