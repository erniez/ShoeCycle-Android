package com.shoecycle.ui.ftu

import androidx.compose.runtime.mutableStateOf
import com.shoecycle.data.repository.FTURepository
import com.shoecycle.domain.FTUHintManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class FTUInteractorTest {
    
    private lateinit var mockRepository: FTURepository
    private lateinit var interactor: FTUInteractor
    private lateinit var state: androidx.compose.runtime.MutableState<FTUState>
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        mockRepository = mock()
        interactor = FTUInteractor(
            ftuRepository = mockRepository,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        state = mutableStateOf(FTUState())
    }
    
    // Given: Repository has a next hint available
    // When: Checking for hints
    // Then: Should update state with the hint
    @Test
    fun testCheckForHints_hintAvailable_updatesState() = runTest(testDispatcher) {
        whenever(mockRepository.nextHintFlow).thenReturn(
            flowOf(FTUHintManager.HintKey.SWIPE_FEATURE)
        )
        
        interactor.handle(state, FTUInteractor.Action.CheckForHints)
        advanceUntilIdle()
        
        assertEquals(FTUHintManager.HintKey.SWIPE_FEATURE, state.value.currentHint)
        assertEquals(FTUHintManager.HintKey.SWIPE_FEATURE.message, state.value.hintMessage)
        assertFalse(state.value.showHint) // Should not show automatically
    }
    
    // Given: Repository has no hints available
    // When: Checking for hints
    // Then: Should not update state
    @Test
    fun testCheckForHints_noHintAvailable_doesNotUpdateState() = runTest(testDispatcher) {
        whenever(mockRepository.nextHintFlow).thenReturn(flowOf(null))
        
        interactor.handle(state, FTUInteractor.Action.CheckForHints)
        advanceUntilIdle()
        
        assertNull(state.value.currentHint)
        assertEquals("", state.value.hintMessage)
        assertFalse(state.value.showHint)
    }
    
    // Given: State has a current hint
    // When: Showing next hint
    // Then: Should set showHint to true
    @Test
    fun testShowNextHint_withCurrentHint_showsHint() {
        state.value = FTUState(
            currentHint = FTUHintManager.HintKey.SWIPE_FEATURE,
            hintMessage = "Test message",
            showHint = false
        )
        
        interactor.handle(state, FTUInteractor.Action.ShowNextHint)
        
        assertTrue(state.value.showHint)
    }
    
    // Given: State has no current hint
    // When: Showing next hint
    // Then: Should not change showHint
    @Test
    fun testShowNextHint_noCurrentHint_doesNotShow() {
        state.value = FTUState(currentHint = null, showHint = false)
        
        interactor.handle(state, FTUInteractor.Action.ShowNextHint)
        
        assertFalse(state.value.showHint)
    }
    
    // Given: Hint is showing
    // When: Dismissing hint
    // Then: Should set showHint to false
    @Test
    fun testDismissHint_hidesHint() {
        state.value = FTUState(
            currentHint = FTUHintManager.HintKey.SWIPE_FEATURE,
            showHint = true
        )
        
        interactor.handle(state, FTUInteractor.Action.DismissHint)
        
        assertFalse(state.value.showHint)
    }
    
    // Given: Current hint exists
    // When: Completing hint
    // Then: Should complete current and clear state (not load next)
    @Test
    fun testCompleteHint_clearsStateWithoutLoadingNext() = runTest(testDispatcher) {
        state.value = FTUState(
            currentHint = FTUHintManager.HintKey.SWIPE_FEATURE,
            hintMessage = "Swipe message",
            showHint = true
        )
        
        interactor.handle(state, FTUInteractor.Action.CompleteHint)
        advanceUntilIdle()
        
        verify(mockRepository).completeHint(FTUHintManager.HintKey.SWIPE_FEATURE)
        assertNull(state.value.currentHint)
        assertEquals("", state.value.hintMessage)
        assertFalse(state.value.showHint)
    }
    
    // Given: Current hint is the last hint
    // When: Completing hint
    // Then: Should clear state
    @Test
    fun testCompleteHint_lastHint_clearsState() = runTest(testDispatcher) {
        state.value = FTUState(
            currentHint = FTUHintManager.HintKey.YEARLY_HISTORY_FEATURE,
            showHint = true
        )
        
        interactor.handle(state, FTUInteractor.Action.CompleteHint)
        advanceUntilIdle()
        
        verify(mockRepository).completeHint(FTUHintManager.HintKey.YEARLY_HISTORY_FEATURE)
        assertNull(state.value.currentHint)
        assertEquals("", state.value.hintMessage)
        assertFalse(state.value.showHint)
    }
    
    // Given: No current hint
    // When: Completing hint
    // Then: Should not call repository
    @Test
    fun testCompleteHint_noCurrentHint_doesNothing() = runTest(testDispatcher) {
        state.value = FTUState(currentHint = null)
        
        interactor.handle(state, FTUInteractor.Action.CompleteHint)
        advanceUntilIdle()
        
        verify(mockRepository, never()).completeHint(any<FTUHintManager.HintKey>())
    }
    
    // Given: A specific hint that hasn't been completed
    // When: Showing specific hint
    // Then: Should show that hint
    @Test
    fun testShowSpecificHint_uncompletedHint_showsHint() = runTest(testDispatcher) {
        whenever(mockRepository.completedHintsFlow).thenReturn(
            flowOf(emptySet())
        )
        
        interactor.handle(
            state, 
            FTUInteractor.Action.ShowSpecificHint(FTUHintManager.HintKey.STRAVA_FEATURE)
        )
        advanceUntilIdle()
        
        assertEquals(FTUHintManager.HintKey.STRAVA_FEATURE, state.value.currentHint)
        assertEquals(FTUHintManager.HintKey.STRAVA_FEATURE.message, state.value.hintMessage)
        assertTrue(state.value.showHint)
    }
    
    // Given: A specific hint that has been completed
    // When: Showing specific hint
    // Then: Should not show hint
    @Test
    fun testShowSpecificHint_completedHint_doesNotShow() = runTest(testDispatcher) {
        whenever(mockRepository.completedHintsFlow).thenReturn(
            flowOf(setOf(FTUHintManager.HintKey.STRAVA_FEATURE.key))
        )
        
        interactor.handle(
            state,
            FTUInteractor.Action.ShowSpecificHint(FTUHintManager.HintKey.STRAVA_FEATURE)
        )
        advanceUntilIdle()
        
        assertNull(state.value.currentHint)
        assertEquals("", state.value.hintMessage)
        assertFalse(state.value.showHint)
    }
}