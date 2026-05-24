package com.fuseforge.cardash.ui.graphs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GraphViewModel(private val context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).obdLogDao()
    
    // Available parameters for graphing
    val availableParameters = listOf(
        GraphParameter("RPM (Avg)", "rpm") { it.avgRpm },
        GraphParameter("Speed (Avg)", "speed", "km/h") { it.avgSpeed },
        GraphParameter("Engine Load", "engineLoad", "%") { it.avgEngineLoad },
        GraphParameter("Max Coolant", "coolantTemp", "°C") { it.maxCoolantTemp },
        GraphParameter("Fuel Level", "fuelLevel", "%") { it.fuelLevel },
        GraphParameter("Intake Temp", "intakeAirTemp", "°C") { it.avgIntakeAirTemp },
        GraphParameter("Throttle", "throttlePosition", "%") { it.avgThrottlePosition },
        GraphParameter("Battery (Avg)", "batteryVoltage", "V") { it.avgBatteryVoltage?.toDouble() }
    )
    
    // Selected parameters to display
    private val _selectedParameters = MutableStateFlow(
        setOf(availableParameters[0], availableParameters[1], availableParameters[2], availableParameters[3]) 
    )
    val selectedParameters: StateFlow<Set<GraphParameter>> = _selectedParameters
    
    // Date range
    private val _startDate = MutableStateFlow(getDefaultStartDate())
    val startDate: StateFlow<Date> = _startDate
    
    private val _endDate = MutableStateFlow(Date()) // Current date
    val endDate: StateFlow<Date> = _endDate
    
    // More efficient loading of filtered readings directly from the database
    // This uses the DB query with time range filter instead of filtering in memory
    val filteredReadings = combine(_startDate, _endDate) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        dao.getHeartbeatsByTimeRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Generate graph data points for each selected parameter
    val graphData = combine(filteredReadings, selectedParameters) { readings, params ->
        params.map { param ->
            val dataPoints = readings.mapNotNull { reading ->
                val value = param.valueExtractor(reading)
                if (value != null) {
                    GraphDataPoint(reading.timestamp, value.toDouble())
                } else {
                    null
                }
            }
            GraphData(param, dataPoints.sortedBy { it.timestamp }) // Ensure chronological order
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Toggle a parameter selection
    fun toggleParameterSelection(parameter: GraphParameter) {
        val current = _selectedParameters.value.toMutableSet()
        if (current.contains(parameter)) {
            current.remove(parameter)
        } else {
            current.add(parameter)
        }
        _selectedParameters.value = current
    }
    
    // Update date range
    fun updateDateRange(start: Date, end: Date) {
        _startDate.value = start
        _endDate.value = end
    }
    
    // Get default start date (24 hours ago)
    private fun getDefaultStartDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -24)
        return calendar.time
    }
    
    // Format date for display
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}

// Class to represent a parameter that can be graphed
data class GraphParameter(
    val displayName: String,
    val id: String,
    val unit: String = "",
    val valueExtractor: (VehicleHeartbeat) -> Number?
)

// Class to represent a single data point on the graph
data class GraphDataPoint(
    val timestamp: Date,
    val value: Double
)

// Class to represent a complete set of data for a graph
data class GraphData(
    val parameter: GraphParameter,
    val dataPoints: List<GraphDataPoint>
)

// Factory for creating GraphViewModel with application context
class GraphViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GraphViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GraphViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}