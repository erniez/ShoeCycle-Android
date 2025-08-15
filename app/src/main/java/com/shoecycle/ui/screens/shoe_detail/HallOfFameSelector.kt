package com.shoecycle.ui.screens.shoe_detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoecycle.ui.theme.shoeCycleOrange

@Composable
fun HallOfFameSelector(
    isInHallOfFame: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isInHallOfFame) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "hall_of_fame_alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isInHallOfFame) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInHallOfFame) {
                shoeCycleOrange.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isInHallOfFame) {
            androidx.compose.foundation.BorderStroke(2.dp, shoeCycleOrange)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(animatedAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Trophy emoji
            Text(
                text = "🏆",
                fontSize = 24.sp,
                modifier = Modifier.alpha(if (isInHallOfFame) 1f else 0.6f)
            )
            
            // Action text
            Text(
                text = if (isInHallOfFame) {
                    "Remove from Hall of Fame"
                } else {
                    "Add to Hall of Fame"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isInHallOfFame) {
                    shoeCycleOrange
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Visual indicator for current state
            if (isInHallOfFame) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "In Hall of Fame",
                    tint = shoeCycleOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}