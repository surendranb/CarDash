package com.example.cardash.ui.metrics

// Removed duplicate import
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells // Keep one GridCells import
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Import viewModel
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun MetricGridScreen(
    viewModel: MetricViewModel = viewModel() // Inject ViewModel
) {
    val rpm by viewModel.rpm.collectAsState() // Collect RPM state

    // TODO: Replace other hardcoded metrics later
    val metrics = listOf(
        // Determine RPM status based on value later, for now NORMAL
        MetricData("RPM", rpm.toString(), "rpm", MetricStatus.NORMAL), // Use collected RPM, default status NORMAL
        MetricData("Speed", "---", "km/h", MetricStatus.NORMAL), // Placeholder, use NORMAL
        MetricData("Coolant", "---", "°C", MetricStatus.NORMAL), // Placeholder, use NORMAL
        MetricData("Voltage", "---", "V", MetricStatus.NORMAL) // Placeholder, use NORMAL
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(16.dp)
    ) {
        items(metrics) { metric ->
            MetricCard(
                title = metric.title,
                value = metric.value,
                unit = metric.unit,
                status = metric.status
            )
        }
    }
}

data class MetricData(
    val title: String,
    val value: String,
    val unit: String,
    val status: MetricStatus
)
