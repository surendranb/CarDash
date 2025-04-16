package com.example.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import com.example.cardash.services.obd.OBDService
import com.example.cardash.services.obd.OBDServiceWithDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MetricViewModel(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics
) : ViewModel() {

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

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    // Engine state check - separate function that can be used by all metrics
    private suspend fun checkEngineRunning(): Boolean {
        try {
            val command = "01 0C" // RPM command
            val response = obdService.sendCommand(command)
            val rpmValue = parseRpm(response)
            
            // Update engine state based on RPM
            val isRunning = rpmValue > 0
            _engineRunning.value = isRunning
            
            return isRunning
        } catch (e: Exception) {
            println("Error checking engine state: ${e.message}")
            return false
        }
    }

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _errorMessage.emit("Connecting to $deviceAddress...")
            
            when (val result = obdService.connect(deviceAddress)) {
                is OBDService.ConnectionResult.Success -> {
                    _connectionState.value = ConnectionState.Connected
                    _errorMessage.emit("Successfully connected to OBD2 adapter")
                    
                    // First check engine status
                    val engineRunning = checkEngineRunning()
                    println("Initial engine status: ${if (engineRunning) "Running" else "Off"}")
                    
                    // Start all metric collections - including the previously missing ones
                    startRpmCollection()
                    startSpeedCollection()
                    startEngineLoadCollection()
                    startCoolantTempCollection()
                    startFuelLevelCollection()
                    startIntakeAirTempCollection()
                    startThrottlePositionCollection()
                    startFuelPressureCollection()
                    startBaroPressureCollection()
                    startBatteryVoltageCollection()
                }
                is OBDService.ConnectionResult.Error -> {
                    println("Connection failed: ${result.message}")
                    _connectionState.value = ConnectionState.Failed(result.message)
                    _errorMessage.emit("Connection error: ${result.message}")
                    // Add retry logic if needed
                }
            }
        }
    }

    private fun startRpmCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    val command = "01 0C"
                    val response = obdService.sendCommand(command)
                    
                    // More detailed logging to debug RPM issues
                    println("RPM Raw Response: $response")
                    
                    val rpmValue = parseRpm(response)
                    
                    // Log the parsed value
                    println("Parsed RPM: $rpmValue")
                    
                    // Update engine state based on RPM
                    _engineRunning.value = rpmValue > 0
                    
                    _rpm.value = rpmValue
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = rpmValue.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.RPM
                    )
                    
                    // Store combined readings periodically
                    storeCombinedReading()
                    
                    delay(500) // Poll every 500ms
                } catch (e: Exception) {
                    println("RPM collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = "01 0C",
                        errorMessage = "RPM read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.RPM
                    )
                    _errorMessage.emit("RPM read error: ${e.message}")
                    // Don't disconnect on errors - just continue trying
                    delay(2000) // Wait a bit before retrying
                }
            }
        }
    }

    private fun parseRpm(response: String): Int {
        // Improved RPM parsing with better error handling
        println("Parsing RPM from: $response")
        
        try {
            // Try pattern matching specifically for RPM data
            val pattern = "(?:41 0C|410C) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = pattern.find(response)
            
            if (matchResult != null) {
                val (hexA, hexB) = matchResult.destructured
                val byteA = hexA.toIntOrNull(16) ?: 0
                val byteB = hexB.toIntOrNull(16) ?: 0
                val rpm = ((byteA * 256) + byteB) / 4
                println("Regex parsed RPM: $rpm (A=$byteA, B=$byteB)")
                return rpm
            }
            
            // Fallback to simple space splitting if regex fails
            val values = response.split(" ")
            if (values.size >= 3 && values[0].equals("41", ignoreCase = true) && 
                values[1].equals("0C", ignoreCase = true)) {
                
                val byteA = values[2].toIntOrNull(16) ?: 0
                val byteB = if (values.size > 3) values[3].toIntOrNull(16) ?: 0 else 0
                val rpm = ((byteA * 256) + byteB) / 4
                println("Fallback parsed RPM: $rpm (A=$byteA, B=$byteB)")
                return rpm
            }
            
            // If we get here, we couldn't parse the response at all
            println("Could not parse RPM response: $response")
            return 0
        } catch (e: Exception) {
            println("Exception parsing RPM: ${e.message}")
            return 0
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            obdService.disconnect()
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
        }
    }

    suspend fun getPairedDevices(): Set<BluetoothDevice> {
        return withContext(Dispatchers.IO) {
            obdService.bluetoothManager.getPairedDevices()
        }
    }

    fun checkPermissions(): String? {
        return null // OBDService doesn't have checkPermissions()
    }

    // Engine Load - Only when engine is running
    private fun startEngineLoadCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Check engine state before collecting engine-dependent metrics
                    if (!_engineRunning.value) {
                        // Engine is off - set to 0 and check less frequently
                        _engineLoad.value = 0
                        delay(2000) // Check less frequently when engine off
                        continue
                    }
                    
                    val command = OBDService.ENGINE_LOAD_COMMAND
                    val response = obdService.sendCommand(command)
                    val load = obdService.getEngineLoad()
                    _engineLoad.value = load
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = load.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.ENGINE_LOAD
                    )
                    
                    delay(1000) // Poll every second
                } catch (e: Exception) {
                    println("Engine load collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.ENGINE_LOAD_COMMAND,
                        errorMessage = "Engine load read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.ENGINE_LOAD
                    )
                    _errorMessage.emit("Engine load read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Battery voltage is available even with engine off
    private fun startBatteryVoltageCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    val command = OBDService.BATTERY_VOLTAGE_COMMAND
                    val response = obdService.sendCommand(command)
                    val voltage = obdService.getBatteryVoltage()
                    _batteryVoltage.value = voltage
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = voltage.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.BATTERY_VOLTAGE
                    )
                    
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    println("Battery voltage collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.BATTERY_VOLTAGE_COMMAND,
                        errorMessage = "Battery voltage read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.BATTERY_VOLTAGE
                    )
                    _errorMessage.emit("Battery voltage read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Speed - Only when engine is running
    private fun startSpeedCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Check engine state before collecting engine-dependent metrics
                    if (!_engineRunning.value) {
                        // Engine is off - set to 0 and check less frequently
                        _speed.value = 0
                        delay(2000) // Check less frequently when engine off
                        continue
                    }
                    
                    val command = OBDService.SPEED_COMMAND
                    val response = obdService.sendCommand(command)
                    val speedValue = obdService.parseSpeedResponse(response)
                    _speed.value = speedValue
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = speedValue.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.SPEED
                    )
                    
                    delay(1000) // Poll every second
                } catch (e: Exception) {
                    println("Speed collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.SPEED_COMMAND,
                        errorMessage = "Speed read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.SPEED
                    )
                    _errorMessage.emit("Speed read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Coolant temperature - Can read last value when engine off
    private fun startCoolantTempCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // When engine is off, poll less frequently but still collect
                    val pollInterval = if (_engineRunning.value) 2000L else 5000L
                    
                    val command = OBDService.COOLANT_TEMP_COMMAND
                    val response = obdService.sendCommand(command)
                    val temp = obdService.getCoolantTemp()
                    _coolantTemp.value = temp
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = temp.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.COOLANT_TEMP
                    )
                    
                    delay(pollInterval) // Adjust polling based on engine state
                } catch (e: Exception) {
                    println("Coolant temp collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.COOLANT_TEMP_COMMAND,
                        errorMessage = "Coolant temp read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.COOLANT_TEMP
                    )
                    _errorMessage.emit("Coolant temp read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Fuel level - Can read when engine off
    private fun startFuelLevelCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Fuel level can be read with engine off, but poll less frequently
                    val pollInterval = if (_engineRunning.value) 5000L else 10000L
                    
                    val command = OBDService.FUEL_LEVEL_COMMAND
                    val response = obdService.sendCommand(command)
                    val level = obdService.getFuelLevel()
                    _fuelLevel.value = level
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = level.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.FUEL_LEVEL
                    )
                    
                    delay(pollInterval) // Adjust polling based on engine state
                } catch (e: Exception) {
                    println("Fuel level collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.FUEL_LEVEL_COMMAND,
                        errorMessage = "Fuel level read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.FUEL_LEVEL
                    )
                    _errorMessage.emit("Fuel level read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Intake air temp - Relevant but less dynamic when engine off
    private fun startIntakeAirTempCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // When engine is off, poll less frequently but still collect
                    val pollInterval = if (_engineRunning.value) 2000L else 5000L
                    
                    val command = OBDService.INTAKE_AIR_TEMP_COMMAND
                    val response = obdService.sendCommand(command)
                    val temp = obdService.getIntakeAirTemp()
                    _intakeAirTemp.value = temp
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = temp.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.INTAKE_AIR_TEMP
                    )
                    
                    delay(pollInterval) // Adjust polling based on engine state
                } catch (e: Exception) {
                    println("Intake air temp collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.INTAKE_AIR_TEMP_COMMAND,
                        errorMessage = "Intake air temp read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.INTAKE_AIR_TEMP
                    )
                    _errorMessage.emit("Intake air temp read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Throttle position - Only when engine is running
    private fun startThrottlePositionCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Check engine state before collecting engine-dependent metrics
                    if (!_engineRunning.value) {
                        // Engine is off - set to 0 and check less frequently
                        _throttlePosition.value = 0
                        delay(2000) // Check less frequently when engine off
                        continue
                    }
                    
                    val command = OBDService.THROTTLE_POSITION_COMMAND
                    val response = obdService.sendCommand(command)
                    val position = obdService.getThrottlePosition()
                    _throttlePosition.value = position
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = position.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.THROTTLE_POSITION
                    )
                    
                    delay(1000) // Poll every second
                } catch (e: Exception) {
                    println("Throttle position collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.THROTTLE_POSITION_COMMAND,
                        errorMessage = "Throttle position read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.THROTTLE_POSITION
                    )
                    _errorMessage.emit("Throttle position read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Fuel pressure - Only when engine is running
    private fun startFuelPressureCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Check engine state before collecting engine-dependent metrics
                    if (!_engineRunning.value) {
                        // Engine is off - set to 0 and check less frequently
                        _fuelPressure.value = 0
                        delay(2000) // Check less frequently when engine off
                        continue
                    }
                    
                    val command = OBDService.FUEL_PRESSURE_COMMAND
                    val response = obdService.sendCommand(command)
                    val pressure = obdService.getFuelPressure()
                    _fuelPressure.value = pressure
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = pressure.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.FUEL_PRESSURE
                    )
                    
                    delay(2000) // Poll every 2 seconds
                } catch (e: Exception) {
                    println("Fuel pressure collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.FUEL_PRESSURE_COMMAND,
                        errorMessage = "Fuel pressure read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.FUEL_PRESSURE
                    )
                    _errorMessage.emit("Fuel pressure read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    // Barometric pressure - Available with engine off (environmental)
    private fun startBaroPressureCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    // Barometric pressure can be read anytime, but less frequently when engine off
                    val pollInterval = if (_engineRunning.value) 5000L else 10000L
                    
                    val command = OBDService.BARO_PRESSURE_COMMAND
                    val response = obdService.sendCommand(command)
                    val pressure = obdService.getBaroPressure()
                    _baroPressure.value = pressure
                    
                    // Log the command and response
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = pressure.toString(),
                        dataType = com.example.cardash.data.db.OBDDataType.BARO_PRESSURE
                    )
                    
                    delay(pollInterval) // Adjust polling based on engine state
                } catch (e: Exception) {
                    println("Barometric pressure collection error: ${e.message}")
                    obdServiceWithDiagnostics.logError(
                        command = OBDService.BARO_PRESSURE_COMMAND,
                        errorMessage = "Barometric pressure read error: ${e.message}",
                        dataType = com.example.cardash.data.db.OBDDataType.BARO_PRESSURE
                    )
                    _errorMessage.emit("Barometric pressure read error: ${e.message}")
                    delay(2000) // Wait before retrying
                }
            }
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
    
    // Database access for storing combined readings
    private val database = com.example.cardash.data.db.AppDatabase.getDatabase(
        obdServiceWithDiagnostics.getContext()
    )
    private val obdLogDao = database.obdLogDao()
    
    // Last time we stored a combined reading (to avoid too frequent writes)
    private var lastCombinedReadingTime = System.currentTimeMillis() - 5000 // Start 5 seconds ago
    
    // Store all current metrics as a combined reading
    private suspend fun storeCombinedReading() {
        val currentTime = System.currentTimeMillis()
        // Only store every 5 seconds to avoid too many database writes
        if (currentTime - lastCombinedReadingTime >= 5000) {
            lastCombinedReadingTime = currentTime
            
            val combinedReading = com.example.cardash.data.db.OBDCombinedReading(
                timestamp = java.util.Date(),
                sessionId = obdServiceWithDiagnostics.getSessionId(),
                rpm = _rpm.value,
                speed = _speed.value,
                engineLoad = _engineLoad.value,
                coolantTemp = _coolantTemp.value,
                fuelLevel = _fuelLevel.value,
                intakeAirTemp = _intakeAirTemp.value,
                throttlePosition = _throttlePosition.value,
                fuelPressure = _fuelPressure.value,
                baroPressure = _baroPressure.value,
                batteryVoltage = _batteryVoltage.value
            )
            
            try {
                obdLogDao.insertCombinedReading(combinedReading)
                println("Stored combined reading: $combinedReading")
            } catch (e: Exception) {
                println("Failed to store combined reading: ${e.message}")
                _errorMessage.emit("Database error: ${e.message}")
            }
        }
    }
}