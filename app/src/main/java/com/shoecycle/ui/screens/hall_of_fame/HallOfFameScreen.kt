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
import com.shoecycle.data.featureflags.FeatureFlagKeys
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.ServiceLocator
import com.shoecycle.ui.theme.shoeCycleGreen

@Composable
fun HallOfFameScreen(
    onNavigateToShoeDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val shoeRepository = remember {
        com.shoecycle.data.repository.ShoeRepository.create(context)
    }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val interactor = remember {
        HallOfFameInteractor(shoeRepository, userSettingsRepository)
    }
    val state = remember { mutableStateOf(HallOfFameState()) }

    // Feature flags are loaded once at app launch by the shared FeatureFlagsStore; this screen
    // only observes the resolved boolean and never owns a fetch or a repository (Android Rule 4).
    val featureFlags = remember { ServiceLocator.provideFeatureFlagsStore() }
    val featureFlagsState by featureFlags.state.collectAsState()

    LaunchedEffect(Unit) {
        interactor.handle(state, HallOfFameInteractor.Action.ViewAppeared)
    }

    // Demo gate: a trivial, reversible subtitle shown only when the demo flag resolves ON.
    val showNewHallOfFameBadge = featureFlagsState.isEnabled(FeatureFlagKeys.NEW_HALL_OF_FAME)

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
            modifier = Modifier.padding(bottom = if (showNewHallOfFameBadge) 4.dp else 16.dp)
        )
        if (showNewHallOfFameBadge) {
            Text(
                text = stringResource(R.string.hall_of_fame_new_badge),
                style = MaterialTheme.typography.bodyMedium,
                color = shoeCycleGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
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
    distanceUnit: com.shoecycle.data.DistanceUnit,
    onShoeClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shoes) { shoe ->
            HallOfFameRow(
                shoe = shoe,
                distanceUnit = distanceUnit,
                onClick = { onShoeClick(shoe.id) }
            )
        }
    }
}

@Composable
private fun HallOfFameRow(
    shoe: com.shoecycle.domain.models.Shoe,
    distanceUnit: com.shoecycle.data.DistanceUnit,
    onClick: () -> Unit
) {
    val distanceDisplay = DistanceUtility.displayString(shoe.totalDistance, distanceUnit)
    val unitLabel = DistanceUtility.getUnitLabel(distanceUnit)
    
    HallOfFameRowView(
        shoe = shoe,
        distanceUnit = unitLabel,
        distanceDisplay = distanceDisplay,
        onClick = onClick
    )
}