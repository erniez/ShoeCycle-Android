package com.shoecycle.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoecycle.data.database.dao.HistoryDao
import com.shoecycle.data.database.dao.ShoeDao
import com.shoecycle.data.database.entities.HistoryEntity
import com.shoecycle.data.database.entities.ShoeEntity
import java.util.UUID

@Database(
    entities = [ShoeEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ShoeCycleDatabase : RoomDatabase() {
    
    abstract fun shoeDao(): ShoeDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShoeCycleDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary tables with new schema
                database.execSQL("""
                    CREATE TABLE shoes_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        brand TEXT NOT NULL,
                        maxDistance REAL NOT NULL,
                        totalDistance REAL NOT NULL,
                        startDistance REAL NOT NULL,
                        startDate INTEGER NOT NULL,
                        expirationDate INTEGER NOT NULL,
                        imageKey TEXT,
                        thumbnailData BLOB,
                        orderingValue REAL NOT NULL,
                        hallOfFame INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shoeId TEXT NOT NULL,
                        runDate INTEGER NOT NULL,
                        runDistance REAL NOT NULL,
                        FOREIGN KEY(shoeId) REFERENCES shoes_new(id) ON DELETE CASCADE
                    )
                """)
                
                // Create index for history_new
                database.execSQL("CREATE INDEX index_history_new_shoeId ON history_new(shoeId)")
                
                // Migrate shoes data with UUID generation
                val cursor = database.query("SELECT * FROM shoes")
                while (cursor.moveToNext()) {
                    val oldId = cursor.getLong(cursor.getColumnIndex("id"))
                    val newId = UUID.randomUUID().toString()
                    
                    // Store the mapping for history migration
                    database.execSQL("""
                        INSERT INTO shoes_new (id, brand, maxDistance, totalDistance, 
                            startDistance, startDate, expirationDate, imageKey, 
                            thumbnailData, orderingValue, hallOfFame)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, arrayOf(
                        newId,
                        cursor.getString(cursor.getColumnIndex("brand")),
                        cursor.getDouble(cursor.getColumnIndex("maxDistance")),
                        cursor.getDouble(cursor.getColumnIndex("totalDistance")),
                        cursor.getDouble(cursor.getColumnIndex("startDistance")),
                        cursor.getLong(cursor.getColumnIndex("startDate")),
                        cursor.getLong(cursor.getColumnIndex("expirationDate")),
                        cursor.getString(cursor.getColumnIndex("imageKey")),
                        cursor.getBlob(cursor.getColumnIndex("thumbnailData")),
                        cursor.getDouble(cursor.getColumnIndex("orderingValue")),
                        cursor.getInt(cursor.getColumnIndex("hallOfFame"))
                    ))
                    
                    // Migrate history entries for this shoe
                    val historyCursor = database.query(
                        "SELECT * FROM history WHERE shoeId = ?",
                        arrayOf(oldId.toString())
                    )
                    while (historyCursor.moveToNext()) {
                        database.execSQL("""
                            INSERT INTO history_new (id, shoeId, runDate, runDistance)
                            VALUES (?, ?, ?, ?)
                        """, arrayOf(
                            historyCursor.getLong(historyCursor.getColumnIndex("id")),
                            newId,
                            historyCursor.getLong(historyCursor.getColumnIndex("runDate")),
                            historyCursor.getDouble(historyCursor.getColumnIndex("runDistance"))
                        ))
                    }
                    historyCursor.close()
                }
                cursor.close()
                
                // Drop old tables
                database.execSQL("DROP TABLE shoes")
                database.execSQL("DROP TABLE history")
                
                // Rename new tables
                database.execSQL("ALTER TABLE shoes_new RENAME TO shoes")
                database.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }
        
        fun getDatabase(context: Context): ShoeCycleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoeCycleDatabase::class.java,
                    "shoecycle_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}