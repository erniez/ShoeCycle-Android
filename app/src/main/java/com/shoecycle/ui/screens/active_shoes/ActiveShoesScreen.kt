package com.shoecycle.ui.screens.active_shoes

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
import com.shoecycle.data.DistanceUnit
import com.shoecycle.domain.DistanceUtility
import com.shoecycle.domain.models.Shoe

@Composable
fun ActiveShoesScreen() {
    ActiveShoesNavigation()
}

@Composable
fun ActiveShoesRowView(
    shoe: Shoe,
    distanceUnit: DistanceUnit,
    isSelected: Boolean = false,
    onShoeSelected: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main content column with padding on the right for the button area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp) // Make room for the full-height button
            ) {
                // Brand name
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
                        text = "${stringResource(R.string.distance)}: ${DistanceUtility.displayString(shoe.totalDistance, distanceUnit)} ${DistanceUtility.getUnitLabel(distanceUnit)}",
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
            
            // Clickable info button area that matches column height
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { onNavigateToDetail(shoe.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "View details",
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}