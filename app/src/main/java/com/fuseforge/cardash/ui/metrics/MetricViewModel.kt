package com.fuseforge.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.fuseforge.cardash.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import com.fuseforge.cardash.data.db.AppDatabase

class MetricViewModel(context: Context) : ViewModel() {
    private val app = context.applicationContext as com.fuseforge.cardash.CarDashApp
    private val telemetrist = app.telemetrist
    private val preferences = app.preferencesManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Engine state tracking
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
    
    private val _speedGps = MutableStateFlow(0)
    val speedGps = _speedGps.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude = _longitude.asStateFlow()

    private val _gForceX = MutableStateFlow(0f)
    val gForceX = _gForceX.asStateFlow()

    private val _gForceY = MutableStateFlow(0f)
    val gForceY = _gForceY.asStateFlow()

    private val _gForceZ = MutableStateFlow(0f)
    val gForceZ = _gForceZ.asStateFlow()
    
    private val _averageSpeed = MutableStateFlow(0)
    val averageSpeed = _averageSpeed.asStateFlow()
    
    private val _fuelLevelHistory = MutableStateFlow<List<Int>>(emptyList())
    val fuelLevelHistory = _fuelLevelHistory.asStateFlow()
    
    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance = _totalDistance.asStateFlow()
    
    private val _totalFuelConsumed = MutableStateFlow(0.0)
    val totalFuelConsumed = _totalFuelConsumed.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()
    
    // Logging preference flow for UI
    private val _verboseLoggingEnabled = MutableStateFlow(preferences.isVerboseLoggingEnabled())
    val verboseLoggingEnabled = _verboseLoggingEnabled.asStateFlow()

    init {
        // Observe connection status from OBDService
        // Observe the high-fidelity Telemetry Reactor (1.0s refresh)
        telemetrist.state
            .onEach { state ->
                // Map reactor status to UI connection state
                _connectionState.value = when(state.connectionStatus) {
                    com.fuseforge.cardash.model.TelemetryStatus.ACTIVE -> ConnectionState.Connected
                    com.fuseforge.cardash.model.TelemetryStatus.CONNECTING, 
                    com.fuseforge.cardash.model.TelemetryStatus.HANDSHAKING -> ConnectionState.Connecting
                    com.fuseforge.cardash.model.TelemetryStatus.ERROR -> ConnectionState.Failed("Connection error")
                    else -> ConnectionState.Disconnected
                }
                
                _rpm.value = state.rpm
                _speed.value = state.speedKph
                _engineLoad.value = state.engineLoad
                _coolantTemp.value = state.coolantTemp
                _fuelLevel.value = state.fuelLevel
                _intakeAirTemp.value = state.intakeAirTemp
                _throttlePosition.value = state.throttlePos
                _baroPressure.value = state.baroPressure
                _batteryVoltage.value = state.batteryVoltage
                _engineRunning.value = state.isEngineRunning
                _speedGps.value = (state.gpsSpeedMps * 3.6f).toInt()
                _gForceX.value = state.gForceX
                _gForceY.value = state.gForceY
                _gForceZ.value = state.gForceZ
            }
            .launchIn(viewModelScope)
            
        // Observe cumulative metrics from DB
        val dao = AppDatabase.getDatabase(context).obdLogDao()
        
        dao.getTotalDistanceFlow()
            .onEach { distance ->
                if (distance != null) {
                    _totalDistance.value = distance
                }
            }
            .launchIn(viewModelScope)
            
        dao.getAllHeartbeatsFlow()
            .onEach { heartbeats ->
                var totalFuel = 0.0
                var currentFuel = -1
                
                for (heartbeat in heartbeats) {
                    val fuel = heartbeat.fuelLevel ?: continue
                    if (currentFuel == -1) {
                        currentFuel = fuel
                        continue
                    }
                    
                    val drop = currentFuel - fuel
                    if (drop > 0) {
                        // Using the 0.4L per 1% drop formula established in SUR-62
                        totalFuel += drop * 0.4
                    }
                    currentFuel = fuel
                }
                
                _totalFuelConsumed.value = totalFuel
            }
            .launchIn(viewModelScope)
    }

    /**
     * Connect to OBD device and start data collection
     */
    fun connectToDevice(address: String) {
        viewModelScope.launch {
            // Re-use the service intent logic to start the reactor in the background
            val intent = android.content.Intent(app, com.fuseforge.cardash.services.CarDashDataCollectorService::class.java).apply {
                action = com.fuseforge.cardash.services.CarDashDataCollectorService.ACTION_START_SERVICE
                putExtra(com.fuseforge.cardash.services.CarDashDataCollectorService.EXTRA_DEVICE_ADDRESS, address)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
    }

    /**
     * Disconnect from OBD device and stop data collection
     */
    fun disconnect() {
        viewModelScope.launch {
            app.telemetrist.stop()
            
            // Reset all state values
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
        }
    }

    /**
     * Get paired Bluetooth devices
     */
    suspend fun getPairedDevices(): Set<BluetoothDevice> {
        return withContext(Dispatchers.IO) {
            app.bluetoothManager.getPairedDevices()
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
}

class MetricViewModelFactory(private val context: android.content.Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}