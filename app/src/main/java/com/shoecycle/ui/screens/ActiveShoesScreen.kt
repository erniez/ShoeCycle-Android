package com.shoecycle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.MockShoeGenerator
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ActiveShoesState(
    val shoes: List<Shoe> = emptyList(),
    val isLoading: Boolean = true,
    val isGeneratingTestData: Boolean = false,
    val selectedShoeId: Long? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class ActiveShoesInteractor(
    private val shoeRepository: IShoeRepository,
    private val historyRepository: IHistoryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
        object GenerateTestData : Action()
        data class ShoeSelected(val shoeId: Long) : Action()
    }
    
    fun handle(state: MutableState<ActiveShoesState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                scope.launch {
                    try {
                        // Combine both flows to react to changes in either
                        combine(
                            userSettingsRepository.userSettingsFlow,
                            shoeRepository.getActiveShoes()
                        ) { settings, shoes ->
                            state.value = state.value.copy(
                                shoes = shoes,
                                isLoading = false,
                                distanceUnit = settings.distanceUnit,
                                selectedShoeId = settings.selectedShoeId
                            )
                        }.collect { }
                    } catch (e: Exception) {
                        state.value = state.value.copy(isLoading = false)
                    }
                }
            }
            is Action.GenerateTestData -> {
                scope.launch {
                    try {
                        state.value = state.value.copy(isGeneratingTestData = true)
                        val mockGenerator = MockShoeGenerator(shoeRepository, historyRepository)
                        mockGenerator.generateNewShoeWithData()
                        state.value = state.value.copy(isGeneratingTestData = false)
                    } catch (e: Exception) {
                        state.value = state.value.copy(isGeneratingTestData = false)
                        // TODO: Handle error state
                    }
                }
            }
            is Action.ShoeSelected -> {
                scope.launch {
                    try {
                        // Update user settings - the Flow will update state naturally
                        userSettingsRepository.updateSelectedShoeId(action.shoeId)
                    } catch (e: Exception) {
                        // TODO: Handle error state
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveShoesScreen() {
    val context = LocalContext.current
    val shoeRepository = remember { 
        // TODO: Use dependency injection
        com.shoecycle.data.repository.ShoeRepository.create(context)
    }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val historyRepository = remember {
        com.shoecycle.data.repository.HistoryRepository.create(context, shoeRepository)
    }
    val interactor = remember { ActiveShoesInteractor(shoeRepository, historyRepository, userSettingsRepository) }
    val state = remember { mutableStateOf(ActiveShoesState()) }
    
    LaunchedEffect(Unit) {
        interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.active_shoes)) },
                actions = {
                    IconButton(
                        onClick = {
                            // TODO: Implement add shoe functionality
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_shoe)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Implement add shoe functionality
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_shoe)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.value.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.value.shoes.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .widthIn(max = 300.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_shoes_hint),
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Debug-only test data generation button for empty state
                            if (true) { // Always show for now, can be changed to BuildConfig.DEBUG
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        interactor.handle(state, ActiveShoesInteractor.Action.GenerateTestData)
                                    },
                                    modifier = Modifier.widthIn(max = 280.dp),
                                    enabled = !state.value.isGeneratingTestData
                                ) {
                                    if (state.value.isGeneratingTestData) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.generating))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.generate_test_shoe))
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.value.shoes) { shoe ->
                            ActiveShoesRowView(
                                shoe = shoe,
                                distanceUnit = state.value.distanceUnit,
                                isSelected = shoe.id == state.value.selectedShoeId,
                                onShoeSelected = { shoeId ->
                                    interactor.handle(state, ActiveShoesInteractor.Action.ShoeSelected(shoeId))
                                }
                            )
                        }
                        
                        // Debug-only test data generation button
                        if (true) { // Always show for now, can be changed to BuildConfig.DEBUG
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        interactor.handle(state, ActiveShoesInteractor.Action.GenerateTestData)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.value.isGeneratingTestData
                                ) {
                                    if (state.value.isGeneratingTestData) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.generating))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.generate_test_shoe))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveShoesRowView(
    shoe: Shoe,
    distanceUnit: DistanceUnit,
    isSelected: Boolean = false,
    onShoeSelected: (Long) -> Unit = {}
) {
    // Animate the distance text position to make room for "Selected" text
    val distanceTextOffset by animateFloatAsState(
        targetValue = if (isSelected) 80f else 0f, // 80dp offset when selected
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "distanceTextOffset"
    )
    
    // Animate selected text alpha and position
    val selectedTextAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "selectedTextAlpha"
    )
    
    val selectedTextTranslationX by animateFloatAsState(
        targetValue = if (isSelected) 0f else -100f,
        animationSpec = tween(
            durationMillis = 500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "selectedTextTranslationX"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShoeSelected(shoe.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Brand name row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shoe.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Distance and selected indicator row with fixed positioning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp) // Fixed height to prevent layout changes
            ) {
                // Selected text - always present but with alpha and offset animation
                Text(
                    text = stringResource(R.string.selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            alpha = selectedTextAlpha
                            translationX = selectedTextTranslationX
                        }
                )
                
                // Distance text with smooth translation
                Text(
                    text = "${stringResource(R.string.distance)}: ${shoe.totalDistance} ${distanceUnit.displayString}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = distanceTextOffset.dp)
                )
            }
        }
    }
}