package com.shoecycle.domain

import android.util.Log
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

class ShoeBusinessLogic(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository
) {
    companion object {
        private const val TAG = "ShoeBusinessLogic"
        const val DEFAULT_MAX_DISTANCE = 350.0
        const val EXPIRATION_MONTHS = 6
        const val NEAR_EXPIRATION_THRESHOLD = 0.1 // 10% of max distance
    }

    suspend fun createShoe(
        brand: String,
        maxDistance: Double = DEFAULT_MAX_DISTANCE,
        startDistance: Double = 0.0
    ): String {
        val now = Date()
        val expirationDate = calculateExpirationDate(now, EXPIRATION_MONTHS)
        val orderingValue = shoeRepository.getNextOrderingValue()

        val newShoe = Shoe(
            brand = brand,
            maxDistance = maxDistance,
            totalDistance = startDistance,
            startDistance = startDistance,
            startDate = now,
            expirationDate = expirationDate,
            orderingValue = orderingValue,
            hallOfFame = false
        )

        val shoeId = shoeRepository.insertShoe(newShoe)
        Log.d(TAG, "Created new shoe: $brand with ID: $shoeId")
        return shoeId
    }

    suspend fun retireShoe(shoeId: String) {
        try {
            shoeRepository.retireShoe(shoeId)
            Log.d(TAG, "Retired shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error retiring shoe: $shoeId", e)
            throw e
        }
    }

    suspend fun reactivateShoe(shoeId: String) {
        try {
            shoeRepository.reactivateShoe(shoeId)
            Log.d(TAG, "Reactivated shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error reactivating shoe: $shoeId", e)
            throw e
        }
    }

    suspend fun logDistance(shoeId: String, distance: Double, date: Date = Date(), notes: String? = null) {
        try {
            val historyId = historyRepository.insertHistory(
                History(
                    shoeId = shoeId,
                    runDistance = distance,
                    runDate = date
                )
            )
            
            // Recalculate shoe's total distance
            recalculateShoeTotal(shoeId)
            
            Log.d(TAG, "Logged distance: $distance for shoe: $shoeId, history ID: $historyId")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging distance for shoe: $shoeId", e)
            throw e
        }
    }

    suspend fun recalculateShoeTotal(shoeId: String) {
        try {
            val shoe = shoeRepository.getShoeByIdOnce(shoeId)
            if (shoe != null) {
                val historyTotal = historyRepository.getTotalDistanceForShoe(shoeId)
                val newTotal = shoe.startDistance + historyTotal
                
                shoeRepository.updateTotalDistance(shoeId, newTotal)
                Log.d(TAG, "Recalculated total for shoe $shoeId: $newTotal (start: ${shoe.startDistance}, history: $historyTotal)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating total for shoe: $shoeId", e)
            throw e
        }
    }

    suspend fun updateShoeDistance(shoeId: String, newDistance: Double) {
        try {
            val shoe = shoeRepository.getShoeByIdOnce(shoeId)
            if (shoe != null) {
                val updatedShoe = shoe.copy(totalDistance = newDistance)
                shoeRepository.updateShoe(updatedShoe)
                Log.d(TAG, "Updated shoe distance for $shoeId to $newDistance")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shoe distance: $shoeId", e)
            throw e
        }
    }

    suspend fun isShoeNearExpiration(shoeId: String): Boolean {
        val shoe = shoeRepository.getShoeByIdOnce(shoeId)
        return shoe?.isNearExpiration ?: false
    }

    suspend fun getShoeStatistics(shoeId: String): ShoeStatistics? {
        return try {
            val shoe = shoeRepository.getShoeByIdOnce(shoeId) ?: return null
            val history = historyRepository.getHistoryForShoe(shoeId).first()
            
            ShoeStatistics(
                shoe = shoe,
                totalRuns = history.size,
                averageDistance = if (history.isNotEmpty()) {
                    history.sumOf { it.runDistance } / history.size
                } else 0.0,
                longestRun = history.maxOfOrNull { it.runDistance } ?: 0.0,
                daysActive = calculateDaysActive(shoe.startDate, if (shoe.isRetired) Date() else Date()),
                progressPercentage = shoe.progressPercentage,
                remainingDistance = shoe.remainingDistance
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting statistics for shoe: $shoeId", e)
            null
        }
    }

    private fun calculateExpirationDate(startDate: Date, months: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.MONTH, months)
        return calendar.time
    }

    private fun calculateDaysActive(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    data class ShoeStatistics(
        val shoe: Shoe,
        val totalRuns: Int,
        val averageDistance: Double,
        val longestRun: Double,
        val daysActive: Int,
        val progressPercentage: Double,
        val remainingDistance: Double
    )
}