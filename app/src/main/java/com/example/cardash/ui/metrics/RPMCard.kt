package com.example.cardash.ui.metrics

import androidx.compose.runtime.Composable
import com.example.cardash.ui.components.MetricCard

@Composable
fun RPMCard(rpm: Int) {
    MetricCard(
        title = "RPM",
        value = rpm.toString(),
        unit = "rpm"
    )
}
