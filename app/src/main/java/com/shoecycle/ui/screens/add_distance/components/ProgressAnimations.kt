package com.shoecycle.ui.screens.add_distance.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Utility functions for progress view animations
 */
object ProgressAnimations {
    
    /**
     * Creates a bounce animation that can be triggered by a boolean state
     */
    @Composable
    fun rememberBounceAnimation(
        trigger: Boolean,
        bounceScale: Float = 1.15f,
        resetDelay: Long = 300L
    ): Float {
        var internalTrigger by remember { mutableStateOf(false) }
        
        // React to external trigger
        LaunchedEffect(trigger) {
            if (trigger) {
                internalTrigger = true
                delay(resetDelay)
                internalTrigger = false
            }
        }
        
        return animateFloatAsState(
            targetValue = if (internalTrigger) bounceScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "bounce_animation"
        ).value
    }
    
    /**
     * Creates a pulse animation for service indicators
     */
    @Composable
    fun rememberPulseAnimation(
        isActive: Boolean,
        pulseScale: Float = 1.1f,
        duration: Int = 1000
    ): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
        
        return if (isActive) {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = pulseScale,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            ).value
        } else {
            1f
        }
    }
    
    /**
     * Creates a wobble animation for validation errors
     */
    @Composable
    fun rememberWobbleAnimation(
        trigger: Boolean,
        wobbleStrength: Float = 10f
    ): Float {
        var internalTrigger by remember { mutableStateOf(false) }
        
        LaunchedEffect(trigger) {
            if (trigger) {
                internalTrigger = true
                delay(500) // Duration of wobble
                internalTrigger = false
            }
        }
        
        val animatable = remember { Animatable(0f) }
        
        LaunchedEffect(internalTrigger) {
            if (internalTrigger) {
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                        wobbleStrength at 50
                        -wobbleStrength at 100
                        wobbleStrength * 0.7f at 150
                        -wobbleStrength * 0.7f at 200
                        wobbleStrength * 0.4f at 250
                        -wobbleStrength * 0.4f at 300
                        wobbleStrength * 0.2f at 350
                        0f at 500
                    }
                )
            }
        }
        
        return animatable.value
    }
    
    /**
     * Creates a slide-in animation for content
     */
    @Composable
    fun rememberSlideInAnimation(
        visible: Boolean,
        slideDistance: Float = 50f
    ): Float {
        return animateFloatAsState(
            targetValue = if (visible) 0f else slideDistance,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "slide_in"
        ).value
    }
    
    /**
     * Creates a fade animation
     */
    @Composable
    fun rememberFadeAnimation(
        visible: Boolean,
        duration: Int = 300
    ): Float {
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(duration, easing = FastOutSlowInEasing),
            label = "fade_animation"
        ).value
    }
}

/**
 * Modifier extensions for common animations
 */

/**
 * Applies a bounce effect to any composable
 */
fun Modifier.bounce(
    trigger: Boolean,
    bounceScale: Float = 1.15f,
    resetDelay: Long = 300L
): Modifier = composed {
    val scale = ProgressAnimations.rememberBounceAnimation(trigger, bounceScale, resetDelay)
    this.scale(scale)
}

/**
 * Applies a pulse effect to any composable
 */
fun Modifier.pulse(
    isActive: Boolean,
    pulseScale: Float = 1.1f,
    duration: Int = 1000
): Modifier = composed {
    val scale = ProgressAnimations.rememberPulseAnimation(isActive, pulseScale, duration)
    this.scale(scale)
}

/**
 * Applies a wobble effect to any composable (useful for error states)
 */
fun Modifier.wobble(
    trigger: Boolean,
    wobbleStrength: Float = 10f
): Modifier = composed {
    val offsetX = ProgressAnimations.rememberWobbleAnimation(trigger, wobbleStrength)
    this.graphicsLayer(translationX = offsetX)
}

/**
 * Applies a slide-in effect to any composable
 */
fun Modifier.slideIn(
    visible: Boolean,
    slideDistance: Float = 50f
): Modifier = composed {
    val offsetY = ProgressAnimations.rememberSlideInAnimation(visible, slideDistance)
    this.graphicsLayer(translationY = offsetY)
}

/**
 * Applies a fade effect to any composable
 */
fun Modifier.fade(
    visible: Boolean,
    duration: Int = 300
): Modifier = composed {
    val alpha = ProgressAnimations.rememberFadeAnimation(visible, duration)
    this.graphicsLayer(alpha = alpha)
}

/**
 * Progress bar animation utilities
 */
object ProgressBarAnimations {
    
    /**
     * Animates progress value changes smoothly
     */
    @Composable
    fun animateProgressAsState(
        targetValue: Float,
        duration: Int = 1000,
        delayMillis: Int = 0
    ): State<Float> {
        return animateFloatAsState(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            label = "progress_animation"
        )
    }
    
    /**
     * Creates a sequential progress animation (useful for multiple progress bars)
     */
    @Composable
    fun animateSequentialProgress(
        targetValues: List<Float>,
        duration: Int = 800,
        delayBetween: Int = 200
    ): List<State<Float>> {
        return targetValues.mapIndexed { index, targetValue ->
            animateFloatAsState(
                targetValue = targetValue,
                animationSpec = tween(
                    durationMillis = duration,
                    delayMillis = index * delayBetween,
                    easing = FastOutSlowInEasing
                ),
                label = "sequential_progress_$index"
            )
        }
    }
}