package com.example.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun FuelPressureMetricCard(
    pressure: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "FUEL PRESS",
        value = "$pressure",
        unit = "kPa",
        status = when {
            pressure > 500 -> MetricStatus.WARNING
            pressure > 400 -> MetricStatus.NORMAL
            else -> MetricStatus.NORMAL
        },
        modifier = modifier
    )
}
