package com.shoecycle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoecycle.R
import com.shoecycle.data.UserSettingsRepository
import com.shoecycle.data.repository.interfaces.IShoeRepository
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class ShoeDetailState(
    val shoe: Shoe? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val distanceUnit: String = "mi"
)

class ShoeDetailInteractor(
    private val shoeRepository: IShoeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val distanceUtility: DistanceUtility,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class ViewAppeared(val shoeId: Long) : Action()
        object Refresh : Action()
    }
    
    fun handle(state: MutableState<ShoeDetailState>, action: Action) {
        when (action) {
            is Action.ViewAppeared -> loadShoe(state, action.shoeId)
            is Action.Refresh -> {
                state.value.shoe?.let { shoe ->
                    loadShoe(state, shoe.id)
                }
            }
        }
    }
    
    private fun loadShoe(state: MutableState<ShoeDetailState>, shoeId: Long) {
        state.value = state.value.copy(isLoading = true, errorMessage = null)
        
        scope.launch {
            try {
                shoeRepository.getShoeById(shoeId).collect { shoe ->
                    if (shoe != null) {
                        val unitLabel = distanceUtility.getUnitLabel()
                        
                        state.value = state.value.copy(
                            shoe = shoe,
                            isLoading = false,
                            errorMessage = null,
                            distanceUnit = unitLabel
                        )
                    } else {
                        state.value = state.value.copy(
                            isLoading = false,
                            errorMessage = "Shoe not found"
                        )
                    }
                }
            } catch (e: Exception) {
                state.value = state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading shoe: ${e.message}"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoeDetailScreen(
    shoeId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val shoeRepository = remember { 
        com.shoecycle.data.repository.ShoeRepository.create(context)
    }
    val userSettingsRepository = remember { UserSettingsRepository(context) }
    val distanceUtility = remember { DistanceUtility(userSettingsRepository) }
    val interactor = remember { 
        ShoeDetailInteractor(shoeRepository, userSettingsRepository, distanceUtility) 
    }
    val state = remember { mutableStateOf(ShoeDetailState()) }
    
    LaunchedEffect(shoeId) {
        interactor.handle(state, ShoeDetailInteractor.Action.ViewAppeared(shoeId))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shoe_detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                    LoadingScreen()
                }
                state.value.errorMessage != null -> {
                    ErrorScreen(
                        message = state.value.errorMessage!!,
                        onRetry = { 
                            interactor.handle(state, ShoeDetailInteractor.Action.Refresh)
                        }
                    )
                }
                state.value.shoe != null -> {
                    ShoeDetailContent(
                        state = state.value,
                        distanceUtility = distanceUtility
                    )
                }
                else -> {
                    ErrorScreen(
                        message = "Shoe not found",
                        onRetry = { 
                            interactor.handle(state, ShoeDetailInteractor.Action.Refresh)
                        }
                    )
                }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
private fun ShoeDetailContent(
    state: ShoeDetailState,
    distanceUtility: DistanceUtility
) {
    val shoe = state.shoe ?: return
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShoeInformationSection(
                shoe = shoe
            )
        }
        
        item {
            DistanceInformationSection(
                shoe = shoe,
                distanceUtility = distanceUtility,
                distanceUnit = state.distanceUnit
            )
        }
        
        item {
            WearTimeInformationSection(shoe = shoe)
        }
        
        item {
            ShoeImageSection(shoe = shoe)
        }
    }
}

@Composable
private fun ShoeInformationSection(
    shoe: Shoe
) {
    ShoeCycleSectionCard(
        title = "Shoe",
        color = Color(0xFFFF9800), // Orange color like iOS
        icon = Icons.Default.Build // Placeholder - will use shoe icon later
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Name:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = shoe.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DistanceInformationSection(
    shoe: Shoe,
    distanceUtility: DistanceUtility,
    distanceUnit: String
) {
    var startDistanceDisplay by remember { mutableStateOf("") }
    var maxDistanceDisplay by remember { mutableStateOf("") }
    
    LaunchedEffect(shoe, distanceUnit) {
        startDistanceDisplay = distanceUtility.displayString(shoe.startDistance)
        maxDistanceDisplay = distanceUtility.displayString(shoe.maxDistance)
    }
    
    ShoeCycleSectionCard(
        title = "Distance",
        color = Color(0xFF4CAF50), // Green color like iOS
        icon = Icons.Default.Build // Placeholder - will use steps icon later
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start Distance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Start:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$startDistanceDisplay $distanceUnit",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Max Distance
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Max:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$maxDistanceDisplay $distanceUnit",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WearTimeInformationSection(
    shoe: Shoe
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    ShoeCycleSectionCard(
        title = "Wear Time",
        color = Color(0xFF2196F3), // Blue color like iOS
        icon = Icons.Default.Build // Placeholder - will use clock icon later
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormatter.format(shoe.startDate),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // End Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "End:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormatter.format(shoe.expirationDate),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ShoeImageSection(
    shoe: Shoe
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 32.dp), // Matching iOS padding
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for shoe image - will be enhanced in future milestones
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "${shoe.displayName} image",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShoeCycleSectionCard(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left side - Icon and title (mimicking iOS design)
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(40.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .padding(vertical = 8.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(8f, 8f)
                        )
                    )
                }
            }
            
            // Content area
            Box(
                modifier = Modifier.weight(1f)
            ) {
                content()
            }
        }
    }
}
