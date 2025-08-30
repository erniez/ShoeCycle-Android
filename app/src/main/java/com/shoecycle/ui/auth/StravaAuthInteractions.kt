package com.shoecycle.ui.auth

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import com.shoecycle.data.strava.StravaKeys

// State
data class StravaAuthState(
    val isLoading: Boolean = true,
    val authorizationCode: String? = null,
    val error: String? = null
)

// Interactor
class StravaAuthInteractor {
    companion object {
        private const val TAG = "StravaAuthInteractor"
        private const val REDIRECT_URI = "ShoeCycle://shoecycleapp.com/callback/"
        private const val SCOPE = "activity:write,read"
        private const val STATE = "strava_auth"
    }
    
    sealed class Action {
        object CloseClicked : Action()
        object PageFinishedLoading : Action()
        data class AuthorizationReceived(val code: String) : Action()
        data class ErrorReceived(val error: String) : Action()
    }
    
    fun handle(state: MutableState<StravaAuthState>, action: Action) {
        when (action) {
            is Action.CloseClicked -> {
                state.value = state.value.copy(error = "User cancelled")
            }
            is Action.PageFinishedLoading -> {
                state.value = state.value.copy(isLoading = false)
            }
            is Action.AuthorizationReceived -> {
                Log.d(TAG, "Authorization code received")
                state.value = state.value.copy(
                    authorizationCode = action.code,
                    isLoading = false
                )
            }
            is Action.ErrorReceived -> {
                Log.e(TAG, "OAuth error: ${action.error}")
                state.value = state.value.copy(
                    error = action.error,
                    isLoading = false
                )
            }
        }
    }
    
    fun buildOAuthUrl(): String {
        val baseUrl = "https://www.strava.com/oauth/mobile/authorize"
        val params = mapOf(
            "client_id" to StravaKeys.CLIENT_ID_VALUE,
            "redirect_uri" to REDIRECT_URI,
            "response_type" to "code",
            "approval_prompt" to "auto",
            "scope" to SCOPE,
            "state" to STATE
        )
        
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${Uri.encode(value)}"
        }
        
        return "$baseUrl?$queryString"
    }
    
    fun shouldHandleUrl(url: String): Boolean {
        return url.startsWith(REDIRECT_URI, ignoreCase = true)
    }
    
    fun extractAuthorizationCode(url: String): String? {
        val uri = Uri.parse(url)
        return uri.getQueryParameter("code")
    }
    
    fun extractError(url: String): String? {
        val uri = Uri.parse(url)
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        return errorDescription ?: error
    }
}