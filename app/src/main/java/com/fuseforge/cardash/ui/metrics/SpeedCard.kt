package com.fuseforge.cardash.ui.metrics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricStatus

@Composable
fun SpeedCard(
    speed: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "SPEED", 
        value = "$speed",
        unit = "km/h",
        status = when {
            speed > 120 -> MetricStatus.ERROR
            speed > 80 -> MetricStatus.WARNING 
            else -> MetricStatus.NORMAL
        },
        modifier = modifier
    )
}
