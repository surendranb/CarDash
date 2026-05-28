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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var vehicleProfile by remember { mutableStateOf(prefsManager.getVehicleProfile()) }
    var aiInsightsEnabled by remember { mutableStateOf(prefsManager.isAiInsightsEnabled()) }
    var geminiApiKey by remember { mutableStateOf(prefsManager.getGeminiApiKey()) }
    var geminiModelName by remember { mutableStateOf(prefsManager.getGeminiModelName()) }
    var fuelMultiplier by remember { mutableStateOf(prefsManager.getFuelMultiplier().toString()) }

    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ZipOutputStream(outputStream).use { zos ->
                                val dbFiles = listOf(
                                    context.getDatabasePath("car_dash_database"),
                                    context.getDatabasePath("car_dash_database-shm"),
                                    context.getDatabasePath("car_dash_database-wal")
                                )
                                dbFiles.forEach { dbFile ->
                                    if (dbFile.exists()) {
                                        zos.putNextEntry(ZipEntry(dbFile.name))
                                        FileInputStream(dbFile).use { it.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                                val prefsFiles = listOf(
                                    File(context.applicationInfo.dataDir, "shared_prefs/CarDashPrefs.xml"),
                                    File(context.filesDir, "datastore/settings.preferences_pb")
                                )
                                prefsFiles.forEach { prefsFile ->
                                    if (prefsFile.exists()) {
                                        zos.putNextEntry(ZipEntry(prefsFile.name))
                                        FileInputStream(prefsFile).use { it.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                    }
                    Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                text = "Critical for accurate AI diagnostics. Helps Gemini interpret manufacturer-specific OBD-II Trouble Codes (DTC) and nominal sensor ranges.",
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
                text = "Scales the OBD-II fuel percentage. For example, if your car's ECU reports a 70L tank but you physically have a 40L tank, set this to 1.75 to correct the gauge display.",
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
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Your key stays securely on your device. Get one for free at aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = geminiModelName,
                    onValueChange = { 
                        geminiModelName = it
                        prefsManager.setGeminiModelName(it)
                    },
                    label = { Text("Gemini Model Target") },
                    placeholder = { Text("e.g. gemini-1.5-flash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

        // Data Management
        item {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { exportLauncher.launch("cardash_export_${System.currentTimeMillis()}.zip") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export App Data (ZIP)")
            }
            Text(
                text = "Export your telemetry database and AI chat history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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
                    Text("Version ${com.fuseforge.cardash.BuildConfig.VERSION_NAME} (Reactor V2)", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "AI powered insights for your vehicle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom nav padding
    }
}
