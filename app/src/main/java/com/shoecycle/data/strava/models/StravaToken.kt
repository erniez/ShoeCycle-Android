package com.shoecycle.data.strava.models

import java.util.Date

/**
 * Represents a Strava authentication token with expiration checking.
 * Matches the iOS StravaToken model for cross-platform consistency.
 */
data class StravaToken(
    val tokenType: String,
    val expiresAt: Long,
    val expiresIn: Long,
    val refreshToken: String,
    val accessToken: String,
    val athleteId: Long? = null,
    val athleteUsername: String? = null,
    val athleteFirstName: String? = null,
    val athleteLastName: String? = null,
    val athleteProfilePicture: String? = null
) {
    /**
     * Checks if the token has expired.
     * Returns true if the token has expired or will expire within the next minute.
     */
    val isExpired: Boolean
        get() {
            val currentTime = System.currentTimeMillis() / 1000
            val bufferTime = 60 // 1 minute buffer before actual expiration
            return currentTime >= (expiresAt - bufferTime)
        }
    
    /**
     * Returns the athlete's full name if available.
     */
    val athleteFullName: String?
        get() = when {
            athleteFirstName != null && athleteLastName != null -> "$athleteFirstName $athleteLastName"
            athleteFirstName != null -> athleteFirstName
            athleteLastName != null -> athleteLastName
            else -> null
        }
    
    companion object {
        /**
         * Creates a StravaToken from the OAuth response JSON.
         */
        fun fromJson(json: Map<String, Any>): StravaToken {
            val athlete = json["athlete"] as? Map<String, Any>
            
            return StravaToken(
                tokenType = json["token_type"] as? String ?: "Bearer",
                expiresAt = (json["expires_at"] as? Number)?.toLong() ?: 0L,
                expiresIn = (json["expires_in"] as? Number)?.toLong() ?: 0L,
                refreshToken = json["refresh_token"] as? String ?: "",
                accessToken = json["access_token"] as? String ?: "",
                athleteId = athlete?.get("id") as? Long,
                athleteUsername = athlete?.get("username") as? String,
                athleteFirstName = athlete?.get("firstname") as? String,
                athleteLastName = athlete?.get("lastname") as? String,
                athleteProfilePicture = athlete?.get("profile") as? String
            )
        }
    }
}