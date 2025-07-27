package com.shoecycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.shoecycle.ui.ShoeCycleApp
import com.shoecycle.ui.theme.ShoeCycleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShoeCycleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShoeCycleApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}