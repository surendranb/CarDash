package com.fuseforge.cardash.ui.graphs

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuseforge.cardash.ui.theme.CarDashTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GraphScreen(
    modifier: Modifier = Modifier,
    viewModel: GraphViewModel = viewModel(factory = GraphViewModelFactory(LocalContext.current))
) {
    val selectedParameters by viewModel.selectedParameters.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val graphData by viewModel.graphData.collectAsState()
    val allReadings by viewModel.filteredReadings.collectAsState()
    
    // For date range picker
    var showDateRangePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Filter section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Just parameter pills without any header
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.availableParameters) { parameter ->
                        val isSelected = selectedParameters.contains(parameter)
                        ParameterChip(
                            parameter = parameter,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleParameterSelection(parameter) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // All quick date range buttons in a single line with horizontalScroll
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Last Hour" button
                    item {
                        DateRangeButton(
                            text = "Last Hour",
                            onClick = { 
                                val now = Date()
                                val hourAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.HOUR, -1)
                                }.time
                                viewModel.updateDateRange(hourAgo, now)
                            }
                        )
                    }
                    
                    // "Last 6h" button
                    item {
                        DateRangeButton(
                            text = "Last 6h",
                            onClick = { 
                                val now = Date()
                                val sixHoursAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.HOUR, -6)
                                }.time
                                viewModel.updateDateRange(sixHoursAgo, now)
                            }
                        )
                    }
                    
                    // "Last 12h" button
                    item {
                        DateRangeButton(
                            text = "Last 12h",
                            onClick = { 
                                val now = Date()
                                val twelveHoursAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.HOUR, -12)
                                }.time
                                viewModel.updateDateRange(twelveHoursAgo, now)
                            }
                        )
                    }
                    
                    // "Last 24h" button
                    item {
                        DateRangeButton(
                            text = "Last 24h",
                            onClick = { 
                                val now = Date()
                                val dayAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.DAY_OF_YEAR, -1)
                                }.time
                                viewModel.updateDateRange(dayAgo, now)
                            }
                        )
                    }
                    
                    // "Last Week" button
                    item {
                        DateRangeButton(
                            text = "Last Week",
                            onClick = { 
                                val now = Date()
                                val weekAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.DAY_OF_YEAR, -7)
                                }.time
                                viewModel.updateDateRange(weekAgo, now)
                            }
                        )
                    }
                    
                    // "Last Month" button
                    item {
                        DateRangeButton(
                            text = "Last Month",
                            onClick = { 
                                val now = Date()
                                val monthAgo = Calendar.getInstance().apply {
                                    time = now
                                    add(Calendar.MONTH, -1)
                                }.time
                                viewModel.updateDateRange(monthAgo, now)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Graph results section with a header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // No header for trend results
                
                // Display graphs
                // If no readings available, show a message
                if (allReadings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available for the selected time period",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (graphData.isEmpty()) {
                    // If readings are available but no parameters are selected for graphing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Please select at least one parameter to graph",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    graphData.forEach { (dataType, dataPoints) ->
                        if (dataPoints.isNotEmpty()) {
                            Text(
                                text = dataType.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            LineChartCanvas(
                                dataPoints = dataPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterChip(
    parameter: GraphParameter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = parameter.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DateRangeButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LineGraph(
    data: GraphData,
    modifier: Modifier = Modifier
) {
    val parameter = data.parameter
    val dataPoints = data.dataPoints
    
    Column(
        modifier = modifier
    ) {
        // Graph title with units
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = parameter.unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Graph display area with stats
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(4.dp)
        ) {
            if (dataPoints.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val minValue = dataPoints.minOf { it.value }
                val maxValue = dataPoints.maxOf { it.value }
                
                // Display min/max values in a horizontal stats bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Min: ${String.format("%.1f", minValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Max: ${String.format("%.1f", maxValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Display time range
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateFormat.format(dataPoints.first().timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = dateFormat.format(dataPoints.last().timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Draw the line chart in the center area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 24.dp) // Make room for labels
                ) {
                    LineChartCanvas(
                        dataPoints = dataPoints,
                        modifier = Modifier.fillMaxSize(),
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GraphScreenPreview() {
    CarDashTheme {
        Surface {
            GraphScreen()
        }
    }
}