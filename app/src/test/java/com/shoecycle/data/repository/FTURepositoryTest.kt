package com.shoecycle.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shoecycle.domain.FTUHintManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FTURepositoryTest {
    
    private lateinit var context: Context
    private lateinit var repository: FTURepository
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        repository = FTURepository(context)
    }
    
    // Given: No hints have been completed
    // When: Observing completed hints flow
    // Then: Should return empty set
    @Test
    fun testCompletedHintsFlow_initialState_returnsEmptySet() = runTest {
        // Clear any existing preferences
        repository.resetAllHints()
        
        val completedHints = repository.completedHintsFlow.first()
        
        assertTrue(completedHints.isEmpty())
    }
    
    // Given: No hints have been completed
    // When: Observing next hint flow
    // Then: Should return first hint
    @Test
    fun testNextHintFlow_noCompletedHints_returnsFirstHint() = runTest {
        repository.resetAllHints()
        
        val nextHint = repository.nextHintFlow.first()
        
        assertEquals(FTUHintManager.HintKey.SWIPE_FEATURE, nextHint)
    }
    
    // Given: A hint to complete
    // When: Completing the hint
    // Then: Should be added to completed hints
    @Test
    fun testCompleteHint_addsToCompletedHints() = runTest {
        repository.resetAllHints()
        
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        val completedHints = repository.completedHintsFlow.first()
        assertTrue(completedHints.contains(FTUHintManager.HintKey.SWIPE_FEATURE.key))
    }
    
    // Given: First hint completed
    // When: Observing next hint flow
    // Then: Should return second hint
    @Test
    fun testNextHintFlow_afterCompletingFirst_returnsSecondHint() = runTest {
        repository.resetAllHints()
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        val nextHint = repository.nextHintFlow.first()
        
        assertEquals(FTUHintManager.HintKey.STRAVA_FEATURE, nextHint)
    }
    
    // Given: A hint key string
    // When: Completing hint by string
    // Then: Should be added to completed hints
    @Test
    fun testCompleteHintByString_validKey_addsToCompletedHints() = runTest {
        repository.resetAllHints()
        
        repository.completeHint("ShoeCycleFTUSwipeFeature")
        
        val completedHints = repository.completedHintsFlow.first()
        assertTrue(completedHints.contains("ShoeCycleFTUSwipeFeature"))
    }
    
    // Given: Invalid hint key string
    // When: Completing hint by string
    // Then: Should not add anything
    @Test
    fun testCompleteHintByString_invalidKey_doesNothing() = runTest {
        repository.resetAllHints()
        
        repository.completeHint("InvalidKey")
        
        val completedHints = repository.completedHintsFlow.first()
        assertTrue(completedHints.isEmpty())
    }
    
    // Given: All hints completed
    // When: Observing next hint flow
    // Then: Should return null
    @Test
    fun testNextHintFlow_allCompleted_returnsNull() = runTest {
        repository.resetAllHints()
        
        // Complete all hints
        FTUHintManager.hintOrder.forEach { hint ->
            repository.completeHint(hint)
        }
        
        val nextHint = repository.nextHintFlow.first()
        
        assertNull(nextHint)
    }
    
    // Given: Some hints completed
    // When: Resetting all hints
    // Then: Should clear all completed hints
    @Test
    fun testResetAllHints_clearsCompletedHints() = runTest {
        // Complete some hints
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        repository.completeHint(FTUHintManager.HintKey.STRAVA_FEATURE)
        
        repository.resetAllHints()
        
        val completedHints = repository.completedHintsFlow.first()
        assertTrue(completedHints.isEmpty())
    }
    
    // Given: A completed hint
    // When: Checking if hint is completed
    // Then: Should return true
    @Test
    fun testIsHintCompleted_completedHint_returnsTrue() = runTest {
        repository.resetAllHints()
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        val isCompleted = repository.isHintCompleted(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        assertTrue(isCompleted)
    }
    
    // Given: An uncompleted hint
    // When: Checking if hint is completed
    // Then: Should return false
    @Test
    fun testIsHintCompleted_uncompletedHint_returnsFalse() = runTest {
        repository.resetAllHints()
        
        val isCompleted = repository.isHintCompleted(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        assertFalse(isCompleted)
    }
    
    // Given: Multiple hints completed
    // When: Completing the same hint again
    // Then: Should not duplicate in set
    @Test
    fun testCompleteHint_duplicate_doesNotDuplicate() = runTest {
        repository.resetAllHints()
        
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        repository.completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        
        val completedHints = repository.completedHintsFlow.first()
        assertEquals(1, completedHints.size)
        assertTrue(completedHints.contains(FTUHintManager.HintKey.SWIPE_FEATURE.key))
    }
}