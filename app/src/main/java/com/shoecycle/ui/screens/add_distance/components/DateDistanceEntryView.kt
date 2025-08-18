package com.shoecycle.ui.screens.add_distance.components

import androidx.compose.foundation.Image
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

// Constants for spacing and sizing
private val LABEL_BOTTOM_SPACING = 4.dp
private val COLUMN_SPACING = 12.dp
private val ADD_BUTTON_SIZE = 56.dp
private val PROGRESS_INDICATOR_SIZE = 24.dp
private val BUTTON_TOP_PADDING = 4.dp
private val ICON_SIZE = 18.dp
private val ICON_SPACING = 4.dp
private val SURFACE_CORNER_RADIUS = 8.dp
private val CONTAINER_CORNER_RADIUS = 12.dp
private val CONTAINER_PADDING = 16.dp
private val SERVICE_INDICATOR_SIZE = 24.dp
private val SERVICE_INDICATOR_ICON_SIZE = 16.dp
private val SERVICE_INDICATOR_SPACING = 12.dp
private val DISTANCE_FIELD_MIN_WIDTH = 100.dp
private val DISTANCE_FIELD_MAX_WIDTH = 110.dp
private val DATE_COLUMN_WIDTH = 120.dp
private val DISTANCE_COLUMN_WIDTH = 130.dp
private val BUTTON_CONTENT_PADDING = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
private val INPUT_FIELD_HEIGHT = 56.dp
private val TEXT_FIELD_CONTENT_PADDING = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

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
            .padding(horizontal = CONTAINER_PADDING),
        shape = RoundedCornerShape(CONTAINER_CORNER_RADIUS),
        color = shoeCycleSecondaryBackground,
        shadowElevation = 0.dp // Flat design
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CONTAINER_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Three-column layout: Date | Distance | Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
                verticalAlignment = Alignment.Top
            ) {
                // Date column
                Column(
                    modifier = Modifier.width(DATE_COLUMN_WIDTH)
                ) {

                    Text(
                        text = "Date:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(LABEL_BOTTOM_SPACING))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(INPUT_FIELD_HEIGHT)
                            .clickable {
                                interactor.handle(
                                    state,
                                    DateDistanceEntryInteractor.Action.ShowDatePicker
                                )
                            },
                        shape = RoundedCornerShape(SURFACE_CORNER_RADIUS),
                        color = shoeCycleBackground
                    ) {
                        Text(
                            text = dateFormatter.format(state.value.runDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(CONTAINER_CORNER_RADIUS)
                        )
                    }
                    
                    // History button in Date column
                    Spacer(modifier = Modifier.height(BUTTON_TOP_PADDING))
                    Button(
                        onClick = onShowHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shoeCycleTertiaryBackground
                        ),
                        shape = RoundedCornerShape(SURFACE_CORNER_RADIUS),
                        contentPadding = BUTTON_CONTENT_PADDING
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "History",
                            modifier = Modifier.size(ICON_SIZE),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(ICON_SPACING))
                        Text("History", color = Color.White)
                    }
                }
                
                // Distance column
                Column(
                    modifier = Modifier.width(DISTANCE_COLUMN_WIDTH)
                ) {

                    Text(
                        text = "Distance:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(LABEL_BOTTOM_SPACING))
                    TextField(
                        value = state.value.runDistance,
                        onValueChange = { 
                            interactor.handle(
                                state,
                                DateDistanceEntryInteractor.Action.DistanceChanged(it),
                                onDistanceChanged = onDistanceChanged
                            )
                        },
                        modifier = Modifier
                            .widthIn(max = DISTANCE_FIELD_MAX_WIDTH)
                            .height(INPUT_FIELD_HEIGHT),
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
                        shape = RoundedCornerShape(SURFACE_CORNER_RADIUS)
                    )
                    
                    // Distances button in Distance column
                    Spacer(modifier = Modifier.height(BUTTON_TOP_PADDING))
                    Button(
                        onClick = onShowFavorites,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shoeCycleTertiaryBackground
                        ),
                        shape = RoundedCornerShape(SURFACE_CORNER_RADIUS),
                        contentPadding = BUTTON_CONTENT_PADDING
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Distances",
                            modifier = Modifier.size(ICON_SIZE),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(ICON_SPACING))
                        Text("Distances", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Add button column - aligned to top with labels
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        val isEnabled = state.value.runDistance.isNotEmpty() && 
                                       !isAddingRun && 
                                       shoe != null
                        
                        Image(
                            painter = painterResource(id = com.shoecycle.R.drawable.btn_add_run),
                            contentDescription = "Add Distance",
                            modifier = Modifier
                                .size(ADD_BUTTON_SIZE)
                                .alpha(if (isEnabled) 1f else 0.5f)
                                .clickable(enabled = isEnabled) {
                                    focusManager.clearFocus()
                                    interactor.handle(
                                        state,
                                        DateDistanceEntryInteractor.Action.AddRunClicked,
                                        onAddRun = onDistanceAdded,
                                        onBounceRequested = onBounceRequested
                                    )
                                }
                        )
                        
                        if (isAddingRun) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(PROGRESS_INDICATOR_SIZE),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    
                    // Service indicators below Add button (when enabled)
                    val hasEnabledServices = state.value.healthConnectEnabled || state.value.stravaEnabled
                    if (hasEnabledServices) {
                        Spacer(modifier = Modifier.height(BUTTON_TOP_PADDING))
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
                                            modifier = Modifier.size(SERVICE_INDICATOR_ICON_SIZE),
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
                                Spacer(modifier = Modifier.width(SERVICE_INDICATOR_SPACING))
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
                                            modifier = Modifier.size(SERVICE_INDICATOR_ICON_SIZE),
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
            .size(SERVICE_INDICATOR_SIZE)
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