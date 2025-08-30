package com.shoecycle.data.strava.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class StravaActivity(
    val name: String,
    val type: String = "run",
    val distance: String,
    @SerialName("elapsed_time")
    val elapsedTime: String = "0.0",
    @SerialName("start_date_local")
    val startDateLocal: String
) {
    companion object {
        private val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        fun create(
            name: String,
            distanceInMeters: Double,
            startDate: Date
        ): StravaActivity {
            return StravaActivity(
                name = name,
                distance = distanceInMeters.toString(),
                startDateLocal = UTC_DATE_FORMAT.format(startDate)
            )
        }
    }
}