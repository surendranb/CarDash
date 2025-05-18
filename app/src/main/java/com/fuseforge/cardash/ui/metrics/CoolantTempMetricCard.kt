package com.fuseforge.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricStatus

@Composable
fun CoolantTempMetricCard(
    temp: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "COOLANT",
        value = "$temp",
        unit = "°C",
        status = when {
            temp > 110 -> MetricStatus.ERROR
            temp > 95 -> MetricStatus.WARNING
            else -> MetricStatus.NORMAL
        },
        modifier = modifier
    )
}
