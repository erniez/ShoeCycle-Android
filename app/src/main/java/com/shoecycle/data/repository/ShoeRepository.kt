package com.shoecycle.data.repository

import android.content.Context
import android.util.Log
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.data.database.dao.HistoryDao
import com.shoecycle.data.database.dao.ShoeDao
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShoeRepository(
    private val shoeDao: ShoeDao,
    private val historyDao: HistoryDao
) : IShoeRepository {

    companion object {
        private const val TAG = "ShoeRepository"
        
        fun create(context: Context): ShoeRepository {
            val database = ShoeCycleDatabase.getDatabase(context)
            return ShoeRepository(
                shoeDao = database.shoeDao(),
                historyDao = database.historyDao()
            )
        }
    }

    override fun getAllShoes(): Flow<List<Shoe>> {
        return shoeDao.getAllShoes().map { entities ->
            entities.map { Shoe.fromEntity(it) }
        }
    }

    override fun getActiveShoes(): Flow<List<Shoe>> {
        return shoeDao.getActiveShoes().map { entities ->
            entities.map { Shoe.fromEntity(it) }
        }
    }

    override fun getRetiredShoes(): Flow<List<Shoe>> {
        return shoeDao.getRetiredShoes().map { entities ->
            entities.map { Shoe.fromEntity(it) }
        }
    }

    override fun getShoeById(id: String): Flow<Shoe?> {
        return shoeDao.getShoeByIdFlow(id).map { entity ->
            entity?.let { Shoe.fromEntity(it) }
        }
    }

    override suspend fun insertShoe(shoe: Shoe): String {
        return try {
            val entity = shoe.toEntity()
            shoeDao.insertShoe(entity)
            Log.d(TAG, "Inserted shoe with ID: ${entity.id}")
            entity.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting shoe: ${shoe.brand}", e)
            throw e
        }
    }

    override suspend fun updateShoe(shoe: Shoe) {
        try {
            val entity = shoe.toEntity()
            shoeDao.updateShoe(entity)
            Log.d(TAG, "Updated shoe: ${shoe.brand}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shoe: ${shoe.brand}", e)
            throw e
        }
    }

    override suspend fun deleteShoe(shoe: Shoe) {
        try {
            val entity = shoe.toEntity()
            shoeDao.deleteShoe(entity)
            Log.d(TAG, "Deleted shoe: ${shoe.brand}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting shoe: ${shoe.brand}", e)
            throw e
        }
    }

    override suspend fun getShoeByIdOnce(id: String): Shoe? {
        return try {
            val entity = shoeDao.getShoeById(id)
            entity?.let { Shoe.fromEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shoe by ID: $id", e)
            null
        }
    }

    override suspend fun createShoe(brand: String, maxDistance: Double): String {
        return try {
            val orderingValue = getNextOrderingValue()
            val newShoe = Shoe.createDefault(brand = brand, maxDistance = maxDistance)
                .copy(orderingValue = orderingValue)
            insertShoe(newShoe)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating shoe: $brand", e)
            throw e
        }
    }

    override suspend fun retireShoe(shoeId: String) {
        try {
            shoeDao.retireShoe(shoeId)
            Log.d(TAG, "Retired shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error retiring shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun reactivateShoe(shoeId: String) {
        try {
            shoeDao.reactivateShoe(shoeId)
            Log.d(TAG, "Reactivated shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error reactivating shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun updateTotalDistance(shoeId: String, totalDistance: Double) {
        try {
            shoeDao.updateTotalDistance(shoeId, totalDistance)
            Log.d(TAG, "Updated total distance for shoe $shoeId: $totalDistance")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total distance for shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun getActiveShoesCount(): Int {
        return try {
            shoeDao.getActiveShoesCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active shoes count", e)
            0
        }
    }

    override suspend fun getRetiredShoesCount(): Int {
        return try {
            shoeDao.getRetiredShoesCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting retired shoes count", e)
            0
        }
    }

    override suspend fun updateShoeOrdering(shoeId: String, newOrderingValue: Double) {
        try {
            val shoe = getShoeByIdOnce(shoeId)
            shoe?.let {
                val updatedShoe = it.copy(orderingValue = newOrderingValue)
                updateShoe(updatedShoe)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shoe ordering: $shoeId", e)
            throw e
        }
    }

    override suspend fun getNextOrderingValue(): Double {
        return try {
            val maxOrdering = shoeDao.getMaxOrderingValue() ?: 0.0
            maxOrdering + 1.0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next ordering value", e)
            1.0
        }
    }

    override suspend fun recalculateShoeTotal(shoeId: String) {
        try {
            val totalDistance = historyDao.getTotalDistanceForShoe(shoeId) ?: 0.0
            val shoe = getShoeByIdOnce(shoeId)
            shoe?.let {
                val finalTotal = it.startDistance + totalDistance
                updateTotalDistance(shoeId, finalTotal)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating shoe total for: $shoeId", e)
            throw e
        }
    }

    override fun getActiveShoeIds(): Flow<List<String>> {
        return shoeDao.getActiveShoes().map { entities ->
            entities.map { it.id }
        }
    }
}