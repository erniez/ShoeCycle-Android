package com.shoecycle.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shoes")
data class ShoeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val maxDistance: Double,
    val totalDistance: Double,
    val startDistance: Double,
    val startDate: Long,
    val expirationDate: Long,
    val imageKey: String? = null,
    val thumbnailData: ByteArray? = null,
    val orderingValue: Double,
    val hallOfFame: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShoeEntity

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