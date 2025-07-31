package com.shoecycle.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shoecycle.data.database.dao.HistoryDao
import com.shoecycle.data.database.dao.ShoeDao
import com.shoecycle.data.database.entities.HistoryEntity
import com.shoecycle.data.database.entities.ShoeEntity

@Database(
    entities = [ShoeEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ShoeCycleDatabase : RoomDatabase() {
    
    abstract fun shoeDao(): ShoeDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShoeCycleDatabase? = null
        
        fun getDatabase(context: Context): ShoeCycleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoeCycleDatabase::class.java,
                    "shoecycle_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}