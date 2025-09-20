package com.shoecycle.data.strava.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents athlete information from Strava OAuth response.
 */
@Serializable
data class StravaAthlete(
    val id: Long? = null,
    val username: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val profile: String? = null
)

/**
 * Represents the OAuth response from Strava API.
 */
@Serializable
data class StravaOAuthResponse(
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("access_token") val accessToken: String = "",
    val athlete: StravaAthlete? = null
)

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
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /**
         * Creates a StravaToken from the OAuth response JSON string.
         */
        fun fromJson(jsonString: String): StravaToken {
            val response = json.decodeFromString<StravaOAuthResponse>(jsonString)
            return StravaToken(
                tokenType = response.tokenType,
                expiresAt = response.expiresAt,
                expiresIn = response.expiresIn,
                refreshToken = response.refreshToken,
                accessToken = response.accessToken,
                athleteId = response.athlete?.id,
                athleteUsername = response.athlete?.username,
                athleteFirstName = response.athlete?.firstname,
                athleteLastName = response.athlete?.lastname,
                athleteProfilePicture = response.athlete?.profile
            )
        }
    }
}