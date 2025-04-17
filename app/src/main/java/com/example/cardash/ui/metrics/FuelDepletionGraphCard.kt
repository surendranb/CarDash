package com.example.cardash.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardash.ui.theme.Success
import com.example.cardash.ui.theme.Warning

/**
 * A card displaying the fuel level over a 7-day timeline with enhanced visualization
 */
@Composable
fun FuelDepletionGraphCard(
    fuelLevelHistory: List<Int>,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardPadding = 16.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with current value
            val currentLevel = fuelLevelHistory.lastOrNull() ?: 0
            Text(
                text = "FUEL LEVEL: $currentLevel%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Graph area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (fuelLevelHistory.size < 2) {
                    // Not enough data
                    Text(
                        text = "Not enough data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Draw graph with enhanced visualization
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val width = size.width
                        val height = size.height
                        
                        // Use all available data points, but cap at 7 days
                        val dataPoints = fuelLevelHistory.takeLast(7)
                        
                        // Find min and max values for better scaling
                        val minValue = dataPoints.minOrNull() ?: 0
                        val maxValue = dataPoints.maxOrNull() ?: 100
                        
                        // Adjust the range to enhance visibility of changes
                        // Use a reasonable minimum range of 20% to show small changes
                        val effectiveMin = (minValue - 5).coerceAtLeast(0)
                        val effectiveMax = (maxValue + 5).coerceAtMost(100).coerceAtLeast(effectiveMin + 20)
                        
                        println("Fuel graph range: $effectiveMin to $effectiveMax (actual data: $minValue to $maxValue)")
                        
                        // Start from bottom left - (old data)
                        val baselineY = height * 0.95f // Small margin from bottom
                        val topY = height * 0.05f // Small margin from top
                        val dataRange = dataPoints.size - 1
                        
                        if (dataRange <= 0) return@Canvas
                        
                        // Draw baseline at effective min level
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, baselineY),
                            end = Offset(width, baselineY),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Draw topline at effective max level
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, topY),
                            end = Offset(width, topY),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Function to convert fuel level to Y position with enhanced scaling
                        fun fuelLevelToY(level: Int): Float {
                            val range = effectiveMax - effectiveMin
                            if (range <= 0) return baselineY
                            val relativeLevel = (level - effectiveMin).toFloat() / range
                            return baselineY - (baselineY - topY) * relativeLevel
                        }
                        
                        // Draw the line segments
                        val graphPath = Path()
                        val firstPoint = dataPoints.first()
                        graphPath.moveTo(0f, fuelLevelToY(firstPoint))
                        
                        for (i in 0 until dataPoints.size) {
                            val x = width * i / dataRange.coerceAtLeast(1)
                            val y = fuelLevelToY(dataPoints[i])
                            graphPath.lineTo(x, y)
                        }
                        
                        // Draw main graph line
                        drawPath(
                            path = graphPath,
                            color = Success,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                        
                        // Draw horizontal guidelines (show actual values, not just percentages)
                        val guidelines = 4
                        for (i in 0..guidelines) {
                            val value = effectiveMin + (effectiveMax - effectiveMin) * i / guidelines
                            val y = fuelLevelToY(value.toInt())
                            
                            // Draw guideline
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        
                        // Draw data points
                        for (i in 0 until dataPoints.size) {
                            val x = width * i / dataRange.coerceAtLeast(1)
                            val y = fuelLevelToY(dataPoints[i])
                            
                            // Draw the point
                            drawCircle(
                                color = Success,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                    
                    // Add data point labels as overlays
                    if (fuelLevelHistory.size >= 2) {
                        val dataPoints = fuelLevelHistory.takeLast(7)
                        val minValue = dataPoints.minOrNull() ?: 0
                        val maxValue = dataPoints.maxOrNull() ?: 100
                        val effectiveMin = (minValue - 5).coerceAtLeast(0)
                        val effectiveMax = (maxValue + 5).coerceAtMost(100).coerceAtLeast(effectiveMin + 20)
                        
                        // Guide values (displayed on left side)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(
                                    text = "${effectiveMax}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Text(
                                    text = "${effectiveMin}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Data point labels
            if (fuelLevelHistory.size >= 2) {
                val dataPoints = fuelLevelHistory.takeLast(7)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dataPoints.forEach { level ->
                        Text(
                            text = "$level%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(24.dp)
                        )
                    }
                }
            }
            
            // Legend/labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "7 days ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Show min/max difference if we have data
            if (fuelLevelHistory.size >= 2) {
                val min = fuelLevelHistory.minOrNull() ?: 0
                val max = fuelLevelHistory.maxOrNull() ?: 0
                
                if (min != max) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Range: $min% - $max% (${max - min}% change)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}