package com.shoecycle.ui.screens.add_distance.components.chart

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoecycle.ui.screens.add_distance.utils.WeeklyCollatedNew
import com.shoecycle.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interactive Run History Chart component
 * Displays weekly running distance data with touch interactions and scrolling
 */
@Composable
fun RunHistoryChartView(
    chartData: List<WeeklyCollatedNew>,
    modifier: Modifier = Modifier
) {
    // VSI pattern state management
    var state by remember { mutableStateOf(RunHistoryChartState()) }
    val interactor = remember { RunHistoryChartInteractor() }
    val animationController = remember { ChartAnimationController() }
    
    // Update state when data changes
    LaunchedEffect(chartData) {
        state = interactor.handle(state, RunHistoryChartInteractor.Action.DataUpdated(chartData))
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(12.dp),
        color = shoeCycleBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Selected week details (if any)
            SelectedWeekDetails(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = shoeCycleBlue
                        )
                    }
                } else if (state.chartData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No run data available",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    ScrollableChartCanvas(
                        state = state,
                        interactor = interactor,
                        animationController = animationController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onStateChange = { newState ->
                            state = newState
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedWeekDetails(
    state: RunHistoryChartState,
    modifier: Modifier = Modifier
) {
    state.selectedWeekIndex?.let { index ->
        if (index in state.chartData.indices) {
            val week = state.chartData[index]
            val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
            
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = shoeCycleSecondaryBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Week of ${dateFormatter.format(week.date)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.1f miles".format(week.runDistance),
                        color = shoeCycleGreen,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableChartCanvas(
    state: RunHistoryChartState,
    interactor: RunHistoryChartInteractor,
    animationController: ChartAnimationController,
    modifier: Modifier = Modifier,
    onStateChange: (RunHistoryChartState) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Auto-scroll to end when data loads
    LaunchedEffect(state.shouldScrollToEnd) {
        if (state.shouldScrollToEnd && state.chartData.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
            onStateChange(interactor.handle(state, RunHistoryChartInteractor.Action.ScrollCompleted))
        }
    }
    
    // Calculate chart width based on data points
    val chartWidth = with(density) {
        if (state.chartData.size <= 6) {
            0.dp // Use full width
        } else {
            (state.chartData.size * 80).dp // 80dp per data point for scrolling
        }
    }
    
    // Y-axis overlays chart content, chart scrolls behind it
    Box(
        modifier = modifier.padding(start = 35.dp) // Space for Y-axis labels
    ) {
        // Scrollable chart area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (chartWidth > 0.dp) {
                        Modifier.horizontalScroll(scrollState)
                    } else {
                        Modifier
                    }
                )
        ) {
            ChartCanvas(
                data = state.chartData,
                maxDistance = state.maxDistance,
                selectedWeekIndex = state.selectedWeekIndex,
                animationController = animationController,
                onWeekSelected = { index ->
                    onStateChange(interactor.handle(state, RunHistoryChartInteractor.Action.WeekSelected(index)))
                },
                modifier = if (chartWidth > 0.dp) {
                    Modifier.width(chartWidth)
                } else {
                    Modifier.fillMaxWidth()
                },
                showYAxis = false // Don't draw Y-axis in scrollable area
            )
        }
        
        // Fixed Y-axis labels overlay on top
        YAxisLabels(
            maxDistance = state.maxDistance,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-35).dp) // Position labels in the padding area
                .width(35.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun YAxisLabels(
    maxDistance: Double,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        val canvasHeight = size.height
        val padding = 16.dp.toPx()
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw Y-axis line on the right edge (where it meets the chart)
        drawLine(
            color = Color.Gray,
            start = Offset(size.width, padding),
            end = Offset(size.width, padding + chartHeight),
            strokeWidth = 2f
        )
        
        // Draw Y-axis labels
        val labelCount = 5
        val textStyle = TextStyle(
            color = Color.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.End
        )
        
        for (i in 0..labelCount) {
            val value = maxDistance * (labelCount - i) / labelCount
            val y = padding + (chartHeight * i / labelCount)
            
            // Draw label as integer, right-aligned to the left of axis
            val text = value.toInt().toString()
            val textResult = textMeasurer.measure(text, textStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = size.width - textResult.size.width - 8f,
                    y = y - textResult.size.height / 2
                )
            )
            
            // Draw tick mark
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(size.width - 3f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun ChartCanvas(
    data: List<WeeklyCollatedNew>,
    maxDistance: Double,
    selectedWeekIndex: Int?,
    animationController: ChartAnimationController,
    onWeekSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showYAxis: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val dateFormatter = remember { SimpleDateFormat("M/d", Locale.US) }
    val density = LocalDensity.current
    
    // Store data point positions for touch detection
    var dataPointPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    // Get animated values from the animation controller
    val animatedPointValues = animationController.animateChartValues(
        data = data,
        initialDelay = 500L
    )
    
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(data, selectedWeekIndex) {
                detectTapGestures { tapOffset ->
                    // Find the closest data point to the tap
                    val threshold = with(density) { 30.dp.toPx() }
                    var closestIndex = -1
                    var closestDistance = Float.MAX_VALUE
                    
                    dataPointPositions.forEachIndexed { index, point ->
                        val distance = (point - tapOffset).distanceToOrigin()
                        if (distance < closestDistance && distance < threshold) {
                            closestDistance = distance
                            closestIndex = index
                        }
                    }
                    
                    if (closestIndex >= 0) {
                        // If tapping the already selected point, unselect it
                        if (closestIndex == selectedWeekIndex) {
                            onWeekSelected(-1) // Pass -1 to clear selection
                        } else {
                            onWeekSelected(closestIndex)
                        }
                    } else {
                        // Tapped in empty space, clear selection
                        onWeekSelected(-1)
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 32.dp.toPx() // More space for X-axis labels
        val paddingLeft = if (showYAxis) 60f else 0f
        val paddingRight = 16.dp.toPx()
        
        // Calculate chart area
        val chartWidth = canvasWidth - paddingLeft - paddingRight
        val chartHeight = canvasHeight - paddingTop - paddingBottom
        
        // Draw grid lines first (behind everything)
        drawGridLines(
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            chartWidth = chartWidth,
            chartHeight = chartHeight
        )
        
        // Draw axes (only X-axis if Y-axis is handled separately)
        drawAxes(
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            showYAxis = showYAxis
        )
        
        // Draw max distance indicator line
        if (maxDistance > 0) {
            drawMaxDistanceLine(
                maxDistance = maxDistance,
                paddingLeft = paddingLeft,
                paddingTop = paddingTop,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                textMeasurer = textMeasurer
            )
        }
        
        // Draw Y-axis labels only if showYAxis is true
        if (showYAxis) {
            drawYAxisLabels(
                maxDistance = maxDistance,
                paddingLeft = paddingLeft,
                paddingTop = paddingTop,
                chartHeight = chartHeight,
                textMeasurer = textMeasurer
            )
        }
        
        // Draw X-axis labels (dates)
        drawXAxisLabels(
            data = data,
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            paddingBottom = paddingBottom,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            dateFormatter = dateFormatter,
            textMeasurer = textMeasurer
        )
        
        // Calculate and store data point positions with animated values
        val positions = mutableListOf<Offset>()
        data.forEachIndexed { index, point ->
            val x = paddingLeft + (chartWidth * index / (data.size - 1).coerceAtLeast(1))
            val animatedDistance = if (index < animatedPointValues.size) animatedPointValues[index].toDouble() else point.runDistance
            val yRatio = if (maxDistance > 0) animatedDistance / maxDistance else 0.0
            val y = paddingTop + chartHeight * (1 - yRatio).toFloat()
            positions.add(Offset(x, y))
        }
        dataPointPositions = positions
        
        // Draw data line immediately (no animation)
        if (data.size >= 2) {
            drawDataLine(positions)
        }
        
        // Draw data points
        drawDataPoints(
            positions = positions,
            selectedIndex = selectedWeekIndex
        )
    }
}

private fun DrawScope.drawGridLines(
    paddingLeft: Float,
    paddingTop: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.1f)
    
    // Horizontal grid lines
    for (i in 1..4) {
        val y = paddingTop + (chartHeight * i / 5)
        drawLine(
            color = gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(paddingLeft + chartWidth, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawAxes(
    paddingLeft: Float,
    paddingTop: Float,
    chartWidth: Float,
    chartHeight: Float,
    showYAxis: Boolean
) {
    val axisColor = Color.Gray
    val strokeWidth = 2f
    
    // Y-axis (only if showYAxis is true)
    if (showYAxis) {
        drawLine(
            color = axisColor,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + chartHeight),
            strokeWidth = strokeWidth
        )
    }
    
    // X-axis
    drawLine(
        color = axisColor,
        start = Offset(paddingLeft, paddingTop + chartHeight),
        end = Offset(paddingLeft + chartWidth, paddingTop + chartHeight),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawMaxDistanceLine(
    maxDistance: Double,
    paddingLeft: Float,
    paddingTop: Float,
    chartWidth: Float,
    chartHeight: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val y = paddingTop // Max is at the top
    
    // Draw dashed line in white
    drawLine(
        color = Color.White.copy(alpha = 0.5f), // White with transparency
        start = Offset(paddingLeft, y),
        end = Offset(paddingLeft + chartWidth, y),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
    
    // Draw label
    val text = "Max: %d".format(maxDistance.toInt())
    val textStyle = TextStyle(
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 10.sp
    )
    val textResult = textMeasurer.measure(text, textStyle)
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = paddingLeft + chartWidth - textResult.size.width - 4f,
            y = y - textResult.size.height - 2f
        )
    )
}

private fun DrawScope.drawYAxisLabels(
    maxDistance: Double,
    paddingLeft: Float,
    paddingTop: Float,
    chartHeight: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val labelCount = 5
    val textStyle = TextStyle(
        color = Color.Gray,
        fontSize = 10.sp
    )
    
    // Calculate max width needed for labels (3 digits)
    val maxLabelWidth = textMeasurer.measure("999", textStyle).size.width
    
    for (i in 0..labelCount) {
        val value = maxDistance * (labelCount - i) / labelCount
        val y = paddingTop + (chartHeight * i / labelCount)
        
        // Draw label as integer
        val text = value.toInt().toString()
        val textResult = textMeasurer.measure(text, textStyle)
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                x = paddingLeft - maxLabelWidth - 8f + (maxLabelWidth - textResult.size.width),
                y = y - textResult.size.height / 2
            )
        )
    }
}

private fun DrawScope.drawXAxisLabels(
    data: List<WeeklyCollatedNew>,
    paddingLeft: Float,
    paddingTop: Float,
    paddingBottom: Float,
    chartWidth: Float,
    chartHeight: Float,
    dateFormatter: SimpleDateFormat,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    if (data.isEmpty()) return
    
    val textStyle = TextStyle(
        color = Color.Gray,
        fontSize = 9.sp
    )
    
    // Show fewer labels if many data points
    val labelInterval = when {
        data.size <= 6 -> 1
        data.size <= 12 -> 2
        else -> 3
    }
    
    data.forEachIndexed { index, point ->
        if (index % labelInterval == 0) {
            val x = paddingLeft + (chartWidth * index / (data.size - 1).coerceAtLeast(1))
            val y = paddingTop + chartHeight + 8f
            
            val text = dateFormatter.format(point.date)
            val textResult = textMeasurer.measure(text, textStyle)
            
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = x - textResult.size.width / 2,
                    y = y
                )
            )
        }
    }
}

private fun DrawScope.drawDataLine(
    positions: List<Offset>
) {
    if (positions.size < 2) return
    
    val path = Path()
    val lineColor = shoeCycleOrange
    
    positions.forEachIndexed { index, position ->
        if (index == 0) {
            path.moveTo(position.x, position.y)
        } else {
            path.lineTo(position.x, position.y)
        }
    }
    
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(
            width = 3f,
            cap = StrokeCap.Round
        )
    )
}

private fun DrawScope.drawDataPoints(
    positions: List<Offset>,
    selectedIndex: Int?
) {
    val normalColor = shoeCycleGreen
    val selectedColor = shoeCycleBlue
    val normalRadius = 4.dp.toPx()
    val selectedRadius = 6.dp.toPx()
    
    positions.forEachIndexed { index, position ->
        val isSelected = index == selectedIndex
        
        // Draw outer ring for selected point
        if (isSelected) {
            drawCircle(
                color = selectedColor.copy(alpha = 0.3f),
                radius = selectedRadius * 1.5f,
                center = position
            )
        }
        
        // Draw main point
        drawCircle(
            color = if (isSelected) selectedColor else normalColor,
            radius = if (isSelected) selectedRadius else normalRadius,
            center = position
        )
    }
}

// Extension function to calculate distance between two offsets
private fun Offset.distanceToOrigin(): Float {
    return kotlin.math.sqrt(x * x + y * y)
}