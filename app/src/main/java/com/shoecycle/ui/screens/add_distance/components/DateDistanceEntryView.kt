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
import com.shoecycle.ui.theme.*
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
    
    // iOS-style container with gray background
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = shoeCycleSecondaryBackground,
        shadowElevation = 0.dp // Flat design
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Three-column layout: Date | Distance | Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Date column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Date:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                interactor.handle(
                                    state,
                                    DateDistanceEntryInteractor.Action.ShowDatePicker
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = shoeCycleBackground
                    ) {
                        Text(
                            text = dateFormatter.format(state.value.runDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Distance column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Distance:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = state.value.runDistance,
                        onValueChange = { 
                            interactor.handle(
                                state,
                                DateDistanceEntryInteractor.Action.DistanceChanged(it),
                                onDistanceChanged = onDistanceChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "Distance", 
                                color = Color.Gray
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
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
                
                // Add button column
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp)) // Align with inputs
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
                        enabled = state.value.runDistance.isNotEmpty() && 
                                 !isAddingRun &&
                                 shoe != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shoeCycleGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isAddingRun) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Distance",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            // Service indicators centered directly below Add Distance button (when enabled)
            val hasEnabledServices = state.value.healthConnectEnabled || state.value.stravaEnabled
            if (hasEnabledServices) {
                // Create a Row that matches the three-column layout but only shows indicators in the Add button column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Empty space for Date column
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Empty space for Distance column
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Service indicators aligned with Add button column
                    Column(
                        modifier = Modifier.weight(0.7f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
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
                                                DateDistanceEntryState.SyncStatus.Failed -> shoeCycleRed
                                                DateDistanceEntryState.SyncStatus.Syncing -> shoeCycleBlue
                                                else -> Color.White
                                            }
                                        )
                                    }
                                )
                            }
                            
                            if (state.value.healthConnectEnabled && state.value.stravaEnabled) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            
                            // Strava indicator
                            if (state.value.stravaEnabled) {
                                ServiceIndicator(
                                    isActive = true,
                                    syncStatus = state.value.stravaSyncStatus,
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Strava",
                                            modifier = Modifier.size(16.dp),
                                            tint = when (state.value.stravaSyncStatus) {
                                                DateDistanceEntryState.SyncStatus.Success -> Color.Green
                                                DateDistanceEntryState.SyncStatus.Failed -> shoeCycleRed
                                                DateDistanceEntryState.SyncStatus.Syncing -> shoeCycleBlue
                                                else -> Color.White
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // History and Distances buttons row (iOS style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // History button
                Button(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = shoeCycleTertiaryBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "History",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History", color = Color.White)
                }
                
                // Distances button (changed from Favorites to match iOS)
                Button(
                    onClick = onShowFavorites, // Still calls favorites for now
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = shoeCycleTertiaryBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Distances",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Distances", color = Color.White)
                }
            }
            
            // Show selected shoe info if available
            if (shoe == null) {
                Text(
                    text = "No shoe selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
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