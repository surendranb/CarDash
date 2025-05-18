package com.fuseforge.cardash.ui.history

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.OBDCombinedReading
import com.fuseforge.cardash.utils.MockDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).obdLogDao()
    private val mockDataGenerator = MockDataGenerator(context)
    
    // Stream of the last 25 readings
    val lastReadings: StateFlow<List<OBDCombinedReading>> = dao.getLastCombinedReadings(25)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // List of parameters to display
    val parameters: List<ParameterInfo> = listOf(
        ParameterInfo("RPM", "rpm", "rpm") { it.rpm?.toString() ?: "-" },
        ParameterInfo("Speed", "speed", "km/h") { it.speed?.toString() ?: "-" },
        ParameterInfo("Load", "engineLoad", "%") { it.engineLoad?.toString() ?: "-" },
        ParameterInfo("Coolant", "coolantTemp", "°C") { it.coolantTemp?.toString() ?: "-" },
        ParameterInfo("Fuel", "fuelLevel", "%") { it.fuelLevel?.toString() ?: "-" },
        ParameterInfo("IAT", "intakeAirTemp", "°C") { it.intakeAirTemp?.toString() ?: "-" },
        ParameterInfo("Throttle", "throttlePosition", "%") { it.throttlePosition?.toString() ?: "-" },
        ParameterInfo("F.Press", "fuelPressure", "kPa") { it.fuelPressure?.toString() ?: "-" },
        ParameterInfo("B.Press", "baroPressure", "kPa") { it.baroPressure?.toString() ?: "-" },
        ParameterInfo("Battery", "batteryVoltage", "V") { 
            it.batteryVoltage?.let { voltage -> String.format("%.1f", voltage) } ?: "-" 
        }
    )
    
    /**
     * Generate mock data for testing
     */
    fun generateMockData() {
        viewModelScope.launch {
            mockDataGenerator.generateMockData(25)
        }
    }
}

/**
 * Class representing a parameter to display in the history screen
 */
data class ParameterInfo(
    val displayName: String,
    val id: String,
    val unit: String,
    val valueFormatter: (OBDCombinedReading) -> String
)

/**
 * Factory for creating HistoryViewModel with application context
 */
class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}