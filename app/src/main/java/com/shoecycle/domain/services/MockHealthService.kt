package com.shoecycle.domain.services

import android.util.Log
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

/**
 * Mock implementation of Health Connect service for development and testing.
 * Simulates adding workout data to Health Connect with random success/failure.
 */
class MockHealthService : HealthService {
    
    companion object {
        private const val TAG = "MockHealthService"
        private const val MOCK_DELAY_MS = 1000L
        private const val SUCCESS_RATE = 0.7 // 70% success rate
        private const val AUTHORIZATION_SUCCESS_RATE = 0.8 // 80% success rate for auth
    }
    
    /**
     * Simulates adding a workout to Health Connect.
     * Randomly succeeds or fails to simulate real-world conditions.
     */
    override suspend fun addWorkout(
        date: Date,
        distance: Double,
        shoeId: String?
    ): Result<HealthService.WorkoutResult> {
        Log.d(TAG, "Mock: Adding workout - Date: $date, Distance: $distance mi, ShoeId: $shoeId")
        
        // Simulate network delay
        delay(MOCK_DELAY_MS)
        
        // Randomly determine success or failure
        return if (Random.nextDouble() < SUCCESS_RATE) {
            val workoutId = "mock_workout_${System.currentTimeMillis()}"
            Log.d(TAG, "Mock: Successfully added workout with ID: $workoutId")
            Result.success(
                HealthService.WorkoutResult(
                    workoutId = workoutId,
                    syncedAt = Date(),
                    distance = distance,
                    date = date
                )
            )
        } else {
            // Randomly select an error type
            val error = when (Random.nextInt(3)) {
                0 -> HealthService.HealthServiceError.AuthorizationDenied(
                    "Mock: User has not granted permission to write workout data"
                )
                1 -> HealthService.HealthServiceError.NetworkError(
                    "Mock: Unable to sync with Health Connect servers"
                )
                else -> HealthService.HealthServiceError.UnknownError(
                    "Mock: An unexpected error occurred"
                )
            }
            Log.e(TAG, "Mock: Failed to add workout - ${error.message}")
            Result.failure(error)
        }
    }
    
    /**
     * Simulates checking if Health Connect is available and authorized.
     */
    override suspend fun isAuthorized(): Boolean {
        delay(500)
        // Randomly return authorization status
        val authorized = Random.nextBoolean()
        Log.d(TAG, "Mock: Health Connect authorization status: $authorized")
        return authorized
    }
    
    /**
     * Simulates requesting Health Connect permissions.
     */
    override suspend fun requestAuthorization(): Result<Boolean> {
        Log.d(TAG, "Mock: Requesting Health Connect authorization")
        delay(1500)
        
        return if (Random.nextDouble() < AUTHORIZATION_SUCCESS_RATE) {
            Log.d(TAG, "Mock: Authorization granted")
            Result.success(true)
        } else {
            Log.d(TAG, "Mock: Authorization denied")
            Result.failure(
                HealthService.HealthServiceError.AuthorizationDenied(
                    "Mock: User denied Health Connect permissions"
                )
            )
        }
    }
    
    /**
     * Simulates deleting a workout from Health Connect.
     */
    override suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        Log.d(TAG, "Mock: Deleting workout with ID: $workoutId")
        delay(800)
        
        return if (Random.nextDouble() < SUCCESS_RATE) {
            Log.d(TAG, "Mock: Successfully deleted workout")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Mock: Failed to delete workout")
            Result.failure(
                HealthService.HealthServiceError.UnknownError(
                    "Mock: Failed to delete workout from Health Connect"
                )
            )
        }
    }
}