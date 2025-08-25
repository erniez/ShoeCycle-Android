package com.shoecycle.domain.models

import com.shoecycle.data.database.entities.HistoryEntity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class History(
    val id: Long = 0,
    val shoeId: String,
    val runDate: Date,
    val runDistance: Double
) {
    val formattedDate: String
        get() = DateFormat.getDateInstance(DateFormat.MEDIUM).format(runDate)

    val formattedDistance: String
        get() = String.format(Locale.getDefault(), "%.2f", runDistance)

    val isValidDistance: Boolean
        get() = runDistance > 0.0

    val isRecentRun: Boolean
        get() {
            val now = Date()
            val sevenDaysAgo = Date(now.time - (7L * 24L * 60L * 60L * 1000L))
            return runDate.after(sevenDaysAgo)
        }

    fun toEntity(): HistoryEntity {
        return HistoryEntity(
            id = id,
            shoeId = shoeId,
            runDate = runDate.time,
            runDistance = runDistance
        )
    }

    companion object {
        fun fromEntity(entity: HistoryEntity): History {
            return History(
                id = entity.id,
                shoeId = entity.shoeId,
                runDate = Date(entity.runDate),
                runDistance = entity.runDistance
            )
        }

        fun create(shoeId: String, runDate: Date = Date(), runDistance: Double): History {
            return History(
                shoeId = shoeId,
                runDate = runDate,
                runDistance = runDistance
            )
        }

        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        fun formatDateForStorage(date: Date): String {
            return dateFormatter.format(date)
        }

        fun parseDateFromStorage(dateString: String): Date? {
            return try {
                dateFormatter.parse(dateString)
            } catch (e: Exception) {
                null
            }
        }
    }
}