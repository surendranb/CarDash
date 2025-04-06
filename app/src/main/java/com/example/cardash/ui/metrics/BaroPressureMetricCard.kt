package com.example.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun BaroPressureMetricCard(
    pressure: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "BARO PRESS",
        value = "$pressure",
        unit = "kPa",
        status = MetricStatus.NORMAL, // Atmospheric pressure doesn't indicate problems
        modifier = modifier
    )
}
