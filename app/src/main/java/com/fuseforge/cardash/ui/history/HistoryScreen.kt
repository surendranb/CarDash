package com.fuseforge.cardash.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import com.fuseforge.cardash.ui.theme.Success
import com.fuseforge.cardash.ui.theme.Warning
import com.fuseforge.cardash.ui.theme.Error as ThemeError
import com.fuseforge.cardash.utils.VehicleHealthVitals
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(LocalContext.current))
) {
    val heartbeats by viewModel.ledgerHeartbeats.collectAsState()
    val vitals by viewModel.vitals.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        
        // Ledger Status Header (Phase 3 restoration)
        LedgerStatusHeader(isRecording)
        
        // Vitals Badges Section (Task 3.3)
        vitals?.let { v ->
            VitalsDashboard(v)
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }
        
        if (heartbeats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "The Digital Ledger is Empty.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History is recorded in 1-minute 'Heartbeats'. Connect to your vehicle to begin the ledger.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Ledger Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TIME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("RPM / LOAD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("KPH / FUEL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("VITAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            
            // Ledger List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(heartbeats) { hb ->
                    HeartbeatRow(hb)
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun LedgerStatusHeader(isRecording: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isRecording) Success else Warning, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) "DIGITAL LEDGER: RECORDING..." else "DIGITAL LEDGER: STANDBY (Disconnected)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (isRecording) Success else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VitalsDashboard(vitals: VehicleHealthVitals) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Digital Clone Vitals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VitalBadge(
                label = "Battery",
                value = "${vitals.batteryHealthScore}%",
                isWarning = vitals.batteryHealthScore < 50
            )
            VitalBadge(
                label = "Thermal",
                value = "${vitals.thermalConsistencyScore}%",
                isWarning = vitals.thermalConsistencyScore < 60
            )
            VitalBadge(
                label = "Idling Stress",
                value = "${vitals.idlingFatiguePercentage}%",
                isWarning = vitals.idlingFatiguePercentage > 30
            )
        }
        
        if (vitals.voltageStressDetected) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = ThemeError.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠ Severe Voltage Stress Detected Recently",
                    style = MaterialTheme.typography.labelMedium,
                    color = ThemeError,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VitalBadge(label: String, value: String, isWarning: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isWarning) Warning.copy(alpha = 0.2f) else Success.copy(alpha = 0.2f))
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .widthIn(min = 80.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) Warning else Success
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HeartbeatRow(hb: VehicleHeartbeat) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeStr = formatter.format(hb.timestamp)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(timeStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Active: ${hb.activeSeconds}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${hb.avgRpm ?: "-"} RPM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Load: ${hb.avgEngineLoad ?: "-"}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${hb.avgSpeed ?: 0} KPH", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            val fuelText = if (hb.fuelLevel != null) "${hb.fuelLevel}%" else "-"
            Text("Fuel: $fuelText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            val voltStr = hb.avgBatteryVoltage?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-"
            Text("${voltStr}V / ${hb.maxCoolantTemp ?: "-"}°C", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if ((hb.maxCoolantTemp ?: 0) > 105) "⚠ OVERHEAT" else "Normal",
                style = MaterialTheme.typography.labelSmall,
                color = if ((hb.maxCoolantTemp ?: 0) > 105) ThemeError else Success
            )
        }
    }
}