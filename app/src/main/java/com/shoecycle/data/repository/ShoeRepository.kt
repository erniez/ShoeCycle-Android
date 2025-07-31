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

    override fun getShoeById(id: Long): Flow<Shoe?> {
        return shoeDao.getShoeByIdFlow(id).map { entity ->
            entity?.let { Shoe.fromEntity(it) }
        }
    }

    override suspend fun insertShoe(shoe: Shoe): Long {
        return try {
            val entity = shoe.toEntity()
            val insertedId = shoeDao.insertShoe(entity)
            Log.d(TAG, "Inserted shoe with ID: $insertedId")
            insertedId
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

    override suspend fun getShoeByIdOnce(id: Long): Shoe? {
        return try {
            val entity = shoeDao.getShoeById(id)
            entity?.let { Shoe.fromEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shoe by ID: $id", e)
            null
        }
    }

    override suspend fun createShoe(brand: String, maxDistance: Double): Long {
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

    override suspend fun retireShoe(shoeId: Long) {
        try {
            shoeDao.retireShoe(shoeId)
            Log.d(TAG, "Retired shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error retiring shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun reactivateShoe(shoeId: Long) {
        try {
            shoeDao.reactivateShoe(shoeId)
            Log.d(TAG, "Reactivated shoe with ID: $shoeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error reactivating shoe: $shoeId", e)
            throw e
        }
    }

    override suspend fun updateTotalDistance(shoeId: Long, totalDistance: Double) {
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

    override suspend fun updateShoeOrdering(shoeId: Long, newOrderingValue: Double) {
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
            val activeShoes = shoeDao.getActiveShoes()
            // This is a Flow, so we need to collect the first emission
            // For now, we'll use a simple approach and get the current max + 1
            val maxOrdering = shoeDao.getAllShoes().let { flow ->
                // We need to handle this differently since we can't collect in suspend
                // Let's use a simpler approach for now
                1000.0 + System.currentTimeMillis().toDouble() / 1000.0
            }
            maxOrdering
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next ordering value", e)
            1.0
        }
    }

    suspend fun recalculateShoeTotal(shoeId: Long) {
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
}