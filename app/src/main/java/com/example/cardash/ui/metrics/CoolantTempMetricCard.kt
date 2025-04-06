package com.example.cardash.ui.metrics

import androidx.compose.runtime.Composable
import com.example.cardash.ui.components.MetricCard

@Composable
fun CoolantTempMetricCard(temp: Int) {
    MetricCard(
        title = "Coolant Temp",
        value = "$temp",
        unit = "°C"
    )
}
