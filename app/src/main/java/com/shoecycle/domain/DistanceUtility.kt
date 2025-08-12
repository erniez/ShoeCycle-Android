package com.shoecycle.domain

import android.util.Log
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class DistanceUtility(
    private val userSettingsRepository: UserSettingsRepository
) {
    companion object {
        private const val TAG = "DistanceUtility"
        private const val MILES_TO_KILOMETERS = 1.609344
        private const val MILES_TO_METERS = 1609.34
        private const val KILOMETERS_TO_MILES = 0.621371
        
        fun format(distance: Double): String {
            val formatter = DecimalFormat("#.##").apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }
            return formatter.format(distance)
        }
    }

    private val formatter: NumberFormat = DecimalFormat("#.##").apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    suspend fun displayString(distance: Double): String {
        val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
        val displayDistance = when (userSettings?.distanceUnit) {
            DistanceUnit.KM -> distance * MILES_TO_KILOMETERS
            else -> distance
        }
        return formatter.format(displayDistance)
    }

    suspend fun favoriteDistanceDisplayString(distance: Double): String {
        return if (distance > 0) {
            displayString(distance)
        } else {
            ""
        }
    }

    suspend fun distance(fromString: String): Double {
        if (fromString.isEmpty()) {
            return 0.0
        }

        val numberFormatter = NumberFormat.getInstance(Locale.getDefault())
        val runDistance = try {
            numberFormatter.parse(fromString)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Could not parse number from string: $fromString", e)
            return 0.0
        }

        val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
        return when (userSettings?.distanceUnit) {
            DistanceUnit.KM -> runDistance * KILOMETERS_TO_MILES
            else -> runDistance
        }
    }

    suspend fun distance(fromMiles: Double): Double {
        val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
        return when (userSettings?.distanceUnit) {
            DistanceUnit.KM -> fromMiles * MILES_TO_KILOMETERS
            else -> fromMiles
        }
    }

    fun stravaDistance(miles: Double): Double {
        return miles * MILES_TO_METERS
    }

    suspend fun getUnitLabel(): String {
        val userSettings = userSettingsRepository.userSettingsFlow.firstOrNull()
        return when (userSettings?.distanceUnit) {
            DistanceUnit.KM -> "km"
            else -> "mi"
        }
    }

    suspend fun convertToMiles(distance: Double, fromUnit: DistanceUnit): Double {
        return when (fromUnit) {
            DistanceUnit.KM -> distance * KILOMETERS_TO_MILES
            DistanceUnit.MILES -> distance
        }
    }

    suspend fun convertFromMiles(miles: Double, toUnit: DistanceUnit): Double {
        return when (toUnit) {
            DistanceUnit.KM -> miles * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> miles
        }
    }
}