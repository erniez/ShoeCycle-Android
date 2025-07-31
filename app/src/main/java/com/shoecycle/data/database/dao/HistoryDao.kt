package com.shoecycle.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoecycle.data.database.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    
    @Query("SELECT * FROM history ORDER BY runDate DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE shoeId = :shoeId ORDER BY runDate DESC")
    fun getHistoryForShoe(shoeId: Long): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?
    
    @Query("SELECT * FROM history WHERE runDate BETWEEN :startDate AND :endDate ORDER BY runDate DESC")
    fun getHistoryInDateRange(startDate: Long, endDate: Long): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE shoeId = :shoeId AND runDate BETWEEN :startDate AND :endDate ORDER BY runDate DESC")
    fun getHistoryForShoeInDateRange(shoeId: Long, startDate: Long, endDate: Long): Flow<List<HistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long
    
    @Update
    suspend fun updateHistory(history: HistoryEntity)
    
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)
    
    @Query("DELETE FROM history WHERE shoeId = :shoeId")
    suspend fun deleteAllHistoryForShoe(shoeId: Long)
    
    @Query("SELECT SUM(runDistance) FROM history WHERE shoeId = :shoeId")
    suspend fun getTotalDistanceForShoe(shoeId: Long): Double?
    
    @Query("SELECT COUNT(*) FROM history WHERE shoeId = :shoeId")
    suspend fun getRunCountForShoe(shoeId: Long): Int
    
    @Query("SELECT * FROM history WHERE shoeId = :shoeId ORDER BY runDate ASC LIMIT 1")
    suspend fun getFirstRunForShoe(shoeId: Long): HistoryEntity?
    
    @Query("SELECT * FROM history WHERE shoeId = :shoeId ORDER BY runDate DESC LIMIT 1")
    suspend fun getLastRunForShoe(shoeId: Long): HistoryEntity?
}