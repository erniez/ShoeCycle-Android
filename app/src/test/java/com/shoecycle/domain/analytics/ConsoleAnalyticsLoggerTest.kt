package com.shoecycle.domain.analytics

import android.util.Log
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ConsoleAnalyticsLoggerTest {
    
    private lateinit var logger: ConsoleAnalyticsLogger
    
    @Before
    fun setUp() {
        logger = ConsoleAnalyticsLogger()
        // Enable logging to be captured by Robolectric
        ShadowLog.stream = System.out
    }
    
    @After
    fun tearDown() {
        // Clear logs after each test
        ShadowLog.clear()
    }
    
    @Test
    fun `initialize logs initialization message`() {
        // When
        logger.initialize()
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics initialized (Console Logger)" })
    }
    
    @Test
    fun `logEvent logs event name without parameters`() {
        // Given
        val eventName = AnalyticsKeys.Event.ADD_SHOE
        
        // When
        logger.logEvent(eventName)
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics Event: $eventName" })
    }
    
    @Test
    fun `logEvent logs event name with single parameter`() {
        // Given
        val eventName = AnalyticsKeys.Event.LOG_MILEAGE
        val parameters = mapOf(AnalyticsKeys.Param.MILEAGE to 5.5)
        
        // When
        logger.logEvent(eventName, parameters)
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics Event: $eventName" })
        assertTrue(logs.any { it.msg == "*** Analytics   ${AnalyticsKeys.Param.MILEAGE}: 5.5" })
    }
    
    @Test
    fun `logEvent logs event name with multiple parameters`() {
        // Given
        val eventName = AnalyticsKeys.Event.LOG_MILEAGE
        val parameters = mapOf(
            AnalyticsKeys.Param.MILEAGE to 10.0,
            AnalyticsKeys.Param.DISTANCE_UNIT to "MILES",
            AnalyticsKeys.Param.TOTAL_MILEAGE to 150.5
        )
        
        // When
        logger.logEvent(eventName, parameters)
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics Event: $eventName" })
        parameters.forEach { (key, value) ->
            assertTrue(logs.any { it.msg == "*** Analytics   $key: $value" })
        }
    }
    
    @Test
    fun `logEvent handles null parameters gracefully`() {
        // Given
        val eventName = AnalyticsKeys.Event.VIEW_SHOE_DETAIL
        
        // When
        logger.logEvent(eventName, null)
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics Event: $eventName" })
        // Should only have one log entry (the event name)
        val analyticsLogs = logs.filter { it.msg.startsWith("*** Analytics") }
        assertTrue(analyticsLogs.size == 1)
    }
    
    @Test
    fun `logEvent handles empty parameters map`() {
        // Given
        val eventName = AnalyticsKeys.Event.SHOW_HISTORY
        val parameters = emptyMap<String, Any>()
        
        // When
        logger.logEvent(eventName, parameters)
        
        // Then
        val logs = ShadowLog.getLogsForTag("ShoeCycle")
        assertTrue(logs.any { it.msg == "*** Analytics Event: $eventName" })
        // Should only have one log entry (the event name)
        val analyticsLogs = logs.filter { it.msg.startsWith("*** Analytics") }
        assertTrue(analyticsLogs.size == 1)
    }
}