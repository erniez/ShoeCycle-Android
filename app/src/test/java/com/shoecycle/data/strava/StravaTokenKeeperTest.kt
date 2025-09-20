package com.shoecycle.data.strava

import com.shoecycle.data.strava.models.StravaToken
import com.shoecycle.data.strava.storage.InMemoryTokenStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for StravaTokenKeeper functionality.
 * Tests token storage, retrieval, and expiration checking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StravaTokenKeeperTest {
    
    private lateinit var tokenKeeper: StravaTokenKeeper
    private lateinit var storage: InMemoryTokenStorage
    
    @Before
    fun setup() {
        storage = InMemoryTokenStorage()
        tokenKeeper = StravaTokenKeeper(storage)
        // Clear any existing tokens before each test
        tokenKeeper.clearToken()
    }
    
    @Test
    fun `test storing and retrieving token`() {
        // Create a test token
        val token = createTestToken()
        
        // Store the token
        tokenKeeper.storeToken(token)
        
        // Retrieve the token
        val retrievedToken = tokenKeeper.getStoredToken()
        
        // Verify the token was stored and retrieved correctly
        assertNotNull(retrievedToken)
        assertEquals(token.accessToken, retrievedToken?.accessToken)
        assertEquals(token.refreshToken, retrievedToken?.refreshToken)
        assertEquals(token.expiresAt, retrievedToken?.expiresAt)
        assertEquals(token.athleteId, retrievedToken?.athleteId)
        assertEquals(token.athleteFirstName, retrievedToken?.athleteFirstName)
        assertEquals(token.athleteLastName, retrievedToken?.athleteLastName)
    }
    
    @Test
    fun `test token expiration check`() {
        // Create an expired token
        val expiredToken = createTestToken(
            expiresAt = (System.currentTimeMillis() / 1000) - 3600 // 1 hour ago
        )
        
        // Create a valid token
        val validToken = createTestToken(
            expiresAt = (System.currentTimeMillis() / 1000) + 3600 // 1 hour from now
        )
        
        // Check expiration
        assertTrue("Expired token should be marked as expired", expiredToken.isExpired)
        assertFalse("Valid token should not be marked as expired", validToken.isExpired)
    }
    
    @Test
    fun `test clearing token`() {
        // Store a token
        val token = createTestToken()
        tokenKeeper.storeToken(token)
        
        // Verify it's stored
        assertTrue(tokenKeeper.hasStoredToken())
        
        // Clear the token
        tokenKeeper.clearToken()
        
        // Verify it's cleared
        assertFalse(tokenKeeper.hasStoredToken())
        assertNull(tokenKeeper.getStoredToken())
    }
    
    @Test
    fun `test hasStoredToken`() {
        // Initially should have no token
        assertFalse(tokenKeeper.hasStoredToken())
        
        // Store a token
        val token = createTestToken()
        tokenKeeper.storeToken(token)
        
        // Should now have a token
        assertTrue(tokenKeeper.hasStoredToken())
    }
    
    @Test
    fun `test athlete full name`() {
        // Test with both first and last name
        val token1 = createTestToken(
            athleteFirstName = "John",
            athleteLastName = "Doe"
        )
        assertEquals("John Doe", token1.athleteFullName)
        
        // Test with only first name
        val token2 = createTestToken(
            athleteFirstName = "Jane",
            athleteLastName = null
        )
        assertEquals("Jane", token2.athleteFullName)
        
        // Test with only last name
        val token3 = createTestToken(
            athleteFirstName = null,
            athleteLastName = "Smith"
        )
        assertEquals("Smith", token3.athleteFullName)
        
        // Test with no name
        val token4 = createTestToken(
            athleteFirstName = null,
            athleteLastName = null
        )
        assertNull(token4.athleteFullName)
    }
    
    @Test
    fun `test token fromJson`() {
        val jsonString = """
            {
                "token_type": "Bearer",
                "expires_at": 1234567890,
                "expires_in": 3600,
                "refresh_token": "refresh_token_value",
                "access_token": "access_token_value",
                "athlete": {
                    "id": 123456,
                    "username": "johndoe",
                    "firstname": "John",
                    "lastname": "Doe",
                    "profile": "https://example.com/profile.jpg"
                }
            }
        """.trimIndent()

        val token = StravaToken.fromJson(jsonString)
        
        assertEquals("Bearer", token.tokenType)
        assertEquals(1234567890L, token.expiresAt)
        assertEquals(3600L, token.expiresIn)
        assertEquals("refresh_token_value", token.refreshToken)
        assertEquals("access_token_value", token.accessToken)
        assertEquals(123456L, token.athleteId)
        assertEquals("johndoe", token.athleteUsername)
        assertEquals("John", token.athleteFirstName)
        assertEquals("Doe", token.athleteLastName)
        assertEquals("https://example.com/profile.jpg", token.athleteProfilePicture)
    }
    
    private fun createTestToken(
        expiresAt: Long = (System.currentTimeMillis() / 1000) + 3600,
        athleteFirstName: String? = "Test",
        athleteLastName: String? = "User"
    ): StravaToken {
        return StravaToken(
            tokenType = "Bearer",
            expiresAt = expiresAt,
            expiresIn = 3600,
            refreshToken = "test_refresh_token",
            accessToken = "test_access_token",
            athleteId = 12345,
            athleteUsername = "testuser",
            athleteFirstName = athleteFirstName,
            athleteLastName = athleteLastName,
            athleteProfilePicture = "https://example.com/photo.jpg"
        )
    }
}