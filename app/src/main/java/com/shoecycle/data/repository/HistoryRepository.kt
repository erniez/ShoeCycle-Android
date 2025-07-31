package com.shoecycle.data.repository

import android.content.Context
import android.util.Log
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.data.database.dao.HistoryDao
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.domain.models.History
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class HistoryRepository(
    private val historyDao: HistoryDao,
    private val shoeRepository: ShoeRepository
) : IHistoryRepository {

    companion object {
        private const val TAG = "HistoryRepository"
        
        fun create(context: Context, shoeRepository: ShoeRepository): HistoryRepository {
            val database = ShoeCycleDatabase.getDatabase(context)
            return HistoryRepository(
                historyDao = database.historyDao(),
                shoeRepository = shoeRepository
            )
        }
    }

    override fun getAllHistory(): Flow<List<History>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { History.fromEntity(it) }
        }
    }

    override fun getHistoryForShoe(shoeId: Long): Flow<List<History>> {
        return historyDao.getHistoryForShoe(shoeId).map { entities ->
            entities.map { History.fromEntity(it) }
        }
    }

    override fun getHistoryInDateRange(startDate: Date, endDate: Date): Flow<List<History>> {
        return historyDao.getHistoryInDateRange(startDate.time, endDate.time).map { entities ->
            entities.map { History.fromEntity(it) }
        }
    }

    override fun getHistoryForShoeInDateRange(
        shoeId: Long,
        startDate: Date,
        endDate: Date
    ): Flow<List<History>> {
        return historyDao.getHistoryForShoeInDateRange(
            shoeId,
            startDate.time,
            endDate.time
        ).map { entities ->
            entities.map { History.fromEntity(it) }
        }
    }

    override suspend fun insertHistory(history: History): Long {
        return try {
            val entity = history.toEntity()
            val insertedId = historyDao.insertHistory(entity)
            
            // Auto-update shoe total distance
            shoeRepository.recalculateShoeTotal(history.shoeId)
            
            Log.d(TAG, "Inserted history with ID: $insertedId for shoe: ${history.shoeId}")
            insertedId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting history for shoe: ${history.shoeId}", e)
            throw e
        }
    }

    override suspend fun updateHistory(history: History) {
        try {
            val entity = history.toEntity()
            historyDao.updateHistory(entity)
            
            // Auto-update shoe total distance
            shoeRepository.recalculateShoeTotal(history.shoeId)
            
            Log.d(TAG, "Updated history: ${history.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating history: ${history.id}", e)
            throw e
        }
    }

    override suspend fun deleteHistory(history: History) {
        try {
            val entity = history.toEntity()
            historyDao.deleteHistory(entity)
            
            // Auto-update shoe total distance
            shoeRepository.recalculateShoeTotal(history.shoeId)
            
            Log.d(TAG, "Deleted history: ${history.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history: ${history.id}", e)
            throw e
        }
    }

    override suspend fun deleteAllHistoryForShoe(shoeId: Long) {
        try {
            historyDao.deleteAllHistoryForShoe(shoeId)
            
            // Auto-update shoe total distance
            shoeRepository.recalculateShoeTotal(shoeId)
            
            Log.d(TAG, "Deleted all history for shoe: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all history for shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun getHistoryById(id: Long): History? {
        return try {
            val entity = historyDao.getHistoryById(id)
            entity?.let { History.fromEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history by ID: $id", e)
            null
        }
    }

    override suspend fun addRun(shoeId: Long, runDate: Date, runDistance: Double): Long {
        return try {
            if (runDistance <= 0) {
                throw IllegalArgumentException("Run distance must be positive")
            }
            
            val history = History.create(
                shoeId = shoeId,
                runDate = runDate,
                runDistance = runDistance
            )
            insertHistory(history)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding run for shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun addRun(shoeId: Long, runDistance: Double): Long {
        return addRun(shoeId, Date(), runDistance)
    }

    override suspend fun getTotalDistanceForShoe(shoeId: Long): Double {
        return try {
            historyDao.getTotalDistanceForShoe(shoeId) ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total distance for shoe: $shoeId", e)
            0.0
        }
    }

    override suspend fun getRunCountForShoe(shoeId: Long): Int {
        return try {
            historyDao.getRunCountForShoe(shoeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting run count for shoe: $shoeId", e)
            0
        }
    }

    override suspend fun getFirstRunForShoe(shoeId: Long): History? {
        return try {
            val entity = historyDao.getFirstRunForShoe(shoeId)
            entity?.let { History.fromEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting first run for shoe: $shoeId", e)
            null
        }
    }

    override suspend fun getLastRunForShoe(shoeId: Long): History? {
        return try {
            val entity = historyDao.getLastRunForShoe(shoeId)
            entity?.let { History.fromEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last run for shoe: $shoeId", e)
            null
        }
    }

    override suspend fun getAverageDistanceForShoe(shoeId: Long): Double {
        return try {
            val totalDistance = getTotalDistanceForShoe(shoeId)
            val runCount = getRunCountForShoe(shoeId)
            if (runCount > 0) totalDistance / runCount else 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating average distance for shoe: $shoeId", e)
            0.0
        }
    }

    override suspend fun getTotalDistanceInDateRange(startDate: Date, endDate: Date): Double {
        return try {
            // This would require a custom query, for now let's implement it by collecting all history
            // In a production app, you'd want a custom DAO query for better performance
            val histories = mutableListOf<History>()
            getHistoryInDateRange(startDate, endDate).collect { historyList ->
                histories.addAll(historyList)
            }
            histories.sumOf { it.runDistance }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total distance in date range", e)
            0.0
        }
    }

    override suspend fun getRunCountInDateRange(startDate: Date, endDate: Date): Int {
        return try {
            // Similar to above, this would benefit from a custom DAO query
            val histories = mutableListOf<History>()
            getHistoryInDateRange(startDate, endDate).collect { historyList ->
                histories.addAll(historyList)
            }
            histories.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting run count in date range", e)
            0
        }
    }
}