package com.shoecycle.ui.screens.add_distance.components.chart

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import com.shoecycle.ui.screens.add_distance.utils.WeeklyCollatedNew
import kotlinx.coroutines.delay

/**
 * Controller for managing all chart animations
 * Handles value animations, morphing transitions, and selection states
 */
class ChartAnimationController {
    
    /**
     * Main animation state for chart values
     */
    data class ChartAnimationState(
        val animatedValues: List<Float> = emptyList(),
        val isInitialLoad: Boolean = true,
        val showValues: Boolean = false,
        val dataSignature: Int = 0
    )
    
    /**
     * Animates chart data points with different behaviors for initial load vs data changes
     * 
     * @param data The chart data to animate
     * @param initialDelay Delay in ms before initial animation starts (default 500ms)
     * @return List of animated values for each data point
     */
    @Composable
    fun animateChartValues(
        data: List<WeeklyCollatedNew>,
        initialDelay: Long = 500L
    ): List<Float> {
        // Track if this is the initial load for animation (start from zero)
        var isInitialLoad by remember { mutableStateOf(true) }
        var showValues by remember { mutableStateOf(false) }
        
        // Track data signature to detect shoe changes
        val dataSignature = remember(data) { 
            data.hashCode() // Simple way to detect when data fundamentally changes
        }
        
        LaunchedEffect(dataSignature) {
            if (data.isNotEmpty()) {
                if (isInitialLoad) {
                    // Initial load: start from zero after delay
                    delay(initialDelay)
                    showValues = true
                    isInitialLoad = false
                } else {
                    // Shoe change: morph immediately from old to new values
                    showValues = true
                }
            }
        }
        
        // Animate points - either from zero (initial) or from old values (shoe change)
        val animatedPointValues = remember { mutableStateListOf<Float>() }
        
        data.forEachIndexed { index, point ->
            val targetValue = point.runDistance.toFloat()
            
            // Determine starting value
            val startValue = when {
                !showValues && isInitialLoad -> 0f  // Initial load starts at zero
                index < animatedPointValues.size -> animatedPointValues[index]  // Morph from current value
                else -> targetValue  // New point starts at target
            }
            
            val animatedValue by animateFloatAsState(
                targetValue = if (showValues) targetValue else startValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "pointValue_$index"
            )
            
            if (index < animatedPointValues.size) {
                animatedPointValues[index] = animatedValue
            } else {
                animatedPointValues.add(animatedValue)
            }
        }
        
        // Trim excess values if new data has fewer points
        while (animatedPointValues.size > data.size) {
            animatedPointValues.removeAt(animatedPointValues.size - 1)
        }
        
        return animatedPointValues
    }
    
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
    
    /**
     * Configuration for animation timing and behavior
     */
    data class AnimationConfig(
        val initialLoadDelay: Long = 500L,
        val springDampingRatio: Float = Spring.DampingRatioMediumBouncy,
        val springStiffness: Float = Spring.StiffnessLow,
        val selectionDuration: Int = 200
    )
    
    /**
     * Configurable animation with custom parameters
     */
    @Composable
    fun animateChartValuesWithConfig(
        data: List<WeeklyCollatedNew>,
        config: AnimationConfig = AnimationConfig()
    ): List<Float> {
        // Track if this is the initial load for animation (start from zero)
        var isInitialLoad by remember { mutableStateOf(true) }
        var showValues by remember { mutableStateOf(false) }
        
        // Track data signature to detect shoe changes
        val dataSignature = remember(data) { 
            data.hashCode()
        }
        
        LaunchedEffect(dataSignature) {
            if (data.isNotEmpty()) {
                if (isInitialLoad) {
                    // Initial load: start from zero after delay
                    delay(config.initialLoadDelay)
                    showValues = true
                    isInitialLoad = false
                } else {
                    // Shoe change: morph immediately from old to new values
                    showValues = true
                }
            }
        }
        
        // Animate points - either from zero (initial) or from old values (shoe change)
        val animatedPointValues = remember { mutableStateListOf<Float>() }
        
        data.forEachIndexed { index, point ->
            val targetValue = point.runDistance.toFloat()
            
            // Determine starting value
            val startValue = when {
                !showValues && isInitialLoad -> 0f  // Initial load starts at zero
                index < animatedPointValues.size -> animatedPointValues[index]  // Morph from current value
                else -> targetValue  // New point starts at target
            }
            
            val animatedValue by animateFloatAsState(
                targetValue = if (showValues) targetValue else startValue,
                animationSpec = spring(
                    dampingRatio = config.springDampingRatio,
                    stiffness = config.springStiffness
                ),
                label = "pointValue_$index"
            )
            
            if (index < animatedPointValues.size) {
                animatedPointValues[index] = animatedValue
            } else {
                animatedPointValues.add(animatedValue)
            }
        }
        
        // Trim excess values if new data has fewer points
        while (animatedPointValues.size > data.size) {
            animatedPointValues.removeAt(animatedPointValues.size - 1)
        }
        
        return animatedPointValues
    }
}