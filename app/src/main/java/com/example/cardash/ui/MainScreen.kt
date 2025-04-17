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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.ExperimentalTextApi
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
import com.example.cardash.ui.settings.SettingsDialog
import com.example.cardash.ui.settings.TabSettings
import com.example.cardash.ui.settings.TabType
import com.example.cardash.ui.theme.CarDashTheme
import com.example.cardash.ui.theme.Success
import com.example.cardash.ui.theme.Warning
import com.example.cardash.ui.theme.Error as ThemeError
import com.example.cardash.ui.theme.Neutral

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun MainScreen(
    onPermissionNeeded: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as CarDashApp
    val factory = MetricViewModelFactory(app.obdService, app.obdServiceDiagnostics)
    val viewModel: MetricViewModel = viewModel(factory = factory)
    val bluetoothManager = app.bluetoothManager
    
    // Tab Settings
    val tabSettings = remember { TabSettings() }
    val context = LocalContext.current
    
    // Load tab settings when the app starts
    LaunchedEffect(Unit) {
        tabSettings.loadSettings(context)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val devices by remember { mutableStateOf(bluetoothManager.getPairedDevices()) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    val engineRunning by viewModel.engineRunning.collectAsState()
    
    // Create tabs list based on visibility settings
    val tabs = mutableListOf<String>()
    tabs.add("Metrics") // Always included
    if (tabSettings.showGraphsTab) tabs.add("Trends")
    if (tabSettings.showDiagnosticsTab) tabs.add("Diagnostics")
    if (tabSettings.showHistoryTab) tabs.add("History")
    
    // Ensure selected tab is valid after settings change
    if (selectedTab >= tabs.size) {
        selectedTab = 0
    }

    // Check permissions when trying to connect
    LaunchedEffect(Unit) {
        if (!bluetoothManager.isBluetoothEnabled() || 
            BluetoothManager.REQUIRED_PERMISSIONS.any { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }) {
            onPermissionNeeded()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Device selection dialog
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
        
        // Settings dialog
        if (showSettingsDialog) {
            SettingsDialog(
                tabSettings = tabSettings,
                onDismiss = { 
                    showSettingsDialog = false
                    // Save settings when dialog is closed
                    tabSettings.saveSettings(context)
                }
            )
        }
        
        // Improved App header with proper spacing and status bar avoidance
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars), // Use WindowInsets for proper status bar padding
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // App title with settings button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // App title
                    Text(
                        text = "CarDash",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // OBD Connection button
                    IconButton(
                        onClick = { showDeviceDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        // Use different colors based on connection state
                        val iconTint = when (connectionState) {
                            is MetricViewModel.ConnectionState.Connected -> Success
                            is MetricViewModel.ConnectionState.Connecting -> Warning
                            is MetricViewModel.ConnectionState.Failed -> ThemeError
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        
                        // Use power button graphic
                        Icon(
                            painter = painterResource(id = com.example.cardash.R.drawable.ic_power),
                            contentDescription = "Connect to OBD",
                            tint = iconTint
                        )
                    }
                }
            }
        }

        // Tabs with racing-inspired styling
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            divider = {
                Divider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            },
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
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
                    modifier = Modifier.padding(vertical = 14.dp) // Increased vertical padding
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
            // Get the tab name at the selected index
            val currentTab = if (selectedTab < tabs.size) tabs[selectedTab] else "Metrics"
            
            when (currentTab) {
                "Metrics" -> MetricGridScreen(removeEngineStatus = true)
                "Trends" -> GraphScreen()
                "Diagnostics" -> LogViewerScreen(onBackPressed = { selectedTab = 0 })
                "History" -> HistoryScreen()
                else -> MetricGridScreen(removeEngineStatus = true) // Default fallback
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
            .clip(RoundedCornerShape(12.dp)), // Increased corner radius
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Only OBD Connection Status (moved to center)
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = connectionColor,
                textAlign = TextAlign.Center
            )
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