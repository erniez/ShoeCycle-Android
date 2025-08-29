package com.shoecycle.data.strava

import android.content.Context
import android.util.Log
import com.shoecycle.data.strava.models.StravaError
import com.shoecycle.data.strava.models.StravaToken
import com.shoecycle.data.strava.storage.SecureTokenStorage
import com.shoecycle.data.strava.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages Strava token storage and refresh operations using Android Keystore encryption.
 * Follows the iOS StravaTokenKeeper pattern for cross-platform consistency.
 */
class StravaTokenKeeper(
    private val storage: TokenStorage
) {
    
    /**
     * Secondary constructor for production use with Context.
     * Creates a SecureTokenStorage instance automatically.
     */
    constructor(context: Context) : this(SecureTokenStorage(context))
    
    companion object {
        private const val TAG = "StravaTokenKeeper"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_ATHLETE_ID = "athlete_id"
        private const val KEY_ATHLETE_USERNAME = "athlete_username"
        private const val KEY_ATHLETE_FIRST_NAME = "athlete_first_name"
        private const val KEY_ATHLETE_LAST_NAME = "athlete_last_name"
        private const val KEY_ATHLETE_PROFILE_PICTURE = "athlete_profile_picture"
        
        private const val STRAVA_TOKEN_URL = "https://www.strava.com/oauth/token"
    }
    
    /**
     * Stores a Strava token securely using Android Keystore encryption.
     */
    fun storeToken(token: StravaToken) {
        try {
            storage.putString(KEY_ACCESS_TOKEN, token.accessToken)
            storage.putString(KEY_REFRESH_TOKEN, token.refreshToken)
            storage.putLong(KEY_EXPIRES_AT, token.expiresAt)
            storage.putLong(KEY_EXPIRES_IN, token.expiresIn)
            storage.putString(KEY_TOKEN_TYPE, token.tokenType)
            token.athleteId?.let { storage.putLong(KEY_ATHLETE_ID, it) }
            token.athleteUsername?.let { storage.putString(KEY_ATHLETE_USERNAME, it) }
            token.athleteFirstName?.let { storage.putString(KEY_ATHLETE_FIRST_NAME, it) }
            token.athleteLastName?.let { storage.putString(KEY_ATHLETE_LAST_NAME, it) }
            token.athleteProfilePicture?.let { storage.putString(KEY_ATHLETE_PROFILE_PICTURE, it) }
            storage.apply()
            
            Log.d(TAG, "Token stored successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store token", e)
            throw StravaError.Unknown("Failed to store token: ${e.message}")
        }
    }
    
    /**
     * Retrieves the stored Strava token if available.
     * @return The stored token or null if no token exists.
     */
    fun getStoredToken(): StravaToken? {
        return try {
            val accessToken = storage.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = storage.getString(KEY_REFRESH_TOKEN, null)
            
            if (accessToken == null || refreshToken == null) {
                Log.d(TAG, "No stored token found")
                return null
            }
            
            StravaToken(
                tokenType = storage.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer",
                expiresAt = storage.getLong(KEY_EXPIRES_AT, 0L),
                expiresIn = storage.getLong(KEY_EXPIRES_IN, 0L),
                refreshToken = refreshToken,
                accessToken = accessToken,
                athleteId = if (storage.contains(KEY_ATHLETE_ID)) {
                    storage.getLong(KEY_ATHLETE_ID, 0L)
                } else null,
                athleteUsername = storage.getString(KEY_ATHLETE_USERNAME, null),
                athleteFirstName = storage.getString(KEY_ATHLETE_FIRST_NAME, null),
                athleteLastName = storage.getString(KEY_ATHLETE_LAST_NAME, null),
                athleteProfilePicture = storage.getString(KEY_ATHLETE_PROFILE_PICTURE, null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve token", e)
            null
        }
    }
    
    /**
     * Gets a valid access token, refreshing if necessary.
     * This method checks token expiration and automatically refreshes if needed.
     * @return A valid access token
     * @throws StravaError if no token exists or refresh fails
     */
    suspend fun getValidToken(): String = withContext(Dispatchers.IO) {
        val token = getStoredToken() ?: throw StravaError.NoToken
        
        if (token.isExpired) {
            Log.d(TAG, "Token expired, refreshing...")
            val refreshedToken = refreshToken(token)
            refreshedToken.accessToken
        } else {
            Log.d(TAG, "Token still valid")
            token.accessToken
        }
    }
    
    /**
     * Refreshes an expired token using the refresh token.
     * @param token The expired token containing the refresh token
     * @return The new refreshed token
     * @throws StravaError if refresh fails
     */
    suspend fun refreshToken(token: StravaToken): StravaToken = withContext(Dispatchers.IO) {
        try {
            val clientId = getStravaClientId()
            val clientSecret = getStravaClientSecret()
            
            val url = URL(STRAVA_TOKEN_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            
            val postData = "client_id=$clientId" +
                    "&client_secret=$clientSecret" +
                    "&grant_type=refresh_token" +
                    "&refresh_token=${token.refreshToken}"
            
            connection.outputStream.use { it.write(postData.toByteArray()) }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                val newToken = StravaToken(
                    tokenType = jsonResponse.optString("token_type", "Bearer"),
                    expiresAt = jsonResponse.optLong("expires_at", 0L),
                    expiresIn = jsonResponse.optLong("expires_in", 0L),
                    refreshToken = jsonResponse.getString("refresh_token"),
                    accessToken = jsonResponse.getString("access_token"),
                    athleteId = token.athleteId,
                    athleteUsername = token.athleteUsername,
                    athleteFirstName = token.athleteFirstName,
                    athleteLastName = token.athleteLastName,
                    athleteProfilePicture = token.athleteProfilePicture
                )
                
                storeToken(newToken)
                Log.d(TAG, "Token refreshed successfully")
                newToken
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "Token refresh failed with code $responseCode: $error")
                throw StravaError.RefreshFailed("Failed to refresh token: $error")
            }
        } catch (e: Exception) {
            when (e) {
                is StravaError -> throw e
                else -> {
                    Log.e(TAG, "Token refresh failed", e)
                    throw StravaError.RefreshFailed("Failed to refresh token: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Clears the stored token (used for logout).
     */
    fun clearToken() {
        try {
            storage.clear()
            storage.apply()
            Log.d(TAG, "Token cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear token", e)
        }
    }
    
    /**
     * Checks if a token is currently stored.
     */
    fun hasStoredToken(): Boolean {
        return storage.contains(KEY_ACCESS_TOKEN) && 
               storage.contains(KEY_REFRESH_TOKEN)
    }
    
    /**
     * Gets the Strava client ID from the obfuscated secrets.
     */
    private fun getStravaClientId(): String {
        // This will be provided by the actual implementation
        // For now, return a placeholder that will be replaced
        return "YOUR_CLIENT_ID"
    }
    
    /**
     * Gets the Strava client secret from the obfuscated secrets.
     */
    private fun getStravaClientSecret(): String {
        // Use the SecretKeyFactory to get the deobfuscated secret
        return SecretKeyFactory.STRAVA.getClearString()
    }
}