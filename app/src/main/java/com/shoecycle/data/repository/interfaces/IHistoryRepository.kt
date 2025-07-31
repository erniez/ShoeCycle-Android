package com.shoecycle.data.repository.interfaces

import com.shoecycle.domain.models.History
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface IHistoryRepository {
    
    // Reactive data streams
    fun getAllHistory(): Flow<List<History>>
    fun getHistoryForShoe(shoeId: Long): Flow<List<History>>
    fun getHistoryInDateRange(startDate: Date, endDate: Date): Flow<List<History>>
    fun getHistoryForShoeInDateRange(
        shoeId: Long,
        startDate: Date,
        endDate: Date
    ): Flow<List<History>>
    
    // CRUD operations
    suspend fun insertHistory(history: History): Long
    suspend fun updateHistory(history: History)
    suspend fun deleteHistory(history: History)
    suspend fun deleteAllHistoryForShoe(shoeId: Long)
    suspend fun getHistoryById(id: Long): History?
    
    // Business logic operations
    suspend fun addRun(shoeId: Long, runDate: Date, runDistance: Double): Long
    suspend fun addRun(shoeId: Long, runDistance: Double): Long // Uses current date
    
    // Statistics and aggregation
    suspend fun getTotalDistanceForShoe(shoeId: Long): Double
    suspend fun getRunCountForShoe(shoeId: Long): Int
    suspend fun getFirstRunForShoe(shoeId: Long): History?
    suspend fun getLastRunForShoe(shoeId: Long): History?
    suspend fun getAverageDistanceForShoe(shoeId: Long): Double
    
    // Date range statistics
    suspend fun getTotalDistanceInDateRange(startDate: Date, endDate: Date): Double
    suspend fun getRunCountInDateRange(startDate: Date, endDate: Date): Int
}