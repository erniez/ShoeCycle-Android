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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShoeCycleDateProgressView(
    shoe: Shoe?,
    bounceRequested: Boolean, // Kept for interface compatibility but not used
    modifier: Modifier = Modifier
) {
    // Calculate days left until expiration
    val daysLeft = if (shoe != null) {
        calculateDaysLeft(shoe.expirationDate)
    } else {
        0
    }
    
    // Calculate progress based on time elapsed
    val progress = if (shoe != null) {
        val totalDays = calculateTotalDays(shoe.startDate, shoe.expirationDate)
        val elapsedDays = calculateDaysSincePurchase(shoe.startDate)
        if (totalDays > 0) {
            (elapsedDays.toFloat() / totalDays).coerceIn(0.0f, 1.0f)
        } else {
            0f
        }
    } else {
        0f
    }
    
    // No bounce animation for date progress
    
    // Progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )
    
    // Date formatter for M/d/yy format (matching iOS)
    val dateFormatter = remember { SimpleDateFormat("M/d/yy", Locale.getDefault()) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Days left display - horizontal layout like iOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${daysLeft.coerceAtLeast(0)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00BCD4) // Cyan color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Days Left",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF00BCD4)
            )
        }
        
        // Progress bar with date labels
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF00BCD4),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shoe?.let { dateFormatter.format(it.startDate) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = shoe?.let { dateFormatter.format(it.expirationDate) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun calculateDaysSincePurchase(startDate: Date): Int {
    val now = Date()
    val diffInMillis = now.time - startDate.time
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
}

private fun calculateDaysLeft(expirationDate: Date): Int {
    val now = Date()
    val diffInMillis = expirationDate.time - now.time
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
}

private fun calculateTotalDays(startDate: Date, expirationDate: Date): Int {
    val diffInMillis = expirationDate.time - startDate.time
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
}

