package com.shoecycle.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository
import java.text.DecimalFormat

@Composable
fun SettingsFavoriteDistancesSection(
    repository: UserSettingsRepository
) {
    val state = remember { mutableStateOf(SettingsFavoriteDistancesState()) }
    val interactor = remember { SettingsFavoriteDistancesInteractor(repository) }
    
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
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(1, distance))
                    },
                    label = "Favorite 1",
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite2,
                    onValueChange = { distance ->
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(2, distance))
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
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(3, distance))
                    },
                    label = "Favorite 3",
                    modifier = Modifier.weight(1f)
                )
                
                FavoriteDistanceTextField(
                    value = state.value.favorite4,
                    onValueChange = { distance ->
                        interactor.handle(state, SettingsFavoriteDistancesInteractor.Action.FavoriteChanged(4, distance))
                    },
                    label = "Favorite 4",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FavoriteDistanceTextField(
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