package com.shoecycle.ui.screens.shoe_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.ui.components.ShoeImage
import com.shoecycle.domain.models.Shoe
import com.shoecycle.ui.theme.shoeCycleRed
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun ShoeDetailScreen(
    shoeId: Long? = null,
    isCreateMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onShoeSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val shoeRepository = remember { 
        com.shoecycle.data.repository.ShoeRepository.create(context)
    }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val distanceUtility = remember { DistanceUtility(userSettingsRepository) }
    val imageRepository = remember { com.shoecycle.data.repository.ImageRepository(context) }
    val selectedShoeStrategy = remember { 
        SelectedShoeStrategy(shoeRepository, userSettingsRepository) 
    }
    val interactor = remember { 
        ShoeDetailInteractor(shoeRepository, userSettingsRepository, distanceUtility, selectedShoeStrategy) 
    }
    val state = remember { 
        mutableStateOf(ShoeDetailState(
            isCreateMode = isCreateMode,
            onShoeSaved = onShoeSaved
        )) 
    }
    
    LaunchedEffect(shoeId) {
        if (shoeId != null) {
            interactor.handle(state, ShoeDetailInteractor.Action.ViewAppeared(shoeId))
        } else {
            interactor.handle(state, ShoeDetailInteractor.Action.InitializeNewShoe)
        }
    }
    
    // Unified back navigation handler
    fun handleBackNavigation() {
        interactor.handle(state, ShoeDetailInteractor.Action.RequestNavigateBack)
    }
    
    // Watch for navigation request and handle it
    LaunchedEffect(state.value.shouldNavigateBack) {
        if (state.value.shouldNavigateBack) {
            onNavigateBack()
            // Reset the flag (though the screen will unmount anyway)
            state.value = state.value.copy(shouldNavigateBack = false)
        }
    }
    
    // Handle back navigation with save
    androidx.activity.compose.BackHandler(enabled = true) {
        handleBackNavigation()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom header based on mode
        if (isCreateMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { 
                        interactor.handle(state, ShoeDetailInteractor.Action.CancelCreate)
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Text(
                    text = stringResource(R.string.new_shoe),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { 
                        interactor.handle(state, ShoeDetailInteractor.Action.SaveChanges)
                    },
                    enabled = !state.value.isSaving
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { handleBackNavigation() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = stringResource(R.string.shoe_detail),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.value.isLoading -> {
                    LoadingScreen()
                }
                state.value.errorMessage != null -> {
                    ErrorScreen(
                        message = state.value.errorMessage!!,
                        onRetry = { 
                            interactor.handle(state, ShoeDetailInteractor.Action.Refresh)
                        }
                    )
                }
                state.value.shoe != null -> {
                    ShoeDetailContent(
                        state = state.value,
                        distanceUtility = distanceUtility,
                        imageRepository = imageRepository,
                        interactor = interactor,
                        stateRef = state
                    )
                }
                else -> {
                    ErrorScreen(
                        message = "Shoe not found",
                        onRetry = { 
                            interactor.handle(state, ShoeDetailInteractor.Action.Refresh)
                        }
                    )
                }
            }
            
            // Saving overlay
            if (state.value.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                            Text(if (state.value.isCreateMode) stringResource(R.string.creating_shoe) else "Saving changes...")
                        }
                    }
                }
            }
        }
        
        // Delete confirmation dialog
        if (state.value.showDeleteConfirmation) {
            DeleteConfirmationDialog(
                onConfirm = { 
                    interactor.handle(state, ShoeDetailInteractor.Action.ConfirmDelete)
                },
                onDismiss = { 
                    interactor.handle(state, ShoeDetailInteractor.Action.CancelDelete)
                }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun ShoeDetailContent(
    state: ShoeDetailState,
    distanceUtility: DistanceUtility,
    imageRepository: com.shoecycle.data.repository.ImageRepository,
    interactor: ShoeDetailInteractor,
    stateRef: MutableState<ShoeDetailState>
) {
    val shoe = state.editedShoe ?: return
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShoeInformationSection(
                shoe = shoe,
                onShoeNameChange = { name ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateShoeName(name))
                }
            )
        }
        
        item {
            DistanceInformationSection(
                shoe = shoe,
                distanceUtility = distanceUtility,
                distanceUnit = state.distanceUnit,
                onStartDistanceChange = { distance ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateStartDistance(distance))
                },
                onMaxDistanceChange = { distance ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateMaxDistance(distance))
                }
            )
        }
        
        item {
            WearTimeInformationSection(
                shoe = shoe,
                onStartDateChange = { date ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateStartDate(date))
                },
                onEndDateChange = { date ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateEndDate(date))
                }
            )
        }
        
        item {
            ShoeImageSection(
                shoe = shoe,
                imageRepository = imageRepository,
                onImageUpdated = { imageKey, thumbnailData ->
                    interactor.handle(stateRef, ShoeDetailInteractor.Action.UpdateShoeImage(imageKey, thumbnailData))
                }
            )
        }
        
        // Add Hall of Fame selector if not in create mode
        if (!state.isCreateMode) {
            item {
                HallOfFameSelector(
                    isInHallOfFame = shoe.hallOfFame,
                    onToggle = { newValue ->
                        interactor.handle(stateRef, ShoeDetailInteractor.Action.HallOfFameToggled(newValue))
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        
        // Add delete button if not in create mode
        if (!state.isCreateMode) {
            item {
                DeleteButton(
                    onDeleteClick = {
                        interactor.handle(stateRef, ShoeDetailInteractor.Action.DeleteShoe)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShoeInformationSection(
    shoe: Shoe,
    onShoeNameChange: (String) -> Unit
) {
    ShoeCycleSectionCard(
        title = "Shoe",
        color = Color(0xFFFF9800), // Orange color like iOS
        icon = Icons.Default.Build // Placeholder - will use shoe icon later
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Name:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = shoe.brand,
                onValueChange = onShoeNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Enter shoe name") }
            )
        }
    }
}

@Composable
private fun DistanceInformationSection(
    shoe: Shoe,
    distanceUtility: DistanceUtility,
    distanceUnit: String,
    onStartDistanceChange: (String) -> Unit,
    onMaxDistanceChange: (String) -> Unit
) {
    var startDistanceDisplay by remember { mutableStateOf("") }
    var maxDistanceDisplay by remember { mutableStateOf("") }
    
    LaunchedEffect(shoe, distanceUnit) {
        startDistanceDisplay = distanceUtility.displayString(shoe.startDistance)
        maxDistanceDisplay = distanceUtility.displayString(shoe.maxDistance)
    }
    
    ShoeCycleSectionCard(
        title = "Distance",
        color = Color(0xFF4CAF50), // Green color like iOS
        icon = Icons.Default.Build // Placeholder - will use steps icon later
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start Distance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Start:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = startDistanceDisplay,
                    onValueChange = onStartDistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(distanceUnit) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
            
            // Max Distance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Max:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = maxDistanceDisplay,
                    onValueChange = onMaxDistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text(distanceUnit) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WearTimeInformationSection(
    shoe: Shoe,
    onStartDateChange: (java.util.Date) -> Unit,
    onEndDateChange: (java.util.Date) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    ShoeCycleSectionCard(
        title = "Wear Time",
        color = Color(0xFF2196F3), // Blue color like iOS
        icon = Icons.Default.Build // Placeholder - will use clock icon later
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = dateFormatter.format(shoe.startDate),
                    onValueChange = { },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .clickable { showStartDatePicker = true },
                    enabled = false,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            
            // End Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "End:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = dateFormatter.format(shoe.expirationDate),
                    onValueChange = { },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .clickable { showEndDatePicker = true },
                    enabled = false,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
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

@Composable
private fun ShoeImageSection(
    shoe: Shoe,
    imageRepository: com.shoecycle.data.repository.ImageRepository,
    onImageUpdated: (String, ByteArray) -> Unit
) {
    ShoeImage(
        shoe = shoe,
        imageRepository = imageRepository,
        onImageUpdated = onImageUpdated
    )
}

@Composable
private fun ShoeCycleSectionCard(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left side - Icon and title (mimicking iOS design)
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(40.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .padding(vertical = 8.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8f, 8f)
                        )
                    )
                }
            }
            
            // Content area
            Box(
                modifier = Modifier.weight(1f)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DeleteButton(
    onDeleteClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = onDeleteClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = shoeCycleRed,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete"
            )
            Text(
                text = "Delete Shoe",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Shoe")
        },
        text = {
            Text("Are you sure you want to delete this shoe? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = shoeCycleRed
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
