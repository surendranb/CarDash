package com.example.cardash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import android.content.pm.PackageManager
import android.bluetooth.BluetoothDevice
import com.example.cardash.services.obd.BluetoothManager
import com.example.cardash.CarDashApp
import com.example.cardash.ui.diagnostics.LogViewerScreen
import com.example.cardash.ui.graphs.GraphScreen
import com.example.cardash.ui.history.HistoryScreen
import com.example.cardash.ui.metrics.MetricGridScreen
import com.example.cardash.ui.metrics.MetricViewModel
import com.example.cardash.ui.metrics.MetricViewModelFactory
import com.example.cardash.ui.theme.CarDashTheme
import com.example.cardash.ui.theme.Success
import com.example.cardash.ui.theme.Warning
import com.example.cardash.ui.theme.Error as ThemeError
import com.example.cardash.ui.theme.Neutral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onPermissionNeeded: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as CarDashApp
    val factory = MetricViewModelFactory(app.obdService, app.obdServiceDiagnostics)
    val viewModel: MetricViewModel = viewModel(factory = factory)
    val bluetoothManager = app.bluetoothManager

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Metrics", "Graphs", "History", "Diagnostics")
    val connectionState by viewModel.connectionState.collectAsState()
    val engineRunning by viewModel.engineRunning.collectAsState()
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
        
        // Improved App header with proper spacing and status bar avoidance
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars), // Use WindowInsets for proper status bar padding
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // App title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CarDash",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Start
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Combined status bar
                CombinedStatusBar(
                    connectionState = connectionState,
                    engineRunning = engineRunning,
                    onClick = { showDeviceDialog = true }
                )
            }
        }

        // Tabs with improved styling
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { 
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        ) 
                    },
                    modifier = Modifier.padding(vertical = 12.dp) // Added vertical padding
                )
            }
        }

        // Content switch - ensure we have space for OS navigation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp) // Add padding for OS navigation bar
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)) // Handle edge-to-edge
        ) {
            when (selectedTab) {
                0 -> MetricGridScreen(removeEngineStatus = true)
                1 -> GraphScreen()
                2 -> HistoryScreen()
                3 -> LogViewerScreen(onBackPressed = { selectedTab = 0 })
            }
        }
    }
}

@Composable
fun CombinedStatusBar(
    connectionState: MetricViewModel.ConnectionState,
    engineRunning: Boolean,
    onClick: () -> Unit = {}
) {
    val isConnected = connectionState is MetricViewModel.ConnectionState.Connected
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OBD Connection Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val (connectionStatus, connectionColor) = when(connectionState) {
                    is MetricViewModel.ConnectionState.Connected -> Pair(
                        "● OBD Connected",
                        Success
                    )
                    is MetricViewModel.ConnectionState.Connecting -> Pair(
                        "◌ OBD Connecting...",
                        Warning
                    )
                    is MetricViewModel.ConnectionState.Disconnected -> Pair(
                        "○ OBD Disconnected",
                        Neutral
                    )
                    is MetricViewModel.ConnectionState.Failed -> Pair(
                        "✕ OBD Connection Failed",
                        ThemeError
                    )
                }
                
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = connectionColor,
                    textAlign = TextAlign.Start
                )
            }
            
            // Engine Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val engineStatus: String
                val engineColor: androidx.compose.ui.graphics.Color
                
                if (!isConnected) {
                    engineStatus = "○ Engine Status Unknown"
                    engineColor = Neutral
                } else if (engineRunning) {
                    engineStatus = "● Engine Running"
                    engineColor = Success
                } else {
                    engineStatus = "○ Engine Off"
                    engineColor = Warning
                }
                
                Text(
                    text = engineStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = engineColor,
                    textAlign = TextAlign.End
                )
            }
        }
    }
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
        title = { 
            Text(
                text = "Select OBD2 Adapter", 
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            if (devices.isEmpty()) {
                Text(
                    text = "No paired devices found. Please pair an OBD2 adapter in Bluetooth settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn {
                    items(devices.toList().toTypedArray()) { device ->
                        Text(
                            text = device.name ?: "Unknown Device (${device.address})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 16.dp, horizontal = 8.dp), // Improved padding
                            textAlign = TextAlign.Start
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    textAlign = TextAlign.Center
                )
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