package com.fuseforge.cardash.ui.metrics

import androidx.compose.runtime.Composable
import com.fuseforge.cardash.ui.components.MetricCard

@Composable
fun RPMCard(rpm: Int) {
    MetricCard(
        title = "RPM",
        value = rpm.toString(),
        unit = "rpm"
    )
}
