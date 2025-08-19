package com.shoecycle.ui.screens.add_distance.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.shoecycle.data.DistanceUnit
import com.shoecycle.domain.DistanceUtility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListItem(
    viewModel: HistoryRowViewModel,
    distanceUnit: DistanceUnit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance ->
            // Require 60% swipe to trigger delete (default is ~30%)
            totalDistance * 0.6f
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DismissBackground(dismissState)
        },
        content = {
            HistoryListItemContent(
                viewModel = viewModel,
                distanceUnit = distanceUnit,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
    )
}

@Composable
private fun HistoryListItemContent(
    viewModel: HistoryRowViewModel,
    distanceUnit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = viewModel.runDateString,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Text(
            text = "${DistanceUtility.displayString(viewModel.runDistance, distanceUnit)} ${DistanceUtility.getUnitLabel(distanceUnit)}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = when (dismissState.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            SwipeToDismissBoxValue.Settled -> Alignment.Center
        }
    ) {
        if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}