package com.shoecycle.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoecycle.data.database.entities.ShoeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoeDao {
    
    @Query("SELECT * FROM shoes ORDER BY orderingValue ASC")
    fun getAllShoes(): Flow<List<ShoeEntity>>
    
    @Query("SELECT * FROM shoes WHERE hallOfFame = 0 ORDER BY orderingValue ASC")
    fun getActiveShoes(): Flow<List<ShoeEntity>>
    
    @Query("SELECT * FROM shoes WHERE hallOfFame = 1 ORDER BY totalDistance DESC")
    fun getRetiredShoes(): Flow<List<ShoeEntity>>
    
    @Query("SELECT * FROM shoes WHERE id = :id")
    suspend fun getShoeById(id: Long): ShoeEntity?
    
    @Query("SELECT * FROM shoes WHERE id = :id")
    fun getShoeByIdFlow(id: Long): Flow<ShoeEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoe(shoe: ShoeEntity): Long
    
    @Update
    suspend fun updateShoe(shoe: ShoeEntity)
    
    @Delete
    suspend fun deleteShoe(shoe: ShoeEntity)
    
    @Query("UPDATE shoes SET totalDistance = :totalDistance WHERE id = :shoeId")
    suspend fun updateTotalDistance(shoeId: Long, totalDistance: Double)
    
    @Query("UPDATE shoes SET hallOfFame = 1 WHERE id = :shoeId")
    suspend fun retireShoe(shoeId: Long)
    
    @Query("UPDATE shoes SET hallOfFame = 0 WHERE id = :shoeId")
    suspend fun reactivateShoe(shoeId: Long)
    
    @Query("SELECT COUNT(*) FROM shoes WHERE hallOfFame = 0")
    suspend fun getActiveShoesCount(): Int
    
    @Query("SELECT COUNT(*) FROM shoes WHERE hallOfFame = 1")
    suspend fun getRetiredShoesCount(): Int
}