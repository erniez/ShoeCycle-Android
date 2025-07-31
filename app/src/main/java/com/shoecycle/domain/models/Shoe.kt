package com.shoecycle.domain.models

import com.shoecycle.data.database.entities.ShoeEntity
import java.util.Date

data class Shoe(
    val id: Long = 0,
    val brand: String,
    val maxDistance: Double,
    val totalDistance: Double,
    val startDistance: Double,
    val startDate: Date,
    val expirationDate: Date,
    val imageKey: String? = null,
    val thumbnailData: ByteArray? = null,
    val orderingValue: Double,
    val hallOfFame: Boolean = false
) {
    val isRetired: Boolean
        get() = hallOfFame

    val isActive: Boolean
        get() = !hallOfFame

    val remainingDistance: Double
        get() = maxOf(0.0, maxDistance - totalDistance)

    val progressPercentage: Double
        get() = if (maxDistance > 0) minOf(100.0, (totalDistance / maxDistance) * 100.0) else 0.0

    val isNearExpiration: Boolean
        get() = remainingDistance <= (maxDistance * 0.1) // Within 10% of max distance

    val displayName: String
        get() = if (brand.isBlank()) "Unnamed Shoe" else brand

    fun toEntity(): ShoeEntity {
        return ShoeEntity(
            id = id,
            brand = brand,
            maxDistance = maxDistance,
            totalDistance = totalDistance,
            startDistance = startDistance,
            startDate = startDate.time,
            expirationDate = expirationDate.time,
            imageKey = imageKey,
            thumbnailData = thumbnailData,
            orderingValue = orderingValue,
            hallOfFame = hallOfFame
        )
    }

    companion object {
        fun fromEntity(entity: ShoeEntity): Shoe {
            return Shoe(
                id = entity.id,
                brand = entity.brand,
                maxDistance = entity.maxDistance,
                totalDistance = entity.totalDistance,
                startDistance = entity.startDistance,
                startDate = Date(entity.startDate),
                expirationDate = Date(entity.expirationDate),
                imageKey = entity.imageKey,
                thumbnailData = entity.thumbnailData,
                orderingValue = entity.orderingValue,
                hallOfFame = entity.hallOfFame
            )
        }

        fun createDefault(brand: String = "", maxDistance: Double = 350.0): Shoe {
            val now = Date()
            val sixMonthsFromNow = Date(now.time + (6L * 30L * 24L * 60L * 60L * 1000L)) // 6 months
            
            return Shoe(
                brand = brand,
                maxDistance = maxDistance,
                totalDistance = 0.0,
                startDistance = 0.0,
                startDate = now,
                expirationDate = sixMonthsFromNow,
                orderingValue = 1.0,
                hallOfFame = false
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shoe

        if (id != other.id) return false
        if (brand != other.brand) return false
        if (maxDistance != other.maxDistance) return false
        if (totalDistance != other.totalDistance) return false
        if (startDistance != other.startDistance) return false
        if (startDate != other.startDate) return false
        if (expirationDate != other.expirationDate) return false
        if (imageKey != other.imageKey) return false
        if (thumbnailData != null) {
            if (other.thumbnailData == null) return false
            if (!thumbnailData.contentEquals(other.thumbnailData)) return false
        } else if (other.thumbnailData != null) return false
        if (orderingValue != other.orderingValue) return false
        if (hallOfFame != other.hallOfFame) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + brand.hashCode()
        result = 31 * result + maxDistance.hashCode()
        result = 31 * result + totalDistance.hashCode()
        result = 31 * result + startDistance.hashCode()
        result = 31 * result + startDate.hashCode()
        result = 31 * result + expirationDate.hashCode()
        result = 31 * result + (imageKey?.hashCode() ?: 0)
        result = 31 * result + (thumbnailData?.contentHashCode() ?: 0)
        result = 31 * result + orderingValue.hashCode()
        result = 31 * result + hallOfFame.hashCode()
        return result
    }
}