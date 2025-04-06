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
                println("Attempting to connect to $deviceAddress")
                socket = bluetoothManager.createSocket(deviceAddress)
                    ?: return@withContext ConnectionResult.Error("Failed to create socket")
                
                println("Socket created, attempting connection...")
                try {
                    socket?.connect()
                } catch (e: IOException) {
                    if (e.message?.contains("Device or resource busy") == true) {
                        return@withContext ConnectionResult.Error(
                            "Device already in use - close other OBD2 apps and try again"
                        )
                    }
                    throw e
                }
                delay(1000) // Wait for connection stabilization
                
                inputStream = socket?.inputStream
                    ?: return@withContext ConnectionResult.Error("No input stream")
                outputStream = socket?.outputStream
                    ?: return@withContext ConnectionResult.Error("No output stream")
                
                // Verify OBD2 compatibility
                try {
                    sendCommand("ATZ") // Reset command
                    val response = sendCommand("0100") // Mode 01 PID 00
                    if (!response.contains("41 00")) {
                        return@withContext ConnectionResult.Error("Device doesn't support OBD2 protocol")
                    }
                } catch (e: Exception) {
                    return@withContext ConnectionResult.Error("OBD2 verification failed: ${e.message}")
                }
                
                println("Connected successfully!")
                isRunning = true
                ConnectionResult.Success
            } catch (e: Exception) {
                println("Connection failed: ${e.message}")
                e.printStackTrace()
                disconnect()
                ConnectionResult.Error(
                    when {
                        e.message?.contains("read failed") == true -> "Incompatible device - please select an OBD2 adapter"
                        else -> e.message ?: "Connection failed"
                    }
                )
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
        val values = response.split(" ")
        if (values.size >= 2) {
            val byteA = values[1].toIntOrNull(16) ?: 0
            val byteB = if (values.size > 2) values[2].toIntOrNull(16) ?: 0 else 0
            return ((byteA * 256) + byteB) / 4 // Convert to RPM
        }
        throw Exception("Invalid RPM response")
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
}
