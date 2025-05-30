package com.fuseforge.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricStatus

@Composable
fun FuelLevelMetricCard(
    level: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "FUEL",
        value = "$level",
        unit = "%",
        status = when {
            level < 10 -> MetricStatus.ERROR
            level < 25 -> MetricStatus.WARNING
            else -> MetricStatus.NORMAL
        },
        modifier = modifier
    )
}
