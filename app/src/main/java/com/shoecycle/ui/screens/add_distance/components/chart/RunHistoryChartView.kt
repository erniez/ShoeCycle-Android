package com.shoecycle.ui.screens.add_distance.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoecycle.ui.screens.add_distance.utils.WeeklyCollatedNew
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Basic Run History Chart component
 * Displays weekly running distance data as a simple line chart
 */
@Composable
fun RunHistoryChartView(
    chartData: List<WeeklyCollatedNew>,
    modifier: Modifier = Modifier
) {
    // VSI pattern state management
    var state by remember { mutableStateOf(RunHistoryChartState()) }
    val interactor = remember { RunHistoryChartInteractor() }
    
    // Update state when data changes
    LaunchedEffect(chartData) {
        state = interactor.handle(state, RunHistoryChartInteractor.Action.DataUpdated(chartData))
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1C1C1E) // iOS dark background
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF007AFF) // iOS blue
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
            ChartCanvas(
                data = state.chartData,
                maxDistance = state.maxDistance,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ChartCanvas(
    data: List<WeeklyCollatedNew>,
    maxDistance: Double,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val dateFormatter = remember { SimpleDateFormat("M/d", Locale.US) }
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40f
        
        // Calculate chart area
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw axes
        drawAxes(
            padding = padding,
            chartWidth = chartWidth,
            chartHeight = chartHeight
        )
        
        // Draw Y-axis labels (distance)
        drawYAxisLabels(
            maxDistance = maxDistance,
            padding = padding,
            chartHeight = chartHeight,
            textMeasurer = textMeasurer
        )
        
        // Draw X-axis labels (dates)
        drawXAxisLabels(
            data = data,
            padding = padding,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            dateFormatter = dateFormatter,
            textMeasurer = textMeasurer
        )
        
        // Draw data line
        if (data.size >= 2) {
            drawDataLine(
                data = data,
                maxDistance = maxDistance,
                padding = padding,
                chartWidth = chartWidth,
                chartHeight = chartHeight
            )
        }
        
        // Draw data points
        drawDataPoints(
            data = data,
            maxDistance = maxDistance,
            padding = padding,
            chartWidth = chartWidth,
            chartHeight = chartHeight
        )
    }
}

private fun DrawScope.drawAxes(
    padding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val axisColor = Color.Gray
    val strokeWidth = 2f
    
    // Y-axis
    drawLine(
        color = axisColor,
        start = Offset(padding, padding),
        end = Offset(padding, padding + chartHeight),
        strokeWidth = strokeWidth
    )
    
    // X-axis
    drawLine(
        color = axisColor,
        start = Offset(padding, padding + chartHeight),
        end = Offset(padding + chartWidth, padding + chartHeight),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawYAxisLabels(
    maxDistance: Double,
    padding: Float,
    chartHeight: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val labelCount = 5
    val textStyle = TextStyle(
        color = Color.Gray,
        fontSize = 10.sp
    )
    
    for (i in 0..labelCount) {
        val value = maxDistance * (labelCount - i) / labelCount
        val y = padding + (chartHeight * i / labelCount)
        
        // Draw grid line
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(padding, y),
            end = Offset(padding + size.width - padding * 2, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
        )
        
        // Draw label
        val text = if (value == 0.0) "0" else "%.1f".format(value)
        val textResult = textMeasurer.measure(text, textStyle)
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                x = padding - textResult.size.width - 8f,
                y = y - textResult.size.height / 2
            )
        )
    }
}

private fun DrawScope.drawXAxisLabels(
    data: List<WeeklyCollatedNew>,
    padding: Float,
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
            val x = padding + (chartWidth * index / (data.size - 1).coerceAtLeast(1))
            val y = padding + chartHeight + 8f
            
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
    data: List<WeeklyCollatedNew>,
    maxDistance: Double,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val path = Path()
    val lineColor = Color(0xFFFF9500) // Orange
    
    data.forEachIndexed { index, point ->
        val x = padding + (chartWidth * index / (data.size - 1).coerceAtLeast(1))
        val yRatio = if (maxDistance > 0) point.runDistance / maxDistance else 0.0
        val y = padding + chartHeight * (1 - yRatio).toFloat()
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
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
    data: List<WeeklyCollatedNew>,
    maxDistance: Double,
    padding: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val pointColor = Color(0xFF32D74B) // Green
    val pointRadius = 4.dp.toPx()
    
    data.forEachIndexed { index, point ->
        val x = padding + (chartWidth * index / (data.size - 1).coerceAtLeast(1))
        val yRatio = if (maxDistance > 0) point.runDistance / maxDistance else 0.0
        val y = padding + chartHeight * (1 - yRatio).toFloat()
        
        drawCircle(
            color = pointColor,
            radius = pointRadius,
            center = Offset(x, y)
        )
    }
}