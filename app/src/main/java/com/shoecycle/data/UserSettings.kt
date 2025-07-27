package com.shoecycle.data

enum class DistanceUnit(val displayString: String) {
    MILES("miles"),
    KM("km");
    
    companion object {
        fun fromOrdinal(ordinal: Int): DistanceUnit = entries.getOrElse(ordinal) { MILES }
    }
}

enum class FirstDayOfWeek(val displayString: String) {
    SUNDAY("Sunday"),
    MONDAY("Monday");
    
    companion object {
        fun fromOrdinal(ordinal: Int): FirstDayOfWeek = entries.getOrElse(ordinal) { MONDAY }
    }
}

data class UserSettingsData(
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY,
    val favorite1: Double = 0.0,
    val favorite2: Double = 0.0,
    val favorite3: Double = 0.0,
    val favorite4: Double = 0.0
)