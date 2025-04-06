package com.example.cardash.services.obd

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdConnectionService(private val context: Context) {
    private var isRunning = false
    companion object {
        const val COOLANT_TEMP_COMMAND = "0105"
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun connect(deviceAddress: String): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                    ?: return@withContext ConnectionResult.Error("Device not found")

                bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                // Initialize OBD connection
                sendCommand("ATZ") // Reset
                sendCommand("ATE0") // Echo off
                sendCommand("ATSP0") // Set protocol auto

                ConnectionResult.Success
            } catch (e: IOException) {
                disconnect()
                ConnectionResult.Error("Connection failed: ${e.message}")
            }
        }
    }

    suspend fun sendCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            try {
                outputStream?.write("$command\r".toByteArray())
                outputStream?.flush()
                val buffer = ByteArray(1024)
                val bytes = inputStream?.read(buffer) ?: 0
                String(buffer, 0, bytes).trim()
            } catch (e: Exception) {
                disconnect()
                throw IOException("Command failed: ${e.message}")
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                bluetoothSocket?.close()
                inputStream?.close()
                outputStream?.close()
            } catch (e: IOException) {
                // Log error
            } finally {
                bluetoothSocket = null
                inputStream = null
                outputStream = null
            }
        }
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices ?: emptySet()
    }

    fun checkPermissions(): String? {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        return if (missingPermissions.isNotEmpty()) {
            "Missing permissions: ${missingPermissions.joinToString()}"
        } else {
            null
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    val coolantTempFlow: Flow<Int> = flow {
        while (isRunning) {
            try {
                val temp = withContext(Dispatchers.IO) { 
                    getCoolantTemp() 
                }
                emit(temp)
                delay(2000)
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }.flowOn(Dispatchers.IO)
     .catch { e -> 
         // Handle flow errors
     }

    private suspend fun getCoolantTemp(): Int {
        val response = sendCommand("0105") // Coolant temp PID
        return parseCoolantTemp(response)
    }

    private fun parseCoolantTemp(response: String): Int {
        val values = response.split(" ")
        return if (values.size >= 2) {
            (values[1].toIntOrNull(16) ?: 0) - 40 // Convert to °C
        } else {
            0
        }
    }

    sealed class ConnectionResult {
        object Success : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
    }
}
