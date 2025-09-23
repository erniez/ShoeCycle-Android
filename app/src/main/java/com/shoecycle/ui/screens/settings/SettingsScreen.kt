package com.shoecycle.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.BuildConfig
import com.shoecycle.data.UserSettingsRepository

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { UserSettingsRepository(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Distance Units Section
        SettingsUnitsSection(
            repository = repository
        )
        
        // First Day of Week Section
        SettingsFirstDaySection(
            repository = repository
        )
        
        // Favorite Distances Section
        SettingsFavoriteDistancesSection(
            repository = repository
        )
        
        // Health Connect Section
        SettingsHealthConnectSection(
            repository = repository
        )
        
        // Strava Section
        SettingsStravaSection(
            repository = repository
        )
        
        // About Section
        AboutSection()
    }
}


@Composable
fun AboutSection() {
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Button(
        onClick = { showAboutDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("About")
    }
    
    if (showAboutDialog) {
        AboutDialog(
            context = context,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun AboutDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    
    val versionName = packageInfo?.versionName ?: "Unknown"
    val buildNumber = BuildConfig.VERSION_CODE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About") },
        text = {
            Text(
                text = "ShoeCycle is programmed by Ernie Zappacosta.\n\nVersion: $versionName (Build $buildNumber)",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

