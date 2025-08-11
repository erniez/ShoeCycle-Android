package com.shoecycle.ui.screens.add_distance.components.chart

import androidx.compose.animation.core.*
import androidx.compose.runtime.*

/**
 * Controller for managing chart animations
 * Simplified to only handle value animations (dots animating from zero to actual values)
 */
class ChartAnimationController {
    
    /**
     * Animates selection changes with smooth transitions
     */
    @Composable
    fun animateSelection(
        isSelected: Boolean,
        index: Int
    ): SelectionAnimation {
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.2f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = "selectionScale_$index"
        )
        
        val alpha by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0.7f,
            animationSpec = tween(200),
            label = "selectionAlpha_$index"
        )
        
        return SelectionAnimation(scale, alpha)
    }
    
    data class SelectionAnimation(
        val scale: Float,
        val alpha: Float
    )
}