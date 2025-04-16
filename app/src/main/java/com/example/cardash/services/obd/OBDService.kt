package com.example.cardash.services.obd

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose

/**
 * Data class representing an OBD command with callback
 */
data class OBDCommand(
    val command: String,
    val dataType: OBDDataType,
    val resultCallback: (Result<String>) -> Unit
)

/**
 * Enum class for OBD data types
 */
enum class OBDDataType {
    RPM,
    SPEED,
    ENGINE_LOAD,
    COOLANT_TEMP,
    FUEL_LEVEL,
    INTAKE_AIR_TEMP,
    THROTTLE_POSITION,
    FUEL_PRESSURE,
    BARO_PRESSURE,
    BATTERY_VOLTAGE,
    CONNECTION,
    INITIALIZATION,
    UNKNOWN
}

class OBDService(
    internal val bluetoothManager: BluetoothManager,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var socket: BluetoothSocket? = null
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set
    
    private val isRunning = AtomicBoolean(false)
    
    // Command queue channel
    private val commandChannel = Channel<OBDCommand>(Channel.BUFFERED)
    
    // Job for the command processor
    private var commandProcessorJob: Job? = null

    // Engine Load polling flow  
    val engineLoadFlow: Flow<Int> = createParameterFlow(
        ENGINE_LOAD_COMMAND,
        OBDDataType.ENGINE_LOAD,
        1000
    ) { response -> parseEngineLoadResponse(response) }

    // RPM polling flow
    val rpmFlow: Flow<Int> = createParameterFlow(
        "01 0C", 
        OBDDataType.RPM,
        500
    ) { response -> parseRPMResponse(response) }

    val speedFlow: Flow<Int> = createParameterFlow(
        SPEED_COMMAND,
        OBDDataType.SPEED,
        1000
    ) { response -> parseSpeedResponse(response) }
    
    val coolantTempFlow: Flow<Int> = createParameterFlow(
        COOLANT_TEMP_COMMAND,
        OBDDataType.COOLANT_TEMP,
        2000
    ) { response -> parseCoolantTempResponse(response) }
    
    val fuelLevelFlow: Flow<Int> = createParameterFlow(
        FUEL_LEVEL_COMMAND,
        OBDDataType.FUEL_LEVEL,
        5000
    ) { response -> parseFuelLevelResponse(response) }
    
    val intakeAirTempFlow: Flow<Int> = createParameterFlow(
        INTAKE_AIR_TEMP_COMMAND,
        OBDDataType.INTAKE_AIR_TEMP,
        2000
    ) { response -> parseIntakeAirTempResponse(response) }
    
    val throttlePositionFlow: Flow<Int> = createParameterFlow(
        THROTTLE_POSITION_COMMAND,
        OBDDataType.THROTTLE_POSITION,
        1000
    ) { response -> parseThrottlePositionResponse(response) }
    
    val fuelPressureFlow: Flow<Int> = createParameterFlow(
        FUEL_PRESSURE_COMMAND,
        OBDDataType.FUEL_PRESSURE,
        2000
    ) { response -> parseFuelPressureResponse(response) }
    
    val baroPressureFlow: Flow<Int> = createParameterFlow(
        BARO_PRESSURE_COMMAND,
        OBDDataType.BARO_PRESSURE,
        5000
    ) { response -> parseBaroPressureResponse(response) }
    
    val batteryVoltageFlow: Flow<Float> = createParameterFlow(
        BATTERY_VOLTAGE_COMMAND,
        OBDDataType.BATTERY_VOLTAGE,
        5000
    ) { response -> parseBatteryVoltageResponse(response) }

    /**
     * Generic function to create a parameter flow
     */
    private fun <T> createParameterFlow(
        command: String,
        dataType: OBDDataType,
        pollingIntervalMs: Long,
        parser: (String) -> T
    ): Flow<T> = callbackFlow {
        val job = ioScope.launch {
            while (isRunning.get()) {
                try {
                    val result = enqueueSendCommand(command, dataType)
                    send(parser(result))
                } catch (e: Exception) {
                    // Handle error but don't close the flow
                    println("Error in flow for ${dataType.name}: ${e.message}")
                }
                delay(pollingIntervalMs)
            }
        }
        
        awaitClose {
            job.cancel()
        }
    }

    sealed class ConnectionResult {
        data object Success : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
    }

    /**
     * Start the command processor
     */
    private fun startCommandProcessor() {
        if (commandProcessorJob != null && commandProcessorJob!!.isActive) return
        
        commandProcessorJob = ioScope.launch {
            println("OBDService: Command processor started")
            for (command in commandChannel) {
                if (!isRunning.get()) break
                
                try {
                    val result = sendCommandInternal(command.command)
                    command.resultCallback(Result.success(result))
                } catch (e: Exception) {
                    println("OBDService: Command failed: ${command.command} - ${e.message}")
                    command.resultCallback(Result.failure(e))
                }
                
                // Add a small delay between commands to give the adapter time to process
                delay(100)
            }
        }
    }

    /**
     * Stop the command processor
     */
    private fun stopCommandProcessor() {
        commandProcessorJob?.cancel()
        commandProcessorJob = null
    }

    /**
     * Enqueue a command to be sent via the command processor
     */
    private suspend fun enqueueSendCommand(command: String, dataType: OBDDataType): String {
        return suspendCancellableCoroutine { continuation ->
            val obdCommand = OBDCommand(command, dataType) { result ->
                if (result.isSuccess) {
                    continuation.resume(result.getOrThrow()) { }
                } else {
                    continuation.resumeWithException(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
            
            ioScope.launch {
                commandChannel.send(obdCommand)
            }
        }
    }

    suspend fun connect(deviceAddress: String): ConnectionResult {
        return withContext(ioScope.coroutineContext) {
            try {
                println("OBDService: Starting connection to $deviceAddress")
                
                // Verify device is paired
                if (!bluetoothManager.isDevicePaired(deviceAddress)) {
                    println("OBDService: Device $deviceAddress not paired")
                    return@withContext ConnectionResult.Error("Device not paired - please pair first in Bluetooth settings")
                }

                socket = bluetoothManager.createSocket(deviceAddress)
                    ?: return@withContext ConnectionResult.Error("Failed to create Bluetooth socket")
                
                println("Socket created, attempting connection...")
                try {
                    println("Attempting direct socket connect...")
                    socket?.connect() // Direct connect call
                    println("Socket connect call completed.")
                } catch (e: IOException) {
                    if (e.message?.contains("Device or resource busy") == true) {
                        return@withContext ConnectionResult.Error(
                            "Device already in use - close other OBD2 apps and try again"
                        )
                    }
                    throw e
                }
                // Verify socket connectivity
                if (socket?.isConnected != true) {
                    return@withContext ConnectionResult.Error("Socket not connected")
                }
                delay(1000) // Wait for connection stabilization
                
                inputStream = socket?.inputStream
                    ?: return@withContext ConnectionResult.Error("No input stream")
                outputStream = socket?.outputStream
                    ?: return@withContext ConnectionResult.Error("No output stream")
                
                 // Full OBD2 Initialization Sequence using the sequential command queue
                 try {
                    isRunning.set(true)
                    startCommandProcessor()
                    
                    println("OBDService: Sending ATZ (Reset)...")
                    enqueueSendCommand("ATZ", OBDDataType.INITIALIZATION)
                    delay(1500) // Delay after reset

                    println("OBDService: Sending ATE0 (Echo Off)...")
                    enqueueSendCommand("ATE0", OBDDataType.INITIALIZATION)
                    delay(200)

                    println("OBDService: Sending ATSP0 (Protocol Auto)...")
                    enqueueSendCommand("ATSP0", OBDDataType.INITIALIZATION) // Auto protocol
                    delay(3000)

                    // Verify communication is working with a simple command
                    val testResult = enqueueSendCommand("0100", OBDDataType.INITIALIZATION)
                    if (!testResult.contains("41 00") && !testResult.contains("4100")) {
                        println("OBDService: Initial test command failed, response: $testResult")
                        // We don't throw here, some vehicles might still work
                    }

                    println("OBDService: Initialization sequence completed.")
                } catch (e: Exception) {
                     println("OBDService: Initialization Error: ${e.javaClass.simpleName} - ${e.message}")
                     return@withContext ConnectionResult.Error("OBD2 initialization failed: ${e.message}")
                }
                
                println("Connected successfully!")
                isRunning.set(true)
                ConnectionResult.Success
            } catch (e: Exception) {
                println("OBDService Connection Failed:")
                e.printStackTrace()
                println("Socket state: ${socket?.isConnected}")
                println("Bluetooth enabled: ${bluetoothManager.isBluetoothEnabled()}")
                println("Error details: ${e.javaClass.simpleName} - ${e.message}")
                disconnect()
                ConnectionResult.Error(
                    when {
                        e is java.net.SocketTimeoutException -> "Connection timed out - verify adapter power and proximity"
                        e.message?.contains("read failed") == true -> "Communication error - try restarting adapter"
                        e.message?.contains("refused") == true -> "Connection refused - ensure adapter is not in use"
                        else -> e.message ?: "Connection failed - check adapter and try again"
                    }
                )
            }
        }
    }

    fun disconnect() {
        isRunning.set(false)
        stopCommandProcessor()
        
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            println("OBDService: Error during disconnect: ${e.message}")
        }
    }

    /**
     * Internal function to send a command directly to the device
     */
    private suspend fun sendCommandInternal(command: String): String {
        return withContext(Dispatchers.IO) {
            outputStream?.let { out ->
                try {
                    // Clear any pending data first
                    while (inputStream?.available() ?: 0 > 0) {
                        inputStream?.skip(inputStream?.available()?.toLong() ?: 0)
                    }
                    
                    // Send the command with carriage return
                    out.write("$command\r".toByteArray())
                    out.flush()
                    
                    // Wait for and collect the response
                    var response = ""
                    val startTime = System.currentTimeMillis()
                    val timeout = 2000L  // 2 second timeout
                    
                    // Read until prompt character or timeout
                    while (System.currentTimeMillis() - startTime < timeout) {
                        if (inputStream?.available() ?: 0 > 0) {
                            val buffer = ByteArray(1024)
                            val bytes = inputStream?.read(buffer) ?: 0
                            if (bytes > 0) {
                                response += String(buffer, 0, bytes)
                                // Check if we have the prompt character, indicating end of response
                                if (response.contains(">")) {
                                    break
                                }
                            }
                        }
                        // Small delay to prevent CPU thrashing
                        delay(10)
                    }
                    
                    if (response.isEmpty()) {
                        throw Exception("No response received for command: $command")
                    }
                    
                    return@withContext response.trim()
                } catch (e: Exception) {
                    throw Exception("Command failed: ${e.message}")
                }
            } ?: throw Exception("Not connected")
        }
    }

    /**
     * Public API for sending commands (uses the queue)
     */
    suspend fun sendCommand(command: String): String {
        return enqueueSendCommand(command, OBDDataType.UNKNOWN)
    }

    companion object {
        const val SPEED_COMMAND = "01 0D"
        const val ENGINE_LOAD_COMMAND = "01 04"
        const val COOLANT_TEMP_COMMAND = "01 05"
        const val FUEL_LEVEL_COMMAND = "01 2F"
        const val FUEL_PRESSURE_COMMAND = "01 0A"
        const val BARO_PRESSURE_COMMAND = "01 33"
        const val INTAKE_AIR_TEMP_COMMAND = "01 0F"
        const val THROTTLE_POSITION_COMMAND = "01 11"
        const val BATTERY_VOLTAGE_COMMAND = "01 42"
    }

    suspend fun getEngineLoad(): Int {
        val response = enqueueSendCommand(ENGINE_LOAD_COMMAND, OBDDataType.ENGINE_LOAD)
        return parseEngineLoadResponse(response)
    }

    private fun parseEngineLoadResponse(response: String): Int {
        // Look for the pattern "41 04 XX" where XX is the hex value
        val pattern = "(?:41 04|4104) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid engine load response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value * 100 / 255  // Convert to percentage
    }

    suspend fun getSpeed(): Int {
        val response = enqueueSendCommand(SPEED_COMMAND, OBDDataType.SPEED)
        return parseSpeedResponse(response)
    }

    fun parseSpeedResponse(response: String): Int {
        // Look for the pattern "41 0D XX" where XX is the hex value
        val pattern = "(?:41 0D|410D) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid speed response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        return hexValue.toIntOrNull(16) ?: 0  // Speed in km/h
    }

    private fun parseRPMResponse(response: String): Int {
        // Look for the pattern "41 0C XX YY" where XX and YY are hex values
        val pattern = "(?:41 0C|410C) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid RPM response format: $response")
            
        val (hexA, hexB) = matchResult.destructured
        val byteA = hexA.toIntOrNull(16) ?: 0
        val byteB = hexB.toIntOrNull(16) ?: 0
        return ((byteA * 256) + byteB) / 4  // Standard formula
    }

    suspend fun getCoolantTemp(): Int {
        val response = enqueueSendCommand(COOLANT_TEMP_COMMAND, OBDDataType.COOLANT_TEMP)
        return parseCoolantTempResponse(response)
    }

    private fun parseCoolantTempResponse(response: String): Int {
        // Look for the pattern "41 05 XX" where XX is the hex value
        val pattern = "(?:41 05|4105) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid coolant temp response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value - 40  // Convert to °C
    }

    suspend fun getFuelLevel(): Int {
        val response = enqueueSendCommand(FUEL_LEVEL_COMMAND, OBDDataType.FUEL_LEVEL)
        return parseFuelLevelResponse(response)
    }

    private fun parseFuelLevelResponse(response: String): Int {
        // Look for the pattern "41 2F XX" where XX is the hex value
        val pattern = "(?:41 2F|412F) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid fuel level response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value * 100 / 255  // Convert to percentage
    }

    suspend fun getIntakeAirTemp(): Int {
        val response = enqueueSendCommand(INTAKE_AIR_TEMP_COMMAND, OBDDataType.INTAKE_AIR_TEMP)
        return parseIntakeAirTempResponse(response)
    }

    private fun parseIntakeAirTempResponse(response: String): Int {
        // Look for the pattern "41 0F XX" where XX is the hex value
        val pattern = "(?:41 0F|410F) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid intake air temp response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value - 40  // Convert to °C
    }

    suspend fun getThrottlePosition(): Int {
        val response = enqueueSendCommand(THROTTLE_POSITION_COMMAND, OBDDataType.THROTTLE_POSITION)
        return parseThrottlePositionResponse(response)
    }

    private fun parseThrottlePositionResponse(response: String): Int {
        // Look for the pattern "41 11 XX" where XX is the hex value
        val pattern = "(?:41 11|4111) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid throttle position response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value * 100 / 255  // Convert to percentage
    }

    suspend fun getFuelPressure(): Int {
        val response = enqueueSendCommand(FUEL_PRESSURE_COMMAND, OBDDataType.FUEL_PRESSURE)
        return parseFuelPressureResponse(response)
    }

    private fun parseFuelPressureResponse(response: String): Int {
        // Look for the pattern "41 0A XX" where XX is the hex value
        val pattern = "(?:41 0A|410A) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid fuel pressure response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value * 3  // Convert to kPa (0-765 kPa)
    }

    suspend fun getBatteryVoltage(): Float {
        val response = enqueueSendCommand(BATTERY_VOLTAGE_COMMAND, OBDDataType.BATTERY_VOLTAGE)
        return parseBatteryVoltageResponse(response)
    }

    private fun parseBatteryVoltageResponse(response: String): Float {
        // Look for the pattern "41 42 XX" where XX is the hex value
        val pattern = "(?:41 42|4142) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid battery voltage response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        val value = hexValue.toIntOrNull(16) ?: 0
        return value / 10f  // Convert to volts
    }

    suspend fun getBaroPressure(): Int {
        val response = enqueueSendCommand(BARO_PRESSURE_COMMAND, OBDDataType.BARO_PRESSURE)
        return parseBaroPressureResponse(response)
    }

    private fun parseBaroPressureResponse(response: String): Int {
        // Look for the pattern "41 33 XX" where XX is the hex value
        val pattern = "(?:41 33|4133) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(response)
            ?: throw Exception("Invalid barometric pressure response format: $response")
            
        val hexValue = matchResult.groupValues[1]
        return hexValue.toIntOrNull(16) ?: 0  // Returns pressure in kPa
    }
}