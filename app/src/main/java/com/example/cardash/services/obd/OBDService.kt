package com.example.cardash.services.obd

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream

class OBDService(
    private val bluetoothManager: BluetoothManager,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isRunning = false

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

    suspend fun connect(deviceAddress: String): Boolean {
        return withContext(ioScope.coroutineContext) {
            try {
                println("Attempting to connect to $deviceAddress")
                socket = bluetoothManager.createSocket(deviceAddress)
                    ?: throw Exception("Failed to create socket")
                
                println("Socket created, attempting connection...")
                socket?.connect() 
                
                inputStream = socket?.inputStream
                    ?: throw Exception("No input stream")
                outputStream = socket?.outputStream
                    ?: throw Exception("No output stream")
                
                println("Connected successfully!")
                isRunning = true
                true
            } catch (e: Exception) {
                println("Connection failed: ${e.message}")
                e.printStackTrace()
                disconnect()
                false
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

    private suspend fun sendCommand(command: String): String {
        outputStream?.let { out ->
            out.write("$command\r".toByteArray())
            out.flush()
            
            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer) ?: 0
            return String(buffer, 0, bytes).trim()
        }
        throw Exception("Not connected")
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
}
