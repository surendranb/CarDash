package com.fuseforge.cardash.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuseforge.cardash.data.db.DiagnosticCode
import com.fuseforge.cardash.model.TelemetryStatus

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel
) {
    val codes by viewModel.scannedCodes.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DIAGNOSTICS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (connectionStatus != TelemetryStatus.ACTIVE) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Connect to OBD-II to scan for faults",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            // Scan Button
            Button(
                onClick = { viewModel.scan() },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning Vehicle...")
                } else {
                    Text("Quick Scan for Faults")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (codes.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trouble codes detected", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(codes) { code ->
                        DiagnosticItem(code)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticItem(code: DiagnosticCode) {
    val cardColor = when (code.severity) {
        3 -> MaterialTheme.colorScheme.errorContainer
        2 -> Color(0xFFFFCC00).copy(alpha = 0.2f) // Warning Yellow
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (code.severity) {
        3 -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (code.severity >= 2) {
                    Text(
                        text = if (code.severity == 3) "CRITICAL" else "WARNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (code.severity == 3) Color.Red else Color(0xFFDAA520)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = code.description,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            
            if (!code.possibleCauses.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = textColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "POSSIBLE CAUSES:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Text(
                    text = code.possibleCauses,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}
