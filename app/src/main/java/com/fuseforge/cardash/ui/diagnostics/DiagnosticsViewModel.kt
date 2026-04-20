package com.fuseforge.cardash.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.data.db.DiagnosticCode
import com.fuseforge.cardash.data.db.DiagnosticDao
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.services.obd.Telemetrist
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiagnosticsViewModelFactory(
    private val context: android.content.Context,
    private val diagnosticDao: DiagnosticDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticsViewModel::class.java)) {
            val app = context.applicationContext as com.fuseforge.cardash.CarDashApp
            @Suppress("UNCHECKED_CAST")
            return DiagnosticsViewModel(app.telemetrist, diagnosticDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DiagnosticsViewModel(
    private val telemetrist: Telemetrist,
    private val diagnosticDao: DiagnosticDao
) : ViewModel() {

    private val _scannedCodes = MutableStateFlow<List<DiagnosticCode>>(emptyList())
    val scannedCodes = _scannedCodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val connectionStatus = telemetrist.state.map { it.connectionStatus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TelemetryStatus.DISCONNECTED)

    fun scan() {
        if (_isScanning.value || connectionStatus.value != TelemetryStatus.ACTIVE) return

        viewModelScope.launch {
            _isScanning.value = true
            try {
                val rawCodes = telemetrist.scanTroubleCodes()
                val mapped = mutableListOf<DiagnosticCode>()
                
                for (raw in rawCodes) {
                    val entity = diagnosticDao.getDiagnosticByCode(raw) 
                        ?: DiagnosticCode(raw, "DTC $raw", "Diagnostic code detected via OBD-II.")
                    mapped.add(entity)
                }
                _scannedCodes.value = mapped
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clear() {
        _scannedCodes.value = emptyList()
    }
}
