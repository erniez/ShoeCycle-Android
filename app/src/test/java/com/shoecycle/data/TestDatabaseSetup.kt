package com.shoecycle.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.data.database.dao.HistoryDao
import com.shoecycle.data.database.dao.ShoeDao
import com.shoecycle.data.repository.HistoryRepository
import com.shoecycle.data.repository.ShoeRepository
import com.shoecycle.domain.models.History
import com.shoecycle.domain.models.Shoe
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

object TestDatabaseSetup {
    
    private val shoeIdCounter = AtomicLong(1)
    private val historyIdCounter = AtomicLong(1)
    
    fun createInMemoryDatabase(): ShoeCycleDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShoeCycleDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
    }
    
    fun createRepositories(database: ShoeCycleDatabase): Pair<ShoeRepository, HistoryRepository> {
        val shoeDao = database.shoeDao()
        val historyDao = database.historyDao()
        val shoeRepository = ShoeRepository(shoeDao, historyDao)
        val historyRepository = HistoryRepository(historyDao, shoeRepository)
        return Pair(shoeRepository, historyRepository)
    }
}

object TestDataFactory {
    
    private val shoeIdCounter = AtomicLong(1)
    private val historyIdCounter = AtomicLong(1)
    
    fun createTestShoe(
        id: Long = 0,
        brand: String = "Test Shoe ${shoeIdCounter.getAndIncrement()}",
        maxDistance: Double = 350.0,
        totalDistance: Double = 0.0,
        startDistance: Double = 0.0,
        hallOfFame: Boolean = false,
        orderingValue: Double = 1.0
    ): Shoe {
        val now = Date()
        val sixMonthsFromNow = Date(now.time + (6L * 30L * 24L * 60L * 60L * 1000L))
        
        return Shoe(
            id = id,
            brand = brand,
            maxDistance = maxDistance,
            totalDistance = totalDistance,
            startDistance = startDistance,
            startDate = now,
            expirationDate = sixMonthsFromNow,
            imageKey = null,
            thumbnailData = null,
            orderingValue = orderingValue,
            hallOfFame = hallOfFame
        )
    }
    
    fun createTestHistory(
        id: Long = 0,
        shoeId: Long,
        runDate: Date = Date(),
        runDistance: Double = 5.0
    ): History {
        return History(
            id = id,
            shoeId = shoeId,
            runDate = runDate,
            runDistance = runDistance
        )
    }
    
    fun createMultipleTestShoes(count: Int, hallOfFame: Boolean = false): List<Shoe> {
        return (1..count).map { index ->
            createTestShoe(
                brand = "Test Shoe $index",
                maxDistance = 300.0 + (index * 50.0),
                hallOfFame = hallOfFame,
                orderingValue = index.toDouble()
            )
        }
    }
    
    fun createMultipleTestHistories(
        shoeId: Long,
        count: Int,
        startDate: Date = Date(),
        distanceRange: Double = 5.0
    ): List<History> {
        val oneDayInMillis = 24L * 60L * 60L * 1000L
        
        return (0 until count).map { index ->
            val runDate = Date(startDate.time - (index * oneDayInMillis))
            val runDistance = distanceRange + (index * 0.5) // Varying distances
            
            createTestHistory(
                shoeId = shoeId,
                runDate = runDate,
                runDistance = runDistance
            )
        }
    }
    
    fun createWeeklyTestHistories(
        shoeId: Long,
        weekCount: Int,
        runsPerWeek: Int = 3
    ): List<History> {
        val histories = mutableListOf<History>()
        val oneDayInMillis = 24L * 60L * 60L * 1000L
        val oneWeekInMillis = 7L * oneDayInMillis
        val baseDate = Date()
        
        for (week in 0 until weekCount) {
            val weekStartDate = Date(baseDate.time - (week * oneWeekInMillis))
            
            for (run in 0 until runsPerWeek) {
                val runDate = Date(weekStartDate.time - (run * 2 * oneDayInMillis)) // Every 2 days
                val runDistance = 3.0 + (run * 0.5) // 3.0, 3.5, 4.0 miles
                
                histories.add(
                    createTestHistory(
                        shoeId = shoeId,
                        runDate = runDate,
                        runDistance = runDistance
                    )
                )
            }
        }
        
        return histories
    }
    
    fun createDateRangeHistories(
        shoeId: Long,
        startDate: Date,
        endDate: Date,
        intervalDays: Int = 2
    ): List<History> {
        val histories = mutableListOf<History>()
        val oneDayInMillis = 24L * 60L * 60L * 1000L
        val intervalMillis = intervalDays * oneDayInMillis
        
        var currentDate = startDate.time
        var runCount = 1
        
        while (currentDate <= endDate.time) {
            histories.add(
                createTestHistory(
                    shoeId = shoeId,
                    runDate = Date(currentDate),
                    runDistance = 3.0 + (runCount % 3) // Varying distances: 3.0, 4.0, 5.0
                )
            )
            
            currentDate += intervalMillis
            runCount++
        }
        
        return histories
    }
    
    fun resetCounters() {
        shoeIdCounter.set(1)
        historyIdCounter.set(1)
    }
}