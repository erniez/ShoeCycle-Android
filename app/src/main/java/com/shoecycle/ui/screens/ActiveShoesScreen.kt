package com.shoecycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ActiveShoesState(
    val shoes: List<Shoe> = emptyList(),
    val isLoading: Boolean = true,
    val selectedShoeId: Long? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES
)

class ActiveShoesInteractor(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        object ViewAppeared : Action()
    }
    
    fun handle(state: MutableState<ActiveShoesState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> {
                scope.launch {
                    try {
                        // Load user settings for distance unit
                        val settings = userSettingsRepository.userSettingsFlow.first()
                        
                        // Collect active shoes
                        shoeRepository.getActiveShoes().collect { shoes ->
                            state.value = state.value.copy(
                                shoes = shoes,
                                isLoading = false,
                                distanceUnit = settings.distanceUnit
                            )
                        }
                    } catch (e: Exception) {
                        state.value = state.value.copy(isLoading = false)
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
    val interactor = remember { ActiveShoesInteractor(shoeRepository, userSettingsRepository) }
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
                                isSelected = shoe.id == state.value.selectedShoeId
                            )
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
    isSelected: Boolean = false
) {
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            
            // Distance and selected indicator row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Text(
                        text = stringResource(R.string.selected),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = "${stringResource(R.string.distance)}: ${shoe.totalDistance} ${distanceUnit.displayString}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}