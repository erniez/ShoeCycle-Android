package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.strava.StravaTokenKeeper
import com.shoecycle.ui.settings.SettingsStravaView

@Composable
fun SettingsStravaSection(
    repository: UserSettingsRepository
) {
    val context = LocalContext.current
    val tokenKeeper = remember { StravaTokenKeeper(context) }
    
    // Use the new SettingsStravaView which handles OAuth flow
    SettingsStravaView(
        tokenKeeper = tokenKeeper,
        userSettingsRepository = repository
    )
}