package com.shoecycle.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.shoecycle.ui.theme.ShoeCycleTheme

class StravaAuthActivity : ComponentActivity() {

    companion object {
        const val RESULT_AUTHORIZATION_CODE = "authorization_code"
        const val RESULT_ERROR = "error"
        private const val TAG = "StravaAuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ShoeCycleTheme {
                val state = remember { mutableStateOf(StravaAuthState()) }
                val interactor = remember { StravaAuthInteractor() }
                
                // Handle completion
                LaunchedEffect(state.value.authorizationCode, state.value.error) {
                    state.value.authorizationCode?.let { code ->
                        handleAuthorizationCode(code)
                    }
                    state.value.error?.let { error ->
                        handleError(error)
                    }
                }
                
                StravaAuthScreen(
                    state = state,
                    interactor = interactor
                )
            }
        }
    }

    private fun handleAuthorizationCode(code: String) {
        Log.d(TAG, "Authorization code received")
        val resultIntent = Intent().apply {
            putExtra(RESULT_AUTHORIZATION_CODE, code)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun handleError(error: String) {
        Log.e(TAG, "OAuth error: $error")
        val resultIntent = Intent().apply {
            putExtra(RESULT_ERROR, error)
        }
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaAuthScreen(
    state: MutableState<StravaAuthState>,
    interactor: StravaAuthInteractor
) {
    val activity = LocalContext.current as? Activity
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect to Strava") },
                navigationIcon = {
                    IconButton(onClick = { 
                        interactor.handle(state, StravaAuthInteractor.Action.CloseClicked)
                        activity?.apply {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                StravaWebView(
                    state = state,
                    interactor = interactor
                )
                
                if (state.value.isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StravaWebView(
    state: MutableState<StravaAuthState>,
    interactor: StravaAuthInteractor
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setupWebView()
                webViewClient = StravaWebViewClient(state, interactor)
                loadUrl(interactor.buildOAuthUrl())
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        cacheMode = WebSettings.LOAD_NO_CACHE
    }
    
    // Clear cookies for fresh login
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
}

private class StravaWebViewClient(
    private val state: MutableState<StravaAuthState>,
    private val interactor: StravaAuthInteractor
) : WebViewClient() {
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        Log.d("StravaWebViewClient", "URL loading: $url")
        
        if (interactor.shouldHandleUrl(url)) {
            handleRedirectUrl(url)
            return true
        }
        
        return false
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        interactor.handle(state, StravaAuthInteractor.Action.PageFinishedLoading)
    }
    
    private fun handleRedirectUrl(url: String) {
        // Check for authorization code
        val code = interactor.extractAuthorizationCode(url)
        if (!code.isNullOrEmpty()) {
            interactor.handle(state, StravaAuthInteractor.Action.AuthorizationReceived(code))
            return
        }
        
        // Check for error
        val error = interactor.extractError(url)
        if (!error.isNullOrEmpty()) {
            interactor.handle(state, StravaAuthInteractor.Action.ErrorReceived(error))
            return
        }
        
        // Unknown redirect
        interactor.handle(state, StravaAuthInteractor.Action.ErrorReceived("Unknown redirect format"))
    }
}