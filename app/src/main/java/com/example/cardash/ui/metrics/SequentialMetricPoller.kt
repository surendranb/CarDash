package com.example.cardash.ui.metrics

import android.util.Log
import com.example.cardash.data.db.OBDCombinedReading
import com.example.cardash.data.db.OBDDataType
import com.example.cardash.data.preferences.AppPreferences
import com.example.cardash.services.obd.OBDService
import com.example.cardash.services.obd.OBDServiceWithDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private const val TAG = "SequentialMetricPoller"

/**
 * Handles sequential polling of OBD metrics for improved data quality and reduced storage usage.
 * Each metric is polled one after another with adequate time for reliable responses.
 */
class SequentialMetricPoller(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics,
    private val preferences: AppPreferences,
    private val obdLogDao: com.example.cardash.data.db.OBDLogDao,
    private val viewModelScope: CoroutineScope,
    
    // State flows from view model
    private val connectionState: MutableStateFlow<MetricViewModel.ConnectionState>,
    private val engineRunning: MutableStateFlow<Boolean>,
    private val rpm: MutableStateFlow<Int>,
    private val speed: MutableStateFlow<Int>,
    private val engineLoad: MutableStateFlow<Int>,
    private val coolantTemp: MutableStateFlow<Int>,
    private val fuelLevel: MutableStateFlow<Int>,
    private val intakeAirTemp: MutableStateFlow<Int>,
    private val throttlePosition: MutableStateFlow<Int>,
    private val fuelPressure: MutableStateFlow<Int>,
    private val baroPressure: MutableStateFlow<Int>,
    private val batteryVoltage: MutableStateFlow<Float>,
    private val errorMessage: kotlinx.coroutines.flow.MutableSharedFlow<String>
) {
    // Keep track of cycle count to determine which metrics to poll in each cycle
    private var cycleCount = 0
    
    // Track last values for comparison to detect significant changes
    private var lastStoredValues = OBDCombinedReading(
        timestamp = Date(),
        sessionId = obdServiceWithDiagnostics.getSessionId() // Add the required sessionId parameter
    )
    
    // Track when the last database write happened
    private var lastStorageTime = 0L
    
    // Flag to control polling
    private var isPolling = false
    
    /**
     * Start sequential polling of all metrics
     */
    fun startPolling() {
        if (isPolling) return
        
        isPolling = true
        cycleCount = 0
        lastStorageTime = System.currentTimeMillis() - preferences.storageFrequencyMs
        
        viewModelScope.launch {
            while (connectionState.value is MetricViewModel.ConnectionState.Connected && isPolling) {
                try {
                    // Calculate the complete poll cycle time
                    // With 300ms per command, ensure we have adequate time
                    val cycleDuration = preferences.dataCollectionFrequencyMs
                    val startTime = System.currentTimeMillis()
                    
                    // Performance logging
                    Log.d(TAG, "Starting poll cycle #$cycleCount")
                    
                    // Always check if engine is running first
                    val engineState = pollEngineState()
                    
                    // Poll high-priority metrics first (always poll these)
                    pollRPM()
                    pollThrottlePosition()
                    
                    // Poll driving metrics when engine is running
                    if (engineState) {
                        pollSpeed()
                        pollEngineLoad()
                    } else {
                        // Reset engine-dependent metrics when engine is off
                        speed.value = 0
                        engineLoad.value = 0
                    }
                    
                    // Poll temperature metrics every other cycle
                    if (cycleCount % 2 == 0) {
                        pollCoolantTemp()
                        
                        if (engineState) {
                            pollFuelPressure()
                        } else {
                            fuelPressure.value = 0
                        }
                    }
                    
                    // Poll slower-changing metrics less frequently
                    if (cycleCount % 4 == 0) {
                        pollIntakeAirTemp()
                        pollBatteryVoltage()
                    }
                    
                    // Poll very slow metrics rarely
                    if (cycleCount % 8 == 0) {
                        pollBaroPressure()
                        pollFuelLevel()
                    }
                    
                    // Decide whether to write to database based on:
                    // 1. Time elapsed since last write
                    // 2. Significant metric changes
                    val timeSinceLastStorage = System.currentTimeMillis() - lastStorageTime
                    val shouldWrite = shouldWriteToDatabase(timeSinceLastStorage)
                    
                    if (shouldWrite) {
                        storeCombinedReading()
                        lastStorageTime = System.currentTimeMillis()
                    }
                    
                    // Calculate remaining time in cycle
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val remainingTime = cycleDuration - elapsedTime
                    
                    // Log performance metrics
                    Log.d(TAG, "Cycle completed in ${elapsedTime}ms, sleeping for ${remainingTime}ms")
                    
                    // Sleep for remaining time to maintain consistent polling rate
                    // If cycle took longer than expected, still delay a minimum amount
                    if (remainingTime > 0) {
                        delay(remainingTime)
                    } else {
                        delay(100) // Minimum delay to avoid tight loops
                    }
                    
                    cycleCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling cycle: ${e.message}")
                    errorMessage.emit("Polling error: ${e.message}")
                    delay(1000) // Wait before trying again
                }
            }
            
            isPolling = false
        }
    }
    
    fun stopPolling() {
        isPolling = false
    }
    
    /**
     * Determines if engine is running based on RPM
     */
    private suspend fun pollEngineState(): Boolean {
        try {
            val command = "01 0C" // RPM command
            val response = obdService.sendCommand(command)
            val rpmValue = parseRpm(response)
            
            // Get current engine state
            val currentEngineState = engineRunning.value
            
            // Determine new state based on RPM
            val newEngineState = rpmValue > 0
            
            // If state is changing from running to off, do additional verification
            if (currentEngineState && !newEngineState) {
                Log.d(TAG, "Engine may be turning off. RPM: $rpmValue. Verifying...")
                
                // Try a second RPM reading to confirm
                delay(300)
                val confirmResponse = obdService.sendCommand(command)
                val confirmRpm = parseRpm(confirmResponse)
                
                if (confirmRpm > 0) {
                    // RPM now shows activity - engine still running
                    Log.d(TAG, "Second check shows engine still running: $confirmRpm RPM")
                    engineRunning.value = true
                    return true
                }
                
                // Engine is confirmed off - reset dynamic metrics
                Log.d(TAG, "Engine confirmed OFF")
                resetDynamicMetrics()
            }
            
            // Only log state change if it's different
            if (currentEngineState != newEngineState) {
                Log.d(TAG, "Engine state changed to: ${if (newEngineState) "RUNNING" else "OFF"}")
            }
            
            engineRunning.value = newEngineState
            return newEngineState
        } catch (e: Exception) {
            Log.e(TAG, "Engine state check error: ${e.message}")
            return engineRunning.value // Keep current state on error
        }
    }
    
    /**
     * Reset all metrics that should be zero when engine is off
     */
    private fun resetDynamicMetrics() {
        Log.d(TAG, "Resetting dynamic metrics to zero")
        rpm.value = 0
        speed.value = 0
        engineLoad.value = 0
        throttlePosition.value = 0
        fuelPressure.value = 0
    }
    
    /**
     * Determines if we should write to database based on time and value changes
     */
    private fun shouldWriteToDatabase(timeSinceLastStorage: Long): Boolean {
        // Always write if it's been longer than the storage frequency
        if (timeSinceLastStorage >= preferences.storageFrequencyMs) {
            return true
        }
        
        // Write more frequently when engine is running and values are changing
        if (engineRunning.value) {
            // Check if important metrics have changed significantly
            if (Math.abs(rpm.value - (lastStoredValues.rpm ?: 0)) > 100) return true
            if (Math.abs(speed.value - (lastStoredValues.speed ?: 0)) > 5) return true
            if (Math.abs(throttlePosition.value - (lastStoredValues.throttlePosition ?: 0)) > 5) return true
            if (Math.abs(coolantTemp.value - (lastStoredValues.coolantTemp ?: 0)) > 2) return true
        }
        
        return false
    }
    
    /**
     * Store all current metrics in the database
     */
    private suspend fun storeCombinedReading() {
        val combinedReading = OBDCombinedReading(
            timestamp = Date(),
            sessionId = obdServiceWithDiagnostics.getSessionId(),
            rpm = rpm.value,
            speed = speed.value,
            engineLoad = engineLoad.value,
            coolantTemp = coolantTemp.value,
            fuelLevel = fuelLevel.value,
            intakeAirTemp = intakeAirTemp.value,
            throttlePosition = throttlePosition.value,
            fuelPressure = fuelPressure.value,
            baroPressure = baroPressure.value,
            batteryVoltage = batteryVoltage.value
        )
        
        try {
            obdLogDao.insertCombinedReading(combinedReading)
            lastStoredValues = combinedReading
            Log.d(TAG, "Stored combined reading")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store combined reading: ${e.message}")
            errorMessage.emit("Database error: ${e.message}")
        }
    }
    
    /**
     * Poll RPM - this is our primary metric
     */
    private suspend fun pollRPM() {
        try {
            // If engine is definitively off, don't need to poll
            if (!engineRunning.value) {
                rpm.value = 0
                return
            }
            
            val command = "01 0C"
            val response = obdService.sendCommand(command)
            val rpmValue = parseRpm(response)
            
            // Log debugging info
            if (preferences.verboseLoggingEnabled) {
                Log.d(TAG, "RPM Raw Response: $response")
                Log.d(TAG, "Parsed RPM: $rpmValue")
            }
            
            // Only update the RPM value
            rpm.value = rpmValue
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = rpmValue.toString(),
                    dataType = OBDDataType.RPM
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "RPM collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = "01 0C",
                errorMessage = "RPM read error: ${e.message}",
                dataType = OBDDataType.RPM
            )
            
            errorMessage.emit("RPM error: ${e.message}")
        }
    }
    
    /**
     * Poll Speed - only meaningful when engine is running
     */
    private suspend fun pollSpeed() {
        try {
            val command = OBDService.SPEED_COMMAND
            val response = obdService.sendCommand(command)
            val speedValue = obdService.parseSpeedResponse(response)
            
            speed.value = speedValue
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = speedValue.toString(),
                    dataType = OBDDataType.SPEED
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Speed collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.SPEED_COMMAND,
                errorMessage = "Speed read error: ${e.message}",
                dataType = OBDDataType.SPEED
            )
            
            errorMessage.emit("Speed error: ${e.message}")
        }
    }
    
    /**
     * Poll Engine Load - only meaningful when engine is running
     */
    private suspend fun pollEngineLoad() {
        try {
            val command = OBDService.ENGINE_LOAD_COMMAND
            val response = obdService.sendCommand(command)
            val load = obdService.getEngineLoad()
            
            engineLoad.value = load
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = load.toString(),
                    dataType = OBDDataType.ENGINE_LOAD
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine load collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.ENGINE_LOAD_COMMAND,
                errorMessage = "Engine load read error: ${e.message}",
                dataType = OBDDataType.ENGINE_LOAD
            )
            
            errorMessage.emit("Engine load error: ${e.message}")
        }
    }
    
    /**
     * Poll Coolant Temperature - important for monitoring engine health
     */
    private suspend fun pollCoolantTemp() {
        try {
            val command = OBDService.COOLANT_TEMP_COMMAND
            val response = obdService.sendCommand(command)
            val temp = obdService.getCoolantTemp()
            
            coolantTemp.value = temp
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = temp.toString(),
                    dataType = OBDDataType.COOLANT_TEMP
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Coolant temp collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.COOLANT_TEMP_COMMAND,
                errorMessage = "Coolant temp read error: ${e.message}",
                dataType = OBDDataType.COOLANT_TEMP
            )
            
            errorMessage.emit("Coolant temperature error: ${e.message}")
        }
    }
    
    /**
     * Poll Fuel Level - changes very slowly
     */
    private suspend fun pollFuelLevel() {
        try {
            val command = OBDService.FUEL_LEVEL_COMMAND
            val response = obdService.sendCommand(command)
            val level = obdService.getFuelLevel()
            
            fuelLevel.value = level
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = level.toString(),
                    dataType = OBDDataType.FUEL_LEVEL
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fuel level collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.FUEL_LEVEL_COMMAND,
                errorMessage = "Fuel level read error: ${e.message}",
                dataType = OBDDataType.FUEL_LEVEL
            )
            
            errorMessage.emit("Fuel level error: ${e.message}")
        }
    }
    
    /**
     * Poll Intake Air Temperature
     */
    private suspend fun pollIntakeAirTemp() {
        try {
            val command = OBDService.INTAKE_AIR_TEMP_COMMAND
            val response = obdService.sendCommand(command)
            val temp = obdService.getIntakeAirTemp()
            
            intakeAirTemp.value = temp
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = temp.toString(),
                    dataType = OBDDataType.INTAKE_AIR_TEMP
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Intake air temp collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.INTAKE_AIR_TEMP_COMMAND,
                errorMessage = "Intake air temp read error: ${e.message}",
                dataType = OBDDataType.INTAKE_AIR_TEMP
            )
            
            errorMessage.emit("Intake air temperature error: ${e.message}")
        }
    }
    
    /**
     * Poll Throttle Position - important for driveability monitoring
     */
    private suspend fun pollThrottlePosition() {
        try {
            // Reset to zero if engine is definitely off
            if (!engineRunning.value) {
                throttlePosition.value = 0
                return
            }
            
            val command = OBDService.THROTTLE_POSITION_COMMAND
            val response = obdService.sendCommand(command)
            val position = obdService.getThrottlePosition()
            
            throttlePosition.value = position
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = position.toString(),
                    dataType = OBDDataType.THROTTLE_POSITION
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Throttle position collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.THROTTLE_POSITION_COMMAND,
                errorMessage = "Throttle position read error: ${e.message}",
                dataType = OBDDataType.THROTTLE_POSITION
            )
            
            errorMessage.emit("Throttle position error: ${e.message}")
        }
    }
    
    /**
     * Poll Fuel Pressure - only meaningful when engine is running
     */
    private suspend fun pollFuelPressure() {
        try {
            val command = OBDService.FUEL_PRESSURE_COMMAND
            val response = obdService.sendCommand(command)
            
            try {
                val pressure = obdService.getFuelPressure()
                fuelPressure.value = pressure
                
                // Only log command if verbose logging is enabled
                if (preferences.verboseLoggingEnabled) {
                    obdServiceWithDiagnostics.logCommand(
                        command = command,
                        response = response,
                        parsedValue = pressure.toString(),
                        dataType = OBDDataType.FUEL_PRESSURE
                    )
                }
            } catch (pe: Exception) {
                // Always log parsing errors
                obdServiceWithDiagnostics.logError(
                    command = command,
                    errorMessage = "Fuel pressure parse error: ${pe.message}",
                    dataType = OBDDataType.FUEL_PRESSURE
                )
                
                // Try fallback parsing method
                val hexPattern = "([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
                val matches = hexPattern.findAll(response).map { it.value }.toList()
                
                if (matches.size >= 3) {
                    // Try to find fuel pressure data
                    for (i in 0 until matches.size - 1) {
                        if (matches[i].equals("0A", ignoreCase = true)) {
                            val valueHex = matches[i + 1]
                            val value = valueHex.toIntOrNull(16) ?: 0
                            val kPa = value * 3
                            fuelPressure.value = kPa
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fuel pressure collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.FUEL_PRESSURE_COMMAND,
                errorMessage = "Fuel pressure read error: ${e.message}",
                dataType = OBDDataType.FUEL_PRESSURE
            )
            
            errorMessage.emit("Fuel pressure error: ${e.message}")
        }
    }
    
    /**
     * Poll Barometric Pressure - changes very slowly
     */
    private suspend fun pollBaroPressure() {
        try {
            val command = OBDService.BARO_PRESSURE_COMMAND
            val response = obdService.sendCommand(command)
            val pressure = obdService.getBaroPressure()
            
            baroPressure.value = pressure
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = pressure.toString(),
                    dataType = OBDDataType.BARO_PRESSURE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Barometric pressure collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.BARO_PRESSURE_COMMAND,
                errorMessage = "Barometric pressure read error: ${e.message}",
                dataType = OBDDataType.BARO_PRESSURE
            )
            
            errorMessage.emit("Barometric pressure error: ${e.message}")
        }
    }
    
    /**
     * Poll Battery Voltage
     */
    private suspend fun pollBatteryVoltage() {
        try {
            val command = OBDService.BATTERY_VOLTAGE_COMMAND
            val response = obdService.sendCommand(command)
            val voltage = obdService.getBatteryVoltage()
            
            batteryVoltage.value = voltage
            
            // Only log command if verbose logging is enabled
            if (preferences.verboseLoggingEnabled) {
                obdServiceWithDiagnostics.logCommand(
                    command = command,
                    response = response,
                    parsedValue = voltage.toString(),
                    dataType = OBDDataType.BATTERY_VOLTAGE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Battery voltage collection error: ${e.message}")
            
            // Always log errors
            obdServiceWithDiagnostics.logError(
                command = OBDService.BATTERY_VOLTAGE_COMMAND,
                errorMessage = "Battery voltage read error: ${e.message}",
                dataType = OBDDataType.BATTERY_VOLTAGE
            )
            
            errorMessage.emit("Battery voltage error: ${e.message}")
        }
    }
    
    /**
     * Parse RPM response from OBD
     */
    private fun parseRpm(response: String): Int {
        // Improved RPM parsing with better error handling
        if (preferences.verboseLoggingEnabled) {
            Log.d(TAG, "Parsing RPM from: $response")
        }
        
        try {
            // Try pattern matching specifically for RPM data
            val pattern = "(?:41 0C|410C) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = pattern.find(response)
            
            if (matchResult != null) {
                val (hexA, hexB) = matchResult.destructured
                val byteA = hexA.toIntOrNull(16) ?: 0
                val byteB = hexB.toIntOrNull(16) ?: 0
                val rpm = ((byteA * 256) + byteB) / 4
                
                if (preferences.verboseLoggingEnabled) {
                    Log.d(TAG, "Regex parsed RPM: $rpm (A=$byteA, B=$byteB)")
                }
                return rpm
            }
            
            // Fallback to simple space splitting if regex fails
            val values = response.split(" ")
            if (values.size >= 3 && values[0].equals("41", ignoreCase = true) && 
                values[1].equals("0C", ignoreCase = true)) {
                
                val byteA = values[2].toIntOrNull(16) ?: 0
                val byteB = if (values.size > 3) values[3].toIntOrNull(16) ?: 0 else 0
                val rpm = ((byteA * 256) + byteB) / 4
                
                if (preferences.verboseLoggingEnabled) {
                    Log.d(TAG, "Fallback parsed RPM: $rpm (A=$byteA, B=$byteB)")
                }
                return rpm
            }
            
            // If we get here, we couldn't parse the response at all
            Log.w(TAG, "Could not parse RPM response: $response")
            return 0
        } catch (e: Exception) {
            Log.e(TAG, "Exception parsing RPM: ${e.message}")
            return 0
        }
    }
    
    companion object {
        // Constants can be added here if needed in the future
    }
}