package com.shoecycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shoecycle.ui.ShoeCycleApp
import com.shoecycle.ui.theme.ShoeCycleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShoeCycleTheme {
                ShoeCycleApp()
            }
        }
    }
}