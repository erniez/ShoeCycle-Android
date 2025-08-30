package com.shoecycle.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import com.shoecycle.data.strava.SecretKeyFactory
import com.shoecycle.data.strava.StravaTokenKeeper
import com.shoecycle.data.strava.models.StravaToken
import com.shoecycle.ui.auth.StravaAuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

// Extension function to convert JSONObject to Map
private fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    keys().forEach { key ->
        val value = get(key)
        map[key] = when (value) {
            is JSONObject -> value.toMap()
            else -> value
        }
    }
    return map
}

// State
data class StravaState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val athleteName: String? = null
)

// Interactor
class StravaInteractor(
    private val tokenKeeper: StravaTokenKeeper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "StravaInteractor"
        private const val TOKEN_EXCHANGE_URL = "https://www.strava.com/oauth/token"
    }
    
    private val httpClient = OkHttpClient()
    private var authLauncher: ActivityResultLauncher<Intent>? = null
    
    sealed class Action {
        object ConnectClicked : Action()
        object DisconnectClicked : Action()
        data class AuthorizationReceived(val code: String) : Action()
        data class AuthorizationFailed(val error: String) : Action()
        object ViewAppeared : Action()
        object DismissError : Action()
    }
    
    fun handle(state: MutableState<StravaState>, action: Action, context: Context? = null) {
        when (action) {
            is Action.ConnectClicked -> {
                handleConnect(state, context)
            }
            is Action.DisconnectClicked -> {
                handleDisconnect(state)
            }
            is Action.AuthorizationReceived -> {
                handleAuthorizationCode(state, action.code)
            }
            is Action.AuthorizationFailed -> {
                state.value = state.value.copy(
                    isLoading = false,
                    error = action.error
                )
            }
            is Action.ViewAppeared -> {
                checkConnectionStatus(state)
            }
            is Action.DismissError -> {
                state.value = state.value.copy(error = null)
            }
        }
    }
    
    fun setAuthLauncher(launcher: ActivityResultLauncher<Intent>) {
        authLauncher = launcher
    }
    
    private fun handleConnect(state: MutableState<StravaState>, context: Context?) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot launch OAuth")
            state.value = state.value.copy(error = "Unable to start authentication")
            return
        }
        
        state.value = state.value.copy(isLoading = true, error = null)
        
        // Launch OAuth activity
        val intent = Intent(context, StravaAuthActivity::class.java)
        authLauncher?.launch(intent) ?: run {
            Log.e(TAG, "Auth launcher not set")
            state.value = state.value.copy(
                isLoading = false,
                error = "Authentication launcher not configured"
            )
        }
    }
    
    private fun handleDisconnect(state: MutableState<StravaState>) {
        state.value = state.value.copy(isLoading = true)
        
        scope.launch {
            try {
                tokenKeeper.clearToken()
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        isConnected = false,
                        isLoading = false,
                        athleteName = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        isLoading = false,
                        error = "Failed to disconnect: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun handleAuthorizationCode(state: MutableState<StravaState>, code: String) {
        state.value = state.value.copy(isLoading = true)
        
        scope.launch {
            try {
                // Exchange authorization code for access token
                val tokenResponse = exchangeCodeForToken(code)
                
                // Parse response and create token
                val jsonResponse = JSONObject(tokenResponse)
                val jsonMap = jsonResponse.toMap()
                val token = StravaToken.fromJson(jsonMap)
                
                // Save token using StravaTokenKeeper
                tokenKeeper.storeToken(token)
                
                val athleteName = token.athleteFullName ?: "Connected Athlete"
                
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        isConnected = true,
                        isLoading = false,
                        athleteName = athleteName,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging code for token", e)
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        isLoading = false,
                        error = "Failed to complete authentication: ${e.message}"
                    )
                }
            }
        }
    }
    
    private suspend fun exchangeCodeForToken(code: String): String {
        return withContext(Dispatchers.IO) {
            val clientSecret = SecretKeyFactory.STRAVA.getClearString()
            
            val formBody = FormBody.Builder()
                .add("client_id", "4002")
                .add("client_secret", clientSecret)
                .add("code", code)
                .add("grant_type", "authorization_code")
                .build()
            
            val request = Request.Builder()
                .url(TOKEN_EXCHANGE_URL)
                .post(formBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Token exchange failed: ${response.code}")
            }
            
            response.body?.string() ?: throw IOException("Empty response body")
        }
    }
    
    private fun checkConnectionStatus(state: MutableState<StravaState>) {
        scope.launch {
            try {
                val token = tokenKeeper.getStoredToken()
                val isConnected = token != null && !token.isExpired
                
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(
                        isConnected = isConnected,
                        athleteName = if (isConnected && token != null) {
                            token.athleteFullName ?: "Connected Athlete"
                        } else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking connection status", e)
                withContext(Dispatchers.Main) {
                    state.value = state.value.copy(isConnected = false)
                }
            }
        }
    }
    
    fun handleActivityResult(
        state: MutableState<StravaState>,
        resultCode: Int,
        data: Intent?
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val code = data?.getStringExtra(StravaAuthActivity.RESULT_AUTHORIZATION_CODE)
                if (code != null) {
                    handle(state, Action.AuthorizationReceived(code))
                } else {
                    handle(state, Action.AuthorizationFailed("No authorization code received"))
                }
            }
            Activity.RESULT_CANCELED -> {
                val error = data?.getStringExtra(StravaAuthActivity.RESULT_ERROR)
                handle(state, Action.AuthorizationFailed(error ?: "Authentication cancelled"))
            }
        }
    }
}