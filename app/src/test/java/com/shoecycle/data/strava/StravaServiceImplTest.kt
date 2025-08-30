package com.shoecycle.data.strava

import com.shoecycle.data.strava.models.StravaActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class StravaServiceImplTest {
    
    private lateinit var mockTokenKeeper: StravaTokenKeeper
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var service: StravaServiceImpl
    
    @Before
    fun setup() {
        mockTokenKeeper = mock()
        mockOkHttpClient = mock()
        mockCall = mock()
        service = StravaServiceImpl(mockTokenKeeper, mockOkHttpClient)
    }
    
    // Given: A valid activity and successful token retrieval
    // When: sendActivity is called and the server responds with success
    // Then: The activity should be uploaded without throwing an exception
    @Test
    fun `sendActivity should upload successfully with valid token`() = runTest {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        val accessToken = "valid_access_token"
        
        whenever(mockTokenKeeper.getValidToken()).thenReturn(accessToken)
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_2)
            .code(201)
            .message("Created")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        
        whenever(mockCall.execute()).thenReturn(response)
        
        service.sendActivity(activity)
        
        verify(mockTokenKeeper).getValidToken()
        verify(mockOkHttpClient).newCall(argThat { request ->
            request.header("Authorization") == "Bearer $accessToken" &&
            request.url.toString() == StravaURLs.ACTIVITIES_URL
        })
    }
    
    // Given: A valid activity
    // When: sendActivity is called and the server responds with 401 Unauthorized
    // Then: It should throw StravaService.DomainError.Unauthorized
    @Test(expected = StravaService.DomainError.Unauthorized::class)
    fun `sendActivity should throw Unauthorized when server returns 401`() = runBlocking {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        
        whenever(mockTokenKeeper.getValidToken()).thenReturn("invalid_token")
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_2)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
        
        whenever(mockCall.execute()).thenReturn(response)
        
        service.sendActivity(activity)
    }
    
    // Given: A valid activity
    // When: sendActivity is called and the network request fails with IOException
    // Then: It should throw StravaService.DomainError.Reachability
    @Test(expected = StravaService.DomainError.Reachability::class)
    fun `sendActivity should throw Reachability when network fails`() = runBlocking {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        
        whenever(mockTokenKeeper.getValidToken()).thenReturn("valid_token")
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenThrow(IOException("Network unavailable"))
        
        service.sendActivity(activity)
    }
    
    // Given: A valid activity
    // When: sendActivity is called and the server returns a 500 error
    // Then: It should throw StravaService.DomainError.Reachability
    @Test(expected = StravaService.DomainError.Reachability::class)
    fun `sendActivity should throw Reachability for server errors`() = runBlocking {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        
        whenever(mockTokenKeeper.getValidToken()).thenReturn("valid_token")
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_2)
            .code(500)
            .message("Internal Server Error")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
        
        whenever(mockCall.execute()).thenReturn(response)
        
        service.sendActivity(activity)
    }
    
    // Given: A valid activity
    // When: sendActivity is called and the server returns a 400 bad request
    // Then: It should throw StravaService.DomainError.Unknown
    @Test(expected = StravaService.DomainError.Unknown::class)
    fun `sendActivity should throw Unknown for client errors`() = runBlocking {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        
        whenever(mockTokenKeeper.getValidToken()).thenReturn("valid_token")
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_2)
            .code(400)
            .message("Bad Request")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
        
        whenever(mockCall.execute()).thenReturn(response)
        
        service.sendActivity(activity)
    }
    
    // Given: A valid activity
    // When: sendActivity is called and tokenKeeper throws an exception
    // Then: It should throw StravaService.DomainError.Unknown
    @Test(expected = StravaService.DomainError.Unknown::class)
    fun `sendActivity should throw Unknown when tokenKeeper fails`() = runBlocking {
        val activity = StravaActivity.create(
            name = "Morning Run",
            distanceInMeters = 5000.0,
            startDate = Date()
        )
        
        whenever(mockTokenKeeper.getValidToken()).thenThrow(RuntimeException("Token error"))
        
        service.sendActivity(activity)
    }
}