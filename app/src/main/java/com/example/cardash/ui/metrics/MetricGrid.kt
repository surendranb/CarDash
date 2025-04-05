package com.example.cardash.ui.metrics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun MetricGridScreen() {
    val metrics = listOf(
        MetricData("RPM", "2500", "rpm", MetricStatus.GOOD),
        MetricData("Speed", "65", "km/h", MetricStatus.WARNING),
        MetricData("Coolant", "92", "°C", MetricStatus.ERROR),
        MetricData("Voltage", "13.8", "V", MetricStatus.GOOD)
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
                status = metric.status
            )
        }
    }
}

data class MetricData(
    val title: String,
    val value: String,
    val unit: String,
    val status: MetricStatus
)
