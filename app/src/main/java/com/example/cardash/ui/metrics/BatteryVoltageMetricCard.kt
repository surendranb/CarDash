package com.example.cardash.ui.metrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardash.ui.theme.CarDashTheme

@Composable
fun BatteryVoltageMetricCard(
    modifier: Modifier = Modifier
) {
    val viewModel: MetricViewModel = viewModel()
    val voltage by viewModel.batteryVoltage.collectAsState(initial = 0f)

    ElevatedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Battery Voltage",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
                        
            val voltageText = if (voltage <= 0f) "N/A" else "%.1fV".format(voltage)
                
            Text(
                text = voltageText,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryVoltageMetricCardPreview() {
    CarDashTheme {
        BatteryVoltageMetricCard()
    }
}
