package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository

@Composable
fun SettingsHealthConnectSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsHealthConnectState()) }
    val interactor = remember { SettingsHealthConnectInteractor(repository) }
    
    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsHealthConnectInteractor.Action.ViewAppeared)
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
                text = "Health Connect",
                style = MaterialTheme.typography.titleMedium
            )
            
            Switch(
                checked = state.value.enabled,
                onCheckedChange = { enabled ->
                    interactor.handle(state, SettingsHealthConnectInteractor.Action.ToggleChanged(enabled))
                }
            )
        }
    }
}