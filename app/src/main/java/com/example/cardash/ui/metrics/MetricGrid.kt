package com.example.cardash.ui.metrics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cardash.ui.components.MetricCard

@Composable
fun MetricGridScreen() {
    val metrics = listOf(
        MetricData("RPM", "2500", "rpm", Color.Green),
        MetricData("Speed", "65", "km/h", Color.Yellow),
        MetricData("Coolant", "92", "°C", Color.Red),
        MetricData("Voltage", "13.8", "V", Color.Green)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(16.dp)
    ) {
        items(metrics) { metric ->
            MetricCard(
                title = metric.title,
                value = metric.value,
                unit = metric.unit,
                statusColor = metric.color
            )
        }
    }
}

data class MetricData(
    val title: String,
    val value: String,
    val unit: String,
    val color: Color
)
