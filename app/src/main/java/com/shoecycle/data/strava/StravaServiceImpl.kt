package com.shoecycle.data.strava

import android.util.Log
import com.shoecycle.data.strava.models.StravaActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class StravaServiceImpl(
    private val tokenKeeper: StravaTokenKeeper,
    private val client: OkHttpClient = createDefaultOkHttpClient()
) : StravaService {
    
    companion object {
        private const val TAG = "StravaServiceImpl"
        private const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"
        
        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        }
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true  // Always include fields with default values
    }
    
    override suspend fun sendActivity(activity: StravaActivity) {
        try {
            val accessToken = tokenKeeper.getValidToken()
            
            val requestBody = json.encodeToString(activity)
                .toRequestBody(MEDIA_TYPE_JSON.toMediaType())
            
            val request = Request.Builder()
                .url(StravaURLs.ACTIVITIES_URL)
                .header("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            when {
                response.isSuccessful -> {
                    Log.d(TAG, "Activity uploaded successfully")
                }
                response.code == 401 -> {
                    Log.e(TAG, "Unauthorized: Token may be invalid")
                    throw StravaService.DomainError.Unauthorized
                }
                response.code in 400..499 -> {
                    Log.e(TAG, "Client error: ${response.code} - ${response.message}")
                    throw StravaService.DomainError.Unknown
                }
                response.code >= 500 -> {
                    Log.e(TAG, "Server error: ${response.code} - ${response.message}")
                    throw StravaService.DomainError.Reachability
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${response.code} - ${response.message}")
                    throw StravaService.DomainError.Unknown
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            throw StravaService.DomainError.Reachability
        } catch (e: StravaService.DomainError) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            throw StravaService.DomainError.Unknown
        }
    }
}