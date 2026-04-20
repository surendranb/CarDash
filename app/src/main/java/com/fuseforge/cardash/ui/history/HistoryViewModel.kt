package com.fuseforge.cardash.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import com.fuseforge.cardash.utils.HealthAnalyticsEngine
import com.fuseforge.cardash.utils.VehicleHealthVitals
import com.fuseforge.cardash.CarDashApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryViewModel(private val context: Context) : ViewModel() {
    private val app = context.applicationContext as CarDashApp
    private val dao = app.database.obdLogDao()
    
    val isRecording: StateFlow<Boolean> = app.telemetrist.state
        .map { it.connectionStatus == com.fuseforge.cardash.model.TelemetryStatus.ACTIVE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Stream of the 1-minute Ledger Heartbeats
    val ledgerHeartbeats: StateFlow<List<VehicleHeartbeat>> = dao.getRecentHeartbeats(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    // Computed Vitals from the ledger
    val vitals: StateFlow<VehicleHealthVitals?> = dao.getRecentHeartbeats(100)
        .map { heartbeats ->
            if (heartbeats.isEmpty()) null else HealthAnalyticsEngine.computeVitals(heartbeats)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}