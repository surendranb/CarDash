package com.example.cardash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.cardash.ui.theme.Error as ThemeError
import com.example.cardash.ui.theme.Success
import com.example.cardash.ui.theme.Warning
import com.example.cardash.ui.theme.Neutral
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

enum class MetricStatus {
    GOOD, WARNING, ERROR, NORMAL, DISCONNECTED
}

enum class MetricCardType {
    VALUE, // Simple numerical value
    PROGRESS, // Percentage-based metrics
    RANGE // Values with min/max range
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    status: MetricStatus = MetricStatus.NORMAL,
    type: MetricCardType = MetricCardType.VALUE,
    currentPercentage: Float = 0f, // For PROGRESS type
    minValue: Float = 0f, // For RANGE type
    maxValue: Float = 100f, // For RANGE type
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        !isConnected -> Neutral
        status == MetricStatus.GOOD -> Success
        status == MetricStatus.WARNING -> Warning
        status == MetricStatus.ERROR -> ThemeError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Check if we're on a tablet
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    // Adjust padding and sizing based on device type
    val cardPadding = if (isTablet) 10.dp else 16.dp
    val valueFontSize = if (isTablet) 28.sp else 32.sp
    val spacerHeight = if (isTablet) 8.dp else 12.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isTablet) 100.dp else 120.dp),
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
            // Title area
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(spacerHeight))
            
            // Metric display based on type
            when (type) {
                MetricCardType.VALUE -> ValueMetric(value, unit, statusColor, valueFontSize)
                MetricCardType.PROGRESS -> ProgressMetric(value, unit, statusColor, currentPercentage, valueFontSize, spacerHeight)
                MetricCardType.RANGE -> RangeMetric(value, unit, statusColor, minValue, maxValue, currentPercentage, valueFontSize, spacerHeight)
            }
        }
    }
}

@Composable
fun ValueMetric(
    value: String,
    unit: String,
    statusColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = statusColor
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
fun ProgressMetric(
    value: String,
    unit: String,
    statusColor: Color,
    percentage: Float,
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    spacerHeight: Dp = 12.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Value display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = statusColor
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(spacerHeight))
        
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp as Dp) // Added explicit Dp cast
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(statusColor)
            )
        }
    }
}

@Composable
fun RangeMetric(
    value: String,
    unit: String,
    statusColor: Color,
    minValue: Float,
    maxValue: Float,
    currentValue: Float,
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    spacerHeight: Dp = 12.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Value display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = statusColor
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(spacerHeight))
        
        // Range indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp as Dp) // Added explicit Dp cast
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            // Calculate position in range
            val range = maxValue - minValue
            val position = if (range > 0) {
                ((currentValue - minValue) / range).coerceIn(0f, 1f)
            } else {
                0.5f
            }
            
            // Indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth(position)
                    .fillMaxHeight()
                    .background(statusColor)
            )
            
            // Current value marker
            Box(
                modifier = Modifier
                    .offset(x = (position * 100).coerceIn(0f, 100f).dp - 4.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(1.dp, statusColor, RoundedCornerShape(4.dp))
            )
        }
        
        // Min/Max labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minValue.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Text(
                text = maxValue.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}