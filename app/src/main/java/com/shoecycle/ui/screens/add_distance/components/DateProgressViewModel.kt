package com.shoecycle.ui.screens.add_distance.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.*
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

/**
 * ViewModel for date progress calculations and animations
 */
class DateProgressViewModel(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    // Constants for calculations
    companion object {
        const val DEFAULT_SHOE_LIFESPAN_DAYS = 365
        const val DEFAULT_TARGET_MILES = 500.0
        const val WEEKS_PER_YEAR = 52.0
        const val DAYS_PER_WEEK = 7.0
    }
    
    private var _bounceRequested = mutableStateOf(false)
    val bounceRequested: State<Boolean> = _bounceRequested
    
    /**
     * Calculate progress data for a shoe
     */
    fun calculateProgressData(shoe: Shoe?): ProgressData {
        return if (shoe != null) {
            val daysSincePurchase = calculateDaysSincePurchase(shoe.startDate)
            val ageProgress = (daysSincePurchase.toFloat() / DEFAULT_SHOE_LIFESPAN_DAYS).coerceIn(0f, 1f)
            val distanceProgress = (shoe.totalDistance / DEFAULT_TARGET_MILES).coerceIn(0.0, 1.0).toFloat()
            
            ProgressData(
                shoe = shoe,
                daysSincePurchase = daysSincePurchase,
                ageProgress = ageProgress,
                distanceProgress = distanceProgress,
                ageStatus = calculateAgeStatus(ageProgress),
                distanceStatus = calculateDistanceStatus(distanceProgress),
                remainingDays = (DEFAULT_SHOE_LIFESPAN_DAYS - daysSincePurchase).coerceAtLeast(0),
                remainingMiles = (DEFAULT_TARGET_MILES - shoe.totalDistance).coerceAtLeast(0.0),
                dailyPace = calculateDailyPace(shoe.totalDistance, daysSincePurchase),
                projectedLifespan = calculateProjectedLifespan(shoe.totalDistance, daysSincePurchase)
            )
        } else {
            ProgressData.empty()
        }
    }
    
    /**
     * Trigger bounce animation with haptic feedback
     */
    fun triggerBounce() {
        // Trigger haptic feedback immediately
        triggerHapticFeedback()
        
        scope.launch {
            _bounceRequested.value = true
            delay(300) // Shorter delay for snappier animation
            _bounceRequested.value = false
        }
    }
    
    /**
     * Trigger a brief vibration for haptic feedback
     */
    private fun triggerHapticFeedback() {
        context?.let { ctx ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // API 31+ - Use VibratorManager
                    val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    val vibrator = vibratorManager?.defaultVibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26-30 - Use Vibrator with VibrationEffect
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // API < 26 - Use deprecated vibrate method
                    @Suppress("DEPRECATION")
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            } catch (e: Exception) {
                // Silently handle any vibration errors (device might not support vibration)
            }
        }
    }
    
    private fun calculateDaysSincePurchase(startDate: Date): Int {
        val now = Date()
        val diffInMillis = now.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
    
    private fun calculateAgeStatus(progress: Float): ProgressStatus {
        return when {
            progress >= 1.0f -> ProgressStatus.CRITICAL
            progress >= 0.8f -> ProgressStatus.WARNING
            progress >= 0.5f -> ProgressStatus.MODERATE
            progress >= 0.25f -> ProgressStatus.GOOD
            else -> ProgressStatus.EXCELLENT
        }
    }
    
    private fun calculateDistanceStatus(progress: Float): ProgressStatus {
        return when {
            progress >= 1.0f -> ProgressStatus.CRITICAL
            progress >= 0.8f -> ProgressStatus.WARNING
            progress >= 0.6f -> ProgressStatus.MODERATE
            progress >= 0.3f -> ProgressStatus.GOOD
            else -> ProgressStatus.EXCELLENT
        }
    }
    
    private fun calculateDailyPace(totalDistance: Double, daysSincePurchase: Int): Double {
        return if (daysSincePurchase > 0) {
            totalDistance / daysSincePurchase
        } else {
            0.0
        }
    }
    
    private fun calculateProjectedLifespan(totalDistance: Double, daysSincePurchase: Int): Int {
        val dailyPace = calculateDailyPace(totalDistance, daysSincePurchase)
        return if (dailyPace > 0) {
            ceil(DEFAULT_TARGET_MILES / dailyPace).toInt()
        } else {
            DEFAULT_SHOE_LIFESPAN_DAYS
        }
    }
}

