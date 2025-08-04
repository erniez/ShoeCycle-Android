package com.shoecycle.data.repository.interfaces

import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.Flow

interface IShoeRepository {
    
    // Reactive data streams
    fun getAllShoes(): Flow<List<Shoe>>
    fun getActiveShoes(): Flow<List<Shoe>>
    fun getRetiredShoes(): Flow<List<Shoe>>
    fun getShoeById(id: Long): Flow<Shoe?>
    
    // CRUD operations
    suspend fun insertShoe(shoe: Shoe): Long
    suspend fun updateShoe(shoe: Shoe)
    suspend fun deleteShoe(shoe: Shoe)
    suspend fun getShoeByIdOnce(id: Long): Shoe?
    
    // Business logic operations
    suspend fun createShoe(brand: String, maxDistance: Double = 350.0): Long
    suspend fun retireShoe(shoeId: Long)
    suspend fun reactivateShoe(shoeId: Long)
    suspend fun updateTotalDistance(shoeId: Long, totalDistance: Double)
    
    // Statistics and counts
    suspend fun getActiveShoesCount(): Int
    suspend fun getRetiredShoesCount(): Int
    
    // Ordering operations
    suspend fun updateShoeOrdering(shoeId: Long, newOrderingValue: Double)
    suspend fun getNextOrderingValue(): Double
    
    // Total distance recalculation
    suspend fun recalculateShoeTotal(shoeId: Long)
}