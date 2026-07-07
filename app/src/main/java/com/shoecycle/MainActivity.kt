package com.shoecycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shoecycle.ui.ShoeCycleApp
import com.shoecycle.ui.theme.ShoeCycleTheme
import com.shoecycle.domain.ServiceLocator
import com.shoecycle.domain.services.HealthConnectInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize services
        ServiceLocator.initialize(this)

        // Initialize Analytics
        ServiceLocator.provideAnalyticsLogger().initialize()

        // Feature flags are app-level config: load once at launch and refresh in the background
        // for the life of the app, so every gated screen shares one fetch (ShoeCycle-Web-tk6).
        ServiceLocator.provideFeatureFlagsStore().start()
        
        // Initialize Health Connect to register the app
        HealthConnectInitializer.initialize(this)
        
        setContent {
            ShoeCycleTheme {
                ShoeCycleApp()
            }
        }
    }
}


