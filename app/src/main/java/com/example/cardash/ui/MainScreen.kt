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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import android.content.pm.PackageManager
import android.bluetooth.BluetoothDevice
import com.example.cardash.services.obd.BluetoothManager
import com.example.cardash.CarDashApp // Import CarDashApp
import com.example.cardash.ui.graphs.GraphScreen
import com.example.cardash.ui.metrics.MetricGridScreen
import com.example.cardash.ui.metrics.MetricViewModel
import com.example.cardash.ui.metrics.MetricViewModelFactory // Import ViewModelFactory
import com.example.cardash.ui.theme.CarDashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onPermissionNeeded: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as CarDashApp
    val factory = MetricViewModelFactory(app.obdService)
    val viewModel: MetricViewModel = viewModel(factory = factory)
    val bluetoothManager = app.bluetoothManager

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Metrics", "Graphs")
    val connectionState by viewModel.connectionState.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }
    val devices by remember { mutableStateOf(bluetoothManager.getPairedDevices()) }

    // Check permissions when trying to connect
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!bluetoothManager.isBluetoothEnabled() || 
            BluetoothManager.REQUIRED_PERMISSIONS.any { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }) {
            onPermissionNeeded()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showDeviceDialog) {
            DeviceSelectionDialog(
                devices = devices,
                onDeviceSelected = { device ->
                    viewModel.connectToDevice(device.address)
                    showDeviceDialog = false
                },
                onDismiss = { showDeviceDialog = false }
            )
        }
        
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
            ConnectionStatusText(
                connectionState = connectionState,
                onClick = { showDeviceDialog = true }
            )
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
fun ConnectionStatusText(
    connectionState: MetricViewModel.ConnectionState,
    onClick: () -> Unit = {}
) {
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
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable(onClick = onClick)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionDialog(
    devices: Set<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select OBD2 Adapter") },
        text = {
            LazyColumn {
                items(devices.toList().toTypedArray()) { device ->
                    Text(
                        text = device.name ?: "Unknown Device (${device.address})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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
