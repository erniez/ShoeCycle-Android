package com.shoecycle.ui.screens.hall_of_fame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.domain.DistanceUtility

@Composable
fun HallOfFameScreen(
    onNavigateToShoeDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val shoeRepository = remember { 
        com.shoecycle.data.repository.ShoeRepository.create(context)
    }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val distanceUtility = remember { DistanceUtility(userSettingsRepository) }
    val interactor = remember { 
        HallOfFameInteractor(shoeRepository, userSettingsRepository, distanceUtility) 
    }
    val state = remember { mutableStateOf(HallOfFameState()) }
    
    LaunchedEffect(Unit) {
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.hall_of_fame),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when {
            state.value.isLoading -> {
                LoadingScreen()
            }
            state.value.errorMessage != null -> {
                ErrorScreen(
                    message = state.value.errorMessage!!,
                    onRetry = { 
                        interactor.handle(state, HallOfFameInteractor.Action.Refresh)
                    }
                )
            }
            state.value.shoes.isEmpty() -> {
                EmptyStateScreen()
            }
            else -> {
                HallOfFameContent(
                    shoes = state.value.shoes,
                    distanceUnit = state.value.distanceUnit,
                    distanceUtility = distanceUtility,
                    onShoeClick = onNavigateToShoeDetail
                )
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
        modifier = Modifier.fillMaxSize(),
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
private fun EmptyStateScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "You have no shoes in the Hall of Fame. Please go to the Active Shoes tab and edit the shoe you want to add.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun HallOfFameContent(
    shoes: List<com.shoecycle.domain.models.Shoe>,
    distanceUnit: String,
    distanceUtility: DistanceUtility,
    onShoeClick: (Long) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shoes) { shoe ->
            HallOfFameRow(
                shoe = shoe,
                distanceUnit = distanceUnit,
                distanceUtility = distanceUtility,
                onClick = { onShoeClick(shoe.id) }
            )
        }
    }
}

@Composable
private fun HallOfFameRow(
    shoe: com.shoecycle.domain.models.Shoe,
    distanceUnit: String,
    distanceUtility: DistanceUtility,
    onClick: () -> Unit
) {
    var distanceDisplay by remember { mutableStateOf("") }
    
    LaunchedEffect(shoe.totalDistance, distanceUnit) {
        distanceDisplay = distanceUtility.displayString(shoe.totalDistance)
    }
    
    HallOfFameRowView(
        shoe = shoe,
        distanceUnit = distanceUnit,
        distanceDisplay = distanceDisplay,
        onClick = onClick
    )
}