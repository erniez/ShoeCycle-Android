package com.shoecycle.domain

import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.History
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
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class ShoeBusinessLogicTest {

    private lateinit var shoeRepository: IShoeRepository
    private lateinit var historyRepository: IHistoryRepository
    private lateinit var shoeBusinessLogic: ShoeBusinessLogic

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

    @Before
    fun setUp() {
        shoeRepository = mock()
        historyRepository = mock()
        shoeBusinessLogic = ShoeBusinessLogic(shoeRepository, historyRepository)
    }

    @Test
    fun `createShoe with defaults creates shoe with correct values`() = runTest {
        // Given: Repository returns next ordering value and shoe ID
        whenever(shoeRepository.getNextOrderingValue()).thenReturn(2.0)
        whenever(shoeRepository.insertShoe(org.mockito.kotlin.any())).thenReturn(5L)

        // When: Creating a new shoe
        val shoeId = shoeBusinessLogic.createShoe("Nike Air Max")

        // Then: Shoe is created
        assertEquals(5L, shoeId)
        verify(shoeRepository).insertShoe(org.mockito.kotlin.any())
    }

    @Test
    fun `retireShoe calls repository retire method`() = runTest {
        // When: Retiring a shoe
        shoeBusinessLogic.retireShoe(1L)

        // Then: Repository retire method is called
        verify(shoeRepository).retireShoe(1L)
    }

    @Test
    fun `reactivateShoe calls repository reactivate method`() = runTest {
        // When: Reactivating a shoe
        shoeBusinessLogic.reactivateShoe(1L)

        // Then: Repository reactivate method is called
        verify(shoeRepository).reactivateShoe(1L)
    }

    @Test
    fun `logDistance creates history and recalculates total`() = runTest {
        // Given: Repository setup
        whenever(historyRepository.insertHistory(org.mockito.kotlin.any())).thenReturn(5L)
        whenever(shoeRepository.getShoeByIdOnce(1L)).thenReturn(testShoe)
        whenever(historyRepository.getTotalDistanceForShoe(1L)).thenReturn(18.0)

        // When: Logging distance
        shoeBusinessLogic.logDistance(1L, 5.0, Date())

        // Then: History is created and total is recalculated
        verify(historyRepository).insertHistory(org.mockito.kotlin.any())
        verify(shoeRepository).updateTotalDistance(1L, 68.0) // 50 (start) + 18 (history total)
    }

    @Test
    fun `isShoeNearExpiration returns true for shoe near max distance`() = runTest {
        // Given: Shoe near expiration (90% of max distance)
        val nearExpirationShoe = testShoe.copy(totalDistance = 320.0, maxDistance = 350.0)
        whenever(shoeRepository.getShoeByIdOnce(1L)).thenReturn(nearExpirationShoe)

        // When: Checking expiration status
        val result = shoeBusinessLogic.isShoeNearExpiration(1L)

        // Then: Returns true (remaining distance is 30, which is < 35 (10% of 350))
        assertTrue(result)
    }

    @Test
    fun `isShoeNearExpiration returns false for shoe with plenty of distance`() = runTest {
        // Given: Shoe with plenty of distance remaining
        val newShoe = testShoe.copy(totalDistance = 50.0, maxDistance = 350.0)
        whenever(shoeRepository.getShoeByIdOnce(1L)).thenReturn(newShoe)

        // When: Checking expiration status
        val result = shoeBusinessLogic.isShoeNearExpiration(1L)

        // Then: Returns false
        assertFalse(result)
    }

    @Test
    fun `default max distance constant is correct`() {
        // Then: Default max distance is 350 miles
        assertEquals(350.0, ShoeBusinessLogic.DEFAULT_MAX_DISTANCE, 0.01)
    }

    @Test
    fun `expiration months constant is correct`() {
        // Then: Expiration is 6 months
        assertEquals(6, ShoeBusinessLogic.EXPIRATION_MONTHS)
    }
}