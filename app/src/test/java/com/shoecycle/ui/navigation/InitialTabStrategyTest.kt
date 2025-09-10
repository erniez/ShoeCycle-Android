package com.shoecycle.ui.navigation

import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.Shoe
import com.shoecycle.ui.ShoeCycleDestination
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InitialTabStrategyTest {
    
    private lateinit var strategy: InitialTabStrategy
    
    @Mock
    private lateinit var mockShoeRepository: IShoeRepository
    
    @Before
    fun setup() {
        org.mockito.MockitoAnnotations.openMocks(this)
    }
    
    // Given: The app has active shoes in the database
    // When: InitialTabStrategy determines the initial tab
    // Then: It should return the Add Distance route
    @Test
    fun `initialTab returns AddDistance route when active shoes exist`() = runBlocking {
        // Given: Create mock shoes
        val mockShoes = listOf(
            Shoe(
                id = "shoe-1",
                brand = "Nike Pegasus",
                maxDistance = 500.0,
                totalDistance = 100.0,
                startDistance = 0.0,
                startDate = Date(),
                expirationDate = Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000), // 90 days from now
                imageKey = null,
                thumbnailData = null,
                orderingValue = 1.0,
                hallOfFame = false
            ),
            Shoe(
                id = "shoe-2",
                brand = "Adidas UltraBoost",
                maxDistance = 400.0,
                totalDistance = 50.0,
                startDistance = 0.0,
                startDate = Date(),
                expirationDate = Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000),
                imageKey = null,
                thumbnailData = null,
                orderingValue = 2.0,
                hallOfFame = false
            )
        )
        
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(mockShoes))
        
        // When: Create strategy with mocked repository
        strategy = InitialTabStrategy(mockShoeRepository)
        val result = strategy.initialTab()
        
        // Then: Should return Add Distance route
        assertEquals(ShoeCycleDestination.AddDistance.route, result)
    }
    
    // Given: The app has no active shoes in the database
    // When: InitialTabStrategy determines the initial tab
    // Then: It should return the Active Shoes route
    @Test
    fun `initialTab returns ActiveShoes route when no active shoes exist`() = runBlocking {
        // Given: Empty shoe list
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(emptyList()))
        
        // When: Create strategy with mocked repository
        strategy = InitialTabStrategy(mockShoeRepository)
        val result = strategy.initialTab()
        
        // Then: Should return Active Shoes route
        assertEquals(ShoeCycleDestination.ActiveShoes.route, result)
    }
    
    // Given: The app has exactly one active shoe
    // When: InitialTabStrategy determines the initial tab
    // Then: It should return the Add Distance route
    @Test
    fun `initialTab returns AddDistance route when exactly one shoe exists`() = runBlocking {
        // Given: Single shoe
        val singleShoe = listOf(
            Shoe(
                id = "shoe-single",
                brand = "Brooks Ghost",
                maxDistance = 600.0,
                totalDistance = 200.0,
                startDistance = 0.0,
                startDate = Date(),
                expirationDate = Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000),
                imageKey = null,
                thumbnailData = null,
                orderingValue = 1.0,
                hallOfFame = false
            )
        )
        
        whenever(mockShoeRepository.getActiveShoes()).thenReturn(flowOf(singleShoe))
        
        // When: Create strategy with mocked repository
        strategy = InitialTabStrategy(mockShoeRepository)
        val result = strategy.initialTab()
        
        // Then: Should return Add Distance route
        assertEquals(ShoeCycleDestination.AddDistance.route, result)
    }
}