package com.shoecycle.domain.analytics

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockAnalyticsLoggerTest {
    
    private lateinit var logger: MockAnalyticsLogger
    
    @Before
    fun setUp() {
        logger = MockAnalyticsLogger()
    }
    
    @Test
    fun `initialize sets isInitialized to true`() {
        // Given
        assertFalse(logger.isInitialized)
        
        // When
        logger.initialize()
        
        // Then
        assertTrue(logger.isInitialized)
    }
    
    @Test
    fun `logEvent records event with name and parameters`() {
        // Given
        val eventName = AnalyticsKeys.Event.ADD_SHOE
        val parameters = mapOf(
            "brand" to "Nike",
            "model" to "Pegasus 40"
        )
        
        // When
        logger.logEvent(eventName, parameters)
        
        // Then
        assertEquals(1, logger.recordedEvents.size)
        val recordedEvent = logger.recordedEvents[0]
        assertEquals(eventName, recordedEvent.name)
        assertEquals(parameters, recordedEvent.parameters)
    }
    
    @Test
    fun `logEvent records multiple events in order`() {
        // When
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, mapOf("brand" to "Nike"))
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, mapOf("distance" to 5.0))
        logger.logEvent(AnalyticsKeys.Event.VIEW_SHOE_DETAIL, null)
        
        // Then
        assertEquals(3, logger.recordedEvents.size)
        assertEquals(AnalyticsKeys.Event.ADD_SHOE, logger.recordedEvents[0].name)
        assertEquals(AnalyticsKeys.Event.LOG_MILEAGE, logger.recordedEvents[1].name)
        assertEquals(AnalyticsKeys.Event.VIEW_SHOE_DETAIL, logger.recordedEvents[2].name)
    }
    
    @Test
    fun `reset clears all recorded events and initialization state`() {
        // Given
        logger.initialize()
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, null)
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, null)
        assertTrue(logger.isInitialized)
        assertEquals(2, logger.recordedEvents.size)
        
        // When
        logger.reset()
        
        // Then
        assertFalse(logger.isInitialized)
        assertTrue(logger.recordedEvents.isEmpty())
    }
    
    @Test
    fun `hasLoggedEvent returns true when event exists`() {
        // Given
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, null)
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, null)
        
        // Then
        assertTrue(logger.hasLoggedEvent(AnalyticsKeys.Event.ADD_SHOE))
        assertTrue(logger.hasLoggedEvent(AnalyticsKeys.Event.LOG_MILEAGE))
        assertFalse(logger.hasLoggedEvent(AnalyticsKeys.Event.VIEW_SHOE_DETAIL))
    }
    
    @Test
    fun `getEventsOfType returns all matching events`() {
        // Given
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, mapOf("distance" to 5.0))
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, null)
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, mapOf("distance" to 10.0))
        
        // When
        val mileageEvents = logger.getEventsOfType(AnalyticsKeys.Event.LOG_MILEAGE)
        
        // Then
        assertEquals(2, mileageEvents.size)
        assertEquals(5.0, mileageEvents[0].parameters?.get("distance"))
        assertEquals(10.0, mileageEvents[1].parameters?.get("distance"))
    }
    
    @Test
    fun `getEventsOfType returns empty list when no matching events`() {
        // Given
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, null)
        
        // When
        val events = logger.getEventsOfType(AnalyticsKeys.Event.LOG_MILEAGE)
        
        // Then
        assertTrue(events.isEmpty())
    }
    
    @Test
    fun `getLastEvent returns most recent event`() {
        // Given
        logger.logEvent(AnalyticsKeys.Event.ADD_SHOE, null)
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, mapOf("distance" to 5.0))
        logger.logEvent(AnalyticsKeys.Event.VIEW_SHOE_DETAIL, null)
        
        // When
        val lastEvent = logger.getLastEvent()
        
        // Then
        assertNotNull(lastEvent)
        assertEquals(AnalyticsKeys.Event.VIEW_SHOE_DETAIL, lastEvent?.name)
    }
    
    @Test
    fun `getLastEvent returns null when no events logged`() {
        // When
        val lastEvent = logger.getLastEvent()
        
        // Then
        assertNull(lastEvent)
    }
    
    @Test
    fun `logEvent handles null parameters correctly`() {
        // When
        logger.logEvent(AnalyticsKeys.Event.SHOW_HISTORY, null)
        
        // Then
        val event = logger.recordedEvents[0]
        assertEquals(AnalyticsKeys.Event.SHOW_HISTORY, event.name)
        assertNull(event.parameters)
    }
    
    @Test
    fun `logEvent preserves parameter types`() {
        // Given
        val parameters = mapOf(
            AnalyticsKeys.Param.MILEAGE to 5.5,
            AnalyticsKeys.Param.DISTANCE_UNIT to "MILES",
            AnalyticsKeys.Param.NUMBER_OF_FAVORITES to 4,
            "isRetired" to false
        )
        
        // When
        logger.logEvent(AnalyticsKeys.Event.LOG_MILEAGE, parameters)
        
        // Then
        val recordedParams = logger.recordedEvents[0].parameters
        assertEquals(5.5, recordedParams?.get(AnalyticsKeys.Param.MILEAGE))
        assertEquals("MILES", recordedParams?.get(AnalyticsKeys.Param.DISTANCE_UNIT))
        assertEquals(4, recordedParams?.get(AnalyticsKeys.Param.NUMBER_OF_FAVORITES))
        assertEquals(false, recordedParams?.get("isRetired"))
    }
}