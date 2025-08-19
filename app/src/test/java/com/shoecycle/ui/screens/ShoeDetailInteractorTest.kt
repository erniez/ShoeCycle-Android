package com.shoecycle.ui.screens

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.domain.models.Shoe
import com.shoecycle.ui.screens.shoe_detail.ShoeDetailInteractor
import com.shoecycle.ui.screens.shoe_detail.ShoeDetailState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class ShoeDetailInteractorTest {

    private val mockShoeRepository = mock<IShoeRepository>()
    private val mockUserSettingsRepository = mock<UserSettingsRepository>()
    private val mockSelectedShoeStrategy = mock<SelectedShoeStrategy>()

    private val testShoe = Shoe(
        id = 1L,
        brand = "Test Brand",
        maxDistance = 350.0,
        totalDistance = 100.0,
        startDistance = 50.0,
        startDate = Date(1640995200000), // 2022-01-01
        expirationDate = Date(1656633600000), // 2022-07-01
        orderingValue = 1.0,
        hallOfFame = false,
        imageKey = "test_image_key",
        thumbnailData = byteArrayOf(1, 2, 3, 4)
    )

    @Test
    fun `initial state should have correct default values`() {
        val state = ShoeDetailState()

        assertNull(state.shoe)
        assertNull(state.editedShoe)
        assertFalse(state.hasUnsavedChanges)
        assertTrue(state.isLoading)
        assertFalse(state.isSaving)
        assertFalse(state.shouldNavigateBack)
        assertNull(state.errorMessage)
        assertEquals(DistanceUnit.MILES, state.distanceUnit)
        assertFalse(state.isCreateMode)
        assertNull(state.onShoeSaved)
    }

    @Test
    fun `when ViewAppeared action, should load shoe successfully`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState())
        
        whenever(mockShoeRepository.getShoeById(1L)).thenReturn(flowOf(testShoe))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ViewAppeared(1L))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(testShoe, state.value.shoe)
        assertEquals(testShoe, state.value.editedShoe)
        assertFalse(state.value.hasUnsavedChanges)
        assertFalse(state.value.isLoading)
        assertNull(state.value.errorMessage)
        assertEquals(DistanceUnit.MILES, state.value.distanceUnit)
    }

    @Test
    fun `when ViewAppeared action with null shoe, should set error state`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState())
        
        whenever(mockShoeRepository.getShoeById(1L)).thenReturn(flowOf(null))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ViewAppeared(1L))
        testScheduler.advanceUntilIdle()

        // Assert
        assertNull(state.value.shoe)
        assertFalse(state.value.isLoading)
        assertEquals("Shoe not found", state.value.errorMessage)
    }

    @Test
    fun `when ViewAppeared action throws exception, should handle error gracefully`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState())
        
        whenever(mockShoeRepository.getShoeById(1L)).thenThrow(RuntimeException("Database error"))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ViewAppeared(1L))
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isLoading)
        assertEquals("Error loading shoe: Database error", state.value.errorMessage)
    }

    @Test
    fun `when InitializeNewShoe action, should create default shoe`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState())
        
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.KM)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        whenever(mockShoeRepository.getNextOrderingValue()).thenReturn(2.0)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.InitializeNewShoe)
        testScheduler.advanceUntilIdle()

        // Assert
        assertNotNull(state.value.shoe)
        assertNotNull(state.value.editedShoe)
        assertEquals(2.0, state.value.editedShoe?.orderingValue)
        assertFalse(state.value.hasUnsavedChanges)
        assertFalse(state.value.isLoading)
        assertEquals(DistanceUnit.KM, state.value.distanceUnit)
        assertTrue(state.value.isCreateMode)
    }

    @Test
    fun `when InitializeNewShoe action fails, should fallback to default`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState())
        
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        whenever(mockShoeRepository.getNextOrderingValue()).thenThrow(RuntimeException("Database error"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.InitializeNewShoe)
        testScheduler.advanceUntilIdle()

        // Assert
        assertNotNull(state.value.shoe)
        assertNotNull(state.value.editedShoe)
        assertFalse(state.value.isLoading)
        assertEquals(DistanceUnit.MILES, state.value.distanceUnit)
        assertTrue(state.value.isCreateMode)
    }

    @Test
    fun `when Refresh action with existing shoe, should reload shoe`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val state = mutableStateOf(ShoeDetailState(shoe = testShoe))
        
        whenever(mockShoeRepository.getShoeById(1L)).thenReturn(flowOf(testShoe))
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(mockUserSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.Refresh)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).getShoeById(1L)
    }

    @Test
    fun `when UpdateShoeName action, should update edited shoe and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateShoeName("New Brand"))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals("New Brand", state.value.editedShoe?.brand)
        assertTrue(state.value.hasUnsavedChanges)
        assertEquals(testShoe, state.value.shoe) // Original shoe unchanged
    }

    @Test
    fun `when UpdateShoeName action with null edited shoe, should do nothing`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(editedShoe = null)
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateShoeName("New Brand"))
        testScheduler.advanceUntilIdle()

        // Assert
        assertNull(state.value.editedShoe)
        assertFalse(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateStartDistance action, should update distance and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false,
            distanceUnit = DistanceUnit.MILES
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateStartDistance("25.5"))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(25.5, state.value.editedShoe?.startDistance)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateStartDistance action with invalid input, should set to zero`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false,
            distanceUnit = DistanceUnit.MILES
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateStartDistance("invalid"))
        testScheduler.advanceUntilIdle()

        // Assert - DistanceUtility.distance returns 0.0 for invalid input
        assertEquals(0.0, state.value.editedShoe?.startDistance)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateMaxDistance action, should update distance and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false,
            distanceUnit = DistanceUnit.MILES
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateMaxDistance("400.0"))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(400.0, state.value.editedShoe?.maxDistance)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateStartDate action, should update date and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)
        val newDate = Date(1672531200000) // 2023-01-01

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateStartDate(newDate))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(newDate, state.value.editedShoe?.startDate)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateEndDate action, should update expiration date and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)
        val newDate = Date(1688169600000) // 2023-07-01

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateEndDate(newDate))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(newDate, state.value.editedShoe?.expirationDate)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when UpdateShoeImage action, should update image and mark as changed`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)
        val newImageKey = "new_image_key"
        val newThumbnailData = byteArrayOf(5, 6, 7, 8)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.UpdateShoeImage(newImageKey, newThumbnailData))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(newImageKey, state.value.editedShoe?.imageKey)
        assertArrayEquals(newThumbnailData, state.value.editedShoe?.thumbnailData)
        assertTrue(state.value.hasUnsavedChanges)
    }

    @Test
    fun `when SaveChanges action in create mode with valid data, should create shoe successfully`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val mockCallback = mock<() -> Unit>()
        val validShoe = testShoe.copy(brand = "Valid Brand")
        val initialState = ShoeDetailState(
            editedShoe = validShoe,
            isCreateMode = true,
            onShoeSaved = mockCallback
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.insertShoe(validShoe)).thenReturn(2L)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).insertShoe(validShoe)
        verify(mockCallback).invoke()
        assertFalse(state.value.isSaving)
        assertTrue(state.value.shouldNavigateBack)
    }

    @Test
    fun `when SaveChanges action in create mode with blank name, should show validation error`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val invalidShoe = testShoe.copy(brand = "")
        val initialState = ShoeDetailState(
            editedShoe = invalidShoe,
            isCreateMode = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository, never()).insertShoe(any())
        assertEquals("Please enter a shoe name", state.value.errorMessage)
        assertFalse(state.value.isSaving)
    }

    @Test
    fun `when SaveChanges action in edit mode, should update shoe successfully`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val editedShoe = testShoe.copy(brand = "Updated Brand")
        val updatedShoe = editedShoe.copy(totalDistance = 120.0)
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = editedShoe,
            hasUnsavedChanges = true,
            isCreateMode = false
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.getShoeByIdOnce(editedShoe.id)).thenReturn(updatedShoe)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).updateShoe(editedShoe)
        verify(mockShoeRepository).recalculateShoeTotal(editedShoe.id)
        verify(mockShoeRepository).getShoeByIdOnce(editedShoe.id)
        assertEquals(updatedShoe, state.value.shoe)
        assertEquals(updatedShoe, state.value.editedShoe)
        assertFalse(state.value.hasUnsavedChanges)
        assertFalse(state.value.isSaving)
    }

    @Test
    fun `when SaveChanges action fails in create mode, should show error`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val validShoe = testShoe.copy(brand = "Valid Brand")
        val initialState = ShoeDetailState(
            editedShoe = validShoe,
            isCreateMode = true
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.insertShoe(validShoe)).thenThrow(RuntimeException("Database error"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isSaving)
        assertEquals("Error creating shoe: Database error", state.value.errorMessage)
    }

    @Test
    fun `when SaveChanges action fails in edit mode, should show error`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val editedShoe = testShoe.copy(brand = "Updated Brand")
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = editedShoe,
            hasUnsavedChanges = true,
            isCreateMode = false
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.updateShoe(editedShoe)).thenThrow(RuntimeException("Update failed"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isSaving)
        assertEquals("Error saving changes: Update failed", state.value.errorMessage)
    }

    @Test
    fun `when RequestNavigateBack action with unsaved changes, should save and navigate`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val editedShoe = testShoe.copy(brand = "Updated Brand")
        val updatedShoe = editedShoe.copy(totalDistance = 120.0)
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = editedShoe,
            hasUnsavedChanges = true,
            isSaving = false
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.getShoeByIdOnce(editedShoe.id)).thenReturn(updatedShoe)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.RequestNavigateBack)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).updateShoe(editedShoe)
        verify(mockShoeRepository).recalculateShoeTotal(editedShoe.id)
        assertFalse(state.value.hasUnsavedChanges)
        assertFalse(state.value.isSaving)
        assertTrue(state.value.shouldNavigateBack)
    }

    @Test
    fun `when RequestNavigateBack action with no unsaved changes, should navigate immediately`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false,
            isSaving = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.RequestNavigateBack)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository, never()).updateShoe(any())
        assertTrue(state.value.shouldNavigateBack)
    }

    @Test
    fun `when RequestNavigateBack action while saving, should do nothing`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = true,
            isSaving = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.RequestNavigateBack)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository, never()).updateShoe(any())
        assertFalse(state.value.shouldNavigateBack)
        assertTrue(state.value.isSaving)
    }

    @Test
    fun `when RequestNavigateBack action save fails, should handle error`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val editedShoe = testShoe.copy(brand = "Updated Brand")
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = editedShoe,
            hasUnsavedChanges = true,
            isSaving = false
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.updateShoe(editedShoe)).thenThrow(RuntimeException("Save failed"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.RequestNavigateBack)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.isSaving)
        assertFalse(state.value.shouldNavigateBack)
        assertEquals("Error saving changes: Save failed", state.value.errorMessage)
    }

    @Test
    fun `when CancelCreate action, should navigate back without saving`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            editedShoe = testShoe,
            isCreateMode = true,
            hasUnsavedChanges = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.CancelCreate)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository, never()).insertShoe(any())
        assertTrue(state.value.shouldNavigateBack)
    }

    @Test
    fun `when DeleteShoe action, should show confirmation dialog`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            showDeleteConfirmation = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.DeleteShoe)
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue(state.value.showDeleteConfirmation)
        verify(mockShoeRepository, never()).deleteShoe(any())
    }

    @Test
    fun `when ConfirmDelete action, should delete shoe and navigate back`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            showDeleteConfirmation = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ConfirmDelete)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).deleteShoe(testShoe)
        verify(mockSelectedShoeStrategy).updateSelectedShoe()
        assertFalse(state.value.showDeleteConfirmation)
        assertFalse(state.value.isSaving)
        assertTrue(state.value.shouldNavigateBack)
    }

    @Test
    fun `when ConfirmDelete action with null shoe, should do nothing`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = null,
            editedShoe = null,
            showDeleteConfirmation = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ConfirmDelete)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository, never()).deleteShoe(any())
        verify(mockSelectedShoeStrategy, never()).updateSelectedShoe()
        assertFalse(state.value.showDeleteConfirmation)
        assertFalse(state.value.isSaving)
        assertFalse(state.value.shouldNavigateBack)
    }

    @Test
    fun `when ConfirmDelete action fails, should show error message`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            showDeleteConfirmation = true
        )
        val state = mutableStateOf(initialState)
        
        whenever(mockShoeRepository.deleteShoe(testShoe)).thenThrow(RuntimeException("Delete failed"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.ConfirmDelete)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockShoeRepository).deleteShoe(testShoe)
        verify(mockSelectedShoeStrategy, never()).updateSelectedShoe()
        assertFalse(state.value.isSaving)
        assertFalse(state.value.shouldNavigateBack)
        assertEquals("Error deleting shoe: Delete failed", state.value.errorMessage)
    }

    @Test
    fun `when CancelDelete action, should hide confirmation dialog`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            showDeleteConfirmation = true
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.CancelDelete)
        testScheduler.advanceUntilIdle()

        // Assert
        assertFalse(state.value.showDeleteConfirmation)
        verify(mockShoeRepository, never()).deleteShoe(any())
    }

    // Given: Shoe with hallOfFame = false
    // When: HallOfFameToggled action with true
    // Then: Should update shoe to hall of fame and save immediately
    @Test
    fun `when HallOfFameToggled action to true, should add shoe to hall of fame`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(true))
        testScheduler.advanceUntilIdle()

        // Assert - After async save completes
        val expectedUpdatedShoe = testShoe.copy(hallOfFame = true)
        assertEquals("Edited shoe should be in hall of fame", true, state.value.editedShoe?.hallOfFame)
        assertEquals("Original shoe should be updated", expectedUpdatedShoe, state.value.shoe)
        assertFalse("Should not have unsaved changes after save", state.value.hasUnsavedChanges)
        
        // Verify repository calls
        verify(mockShoeRepository).updateShoe(expectedUpdatedShoe)
        verify(mockSelectedShoeStrategy).updateSelectedShoe()
    }

    // Given: Shoe with hallOfFame = true
    // When: HallOfFameToggled action with false
    // Then: Should remove shoe from hall of fame and save immediately
    @Test
    fun `when HallOfFameToggled action to false, should remove shoe from hall of fame`() = runTest {
        // Arrange
        val hallOfFameShoe = testShoe.copy(hallOfFame = true)
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = hallOfFameShoe,
            editedShoe = hallOfFameShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(false))
        testScheduler.advanceUntilIdle()

        // Assert - After async save completes
        val expectedUpdatedShoe = hallOfFameShoe.copy(hallOfFame = false)
        assertEquals("Edited shoe should not be in hall of fame", false, state.value.editedShoe?.hallOfFame)
        assertEquals("Original shoe should be updated", expectedUpdatedShoe, state.value.shoe)
        assertFalse("Should not have unsaved changes after save", state.value.hasUnsavedChanges)
        
        // Verify repository calls
        verify(mockShoeRepository).updateShoe(expectedUpdatedShoe)
        verify(mockSelectedShoeStrategy).updateSelectedShoe()
    }

    // Given: Shoe in normal state
    // When: HallOfFameToggled action with null edited shoe
    // Then: Should do nothing
    @Test
    fun `when HallOfFameToggled action with null edited shoe, should do nothing`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = null,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(true))
        testScheduler.advanceUntilIdle()

        // Assert
        assertNull("Edited shoe should remain null", state.value.editedShoe)
        assertFalse("Should not mark as changed", state.value.hasUnsavedChanges)
        verify(mockShoeRepository, never()).updateShoe(any())
        verify(mockSelectedShoeStrategy, never()).updateSelectedShoe()
    }

    // Given: Repository update fails
    // When: HallOfFameToggled action is handled
    // Then: Should handle error gracefully
    @Test
    fun `when HallOfFameToggled action repository fails, should handle error gracefully`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)
        
        val expectedUpdatedShoe = testShoe.copy(hallOfFame = true)
        whenever(mockShoeRepository.updateShoe(expectedUpdatedShoe)).thenThrow(RuntimeException("Database error"))

        // Act
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(true))
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals("Edited shoe should show hall of fame status", true, state.value.editedShoe?.hallOfFame)
        assertTrue("Should show error message", state.value.errorMessage?.contains("Error updating hall of fame status") == true)
        assertTrue("Should contain original error", state.value.errorMessage?.contains("Database error") == true)
        verify(mockShoeRepository).updateShoe(expectedUpdatedShoe)
        verify(mockSelectedShoeStrategy, never()).updateSelectedShoe() // Should not update on error
    }

    // Given: Shoe with specific hall of fame states
    // When: Multiple HallOfFameToggled actions
    // Then: Should update selected shoe strategy each time
    @Test
    fun `when HallOfFameToggled action succeeds, should update selected shoe strategy`() = runTest {
        // Arrange
        val interactor = ShoeDetailInteractor(
            mockShoeRepository,
            mockUserSettingsRepository,
            mockSelectedShoeStrategy,
            this
        )
        val initialState = ShoeDetailState(
            shoe = testShoe,
            editedShoe = testShoe,
            hasUnsavedChanges = false
        )
        val state = mutableStateOf(initialState)

        // Act - Toggle to hall of fame
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(true))
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockSelectedShoeStrategy).updateSelectedShoe()
        
        // Reset mocks for second test
        org.mockito.kotlin.reset(mockSelectedShoeStrategy)
        
        // Act - Toggle back to active
        interactor.handle(state, ShoeDetailInteractor.Action.HallOfFameToggled(false))
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockSelectedShoeStrategy).updateSelectedShoe()
    }
}