package com.fuseforge.cardash.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt

@Composable
fun LineChartCanvas(
    dataPoints: List<GraphDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    yMin: Double? = null,
    yMax: Double? = null
) {
    if (dataPoints.isEmpty()) return
    
    // Get the min and max values
    val dataMin = dataPoints.minOf { it.value }
    val dataMax = dataPoints.maxOf { it.value }
    
    // Determine bounds (use provided, or calculate with padding)
    var minValue = yMin ?: dataMin
    var maxValue = yMax ?: dataMax
    
    // If auto-scaling and the line is perfectly flat, pad it so it draws in the middle
    if (yMin == null && yMax == null) {
        if (minValue == maxValue) {
            if (minValue == 0.0) {
                maxValue = 10.0
            } else {
                minValue -= Math.abs(minValue * 0.1)
                maxValue += Math.abs(maxValue * 0.1)
            }
        } else {
            // Add a small 5% visual margin so points don't touch the very top/bottom edges
            val margin = (maxValue - minValue) * 0.05
            minValue -= margin
            maxValue += margin
        }
    }
    
    val valueRange = (maxValue - minValue).coerceAtLeast(0.1) // Avoid division by zero
    
    // Get the min and max timestamps
    val minTimestamp = dataPoints.minOf { it.timestamp.time }
    val maxTimestamp = dataPoints.maxOf { it.timestamp.time }
    val timeRange = (maxTimestamp - minTimestamp).coerceAtLeast(1L) // Avoid division by zero
    
    // Padding for the chart
    val padding = 16.dp
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        val height = size.height
        val width = size.width
        
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1f
        )
        
        drawLine(
            color = Color.Gray,
            start = Offset(0f, 0f),
            end = Offset(0f, height),
            strokeWidth = 1f
        )
        
        // Calculate points for the line chart
        val points = dataPoints.map { point ->
            val x = ((point.timestamp.time - minTimestamp) / timeRange.toFloat() * width).toFloat()
            val y = (height - ((point.value - minValue) / valueRange * height)).toFloat()
            Offset(x, y)
        }
        
        // Draw the line connecting points
        if (points.size > 1) {
            // Create a path for the line
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }
            
            // Draw the path
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = point
                )
            }
        }
        
        // Draw grid lines
        val numHorizontalLines = 5
        val numVerticalLines = 5
        
        // Horizontal grid lines
        for (i in 0..numHorizontalLines) {
            val y = height - (height * i / numHorizontalLines)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5f
            )
            
            // Draw value markers
            val value = minValue + (i.toFloat() / numHorizontalLines) * valueRange
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", value),
                10f,
                y - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )
        }
        
        // Vertical grid lines
        for (i in 0..numVerticalLines) {
            val x = width * i / numVerticalLines
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 0.5f
            )
            
            // Draw time markers
            if (i > 0) {
                val timeFraction = 1.0 - (i.toDouble() / numVerticalLines)
                val timeOffsetMillis = (timeRange * timeFraction).toLong()
                val minutesAgo = timeOffsetMillis / 60000
                
                val label = if (minutesAgo == 0L) "Now" else "-${minutesAgo}m"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x - 20f,
                    height - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 30f
                    }
                )
            }
        }
    }
}