package com.shoecycle.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    foreignKeys = [
        ForeignKey(
            entity = ShoeEntity::class,
            parentColumns = ["id"],
            childColumns = ["shoeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shoeId"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shoeId: Long,
    val runDate: Long,
    val runDistance: Double
)