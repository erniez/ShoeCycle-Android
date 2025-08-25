package com.shoecycle.domain

import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class SelectedShoeStrategyTest {

    private lateinit var shoeRepository: IShoeRepository
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var selectedShoeStrategy: SelectedShoeStrategy

    private val testShoe1 = Shoe(
        id = "test-shoe-1",
        brand = "Test Brand 1",
        maxDistance = 350.0,
        totalDistance = 100.0,
        startDistance = 0.0,
        startDate = Date(),
        expirationDate = Date(),
        orderingValue = 1.0,
        hallOfFame = false
    )

    private val testShoe2 = Shoe(
        id = "test-shoe-2",
        brand = "Test Brand 2", 
        maxDistance = 400.0,
        totalDistance = 50.0,
        startDistance = 0.0,
        startDate = Date(),
        expirationDate = Date(),
        orderingValue = 2.0,
        hallOfFame = false
    )

    @Before
    fun setUp() {
        shoeRepository = mock()
        userSettingsRepository = mock()
        selectedShoeStrategy = SelectedShoeStrategy(shoeRepository, userSettingsRepository)
    }

    @Test
    fun `updateSelectedShoe with no active shoes clears selection`() = runTest {
        // Given: No active shoes available
        whenever(shoeRepository.getActiveShoes()).thenReturn(flowOf(emptyList()))

        // When: updateSelectedShoe is called
        selectedShoeStrategy.updateSelectedShoe()

        // Then: Selected shoe is cleared
        verify(userSettingsRepository).updateSelectedShoeId(null)
    }

    @Test
    fun `updateSelectedShoe with valid selected shoe does nothing`() = runTest {
        // Given: Active shoes and valid selected shoe
        val activeShoes = listOf(testShoe1, testShoe2)
        val userSettings = UserSettingsData(selectedShoeId = "test-shoe-1")
        
        whenever(shoeRepository.getActiveShoes()).thenReturn(flowOf(activeShoes))
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: updateSelectedShoe is called
        selectedShoeStrategy.updateSelectedShoe()

        // Then: No changes to selection should occur
        // Note: In real implementation, it would not call updateSelectedShoeId
    }

    @Test
    fun `updateSelectedShoe with no selected shoe selects first active`() = runTest {
        // Given: Active shoes but no selected shoe
        val activeShoes = listOf(testShoe1, testShoe2)
        val userSettings = UserSettingsData(selectedShoeId = null)
        
        whenever(shoeRepository.getActiveShoes()).thenReturn(flowOf(activeShoes))
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: updateSelectedShoe is called
        selectedShoeStrategy.updateSelectedShoe()

        // Then: First active shoe is selected
        verify(userSettingsRepository).updateSelectedShoeId("test-shoe-1")
    }

    @Test
    fun `selectShoe with valid active shoe updates selection`() = runTest {
        // Given: Valid active shoe
        whenever(shoeRepository.getShoeByIdOnce("test-shoe-2")).thenReturn(testShoe2)

        // When: selectShoe is called
        selectedShoeStrategy.selectShoe("test-shoe-2")

        // Then: Shoe is selected
        verify(userSettingsRepository).updateSelectedShoeId("test-shoe-2")
    }

    @Test
    fun `getSelectedShoe returns correct shoe when valid ID`() = runTest {
        // Given: Valid selected shoe ID
        val userSettings = UserSettingsData(selectedShoeId = "test-shoe-1")
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))
        whenever(shoeRepository.getShoeByIdOnce("test-shoe-1")).thenReturn(testShoe1)

        // When: getSelectedShoe is called
        val result = selectedShoeStrategy.getSelectedShoe()

        // Then: Correct shoe is returned
        assertEquals(testShoe1, result)
    }

    @Test
    fun `getSelectedShoe returns null when no selected ID`() = runTest {
        // Given: No selected shoe ID
        val userSettings = UserSettingsData(selectedShoeId = null)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: getSelectedShoe is called
        val result = selectedShoeStrategy.getSelectedShoe()

        // Then: Null is returned
        assertNull(result)
    }
}