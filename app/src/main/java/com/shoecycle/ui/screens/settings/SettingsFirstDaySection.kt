package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.ui.theme.shoeCycleBlue

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
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(shoeCycleBlue)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = shoeCycleBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "First Day of Week",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
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