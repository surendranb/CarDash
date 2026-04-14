package com.fuseforge.cardash.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuseforge.cardash.data.preferences.AppPreferences
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.ui.metrics.MetricViewModel

@Composable
fun SettingsDialog(
    tabSettings: TabSettings,
    metricViewModel: MetricViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // Collect states correctly
    val verboseLoggingEnabled by metricViewModel.verboseLoggingEnabled.collectAsState()
    var verboseLoggingState by remember { mutableStateOf(verboseLoggingEnabled) }
    
    // Data collection frequency slider value
    var dataCollectionFrequency by remember { mutableStateOf(AppPreferences.DEFAULT_DATA_COLLECTION_FREQUENCY) } 
    
    // Storage frequency slider value
    var storageFrequency by remember { mutableStateOf(AppPreferences.DEFAULT_STORAGE_FREQUENCY) }
    
    // Update local state when external state changes
    LaunchedEffect(verboseLoggingEnabled) {
        verboseLoggingState = verboseLoggingEnabled
    }
    
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    var vehicleProfile by remember { mutableStateOf(prefsManager.getVehicleProfile()) }
    var aiInsightsEnabled by remember { mutableStateOf(prefsManager.isAiInsightsEnabled()) }
    var geminiApiKey by remember { mutableStateOf(prefsManager.getGeminiApiKey()) }
    var geminiModelName by remember { mutableStateOf(prefsManager.getGeminiModelName()) }
    var odometerOffset by remember { mutableStateOf(if (prefsManager.getOdometerOffset() == 0) "" else prefsManager.getOdometerOffset().toString()) }
    
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Show/Hide Tabs",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Metrics Tab (Always on and disabled)
                TabSettingItem(
                    tabName = "Metrics",
                    checked = true,
                    enabled = false, // Can't be toggled
                    onCheckedChange = { /* No-op */ }
                )
                
                // History Tab
                TabSettingItem(
                    tabName = "History",
                    checked = tabSettings.showHistoryTab,
                    enabled = true,
                    onCheckedChange = { tabSettings.showHistoryTab = it }
                )
                
                // Diagnostics Tab
                TabSettingItem(
                    tabName = "Diagnostics",
                    checked = tabSettings.showDiagnosticsTab,
                    enabled = true,
                    onCheckedChange = { tabSettings.showDiagnosticsTab = it }
                )
                
                // Graphs Tab (renamed to Trends)
                TabSettingItem(
                    tabName = "Trends",
                    checked = tabSettings.showGraphsTab,
                    enabled = true,
                    onCheckedChange = { tabSettings.showGraphsTab = it }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))

                // Vehicle Profile
                Text(
                    text = "Vehicle Profile",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = vehicleProfile,
                    onValueChange = { 
                        vehicleProfile = it
                        prefsManager.setVehicleProfile(it)
                    },
                    label = { Text("Make & Model (Optional)") },
                    placeholder = { Text("e.g. 2018 Toyota RAV4") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Helps Gemini AI provide better insights for your specific car.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // AI Settings
                Text(
                    text = "Intelligence",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TabSettingItem(
                    tabName = "Gemini AI Insights (Sparkle)",
                    checked = aiInsightsEnabled,
                    enabled = true,
                    onCheckedChange = { 
                        aiInsightsEnabled = it
                        prefsManager.setAiInsightsEnabled(it)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { 
                        geminiApiKey = it
                        prefsManager.setGeminiApiKey(it)
                    },
                    label = { Text("Google AI Studio Token") },
                    placeholder = { Text("AIza...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = geminiModelName,
                    onValueChange = { 
                        geminiModelName = it
                        prefsManager.setGeminiModelName(it)
                    },
                    label = { Text("LLM Model Target") },
                    placeholder = { Text("e.g. gemma-4-31b-it") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))
                
                // Manual Odometer Sync
                Text(
                    text = "Odometer Sync",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = odometerOffset,
                    onValueChange = { 
                        odometerOffset = it
                        val parsed = it.toIntOrNull()
                        if (parsed != null) {
                            prefsManager.setOdometerOffset(parsed)
                        } else if (it.isEmpty()) {
                            prefsManager.setOdometerOffset(0)
                        }
                    },
                    label = { Text("Manual Dashboard Odometer (km)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Allows CarDash to sync recorded distances accurately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Data Collection Settings
                Text(
                    text = "Data Collection",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Verbose logging setting
                TabSettingItem(
                    tabName = "Verbose OBD Command Logging",
                    checked = verboseLoggingState,
                    enabled = true,
                    onCheckedChange = { 
                        verboseLoggingState = it
                        metricViewModel.toggleVerboseLogging(it)
                    }
                )
                
                // Data collection frequency
                Text(
                    text = "Data Collection Cycle: ${dataCollectionFrequency / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Slider(
                    value = dataCollectionFrequency.toFloat(),
                    onValueChange = { dataCollectionFrequency = it.toInt() },
                    onValueChangeFinished = {
                        metricViewModel.setDataCollectionFrequency(dataCollectionFrequency)
                    },
                    valueRange = 1000f..10000f,
                    steps = 8,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // Storage frequency
                Text(
                    text = "Database Write Interval: ${storageFrequency / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Slider(
                    value = storageFrequency.toFloat(),
                    onValueChange = { storageFrequency = it.toInt() },
                    onValueChangeFinished = {
                        metricViewModel.setStorageFrequency(storageFrequency)
                    },
                    valueRange = 3000f..30000f,
                    steps = 8,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // About button
                Button(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("About CarDash")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun TabSettingItem(
    tabName: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tabName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}