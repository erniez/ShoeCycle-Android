package com.shoecycle.domain

import org.junit.Assert.*
import org.junit.Test

class FTUHintManagerTest {
    
    private val manager = FTUHintManager()
    
    // Given: No hints have been completed
    // When: Getting the next hint
    // Then: Should return the first hint in order
    @Test
    fun testGetNextHint_noCompletedHints_returnsFirstHint() {
        val completedHints = emptySet<String>()
        
        val nextHint = manager.getNextHint(completedHints)
        
        assertEquals(FTUHintManager.HintKey.SWIPE_FEATURE, nextHint)
    }
    
    // Given: First hint has been completed
    // When: Getting the next hint
    // Then: Should return the second hint in order
    @Test
    fun testGetNextHint_firstHintCompleted_returnsSecondHint() {
        val completedHints = setOf(FTUHintManager.HintKey.SWIPE_FEATURE.key)
        
        val nextHint = manager.getNextHint(completedHints)
        
        assertEquals(FTUHintManager.HintKey.STRAVA_FEATURE, nextHint)
    }
    
    // Given: Multiple hints have been completed
    // When: Getting the next hint
    // Then: Should return the first uncompleted hint in order
    @Test
    fun testGetNextHint_multipleCompleted_returnsFirstUncompleted() {
        val completedHints = setOf(
            FTUHintManager.HintKey.SWIPE_FEATURE.key,
            FTUHintManager.HintKey.STRAVA_FEATURE.key,
            FTUHintManager.HintKey.HOF_FEATURE.key
        )
        
        val nextHint = manager.getNextHint(completedHints)
        
        assertEquals(FTUHintManager.HintKey.GRAPH_ALL_SHOES_FEATURE, nextHint)
    }
    
    // Given: All hints have been completed
    // When: Getting the next hint
    // Then: Should return null
    @Test
    fun testGetNextHint_allCompleted_returnsNull() {
        val completedHints = FTUHintManager.hintOrder.map { it.key }.toSet()
        
        val nextHint = manager.getNextHint(completedHints)
        
        assertNull(nextHint)
    }
    
    // Given: A hint key
    // When: Getting its message
    // Then: Should return the correct message
    @Test
    fun testGetHintMessage_returnsCorrectMessage() {
        val message = manager.getHintMessage(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        assertEquals(
            "You can swipe between shoes just by swiping up or down on the shoe image in the \"Add Distance\" screen.",
            message
        )
    }
    
    // Given: All hints completed
    // When: Checking if all hints are completed
    // Then: Should return true
    @Test
    fun testAllHintsCompleted_allCompleted_returnsTrue() {
        val completedHints = FTUHintManager.hintOrder.map { it.key }.toSet()
        
        val allCompleted = manager.allHintsCompleted(completedHints)
        
        assertTrue(allCompleted)
    }
    
    // Given: Some hints not completed
    // When: Checking if all hints are completed
    // Then: Should return false
    @Test
    fun testAllHintsCompleted_someIncomplete_returnsFalse() {
        val completedHints = setOf(
            FTUHintManager.HintKey.SWIPE_FEATURE.key,
            FTUHintManager.HintKey.STRAVA_FEATURE.key
        )
        
        val allCompleted = manager.allHintsCompleted(completedHints)
        
        assertFalse(allCompleted)
    }
    
    // Given: A specific hint is completed
    // When: Checking if that hint is completed
    // Then: Should return true
    @Test
    fun testIsHintCompleted_hintInSet_returnsTrue() {
        val completedHints = setOf(FTUHintManager.HintKey.SWIPE_FEATURE.key)
        
        val isCompleted = manager.isHintCompleted(
            FTUHintManager.HintKey.SWIPE_FEATURE,
            completedHints
        )
        
        assertTrue(isCompleted)
    }
    
    // Given: A specific hint is not completed
    // When: Checking if that hint is completed
    // Then: Should return false
    @Test
    fun testIsHintCompleted_hintNotInSet_returnsFalse() {
        val completedHints = setOf(FTUHintManager.HintKey.SWIPE_FEATURE.key)
        
        val isCompleted = manager.isHintCompleted(
            FTUHintManager.HintKey.STRAVA_FEATURE,
            completedHints
        )
        
        assertFalse(isCompleted)
    }
    
    // Given: A string key
    // When: Converting to HintKey enum
    // Then: Should return correct enum value
    @Test
    fun testHintKeyFromKey_validKey_returnsEnum() {
        val hintKey = FTUHintManager.HintKey.fromKey("ShoeCycleFTUSwipeFeature")
        
        assertEquals(FTUHintManager.HintKey.SWIPE_FEATURE, hintKey)
    }
    
    // Given: An invalid string key
    // When: Converting to HintKey enum
    // Then: Should return null
    @Test
    fun testHintKeyFromKey_invalidKey_returnsNull() {
        val hintKey = FTUHintManager.HintKey.fromKey("InvalidKey")
        
        assertNull(hintKey)
    }
    
    // Given: The hint order
    // When: Checking the order
    // Then: Should match expected sequence
    @Test
    fun testHintOrder_matchesExpectedSequence() {
        val expectedOrder = listOf(
            FTUHintManager.HintKey.SWIPE_FEATURE,
            FTUHintManager.HintKey.STRAVA_FEATURE,
            FTUHintManager.HintKey.HOF_FEATURE,
            FTUHintManager.HintKey.GRAPH_ALL_SHOES_FEATURE,
            FTUHintManager.HintKey.EMAIL_HISTORY_FEATURE,
            FTUHintManager.HintKey.YEARLY_HISTORY_FEATURE
        )
        
        assertEquals(expectedOrder, FTUHintManager.hintOrder)
    }
}