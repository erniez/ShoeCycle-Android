package com.shoecycle.ui.screens.add_distance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.domain.models.Shoe
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDistanceEntryView(
    shoe: Shoe?,
    currentDate: Date,
    currentDistance: String,
    isAddingRun: Boolean,
    healthConnectEnabled: Boolean,
    stravaEnabled: Boolean,
    onDateChanged: (Date) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onDistanceAdded: () -> Unit,
    onBounceRequested: () -> Unit,
    onShowFavorites: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    // Local state for the child component
    val state = remember { mutableStateOf(DateDistanceEntryState()) }
    val interactor = remember { DateDistanceEntryInteractor() }
    
    // Sync parent state with local state
    LaunchedEffect(currentDate, currentDistance) {
        state.value = state.value.copy(
            runDate = currentDate,
            runDistance = currentDistance
        )
    }
    
    // Update service status
    LaunchedEffect(healthConnectEnabled, stravaEnabled) {
        state.value = state.value.copy(
            healthConnectEnabled = healthConnectEnabled,
            stravaEnabled = stravaEnabled
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date and Distance inputs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date selector
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            interactor.handle(
                                state,
                                DateDistanceEntryInteractor.Action.ShowDatePicker
                            )
                        },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormatter.format(state.value.runDate),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select date",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Distance input
                OutlinedTextField(
                    value = state.value.runDistance,
                    onValueChange = { 
                        interactor.handle(
                            state,
                            DateDistanceEntryInteractor.Action.DistanceChanged(it),
                            onDistanceChanged = onDistanceChanged
                        )
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Distance") },
                    placeholder = { Text("0.0") },
                    suffix = { Text("mi") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // History and Favorites buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // History button
                OutlinedButton(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "History",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History")
                }
                
                // Favorites button
                OutlinedButton(
                    onClick = onShowFavorites,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorites",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Favorites")
                }
            }
            
            // Add Run button with service indicators
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        interactor.handle(
                            state,
                            DateDistanceEntryInteractor.Action.AddRunClicked,
                            onAddRun = onDistanceAdded,
                            onBounceRequested = onBounceRequested
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.value.runDistance.isNotEmpty() && 
                             !isAddingRun &&
                             shoe != null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isAddingRun) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Add Run")
                    }
                }
                
                // Service indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Health Connect indicator
                    if (state.value.healthConnectEnabled) {
                        ServiceIndicator(
                            isActive = true,
                            syncStatus = state.value.healthConnectSyncStatus,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Health Connect",
                                    modifier = Modifier.size(16.dp),
                                    tint = when (state.value.healthConnectSyncStatus) {
                                        DateDistanceEntryState.SyncStatus.Success -> Color.Green
                                        DateDistanceEntryState.SyncStatus.Failed -> Color.Red
                                        DateDistanceEntryState.SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onPrimary
                                    }
                                )
                            }
                        )
                    }
                    
                    // Strava indicator
                    if (state.value.stravaEnabled) {
                        ServiceIndicator(
                            isActive = true,
                            syncStatus = state.value.stravaSyncStatus,
                            icon = {
                                // Using a simple icon as placeholder for Strava logo
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Strava",
                                    modifier = Modifier.size(16.dp),
                                    tint = when (state.value.stravaSyncStatus) {
                                        DateDistanceEntryState.SyncStatus.Success -> Color.Green
                                        DateDistanceEntryState.SyncStatus.Failed -> Color.Red
                                        DateDistanceEntryState.SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onPrimary
                                    }
                                )
                            }
                        )
                    }
                }
            }
            
            // Show selected shoe info if available
            if (shoe == null) {
                Text(
                    text = "No shoe selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Date picker dialog
    if (state.value.showDatePicker) {
        DatePickerDialog(
            currentDate = state.value.runDate,
            onDateSelected = { date ->
                interactor.handle(
                    state,
                    DateDistanceEntryInteractor.Action.DateChanged(date),
                    onDateChanged = onDateChanged
                )
                interactor.handle(
                    state,
                    DateDistanceEntryInteractor.Action.HideDatePicker
                )
            },
            onDismiss = {
                interactor.handle(
                    state,
                    DateDistanceEntryInteractor.Action.HideDatePicker
                )
            }
        )
    }
}

@Composable
private fun ServiceIndicator(
    isActive: Boolean,
    syncStatus: DateDistanceEntryState.SyncStatus,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                color = when (syncStatus) {
                    DateDistanceEntryState.SyncStatus.Syncing -> 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    DateDistanceEntryState.SyncStatus.Success -> 
                        Color.Green.copy(alpha = 0.2f)
                    DateDistanceEntryState.SyncStatus.Failed -> 
                        Color.Red.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (syncStatus == DateDistanceEntryState.SyncStatus.Syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 1.dp
            )
        } else {
            icon()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    currentDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDate.time
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Date(it))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}