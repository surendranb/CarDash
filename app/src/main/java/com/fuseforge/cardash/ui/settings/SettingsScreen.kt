package com.fuseforge.cardash.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.ui.theme.Error
import com.fuseforge.cardash.ui.theme.Success

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var vehicleProfile by remember { mutableStateOf(prefsManager.getVehicleProfile()) }
    var aiInsightsEnabled by remember { mutableStateOf(prefsManager.isAiInsightsEnabled()) }
    var geminiApiKey by remember { mutableStateOf(prefsManager.getGeminiApiKey()) }
    var geminiModelName by remember { mutableStateOf(prefsManager.getGeminiModelName()) }
    var fuelMultiplier by remember { mutableStateOf(prefsManager.getFuelMultiplier().toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Vehicle Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // 1. Vehicle Profile
        item {
            OutlinedTextField(
                value = vehicleProfile,
                onValueChange = { 
                    vehicleProfile = it
                    prefsManager.setVehicleProfile(it)
                },
                label = { Text("Make & Model") },
                placeholder = { Text("e.g. 2018 Toyota RAV4") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Helps AI provide engine-specific diagnostics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        // Fuel Calibration Offset
        item {
            Text(
                text = "Fuel Gauge Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = fuelMultiplier,
                onValueChange = { 
                    fuelMultiplier = it
                    it.toFloatOrNull()?.let { multiplier -> prefsManager.setFuelMultiplier(multiplier) }
                },
                label = { Text("Fuel Multiplier") },
                placeholder = { Text("e.g. 1.75 for Renault Kiger") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Scales the OBD-II reported fuel percentage. For example, Renault CMF-A+ cars (Kiger, Triber, Magnite) use a 70L ECU nominal capacity baseline but physically have a 40L/35L tank. Set this to 1.75 (or 2.0) to correct the gauge display.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        // 3. AI Insights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Mechanic Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable predictive maintenance alerts using Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = aiInsightsEnabled,
                    onCheckedChange = { 
                        aiInsightsEnabled = it
                        prefsManager.setAiInsightsEnabled(it)
                    }
                )
            }
            
            if (aiInsightsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { 
                        geminiApiKey = it
                        prefsManager.setGeminiApiKey(it)
                    },
                    label = { Text("Google AI API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = geminiModelName,
                    onValueChange = { 
                        geminiModelName = it
                        prefsManager.setGeminiModelName(it)
                    },
                    label = { Text("Gemini Model") },
                    placeholder = { Text("e.g. gemini-1.5-flash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        // 4. About
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About CarDash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 2.2.1 (Reactor V2)", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Designed for high-fidelity vehicle monitoring. All telemetry data is stored locally. AI insights are processed via Google Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom nav padding
    }
}
