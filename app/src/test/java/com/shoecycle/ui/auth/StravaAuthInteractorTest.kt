package com.shoecycle.ui.auth

import androidx.compose.runtime.MutableState
import com.shoecycle.data.strava.StravaKeys
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

@RunWith(RobolectricTestRunner::class)
class StravaAuthInteractorTest {
    
    private lateinit var interactor: StravaAuthInteractor
    
    @Before
    fun setup() {
        interactor = StravaAuthInteractor()
    }
    
    @Test
    fun `CloseClicked sets error to user cancelled`() {
        val state = TestMutableState(StravaAuthState())
        
        interactor.handle(state, StravaAuthInteractor.Action.CloseClicked)
        
        assertEquals("User cancelled", state.value.error)
    }
    
    @Test
    fun `PageFinishedLoading sets loading to false`() {
        val state = TestMutableState(StravaAuthState(isLoading = true))
        
        interactor.handle(state, StravaAuthInteractor.Action.PageFinishedLoading)
        
        assertFalse(state.value.isLoading)
    }
    
    @Test
    fun `AuthorizationReceived sets code and stops loading`() {
        val state = TestMutableState(StravaAuthState(isLoading = true))
        val code = "test_auth_code_123"
        
        interactor.handle(state, StravaAuthInteractor.Action.AuthorizationReceived(code))
        
        assertEquals(code, state.value.authorizationCode)
        assertFalse(state.value.isLoading)
        assertNull(state.value.error)
    }
    
    @Test
    fun `ErrorReceived sets error and stops loading`() {
        val state = TestMutableState(StravaAuthState(isLoading = true))
        val errorMessage = "Invalid client_id"
        
        interactor.handle(state, StravaAuthInteractor.Action.ErrorReceived(errorMessage))
        
        assertEquals(errorMessage, state.value.error)
        assertFalse(state.value.isLoading)
        assertNull(state.value.authorizationCode)
    }
    
    @Test
    fun `buildOAuthUrl creates proper URL with all parameters`() {
        val url = interactor.buildOAuthUrl()
        
        assertTrue(url.startsWith("https://www.strava.com/oauth/mobile/authorize"))
        assertTrue(url.contains("client_id=${StravaKeys.CLIENT_ID_VALUE}"))
        assertTrue(url.contains("redirect_uri=ShoeCycle%3A%2F%2Fshoecycleapp.com%2Fcallback%2F"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("approval_prompt=auto"))
        assertTrue(url.contains("scope=activity%3Awrite%2Cread"))
        assertTrue(url.contains("state=strava_auth"))
    }
    
    @Test
    fun `shouldHandleUrl returns true for redirect URI`() {
        val validUrls = listOf(
            "ShoeCycle://shoecycleapp.com/callback/",
            "ShoeCycle://shoecycleapp.com/callback/?code=123",
            "shoecycle://shoecycleapp.com/callback/", // Case insensitive
            "SHOECYCLE://SHOECYCLEAPP.COM/CALLBACK/"
        )
        
        validUrls.forEach { url ->
            assertTrue("Should handle: $url", interactor.shouldHandleUrl(url))
        }
    }
    
    @Test
    fun `shouldHandleUrl returns false for non-redirect URLs`() {
        val invalidUrls = listOf(
            "https://www.strava.com/oauth/authorize",
            "https://www.google.com",
            "SomeOtherApp://callback/",
            ""
        )
        
        invalidUrls.forEach { url ->
            assertFalse("Should not handle: $url", interactor.shouldHandleUrl(url))
        }
    }
    
    @Test
    fun `extractAuthorizationCode returns code from valid URL`() {
        val urls = mapOf(
            "ShoeCycle://shoecycleapp.com/callback/?code=abc123" to "abc123",
            "ShoeCycle://shoecycleapp.com/callback/?code=xyz789&state=test" to "xyz789",
            "ShoeCycle://shoecycleapp.com/callback/?other=param&code=test456" to "test456"
        )
        
        urls.forEach { (url, expectedCode) ->
            val code = interactor.extractAuthorizationCode(url)
            assertEquals("URL: $url", expectedCode, code)
        }
    }
    
    @Test
    fun `extractAuthorizationCode returns null when no code present`() {
        val urls = listOf(
            "ShoeCycle://shoecycleapp.com/callback/",
            "ShoeCycle://shoecycleapp.com/callback/?error=access_denied",
            "ShoeCycle://shoecycleapp.com/callback/?state=test"
        )
        
        urls.forEach { url ->
            val code = interactor.extractAuthorizationCode(url)
            assertNull("Should return null for URL: $url", code)
        }
    }
    
    @Test
    fun `extractError returns error from URL`() {
        val error = interactor.extractError("ShoeCycle://shoecycleapp.com/callback/?error=access_denied")
        assertEquals("access_denied", error)
    }
    
    @Test
    fun `extractError returns error_description when present`() {
        val url = "ShoeCycle://shoecycleapp.com/callback/?error=invalid_request&error_description=The+request+is+missing+a+required+parameter"
        val error = interactor.extractError(url)
        assertEquals("The request is missing a required parameter", error)
    }
    
    @Test
    fun `extractError prefers error_description over error`() {
        val url = "ShoeCycle://shoecycleapp.com/callback/?error=invalid_request&error_description=Detailed+error+message"
        val error = interactor.extractError(url)
        assertEquals("Detailed error message", error)
    }
    
    @Test
    fun `extractError returns null when no error present`() {
        val urls = listOf(
            "ShoeCycle://shoecycleapp.com/callback/?code=123",
            "ShoeCycle://shoecycleapp.com/callback/",
            "ShoeCycle://shoecycleapp.com/callback/?state=test"
        )
        
        urls.forEach { url ->
            val error = interactor.extractError(url)
            assertNull("Should return null for URL: $url", error)
        }
    }
    
    @Test
    fun `initial state has correct defaults`() {
        val state = StravaAuthState()
        
        assertTrue(state.isLoading)
        assertNull(state.authorizationCode)
        assertNull(state.error)
    }
}