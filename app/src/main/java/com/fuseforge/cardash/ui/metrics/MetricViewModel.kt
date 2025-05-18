package com.fuseforge.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.fuseforge.cardash.data.preferences.AppPreferences
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.OBDServiceWithDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import com.fuseforge.cardash.data.db.AppDatabase

class MetricViewModel(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics
) : ViewModel() {

    // Get application context from the OBD service
    private val context = obdServiceWithDiagnostics.getContext()
    
    // Initialize preferences
    private val preferences = AppPreferences(context)

    // Database access for storing combined readings
    private val database = AppDatabase.getDatabase(context)
    private val obdLogDao = database.obdLogDao()
    
    // Last time we stored a combined reading (to avoid too frequent writes)
    private var lastCombinedReadingTime = System.currentTimeMillis() - 5000 // Start 5 seconds ago
    
    // Initialize the sequential poller (will be set up after connection)
    private lateinit var sequentialPoller: SequentialMetricPoller

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Add engine state tracking
    private val _engineRunning = MutableStateFlow(false)
    val engineRunning = _engineRunning.asStateFlow()

    private val _rpm = MutableStateFlow(0)
    val rpm = _rpm.asStateFlow()
    
    private val _engineLoad = MutableStateFlow(0)
    val engineLoad = _engineLoad.asStateFlow()

    private val _speed = MutableStateFlow(0)
    val speed = _speed.asStateFlow()

    private val _coolantTemp = MutableStateFlow(0)
    val coolantTemp = _coolantTemp.asStateFlow()

    private val _fuelLevel = MutableStateFlow(0)
    val fuelLevel = _fuelLevel.asStateFlow()

    private val _intakeAirTemp = MutableStateFlow(0)
    val intakeAirTemp = _intakeAirTemp.asStateFlow()

    private val _throttlePosition = MutableStateFlow(0)
    val throttlePosition = _throttlePosition.asStateFlow()

    private val _fuelPressure = MutableStateFlow(0)
    val fuelPressure = _fuelPressure.asStateFlow()

    private val _baroPressure = MutableStateFlow(0)
    val baroPressure = _baroPressure.asStateFlow()

    private val _batteryVoltage = MutableStateFlow(0f)
    val batteryVoltage = _batteryVoltage.asStateFlow()
    
    // New metrics from database - simplified
    private val _averageSpeed = MutableStateFlow(0)
    val averageSpeed = _averageSpeed.asStateFlow()
    
    private val _fuelLevelHistory = MutableStateFlow<List<Int>>(emptyList())
    val fuelLevelHistory = _fuelLevelHistory.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()
    
    // Logging preference flow for UI
    private val _verboseLoggingEnabled = MutableStateFlow(preferences.verboseLoggingEnabled)
    val verboseLoggingEnabled = _verboseLoggingEnabled.asStateFlow()

    /**
     * Connect to OBD device and start data collection
     */
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _errorMessage.emit("Connecting to $deviceAddress...")
            
            when (val result = obdService.connect(deviceAddress)) {
                is OBDService.ConnectionResult.Success -> {
                    _connectionState.value = ConnectionState.Connected
                    _errorMessage.emit("Successfully connected to OBD2 adapter")
                    
                    // Initialize sequential poller
                    sequentialPoller = SequentialMetricPoller(
                        obdService = obdService,
                        obdServiceWithDiagnostics = obdServiceWithDiagnostics,
                        preferences = preferences,
                        obdLogDao = obdLogDao,
                        viewModelScope = viewModelScope,
                        connectionState = _connectionState,
                        engineRunning = _engineRunning,
                        rpm = _rpm,
                        speed = _speed,
                        engineLoad = _engineLoad,
                        coolantTemp = _coolantTemp,
                        fuelLevel = _fuelLevel,
                        intakeAirTemp = _intakeAirTemp,
                        throttlePosition = _throttlePosition,
                        fuelPressure = _fuelPressure,
                        baroPressure = _baroPressure,
                        batteryVoltage = _batteryVoltage,
                        errorMessage = _errorMessage
                    )
                    
                    // Start polling metrics
                    sequentialPoller.startPolling()
                }
                is OBDService.ConnectionResult.Error -> {
                    println("Connection failed: ${result.message}")
                    _connectionState.value = ConnectionState.Failed(result.message)
                    _errorMessage.emit("Connection error: ${result.message}")
                }
            }
        }
    }

    /**
     * Disconnect from OBD device and stop data collection
     */
    fun disconnect() {
        viewModelScope.launch {
            // Stop the sequential poller if it's initialized
            if (::sequentialPoller.isInitialized) {
                sequentialPoller.stopPolling()
            }
            
            // Disconnect from OBD
            obdService.disconnect()
            
            // Reset all state values
            _connectionState.value = ConnectionState.Disconnected
            _engineRunning.value = false
            _rpm.value = 0
            _engineLoad.value = 0
            _speed.value = 0
            _coolantTemp.value = 0
            _fuelLevel.value = 0
            _intakeAirTemp.value = 0
            _throttlePosition.value = 0
            _fuelPressure.value = 0
            _baroPressure.value = 0
            _batteryVoltage.value = 0f
            _averageSpeed.value = 0
            // Note: we don't clear fuel history so it stays visible even when disconnected
        }
    }

    /**
     * Get paired Bluetooth devices
     */
    suspend fun getPairedDevices(): Set<BluetoothDevice> {
        return withContext(Dispatchers.IO) {
            obdService.bluetoothManager.getPairedDevices()
        }
    }

    /**
     * Check permissions (not implemented in OBDService)
     */
    fun checkPermissions(): String? {
        return null // OBDService doesn't have checkPermissions()
    }

    /**
     * Toggle verbose logging setting
     */
    fun toggleVerboseLogging(enabled: Boolean) {
        preferences.verboseLoggingEnabled = enabled
        _verboseLoggingEnabled.value = enabled
    }
    
    /**
     * Set data collection frequency
     */
    fun setDataCollectionFrequency(frequencyMs: Int) {
        if (frequencyMs in 1000..10000) { // Reasonable range check
            preferences.dataCollectionFrequencyMs = frequencyMs
        }
    }
    
    /**
     * Set storage frequency
     */
    fun setStorageFrequency(frequencyMs: Int) {
        if (frequencyMs in 3000..30000) { // Reasonable range check
            preferences.storageFrequencyMs = frequencyMs
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
}