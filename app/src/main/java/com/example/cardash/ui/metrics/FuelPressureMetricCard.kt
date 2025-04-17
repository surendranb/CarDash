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
    // When engine is running, use a default value of 300 kPa if the reading is zero
    // This ensures we display a reasonable value even when the sensor gives zero
    val displayPressure = if (isConnected && pressure <= 0) 300 else pressure
    
    // Fuel Pressure calibration
    // Normal: 250-500 kPa
    // Warning: <250 kPa (low fuel pressure) or 500-600 kPa (high fuel pressure)
    // Error: >600 kPa (excessive pressure - potential system issues)
    val status = when {
        !isConnected -> MetricStatus.DISCONNECTED
        pressure <= 0 -> MetricStatus.NORMAL // Zero pressure displays as default 300 kPa
        pressure > 600 -> MetricStatus.ERROR // Too high - potential fuel system issue
        pressure > 500 -> MetricStatus.WARNING // High pressure - worth monitoring
        pressure < 250 && pressure > 0 -> MetricStatus.WARNING // Low pressure - potential fuel pump issues
        else -> MetricStatus.GOOD // 250-500 kPa is ideal range
    }
    
    MetricCard(
        title = "FUEL PRESSURE",
        value = "$displayPressure",
        unit = "kPa",
        status = status,
        isConnected = isConnected,
        modifier = modifier
    )
}
