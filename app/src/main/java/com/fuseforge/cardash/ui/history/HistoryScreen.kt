package com.fuseforge.cardash.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuseforge.cardash.data.db.OBDCombinedReading
import com.fuseforge.cardash.ui.theme.CarDashTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(LocalContext.current))
) {
    val readings by viewModel.lastReadings.collectAsState()
    val parameters = viewModel.parameters
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Table header
        TableHeader(parameters)
        
        Divider()
        
        if (readings.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No data available yet. Connect to your vehicle to start collecting data.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Button to generate mock data
                    Button(
                        onClick = { viewModel.generateMockData() }
                    ) {
                        Text("Generate Test Data")
                    }
                }
            }
        } else {
            // Data rows
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(readings) { index, reading ->
                    TableRow(
                        reading = reading,
                        parameters = parameters,
                        isEven = index % 2 == 0
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun TableHeader(parameters: List<ParameterInfo>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        // Timestamp column
        Text(
            text = "Time",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 4.dp)
        )
        
        // Parameter columns
        parameters.forEach { param ->
            Text(
                text = param.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun TableRow(
    reading: OBDCombinedReading,
    parameters: List<ParameterInfo>,
    isEven: Boolean
) {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestampText = formatter.format(reading.timestamp)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEven) MaterialTheme.colorScheme.surface 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        // Timestamp column
        Text(
            text = timestampText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 4.dp)
        )
        
        // Parameter columns
        parameters.forEach { param ->
            Text(
                text = param.valueFormatter(reading),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    CarDashTheme {
        Surface {
            HistoryScreen()
        }
    }
}