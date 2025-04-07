package com.example.cardash.services.obd

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class OBDService(
    internal val bluetoothManager: BluetoothManager,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isRunning = false

    // Engine Load polling flow  
    val engineLoadFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getEngineLoad())
                delay(1000) // Poll every second
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    // RPM polling flow
    val rpmFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(readRPM())
                delay(500) // Poll every 500ms
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e -> 
        // Handle flow errors
    }

    val speedFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getSpeed())
                delay(1000) // Poll every second
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    sealed class ConnectionResult {
        data object Success : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
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
                
                 // Full OBD2 Initialization Sequence
                 var response = "" // Declare response here
                 try {
                    println("OBDService: Sending ATZ (Reset)...")
                    sendCommand("ATZ")
                    delay(1500) // Delay after reset

                    println("OBDService: Sending ATE0 (Echo Off)...")
                    sendCommand("ATE0")
                    delay(200)

                    println("OBDService: Sending ATSP0 (Protocol Auto)...")
                    sendCommand("ATSP0") // Revert to automatic protocol detection
                    delay(3000) // Increased delay after protocol set, matching DriveWise's implicit delay

                    // Removed ATL0, ATS0, ATH0 commands
                    // Removed consumeInitialBuffer() call
                    // Removed 0100 verification command and check

                    println("OBDService: Initialization sequence completed (DriveWise style).")
                } catch (e: Exception) {
                     println("OBDService: Initialization Error: ${e.javaClass.simpleName} - ${e.message}")
                     return@withContext ConnectionResult.Error("OBD2 initialization failed: ${e.message}")
                }
                // println("OBDService: Connect function - exiting try block") // Removed redundant log
                
                println("Connected successfully!")
                isRunning = true
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

    // Helper function to read and discard initial buffer data
    private suspend fun consumeInitialBuffer() {
        withContext(Dispatchers.IO) {
            try {
                // Give some time for potential initial data to arrive
                delay(500)
                val buffer = ByteArray(1024)
                var bytesRead = 0
                // Read available data without blocking indefinitely
                while (inputStream?.available() ?: 0 > 0) {
                    bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                         val discardedData = String(buffer, 0, bytesRead).trim()
                         println("OBDService: Discarded initial data: '$discardedData'")
                    }
                    // Add a small delay to prevent tight loop if data keeps coming
                    delay(50)
                }
                 println("OBDService: Finished consuming initial buffer.")
            } catch (e: IOException) {
                println("OBDService: Error consuming initial buffer: ${e.message}")
                // Don't necessarily fail connection here, proceed to verification
            } catch (e: Exception) {
                 println("OBDService: Unexpected error consuming initial buffer: ${e.message}")
            }
        }
    }


    fun disconnect() {
        isRunning = false
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            // Log disconnect error
        }
    }

    private suspend fun readRPM(): Int {
        try {
            val response = sendCommand("01 0C")
            return parseRPMResponse(response)   
        } catch (e: Exception) {
            throw Exception("RPM read failed: ${e.message}")
        }
    }

    suspend fun sendCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            outputStream?.let { out ->
                try {
                    out.write("$command\r".toByteArray())
                    out.flush()
                    
                    val buffer = ByteArray(1024)
                    val bytes = inputStream?.read(buffer) ?: 0
                    if (bytes <= 0) throw Exception("No response received")
                    
                    val response = String(buffer, 0, bytes).trim()
                    if (response.isEmpty()) throw Exception("Empty response")
                    
                    return@withContext response
                } catch (e: Exception) {
                    throw Exception("Command failed: ${e.message}")
                }
            } ?: throw Exception("Not connected")
        }
    }

    companion object {
        private const val SPEED_COMMAND = "01 0D"
        private const val ENGINE_LOAD_COMMAND = "01 04"
        private const val COOLANT_TEMP_COMMAND = "01 05"
        private const val FUEL_LEVEL_COMMAND = "01 2F"
        private const val FUEL_PRESSURE_COMMAND = "01 0A"
    private const val BARO_PRESSURE_COMMAND = "01 33"
    private const val INTAKE_AIR_TEMP_COMMAND = "01 0F"
    private const val THROTTLE_POSITION_COMMAND = "01 11"
    private const val BATTERY_VOLTAGE_COMMAND = "01 42"
    }

    suspend fun getEngineLoad(): Int {
        val response = sendCommand(ENGINE_LOAD_COMMAND)
        return parseEngineLoadResponse(response)
    }

    private fun parseEngineLoadResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return values[1].toIntOrNull(16) ?: 0 // Returns percentage (0-100)
        }
        throw Exception("Invalid engine load response")
    }

    suspend fun getSpeed(): Int {
        val response = sendCommand(SPEED_COMMAND)
        return parseSpeedResponse(response)
    }

    private fun parseSpeedResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return values[1].toIntOrNull(16) ?: 0 // Speed in km/h
        }
        throw Exception("Invalid speed response")
    }

    private fun parseRPMResponse(response: String): Int {
        // Clean the response: remove whitespace, newlines, and the '>' prompt character
        val cleanedResponse = response.replace(Regex("[\\s\\r\\n>]"), "")

        // Expected format after cleaning: 410CAB... (where A and B are hex bytes)
        // Check if the response starts with 410C and has enough length for data bytes
        if (cleanedResponse.startsWith("410C") && cleanedResponse.length >= 8) {
            try {
                val hexA = cleanedResponse.substring(4, 6)
                val hexB = cleanedResponse.substring(6, 8)
                val byteA = hexA.toIntOrNull(16) ?: 0
                val byteB = hexB.toIntOrNull(16) ?: 0
                return ((byteA * 256) + byteB) / 4 // Standard formula
            } catch (e: NumberFormatException) {
                throw Exception("Invalid RPM data format in response: $response", e)
            } catch (e: IndexOutOfBoundsException) {
                 throw Exception("Invalid RPM response length after cleaning: $cleanedResponse", e)
            }
        }
        // Log the problematic response for debugging
        println("OBDService: Invalid RPM response received: '$response', Cleaned: '$cleanedResponse'")
        throw Exception("Invalid or unexpected RPM response format: $response")
    }

    val coolantTempFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getCoolantTemp())
                delay(2000) // Poll every 2 seconds
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getCoolantTemp(): Int {
        val response = sendCommand(COOLANT_TEMP_COMMAND)
        return parseCoolantTempResponse(response)
    }

    private fun parseCoolantTempResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return (values[1].toIntOrNull(16) ?: 0) - 40 // Convert to °C
        }
        throw Exception("Invalid coolant temp response")
    }

    val fuelLevelFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getFuelLevel())
            delay(5000) // Poll every 5 seconds
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getFuelLevel(): Int {
        val response = sendCommand(FUEL_LEVEL_COMMAND)
        return parseFuelLevelResponse(response)
    }

    private fun parseFuelLevelResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return (values[1].toIntOrNull(16) ?: 0) * 100 / 255 // Convert to percentage
        }
        throw Exception("Invalid fuel level response")
    }

    val intakeAirTempFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getIntakeAirTemp())
                delay(2000) // Poll every 2 seconds
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getIntakeAirTemp(): Int {
        val response = sendCommand(INTAKE_AIR_TEMP_COMMAND)
        return parseIntakeAirTempResponse(response)
    }

    private fun parseIntakeAirTempResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return (values[1].toIntOrNull(16) ?: 0) - 40 // Convert to °C
        }
        throw Exception("Invalid intake air temp response")
    }

    val throttlePositionFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getThrottlePosition())
                delay(1000) // Poll every second
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getThrottlePosition(): Int {
        val response = sendCommand(THROTTLE_POSITION_COMMAND)
        return parseThrottlePositionResponse(response)
    }

    private fun parseThrottlePositionResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return (values[1].toIntOrNull(16) ?: 0) * 100 / 255 // Convert to percentage (0-100%)
        }
        throw Exception("Invalid throttle position response")
    }

    val fuelPressureFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getFuelPressure())
                delay(2000) // Poll every 2 seconds
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getFuelPressure(): Int {
        val response = sendCommand(FUEL_PRESSURE_COMMAND)
        return parseFuelPressureResponse(response)
    }

    private fun parseFuelPressureResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return values[1].toIntOrNull(16) ?: 0 // Returns pressure in kPa (0-765 kPa)
        }
        throw Exception("Invalid fuel pressure response")
    }

    val baroPressureFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                emit(getBaroPressure())
                delay(5000) // Poll every 5 seconds (changes slowly)
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    val batteryVoltageFlow: Flow<Float> = flow {
        while (isRunning) {
            try {
                emit(getBatteryVoltage())
                delay(5000) // Poll every 5 seconds
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.catch { e ->
        // Handle flow errors
    }

    suspend fun getBatteryVoltage(): Float {
        val response = sendCommand(BATTERY_VOLTAGE_COMMAND)
        return parseBatteryVoltageResponse(response)
    }

    private fun parseBatteryVoltageResponse(response: String): Float {
        val values = response.split(" ")
        if (values.size >= 2) {
            return values[1].toIntOrNull(16)?.let { it / 10f } ?: 0f
        }
        throw Exception("Invalid battery voltage response")
    }

    suspend fun getBaroPressure(): Int {
        val response = sendCommand(BARO_PRESSURE_COMMAND)
        return parseBaroPressureResponse(response)
    }

    private fun parseBaroPressureResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) {
            return values[1].toIntOrNull(16) ?: 0 // Returns pressure in kPa
        }
        throw Exception("Invalid barometric pressure response")
    }
}
