package com.shoecycle.ui.screens.add_distance.services

import android.util.Log
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

/**
 * Mock implementation of Health Connect service for development and testing.
 * Simulates adding workout data to Health Connect with random success/failure.
 */
class MockHealthService {
    
    companion object {
        private const val TAG = "MockHealthService"
        private const val MOCK_DELAY_MS = 1000L
        private const val SUCCESS_RATE = 0.7 // 70% success rate
    }
    
    data class HealthConnectError(
        val type: ErrorType,
        override val message: String
    ) : Exception(message) {
        enum class ErrorType {
            AUTHORIZATION_DENIED,
            NETWORK_ERROR,
            UNKNOWN_ERROR
        }
    }
    
    /**
     * Simulates adding a workout to Health Connect.
     * Randomly succeeds or fails to simulate real-world conditions.
     */
    suspend fun addWorkout(
        date: Date,
        distance: Double,
        durationMinutes: Int? = null
    ): Result<WorkoutResult> {
        Log.d(TAG, "Mock: Adding workout - Date: $date, Distance: $distance mi")
        
        // Simulate network delay
        delay(MOCK_DELAY_MS)
        
        // Randomly determine success or failure
        return if (Random.nextDouble() < SUCCESS_RATE) {
            val workoutId = "mock_workout_${System.currentTimeMillis()}"
            Log.d(TAG, "Mock: Successfully added workout with ID: $workoutId")
            Result.success(
                WorkoutResult(
                    workoutId = workoutId,
                    syncedAt = Date(),
                    distance = distance,
                    date = date
                )
            )
        } else {
            // Randomly select an error type
            val error = when (Random.nextInt(3)) {
                0 -> HealthConnectError(
                    HealthConnectError.ErrorType.AUTHORIZATION_DENIED,
                    "Mock: User has not granted permission to write workout data"
                )
                1 -> HealthConnectError(
                    HealthConnectError.ErrorType.NETWORK_ERROR,
                    "Mock: Unable to sync with Health Connect servers"
                )
                else -> HealthConnectError(
                    HealthConnectError.ErrorType.UNKNOWN_ERROR,
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
    suspend fun isAuthorized(): Boolean {
        delay(500)
        // Randomly return authorization status
        val authorized = Random.nextBoolean()
        Log.d(TAG, "Mock: Health Connect authorization status: $authorized")
        return authorized
    }
    
    /**
     * Simulates requesting Health Connect permissions.
     */
    suspend fun requestAuthorization(): Result<Boolean> {
        Log.d(TAG, "Mock: Requesting Health Connect authorization")
        delay(1500)
        
        return if (Random.nextDouble() < 0.8) { // 80% chance of success
            Log.d(TAG, "Mock: Authorization granted")
            Result.success(true)
        } else {
            Log.d(TAG, "Mock: Authorization denied")
            Result.failure(
                HealthConnectError(
                    HealthConnectError.ErrorType.AUTHORIZATION_DENIED,
                    "Mock: User denied Health Connect permissions"
                )
            )
        }
    }
    
    /**
     * Simulates deleting a workout from Health Connect.
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        Log.d(TAG, "Mock: Deleting workout with ID: $workoutId")
        delay(800)
        
        return if (Random.nextDouble() < SUCCESS_RATE) {
            Log.d(TAG, "Mock: Successfully deleted workout")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Mock: Failed to delete workout")
            Result.failure(
                HealthConnectError(
                    HealthConnectError.ErrorType.UNKNOWN_ERROR,
                    "Mock: Failed to delete workout from Health Connect"
                )
            )
        }
    }
    
    data class WorkoutResult(
        val workoutId: String,
        val syncedAt: Date,
        val distance: Double,
        val date: Date
    )
}