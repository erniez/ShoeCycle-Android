package com.shoecycle.domain.services

import java.util.Date

/**
 * Interface for health service implementations.
 * Provides abstraction for integrating with health tracking services like Health Connect.
 * Supports both mock and real implementations for testing and production.
 */
interface HealthService {
    /**
     * Adds a workout to the health service.
     * @param date The date of the workout
     * @param distance The distance in miles (will be converted as needed)
     * @param shoeId Optional shoe identifier for metadata
     * @return Result containing WorkoutResult on success or exception on failure
     */
    suspend fun addWorkout(
        date: Date,
        distance: Double,
        shoeId: String? = null
    ): Result<WorkoutResult>
    
    /**
     * Checks if the health service is authorized to write data.
     * @return true if authorized, false otherwise
     */
    suspend fun isAuthorized(): Boolean
    
    /**
     * Requests authorization from the user to access health data.
     * @return Result containing true if authorization granted, false if denied
     */
    suspend fun requestAuthorization(): Result<Boolean>
    
    /**
     * Deletes a workout from the health service.
     * @param workoutId The ID of the workout to delete
     * @return Result containing Unit on success or exception on failure
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit>
    
    /**
     * Data class representing the result of adding a workout.
     */
    data class WorkoutResult(
        val workoutId: String,
        val syncedAt: Date,
        val distance: Double,
        val date: Date
    )
    
    /**
     * Common error types for health service operations.
     */
    sealed class HealthServiceError(
        override val message: String
    ) : Exception(message) {
        class AuthorizationDenied(message: String) : HealthServiceError(message)
        class NetworkError(message: String) : HealthServiceError(message)
        class DataError(message: String) : HealthServiceError(message)
        class UnknownError(message: String) : HealthServiceError(message)
    }
}