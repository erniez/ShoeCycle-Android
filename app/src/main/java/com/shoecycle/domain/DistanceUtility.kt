package com.shoecycle.domain

import android.util.Log
import com.shoecycle.data.DistanceUnit
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object DistanceUtility {
    private const val TAG = "DistanceUtility"
    private const val MILES_TO_KILOMETERS = 1.609344
    private const val MILES_TO_METERS = 1609.34
    private const val KILOMETERS_TO_MILES = 0.621371
    
    private val formatter: DecimalFormat = DecimalFormat("#.##").apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    
    fun format(distance: Double): String {
        return formatter.format(distance)
    }

    fun displayString(distance: Double, unit: DistanceUnit): String {
        val displayDistance = when (unit) {
            DistanceUnit.KM -> distance * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> distance
        }
        return formatter.format(displayDistance)
    }

    fun favoriteDistanceDisplayString(distance: Double, unit: DistanceUnit): String {
        return if (distance > 0) {
            displayString(distance, unit)
        } else {
            ""
        }
    }

    fun distance(fromString: String, unit: DistanceUnit): Double {
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

        return when (unit) {
            DistanceUnit.KM -> runDistance * KILOMETERS_TO_MILES
            DistanceUnit.MILES -> runDistance
        }
    }

    fun distance(fromMiles: Double, unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KM -> fromMiles * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> fromMiles
        }
    }

    fun milesToMeters(miles: Double): Double {
        return miles * MILES_TO_METERS
    }

    fun getUnitLabel(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
        }
    }

    fun convertToMiles(distance: Double, fromUnit: DistanceUnit): Double {
        return when (fromUnit) {
            DistanceUnit.KM -> distance * KILOMETERS_TO_MILES
            DistanceUnit.MILES -> distance
        }
    }

    fun convertFromMiles(miles: Double, toUnit: DistanceUnit): Double {
        return when (toUnit) {
            DistanceUnit.KM -> miles * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> miles
        }
    }
}