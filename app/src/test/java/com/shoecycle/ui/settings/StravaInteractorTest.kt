package com.shoecycle.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import com.shoecycle.data.strava.StravaTokenKeeper
import com.shoecycle.data.strava.models.StravaToken
import com.shoecycle.ui.auth.StravaAuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

// Test implementation of MutableState for unit testing
class TestMutableState<T>(initialValue: T) : MutableState<T> {
    private var currentValue = initialValue
    
    override var value: T
        get() = currentValue
        set(value) {
            currentValue = value
        }
        
    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StravaInteractorTest {
    
    @Mock
    private lateinit var tokenKeeper: StravaTokenKeeper
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var authLauncher: ActivityResultLauncher<Intent>
    
    private lateinit var interactor: StravaInteractor
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        interactor = StravaInteractor(
            tokenKeeper = tokenKeeper,
            scope = CoroutineScope(testDispatcher)
        )
    }
    
    @Test
    fun `ConnectClicked with null context shows error`() {
        val state = TestMutableState(StravaState())
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, null)
        
        assertEquals("Unable to start authentication", state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `ConnectClicked with context and no launcher shows error`() {
        val state = TestMutableState(StravaState())
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, context)
        
        assertEquals("Authentication launcher not configured", state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `ConnectClicked with context and launcher starts OAuth flow`() {
        val state = TestMutableState(StravaState())
        interactor.setAuthLauncher(authLauncher)
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, context)
        
        assertTrue(state.value.isLoading)
        assertNull(state.value.error)
        
        val intentCaptor = argumentCaptor<Intent>()
        verify(authLauncher).launch(intentCaptor.capture())
        assertEquals(StravaAuthActivity::class.java.name, intentCaptor.firstValue.component?.className)
    }
    
    @Test
    fun `DisconnectClicked clears token and updates state`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState(
            isConnected = true,
            athleteName = "Test Athlete"
        ))
        
        interactor.handle(state, StravaInteractor.Action.DisconnectClicked)
        advanceUntilIdle()
        
        verify(tokenKeeper).clearToken()
        assertFalse(state.value.isConnected)
        assertNull(state.value.athleteName)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `DisconnectClicked handles exception`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState(isConnected = true))
        val errorMessage = "Clear token failed"
        whenever(tokenKeeper.clearToken()).thenThrow(RuntimeException(errorMessage))
        
        interactor.handle(state, StravaInteractor.Action.DisconnectClicked)
        advanceUntilIdle()
        
        assertTrue(state.value.error?.contains(errorMessage) == true)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `AuthorizationFailed sets error state`() {
        val state = TestMutableState(StravaState(isLoading = true))
        val errorMessage = "User cancelled"
        
        interactor.handle(state, StravaInteractor.Action.AuthorizationFailed(errorMessage))
        
        assertEquals(errorMessage, state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `DismissError clears error`() {
        val state = TestMutableState(StravaState(error = "Some error"))
        
        interactor.handle(state, StravaInteractor.Action.DismissError)
        
        assertNull(state.value.error)
    }
    
    @Test
    fun `ViewAppeared checks connection status when token exists and not expired`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        val mockToken = mock(StravaToken::class.java)
        whenever(mockToken.isExpired).thenReturn(false)
        whenever(mockToken.athleteFullName).thenReturn("John Doe")
        whenever(tokenKeeper.getStoredToken()).thenReturn(mockToken)
        
        interactor.handle(state, StravaInteractor.Action.ViewAppeared)
        advanceUntilIdle()
        
        assertTrue(state.value.isConnected)
        assertEquals("John Doe", state.value.athleteName)
    }
    
    @Test
    fun `ViewAppeared shows not connected when token is expired`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        val mockToken = mock(StravaToken::class.java)
        whenever(mockToken.isExpired).thenReturn(true)
        whenever(tokenKeeper.getStoredToken()).thenReturn(mockToken)
        
        interactor.handle(state, StravaInteractor.Action.ViewAppeared)
        advanceUntilIdle()
        
        assertFalse(state.value.isConnected)
        assertNull(state.value.athleteName)
    }
    
    @Test
    fun `ViewAppeared shows not connected when no token exists`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        whenever(tokenKeeper.getStoredToken()).thenReturn(null)
        
        interactor.handle(state, StravaInteractor.Action.ViewAppeared)
        advanceUntilIdle()
        
        assertFalse(state.value.isConnected)
        assertNull(state.value.athleteName)
    }
    
    @Test
    fun `ViewAppeared handles exception gracefully`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        whenever(tokenKeeper.getStoredToken()).thenThrow(RuntimeException("Storage error"))
        
        interactor.handle(state, StravaInteractor.Action.ViewAppeared)
        advanceUntilIdle()
        
        assertFalse(state.value.isConnected)
        assertNull(state.value.athleteName)
    }
    
    @Test
    fun `handleActivityResult with RESULT_OK and code triggers AuthorizationReceived`() {
        val state = TestMutableState(StravaState())
        val code = "test_auth_code"
        val intent = mock(Intent::class.java)
        whenever(intent.getStringExtra(StravaAuthActivity.RESULT_AUTHORIZATION_CODE)).thenReturn(code)
        
        // We can't directly test the authorization code exchange without mocking HTTP,
        // but we can verify the state changes to loading
        interactor.handleActivityResult(state, Activity.RESULT_OK, intent)
        
        assertTrue(state.value.isLoading)
    }
    
    @Test
    fun `handleActivityResult with RESULT_OK but no code shows error`() {
        val state = TestMutableState(StravaState())
        val intent = mock(Intent::class.java)
        whenever(intent.getStringExtra(StravaAuthActivity.RESULT_AUTHORIZATION_CODE)).thenReturn(null)
        
        interactor.handleActivityResult(state, Activity.RESULT_OK, intent)
        
        assertEquals("No authorization code received", state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `handleActivityResult with RESULT_CANCELED shows error`() {
        val state = TestMutableState(StravaState())
        val errorMessage = "User denied access"
        val intent = mock(Intent::class.java)
        whenever(intent.getStringExtra(StravaAuthActivity.RESULT_ERROR)).thenReturn(errorMessage)
        
        interactor.handleActivityResult(state, Activity.RESULT_CANCELED, intent)
        
        assertEquals(errorMessage, state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `handleActivityResult with RESULT_CANCELED and no error shows default message`() {
        val state = TestMutableState(StravaState())
        val intent = mock(Intent::class.java)
        whenever(intent.getStringExtra(StravaAuthActivity.RESULT_ERROR)).thenReturn(null)
        
        interactor.handleActivityResult(state, Activity.RESULT_CANCELED, intent)
        
        assertEquals("Authentication cancelled", state.value.error)
        assertFalse(state.value.isLoading)
    }
}