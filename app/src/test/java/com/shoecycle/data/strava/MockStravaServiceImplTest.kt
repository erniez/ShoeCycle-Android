package com.shoecycle.data.strava

import com.shoecycle.data.strava.models.StravaActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class MockStravaServiceImplTest {
    
    private fun createTestActivity() = StravaActivity(
        name = "Test Run",
        type = "run",
        startDateLocal = "2024-01-01T10:00:00Z",
        elapsedTime = "1800",
        distance = "5000.0"
    )
    
    @Test
    fun `sendActivity succeeds when no failure mode is set`() = runBlocking {
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                networkDelayMs = 100,
                failureMode = null
            )
        )
        
        // Should not throw exception
        service.sendActivity(createTestActivity())
    }
    
    @Test
    fun `sendActivity always fails with unauthorized when configured`() = runBlocking {
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                failureMode = MockStravaServiceImpl.FailureMode.ALWAYS_UNAUTHORIZED,
                networkDelayMs = 100
            )
        )
        
        try {
            service.sendActivity(createTestActivity())
            fail("Expected Unauthorized error")
        } catch (e: StravaService.DomainError.Unauthorized) {
            // Expected
        }
    }
    
    @Test
    fun `sendActivity always fails with network error when configured`() = runBlocking {
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                failureMode = MockStravaServiceImpl.FailureMode.ALWAYS_NETWORK_ERROR,
                networkDelayMs = 100
            )
        )
        
        try {
            service.sendActivity(createTestActivity())
            fail("Expected Reachability error")
        } catch (e: StravaService.DomainError.Reachability) {
            // Expected
        }
    }
    
    @Test
    fun `sendActivity always fails with unknown error when configured`() = runBlocking {
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                failureMode = MockStravaServiceImpl.FailureMode.ALWAYS_UNKNOWN_ERROR,
                networkDelayMs = 100
            )
        )
        
        try {
            service.sendActivity(createTestActivity())
            fail("Expected Unknown error")
        } catch (e: StravaService.DomainError.Unknown) {
            // Expected
        }
    }
    
    @Test
    fun `sendActivity respects network delay configuration`() = runBlocking {
        val delayMs = 500L
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                networkDelayMs = delayMs,
                failureMode = null
            )
        )
        
        val elapsed = measureTimeMillis {
            service.sendActivity(createTestActivity())
        }
        
        assertTrue("Expected delay of at least ${delayMs}ms, but was ${elapsed}ms", 
            elapsed >= delayMs)
    }
    
    @Test
    fun `sendActivity produces random errors when configured`() = runBlocking {
        val service = MockStravaServiceImpl(
            MockStravaServiceImpl.MockConfig(
                failureMode = MockStravaServiceImpl.FailureMode.RANDOM_ERRORS,
                networkDelayMs = 50
            )
        )
        
        val errors = mutableSetOf<Class<out StravaService.DomainError>>()
        
        // Run multiple times to collect different error types
        repeat(30) {
            try {
                service.sendActivity(createTestActivity())
            } catch (e: StravaService.DomainError) {
                errors.add(e::class.java)
            }
        }
        
        // Should have encountered at least 2 different error types
        assertTrue("Expected multiple error types, but got: $errors", 
            errors.size >= 2)
    }
    
    
    @Test
    fun `factory creates mock service in debug builds`() {
        val tokenKeeper = StravaTokenKeeper(
            storage = com.shoecycle.data.strava.storage.InMemoryTokenStorage()
        )
        
        val service = StravaServiceFactory.create(tokenKeeper)
        
        // In debug builds, should return MockStravaServiceImpl
        assertTrue(service is MockStravaServiceImpl)
    }
    
    @Test
    fun `test configurations provide expected behavior`() = runBlocking {
        // Test always successful configuration
        val successfulService = StravaServiceFactory.TestConfigurations.alwaysSuccessful()
        
        // Should not throw
        successfulService.sendActivity(createTestActivity())
        
        // Test always unauthorized configuration
        val unauthorizedService = StravaServiceFactory.TestConfigurations.alwaysUnauthorized()
        
        try {
            unauthorizedService.sendActivity(createTestActivity())
            fail("Expected Unauthorized error")
        } catch (e: StravaService.DomainError.Unauthorized) {
            // Expected
        }
        
        // Test always network error configuration
        val networkErrorService = StravaServiceFactory.TestConfigurations.alwaysNetworkError()
        
        try {
            networkErrorService.sendActivity(createTestActivity())
            fail("Expected Reachability error")
        } catch (e: StravaService.DomainError.Reachability) {
            // Expected
        }
    }
    
    @Test
    fun `slow but reliable configuration succeeds with delay`() = runBlocking {
        // Create a custom mock with short delay for testing
        val service = StravaServiceFactory.createMockService(
            networkDelayMs = 10,
            failureMode = null
        )
        
        val elapsed = measureTimeMillis {
            // Should succeed
            service.sendActivity(createTestActivity())
        }
        
        // Should have minimal delay for test speed
        assertTrue("Expected delay of at least 10ms, but was ${elapsed}ms", 
            elapsed >= 10)
    }
}