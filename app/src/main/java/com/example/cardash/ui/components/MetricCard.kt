package com.example.cardash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cardash.ui.theme.Error
import com.example.cardash.ui.theme.Success
import com.example.cardash.ui.theme.Warning

enum class MetricStatus {
    GOOD, WARNING, ERROR, NORMAL
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    status: MetricStatus = MetricStatus.NORMAL,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        MetricStatus.GOOD -> Success
        MetricStatus.WARNING -> Warning
        MetricStatus.ERROR -> Error
        MetricStatus.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
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
    }
}
