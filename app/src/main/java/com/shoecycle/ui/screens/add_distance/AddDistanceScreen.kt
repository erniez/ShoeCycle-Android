package com.shoecycle.ui.screens.add_distance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.database.ShoeCycleDatabase
import com.shoecycle.data.repository.HistoryRepository
import com.shoecycle.data.repository.ImageRepository
import com.shoecycle.data.repository.ShoeRepository
import com.shoecycle.domain.SelectedShoeStrategy
import com.shoecycle.ui.screens.add_distance.components.ShoeImageView
import com.shoecycle.ui.screens.add_distance.components.DateDistanceEntryView
import com.shoecycle.ui.screens.add_distance.components.ShoeCycleDistanceProgressView
import com.shoecycle.ui.screens.add_distance.components.ShoeCycleDateProgressView
import com.shoecycle.ui.screens.add_distance.components.DateProgressViewModel
import com.shoecycle.ui.screens.add_distance.services.MockHealthService
import com.shoecycle.ui.screens.add_distance.services.MockStravaService
import com.shoecycle.ui.screens.add_distance.components.chart.RunHistoryChartView
import com.shoecycle.ui.screens.add_distance.utils.HistoryCollation
import com.shoecycle.ui.screens.add_distance.modals.HistoryListView
import com.shoecycle.ui.screens.add_distance.modals.FavoriteDistancesView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private val HEADER_IMAGE_SIZE = 120.dp

