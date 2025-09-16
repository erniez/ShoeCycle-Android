package com.shoecycle.domain.services

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.shoecycle.domain.DistanceUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID

/**
 * Real implementation of HealthService using Android Health Connect.
 * Handles actual integration with Health Connect for workout data synchronization.
 */
class RealHealthConnectService(
    private val context: Context
) : HealthService {
    
    companion object {
        private const val TAG = "RealHealthConnectService"
        private const val SHOE_ID_KEY = "shoe_id"
        private const val SHOE_CYCLE_CLIENT_ID = "com.shoecycle"
    }
    
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }
    
    private val requiredPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class)
    )
    
    /**
     * Adds a workout (running session) to Health Connect.
     * Stores distance in meters (converted from miles) and includes shoe metadata.
     */
    override suspend fun addWorkout(
        date: Date,
        distance: Double,
        shoeId: String?
    ): Result<HealthService.WorkoutResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Adding workout to Health Connect - Date: $date, Distance: $distance mi, ShoeId: $shoeId")
                
                // Check if we have permissions first
                if (!isAuthorized()) {
                    Log.e(TAG, "Not authorized to write to Health Connect")
                    return@withContext Result.failure(
                        HealthService.HealthServiceError.AuthorizationDenied(
                            "Health Connect permissions not granted"
                        )
                    )
                }
                
                // Convert distance from miles to meters using DistanceUtility
                val distanceInMeters = DistanceUtility.milesToMeters(distance)
                
                // Create timestamps
                val startTime = date.toInstant()
                val endTime = startTime.plusSeconds(1) // Minimal duration for a completed workout
                
                // Create metadata with shoe information
                val metadata = Metadata(
                    id = UUID.randomUUID().toString(),
                    clientRecordId = UUID.randomUUID().toString()
                )
                
                // Get the system default zone offset
                val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
                
                // Create exercise session record (running) with distance
                val exerciseSession = ExerciseSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "ShoeCycle Run",
                    notes = shoeId?.let { "Shoe ID: $it\nDistance: $distance miles" },
                    metadata = metadata
                )

                // Create distance record with the converted meters value
                val distanceRecord = DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset,
                    endTime = endTime,
                    endZoneOffset = zoneOffset,
                    distance = androidx.health.connect.client.units.Length.meters(distanceInMeters),
                    metadata = metadata
                )

                // Insert both records into Health Connect
                val insertedRecords = healthConnectClient.insertRecords(
                    listOf(exerciseSession, distanceRecord)
                )
                
                // Get the workout ID from the inserted session record
                val workoutId = insertedRecords.recordIdsList.firstOrNull()
                    ?: UUID.randomUUID().toString()
                
                Log.d(TAG, "Successfully added workout to Health Connect with ID: $workoutId")
                
                Result.success(
                    HealthService.WorkoutResult(
                        workoutId = workoutId,
                        syncedAt = Date(),
                        distance = distance,
                        date = date
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add workout to Health Connect", e)
                Result.failure(
                    when {
                        e.message?.contains("permission", ignoreCase = true) == true ->
                            HealthService.HealthServiceError.AuthorizationDenied(
                                "Health Connect permission denied: ${e.message}"
                            )
                        e.message?.contains("network", ignoreCase = true) == true ->
                            HealthService.HealthServiceError.NetworkError(
                                "Network error: ${e.message}"
                            )
                        else ->
                            HealthService.HealthServiceError.UnknownError(
                                "Failed to add workout: ${e.message}"
                            )
                    }
                )
            }
        }
    }
    
    /**
     * Checks if the app has the required Health Connect permissions.
     */
    override suspend fun isAuthorized(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val granted = healthConnectClient.permissionController
                    .getGrantedPermissions()
                val hasAllPermissions = granted.containsAll(requiredPermissions)
                Log.d(TAG, "Health Connect authorization status: $hasAllPermissions")
                hasAllPermissions
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check Health Connect authorization", e)
                false
            }
        }
    }
    
    /**
     * Requests the required Health Connect permissions from the user.
     */
    override suspend fun requestAuthorization(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Requesting Health Connect permissions")
                
                // Check if Health Connect is available
                val availabilityStatus = HealthConnectClient.getSdkStatus(context)
                if (availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
                    Log.e(TAG, "Health Connect is not available on this device")
                    return@withContext Result.failure(
                        HealthService.HealthServiceError.UnknownError(
                            "Health Connect is not available on this device"
                        )
                    )
                }
                
                // Check current permissions
                val granted = healthConnectClient.permissionController
                    .getGrantedPermissions()
                
                if (granted.containsAll(requiredPermissions)) {
                    Log.d(TAG, "Permissions already granted")
                    return@withContext Result.success(true)
                }
                
                // Create a permission request contract
                // Note: In a real implementation, this would launch an activity
                // For now, we'll return that permissions need to be requested
                Log.d(TAG, "Permissions need to be requested through UI")
                Result.failure(
                    HealthService.HealthServiceError.AuthorizationDenied(
                        "Please grant Health Connect permissions in the app settings"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request Health Connect permissions", e)
                Result.failure(
                    HealthService.HealthServiceError.UnknownError(
                        "Failed to request permissions: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Deletes a workout from Health Connect.
     */
    override suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting workout from Health Connect with ID: $workoutId")
                
                // Check if we have permissions first
                if (!isAuthorized()) {
                    Log.e(TAG, "Not authorized to delete from Health Connect")
                    return@withContext Result.failure(
                        HealthService.HealthServiceError.AuthorizationDenied(
                            "Health Connect permissions not granted"
                        )
                    )
                }
                
                // Delete the record by ID
                // Note: We need both the record type and ID
                healthConnectClient.deleteRecords(
                    ExerciseSessionRecord::class,
                    listOf(workoutId),
                    emptyList()
                )
                
                Log.d(TAG, "Successfully deleted workout from Health Connect")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete workout from Health Connect", e)
                Result.failure(
                    HealthService.HealthServiceError.UnknownError(
                        "Failed to delete workout: ${e.message}"
                    )
                )
            }
        }
    }
}