/**
 * Data class representing all progress information for a shoe
 */
data class ProgressData(
    val shoe: Shoe?,
    val daysSincePurchase: Int,
    val ageProgress: Float,
    val distanceProgress: Float,
    val ageStatus: ProgressStatus,
    val distanceStatus: ProgressStatus,
    val remainingDays: Int,
    val remainingMiles: Double,
    val dailyPace: Double,
    val projectedLifespan: Int
) {
    companion object {
        fun empty() = ProgressData(
            shoe = null,
            daysSincePurchase = 0,
            ageProgress = 0f,
            distanceProgress = 0f,
            ageStatus = ProgressStatus.EXCELLENT,
            distanceStatus = ProgressStatus.EXCELLENT,
            remainingDays = DateProgressViewModel.DEFAULT_SHOE_LIFESPAN_DAYS,
            remainingMiles = DateProgressViewModel.DEFAULT_TARGET_MILES,
            dailyPace = 0.0,
            projectedLifespan = DateProgressViewModel.DEFAULT_SHOE_LIFESPAN_DAYS
        )
    }
    
    /**
     * Get status message for age progress
     */
    val ageStatusMessage: String
        get() = when (ageStatus) {
            ProgressStatus.CRITICAL -> "Time for new shoes!"
            ProgressStatus.WARNING -> "Getting worn"
            ProgressStatus.MODERATE -> "Well broken in"
            ProgressStatus.GOOD -> "Still fresh"
            ProgressStatus.EXCELLENT -> "Brand new"
        }
    
    /**
     * Get status message for distance progress
     */
    val distanceStatusMessage: String
        get() = when (distanceStatus) {
            ProgressStatus.CRITICAL -> "Goal reached! 🎉"
            ProgressStatus.WARNING -> "Almost there!"
            ProgressStatus.MODERATE -> "Halfway mark!"
            ProgressStatus.GOOD -> "Making progress"
            ProgressStatus.EXCELLENT -> "Just getting started"
        }
    
    /**
     * Get formatted daily pace string
     */
    val dailyPaceFormatted: String
        get() = String.format("%.1f mi/day", dailyPace)
    
    /**
     * Get estimated weeks remaining based on current pace
     */
    val estimatedWeeksRemaining: Int
        get() = if (dailyPace > 0) {
            ceil(remainingMiles / (dailyPace * DateProgressViewModel.DAYS_PER_WEEK)).toInt()
        } else {
            Int.MAX_VALUE
        }
    
    /**
     * Get efficiency rating (distance covered vs time owned)
     */
    val efficiencyRating: EfficiencyRating
        get() {
            val expectedDistance = daysSincePurchase * (DateProgressViewModel.DEFAULT_TARGET_MILES / DateProgressViewModel.DEFAULT_SHOE_LIFESPAN_DAYS)
            val actualDistance = shoe?.totalDistance ?: 0.0
            
            val ratio = if (expectedDistance > 0) actualDistance / expectedDistance else 0.0
            
            return when {
                ratio >= 1.5 -> EfficiencyRating.VERY_HIGH
                ratio >= 1.2 -> EfficiencyRating.HIGH
                ratio >= 0.8 -> EfficiencyRating.NORMAL
                ratio >= 0.5 -> EfficiencyRating.LOW
                else -> EfficiencyRating.VERY_LOW
            }
        }
}

/**
 * Enum for progress status levels
 */
enum class ProgressStatus {
    EXCELLENT,
    GOOD,
    MODERATE,
    WARNING,
    CRITICAL
}

/**
 * Enum for shoe usage efficiency
 */
enum class EfficiencyRating(val displayName: String, val description: String) {
    VERY_HIGH("Very Active", "You're really putting these shoes to work!"),
    HIGH("Active", "Great usage for these shoes"),
    NORMAL("Normal", "Right on track with usage"),
    LOW("Light Use", "These shoes have more miles to give"),
    VERY_LOW("Minimal Use", "Time to lace up more often!")
}