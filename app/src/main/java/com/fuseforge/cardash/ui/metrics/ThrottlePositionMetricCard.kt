package com.fuseforge.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricStatus

@Composable
fun ThrottlePositionMetricCard(
    position: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "THROTTLE",
        value = "$position",
        unit = "%",
        status = when {
            position > 90 -> MetricStatus.WARNING
            position > 70 -> MetricStatus.NORMAL
            else -> MetricStatus.NORMAL
        },
        modifier = modifier
    )
}
