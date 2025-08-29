package com.shoecycle.data.strava.models

/**
 * Represents different types of Strava API errors.
 * Matches the iOS error handling pattern for consistency.
 */
sealed class StravaError : Exception() {
    object NoToken : StravaError() {
        override val message = "No Strava token found. Please authenticate first."
    }
    
    object TokenExpired : StravaError() {
        override val message = "Strava token has expired."
    }
    
    data class RefreshFailed(override val message: String) : StravaError()
    
    data class NetworkError(override val message: String) : StravaError()
    
    data class ApiError(
        val statusCode: Int,
        override val message: String
    ) : StravaError()
    
    data class ParseError(override val message: String) : StravaError()
    
    object UnauthorizedAccess : StravaError() {
        override val message = "Unauthorized access to Strava API. Please re-authenticate."
    }
    
    object RateLimitExceeded : StravaError() {
        override val message = "Strava API rate limit exceeded. Please try again later."
    }
    
    data class Unknown(override val message: String = "An unknown error occurred") : StravaError()
}