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
import androidx.compose.runtime.collectAsState
import com.example.cardash.ui.components.MetricCard
import com.example.cardash.ui.components.MetricStatus

@Composable
fun MetricGridScreen(
    viewModel: MetricViewModel = viewModel() // Get ViewModel
) {
    val rpm by viewModel.rpm.collectAsState()
    val engineLoad by viewModel.engineLoad.collectAsState()
    val speed by viewModel.speed.collectAsState()

    val metrics = listOf(
        MetricData("RPM", rpm.toString(), "rpm", MetricStatus.NORMAL),
        MetricData("Engine Load", "$engineLoad%", "", MetricStatus.NORMAL),
        MetricData("Speed", speed.toString(), "km/h", MetricStatus.NORMAL)
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
