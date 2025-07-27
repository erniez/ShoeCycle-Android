package com.shoecycle.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.FirstDayOfWeek
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.ui.settings.SettingsUnitsState
import com.shoecycle.ui.settings.SettingsUnitsInteractor
import com.shoecycle.ui.settings.SettingsFirstDayState
import com.shoecycle.ui.settings.SettingsFirstDayInteractor
import com.shoecycle.ui.settings.SettingsFavoriteDistancesState
import com.shoecycle.ui.settings.SettingsFavoriteDistancesInteractor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { UserSettingsRepository(context) }
    val userSettings by repository.userSettingsFlow.collectAsState(
        initial = com.shoecycle.data.UserSettingsData()
    )
    val coroutineScope = rememberCoroutineScope()
    
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Distance Units Section
        SettingsUnitsSection(
            repository = repository
        )
        
        // First Day of Week Section
        SettingsFirstDaySection(
            repository = repository
        )
        
        // Favorite Distances Section
        SettingsFavoriteDistancesSection(
            repository = repository
        )
        
        // About Section
        AboutSection()
    }
}

@Composable
fun SettingsUnitsSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsUnitsState()) }
    val interactor = remember { SettingsUnitsInteractor(repository) }
    val coroutineScope = rememberCoroutineScope()
    
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
                            coroutineScope.launch {
                                interactor.handle(state, SettingsUnitsInteractor.Action.UnitChanged(unit))
                            }
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

@Composable
fun SettingsFirstDaySection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsFirstDayState()) }
    val interactor = remember { SettingsFirstDayInteractor(repository) }
    val coroutineScope = rememberCoroutineScope()
    
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
                            coroutineScope.launch {
                                interactor.handle(state, SettingsFirstDayInteractor.Action.DayChanged(day))
                            }
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

@Composable
fun SettingsFavoriteDistancesSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsFavoriteDistancesState()) }
    val interactor = remember { SettingsFavoriteDistancesInteractor(repository) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.ViewAppeared)
    }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Favorite Distances",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteDistanceTextField(
                    value = state.value.favorite1,
                    onValueChange = { distance ->
                        coroutineScope.launch {
                            interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(1, distance))
                        }
                    },
                    label = "Favorite 1",
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite2,
                    onValueChange = { distance ->
                        coroutineScope.launch {
                            interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(2, distance))
                        }
                    },
                    label = "Favorite 2",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteDistanceTextField(
                    value = state.value.favorite3,
                    onValueChange = { distance ->
                        coroutineScope.launch {
                            interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(3, distance))
                        }
                    },
                    label = "Favorite 3",
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite4,
                    onValueChange = { distance ->
                        coroutineScope.launch {
                            interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(4, distance))
                        }
                    },
                    label = "Favorite 4",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AboutSection() {
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("About")
            }
        }
    }
    
    if (showAboutDialog) {
        AboutDialog(
            context = context,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun AboutDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: Exception) {
        null
    }
    
    val versionName = packageInfo?.versionName ?: "Unknown"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About") },
        text = {
            Text(
                text = "ShoeCycle is programmed by Ernie Zappacosta.\n\nCurrent version is $versionName",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun FavoriteDistanceTextField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { 
        mutableStateOf(if (value > 0.0) formatDecimal(value) else "") 
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            // Allow empty string
            if (newText.isEmpty()) {
                textValue = ""
                onValueChange(0.0)
                return@OutlinedTextField
            }
            
            // Validate decimal input
            if (isValidDecimalInput(newText)) {
                textValue = newText
                newText.toDoubleOrNull()?.let { doubleValue ->
                    if (doubleValue >= 0.0) {
                        onValueChange(doubleValue)
                    }
                }
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
            }
        ),
        modifier = modifier,
        singleLine = true
    )
}

private fun formatDecimal(value: Double): String {
    val df = DecimalFormat("#.##")
    return df.format(value)
}

private fun isValidDecimalInput(input: String): Boolean {
    // Allow digits, one decimal point, and reasonable length
    val regex = Regex("^\\d*\\.?\\d*$")
    return input.matches(regex) && input.length <= 10
}