package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsRepository

@Composable
fun SettingsFirstDaySection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsFirstDayState()) }
    val interactor = remember { SettingsFirstDayInteractor(repository) }
    
    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsFirstDayInteractor.Action.ViewAppeared)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "First Day of Week",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FirstDayOfWeek.entries.forEach { day ->
                    val isSelected = state.value.selectedDay == day
                    FilterChip(
                        onClick = { 
                            interactor.handle(state, SettingsFirstDayInteractor.Action.DayChanged(day))
                        },
                        label = { Text(day.displayString) },
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