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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Legacy connection service that uses the new OBDService under the hood for compatibility
 */
class ObdConnectionService(private val context: Context) {
    private val isRunning = AtomicBoolean(false)
    private val bluetoothManager = BluetoothManager(context)
    private val obdService = OBDService(bluetoothManager)
    
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
        return when (val result = obdService.connect(deviceAddress)) {
            is OBDService.ConnectionResult.Success -> {
                isRunning.set(true)
                inputStream = obdService.inputStream
                outputStream = obdService.outputStream
                ConnectionResult.Success
            }
            is OBDService.ConnectionResult.Error -> {
                ConnectionResult.Error(result.message)
            }
        }
    }

    suspend fun sendCommand(command: String): String {
        return obdService.sendCommand(command)
    }

    suspend fun disconnect() {
        isRunning.set(false)
        obdService.disconnect()
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        return bluetoothManager.getPairedDevices()
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
        return bluetoothManager.isBluetoothEnabled()
    }

    val coolantTempFlow: Flow<Int> = obdService.coolantTempFlow

    private suspend fun getCoolantTemp(): Int {
        return obdService.getCoolantTemp()
    }

    sealed class ConnectionResult {
        object Success : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
    }
}