package com.shoecycle.domain.analytics

/**
 * Mock implementation of AnalyticsLogger for testing purposes.
 * Records all events for verification in unit tests without external dependencies.
 */
class MockAnalyticsLogger : AnalyticsLogger {
    
    /**
     * Data class representing a recorded analytics event.
     */
    data class AnalyticsEvent(
        val name: String,
        val parameters: Map<String, Any>?
    )
    
    /**
     * List of all recorded events for test verification.
     */
    val recordedEvents = mutableListOf<AnalyticsEvent>()
    
    /**
     * Track whether initialize was called.
     */
    var isInitialized = false
        private set
    
    override fun initialize() {
        isInitialized = true
    }
    
    override fun logEvent(name: String, parameters: Map<String, Any>?) {
        recordedEvents.add(AnalyticsEvent(name, parameters))
    }
    
    /**
     * Clears all recorded events and reset initialization state.
     * Useful for test cleanup between test cases.
     */
    fun reset() {
        recordedEvents.clear()
        isInitialized = false
    }
    
    /**
     * Helper function to check if a specific event was logged.
     * 
     * @param eventName The name of the event to check for
     * @return True if the event was logged, false otherwise
     */
    fun hasLoggedEvent(eventName: String): Boolean {
        return recordedEvents.any { it.name == eventName }
    }
    
    /**
     * Helper function to get all events of a specific type.
     * 
     * @param eventName The name of the event to filter by
     * @return List of matching events
     */
    fun getEventsOfType(eventName: String): List<AnalyticsEvent> {
        return recordedEvents.filter { it.name == eventName }
    }
    
    /**
     * Helper function to get the last logged event.
     * 
     * @return The last event or null if no events logged
     */
    fun getLastEvent(): AnalyticsEvent? {
        return recordedEvents.lastOrNull()
    }
}