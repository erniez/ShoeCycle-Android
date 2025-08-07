package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository

@Composable
fun SettingsStravaSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsStravaState()) }
    val interactor = remember { SettingsStravaInteractor(repository) }
    
    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsStravaInteractor.Action.ViewAppeared)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strava",
                style = MaterialTheme.typography.titleMedium
            )
            
            Switch(
                checked = state.value.enabled,
                onCheckedChange = { enabled ->
                    interactor.handle(state, SettingsStravaInteractor.Action.ToggleChanged(enabled))
                }
            )
        }
    }
}