package com.shoecycle.ui.screens.add_distance.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

@Composable
fun FavoriteDistancesView(
    userSettingsRepository: UserSettingsRepository,
    onDistanceSelected: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = remember { mutableStateOf(FavoriteDistancesState()) }
    val interactor = remember { 
        FavoriteDistancesInteractor(userSettingsRepository)
    }
    
    // Load user's favorite distances
    val userFavorites = remember {
        runBlocking {
            userSettingsRepository.userSettingsFlow.firstOrNull()?.let {
                listOf(it.favorite1, it.favorite2, it.favorite3, it.favorite4)
            } ?: listOf(0.0, 0.0, 0.0, 0.0)
        }
    }
    
    LaunchedEffect(Unit) {
        interactor.handle(state, FavoriteDistancesInteractor.Action.ViewAppeared)
    }
    
    // Handle distance selection
    LaunchedEffect(state.value.distanceToAdd) {
        if (state.value.distanceToAdd > 0) {
            onDistanceSelected(state.value.distanceToAdd)
            onDismiss()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Select Distance",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Popular Distances Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Popular Distances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FavoriteDistanceButton(
                                title = "5K",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(3.10686))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FavoriteDistanceButton(
                                title = "10K",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(6.21371))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FavoriteDistanceButton(
                                title = "5 miles",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(5.0))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FavoriteDistanceButton(
                                title = "10 miles",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(10.0))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FavoriteDistanceButton(
                                title = "Half Marathon",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(13.1))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FavoriteDistanceButton(
                                title = "Marathon",
                                onClick = {
                                    interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(26.2))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Favorite Distances Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Favorite Distances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FavoriteDistanceButton(
                                title = state.value.favorite1DisplayString ?: "Favorite 1",
                                onClick = {
                                    if (userFavorites[0] > 0) {
                                        interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(userFavorites[0]))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = userFavorites[0] > 0
                            )
                            FavoriteDistanceButton(
                                title = state.value.favorite2DisplayString ?: "Favorite 2",
                                onClick = {
                                    if (userFavorites[1] > 0) {
                                        interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(userFavorites[1]))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = userFavorites[1] > 0
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FavoriteDistanceButton(
                                title = state.value.favorite3DisplayString ?: "Favorite 3",
                                onClick = {
                                    if (userFavorites[2] > 0) {
                                        interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(userFavorites[2]))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = userFavorites[2] > 0
                            )
                            FavoriteDistanceButton(
                                title = state.value.favorite4DisplayString ?: "Favorite 4",
                                onClick = {
                                    if (userFavorites[3] > 0) {
                                        interactor.handle(state, FavoriteDistancesInteractor.Action.DistanceSelected(userFavorites[3]))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = userFavorites[3] > 0
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                interactor.handle(state, FavoriteDistancesInteractor.Action.CancelPressed)
                onDismiss()
            }) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}