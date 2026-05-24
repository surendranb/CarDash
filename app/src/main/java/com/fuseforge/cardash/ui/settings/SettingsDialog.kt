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

@OptIn(ExperimentalMaterial3Api::class)
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
    var fuelMultiplier by remember { mutableStateOf(prefsManager.getFuelMultiplier().toString()) }
    
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
                    text = "Critical for accurate AI diagnostics. Helps Gemini interpret manufacturer-specific OBD-II Trouble Codes (DTC) and nominal sensor ranges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = fuelMultiplier,
                    onValueChange = { 
                        fuelMultiplier = it
                        it.toFloatOrNull()?.let { multiplier -> prefsManager.setFuelMultiplier(multiplier) }
                    },
                    label = { Text("Fuel Multiplier") },
                    placeholder = { Text("e.g. 1.75") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Scales the OBD-II fuel percentage. Use this factor to calibrate your fuel display if the raw data provided by your vehicle's computer does not accurately represent your actual tank capacity.",
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
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Text(
                    text = "Your key stays securely on your device. Get one for free at aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = geminiModelName,
                    onValueChange = { 
                        geminiModelName = it
                        prefsManager.setGeminiModelName(it)
                    },
                    label = { Text("Gemini Model Target") },
                    placeholder = { Text("e.g. gemini-1.5-flash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                val models = listOf("gemini-flash-lite-latest", "gemini-flash-latest", "gemini-pro-latest")
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models.size) { index ->
                        val model = models[index]
                        AssistChip(
                            onClick = {
                                geminiModelName = model
                                prefsManager.setGeminiModelName(model)
                            },
                            label = { Text(model.replace("-latest", "").replace("gemini-", "")) }
                        )
                    }
                }

                Text(
                    text = "Enter any valid model ID from Google AI Studio. Flash-Lite is fast; Pro is for deep diagnostics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Reactor Engine: Cycles are now deterministic (2.5s) for AA Safety.
                // Adjustable frequency is disabled to prevent Quota violations.
                
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