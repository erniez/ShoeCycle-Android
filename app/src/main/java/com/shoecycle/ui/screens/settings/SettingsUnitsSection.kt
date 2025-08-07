package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository

@Composable
fun SettingsUnitsSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsUnitsState()) }
    val interactor = remember { SettingsUnitsInteractor(repository) }
    
    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsUnitsInteractor.Action.ViewAppeared)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Units",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistanceUnit.entries.forEach { unit ->
                    val isSelected = state.value.selectedUnit == unit
                    FilterChip(
                        onClick = { 
                            interactor.handle(state, SettingsUnitsInteractor.Action.UnitChanged(unit))
                        },
                        label = { Text(unit.displayString.replaceFirstChar { it.uppercase() }) },
                        selected = isSelected,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderWidth = 2.dp,
                            borderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 1.dp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}