package com.shoecycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
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
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import com.shoecycle.ui.components.ShoeImage

data class ShoeDetailState(
    val shoe: Shoe? = null,
    val editedShoe: Shoe? = null,
    val hasUnsavedChanges: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val errorMessage: String? = null,
    val distanceUnit: String = "mi",
    val isCreateMode: Boolean = false,
    val onShoeSaved: (() -> Unit)? = null
)

class ShoeDetailInteractor(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val distanceUtility: DistanceUtility,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class ViewAppeared(val shoeId: Long) : Action()
        object InitializeNewShoe : Action()
        object Refresh : Action()
        data class UpdateShoeName(val name: String) : Action()
        data class UpdateStartDistance(val distance: String) : Action()
        data class UpdateMaxDistance(val distance: String) : Action()
        data class UpdateStartDate(val date: java.util.Date) : Action()
        data class UpdateEndDate(val date: java.util.Date) : Action()
        object SaveChanges : Action()
        object RequestNavigateBack : Action()
        object CancelCreate : Action()
        data class UpdateShoeImage(val imageKey: String, val thumbnailData: ByteArray) : Action() {            
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as UpdateShoeImage
                return imageKey == other.imageKey && thumbnailData.contentEquals(other.thumbnailData)
            }
            
            override fun hashCode(): Int {
                var result = imageKey.hashCode()
                result = 31 * result + thumbnailData.contentHashCode()
                return result
            }
        }
    }
    
    fun handle(state: MutableState<ShoeDetailState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> loadShoe(state, action.shoeId)
            is Action.InitializeNewShoe -> initializeNewShoe(state)
            is Action.Refresh -> {
                state.value.shoe?.let { shoe ->
                    loadShoe(state, shoe.id)
                }
            }
            is Action.UpdateShoeName -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(brand = action.name)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.UpdateStartDistance -> {
                val currentEdited = state.value.editedShoe ?: return
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = currentEdited.copy(startDistance = distance)
                        state.value = state.value.copy(
                            editedShoe = updatedShoe,
                            hasUnsavedChanges = updatedShoe != state.value.shoe
                        )
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            is Action.UpdateMaxDistance -> {
                val currentEdited = state.value.editedShoe ?: return
                scope.launch {
                    try {
                        val distance = distanceUtility.distance(action.distance)
                        val updatedShoe = currentEdited.copy(maxDistance = distance)
                        state.value = state.value.copy(
                            editedShoe = updatedShoe,
                            hasUnsavedChanges = updatedShoe != state.value.shoe
                        )
                    } catch (e: Exception) {
                        // Keep current value if parsing fails
                    }
                }
            }
            is Action.UpdateStartDate -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(startDate = action.date)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.UpdateEndDate -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(expirationDate = action.date)
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
            is Action.SaveChanges -> {
                val editedShoe = state.value.editedShoe ?: return
                
                // Validate shoe name
                if (editedShoe.brand.isBlank()) {
                    state.value = state.value.copy(
                        errorMessage = "Please enter a shoe name"
                    )
                    return
                }
                
                state.value = state.value.copy(isSaving = true, errorMessage = null)
                scope.launch {
                    try {
                        if (state.value.isCreateMode) {
                            // Create new shoe
                            val insertedId = shoeRepository.insertShoe(editedShoe)
                            Log.d("ShoeDetailInteractor", "Successfully created shoe with ID: $insertedId")
                            
                            // Call the saved callback if provided
                            state.value.onShoeSaved?.invoke()
                            
                            state.value = state.value.copy(
                                isSaving = false,
                                shouldNavigateBack = true
                            )
                        } else {
                            // Update existing shoe
                            shoeRepository.updateShoe(editedShoe)
                            
                            // Recalculate total distance in case start distance changed
                            shoeRepository.recalculateShoeTotal(editedShoe.id)
                            
                            // Get the updated shoe with recalculated total
                            val updatedShoe = shoeRepository.getShoeByIdOnce(editedShoe.id)
                            
                            state.value = state.value.copy(
                                shoe = updatedShoe,
                                editedShoe = updatedShoe,
                                hasUnsavedChanges = false,
                                isSaving = false
                            )
                        }
                    } catch (e: Exception) {
                        state.value = state.value.copy(
                            isSaving = false,
                            errorMessage = if (state.value.isCreateMode) {
                                "Error creating shoe: ${e.message}"
                            } else {
                                "Error saving changes: ${e.message}"
                            }
                        )
                    }
                }
            }
            is Action.RequestNavigateBack -> {
                if (state.value.hasUnsavedChanges && !state.value.isSaving) {
                    // Set flag to navigate after save, then save
                    state.value = state.value.copy(shouldNavigateBack = true, isSaving = true)
                    val editedShoe = state.value.editedShoe ?: return
                    scope.launch {
                        try {
                            // Update the shoe details
                            shoeRepository.updateShoe(editedShoe)
                            
                            // Recalculate total distance in case start distance changed
                            shoeRepository.recalculateShoeTotal(editedShoe.id)
                            
                            // Get the updated shoe with recalculated total
                            val updatedShoe = shoeRepository.getShoeByIdOnce(editedShoe.id)
                            
                            state.value = state.value.copy(
                                shoe = updatedShoe,
                                editedShoe = updatedShoe,
                                hasUnsavedChanges = false,
                                isSaving = false
                                // shouldNavigateBack remains true for UI to handle
                            )
                        } catch (e: Exception) {
                            state.value = state.value.copy(
                                isSaving = false,
                                shouldNavigateBack = false,
                                errorMessage = "Error saving changes: ${e.message}"
                            )
                        }
                    }
                } else if (!state.value.isSaving) {
                    // No unsaved changes, can navigate immediately
                    state.value = state.value.copy(shouldNavigateBack = true)
                }
            }
            is Action.CancelCreate -> {
                // In create mode, simply dismiss without saving
                state.value = state.value.copy(shouldNavigateBack = true)
            }
            is Action.UpdateShoeImage -> {
                val currentEdited = state.value.editedShoe ?: return
                val updatedShoe = currentEdited.copy(
                    imageKey = action.imageKey,
                    thumbnailData = action.thumbnailData
                )
                state.value = state.value.copy(
                    editedShoe = updatedShoe,
                    hasUnsavedChanges = updatedShoe != state.value.shoe
                )
            }
        }
    }
    
    private fun loadShoe(state: MutableState<ShoeDetailState>, shoeId: Long) {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        
        scope.launch {
            try {
                shoeRepository.getShoeById(shoeId).collect { shoe ->
                    if (shoe != null) {
                        val unitLabel = distanceUtility.getUnitLabel()
                        
                        state.value = state.value.copy(
                            shoe = shoe,
                            editedShoe = shoe,
                            hasUnsavedChanges = false,
                            isLoading = false,
                            errorMessage = null,
                            distanceUnit = unitLabel
                        )
                    } else {
                        state.value = state.value.copy(
                            isLoading = false,
                            errorMessage = "Shoe not found"
                        )
                    }
                }
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading shoe: ${e.message}"
                )
            }
        }
    }
    
    private fun initializeNewShoe(state: MutableState<ShoeDetailState>) {
        scope.launch {
            try {
                val unitLabel = distanceUtility.getUnitLabel()
                val nextOrderingValue = shoeRepository.getNextOrderingValue()
                val newShoe = Shoe.createDefault().copy(orderingValue = nextOrderingValue)
                
                state.value = state.value.copy(
                    shoe = newShoe,
                    editedShoe = newShoe,
                    hasUnsavedChanges = false,
                    isLoading = false,
                    distanceUnit = unitLabel,
                    isCreateMode = true
                )
            } catch (e: Exception) {
                Log.e("ShoeDetailInteractor", "Error initializing new shoe", e)
                // Fallback to default if there's an error
                val unitLabel = distanceUtility.getUnitLabel()
                val newShoe = Shoe.createDefault()
                
                state.value = state.value.copy(
                    shoe = newShoe,
                    editedShoe = newShoe,
                    hasUnsavedChanges = false,
                    isLoading = false,
                    distanceUnit = unitLabel,
                    isCreateMode = true
                )
            }
        }
    }
}

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
    val interactor = remember { 
        ShoeDetailInteractor(shoeRepository, userSettingsRepository, distanceUtility) 
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
