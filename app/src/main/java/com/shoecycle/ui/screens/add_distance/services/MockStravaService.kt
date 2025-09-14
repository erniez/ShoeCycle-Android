package com.shoecycle.ui.screens.add_distance.services

import android.util.Log
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

/**
 * Mock implementation of Strava API service for development and testing.
 * Simulates uploading activities to Strava with random success/failure.
 */
class MockStravaService {
    
    companion object {
        private const val TAG = "MockStravaService"
        private const val MOCK_DELAY_MS = 1500L
        private const val SUCCESS_RATE = 0.65 // 65% success rate (slightly less reliable than Health Connect)
    }
    
    data class StravaError(
        val type: ErrorType,
        override val message: String
    ) : Exception(message) {
        enum class ErrorType {
            AUTHENTICATION_FAILED,
            NETWORK_ERROR,
            RATE_LIMIT_EXCEEDED,
            DUPLICATE_ACTIVITY,
            UNKNOWN_ERROR
        }
    }
    
    /**
     * Simulates uploading an activity to Strava.
     * Randomly succeeds or fails to simulate real-world API conditions.
     */
    suspend fun uploadActivity(
        date: Date,
        distance: Double,
        shoeName: String? = null,
        _notes: String? = null
    ): Result<ActivityResult> {
        Log.d(TAG, "Mock: Uploading activity to Strava - Date: $date, Distance: $distance mi, Shoe: $shoeName")
        
        // Simulate API call delay
        delay(MOCK_DELAY_MS)
        
        // Randomly determine success or failure
        return if (Random.nextDouble() < SUCCESS_RATE) {
            val activityId = Random.nextLong(1000000, 9999999)
            Log.d(TAG, "Mock: Successfully uploaded activity with ID: $activityId")
            Result.success(
                ActivityResult(
                    activityId = activityId,
                    uploadedAt = Date(),
                    distance = distance,
                    date = date,
                    kudosCount = Random.nextInt(0, 15),
                    commentCount = Random.nextInt(0, 5)
                )
            )
        } else {
            // Randomly select an error type
            val error = when (Random.nextInt(5)) {
                0 -> StravaError(
                    StravaError.ErrorType.AUTHENTICATION_FAILED,
                    "Mock: Strava access token expired or invalid"
                )
                1 -> StravaError(
                    StravaError.ErrorType.NETWORK_ERROR,
                    "Mock: Unable to connect to Strava servers"
                )
                2 -> StravaError(
                    StravaError.ErrorType.RATE_LIMIT_EXCEEDED,
                    "Mock: Strava API rate limit exceeded. Try again later."
                )
                3 -> StravaError(
                    StravaError.ErrorType.DUPLICATE_ACTIVITY,
                    "Mock: Similar activity already exists for this time"
                )
                else -> StravaError(
                    StravaError.ErrorType.UNKNOWN_ERROR,
                    "Mock: An unexpected error occurred while uploading to Strava"
                )
            }
            Log.e(TAG, "Mock: Failed to upload activity - ${error.message}")
            Result.failure(error)
        }
    }
    
    /**
     * Simulates checking if Strava is authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        delay(500)
        // Randomly return authentication status
        val authenticated = Random.nextDouble() < 0.75 // 75% chance of being authenticated
        Log.d(TAG, "Mock: Strava authentication status: $authenticated")
        return authenticated
    }
    
    /**
     * Simulates Strava OAuth authentication flow.
     */
    suspend fun authenticate(): Result<AuthResult> {
        Log.d(TAG, "Mock: Starting Strava authentication flow")
        delay(2000) // Simulate OAuth redirect time
        
        return if (Random.nextDouble() < 0.85) { // 85% chance of successful auth
            val athleteId = Random.nextLong(100000, 999999)
            Log.d(TAG, "Mock: Authentication successful for athlete ID: $athleteId")
            Result.success(
                AuthResult(
                    accessToken = "mock_access_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                    athleteId = athleteId,
                    athleteName = "Mock Runner ${Random.nextInt(1000)}",
                    expiresAt = Date(System.currentTimeMillis() + 6 * 60 * 60 * 1000) // 6 hours
                )
            )
        } else {
            Log.e(TAG, "Mock: Authentication failed")
            Result.failure(
                StravaError(
                    StravaError.ErrorType.AUTHENTICATION_FAILED,
                    "Mock: User cancelled authentication or denied permissions"
                )
            )
        }
    }
    
    /**
     * Simulates refreshing the Strava access token.
     */
    suspend fun refreshToken(refreshToken: String): Result<AuthResult> {
        Log.d(TAG, "Mock: Refreshing Strava access token")
        delay(1000)
        
        return if (Random.nextDouble() < 0.9) { // 90% success rate for refresh
            Log.d(TAG, "Mock: Token refresh successful")
            Result.success(
                AuthResult(
                    accessToken = "mock_refreshed_token_${System.currentTimeMillis()}",
                    refreshToken = refreshToken,
                    athleteId = Random.nextLong(100000, 999999),
                    athleteName = "Mock Runner",
                    expiresAt = Date(System.currentTimeMillis() + 6 * 60 * 60 * 1000)
                )
            )
        } else {
            Log.e(TAG, "Mock: Token refresh failed")
            Result.failure(
                StravaError(
                    StravaError.ErrorType.AUTHENTICATION_FAILED,
                    "Mock: Refresh token invalid or expired"
                )
            )
        }
    }
    
    /**
     * Simulates deleting an activity from Strava.
     */
    suspend fun deleteActivity(activityId: Long): Result<Unit> {
        Log.d(TAG, "Mock: Deleting activity with ID: $activityId")
        delay(1000)
        
        return if (Random.nextDouble() < SUCCESS_RATE) {
            Log.d(TAG, "Mock: Successfully deleted activity")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Mock: Failed to delete activity")
            Result.failure(
                StravaError(
                    StravaError.ErrorType.UNKNOWN_ERROR,
                    "Mock: Failed to delete activity from Strava"
                )
            )
        }
    }
    
    data class ActivityResult(
        val activityId: Long,
        val uploadedAt: Date,
        val distance: Double,
        val date: Date,
        val kudosCount: Int = 0,
        val commentCount: Int = 0
    )
    
    data class AuthResult(
        val accessToken: String,
        val refreshToken: String,
        val athleteId: Long,
        val athleteName: String,
        val expiresAt: Date
    )
}