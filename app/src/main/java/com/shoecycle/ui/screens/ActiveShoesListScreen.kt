package com.shoecycle.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IHistoryRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveShoesListScreen(
    onNavigateToShoeDetail: (Long) -> Unit = {}
) {
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
    
    // Modal state
    var showAddShoeModal by remember { mutableStateOf(false) }
    
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
                            showAddShoeModal = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_shoe)
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
                                },
                                onNavigateToDetail = onNavigateToShoeDetail
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
    
    // Add Shoe Modal
    AddShoeModal(
        isVisible = showAddShoeModal,
        shoeRepository = shoeRepository,
        onDismiss = { showAddShoeModal = false },
        onShoeSaved = {
            showAddShoeModal = false
            // Refresh the list to show the new shoe
            interactor.handle(state, ActiveShoesInteractor.Action.ViewAppeared)
        }
    )
}