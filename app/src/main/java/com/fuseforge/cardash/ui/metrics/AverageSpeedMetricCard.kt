package com.fuseforge.cardash.ui.metrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricStatus
import com.fuseforge.cardash.ui.theme.Success

@Composable
fun AverageSpeedMetricCard(
    averageSpeed: Int,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Simplified status logic
    val status = when {
        averageSpeed > 120 -> MetricStatus.WARNING // High average speed warning
        averageSpeed > 160 -> MetricStatus.ERROR   // Very high average speed
        else -> MetricStatus.GOOD
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "AVG SPEED",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Value
            Text(
                text = "$averageSpeed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = when(status) {
                    MetricStatus.GOOD -> Success
                    MetricStatus.WARNING -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    MetricStatus.ERROR -> MaterialTheme.colorScheme.error
                    MetricStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    MetricStatus.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Unit
            Text(
                text = "km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}