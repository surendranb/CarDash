package com.example.cardash.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun FuelPressureMetricCard(
    pressure: Int,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val status = when {
        !isConnected -> MetricStatus.DISCONNECTED
        pressure <= 0 -> MetricStatus.NORMAL // Changed from UNKNOWN to NORMAL
        pressure > 600 -> MetricStatus.ERROR
        pressure > 500 -> MetricStatus.WARNING
        pressure < 250 && pressure > 0 -> MetricStatus.WARNING // Low pressure is also concerning
        else -> MetricStatus.GOOD
    }
    
    MetricCard(
        title = "FUEL PRESS",
        value = "$pressure",
        unit = "kPa",
        status = status,
        isConnected = isConnected,
        modifier = modifier
    )
}
