package com.shoecycle.ui.screens.add_distance.utils

import com.shoecycle.data.DistanceUnit
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * Enhanced distance utility for Add Distance screen
 * Provides iOS-compatible distance formatting and conversion utilities
 */
object AddDistanceUtility {
    
    private const val MILES_TO_KILOMETERS = 1.609344
    private const val KILOMETERS_TO_MILES = 0.621371
    private const val MILES_TO_METERS = 1609.34
    
    /**
     * Format distance for display with appropriate decimal places
     * Matches iOS distance formatting behavior
     */
    fun formatDistance(
        miles: Double,
        unit: DistanceUnit = DistanceUnit.MILES,
        decimals: Int = 2
    ): String {
        val displayDistance = when (unit) {
            DistanceUnit.KM -> miles * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> miles
        }
        
        return when (decimals) {
            0 -> displayDistance.roundToInt().toString()
            1 -> DecimalFormat("#.#").format(displayDistance)
            2 -> DecimalFormat("#.##").format(displayDistance)
            else -> DecimalFormat("#.##").format(displayDistance)
        }
    }
    
    /**
     * Format distance with unit label
     */
    fun formatDistanceWithUnit(
        miles: Double,
        unit: DistanceUnit = DistanceUnit.MILES
    ): String {
        val formatted = formatDistance(miles, unit)
        val unitLabel = getUnitLabel(unit)
        return "$formatted $unitLabel"
    }
    
    /**
     * Convert user input string to miles
     * Handles localized number formats and unit conversion
     */
    fun parseDistanceToMiles(
        input: String,
        fromUnit: DistanceUnit = DistanceUnit.MILES
    ): Double {
        if (input.isBlank()) return 0.0
        
        // Remove any non-numeric characters except decimal point and comma
        val cleanedInput = input.replace(Regex("[^0-9.,]"), "")
            .replace(",", ".") // Handle European decimal format
        
        val parsedValue = cleanedInput.toDoubleOrNull() ?: 0.0
        
        return when (fromUnit) {
            DistanceUnit.KM -> parsedValue * KILOMETERS_TO_MILES
            DistanceUnit.MILES -> parsedValue
        }
    }
    
    /**
     * Convert miles to the specified unit
     */
    fun convertFromMiles(miles: Double, toUnit: DistanceUnit): Double {
        return when (toUnit) {
            DistanceUnit.KM -> miles * MILES_TO_KILOMETERS
            DistanceUnit.MILES -> miles
        }
    }
    
    /**
     * Convert from specified unit to miles
     */
    fun convertToMiles(distance: Double, fromUnit: DistanceUnit): Double {
        return when (fromUnit) {
            DistanceUnit.KM -> distance * KILOMETERS_TO_MILES
            DistanceUnit.MILES -> distance
        }
    }
    
    /**
     * Get the display label for a distance unit
     */
    fun getUnitLabel(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "km"
            DistanceUnit.MILES -> "mi"
        }
    }
    
    /**
     * Get the full name of a distance unit
     */
    fun getUnitName(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.KM -> "Kilometers"
            DistanceUnit.MILES -> "Miles"
        }
    }
    
    /**
     * Convert miles to meters for Strava API
     */
    fun milesToMeters(miles: Double): Double {
        return miles * MILES_TO_METERS
    }
    
    /**
     * Calculate percentage of distance completed
     */
    fun calculateProgress(current: Double, target: Double): Float {
        if (target <= 0) return 0f
        val progress = (current / target).toFloat()
        return progress.coerceIn(0f, 1f)
    }
    
    /**
     * Format progress percentage for display
     */
    fun formatProgressPercentage(current: Double, target: Double): String {
        val percentage = (calculateProgress(current, target) * 100).roundToInt()
        return "$percentage%"
    }
    
    /**
     * Calculate remaining distance to target
     */
    fun calculateRemaining(current: Double, target: Double): Double {
        return (target - current).coerceAtLeast(0.0)
    }
    
    /**
     * Format remaining distance with appropriate messaging
     */
    fun formatRemaining(
        current: Double,
        target: Double,
        unit: DistanceUnit = DistanceUnit.MILES
    ): String {
        val remaining = calculateRemaining(current, target)
        return if (remaining > 0) {
            "${formatDistance(remaining, unit)} remaining"
        } else {
            "Goal reached!"
        }
    }
    
    /**
     * Validate distance input
     */
    fun isValidDistance(input: String): Boolean {
        if (input.isBlank()) return true // Allow empty for clearing
        
        val cleanedInput = input.replace(Regex("[^0-9.,]"), "")
            .replace(",", ".")
        
        val value = cleanedInput.toDoubleOrNull()
        return value != null && value >= 0 && value < 1000 // Reasonable max of 1000 miles/km
    }
    
    /**
     * Generate common distance shortcuts based on unit
     */
    fun getCommonDistances(unit: DistanceUnit): List<Double> {
        return when (unit) {
            DistanceUnit.MILES -> listOf(3.0, 5.0, 6.0, 10.0, 13.1, 26.2)
            DistanceUnit.KM -> listOf(5.0, 10.0, 15.0, 21.1, 42.2)
        }
    }
}