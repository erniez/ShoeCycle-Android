package com.shoecycle.data.strava.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StravaActivity(
    val name: String,
    @EncodeDefault  // Force serialization even with default value
    val type: String = "run",  // Activity type in lowercase
    val distance: String,  // Distance value in meters as string
    @EncodeDefault  // Force serialization even with default value
    @SerialName("elapsed_time")
    val elapsedTime: String = "0.0",  // Elapsed time as string
    @SerialName("start_date_local")
    val startDateLocal: String
) {
    companion object {
        // Date format for local time with 'Z' suffix - no timezone conversion
        private val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        // Uses local time with 'Z' suffix as expected by Strava API
        
        fun create(
            name: String,
            distanceInMeters: Double,
            startDate: Date
        ): StravaActivity {
            return StravaActivity(
                name = name,
                distance = distanceInMeters.toString(),  // Convert to string like iOS
                startDateLocal = UTC_DATE_FORMAT.format(startDate),
                elapsedTime = "0.0"  // Match iOS default
            )
        }
    }
}