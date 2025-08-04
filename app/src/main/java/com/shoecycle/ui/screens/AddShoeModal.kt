package com.shoecycle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.DistanceUtility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoeModal(
    isVisible: Boolean,
    shoeRepository: com.shoecycle.data.repository.interfaces.IShoeRepository,
    onDismiss: () -> Unit,
    onShoeSaved: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Full-screen modal content
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AddShoeContent(
                        shoeRepository = shoeRepository,
                        onDismiss = onDismiss,
                        onShoeSaved = onShoeSaved
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoeContent(
    shoeRepository: com.shoecycle.data.repository.interfaces.IShoeRepository,
    onDismiss: () -> Unit,
    onShoeSaved: () -> Unit
) {
    val context = LocalContext.current
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val distanceUtility = remember { DistanceUtility(userSettingsRepository) }
    val interactor = remember { 
        AddShoeInteractor(shoeRepository, distanceUtility) 
    }
    val state = remember { mutableStateOf(AddShoeState()) }
    
    LaunchedEffect(Unit) {
        interactor.handle(state, AddShoeInteractor.Action.InitializeNewShoe)
    }
    
    // Watch for dismiss signal
    LaunchedEffect(state.value.shouldDismiss) {
        if (state.value.shouldDismiss) {
            if (state.value.errorMessage == null) {
                // Only call onShoeSaved if there was no error (i.e., successful save)
                if (!state.value.isSaving) {
                    onShoeSaved()
                }
            }
            onDismiss()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_shoe)) },
                navigationIcon = {
                    TextButton(
                        onClick = { 
                            interactor.handle(state, AddShoeInteractor.Action.Cancel)
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            interactor.handle(state, AddShoeInteractor.Action.SaveShoe)
                        },
                        enabled = !state.value.isSaving
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Add shoe form content
            AddShoeFormContent(
                shoe = state.value.newShoe,
                distanceUnit = state.value.distanceUnit,
                distanceUtility = distanceUtility,
                onShoeNameChange = { name ->
                    interactor.handle(state, AddShoeInteractor.Action.UpdateShoeName(name))
                },
                onStartDistanceChange = { distance ->
                    interactor.handle(state, AddShoeInteractor.Action.UpdateStartDistance(distance))
                },
                onMaxDistanceChange = { distance ->
                    interactor.handle(state, AddShoeInteractor.Action.UpdateMaxDistance(distance))
                },
                onStartDateChange = { date ->
                    interactor.handle(state, AddShoeInteractor.Action.UpdateStartDate(date))
                },
                onEndDateChange = { date ->
                    interactor.handle(state, AddShoeInteractor.Action.UpdateEndDate(date))
                }
            )
            
            // Error message overlay
            state.value.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(
                            onClick = { 
                                state.value = state.value.copy(errorMessage = null)
                            }
                        ) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            
            // Saving overlay
            if (state.value.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) { }, // Intercept clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.creating_shoe))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddShoeFormContent(
    shoe: com.shoecycle.domain.models.Shoe,
    distanceUnit: String,
    distanceUtility: DistanceUtility,
    onShoeNameChange: (String) -> Unit,
    onStartDistanceChange: (String) -> Unit,
    onMaxDistanceChange: (String) -> Unit,
    onStartDateChange: (java.util.Date) -> Unit,
    onEndDateChange: (java.util.Date) -> Unit
) {
    var startDistanceDisplay by remember { mutableStateOf("") }
    var maxDistanceDisplay by remember { mutableStateOf("") }
    
    LaunchedEffect(shoe, distanceUnit) {
        startDistanceDisplay = distanceUtility.displayString(shoe.startDistance)
        maxDistanceDisplay = distanceUtility.displayString(shoe.maxDistance)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Shoe Name Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shoe Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = shoe.brand,
                        onValueChange = onShoeNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter shoe name") },
                        isError = shoe.brand.isBlank()
                    )
                    if (shoe.brand.isBlank()) {
                        Text(
                            text = "Shoe name is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        item {
            // Distance Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Distance Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Start Distance
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Start Distance",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = startDistanceDisplay,
                                onValueChange = { onStartDistanceChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                suffix = { Text(distanceUnit) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                        
                        // Max Distance
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Max Distance",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = maxDistanceDisplay,
                                onValueChange = { onMaxDistanceChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                suffix = { Text(distanceUnit) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }
        }
        
        item {
            // Dates Section
            DateEditSection(
                shoe = shoe,
                onStartDateChange = onStartDateChange,
                onEndDateChange = onEndDateChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateEditSection(
    shoe: com.shoecycle.domain.models.Shoe,
    onStartDateChange: (java.util.Date) -> Unit,
    onEndDateChange: (java.util.Date) -> Unit
) {
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Wear Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Start Date
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Start Date",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = dateFormatter.format(shoe.startDate),
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select date",
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Expiration Date
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Expiration Date",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = dateFormatter.format(shoe.expirationDate),
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true },
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select date",
                            modifier = Modifier.clickable { showEndDatePicker = true }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
    
    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = shoe.startDate.time
        )
        DatePickerDialog(
            onDateSelected = { 
                datePickerState.selectedDateMillis?.let { dateMillis ->
                    onStartDateChange(java.util.Date(dateMillis))
                }
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = shoe.expirationDate.time
        )
        DatePickerDialog(
            onDateSelected = { 
                datePickerState.selectedDateMillis?.let { dateMillis ->
                    onEndDateChange(java.util.Date(dateMillis))
                }
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDateSelected) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column {
                content()
            }
        }
    )
}