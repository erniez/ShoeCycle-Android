package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.ui.theme.shoeCycleGreen
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat

@Composable
fun SettingsFavoriteDistancesSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsFavoriteDistancesState()) }
    val interactor = remember { SettingsFavoriteDistancesInteractor(repository) }
    val userSettings by repository.userSettingsFlow.collectAsState(initial = null)
    val currentUnit = userSettings?.distanceUnit ?: DistanceUnit.MILES

    // Initialize state when view appears
    LaunchedEffect(Unit) {
        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.ViewAppeared)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(shoeCycleGreen)
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
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = shoeCycleGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Favorite Distances (${DistanceUtility.getUnitLabel(currentUnit)})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteDistanceTextField(
                    value = state.value.favorite1,
                    onValueChange = { distance ->
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(1, distance))
                    },
                    label = "Favorite 1",
                    unit = currentUnit,
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite2,
                    onValueChange = { distance ->
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(2, distance))
                    },
                    label = "Favorite 2",
                    unit = currentUnit,
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
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(3, distance))
                    },
                    label = "Favorite 3",
                    unit = currentUnit,
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite4,
                    onValueChange = { distance ->
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(4, distance))
                    },
                    label = "Favorite 4",
                    unit = currentUnit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FavoriteDistanceTextField(
    value: Double, // Value in miles from storage
    onValueChange: (Double) -> Unit, // Expects miles to save
    label: String,
    unit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    // Convert miles to display unit
    val displayValue = DistanceUtility.convertFromMiles(value, unit)
    var textValue by remember(displayValue, unit) {
        mutableStateOf(if (displayValue > 0.0) formatDecimal(displayValue) else "")
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
                        // Convert from display unit to miles for storage
                        val milesValue = DistanceUtility.convertToMiles(doubleValue, unit)
                        onValueChange(milesValue)
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