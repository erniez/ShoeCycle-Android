package com.shoecycle.domain

import android.util.Log
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.flow.first
import java.util.Date
import kotlin.random.Random

class MockShoeGenerator(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val totalWeeks: Int = 16
) {
    companion object {
        private const val TAG = "MockShoeGenerator"
    }

    suspend fun generateNewShoeWithData(): Shoe {
        Log.d(TAG, "Generating new shoe with test data")
        
        // Get current shoe count for naming
        val activeShoes = shoeRepository.getActiveShoes().first()
        val shoeCount = activeShoes.size
        
        // Create new shoe with test data
        val shoeId = shoeRepository.createShoe(
            brand = "Test Shoe ${shoeCount + 1}",
            maxDistance = 350.0
        )
        
        // Generate and add random run histories
        addRunHistories(shoeId)
        
        // Return the created shoe
        return shoeRepository.getShoeByIdOnce(shoeId) 
            ?: throw IllegalStateException("Failed to retrieve created shoe")
    }
    
    private suspend fun addRunHistories(shoeId: String) {
        val dates = generateRandomDates(fromPriorWeeks = totalWeeks)
        val runHistories = addRandomDistances(toDates = dates)
        
        var totalDistance = 0.0
        runHistories.forEach { (date, distance) ->
            val history = History.create(
                shoeId = shoeId,
                runDate = date,
                runDistance = distance.toDouble()
            )
            historyRepository.insertHistory(history)
            totalDistance += distance
        }
        
        // Update the shoe's total distance
        shoeRepository.updateTotalDistance(shoeId, totalDistance)
        Log.d(TAG, "Added ${runHistories.size} history entries with total distance: $totalDistance")
    }
    
    private fun generateRandomDates(fromPriorWeeks: Int): List<Date> {
        val dateArray = mutableListOf<Date>()
        val today = Date()
        val startTime = today.time - (TimeConstants.MILLIS_IN_WEEK * fromPriorWeeks)
        var currentTime = startTime
        
        // Always add the start date
        dateArray.add(Date(currentTime))
        
        // Randomly add dates between start and today
        while (currentTime < today.time) {
            currentTime += TimeConstants.MILLIS_IN_DAY
            if (Random.nextBoolean()) { // 50% chance to add this date
                dateArray.add(Date(currentTime))
            }
        }
        
        return dateArray
    }
    
    private fun addRandomDistances(toDates: List<Date>): List<Pair<Date, Float>> {
        return toDates.map { date ->
            val distance = Random.nextInt(1, 10).toFloat() // 1-9 miles
            Pair(date, distance)
        }
    }
}