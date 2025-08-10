package com.shoecycle.ui.screens.add_distance.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoecycle.domain.models.Shoe

@Composable
fun ShoeCycleDistanceProgressView(
    shoe: Shoe?,
    bounceRequested: Boolean,
    modifier: Modifier = Modifier
) {
    // Use shoe's max distance or default to 350 (matching iOS)
    val targetDistance = shoe?.maxDistance ?: 350.0
    val currentDistance = shoe?.totalDistance ?: 0.0
    val progress = if (targetDistance > 0) {
        (currentDistance / targetDistance).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    
    // Bounce animation - twice as large
    val bounceScale by animateFloatAsState(
        targetValue = if (bounceRequested) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )
    
    // Progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Distance display - horizontal layout like iOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${currentDistance.toInt()}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50), // Green color
                modifier = Modifier.scale(bounceScale)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Miles",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4CAF50)
            )
        }
        
        // Progress bar with labels
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "${targetDistance.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

