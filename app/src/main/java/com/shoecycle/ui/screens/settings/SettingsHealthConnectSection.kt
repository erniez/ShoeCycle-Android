package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.ui.settings.SettingsHealthConnectView

@Composable
fun SettingsHealthConnectSection(
    repository: UserSettingsRepository
) {
    // Use the new SettingsHealthConnectView which handles permissions
    SettingsHealthConnectView(
        repository = repository,
        modifier = Modifier.fillMaxWidth()
    )
}