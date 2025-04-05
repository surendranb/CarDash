package com.example.cardash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardash.CarDashApp // Import CarDashApp
import com.example.cardash.ui.graphs.GraphScreen
import com.example.cardash.ui.metrics.MetricGridScreen
import com.example.cardash.ui.metrics.MetricViewModel
import com.example.cardash.ui.metrics.MetricViewModelFactory // Import ViewModelFactory
import com.example.cardash.ui.theme.CarDashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val app = LocalContext.current.applicationContext as CarDashApp // Get CarDashApp instance
    val factory = MetricViewModelFactory(app.obdService) // Create ViewModelFactory
    val viewModel: MetricViewModel = viewModel(factory = factory) // Use factory to create ViewModel

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Metrics", "Graphs")
    val connectionState by viewModel.connectionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CarDash",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            ConnectionStatusText(connectionState = connectionState)
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content switch
        when (selectedTab) {
            0 -> MetricGridScreen()
            1 -> GraphScreen()
        }
    }
}

@Composable
fun ConnectionStatusText(connectionState: MetricViewModel.ConnectionState) {
    val statusText = when (connectionState) {
        is MetricViewModel.ConnectionState.Connected -> "Connected"
        is MetricViewModel.ConnectionState.Connecting -> "Connecting..."
        is MetricViewModel.ConnectionState.Disconnected -> "Disconnected"
        is MetricViewModel.ConnectionState.Failed -> "Error"
    }

    val statusColor = when (connectionState) {
        is MetricViewModel.ConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is MetricViewModel.ConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
        is MetricViewModel.ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
        is MetricViewModel.ConnectionState.Failed -> MaterialTheme.colorScheme.error
    }

    Text(
        text = statusText,
        color = statusColor,
        modifier = Modifier.padding(start = 8.dp)
    )
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CarDashTheme {
        Surface {
            MainScreen()
        }
    }
}
