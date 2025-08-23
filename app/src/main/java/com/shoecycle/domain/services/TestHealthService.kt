package com.shoecycle.domain.services

import java.util.Date

/**
 * Test implementation of HealthService for unit testing.
 * Provides predictable behavior and verification capabilities.
 */
class TestHealthService : HealthService {
    
    var shouldSucceed = true
    var authorizationStatus = true
    var addWorkoutCallCount = 0
    var deleteWorkoutCallCount = 0
    var lastAddedWorkout: Triple<Date, Double, String?>? = null
    var lastDeletedWorkoutId: String? = null
    
    override suspend fun addWorkout(
        date: Date,
        distance: Double,
        shoeId: String?
    ): Result<HealthService.WorkoutResult> {
        addWorkoutCallCount++
        lastAddedWorkout = Triple(date, distance, shoeId)
        
        return if (shouldSucceed) {
            Result.success(
                HealthService.WorkoutResult(
                    workoutId = "test_${System.currentTimeMillis()}",
                    syncedAt = Date(),
                    distance = distance,
                    date = date
                )
            )
        } else {
            Result.failure(
                HealthService.HealthServiceError.UnknownError("Test error")
            )
        }
    }
    
    override suspend fun isAuthorized(): Boolean = authorizationStatus
    
    override suspend fun requestAuthorization(): Result<Boolean> {
        return if (shouldSucceed) {
            Result.success(authorizationStatus)
        } else {
            Result.failure(
                HealthService.HealthServiceError.AuthorizationDenied("Test denied")
            )
        }
    }
    
    override suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        deleteWorkoutCallCount++
        lastDeletedWorkoutId = workoutId
        
        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(
                HealthService.HealthServiceError.UnknownError("Test error")
            )
        }
    }
    
    fun reset() {
        shouldSucceed = true
        authorizationStatus = true
        addWorkoutCallCount = 0
        deleteWorkoutCallCount = 0
        lastAddedWorkout = null
        lastDeletedWorkoutId = null
    }
}