@Composable
fun AddDistanceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize repositories
    val database = remember { ShoeCycleDatabase.getDatabase(context) }
    val shoeRepository = remember { ShoeRepository(database.shoeDao(), database.historyDao()) }
    val historyRepository = remember { HistoryRepository(database.historyDao(), shoeRepository) }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val imageRepository = remember { ImageRepository(context) }
    val selectedShoeStrategy = remember { 
        SelectedShoeStrategy(shoeRepository, userSettingsRepository) 
    }
    
    // Mock services
    val mockHealthService = remember { MockHealthService() }
    val mockStravaService = remember { MockStravaService() }
    
    // Progress view model
    val progressViewModel = remember { DateProgressViewModel(context, scope) }
    
    // VSI pattern state and interactor
    val state = remember { mutableStateOf(AddDistanceState()) }
    val interactor = remember { 
        AddDistanceInteractor(
            shoeRepository = shoeRepository,
            historyRepository = historyRepository,
            userSettingsRepository = userSettingsRepository,
            selectedShoeStrategy = selectedShoeStrategy
        )
    }
    
    // Load shoes on first composition
    LaunchedEffect(Unit) {
        interactor.handle(state, AddDistanceInteractor.Action.ViewAppeared)
    }
    
    // Track user settings for services - reactive to settings changes
    val userSettings by userSettingsRepository.userSettingsFlow.collectAsState(
        initial = UserSettingsData()
    )
    val healthConnectEnabled = userSettings.healthConnectEnabled
    val stravaEnabled = userSettings.stravaEnabled
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with logo, shoe image, and scroll indicators
        AddDistanceHeader(
            selectedShoe = state.value.selectedShoe,
            hasMultipleShoes = state.value.activeShoes.size > 1,
            imageRepository = imageRepository,
            onSwipeUp = { 
                interactor.handle(state, AddDistanceInteractor.Action.SwipeUp)
            },
            onSwipeDown = {
                interactor.handle(state, AddDistanceInteractor.Action.SwipeDown)
            }
        )
        
        // Date-Distance Entry Component
        DateDistanceEntryView(
            shoe = state.value.selectedShoe,
            currentDate = state.value.runDate,
            currentDistance = state.value.runDistance,
            isAddingRun = state.value.isAddingRun,
            healthConnectEnabled = healthConnectEnabled,
            stravaEnabled = stravaEnabled,
            onDateChanged = { date ->
                interactor.handle(state, AddDistanceInteractor.Action.DateChanged(date))
            },
            onDistanceChanged = { distance ->
                interactor.handle(state, AddDistanceInteractor.Action.DistanceChanged(distance))
            },
            onDistanceAdded = {
                // Handle the add run with mock service calls
                scope.launch {
                    interactor.handle(state, AddDistanceInteractor.Action.AddRunClicked)

                    // Mock service calls
                    if (healthConnectEnabled) {
                        mockHealthService.addWorkout(
                            date = state.value.runDate,
                            distance = state.value.runDistance.toDoubleOrNull() ?: 0.0,
                        )
                    }
                    if (stravaEnabled) {
                        mockStravaService.uploadActivity(
                            date = state.value.runDate,
                            distance = state.value.runDistance.toDoubleOrNull() ?: 0.0,
                            shoeName = state.value.selectedShoe?.displayName,
                        )
                    }
                }
            },
            onBounceRequested = {
                progressViewModel.triggerBounce()
            },
            onShowFavorites = {
                interactor.handle(state, AddDistanceInteractor.Action.ShowFavoritesModal)
            },
            onShowHistory = {
                interactor.handle(state, AddDistanceInteractor.Action.ShowHistoryModal)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Views - Vertical layout for iOS parity
        val bounceRequested by progressViewModel.bounceRequested

        // Distance Progress View
        ShoeCycleDistanceProgressView(
            shoe = state.value.selectedShoe,
            bounceRequested = bounceRequested,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Date Progress View
        ShoeCycleDateProgressView(
            shoe = state.value.selectedShoe,
            bounceRequested = bounceRequested,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Run History Chart - auto-updates when new distances are added
        state.value.selectedShoe?.let { shoe ->
            val histories by produceState(
                initialValue = emptyList<com.shoecycle.domain.models.History>(),
                key1 = shoe.id  // Re-fetch when shoe changes
            ) {
                historyRepository.getHistoryForShoe(shoe.id).collect { 
                    value = it 
                }
            }
            
            val weeklyData = remember(histories) {
                HistoryCollation.collateHistories(
                    histories = histories.toSet(),
                    ascending = true,
                    firstDayOfWeek = userSettings.firstDayOfWeek
                )
            }
            
            RunHistoryChartView(
                chartData = weeklyData,
                modifier = Modifier
                    .padding(vertical = 8.dp)
            )
        }
    }
    
    // Show History Modal
    if (state.value.showHistoryModal && state.value.selectedShoe != null) {
        HistoryListView(
            shoe = state.value.selectedShoe!!,
            shoeRepository = shoeRepository,
            historyRepository = historyRepository,
            userSettingsRepository = userSettingsRepository,
            onDismiss = {
                interactor.handle(state, AddDistanceInteractor.Action.HideHistoryModal)
            }
        )
    }
    
    // Show Favorite Distances Modal
    if (state.value.showFavoritesModal) {
        FavoriteDistancesView(
            userSettingsRepository = userSettingsRepository,
            onDistanceSelected = { distance ->
                interactor.handle(state, AddDistanceInteractor.Action.FavoriteDistanceSelected(distance))
            },
            onDismiss = {
                interactor.handle(state, AddDistanceInteractor.Action.HideFavoritesModal)
            }
        )
    }
}

@Composable
private fun AddDistanceHeader(
    selectedShoe: com.shoecycle.domain.models.Shoe?,
    hasMultipleShoes: Boolean,
    imageRepository: ImageRepository,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Logo (pinned to leading edge)
            Image(
                painter = painterResource(id = R.drawable.img_shoecycle_logo),
                contentDescription = "ShoeCycle Logo",
                modifier = Modifier.height(HEADER_IMAGE_SIZE),
                contentScale = ContentScale.Fit
            )
            
            // Grouped shoe image with brand name and arrows (pinned to trailing edge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Shoe image with brand name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShoeImageView(
                        shoe = selectedShoe,
                        imageRepository = imageRepository,
                        onSwipeUp = onSwipeUp,
                        onSwipeDown = onSwipeDown,
                        imageSize = HEADER_IMAGE_SIZE
                    )
                    
                    // Shoe brand name under image (iOS style)
                    Text(
                        text = selectedShoe?.displayName ?: "No Shoe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Scroll arrows indicator
                if (hasMultipleShoes) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe up",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Swipe down",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}