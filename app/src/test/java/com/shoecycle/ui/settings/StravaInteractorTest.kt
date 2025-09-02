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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
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
        Dispatchers.setMain(testDispatcher)
        interactor = StravaInteractor(
            tokenKeeper = tokenKeeper,
            scope = CoroutineScope(testDispatcher)
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `ConnectClicked with null context shows error`() {
        val state = TestMutableState(StravaState())
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, null)
        
        assertEquals("Unable to start authentication", state.value.error)
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `ConnectClicked with context and no launcher shows loading in mock mode`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, context)
        
        // In debug mode with USE_MOCK_SERVICES=true, it uses mock auth
        assertTrue(state.value.isLoading)
        assertNull(state.value.error)
    }
    
    @Test
    fun `ConnectClicked with context and launcher uses mock auth in debug mode`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState())
        interactor.setAuthLauncher(authLauncher)
        
        // Mock the storeToken call
        doNothing().whenever(tokenKeeper).storeToken(any())
        
        interactor.handle(state, StravaInteractor.Action.ConnectClicked, context)
        
        // In debug mode with USE_MOCK_SERVICES=true, it uses mock auth instead of launcher
        assertTrue(state.value.isLoading)
        assertNull(state.value.error)
        
        // Launcher should NOT be called in mock mode
        verify(authLauncher, never()).launch(any())
        
        // Wait for the mock auth to complete
        advanceUntilIdle()
        
        // After mock auth completes
        assertTrue(state.value.isConnected)
        assertFalse(state.value.isLoading)
        assertEquals("Test Runner", state.value.athleteName)
    }
    
    @Test
    fun `DisconnectClicked clears token and updates state`() = runTest(testDispatcher) {
        val state = TestMutableState(StravaState(
            isConnected = true,
            athleteName = "Test Athlete"
        ))
        
        // clearToken is a void method, no need to stub it
        
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
        
        assertEquals("Failed to disconnect: $errorMessage", state.value.error)
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
        val mockToken = StravaToken(
            tokenType = "Bearer",
            accessToken = "test_token",
            refreshToken = "refresh_token",
            expiresAt = System.currentTimeMillis() / 1000 + 3600, // Not expired
            expiresIn = 3600,
            athleteFirstName = "John",
            athleteLastName = "Doe"
        )
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