package com.example.cardash.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardash.data.db.OBDDataType
import com.example.cardash.data.db.OBDLogEntry
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBackPressed: () -> Unit,
    viewModel: DiagnosticViewModel = viewModel(factory = DiagnosticViewModelFactory(LocalContext.current))
) {
    val logs by viewModel.logs.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val selectedDataTypes by viewModel.selectedDataTypes.collectAsState()
    val showOnlyErrors by viewModel.showOnlyErrors.collectAsState()
    
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSessionMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.List, contentDescription = "Filter")
                    }
                    
                    // Session selector
                    Box {
                        IconButton(onClick = { showSessionMenu = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedSession?.let { 
                                        it.deviceName ?: it.deviceAddress 
                                    } ?: "All Sessions",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Session")
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showSessionMenu,
                            onDismissRequest = { showSessionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sessions") },
                                onClick = {
                                    viewModel.selectSession(null)
                                    showSessionMenu = false
                                }
                            )
                            
                            sessions.forEach { session ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            session.deviceName ?: session.deviceAddress,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectSession(session)
                                        showSessionMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Options menu
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Logs") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    viewModel.exportLogs()
                                    showOptionsMenu = false
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Clear Logs") },
                                onClick = {
                                    viewModel.clearLogs()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Logs cleared")
                                    }
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Filter dialog
            if (showFilterMenu) {
                FilterMenu(
                    selectedDataTypes = selectedDataTypes,
                    showOnlyErrors = showOnlyErrors,
                    onDataTypeSelected = { dataType, selected ->
                        viewModel.toggleDataTypeFilter(dataType, selected)
                    },
                    onShowOnlyErrorsChanged = {
                        viewModel.toggleShowOnlyErrors(it)
                    },
                    onDismiss = {
                        showFilterMenu = false
                    }
                )
            }
            
            // Log entries
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No logs found for the selected criteria")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.material3.Button(
                            onClick = { viewModel.generateTestData() }
                        ) {
                            Text("Generate Test Data")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        LogEntryCard(log = log, viewModel = viewModel)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterMenu(
    selectedDataTypes: Set<OBDDataType>,
    showOnlyErrors: Boolean,
    onDataTypeSelected: (OBDDataType, Boolean) -> Unit,
    onShowOnlyErrorsChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowOnlyErrorsChanged(!showOnlyErrors) }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = showOnlyErrors,
                    onCheckedChange = { onShowOnlyErrorsChanged(it) }
                )
                Text("Show only errors")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Data Types",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Data type filters - display in a grid
            Column {
                OBDDataType.values().forEach { dataType ->
                    // Skip UNKNOWN type as it's not a user-facing category
                    if (dataType != OBDDataType.UNKNOWN) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDataTypeSelected(dataType, !selectedDataTypes.contains(dataType)) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedDataTypes.contains(dataType),
                                onCheckedChange = { onDataTypeSelected(dataType, it) }
                            )
                            Text(dataType.name)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LogEntryCard(
    log: OBDLogEntry,
    viewModel: DiagnosticViewModel
) {
    val backgroundColor = if (log.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (log.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header row with timestamp and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Data type badge
                Box(
                    modifier = Modifier
                        .background(
                            color = getColorForDataType(log.dataType),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.dataType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                
                // Timestamp
                Text(
                    text = viewModel.formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Command
            Text(
                text = "Command: ${log.command}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            
            // Error or response
            if (log.isError) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = log.errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Raw response
                if (log.rawResponse.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Response: ${log.rawResponse}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Parsed value
                if (log.parsedValue.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Value: ${log.parsedValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun getColorForDataType(dataType: OBDDataType): Color {
    return when (dataType) {
        OBDDataType.RPM -> Color(0xFF5C6BC0) // Indigo
        OBDDataType.SPEED -> Color(0xFF66BB6A) // Green
        OBDDataType.ENGINE_LOAD -> Color(0xFFFFB74D) // Orange
        OBDDataType.COOLANT_TEMP -> Color(0xFFEF5350) // Red
        OBDDataType.FUEL_LEVEL -> Color(0xFF8D6E63) // Brown
        OBDDataType.INTAKE_AIR_TEMP -> Color(0xFF42A5F5) // Blue
        OBDDataType.THROTTLE_POSITION -> Color(0xFFAB47BC) // Purple
        OBDDataType.FUEL_PRESSURE -> Color(0xFF26A69A) // Teal
        OBDDataType.BARO_PRESSURE -> Color(0xFF78909C) // Blue Grey
        OBDDataType.BATTERY_VOLTAGE -> Color(0xFFFFEE58) // Yellow
        OBDDataType.CONNECTION -> Color(0xFF1E88E5) // Blue
        OBDDataType.INITIALIZATION -> Color(0xFF7CB342) // Light Green
        OBDDataType.UNKNOWN -> Color(0xFF757575) // Grey
    }
}

@Preview(showBackground = true)
@Composable
fun LogViewerScreenPreview() {
    Surface {
        LogViewerScreen(onBackPressed = {})
    }
}